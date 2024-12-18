package com.palisand.bones.meta;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Method extends Member {

	private Type type;
	private final List<Parameter> parameters = new ArrayList<>();
	
}
