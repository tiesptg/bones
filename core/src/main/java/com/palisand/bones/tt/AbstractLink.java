package com.palisand.bones.tt;

import java.io.IOException;

public interface AbstractLink<C extends Node<?>,X extends Node<?>> {
	
	public void internalSet(X node) throws IOException;
	public void internalUnset(X node) throws IOException;
	
}