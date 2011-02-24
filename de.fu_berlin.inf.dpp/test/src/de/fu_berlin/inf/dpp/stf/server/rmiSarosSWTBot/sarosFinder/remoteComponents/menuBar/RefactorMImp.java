package de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.sarosFinder.remoteComponents.menuBar;

import java.rmi.RemoteException;

import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.STFBotShell;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.sarosFinder.remoteComponents.EclipseComponentImp;

public class RefactorMImp extends EclipseComponentImp implements RefactorM {

    private static transient RefactorMImp refactorImp;

    /**
     * {@link FileMImp} is a singleton, but inheritance is possible.
     */
    public static RefactorMImp getInstance() {
        if (refactorImp != null)
            return refactorImp;
        refactorImp = new RefactorMImp();
        return refactorImp;
    }

    /**************************************************************
     * 
     * exported functions
     * 
     **************************************************************/

    /**********************************************
     * 
     * actions
     * 
     **********************************************/

    public void moveClassTo(String targetProject, String targetPkg)
        throws RemoteException {
        moveTo(SHELL_MOVE, OK, getPkgNodes(targetProject, targetPkg));
    }

    public void renameClass(String newName) throws RemoteException {
        rename(SHELL_RENAME_COMPiIATION_UNIT, FINISH, newName);
    }

    public void renameFile(String newName) throws RemoteException {
        rename(SHELL_RENAME_RESOURCE, OK, newName);
    }

    public void renameFolder(String newName) throws RemoteException {
        rename(SHELL_RENAME_RESOURCE, OK, newName);
    }

    public void renameJavaProject(String newName) throws RemoteException {
        rename("Rename Java Project", OK, newName);
    }

    public void renamePkg(String newName) throws RemoteException {
        rename(SHELL_RENAME_PACKAGE, OK, newName);
    }

    /**************************************************************
     * 
     * inner functions
     * 
     **************************************************************/
    private void rename(String shellTitle, String buttonName, String newName)
        throws RemoteException {
        precondition();
        bot().menu(MENU_REFACTOR).menu(MENU_RENAME).click();

        STFBotShell shell = bot().shell(shellTitle);
        shell.activate();
        shell.bot_().textWithLabel(LABEL_NEW_NAME).setText(newName);
        bot().shell(shellTitle).bot_().button(buttonName).waitUntilIsEnabled();
        shell.bot_().button(buttonName).click();
        if (bot().isShellOpen("Rename Compilation Unit")) {
            bot().shell("Rename Compilation Unit").bot_().button(buttonName)
                .click();
        }
        if (bot().isShellOpen(shellTitle))
            bot().waitsUntilShellIsClosed(shellTitle);
    }

    private void moveTo(String shellTitle, String buttonName, String... nodes)
        throws RemoteException {
        precondition();
        bot().menu(MENU_REFACTOR).menu(MENU_MOVE).click();
        // bot().shell(shellTitle).waitUntilActive();
        bot().shell(shellTitle).confirmShellWithTree(buttonName, nodes);
        bot().waitsUntilShellIsClosed(shellTitle);
    }

    private void precondition() throws RemoteException {
        bot().activateWorkbench();
    }

}
