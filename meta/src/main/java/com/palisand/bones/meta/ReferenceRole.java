package com.palisand.bones.meta;

import java.util.List;
import com.palisand.bones.Classes.Property;
import com.palisand.bones.meta.ui.PatternComponent;
import com.palisand.bones.tt.Editor;
import com.palisand.bones.tt.FieldOrder;
import com.palisand.bones.tt.Link;
import com.palisand.bones.validation.NotNull;
import com.palisand.bones.validation.Rules.PredicateWithException;
import com.palisand.bones.validation.Rules.Severity;
import com.palisand.bones.validation.Rules.Violation;
import com.palisand.bones.validation.ValidWhen;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@FieldOrder({"name", "label", "description", "entity", "opposite", "multiple", "external",
    "notNull", "notEmpty", "enableWhen", "pointerPattern"})
public class ReferenceRole extends Member {
  public static class IsMultiple implements PredicateWithException<ReferenceRole> {

    @Override
    public boolean test(ReferenceRole a) throws Exception {
      return a.isMultiple();
    }

  }

  public static class IsSingle implements PredicateWithException<ReferenceRole> {

    @Override
    public boolean test(ReferenceRole a) throws Exception {
      return !a.isMultiple();
    }

  }

  @NotNull private String pointerPattern;
  private boolean external = false;
  @ValidWhen(IsMultiple.class) private boolean notEmpty = false;
  @NotNull private final Link<ReferenceRole, ReferenceRole> opposite =
      Link.newLink(this, ".*#/entities/.*/members/.*", role -> role.getOpposite());
  @NotNull private final Link<ReferenceRole, Entity> entity =
      Link.newLink(this, ".*#/entities/.*", entity -> entity.getReferencedFrom());
  @ValidWhen(IsSingle.class) private boolean notNull;

  @Editor(PatternComponent.class)
  public String getPointerPattern() {
    return pointerPattern;
  }

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
