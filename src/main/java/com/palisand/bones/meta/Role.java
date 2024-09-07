package com.palisand.bones.meta;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Role extends Member {

	private Role opposite = null;
}
