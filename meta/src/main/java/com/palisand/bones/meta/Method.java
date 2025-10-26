package com.palisand.bones.meta;

import java.util.ArrayList;
import java.util.List;
import com.palisand.bones.tt.FieldOrder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@FieldOrder({"parameters", "returnType", "returnTypeMultiple"})
public class Method extends Item<Entity> {

  private boolean returnTypeMultiple;
  private Type returnType;
  private final List<Parameter> parameters = new ArrayList<>();

}
