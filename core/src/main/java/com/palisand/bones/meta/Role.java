package com.palisand.bones.meta;

import com.palisand.bones.tt.ExternalLink;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Role extends Member {

	private final ExternalLink<Role,Role> opposite = new ExternalLink<Role,Role>(this,".*#/entities/.*/roles/.*",role -> role.getOpposite());
	
	public Type getType() {
		return Type.OBJECT;
	}
	
}
