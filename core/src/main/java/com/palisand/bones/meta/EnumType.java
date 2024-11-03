package com.palisand.bones.meta;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EnumType extends Type<Attribute> {
	
	public String name = "<NoName>";
	public List<String> values = new ArrayList<>();

	@Override
	public String getId() {
		return name;
	}
	
}
