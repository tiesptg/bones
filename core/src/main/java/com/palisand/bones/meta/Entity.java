package com.palisand.bones.meta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.palisand.bones.tt.Link;
import com.palisand.bones.tt.LinkList;
import com.palisand.bones.tt.Rules;
import com.palisand.bones.tt.Rules.ListRules;
import com.palisand.bones.tt.Rules.RulesMap;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Entity extends Item<Model> {
	private static final RulesMap<Entity> RULES = Rules.<Entity>map()
			.and("members",ListRules.<Entity>builder().notEmpty(true).build());
		
	@SuppressWarnings("unchecked")
	@Override
	public Rules<Entity> getConstraint(String field) {
		return RULES.of(field);
	}
	
	private final Link<Entity,Entity> superEntity = Link.newLink(this,".*#/entities/.*",obj -> obj.getSubEntities());
	private boolean abstractEntity = false;
	private final LinkList<Entity,Entity> subEntities = new LinkList<>(this,".*#/entities/.*", obj -> obj.getSuperEntity());
	private List<Member> members = new ArrayList<>();
	private List<Method> methods = new ArrayList<>();
	
	public void setSuperEntity(Entity entity) throws IOException {
		superEntity.set(entity);
	}
	
	public void addSubEntity(Entity entity) throws IOException {
		subEntities.add(entity);
	}
	
	public void removeSubEntity(Entity entity) throws IOException {
		subEntities.remove(entity);
	}
	
	public List<Attribute> getAttributes() {
		return members.stream().filter(member -> member instanceof Attribute).map(member -> (Attribute)member).toList();
	}

	public List<Role> getRoles() {
		return members.stream().filter(member -> member instanceof Role).map(member -> (Role)member).toList();
	}
	
}
