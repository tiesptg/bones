package com.palisand.bones.meta;

import com.palisand.bones.tt.FieldOrder;
import com.palisand.bones.tt.Link;
import com.palisand.bones.validation.NotNull;
import com.palisand.bones.validation.Rules.PredicateWithException;
import com.palisand.bones.validation.ValidWhen;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@FieldOrder({"entity", "multiple", "notEmpty"})
public class ContainerRole extends Member {
  public static class IsMultiple implements PredicateWithException<ContainerRole> {

    @Override
    public boolean test(ContainerRole a) throws Exception {
      return a.isMultiple();
    }

  }

  @NotNull private Link<ContainerRole, Entity> entity =
      Link.newLink(this, ".*#/entities/.*", entity -> entity.getEntityContainer());
  private boolean multiple = true;
  @ValidWhen(IsMultiple.class) private boolean notEmpty = false;

  @Override
  public Type getType() {
    return Type.OBJECT;
  }

}
