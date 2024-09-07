package com.palisand.bones.tt;

public abstract class Document extends Node<Document> {
	
	public String getFilename() {
		return getContainingAttribute();
	}
	
	public void setFilename(String filename) {
		setContainingAttribute(filename);
	}

}
