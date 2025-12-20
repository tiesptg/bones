package com.palisand.bones.meta;

import java.io.IOException;
import com.palisand.bones.tt.FieldOrder;
import com.palisand.bones.tt.Link;
import com.palisand.bones.validation.CamelCase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@FieldOrder({"name", "label", "description", "multiple", "enableWhen"})
public abstract class Member extends Item<Entity> {

  public void setName(String name) throws IOException {
    beforeIdChange(this.name, name);
    this.name = name;
  }

  @CamelCase(startsWithCapitel = false)
  private String name;
  private boolean multiple;
  private String enabledWhen = null;
  private Link<Member, Entity> prependedFor =
      Link.newLink(this, "..", entity -> entity.getPrependInOrder());

  public abstract Type getType();

}
