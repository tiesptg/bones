package com.palisand.bones.meta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.palisand.bones.tt.FieldOrder;
import com.palisand.bones.tt.Node;
import com.palisand.bones.tt.Rules;
import com.palisand.bones.tt.Rules.ListRules;
import com.palisand.bones.tt.Rules.RulesMap;
import com.palisand.bones.tt.Rules.StringRules;
import com.palisand.bones.tt.TextIgnore;
import com.palisand.bones.tt.Validator;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@FieldOrder({"name", "description", "packageName", "entities", "enumTypes"})
public class MetaModel extends Node<Node<?>> {
  private static final RulesMap RULES = Rules.map()
      .and("name", StringRules.builder().notNull(true).pattern("[A-Z]\\w+").build())
      .and("description", StringRules.builder().multiLine(true).build())
      .and("entities", ListRules.builder().notEmpty(true).notNull(true).build()).and("packageName",
          StringRules.builder().notEmpty(true).pattern("[a-z0-9_]+(\\.[a-z0-9_]+)*").build());

  @Override
  public Rules getConstraint(String field) {
    return RULES.of(field, super::getConstraint);
  }

  private String name = "<NoName>";
  private String description = null;
  private String packageName = "";


  private List<Entity> entities = new ArrayList<>();
  private List<EnumType> enumTypes = new ArrayList<>();

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

  @Override
  protected void doValidate(Validator validator) {
    validator
        .assertTrue("entities",
            getEntities().stream().filter(validator.checkException("entities",
                entity -> entity.getEntityContainer().get() == null && !entity.isAbstractEntity()))
                .count() == 1,
            "The model should have exactly one entity that does not have an entitycontainer");
  }

}
