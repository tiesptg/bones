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
public class ReferenceRole extends Member {

	private String pointerPattern;
	private boolean external = false;
	private boolean notEmpty = false;
	private final Link<ReferenceRole,ReferenceRole> opposite = Link.newLink(this,".*#/entities/.*/members/.*",role -> role.getOpposite());
	private final Link<ReferenceRole,Entity> entity = Link.newLink(this,".*#/entities/.*");

	@Editor(PatternComponent.class)
	public String getPointerPattern() {
	  return pointerPattern;
	}
	
	@TextIgnore
	public Type getType() {
		return Type.OBJECT;
	}
	
}
