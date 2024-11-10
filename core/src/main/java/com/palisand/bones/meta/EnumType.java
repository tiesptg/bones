package com.palisand.bones.meta;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EnumType extends Type<Attribute> {
	
	public String name = "<NoName>";
	public List<String> values = new ArrayList<>();

	@Override
	public String getId() {
		return name;
	}
	
}
