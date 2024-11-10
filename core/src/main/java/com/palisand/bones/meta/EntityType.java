package com.palisand.bones.meta;

import com.palisand.bones.tt.ExternalLink;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EntityType extends Type<Member> {

	public ExternalLink<EntityType,Entity> type = new ExternalLink<>(this,".*#/entities/.*");

	@Override
	public String getId() {
		try {
			return type.get().getId();
		} catch (Exception ex) {
			// ignore
		}
		return "<noname>";
	}
	
}
