package com.palisand.bones.meta;

import com.palisand.bones.tt.Node;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public abstract class Type<M extends Member> extends Node<M> {
	
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
	
}
