package com.palisand.bones.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.palisand.bones.tt.ExternalLink;
import com.palisand.bones.tt.InternalLink;
import com.palisand.bones.tt.Repository;
import com.palisand.bones.tt.Node;

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
		private long order = 10l;
		
		private final ExternalLink<Child,Child> other = new ExternalLink<>(this,".*#/children/.*",child -> child.getOther());
		
		private List<Bottom> bottoms = new ArrayList<Bottom>();
		
		public void addBottom(Bottom b) {
			b.setContainer(this,"bottoms");
			bottoms.add(b);
		}
		
	}
	
	@Getter
	@Setter
	@NoArgsConstructor
	public static class Bottom extends Node<Child> {
		private String id = null;
		
		private final InternalLink<Bottom,Bottom> other = new InternalLink<>(this,"../bottoms/.*",bottom -> bottom.getOther());
		
	}
	
	@Getter
	@Setter
	@NoArgsConstructor
	public static class Child2  extends Child {
		private boolean flag = false;
	}
	
	@Getter
	@Setter
	@NoArgsConstructor
	public static class Root extends Node<Root> {
		private boolean flag = false;
		private int number = 0;
		private String name = null;
		private double fraction = 0.0;
		private List<Child> children = new ArrayList<>();
		private Choice choice = null;
		
		public void addChild(Child child) {
			children.add(child);
			child.setContainer(this,"children");
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
		root.setName("Root");
		root.setFraction(20.20);
		Child child = new Child();
		child.setId("sd6f7s");
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
		
		Repository mapper = new Repository();
		String tmt = mapper.toTypedText(root);
		System.out.print(tmt);
		Root check = mapper.fromTypedText(tmt);
		assertEquals(mapper.toTypedText(check),tmt);
		
		File f = mapper.write("target/repository", root);
		Root root2 = (Root)mapper.read(f.getAbsolutePath());
		assertEquals(tmt,mapper.toTypedText(root2));
		assertEquals("b2",root2.getChildren().get(0).getBottoms().get(0).getOther().get().getId());
	}

	@Test
	void testTextWithAbsoluteLinks() throws IOException {
		Repository mapper = new Repository();
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
		
		Root rootX = (Root)mapper.read(f2.getAbsolutePath());
		assertEquals("sd6f7s",rootX.getChildren().get(0).getOther().get().getId());
	}

	@Test
	void testTextWithAbsoluteLinks2() throws IOException {
		Repository mapper = new Repository();
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
		
		Root rootX = (Root)mapper.read(f2.getAbsolutePath());
		assertEquals("sd6f7s",rootX.getChildren().get(0).getOther().get().getId());
	}

}
