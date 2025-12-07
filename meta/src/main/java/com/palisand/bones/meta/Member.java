package com.palisand.bones.meta;

import java.io.IOException;
import com.palisand.bones.tt.FieldOrder;
import com.palisand.bones.validation.CamelCase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@FieldOrder({"multiple", "notNull", "enableWhen"})
public abstract class Member extends Item<Entity> {

  public void setName(String name) throws IOException {
    beforeIdChange(this.name, name);
    this.name = name;
  }

  @CamelCase(startsWithCapitel = false) private String name;
  private boolean multiple;
  private boolean notNull;
  private String enabledWhen = null;

  public abstract Type getType();

}
