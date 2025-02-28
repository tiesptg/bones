package com.palisand.bones.meta;

import com.palisand.bones.tt.Node;
import com.palisand.bones.tt.TextIgnore;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@EqualsAndHashCode(callSuper = true)
public class Item<N extends Node<?>> extends Node<N> {

	private String name = "<NoName>";

	@TextIgnore
	@Override
	public String getId() {
		return name;
	}

}
