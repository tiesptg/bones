package com.palisand.bones.meta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.palisand.bones.tt.Document;
import com.palisand.bones.tt.ListConstraint;
import com.palisand.bones.tt.PropertyConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class Model extends Document {
	private static final Map<String,PropertyConstraint<?>> CONSTRAINTS = new TreeMap<>();
	
	static {
		CONSTRAINTS.put("entities",ListConstraint.builder().notEmpty(true).build());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public PropertyConstraint<?> getConstraint(String field) {
		return CONSTRAINTS.get(field);
	}
	
	private String name = "<NoName>";

	private List<Entity> entities = new ArrayList<>();
	private List<EnumType> enumTypes = new ArrayList<>();
	
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
