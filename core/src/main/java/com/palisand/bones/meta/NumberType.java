package com.palisand.bones.meta;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class NumberType extends AttributeType {
	
	private int precision = 10;
	private int scale = 0;

}
