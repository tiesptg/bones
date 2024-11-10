package com.palisand.bones.meta;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DateTimeType extends AttributeType {

	private boolean onlyDate = false;
	private boolean onlyTime = false;
	private boolean local = false;
}
