package com.palisand.bones.meta;

import java.io.IOException;

import com.palisand.bones.tt.ExternalLink;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Role extends Member {

	private final ExternalLink<Role,Role> opposite = new ExternalLink<Role,Role>(this,".*#/entities/.*/roles/.*",role -> role.getOpposite());
	
	private EntityType type = new EntityType();
	
	public Type<Member> getType() {
		if (type == null && opposite != null) {
			type = new EntityType();
			try {
				type.getType().set((Entity)opposite.getContainer().getContainer());
			} catch (Exception ex) {
				// ignore
			}
			return type;
		}
		return type;
	}
	
}
