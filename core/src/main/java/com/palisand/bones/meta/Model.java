package com.palisand.bones.meta;

import java.util.ArrayList;
import java.util.List;

import com.palisand.bones.tt.Document;
import com.palisand.bones.tt.Rules;
import com.palisand.bones.tt.Rules.ListRules;
import com.palisand.bones.tt.Rules.RulesMap;
import com.palisand.bones.tt.TextIgnore;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class Model extends Document {
	private static final RulesMap<Model> RULES = Rules.<Model>map().and("entities", ListRules.<Model>builder().notEmpty(true).notNull(true).build());
	
	@SuppressWarnings("unchecked")
	@Override
	public Rules<Model> getConstraint(String field) {
		return RULES.of(field);
	}
	
	private String name = "<NoName>";

	private List<Entity> entities = new ArrayList<>();
	private List<EnumType> enumTypes = new ArrayList<>();
	
	@TextIgnore
	public String getId() {
		return name;
	}
	
	public void addEntity(Entity entity) {
		entities.add(entity);
		entity.setContainer(this, "entities");
	}
	
	public void addEnumType(EnumType type) {
		enumTypes.add(type);
		type.setContainer(this, "types");
	}

}
