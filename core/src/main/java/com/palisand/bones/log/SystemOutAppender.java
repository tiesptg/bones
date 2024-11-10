package com.palisand.bones.log;

public class SystemOutAppender extends PrintStreamAppender {
	public SystemOutAppender() {
		super(System.out);
	}
}