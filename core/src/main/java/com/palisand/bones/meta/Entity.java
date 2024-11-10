package com.palisand.bones.meta;

import java.util.ArrayList;
import java.util.List;

import com.palisand.bones.tt.ExternalLink;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Entity extends Item<Model> {
	
	private ExternalLink<Entity,Entity> superEntity = new ExternalLink<>(this,".*#/entities/.*");
	private boolean abstractEntity = false;
	private List<Member> members = new ArrayList<>();
	private List<Method> methods = new ArrayList<>();
	
	public List<Attribute> getAttributes() {
		return members.stream().filter(member -> member instanceof Attribute).map(member -> (Attribute)member).toList();
	}

	public List<Role> getRoles() {
		return members.stream().filter(member -> member instanceof Role).map(member -> (Role)member).toList();
	}

}
