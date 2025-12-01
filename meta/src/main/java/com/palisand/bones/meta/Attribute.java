package com.palisand.bones.meta;

import java.io.IOException;
import com.palisand.bones.tt.FieldOrder;
import com.palisand.bones.tt.Link;
import com.palisand.bones.tt.LinkList;
import com.palisand.bones.tt.Node;
import com.palisand.bones.tt.Rules;
import com.palisand.bones.tt.Rules.BooleanRules;
import com.palisand.bones.tt.Rules.EnumRules;
import com.palisand.bones.tt.Rules.LinkRules;
import com.palisand.bones.tt.Rules.NumberRules;
import com.palisand.bones.tt.Rules.RulesMap;
import com.palisand.bones.tt.Rules.StringRules;
import com.palisand.bones.tt.TextIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@FieldOrder({"type", "enumType", "defaultValue", "multiLine", "pattern", "minValue", "maxValue",
    "idFor"})
public class Attribute extends Member {
  private static final RulesMap<Attribute> RULES = Rules.<Attribute>map()
      .and("type", EnumRules.<Attribute>builder().notNull(true).notAllowed(Type.OBJECT).build())
      .and("multiLine",
          BooleanRules.<Attribute>builder().enabled(attribute -> attribute.getType() == Type.STRING)
              .build())
      .and("pattern",
          StringRules.<Attribute>builder().enabled(attribute -> attribute.getType() == Type.STRING)
              .build())
      .and("maxValue",
          NumberRules.<Attribute>builder().enabled(attribute -> attribute.getType().isNumber())
              .build())
      .and("minValue",
          NumberRules.<Attribute>builder().enabled(attribute -> attribute.getType().isNumber())
              .build())
      .and("enumType", LinkRules.<Attribute>builder()
          .enabled(attribute -> attribute.getType() == Type.ENUM).build());

  @Override
  public Rules<? extends Node<?>> getConstraint(String field) {
    return RULES.of(field, super::getConstraint);
  }

  private Type type = Type.STRING;
  private String defaultValue = null;
  private Boolean multiLine = false;
  private Long minValue = null;
  private Long maxValue = null;
  private String pattern = null;
  private Link<Attribute, EnumType> enumType =
      Link.newLink(this, ".*#/enumTypes/.*", type -> type.getTypeFor());
  private LinkList<Attribute, Entity> idFor =
      new LinkList<>(this, "#/entities/.*", entity -> entity.getIdAttribute());

  @TextIgnore
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

  @TextIgnore
  public String getJavaDefaultValue() {
    if (getDefaultValue() != null) {
      return getDefaultValue();
    }
    return "null";
  }
}
