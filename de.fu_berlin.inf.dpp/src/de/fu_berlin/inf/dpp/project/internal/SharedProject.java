/*
 * DPP - Serious Distributed Pair Programming
 * (c) Freie Universitaet Berlin - Fachbereich Mathematik und Informatik - 2006
 * (c) Riad Djemili - 2006
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 1, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package de.fu_berlin.inf.dpp.project.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.SubMonitor;
import org.picocontainer.Disposable;

import de.fu_berlin.inf.dpp.FileList;
import de.fu_berlin.inf.dpp.Saros;
import de.fu_berlin.inf.dpp.User;
import de.fu_berlin.inf.dpp.User.UserRole;
import de.fu_berlin.inf.dpp.activities.IActivity;
import de.fu_berlin.inf.dpp.activities.RoleActivity;
import de.fu_berlin.inf.dpp.concurrent.management.ConcurrentDocumentManager;
import de.fu_berlin.inf.dpp.concurrent.management.ConcurrentDocumentManager.Side;
import de.fu_berlin.inf.dpp.invitation.IOutgoingInvitationProcess;
import de.fu_berlin.inf.dpp.invitation.IInvitationProcess.IInvitationUI;
import de.fu_berlin.inf.dpp.invitation.internal.OutgoingInvitationProcess;
import de.fu_berlin.inf.dpp.net.ITransmitter;
import de.fu_berlin.inf.dpp.net.JID;
import de.fu_berlin.inf.dpp.net.internal.ActivitySequencer;
import de.fu_berlin.inf.dpp.net.internal.DataTransferManager;
import de.fu_berlin.inf.dpp.project.IActivityManager;
import de.fu_berlin.inf.dpp.project.ISharedProject;
import de.fu_berlin.inf.dpp.project.ISharedProjectListener;
import de.fu_berlin.inf.dpp.synchronize.Blockable;
import de.fu_berlin.inf.dpp.synchronize.StartHandle;
import de.fu_berlin.inf.dpp.synchronize.StopManager;
import de.fu_berlin.inf.dpp.util.FileUtil;
import de.fu_berlin.inf.dpp.util.Util;

/**
 * TODO Review if SharedProject, ConcurrentDocumentManager, ActivitySequencer
 * all honor start() and stop() semantics.
 */
public class SharedProject implements ISharedProject, Disposable {

    public static Logger log = Logger.getLogger(SharedProject.class.getName());

    public static final int MAX_USERCOLORS = 5;

    /* Dependencies */
    protected Saros saros;

    protected ITransmitter transmitter;

    protected ActivitySequencer activitySequencer;

    protected DataTransferManager transferManager;

    protected IProject project;

    protected StopManager stopManager;

    /* Instance fields */
    protected User localUser;

    protected ConcurrentHashMap<JID, User> participants = new ConcurrentHashMap<JID, User>();

    protected List<ISharedProjectListener> listeners = new ArrayList<ISharedProjectListener>();

    protected User host;

    protected FreeColors freeColors = null;

    protected Blockable stopManagerListener = new Blockable() {

        public void unblock() {
            setProjectReadonly(false);
        }

        public void block() {
            setProjectReadonly(true);
            // TODO Setting readonly possibly confuses the consistency watchdog
        }
    };

    protected SharedProject(Saros saros, ITransmitter transmitter,
        DataTransferManager transferManager, IProject project,
        StopManager stopManager) {

        assert (transmitter != null);

        this.saros = saros;
        this.transmitter = transmitter;
        this.project = project;
        this.transferManager = transferManager;
        this.activitySequencer = new ActivitySequencer(this, transmitter,
            transferManager);
        this.stopManager = stopManager;
        stopManager.addBlockable(stopManagerListener);
    }

    /**
     * Constructor called for SharedProject of the host
     */
    public SharedProject(Saros saros, ITransmitter transmitter,
        DataTransferManager transferManager, IProject project, JID myID,
        StopManager stopManager) {

        this(saros, transmitter, transferManager, project, stopManager);
        assert (myID != null);

        this.freeColors = new FreeColors(MAX_USERCOLORS - 1);

        this.localUser = new User(this, myID, 0);
        localUser.setUserRole(UserRole.DRIVER);
        this.host = localUser;

        this.participants.put(this.host.getJID(), this.host);

        /* add host to driver list. */
        this.activitySequencer
            .setConcurrentManager(new ConcurrentDocumentManager(Side.HOST_SIDE,
                this.host, myID, this, activitySequencer));

        setProjectReadonly(false);
    }

    /**
     * Constructor of client
     */
    public SharedProject(Saros saros, ITransmitter transmitter,
        DataTransferManager transferManager, IProject project, JID myID,
        JID hostID, int myColorID, StopManager stopManager) {

        this(saros, transmitter, transferManager, project, stopManager);

        this.host = new User(this, hostID, 0);
        this.host.setUserRole(UserRole.DRIVER);

        this.localUser = new User(this, myID, myColorID);

        this.participants.put(hostID, host);
        this.participants.put(myID, localUser);

        this.activitySequencer
            .setConcurrentManager(new ConcurrentDocumentManager(
                Side.CLIENT_SIDE, this.host, myID, this, activitySequencer));
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.fu_berlin.inf.dpp.ISharedProject
     */
    public Collection<User> getParticipants() {
        return this.participants.values();
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.fu_berlin.inf.dpp.ISharedProject
     */
    public ActivitySequencer getSequencer() {
        return this.activitySequencer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.fu_berlin.inf.dpp.project.ISharedProject
     */
    public IActivityManager getActivityManager() {
        return this.activitySequencer;
    }

    /**
     * {@inheritDoc}
     */
    public void initiateRoleChange(final User user, final UserRole newRole) {
        assert localUser.isHost() : "Only the host can initiate role changes";

        if (user.isHost()) {
            IActivity activity = new RoleActivity(getLocalUser().getJID()
                .toString(), user.getJID().toString(), newRole);
            activitySequencer.activityCreated(activity);

            setUserRole(user, newRole);
        } else {

            Util.runSafeAsync(log, new Runnable() {
                public void run() {
                    StartHandle startHandle = stopManager.stop(user);

                    if (startHandle == null) {
                        log.error("Role change failed because"
                            + " StopActivity was not acknowledged");
                        return;
                    }

                    IActivity activity = new RoleActivity(getLocalUser()
                        .getJID().toString(), user.getJID().toString(), newRole);
                    activitySequencer.activityCreated(activity);

                    setUserRole(user, newRole);

                    startHandle.start();
                }
            });
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setUserRole(final User user, final UserRole role) {

        assert user != null;
        Util.runSafeSWTSync(log, new Runnable() {
            public void run() {
                user.setUserRole(role);

                log.info("User " + user + " is now a " + role);
                if (user.isLocal()) {
                    setProjectReadonly(user.isObserver());
                }

                for (ISharedProjectListener listener : SharedProject.this.listeners) {
                    listener.roleChanged(user);
                }
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.fu_berlin.inf.dpp.ISharedProject
     */
    public User getHost() {
        return this.host;
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.fu_berlin.inf.dpp.project.ISharedProject
     */
    public boolean isHost() {
        return this.host.equals(localUser);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.fu_berlin.inf.dpp.ISharedProject
     */
    public boolean isDriver() {
        return localUser.isDriver();
    }

    public boolean isExclusiveDriver() {
        if (!isDriver()) {
            return false;
        }
        for (User user : getParticipants()) {
            if (user.isRemote() && user.isDriver()) {
                return false;
            }
        }
        return true;
    }

    public void addUser(User user) {

        assert user.getSharedProject().equals(this);

        JID jid = user.getJID();
        if (this.participants.containsKey(jid)) {
            log.warn("User " + jid + " added twice to SharedProject");
        }
        participants.putIfAbsent(jid, user);
        for (ISharedProjectListener listener : this.listeners) {
            listener.userJoined(user);
        }
        SharedProject.log.info("User " + jid + " joined session");
    }

    public void removeUser(User user) {
        JID jid = user.getJID();
        if (this.participants.remove(jid) == null) {
            log
                .warn("Tried to remove user who was not in participants: "
                    + jid);
            return;
        }
        if (isHost()) {
            returnColor(user.getColorID());
        }

        this.activitySequencer.userLeft(jid);

        // TODO what is to do here if no driver exists anymore?

        for (ISharedProjectListener listener : this.listeners) {
            listener.userLeft(user);
        }

        SharedProject.log.info("User " + jid + " left session");
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.fu_berlin.inf.dpp.project.ISharedProject
     */
    public IOutgoingInvitationProcess invite(JID jid, String description,
        boolean inactive, IInvitationUI inviteUI, FileList filelist,
        SubMonitor monitor) {

        return new OutgoingInvitationProcess(saros, transmitter,
            transferManager, jid, this, description, inactive, inviteUI,
            getFreeColor(), filelist, monitor);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.fu_berlin.inf.dpp.project.ISharedProject
     */
    public void addListener(ISharedProjectListener listener) {
        if (!this.listeners.contains(listener)) {
            this.listeners.add(listener);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.fu_berlin.inf.dpp.project.ISharedProject
     */
    public void removeListener(ISharedProjectListener listener) {
        this.listeners.remove(listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.fu_berlin.inf.dpp.project.ISharedProject
     */
    public IProject getProject() {
        return this.project;
    }

    public Thread requestTransmitter = null;

    public void start() {
        if (!stopped) {
            throw new IllegalStateException();
        }

        activitySequencer.start();

        stopped = false;

    }

    // TODO Review sendRequest for InterruptedException and remove this flag.
    boolean stopped = true;

    /**
     * Stops the associated activity sequencer.
     * 
     * @throws IllegalStateException
     *             if the shared project is already stopped.
     */
    public void stop() {
        if (stopped) {
            throw new IllegalStateException();
        }

        activitySequencer.stop();

        stopped = true;
    }

    public void dispose() {
        stopManager.removeBlockable(stopManagerListener);
        activitySequencer.dispose();
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.fu_berlin.inf.dpp.project.ISharedProject
     */
    public User getParticipant(JID jid) {
        return this.participants.get(jid);
    }

    public User getLocalUser() {
        return localUser;
    }

    public void setProjectReadonly(final boolean readonly) {
        /* TODO run project read only settings in progress monitor thread. */
        Util.runSafeSWTSync(log, new Runnable() {
            public void run() {
                FileUtil.setReadOnly(getProject(), readonly);
            }
        });
    }

    public ConcurrentDocumentManager getConcurrentDocumentManager() {
        return this.activitySequencer.getConcurrentDocumentManager();
    }

    public ITransmitter getTransmitter() {
        return transmitter;
    }

    public Saros getSaros() {
        return saros;
    }

    public int getFreeColor() {
        return freeColors.get();
    }

    public void returnColor(int colorID) {
        freeColors.add(colorID);
    }
}
