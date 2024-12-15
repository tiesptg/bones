package com.palisand.bones.meta;

import com.palisand.bones.tt.Link;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Role extends Member {

	private String pointerPattern;
	private boolean external = false;
	private final Link<Role,Role> opposite = Link.newLink(this,".*#/entities/.*/roles/.*",role -> role.getOpposite());
	
	public Type getType() {
		return Type.OBJECT;
	}
	
}
