package com.palisand.bones.meta;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DateTimeType extends AttributeType {

	private boolean onlyDate = false;
	private boolean onlyTime = false;
	private boolean local = false;
}
