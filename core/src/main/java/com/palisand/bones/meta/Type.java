package com.palisand.bones.meta;

import com.palisand.bones.tt.Node;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class Type<M extends Member> extends Node<M> {
	
}
