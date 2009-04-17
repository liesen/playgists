package orchestra.util;

import org.spearce.jgit.lib.ProgressMonitor;

/**
 * An indifferent progress monitor.
 * 
 */
public class NoProgressMonitor implements ProgressMonitor {
  public void beginTask(String title, int totalWork) {
  }

  public void endTask() {
  }

  public boolean isCancelled() {
    return false;
  }

  public void start(int totalTasks) {
  }

  public void update(int completed) {
  }
}
