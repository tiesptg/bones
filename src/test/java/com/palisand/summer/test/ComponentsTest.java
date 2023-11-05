package com.palisand.summer.test;



import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.palisand.bones.ApplicationContext;
import com.palisand.bones.Injectable;

class ComponentsTest {
	
	public static class A implements Injectable {
		public C c;
		public B b;
		
		public void injectFrom(ApplicationContext ctx) {
			this.c = ctx.get(C.class);
			this.b = ctx.get("B");
		}
	}
	
	public static class B implements Injectable {
		public C c;
		public A a;
		
		public void injectFrom(ApplicationContext ctx) {
			this.a = ctx.get(A.class);
			this.c = ctx.get(C.class);
		}
	}
	
	public static abstract class C implements Injectable {
		public A a;
		public B b;
		
		public void injectFrom(ApplicationContext ctx) {
			this.a = ctx.get(A.class);
			this.b = ctx.get("B");
		}
		
	}
	
	public static class D extends C {
		
	}
	
	public static class E implements Injectable {
		public A a;
		public B b;
		public C c;
		
		public void injectFrom(ApplicationContext ctx) {
			this.a = ctx.get(A.class);
			this.b = ctx.get("B");
			this.c = ctx.get(C.class);
		}
	}
	
	@Test
	void test() throws Exception {
		ApplicationContext ctx = new ApplicationContext() {
			{
				register(new A(), new D(),new E());
				register("B",new B());
			}
		};
		E e = ctx.get(E.class);
		assertNotNull(e);
		assertNotNull(e.a);
		assertNotNull(e.b);
		assertNotNull(e.c);
		assertTrue(e.c instanceof D);
		assertEquals(e.c,e.b.c);
		assertEquals(e.b,e.a.b);
		assertEquals(e.c.b,e.b);
		assertEquals(e.c.a, e.a);
	}
	
}
