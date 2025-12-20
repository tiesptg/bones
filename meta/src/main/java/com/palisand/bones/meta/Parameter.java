package com.palisand.bones.meta;

import java.io.IOException;
import com.palisand.bones.tt.FieldOrder;
import com.palisand.bones.validation.CamelCase;
import com.palisand.bones.validation.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@FieldOrder({"name", "label", "description", "type"})
public class Parameter extends Item<Method> {

  public void setName(String name) throws IOException {
    beforeIdChange(this.name, name);
    this.name = name;
  }

  @NotNull
  @CamelCase(startsWithCapitel = false) private String name;
  private Type type;

}
