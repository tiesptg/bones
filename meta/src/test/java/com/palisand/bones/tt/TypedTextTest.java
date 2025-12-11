package com.palisand.bones.tt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.palisand.bones.meta.Entity;
import com.palisand.bones.meta.MetaModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

class TypedTextTest {

  public enum Choice {
    ONE, TWO, THREE;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class Child extends Node<Root> {
    private String id = null;
    private List<String> strings = new ArrayList<>();
    private long order = 10l;

    private final Link<Child, Child> other =
        Link.newLink(this, ".*#/children/.*", child -> child.getOther());

    private List<Bottom> bottoms = new ArrayList<Bottom>();

    public void addBottom(Bottom b) {
      b.setContainer(this, "bottoms");
      bottoms.add(b);
    }

  }

  @Getter
  @Setter
  @NoArgsConstructor
  public static class Bottom extends Node<Child> {
    private String id = null;

    private final Link<Bottom, Bottom> other =
        Link.newLink(this, "../bottoms/.*", bottom -> bottom.getOther());

  }

  @Getter
  @Setter
  @NoArgsConstructor
  public static class Child2 extends Child {
    private boolean flag = false;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  public static class Root extends Node<Node<?>> {
    private boolean flag = false;
    private int number = 0;
    private String name = null;
    private List<Double> doubles = new ArrayList<>();
    private double fraction = 0.0;
    private List<Child> children = new ArrayList<>();
    private Choice choice = null;

    public void addChild(Child child) {
      children.add(child);
      child.setContainer(this, "children");
    }

    @Override
    public String getId() {
      return name;
    }
  }

  @Test
  void testText() throws IOException {
    Root root = new Root();
    root.setFlag(true);
    root.setNumber(8);
    root.getDoubles().add(6345.768);
    root.getDoubles().add((double) 1);
    root.getDoubles().add(Double.MAX_VALUE);
    root.setName("Root");
    root.setFraction(20.20);
    Child child = new Child();
    child.setId("sd6f7s");
    child.getStrings().add("one string");
    child.getStrings().add("and another");
    Child2 child2 = new Child2();
    child2.setId("some very long comment \n    with a new line and spaces       ");
    child2.setOrder(25l);
    child2.setFlag(true);

    root.setChoice(Choice.THREE);
    root.addChild(child);
    root.addChild(child2);

    Bottom bottom1 = new Bottom();
    bottom1.setId("b1");
    child.addBottom(bottom1);
    Bottom bottom2 = new Bottom();
    bottom2.setId("b2");
    child.addBottom(bottom2);
    bottom1.getOther().set(bottom2);

    Repository mapper = Repository.getInstance();
    String tmt = mapper.toTypedText(root);
    System.out.print(tmt);
    Root check = mapper.fromTypedText(tmt);
    System.out.print(mapper.toTypedText(check));
    assertEquals(mapper.toTypedText(check), tmt);

    File f = mapper.write("target/repository", root);
    Root root2 = (Root) mapper.read(f.getAbsolutePath());
    assertEquals(tmt, mapper.toTypedText(root2));
    assertEquals("b2", root2.getChildren().get(0).getBottoms().get(0).getOther().get().getId());
  }

  @Test
  void testTextWithAbsoluteLinks() throws IOException {
    Repository mapper = Repository.getInstance();
    Root root = new Root();
    root.setName("Root");
    root.setFlag(true);
    root.setNumber(8);
    root.setFraction(20.20);
    Child child = new Child();
    child.setId("sd6f7s");
    root.addChild(child);
    root.setChoice(Choice.THREE);
    mapper.write("target/repository", root);

    Root root2 = new Root();
    root2.setName("Root2");
    root2.setFlag(true);
    root2.setNumber(8);
    root2.setFraction(20.20);
    mapper.write("target/repository", root2);
    Child2 child2 = new Child2();
    child2.setId("some very long comment \n    with a new line and spaces       ");
    child2.setOrder(25l);
    child2.setFlag(true);
    root2.addChild(child2);
    child.getOther().set(child2);
    root2.setChoice(Choice.ONE);

    mapper.write("target/repository", root);
    File f2 = mapper.write("target/repository", root2);

    Root rootX = (Root) mapper.read(f2.getAbsolutePath());
    assertEquals("sd6f7s", rootX.getChildren().get(0).getOther().get().getId());
  }

  @Test
  void testTextWithAbsoluteLinks2() throws IOException {
    Repository mapper = Repository.getInstance();
    Root root = new Root();
    root.setName("Root");
    root.setFlag(true);
    root.setNumber(8);
    root.setFraction(20.20);
    Child child = new Child();
    child.setId("sd6f7s");
    root.addChild(child);
    root.setChoice(Choice.THREE);
    mapper.write("target/repository", root);

    Root root2 = new Root();
    root2.setName("Root2");
    root2.setFlag(true);
    root2.setNumber(8);
    root2.setFraction(20.20);
    mapper.write("target/repository/deeper", root2);
    Child2 child2 = new Child2();
    child2.setId("some very long comment \n    with a new line and spaces       ");
    child2.setOrder(25l);
    child2.setFlag(true);
    root2.addChild(child2);
    child.getOther().set(child2);
    root2.setChoice(Choice.ONE);

    mapper.write("target/repository", root);
    File f2 = mapper.write("target/repository/deeper", root2);

    Root rootX = (Root) mapper.read(f2.getAbsolutePath());
    assertEquals("sd6f7s", rootX.getChildren().get(0).getOther().get().getId());
  }

  @Getter
  @Setter
  @NoArgsConstructor
  public static class SimpleObject {
    private String name;
    private int age;
    private boolean done;
    List<SimpleObject> list = new ArrayList<>();
  }

  private static final String TESTT =
      "com.palisand.bones.tt.TypedTextTest$SimpleObject>\n" + "unknownString:	something\n"
          + "name:	Object\n" + "justALabel:\n" + "age:	23\n" + "list:	\n" + "-	SimpleObject>\n"
          + "	name:	Another\n" + "	unknownObject: UnknownType>\n" + "		label:\n"
          + "		list:\n" + "		-	X>\n" + "			any:	false\n"
          + "	age:	40\n" + "\tdone:	true\n" + "\tdummyobject:\tG>\n" + "\t\tignore: text";

  @Test
  void testUnknownField() throws IOException {
    Repository rep = Repository.getInstance();
    SimpleObject object = rep.fromTypedText(TESTT);
    System.out.println(rep.toTypedText(object));
    assertEquals(40, object.getList().get(0).getAge());
    assertEquals(true, object.getList().get(0).isDone());
  }

  @Test
  void linkTest() throws IOException {
    Repository repository = Repository.getInstance();
    MetaModel model = new MetaModel();
    model.setName("TestModel");
    Entity one = new Entity();
    one.setName("One");
    Entity two = new Entity();
    two.setName("Two");
    Entity three = new Entity();
    three.setName("Three");
    model.addEntity(one);
    model.addEntity(two);
    model.addEntity(three);
    two.setSuperEntity(one);
    one.addSpecialisation(three);
    repository.write("target", model);
    repository.clear();
    MetaModel check = (MetaModel) repository.read("target/TestModel.tt");
    assertEquals(2, check.getEntities().get(0).getSpecialisations().getList().size());
  }

  @Test
  void checkRegex() {
    assertTrue("com.palisand.bon3s.test".matches("[a-z0-9]+(\\.[a-z0-9]+)*"));
  }

}
