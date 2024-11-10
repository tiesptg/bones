package com.palisand.bones.meta;

import com.palisand.bones.tt.InternalLink;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Role extends Member {

	private final InternalLink<Role,Role> opposite = new InternalLink<Role,Role>(this,".*#/entities/.*/roles/.*",role -> role.getOpposite());
	
	private transient EntityType type = null;
	
	public Type<Member> getType() {
		if (type == null && opposite != null) {
			type = new EntityType();
			type.setType((Entity)opposite.getContainer().getContainer());
		}
		return type;
	}
	
}
