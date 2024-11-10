package com.palisand.bones.meta;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NumberType extends AttributeType {
	
	private int precision = 10;
	private int scale = 0;

}
