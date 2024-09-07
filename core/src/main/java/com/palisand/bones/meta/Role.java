package com.palisand.bones.meta;

import com.palisand.bones.tt.Ref;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Role extends Member {

	@Ref(".*#/entities/.*/roles/.*")
	private Role opposite = null;
	
	private transient EntityType type = null;
	
	public Type<Member> getType() {
		if (type == null && opposite != null) {
			type = new EntityType();
			type.setType((Entity)opposite.getContainer());
		}
		return type;
	}
	
}
