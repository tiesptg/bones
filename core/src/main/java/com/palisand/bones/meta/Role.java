package com.palisand.bones.meta;

import com.palisand.bones.meta.ui.PatternComponent;
import com.palisand.bones.tt.Editor;
import com.palisand.bones.tt.Link;
import com.palisand.bones.tt.TextIgnore;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Role extends Member {

	private String pointerPattern;
	private boolean external = false;
	private final Link<Role,Role> opposite = Link.newLink(this,".*#/entities/.*/roles/.*",role -> role.getOpposite());

	@Editor(PatternComponent.class)
	public String getPointerPattern() {
	  return pointerPattern;
	}
	
	@TextIgnore
	public Type getType() {
		return Type.OBJECT;
	}
	
}
