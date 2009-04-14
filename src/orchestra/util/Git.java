package orchestra.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Git {
  private static final Logger log = LoggerFactory.getLogger(Git.class);
  
  private final ProcessBuilder processBuilder;
  
  private final boolean execShell;
  
  /**
   * @param root
   */
  public Git(File root, boolean shell) {
    if (!root.isAbsolute()) {
      throw new IllegalArgumentException("root must be absolute");
    }
    
    execShell = shell;
    processBuilder = new ProcessBuilder().directory(root).redirectErrorStream(true);
  }
  
  private Process gitProcess(String command) throws IOException {
    if (!execShell) {
      return processBuilder.command("git " + command).start();
    }
    
    return processBuilder.command("sh", "-c", "exec", "git " + command).start();
  }
  
  private int git(String command) throws IOException {
    return git(null, command);
  }
  
  private int git(Writer out, String command) throws IOException {
    Process proc = gitProcess(command);
    
    if (out != null) {
      BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
      
      for (String line; (line = in.readLine()) != null; ) {
        out.write(line);
      }
      
      out.flush();
      in.close();
    }
    
    try {
      return proc.waitFor();
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
  }

  public void add(File path) throws IOException {
    log.info("git add {}", path.getPath());
    StringWriter out = new StringWriter();
    int exit = git(out, "add -v " + path.getPath());
    
    if (exit != 0) {
      log.info("git exited: {}", exit);
      throw new IOException(out.toString());
    }
  }
  
  public void commit(String message) throws IOException {
    log.info("commit -m '{}'", message);

    StringWriter out = new StringWriter();
    int exit = git(String.format("commit -m '%s'", message.replace("'", "\\'")));;
    
    if (exit != 0) {
      log.info("git exited: {}", exit);
      throw new IOException(out.toString());
    }
  }
  
  public void commit(String message, File... files) throws IOException {
    log.info("commit -m '{}'", message);
    StringBuffer command = new StringBuffer("git commit ");
    
    for (File f : files) {
      command.append(f.getPath());
      command.append(' ');
    }
    
    command.append("-m '").append(message.replace("'", "\\'")).append('\'');
    git(command.toString());
  }
  
  // public Future<Integer> asyncCommit(String message);
  
  public void push() throws IOException {
    log.info("git push");
    git("push");
  }
  
  public void pull() {
    log.info("git pull");
  }

  public File getRepositoryPath() {
    return processBuilder.directory();
  }
  
  private final void validateFile(File path) throws Exception {
    // Very simple resolving of paths
    /*
    if (!path.getAbsolutePath().startsWith(getRepositoryPath().getPath())) {
      throw new IOException(String.format("'%s' is outside repository", path));
    }
    */
    
    if (!new File(getRepositoryPath(), path.getPath()).exists()) {
      throw new IllegalArgumentException(String.format("'%s' does not exist in the repository", path.getPath()));
    }
  }
}
