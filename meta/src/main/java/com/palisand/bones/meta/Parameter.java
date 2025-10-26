package com.palisand.bones.meta;

import com.palisand.bones.tt.FieldOrder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@FieldOrder("type")
public class Parameter extends Item<Method> {

  private Type type;

}
