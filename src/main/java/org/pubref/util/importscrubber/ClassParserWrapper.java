package org.pubref.util.importscrubber;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.DescendingVisitor;
import org.apache.bcel.classfile.EmptyVisitor;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Visitor;

/**
 * This class encapsulates whatever utility we are using to parse the class file
 */
public class ClassParserWrapper {
  private static final char[] SIGNATURE_CHARS =
      new char[] {'L', 'Z', '(', ')', '[', ']', 'I', 'C', 'D'};
  private static final String STD_PREAMBLE = "\"CONSTANT_Utf8[1](\"";

  private static class ClassRefVisitor extends EmptyVisitor {
    private IReferenceFoundListener listener;
    private ConstantPool constantPool;
    private List excludedIndexes;

    public ClassRefVisitor(
        ConstantPool constantPool, IReferenceFoundListener aListener, List excludedIndexes) {
      this.listener = aListener;
      this.constantPool = constantPool;
      this.excludedIndexes = excludedIndexes;
    }

    public void visitConstantClass(ConstantClass obj) {
      String current = constantPool.constantToString(obj);
      if (current.indexOf(';') != -1) {
        current = cleanSignature(current);
      }
      if (current.indexOf('(') != -1 || current.indexOf(')') != -1 || inJavaLang(current)) {
        return;
      }
      listener.referenceFound(current);
    }

    public void visitConstantUtf8(ConstantUtf8 obj) {
      // skip the ones which we know are referred to by constant strings
      for (Iterator iter = excludedIndexes.iterator(); iter.hasNext(); ) {
        Constant constant = constantPool.getConstant(((Integer) iter.next()).intValue());
        if (constant.equals(obj)) {
          return;
        }
      }

      if (obj.toString().indexOf('$') != -1) {
        String current = removePreamble(obj).replace('$', '.');
        if (current.startsWith("class.")) {
          current = current.substring("class.".length());
        }
        if (current.length() == 0
            || current.startsWith("array")
            || current.startsWith("access")
            || current.startsWith("this")
            || current.startsWith(".")
            || current.startsWith("constructor")
            || current.startsWith("L")
            || current.endsWith(".0")
            || current.endsWith(".1")
            || current.endsWith(".2")
            || current.endsWith(".3")
            || current.endsWith(".4")
            || current.endsWith(".5")) {
          return;
        }
        for (int i = 0; i < SIGNATURE_CHARS.length; i++) {
          if (current.charAt(0) == SIGNATURE_CHARS[i]) {
            return;
          }
        }
        if (isGoodReference(current)) {
          listener.referenceFound(current);
        }
        return;
      }

      if (obj.toString().indexOf('/') == -1) {
        return;
      }

      String name = removePreamble(obj);

      if (name.startsWith(".")) {
        return;
      }

      if (name.indexOf(';') != -1) {
        StringTokenizer st = new StringTokenizer(name, ";");
        while (st.hasMoreTokens()) {
          String current = dropType(st.nextToken());
          if (isGoodReference(current)) {
            listener.referenceFound(current);
          }
        }
        return;
      }
      if (isGoodReference(name)) {
        listener.referenceFound(name);
      }
    }

    private boolean isGoodReference(String in) {
      if (in == null || in.length() == 0) {
        return false;
      }
      if (inJavaLang(in)) {
        return false;
      }
      if (in.endsWith(".")) {
        return false;
      }
      if (in.indexOf(' ') != -1
          || in.indexOf('\'') != -1
          || in.indexOf('*') != -1
          || in.indexOf(':') != -1
          || in.indexOf('(') != -1
          || in.indexOf(')') != -1
          || in.indexOf('<') != -1
          || in.indexOf('>') != -1) {
        return false;
      }
      if (Character.isUpperCase(in.charAt(0))) {
        return false;
      }
      if (in.startsWith("val.")) {
        return false;
      }
      return true;
    }

    private String cleanSignature(String in) {
      return dropType(slashToDot(dropSemicolon(in)));
    }

    private String dropSemicolon(String in) {
      return in.substring(0, in.length() - 1);
    }

    private String slashToDot(String in) {
      return in.replace('/', '.');
    }

    private String dropType(String in) {
      boolean foundSigChar = true;
      while (foundSigChar) {
        foundSigChar = false;
        if (in.length() == 0) {
          return in;
        }
        char current = in.charAt(0);
        for (int i = 0; i < SIGNATURE_CHARS.length; i++) {
          if (current == SIGNATURE_CHARS[i]) {
            in = in.substring(1);
            foundSigChar = true;
            break;
          }
        }
      }
      return in;
    }

    private String removePreamble(Constant in) {
      return in.toString()
          .substring(STD_PREAMBLE.length() - 1, in.toString().length() - 2)
          .replace('/', '.');
    }

    private boolean inJavaLang(String in) {
      return (in.startsWith("java.lang")
          && in.indexOf("java.lang.reflect") == -1
          && in.indexOf("java.lang.ref") == -1);
    }
  }

  @SuppressWarnings("unchecked")
  public static void parse(File file, IReferenceFoundListener aListener)
      throws IOException, FileNotFoundException {
    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
    ClassParser parser = new ClassParser(bis, "hello");
    JavaClass clazz = parser.parse();
    ConstantPool pool = clazz.getConstantPool();

    List excluded = new ArrayList();
    for (int i = 0; i < pool.getLength(); i++) {
      if (pool.getConstant(i) instanceof ConstantString) {
        excluded.add(new Integer(((ConstantString) pool.getConstant(i)).getStringIndex()));
      }
    }
    bis.close();
    ClassRefVisitor visitor = new ClassRefVisitor(pool, aListener, excluded);
    DescendingVisitor vehicle = new DescendingVisitor(clazz, visitor);
    vehicle.visit();
  }

  public static void main(String[] args) {
    try {
      parse(new File("d:\\data\\importscrubber\\etc\\Hello.class"), new PrintListener());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
