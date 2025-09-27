package com.palisand.bones.meta.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class CodeGenerator<X> {
  private String file;
  private PrintWriter out;
  private boolean manualEditingAllowed = false;
  private String margin = "";
  private String marginStep = "  ";
  private LogFacade logger = new LogFacade() {};

  protected void incMargin() {
    margin += marginStep;
  }

  protected void decMargin() {
    margin = margin.substring(marginStep.length());
  }

  public boolean isManualEditingAllowed() {
    return manualEditingAllowed;
  }

  protected void nl() {
    out.println();
  }

  protected void margin() {
    out.print(margin);
  }

  protected void nl(String format, Object... objects) {
    margin();
    out.println(String.format(format, objects));
  }

  protected void l(String format, Object... objects) {
    out.print(String.format(format, objects));
  }

  public abstract void config(X object);

  public abstract void generate(X object) throws IOException;

  protected void clear() {}

  public void doGenerate(File rootDir, File srcDir, X object) throws IOException {
    clear();
    config(object);
    boolean generationAllowed = true;
    if (isManualEditingAllowed()) {
      File manualFile = new File(srcDir, file);
      generationAllowed = !manualFile.exists();
    }
    if (generationAllowed) {
      File result = new File(rootDir, file);
      if (!result.getParentFile().exists() && !result.getParentFile().mkdirs()) {
        throw new IOException("Could not create parent directory of file " + file);
      }
      try (PrintWriter out = new PrintWriter(new FileWriter(result))) {
        this.out = out;
        generate(object);
      }
    }
  }

  public static String cap(String name) {
    return Character.toUpperCase(name.charAt(0)) + name.substring(1);
  }
}
