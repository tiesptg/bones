package com.palisand.meta.maven;

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
  
  protected void nl(String format, Object...objects) {
    margin();
    out.println(String.format(format,objects));
  }
  
  protected void l(String format, Object...objects) {
    out.println(String.format(format,objects));
  }
  
  public abstract void config(X object);
  
  public abstract void generate(X object);
  
  public void doGenerate(X object) throws IOException {
    config(object);
    File result = new File(file);
    if (!result.getParentFile().mkdirs()) {
      throw new IOException("Could not create parent directory of file " + file );
    }
    try (PrintWriter out = new PrintWriter(new FileWriter(result))) {
      this.out = out;
      generate(object);
    }
  }
}
