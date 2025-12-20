package com.palisand.bones.meta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.palisand.bones.tt.FieldOrder;
import com.palisand.bones.validation.CamelCase;
import com.palisand.bones.validation.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@FieldOrder({"name", "label", "description", "parameters", "returnType", "returnTypeMultiple"})
public class Method extends Item<Entity> {

  @NotNull
  @CamelCase(startsWithCapitel = false) private String name;
  private boolean returnTypeMultiple;
  private Type returnType;
  private final List<Parameter> parameters = new ArrayList<>();

  public void setName(String name) throws IOException {
    beforeIdChange(this.name, name);
    this.name = name;
  }

}
