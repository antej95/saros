package saros.server.session;

import java.io.IOException;
import org.apache.log4j.Logger;
import saros.activities.FolderCreatedActivity;
import saros.activities.FolderDeletedActivity;
import saros.filesystem.IFolder;
import saros.repackaged.picocontainer.Startable;
import saros.server.editor.ServerEditorManager;
import saros.session.AbstractActivityConsumer;
import saros.session.ISarosSession;

/** Executes FolderActivities and performs actual filesystem operations. */
public class FolderActivityExecutor extends AbstractActivityConsumer implements Startable {

  private static final Logger log = Logger.getLogger(FolderActivityExecutor.class);

  private final ISarosSession session;
  private final ServerEditorManager editorManager;

  /**
   * Creates a FolderActivityExecutor.
   *
   * @param session the current session
   * @param editorManager the editor manager
   */
  public FolderActivityExecutor(ISarosSession session, ServerEditorManager editorManager) {

    this.session = session;
    this.editorManager = editorManager;
  }

  @Override
  public void start() {
    session.addActivityConsumer(this, Priority.ACTIVE);
  }

  @Override
  public void stop() {
    session.removeActivityConsumer(this);
  }

  @Override
  public void receive(FolderCreatedActivity activity) {
    try {
      executeFolderCreation(activity);
    } catch (IOException e) {
      log.error("Could not execute " + activity, e);
    }
  }

  @Override
  public void receive(FolderDeletedActivity activity) {
    try {
      executeFolderRemoval(activity);
    } catch (IOException e) {
      log.error("Could not execute " + activity, e);
    }
  }

  private void executeFolderCreation(FolderCreatedActivity activity) throws IOException {

    IFolder folder = activity.getResource();
    folder.create();
  }

  private void executeFolderRemoval(FolderDeletedActivity activity) throws IOException {

    IFolder folder = activity.getResource();
    folder.delete();
    editorManager.closeEditorsInFolder(folder);
  }
}
