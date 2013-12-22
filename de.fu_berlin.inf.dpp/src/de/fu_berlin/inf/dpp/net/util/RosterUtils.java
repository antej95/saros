package de.fu_berlin.inf.dpp.net.util;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Registration;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo.Identity;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.jivesoftware.smackx.search.UserSearch;
import org.picocontainer.annotations.Inject;

import de.fu_berlin.inf.dpp.Saros;
import de.fu_berlin.inf.dpp.SarosPluginContext;
import de.fu_berlin.inf.dpp.net.JID;
import de.fu_berlin.inf.dpp.net.SarosNet;

/**
 * Utility class for classic {@link Roster} operations
 * 
 * @author bkahlert
 */
public class RosterUtils {
    private static final Logger log = Logger.getLogger(RosterUtils.class);

    @Inject
    private static SarosNet defaultNetwork;

    /*
     * HACK this should be initialized in a better way and removed if resolving
     * nicknames is removed from the User class
     */

    static {
        SarosPluginContext.initComponent(new RosterUtils());
    }

    private RosterUtils() {
        // no public instantiation allowed
    }

    /**
     * @param sarosNet
     *            network component that should be used to resolve the nickname
     *            or <code>null</code> to use the default one
     * @param jid
     *            the JID to resolve the nickname for
     * @return The nickname associated with the given JID in the current roster
     *         or null if the current roster is not available or the nickname
     *         has not been set.
     */
    public static String getNickname(SarosNet sarosNet, JID jid) {

        if (sarosNet == null)
            sarosNet = defaultNetwork;

        if (sarosNet == null)
            return null;

        Connection connection = sarosNet.getConnection();
        if (connection == null)
            return null;

        Roster roster = connection.getRoster();
        if (roster == null)
            return null;

        RosterEntry entry = roster.getEntry(jid.getBase());
        if (entry == null)
            return null;

        String nickName = entry.getName();
        if (nickName != null && nickName.trim().length() > 0) {
            return nickName;
        }
        return null;
    }

    public static String getDisplayableName(RosterEntry entry) {
        String nickName = entry.getName();
        if (nickName != null && nickName.trim().length() > 0) {
            return nickName.trim();
        }
        return entry.getUser();
    }

    /**
     * Creates the given account on the given XMPP server.
     * 
     * @blocking
     * 
     * @param server
     *            the server on which to create the account
     * @param username
     *            for the new account
     * @param password
     *            for the new account
     * @return <code>null</code> if the account was registered, otherwise a
     *         {@link Registration description} is returned which may containing
     *         additional information on how to register an account on the given
     *         XMPP server or an error code
     * 
     * @see Registration#getError()
     * @throws XMPPException
     *             exception that occurs while registering
     */
    public static Registration createAccount(String server, String username,
        String password) throws XMPPException {

        Connection connection = new XMPPConnection(server);

        try {
            connection.connect();

            Registration registration = getRegistrationInfo(connection,
                username);

            if (registration != null) {

                // no in band registration
                if (registration.getError() != null)
                    return registration;

                // already registered
                if (registration.getAttributes().containsKey("registered"))
                    return registration;

                // redirect
                if (registration.getAttributes().size() == 1
                    && registration.getAttributes().containsKey("instructions"))
                    return registration;
            }

            AccountManager manager = connection.getAccountManager();
            manager.createAccount(username, password);
        } finally {
            connection.disconnect();
        }

        return null;
    }

    /**
     * Removes given contact from the {@link Roster}.
     * 
     * @blocking
     * 
     * @param rosterEntry
     *            the contact that is to be removed
     * @throws XMPPException
     *             is thrown if no connection is established.
     */
    public static void removeFromRoster(Connection connection,
        RosterEntry rosterEntry) throws XMPPException {
        if (!connection.isConnected()) {
            throw new XMPPException("Not connected");
        }
        connection.getRoster().removeEntry(rosterEntry);
    }

    /**
     * Returns whether the given JID can be found on the server.
     * 
     * @blocking
     * 
     * @param connection
     * @throws XMPPException
     *             if the service discovery failed
     */
    public static boolean isJIDonServer(Connection connection, JID jid)
        throws XMPPException {

        ServiceDiscoveryManager sdm = ServiceDiscoveryManager
            .getInstanceFor(connection);

        boolean discovered = sdm.discoverInfo(jid.getRAW()).getIdentities()
            .hasNext();

        if (!discovered && jid.isBareJID())
            discovered = sdm.discoverInfo(jid.getBase() + "/" + Saros.RESOURCE)
                .getIdentities().hasNext();

        return discovered;
    }

    /**
     * Retrieve XMPP Registration information from a server.
     * 
     * This implementation reuses code from Smack but also sets the from element
     * of the IQ-Packet so that the server could reply with information that the
     * account already exists as given by XEP-0077.
     * 
     * To see what additional information can be queried from the registration
     * object, refer to the XEP directly:
     * 
     * http://xmpp.org/extensions/xep-0077.html
     */
    public static synchronized Registration getRegistrationInfo(
        Connection connection, String toRegister) throws XMPPException {
        Registration reg = new Registration();
        reg.setTo(connection.getServiceName());
        reg.setFrom(toRegister);
        PacketFilter filter = new AndFilter(new PacketIDFilter(
            reg.getPacketID()), new PacketTypeFilter(IQ.class));
        PacketCollector collector = connection.createPacketCollector(filter);

        final IQ result;

        try {
            connection.sendPacket(reg);
            result = (IQ) collector.nextResult(SmackConfiguration
                .getPacketReplyTimeout());

        } finally {
            collector.cancel();
        }

        if (result == null) {
            throw new XMPPException("No response from server.");
        } else if (result.getType() == IQ.Type.ERROR) {
            throw new XMPPException(result.getError());
        } else {
            return (Registration) result;
        }
    }

    /**
     * Returns the service for a user directory. The user directory can be used
     * to perform search queries.
     * 
     * @param connection
     *            the current XMPP connection
     * @param service
     *            a service, normally the domain of a XMPP server
     * @return the service for the user directory or <code>null</code> if it
     *         could not be determined
     * 
     * @See {@link UserSearch#getSearchForm(Connection con, String searchService)}
     */
    public static String getUserDirectoryService(Connection connection,
        String service) {

        ServiceDiscoveryManager manager = ServiceDiscoveryManager
            .getInstanceFor(connection);

        DiscoverItems items;

        try {
            items = manager.discoverItems(service);
        } catch (XMPPException e) {
            log.error("discovery for service '" + service + "' failed", e);
            return null;
        }

        Iterator<DiscoverItems.Item> iter = items.getItems();
        while (iter.hasNext()) {
            DiscoverItems.Item item = iter.next();
            try {
                Iterator<Identity> identities = manager.discoverInfo(
                    item.getEntityID()).getIdentities();
                while (identities.hasNext()) {
                    Identity identity = identities.next();
                    if ("user".equalsIgnoreCase(identity.getType())) {
                        return item.getEntityID();
                    }
                }
            } catch (XMPPException e) {
                log.warn("could not query identity: " + item.getEntityID(), e);
            }
        }

        iter = items.getItems();

        // make a good guess
        while (iter.hasNext()) {
            DiscoverItems.Item item = iter.next();

            String entityID = item.getEntityID();

            if (entityID == null)
                continue;

            if (entityID.startsWith("vjud.") || entityID.startsWith("search.")
                || entityID.startsWith("users.") || entityID.startsWith("jud.")
                || entityID.startsWith("id."))
                return entityID;
        }

        return null;
    }

    /**
     * Returns the service for multiuser chat.
     * 
     * @param connection
     *            the current XMPP connection
     * @param service
     *            a service, normally the domain of a XMPP server
     * @return the service for the multiuser chat or <code>null</code> if it
     *         could not be determined
     */
    public static String getMultiUserChatService(Connection connection,
        String service) {

        ServiceDiscoveryManager manager = ServiceDiscoveryManager
            .getInstanceFor(connection);

        DiscoverItems items;

        try {
            items = manager.discoverItems(service);
        } catch (XMPPException e) {
            log.error("discovery for service '" + service + "' failed", e);
            return null;
        }

        Iterator<DiscoverItems.Item> iter = items.getItems();
        while (iter.hasNext()) {
            DiscoverItems.Item item = iter.next();
            try {
                Iterator<Identity> identities = manager.discoverInfo(
                    item.getEntityID()).getIdentities();
                while (identities.hasNext()) {
                    Identity identity = identities.next();
                    if ("text".equalsIgnoreCase(identity.getType())
                        && "conference"
                            .equalsIgnoreCase(identity.getCategory())) {
                        return item.getEntityID();
                    }
                }
            } catch (XMPPException e) {
                log.warn("could not query identity: " + item.getEntityID(), e);
            }
        }

        return null;
    }
}