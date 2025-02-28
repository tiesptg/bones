package com.palisand.bones.tt;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public abstract class Document extends Node<Document> {
  
  private Repository repository;
  
  @TextIgnore
  public Repository getRepository() {
    return repository;
  }
	
	@TextIgnore
	public String getFilename() {
		return getContainingAttribute();
	}
	
	public void setFilename(String filename) {
		setContainingAttribute(filename);
	}

}
