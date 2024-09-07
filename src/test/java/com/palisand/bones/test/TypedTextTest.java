package com.palisand.bones.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.palisand.bones.typedtext.Mapper;
import com.palisand.bones.typedtext.Node;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

class TypedTextTest {
	
	public enum Choice {
		ONE, TWO, THREE;
	}
	
	@Getter
	@Setter
	@NoArgsConstructor
	public static class Child extends Node<Root> {
		private String id = null;
		private long order = 10l;
		
	}
	
	@Getter
	@Setter
	@NoArgsConstructor
	public static class Child2  extends Child {
		private boolean other = false;
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
	}

	@Test
	void test() throws IOException {
		Root root = new Root();
		root.setFlag(true);
		root.setNumber(8);
		root.setName("Root");
		root.setFraction(20.20);
		Child child = new Child();
		child.setContainer(root);
		child.setId("sd6f7s");
		Child2 child2 = new Child2();
		child2.setId("some very long comment \nwith a new line and spaces       ");
		child2.setOrder(25l);
		child2.setOther(true);
		child2.setContainer(root);
		root.setChoice(Choice.THREE);
		root.getChildren().add(child);
		root.getChildren().add(child2);
		Mapper mapper = new Mapper();
		String tmt = mapper.toText(root);
		System.out.print(tmt);
		Root check = mapper.fromText(tmt, Root.class);
		assertEquals(mapper.toText(check),tmt);
	}

}
