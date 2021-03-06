package saros.intellij.eventhandler.editor.selection;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.SelectionEvent;
import com.intellij.openapi.editor.event.SelectionListener;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import saros.filesystem.IFile;
import saros.intellij.editor.EditorManager;
import saros.intellij.eventhandler.IProjectEventHandler;

/** Dispatches activities for selection changes. */
public class LocalTextSelectionChangeHandler implements IProjectEventHandler {

  private final Project project;
  private final EditorManager editorManager;

  private final SelectionListener selectionListener =
      new SelectionListener() {
        @Override
        public void selectionChanged(@NotNull SelectionEvent selectionEvent) {
          handleTextSelectionChanged(selectionEvent);
        }
      };

  private boolean enabled;
  private boolean disposed;

  /**
   * Instantiates a LocalTextSelectionChangeHandler object.
   *
   * <p>The handler is disabled and the listener is not registered by default.
   *
   * @param editorManager the EditorManager instance
   */
  public LocalTextSelectionChangeHandler(Project project, EditorManager editorManager) {
    this.project = project;
    this.editorManager = editorManager;

    this.enabled = false;
    this.disposed = false;
  }

  @Override
  @NotNull
  public ProjectEventHandlerType getHandlerType() {
    return ProjectEventHandlerType.TEXT_SELECTION_CHANGE_HANDLER;
  }

  @Override
  public void initialize() {
    setEnabled(true);
  }

  @Override
  public void dispose() {
    disposed = true;
    setEnabled(false);
  }

  /**
   * Calls {@link EditorManager#generateSelection(IFile, Editor, int, int)}.
   *
   * <p>This method relies on the EditorPool to filter editor events.
   *
   * @param event the event to react to
   */
  // TODO handle TextRange.EMPTY_RANGE separately? Could represent no valid selection for editor
  private void handleTextSelectionChanged(SelectionEvent event) {
    Editor editor = event.getEditor();
    IFile file = editorManager.getFileForOpenEditor(editor.getDocument());

    if (file == null) {
      return;
    }

    int selectionStart = event.getNewRange().getStartOffset();
    int selectionEnd = event.getNewRange().getEndOffset();

    editorManager.generateSelection(file, editor, selectionStart, selectionEnd);
  }

  /**
   * Enables or disables the handler. This is not done by disabling the underlying listener.
   *
   * @param enabled <code>true</code> to enable the handler, <code>false</code> disable the handler
   */
  @Override
  public void setEnabled(boolean enabled) {
    assert !disposed || !enabled : "disposed listeners must not be enabled";

    if (this.enabled && !enabled) {
      EditorFactory.getInstance().getEventMulticaster().removeSelectionListener(selectionListener);

      this.enabled = false;

    } else if (!this.enabled && enabled) {
      EditorFactory.getInstance()
          .getEventMulticaster()
          .addSelectionListener(selectionListener, project);

      this.enabled = true;
    }
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }
}
