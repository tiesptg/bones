package com.palisand.bones.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.palisand.bones.tt.ExternalLink;
import com.palisand.bones.tt.InternalLink;
import com.palisand.bones.tt.Mapper;
import com.palisand.bones.tt.Node;
import com.palisand.bones.tt.Ref;

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
		
		@Ref(".*#/children/.*")
		private final ExternalLink<Child,Child> other = new ExternalLink<>(this,child -> child.getOther());
		
		private List<Bottom> bottoms = new ArrayList<Bottom>();
		
		public void addBottom(Bottom b) {
			b.setContainer(this,"bottoms");
			bottoms.add(b);
		}
		
		public void setOther(Mapper mapper, Child child) throws IOException {
			other.set(child);
		}
		
	}
	
	@Getter
	@Setter
	@NoArgsConstructor
	public static class Bottom extends Node<Child> {
		private String id = null;
		
		@Ref("../bottoms/.*")
		private final InternalLink<Bottom,Bottom> other = new InternalLink<>(this,bottom -> bottom.getOther());
		
		public void setOther(Bottom child) {
			other.set(child);
		}
		
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
		bottom1.setOther(bottom2);
		
		Mapper mapper = new Mapper();
		String tmt = mapper.toText(root);
		System.out.print(tmt);
		Root check = mapper.fromText(tmt);
		assertEquals(mapper.toText(check),tmt);
	}

	@Test
	void testTextWithAbsoluteLinks() throws IOException {
		Mapper mapper = new Mapper();
		Root root = new Root();
		root.setName("Root");
		mapper.write("target/docs/test", root);
		root.setFlag(true);
		root.setNumber(8);
		root.setFraction(20.20);
		Child child = new Child();
		child.setId("sd6f7s");
		Child2 child2 = new Child2();
		child2.setId("some very long comment \n    with a new line and spaces       ");
		child2.setOrder(25l);
		child2.setFlag(true);
		root.addChild(child);
		root.addChild(child2);
		child.setOther(mapper,child2);
		
		root.setChoice(Choice.THREE);
		
		Bottom bottom1 = new Bottom();
		bottom1.setId("b1");
		child.addBottom(bottom1);
		Bottom bottom2 = new Bottom();
		bottom2.setId("b2");
		child.addBottom(bottom2);
		bottom1.setOther(bottom2);
		
		String tmt = mapper.toText(root);
		System.out.print(tmt);
		Root check = mapper.fromText(tmt);
		assertEquals(mapper.toText(check),tmt);
	}

}
