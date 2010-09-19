package de.fu_berlin.inf.dpp.stf.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.fu_berlin.inf.dpp.net.JID;
import de.fu_berlin.inf.dpp.stf.sarosswtbot.BotConfiguration;
import de.fu_berlin.inf.dpp.stf.sarosswtbot.Musician;

public class TestShare3UsersConcurrently {
    private static final Logger log = Logger
        .getLogger(TestShare3UsersConcurrently.class);

    // bots
    protected static Musician alice;
    protected static Musician bob;
    protected static Musician carl;

    protected static ExecutorService pool;

    public static class MusicianConfiguration {
        public MusicianConfiguration(String jidString, String password,
            String host, int port) {
            this.jidString = jidString;
            this.password = password;
            this.host = host;
            this.port = port;
        }

        public String jidString;
        public String password;
        public String host;
        public int port;
    }

    @BeforeClass
    public static void setUpClass() throws RemoteException,
        InterruptedException {
        pool = Executors.newFixedThreadPool(3);

        List<Callable<Musician>> initTasks = new ArrayList<Callable<Musician>>();
        List<MusicianConfiguration> configs = new ArrayList<TestShare3UsersConcurrently.MusicianConfiguration>();
        configs.add(new MusicianConfiguration(BotConfiguration.JID_ALICE,
            BotConfiguration.PASSWORD_ALICE, BotConfiguration.HOST_ALICE,
            BotConfiguration.PORT_ALICE));
        configs.add(new MusicianConfiguration(BotConfiguration.JID_BOB,
            BotConfiguration.PASSWORD_BOB, BotConfiguration.HOST_BOB,
            BotConfiguration.PORT_BOB));
        configs.add(new MusicianConfiguration(BotConfiguration.JID_CARL,
            BotConfiguration.PASSWORD_CARL, BotConfiguration.HOST_CARL,
            BotConfiguration.PORT_CARL));

        for (final MusicianConfiguration config : configs) {
            initTasks.add(new Callable<Musician>() {
                public Musician call() throws Exception {
                    Musician result = new Musician(new JID(config.jidString),
                        config.password, config.host, config.port);
                    result.initBot();
                    return result;
                }
            });

        }

        List<Musician> result = workAll(initTasks);

        alice = result.get(0);
        bob = result.get(1);
        carl = result.get(2);

        alice.newProjectWithClass(BotConfiguration.PROJECTNAME,
            BotConfiguration.PACKAGENAME, BotConfiguration.CLASSNAME);
    }

    protected static <T> List<T> workAll(List<Callable<T>> tasks)
        throws InterruptedException {
        final List<Future<T>> futureResult;
        futureResult = pool.invokeAll(tasks);

        List<T> result = new ArrayList<T>();
        for (Future<T> future : futureResult) {
            final T value;
            try {
                value = future.get();
            } catch (ExecutionException e) {
                log.error("Couldn't execute task", e);
                continue;
            }
            result.add(value);
        }
        return result;
    }

    @AfterClass
    public static void tearDownClass() throws InterruptedException {
        Musician musicians[] = { alice, bob, carl };
        List<Callable<Void>> tasks = new ArrayList<Callable<Void>>();
        for (int i = 0; i < musicians.length; i++) {
            final Musician musician = musicians[i];
            tasks.add(new Callable<Void>() {
                public Void call() throws Exception {
                    musician.xmppDisconnect();
                    final String projectName = BotConfiguration.PROJECTNAME;
                    if (musician.isJavaProjectExist(projectName))
                        musician.deleteProject(projectName);
                    return null;
                }
            });
        }
        workAll(tasks);
    }

    @Test
    public void testShareProjectParallel() throws RemoteException,
        InterruptedException {

        List<Musician> peers = new LinkedList<Musician>();
        peers.add(carl);
        peers.add(bob);

        log.trace("alice.shareProjectParallel");
        alice.shareProjectParallel(BotConfiguration.PROJECTNAME, peers);

        List<Callable<Void>> joinSessionTasks = new ArrayList<Callable<Void>>();
        for (int i = 0; i < peers.size(); i++) {
            final Musician musician = peers.get(i);
            joinSessionTasks.add(new Callable<Void>() {
                public Void call() throws Exception {
                	musician.confirmSessionInvitationWizard(alice,
                            BotConfiguration.PROJECTNAME);
                    return null;
                }
            });
        }
        log.trace("workAll(joinSessionTasks)");
        workAll(joinSessionTasks);

        assertTrue(carl.isParticipant());
        assertTrue(carl.isObserver());
        assertTrue(carl.hasParticipant(bob));
        assertTrue(carl.isObserver(bob));
        assertTrue(carl.hasParticipant(alice));
        assertTrue(carl.isDriver(alice));

        assertTrue(bob.isParticipant());
        assertTrue(bob.isObserver());
        assertTrue(bob.hasParticipant(carl));
        assertTrue(bob.isObserver(carl));
        assertTrue(bob.hasParticipant(alice));
        assertTrue(bob.isDriver(alice));

        assertTrue(alice.isParticipant());
        assertTrue(alice.isDriver());
        assertTrue(alice.hasParticipant(carl));
        assertTrue(alice.isObserver(carl));
        assertTrue(alice.hasParticipant(bob));
        assertTrue(alice.isObserver(bob));

        List<Callable<Boolean>> leaveTasks = new ArrayList<Callable<Boolean>>();
        for (int i = 0; i < peers.size(); i++) {
            final Musician musician = peers.get(i);
            leaveTasks.add(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    musician.leaveSession();
                    return musician.isParticipant();
                }
            });
        }
        log.trace("workAll(leaveTasks)");
        final List<Boolean> workAll = workAll(leaveTasks);
        for (Boolean w : workAll)
            assertFalse(w);

        log.trace("waitUntilOtherLeaveSession");
        alice.waitUntilOtherLeaveSession(carl);
        alice.waitUntilOtherLeaveSession(bob);
        alice.leaveSession();
        assertFalse(alice.isParticipant());
        log.trace("testShareProjectParallel done");
    }
}
