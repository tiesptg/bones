package com.palisand.bones.meta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.palisand.bones.tt.FieldOrder;
import com.palisand.bones.tt.LinkList;
import com.palisand.bones.validation.CamelCase;
import com.palisand.bones.validation.NotEmpty;
import com.palisand.bones.validation.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@FieldOrder({"values", "typeFor"})
public class EnumType extends Item<MetaModel> {

  public void setName(String name) throws IOException {
    beforeIdChange(this.name, name);
    this.name = name;
  }

  @NotNull
  @CamelCase private String name;
  @NotEmpty private List<String> values = new ArrayList<>();
  private LinkList<EnumType, Attribute> typeFor =
      new LinkList<>(this, ".*#/entities/.*/members/.*", attribute -> attribute.getEnumType());

}
