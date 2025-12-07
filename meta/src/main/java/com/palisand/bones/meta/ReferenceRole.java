package com.palisand.bones.meta;

import java.util.List;
import com.palisand.bones.Classes.Property;
import com.palisand.bones.meta.ui.PatternComponent;
import com.palisand.bones.tt.Editor;
import com.palisand.bones.tt.FieldOrder;
import com.palisand.bones.tt.Link;
import com.palisand.bones.tt.TextIgnore;
import com.palisand.bones.validation.NotNull;
import com.palisand.bones.validation.Rules.Severity;
import com.palisand.bones.validation.Rules.Violation;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@FieldOrder({"entity", "opposite", "pointerPattern", "external", "notEmpty"})
public class ReferenceRole extends Member {

  @NotNull private String pointerPattern;
  private boolean external = false;
  private boolean notEmpty = false;
  @NotNull private final Link<ReferenceRole, ReferenceRole> opposite =
      Link.newLink(this, ".*#/entities/.*/members/.*", role -> role.getOpposite());
  @NotNull private final Link<ReferenceRole, Entity> entity =
      Link.newLink(this, ".*#/entities/.*", entity -> entity.getReferencedFrom());

  @Editor(PatternComponent.class)
  public String getPointerPattern() {
    return pointerPattern;
  }

  @TextIgnore
  public Type getType() {
    return Type.OBJECT;
  }

  @Override
  public void doValidate(List<Violation> violations, List<Property<?>> properties)
      throws Exception {
    super.doValidate(violations, properties);
    if (getContainer().getEntityOfPattern(pointerPattern) == null) {
      violations.add(new Violation(Severity.ERROR, this, getProperty(properties, "pointerPattern"),
          "pattern is invalid", null));
    }
  }
}
