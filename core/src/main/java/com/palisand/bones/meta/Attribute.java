package com.palisand.bones.meta;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Attribute extends Member {

	private AttributeType type = null;
	private String defaultValue = null;
}
