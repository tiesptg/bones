package com.palisand.bones.meta;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Member extends Item {

	private Class<?> type;
	private boolean multiple;

}
