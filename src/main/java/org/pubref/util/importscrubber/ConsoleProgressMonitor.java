package org.pubref.util.importscrubber;

public class ConsoleProgressMonitor implements IProgressMonitor {
  private int filesProcessed;

  public void taskStarted(ScrubTask task) {}

  public void taskComplete(ScrubTask task) {
    filesProcessed++;
  }

  public int getFilesProcessed() {
    return filesProcessed;
  }
}
