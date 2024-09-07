package com.palisand.bones.meta;

import com.palisand.bones.tt.Node;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Item<N extends Node<?>> extends Node<N> {

	private String name;

	@Override
	public String getId() {
		return name;
	}
}
