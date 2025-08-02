package com.palisand.bones.log;

public class SystemErrAppender extends PrintStreamAppender {
	public SystemErrAppender() {
		super(System.out);
	}
}