package saros.ui.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;
import org.apache.log4j.Logger;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import saros.util.StackTrace;
import saros.util.ThreadUtils;

public class SWTUtils {

  private static final Logger log = Logger.getLogger(SWTUtils.class);

  private static Display display;

  private static class CallableResult<T> {
    private T result;
    private Exception exception;
    private Error error;
  }

  private SWTUtils() {
    // NOP
  }

  /**
   * Tries to open the given URL string in Eclipse's internal browser. However if the user specified
   * in the preferences to use an external browser instead, the external browser is tried to open.
   *
   * @param urlString the URL to show as a String
   * @param title a string displayed in the browsers title area
   * @return true if the browser could be opened, false otherwise
   */
  public static boolean openInternalBrowser(String urlString, String title) {
    URL url;
    try {
      url = new URL(urlString);
    } catch (MalformedURLException e) {
      log.error("Couldn't parse URL from string " + urlString, e);
      return false;
    }

    IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
    IWebBrowser browser;
    try {
      browser =
          browserSupport.createBrowser(
              IWorkbenchBrowserSupport.AS_EDITOR
                  | IWorkbenchBrowserSupport.LOCATION_BAR
                  | IWorkbenchBrowserSupport.NAVIGATION_BAR,
              null,
              title,
              "");
      browser.openURL(url);
      return true;
    } catch (Exception e) {
      log.error("Couldn't open internal Browser", e);
      return false;
    }
  }

  /**
   * Tries to open the given URL string in the default external browser. The Desktop API is
   * deliberately not used for this because it only works with Java 1.6.
   *
   * @param urlString the URL to show as a String
   * @return true if the browser could be opened, false otherwise
   */
  public static boolean openExternalBrowser(String urlString) {
    URL url;
    try {
      url = new URL(urlString);
    } catch (MalformedURLException e) {
      log.error("Couldn't parse URL from string " + urlString, e);
      return false;
    }

    IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
    IWebBrowser browser;
    try {
      browser = browserSupport.getExternalBrowser();
      browser.openURL(url);
      return true;
    } catch (Exception e) {
      log.error("Couldn't open external browser", e);
      return false;
    }
  }

  /** Crude check whether we are on the SWT thread */
  public static boolean isSWT() {
    return Display.getCurrent() != null;
  }

  /** @swt Needs to be called from the SWT-UI thread, otherwise <code>null</code> is returned. */
  public static IViewPart findView(String id) {
    IWorkbench workbench = PlatformUI.getWorkbench();
    if (workbench == null) return null;

    IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
    if (window == null) return null;

    IWorkbenchPage page = window.getActivePage();
    if (page == null) return null;

    return page.findView(id);
  }

  /**
   * Run the given runnable in the SWT-Thread and log any RuntimeExceptions to the given log.
   *
   * @nonBlocking
   */
  public static void runSafeSWTAsync(final Logger log, final Runnable runnable) {
    try {
      getDisplay().asyncExec(ThreadUtils.wrapSafe(log, runnable));
    } catch (SWTException e) {
      if (!PlatformUI.getWorkbench().isClosing()) throw e;

      log.warn(
          "could not execute runnable " + runnable + ", UI thread is not available",
          new StackTrace());
    }
  }

  /**
   * Runs the given callable in the SWT Thread returning the result of the computation or throwing
   * an exception that was thrown by the callable.
   */
  public static <T> T runSWTSync(final Callable<T> callable) throws Exception {

    final SWTUtils.CallableResult<T> result = new SWTUtils.CallableResult<T>();

    SWTUtils.runSafeSWTSync(
        log,
        new Runnable() {
          @Override
          public void run() {
            try {
              result.result = callable.call();
            } catch (Exception e) {
              result.exception = e;
            } catch (Error e) {
              result.error = e;
            }
          }
        });

    if (result.error != null) throw result.error;

    if (result.exception != null) throw result.exception;

    return result.result;
  }

  /**
   * Run the given runnable in the SWT-Thread, log any RuntimeExceptions to the given log and block
   * until the runnable returns.
   *
   * @blocking
   */
  public static void runSafeSWTSync(final Logger log, final Runnable runnable) {
    try {
      getDisplay().syncExec(ThreadUtils.wrapSafe(log, runnable));
    } catch (SWTException e) {
      if (!PlatformUI.getWorkbench().isClosing()) throw e;

      log.warn(
          "could not execute runnable " + runnable + ", UI thread is not available",
          new StackTrace());
    }
  }

  /**
   * Returns the display of the current workbench. Should be used instead of {@link
   * Display#getDefault()}.
   *
   * @see IWorkbench#getDisplay()
   * @return the display of the current workbench
   */
  public static Display getDisplay() {
    /**
     * This is a temporary solution. Migrating the UI from Eclipse3 to Eclipse4 caused an earlier
     * initialization of Saros and its context. Calling PlatformUI.getWorkbench.getDisplay() can not
     * be used, since the Workbench wont be created at that point, when this function is first
     * called. Continuing the migration it would be best to find an other solution by replacing all
     * calls to this function and keeping track of the display another way or find an equivalent to
     * PlatformUI.getWorkbench.getDisplay() working in E4.
     */
    if (display == null) {
      display = Display.getCurrent();
      if (display == null) {
        display = Display.getDefault();
      }
    }
    return display;
  }

  /**
   * Tries to get a {@linkplain Shell shell} that is centered on the shells of the current Eclipse
   * application.
   *
   * <p>Should be used instead of {@link Display#getActiveShell()}.
   *
   * @return a shell centered on top of the current application or <code>null</code> if no such
   *     shell exists or the default display is disposed
   */
  public static Shell getShell() {

    final Display display = getDisplay();

    if (display.isDisposed()) return null;

    final IWorkbench workbench = PlatformUI.getWorkbench();

    final IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();

    if (activeWorkbenchWindow != null && !activeWorkbenchWindow.getShell().isDisposed())
      return activeWorkbenchWindow.getShell();

    final IWorkbenchWindow[] workbenchWindows = workbench.getWorkbenchWindows();

    if (workbenchWindows.length > 0 && !workbenchWindows[0].getShell().isDisposed())
      return workbenchWindows[0].getShell();

    return display.getActiveShell();
  }
}
