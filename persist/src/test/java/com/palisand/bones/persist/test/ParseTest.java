package com.palisand.bones.persist.test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import lombok.Data;

class ParseTest {

	class Expr {

	}

	@Data
	class CompExpr extends Expr {
		private Expr first;
		private String keyword;
		private Expr second;
	}

	@Data
	class Constant extends Expr {
		private String value;
	}

	@Data
	class Path extends Expr {
		private String path;
	}

	@Data
	class Function extends Expr {
		private String label;
		private Expr parameter = null;
	}

	class Parser {
		private static final Pattern ALL = Pattern.compile(
				"([a-zA-Z_]+[\\.a-zA-Z_0-9]*|[\\(\\)]|['][^']*[']|[<>!=]{1,2}|[\\+\\-\\*\\\\\\,]|[0-9]+[\\.]*[0-9Ee]*|\\?|@)\\s*");
		private static final Pattern PATH = Pattern.compile("[a-zA-Z_]+[\\.a-zA-Z_0-9]*");
		private static final Pattern KEYWORD = Pattern.compile("AND|OR|BETWEEN|SUM|AVG");
		private static final Pattern COMPARER = Pattern.compile("[<>!=]{1,2}");
		private static final Pattern OPERATOR = Pattern.compile("[\\+\\-\\*\\\\\\,]");

		public Expr parse(String sql) {

		}

	}

	@Test
	void test() {
		String sql = "test.role.field = ? and ? = yuz.ofl.field and ((fjk.shj_.dr47 in (?,?,?) or uiof.jsdkfjlsf879_ in @) or "
				+ "(jfkljf.gsdf78 * 45.09 <> jfkl.tf56hf + ? / jfkl.f6f7 and jflksdf.yui != ? and 'fhjkjf' = gsdfjh.iuyt.f7d8f7 and ufiof.hfjkhf.hfjds.hfksdf between ? and 879))"
				+ "or sum(jfklf.jdkf) = avg(hfjkdfh) + .9877e8";
		Pattern p = Pattern.compile(
				"([a-zA-Z_]+[\\.a-zA-Z_0-9]*|[\\(\\)]|['][^']*[']|[<>!=]{1,2}|[\\+\\-\\*\\\\\\,]|[0-9]+[\\.]*[0-9Ee]*|\\?|@)\\s*");
		Pattern path = Pattern.compile("[a-zA-Z_]+[\\.a-zA-Z_0-9]*");
		Pattern keyword = Pattern.compile("AND|OR|BETWEEN");
		Pattern comparer = Pattern.compile("[<>!=]{1,2}");
		Pattern operator = Pattern.compile("[\\+\\-\\*\\\\\\,]");
		char open = '(';
		char close = ')';
		Matcher m = p.matcher(sql);
		while (m.find()) {
			String token = m.group().trim();
			System.out.print(token + " -> ");
			if (keyword.matcher(token.toUpperCase()).matches()) {
				System.out.println("keyword");
			} else if (comparer.matcher(token).matches()) {
				System.out.println("comparer");
			} else if (operator.matcher(token).matches()) {
				System.out.println("operator");
			} else if (token.length() == 1 && token.charAt(0) == open || token.charAt(0) == close) {
				System.out.println("bracket");
			} else if (path.matcher(token).matches()) {
				System.out.println("path");
			} else {
				System.out.println("constant");
			}
		}
	}

}
