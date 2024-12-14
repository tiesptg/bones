package com.palisand.bones.meta;

import java.util.ArrayList;
import java.util.List;

import com.palisand.bones.tt.ExternalLink;
import com.palisand.bones.tt.Rules;
import com.palisand.bones.tt.Validator;
import com.palisand.bones.tt.Rules.EnumRules;
import com.palisand.bones.tt.Rules.ListRules;
import com.palisand.bones.tt.Rules.NumberRules;
import com.palisand.bones.tt.Rules.RulesMap;
import com.palisand.bones.tt.Rules.StringRules;

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
