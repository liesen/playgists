package orchestra.util;

import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spearce.jgit.lib.ProgressMonitor;

public class LoggingProgressMonitor implements ProgressMonitor {
  private final Logger logger;
  
  private Stack<String> tasks;
  
  /**
   * @param logger
   */
  public LoggingProgressMonitor(Logger logger) {
    this.logger = logger;
    tasks = new Stack<String>();
  }
  
  public LoggingProgressMonitor() {
    this(LoggerFactory.getLogger(LoggingProgressMonitor.class));
  }

  public void beginTask(String title, int totalWork) {
    tasks.push(title);
    logger.info("Beginning task: {}", title);
  }

  public void endTask() {
    logger.info("Task ended: {}", tasks.pop());
  }

  public boolean isCancelled() {
    return false;
  }

  public void start(int totalTasks) {
    logger.info("Starting {} tasks", totalTasks);
  }

  public void update(int completed) {
    logger.info("Completed: {}", completed);
  }
}
