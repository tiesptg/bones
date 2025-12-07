package com.palisand.bones.meta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import com.palisand.bones.tt.FieldOrder;
import com.palisand.bones.tt.Link;
import com.palisand.bones.tt.Node;
import com.palisand.bones.tt.TextIgnore;
import com.palisand.bones.validation.CamelCase;
import com.palisand.bones.validation.NoXss;
import com.palisand.bones.validation.NotEmpty;
import com.palisand.bones.validation.NotNull;
import com.palisand.bones.validation.RegexPattern;
import com.palisand.bones.validation.Rules;
import com.palisand.bones.validation.Rules.Rule;
import com.palisand.bones.validation.Rules.Severity;
import com.palisand.bones.validation.Rules.Violation;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@FieldOrder({"name", "description", "packageName", "entities", "enumTypes"})
public class MetaModel extends Node<Node<?>> {

  private static void registerNoCycle() {
    Rules.addRule(NoCycle.class, (violations, ownerOfField, spec, property, value) -> {
      Object check = value;
      HashSet<Object> elements = new HashSet<>();
      elements.add(ownerOfField);
      while (check != null) {
        if (!elements.add(check)) {
          violations.add(new Violation(Severity.ERROR, ownerOfField, property,
              "The value is part of a cycle", null));
          break;
        }
        if (check instanceof Link<?, ?> link) {
          check = link.get();
        }
        check = property.get(check);
      }
      return null;
    });
  }

  private static void registerNotNull() {
    Rule rule = Rules.getRule(NotNull.class);
    Rules.addRule(NotNull.class, (violations, ownerOfField, spec, property, value) -> {
      if (value instanceof Link<?, ?> link) {
        value = link.get();
      }
      return rule.validate(violations, ownerOfField, spec, property, value);
    });
  }

  static {
    registerNoCycle();
    registerNotNull();
  }

  @NotNull
  @CamelCase private String name = null;
  @MultiLine
  @NoXss private String description = null;
  @NotNull
  @RegexPattern("[a-z0-1]+(\\.[a-z0-9]+)*") private String packageName = "";

  @NotEmpty private List<Entity> entities = new ArrayList<>();
  private List<EnumType> enumTypes = new ArrayList<>();

  public void setName(String name) throws IOException {
    beforeIdChange(this.name, name);
    this.name = name;
  }

  @Override
  @TextIgnore
  public String getId() {
    return name;
  }

  public void addEntity(Entity entity) {
    entities.add(entity);
    entity.setContainer(this, "entities");
  }

  public void addEnumType(EnumType type) {
    enumTypes.add(type);
    type.setContainer(this, "types");
  }

  @TextIgnore
  public Entity getModelRootEntity() throws IOException {
    for (Entity entity : entities) {
      if (!entity.getEntityContainer().isPresent() && !entity.isAbstractEntity()) {
        return entity;
      }
    }
    return null;
  }
}
