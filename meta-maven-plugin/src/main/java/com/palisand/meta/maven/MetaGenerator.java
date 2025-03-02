package com.palisand.meta.maven;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.palisand.bones.meta.generator.CodeGenerator;
import com.palisand.bones.meta.generator.GeneratorConfig;
import com.palisand.bones.tt.Document;
import com.palisand.bones.tt.Node;
import com.palisand.bones.tt.ObjectConverter;
import com.palisand.bones.tt.ObjectConverter.Property;
import com.palisand.bones.tt.Repository;
import com.palisand.bones.tt.Rules.ConstraintViolation;
import com.palisand.bones.tt.Rules.Severity;
import com.palisand.bones.tt.Validator;

@Mojo(name="generate-sources", requiresProject=true, defaultPhase=LifecyclePhase.GENERATE_SOURCES)
public class MetaGenerator extends AbstractMojo {
  
  @Parameter(property="outputDirectory",defaultValue = "${project.build.directory}/generated-sources/meta-generator")
  private String outputDirectory;
  
  @Parameter(property="model")
  private String model;
  
  @Parameter(property="generatorConfig")
  private GeneratorConfig generatorConfig;
  
  @Parameter(defaultValue="${project}")
  private MavenProject project;
  
  private Repository repository = new Repository();

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    File dir = new File(outputDirectory);
    if (!dir.exists() && !dir.mkdirs()) {
      throw new MojoExecutionException("output directory [" + outputDirectory + "] does not exist and cannot be created");
    }
    project.addCompileSourceRoot(outputDirectory);
    File modelFile = new File(project.getBasedir(),model);
    if (!modelFile.exists()) {
      throw new MojoExecutionException("model file [" + model + "] does not exist");
    }
    try {
      repository.read(modelFile.getAbsolutePath());
      Validator validator = new Validator();
      repository.getLoadedDocuments().forEach(doc -> doc.validate(validator));
      List<ConstraintViolation> problems = validator.getViolations();
      problems.forEach(violation -> {
        switch (violation.severity()) {
        case ERROR: getLog().error(violation.toString()); break;
        case WARNING: getLog().warn(violation.toString()); break;
        }
      });
      if (problems.stream().noneMatch(problem -> problem.severity() == Severity.ERROR)) {
        for (Document document: repository.getLoadedDocuments()) {
          generateNode(dir, document);
        }
      } else {
        throw new MojoFailureException("Model is not valid");
      }
    } catch (Exception e) {
      throw new MojoExecutionException(e);
    }
  }
  
  private File getSourceDirectory() {
    return new File((String)project.getCompileSourceRoots().get(0));
  }
  
  @SuppressWarnings("unchecked")
  private <X extends Node<?>> void generateNode(File outputDirectory, X node) throws Exception {
    CodeGenerator<X>[] generators = (CodeGenerator<X>[])generatorConfig.getGenerators(node.getClass());
    for (CodeGenerator<X> generator: generators) {
      getLog().info("Generate " + generator.getClass().getSimpleName() + " with " + node);
      generator.doGenerate(outputDirectory,getSourceDirectory(),node);
    }
    ObjectConverter converter = (ObjectConverter)repository.getConverter(node.getClass());
    for (Property property: converter.getProperties()) {
      if (Node.class.isAssignableFrom(property.getComponentType()) && !property.isLink()) {
        if (property.isList()) {
          List<Node<?>> list = (List<Node<?>>)property.getGetter().invoke(node);
          for (Node<?> child: list) {
            generateNode(outputDirectory,child);
          }
        } else {
          Node<?> child = (Node<?>)property.getGetter().invoke(node);
          generateNode(outputDirectory,child);
        }
      }
    }
  }

}
