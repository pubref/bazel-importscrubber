package org.pubref.util.importscrubber;

public class PrintListener implements IReferenceFoundListener {
  private int total = 0;

  public void referenceFound(String className) {
    System.out.println(total + ":" + className);
    total++;
  }
}
