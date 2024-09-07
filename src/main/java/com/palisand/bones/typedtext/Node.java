package com.palisand.bones.typedtext;

import lombok.Getter;
import lombok.Setter;

@Getter
public abstract class Node<N extends Node<?>> {
	private N container;
	@Setter private String containerField;
	
	@SuppressWarnings("unchecked")
	public void setContainer(Node<?> node) {
		container = (N)node;
	}
}
