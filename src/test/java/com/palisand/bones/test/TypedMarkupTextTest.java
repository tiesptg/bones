package com.palisand.bones.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.palisand.bones.text.TypedMarkupText;

import lombok.Data;
import lombok.NoArgsConstructor;

class TypedMarkupTextTest {
	
	@Data
	@NoArgsConstructor
	public static class Child {
		String id = null;
		long order = 10l;
	}
	
	@Data
	@NoArgsConstructor
	public static class Child2  extends Child {
		boolean other = false;
	}
	
	@Data
	@NoArgsConstructor
	public static class Root {
		boolean flag = false;
		int number = 0;
		String name = null;
		double fraction = 0.0;
		List<Child> children = new ArrayList<>();
	}

	@Test
	void test() throws IOException {
		Root root = new Root();
		root.setFlag(true);
		root.setNumber(8);
		root.setName("Root");
		root.setFraction(20.20);
		Child child = new Child();
		child.setId("sd6f7s");
		Child2 child2 = new Child2();
		child2.setId("some very long comment \nwith a new line and spaces       ");
		child2.setOrder(25l);
		child2.setOther(true);
		root.getChildren().add(child);
		root.getChildren().add(child2);
		TypedMarkupText mapper = new TypedMarkupText("com.palisand.bones.test.TypedMarkupTextTest$");
		String tmt = mapper.toYaml(root);
		System.out.print(tmt);
		Root check = mapper.fromYaml(tmt, Root.class);
		assertEquals(mapper.toYaml(check),tmt);
	}

}
