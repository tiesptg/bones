package com.palisand.bones.meta;

import com.palisand.bones.tt.Node;
import com.palisand.bones.tt.Rule;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@EqualsAndHashCode(callSuper = true)
public class Item<N extends Node<?>> extends Node<N> {

	@Rule(required=true,min=3)
	private String name = "<NoName>";

	@Override
	public String getId() {
		return name;
	}
}
