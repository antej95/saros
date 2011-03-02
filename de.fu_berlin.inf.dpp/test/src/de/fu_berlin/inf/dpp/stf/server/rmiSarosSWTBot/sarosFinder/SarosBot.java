package de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.sarosFinder;

import java.rmi.Remote;
import java.rmi.RemoteException;

import de.fu_berlin.inf.dpp.net.JID;
import de.fu_berlin.inf.dpp.stf.client.testProject.testsuits.STFTest.TypeOfCreateProject;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.sarosFinder.remoteComponents.contextMenu.SarosC;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.sarosFinder.remoteComponents.menuBar.EditM;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.sarosFinder.remoteComponents.menuBar.SarosM;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.sarosFinder.remoteComponents.menuBar.WindowM;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.sarosFinder.remoteComponents.views.ConsoleView;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.sarosFinder.remoteComponents.views.PEView;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.sarosFinder.remoteComponents.views.ProgressView;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.sarosFinder.remoteComponents.views.sarosViews.ChatView;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.sarosFinder.remoteComponents.views.sarosViews.RSView;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.sarosFinder.remoteComponents.views.sarosViews.RosterView;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.sarosFinder.remoteComponents.views.sarosViews.SessionView;

public interface SarosBot extends Remote {

    /**********************************************
     * 
     * finders
     * 
     **********************************************/
    public State state() throws RemoteException;

    public Wait condition() throws RemoteException;

    // public FileM file() throws RemoteException;

    public EditM edit() throws RemoteException;

    // public RefactorM refactor() throws RemoteException;

    public SarosM saros() throws RemoteException;

    public WindowM window() throws RemoteException;

    public ChatView chatView() throws RemoteException;

    public RosterView buddiesView() throws RemoteException;

    public RSView remoteScreenView() throws RemoteException;

    public SessionView sessionView() throws RemoteException;

    public ConsoleView consoleView() throws RemoteException;

    public PEView packageExplorerView() throws RemoteException;

    public ProgressView progressView() throws RemoteException;

    public void setJID(JID jid) throws RemoteException;

    /**
     * After the {@link SarosC#confirmShellInvitation(String...)} the popup
     * wizard with the title "Session Invitation" should be appeared by the
     * invitees' side. This method confirm the wizard "Session Invitation" using
     * a new project.
     * 
     * @throws RemoteException
     */
    public void confirmShellAddProjectWithNewProject(String projectname)
        throws RemoteException;

    /**
     * After the {@link SarosC#confirmShellInvitation(String...)} the popup
     * wizard with the title "Session Invitation" should be appeared by the
     * invitees' side. This method confirm the wizard "Session Invitation" using
     * a exist project.
     * 
     * @throws RemoteException
     */
    public void confirmShellAddProjectUsingExistProject(String projectName)
        throws RemoteException;

    /**
     * After the {@link SarosC#confirmShellInvitation(String...)} the popup
     * wizard with the title "Session Invitation" should be appeared by the
     * invitees' side. This method confirm the wizard "Session Invitation" using
     * a exist project with clicking the button browser->confirming popup window
     * -> clicking the button "finish" -> conforming the local change
     * 
     * @throws RemoteException
     */
    public void confirmShellAddProjectUsingExistProjectWithCopyAfterCancelLocalChange(
        String projectName) throws RemoteException;

    /**
     * After the {@link SarosC#confirmShellInvitation(String...)} the popup
     * wizard with the title "Session Invitation" should be appeared by the
     * invitees' side. This method confirm the wizard "Session Invitation" using
     * a exist project with copy
     * 
     * @throws RemoteException
     */
    public void confirmShellAddProjectUsingExistProjectWithCopy(
        String projectName) throws RemoteException;

    /**
     * After the {@link SarosC#confirmShellInvitation(String...)} the popup
     * wizard with the title "Session Invitation" should be appeared by the
     * invitees' side. This method confirm the wizard "Session Invitation" using
     * a new project or a existed project according the passed parameter
     * "usingWhichProject".
     * 
     * @throws RemoteException
     */
    public void confirmShellAddProjectUsingWhichProject(String projectName,
        TypeOfCreateProject usingWhichProject) throws RemoteException;

    public void confirmShellChangeXMPPAccount(String newServer,
        String newUserName, String newPassword) throws RemoteException;

    /**
     * Confirm the popUp window "create new XMPP account".
     * 
     * @param jid
     *            jid of the new XMPP account
     * @param password
     *            password of the new XMPP account
     * @throws RemoteException
     */
    public void confirmShellCreateNewXMPPAccount(JID jid, String password)
        throws RemoteException;

    /**
     * confirm the wizard "Saros configuration".
     * 
     * @param jid
     *            jid of the new XMPP account
     * @param password
     *            password of the new XMPP account
     * @throws RemoteException
     */
    public void confirmWizardSarosConfiguration(JID jid, String password)
        throws RemoteException;

    /**
     * After clicking one of the sub menu of the context menu "Saros" in the
     * package explorer view host will get the popup window with the title
     * "Invitation". This method confirm the popup window.
     * 
     * @param inviteesBaseJIDs
     *            the base JID of the users with whom you want to share your
     *            project.
     * @throws RemoteException
     */
    public void confirmShellInvitation(String... inviteesBaseJIDs)
        throws RemoteException;

    /**
     * Confirm the popUp window "Closing the session", which would be triggered,
     * when host try to leave a session.
     * 
     * @throws RemoteException
     */
    public void confirmShellClosingTheSession() throws RemoteException;

}