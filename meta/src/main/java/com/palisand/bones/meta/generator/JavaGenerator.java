package com.palisand.bones.meta.generator;

import java.util.Set;
import java.util.TreeSet;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class JavaGenerator<X> extends CodeGenerator<X> {
  private String className;
  private String packageName;
  private Set<String> imports = new TreeSet<>();
  
  public void addImport(Class<?> cls) {
    addImport(cls.getName());
  }
  
  public void addImport(String className) {
    imports.add(className);
  }
  
  public void addStaticImport(String fullName) {
    addImport("static " + fullName);
  }
  
  public void printImports() {
    imports.forEach(imp -> nl("import %s;",imp));
  }
  
  public void setPackageAndClass(String packageName, String className) {
    this.packageName = packageName;
    this.className = className;
    setFile(packageName.replace(".","/") + "/" + className + ".java");
  }

}
