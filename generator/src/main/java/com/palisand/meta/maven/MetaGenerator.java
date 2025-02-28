package com.palisand.meta.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name="meta-generator", defaultPhase=LifecyclePhase.GENERATE_SOURCES)
public class MetaGenerator extends AbstractMojo {
  
  @Parameter(property="outputDirectory",defaultValue = "${project.build.directory}/generated-sources/bones-generator")
  private String outputDirectory;
  
  @Parameter(property="model")
  private String modelFile;
  
  @Parameter(property="generatorClass")
  private Class<? extends GeneratorConfig> generatorClass;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
  }

}
