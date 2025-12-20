package com.palisand.bones.meta;

import java.io.IOException;
import com.palisand.bones.tt.FieldOrder;
import com.palisand.bones.tt.Link;
import com.palisand.bones.tt.LinkList;
import com.palisand.bones.validation.NotAllowed;
import com.palisand.bones.validation.NotNull;
import com.palisand.bones.validation.Rules.PredicateWithException;
import com.palisand.bones.validation.ValidWhen;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@FieldOrder({"name", "label", "description", "type", "enumType", "defaultValue", "multiple",
    "notNull", "enableWhen", "multiLine", "casing", "pattern", "minValue", "maxValue", "idFor"})
public class Attribute extends Member {

  public static class TypeIsString implements PredicateWithException<Attribute> {
    @Override
    public boolean test(Attribute a) throws Exception {
      return a.getType() == Type.STRING;
    }
  }

  public static class TypeIsNumber implements PredicateWithException<Attribute> {
    @Override
    public boolean test(Attribute a) throws Exception {
      return a.getType().isNumber();
    }
  }

  public static class TypeIsEnum implements PredicateWithException<Attribute> {
    @Override
    public boolean test(Attribute a) throws Exception {
      return a.getType() == Type.ENUM;
    }
  }

  @NotAllowed("OBJECT")
  @NotNull
  private Type type = Type.STRING;
  private String defaultValue = null;
  private boolean notNull;
  @ValidWhen(TypeIsString.class)
  private Casing casing = null;
  @ValidWhen(TypeIsString.class)
  private Boolean multiLine = false;
  @ValidWhen(TypeIsNumber.class)
  private Long minValue = null;
  @ValidWhen(TypeIsNumber.class)
  private Long maxValue = null;
  private String pattern = null;
  @ValidWhen(TypeIsEnum.class)
  private Link<Attribute, EnumType> enumType =
      Link.newLink(this, ".*#/enumTypes/.*", type -> type.getTypeFor());
  private LinkList<Attribute, Entity> idFor =
      new LinkList<>(this, "#/entities/.*", entity -> entity.getIdAttribute());

  public String getJavaType() throws IOException {
    switch (type) {
      case STRING:
        return "String";
      case INTEGER:
        return "Integer";
      case DOUBLE:
        return "Double";
      case TIMESTAMP:
        return "OffsetDateTime";
      case BOOLEAN:
        return "Boolean";
      case ENUM:
        return enumType.get().getName();
      case OBJECT:
        break;
    }
    throw new IOException("attribute " + getName() + " has unsupported type " + type);
  }

  public String getJavaDefaultValue() {
    if (getDefaultValue() != null) {
      return getDefaultValue();
    }
    return "null";
  }
}
