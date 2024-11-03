package com.palisand.bones.meta;

import java.util.ArrayList;
import java.util.List;

import com.palisand.bones.tt.Document;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Model extends Document {
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
