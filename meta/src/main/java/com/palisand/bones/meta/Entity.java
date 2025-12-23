package com.palisand.bones.meta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import com.palisand.bones.tt.FieldOrder;
import com.palisand.bones.tt.Link;
import com.palisand.bones.tt.LinkList;
import com.palisand.bones.validation.CamelCase;
import com.palisand.bones.validation.NotNull;
import com.palisand.bones.validation.Rules.PredicateWithException;
import com.palisand.bones.validation.ValidWhen;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@FieldOrder({"name", "label", "description", "entityContainer", "idAttribute", "abstractEntity",
    "superEntity", "specialisations", "members", "methods", "referencesFrom"})
public class Entity extends Item<MetaModel> {

  public static class IdNecessary implements PredicateWithException<Entity> {

    @Override
    public boolean test(Entity entity) throws Exception {
      Entity e = entity;
      while (e.getSuperEntity().isPresent()) {
        e = e.getSuperEntity().get();
        if (e.getIdAttribute().isPresent()) {
          return false;
        }
      }
      return !entity.isAbstractEntity();
    }

  }

  public static class NotAbstract implements PredicateWithException<Entity> {

    @Override
    public boolean test(Entity a) throws Exception {
      return !a.isAbstractEntity();
    }

  }

  @NotNull
  @CamelCase
  private String name;
  private String pluralName;
  private String pluralLabel;
  private final Link<Entity, Entity> superEntity =
      Link.newLink(this, ".*#/entities/.*", obj -> obj.getSpecialisations());
  private boolean abstractEntity = false;
  private final LinkList<Entity, Entity> specialisations =
      new LinkList<>(this, ".*#/entities/.*", obj -> obj.getSuperEntity());
  private List<Member> members = new ArrayList<>();
  private List<Method> methods = new ArrayList<>();
  @ValidWhen(NotAbstract.class)
  private Link<Entity, ContainerRole> entityContainer =
      Link.newLink(this, ".*#/entities/.*/members/.*", contained -> contained.getEntity());
  @ValidWhen(IdNecessary.class)
  @NotNull
  private Link<Entity, Attribute> idAttribute =
      Link.newLink(this, "members/.*", attr -> attr.getIdFor());
  private LinkList<Entity, ReferenceRole> referencedFrom =
      new LinkList<>(this, ".*#/entities/.*/members/.*", refrole -> refrole.getEntity());
  private LinkList<Entity, Member> prependInOrder =
      new LinkList<>(this, "members/.*", member -> member.getPrependedFor());

  public void setName(String name) throws IOException {
    beforeIdChange(this.name, name);
    this.name = name;
  }

  public String getPluralName() {
    if (pluralName == null) {
      return getName() + "s";
    }
    return pluralName;
  }

  public String getPluralLabel() {
    if (pluralLabel == null) {
      return getPluralName();
    }
    return pluralLabel;
  }

  public Link<Entity, ContainerRole> getActiveContainer() throws IOException {
    Entity entity = this;
    while (!entity.getEntityContainer().isPresent() && entity.getSuperEntity().isPresent()) {
      entity = entity.getSuperEntity().get();
    }
    return entity.getEntityContainer();
  }

  public void setSuperEntity(Entity entity) throws IOException {
    superEntity.set(entity);
  }

  public void addSpecialisation(Entity entity) throws IOException {
    specialisations.add(entity);
  }

  public void removeSpecialisations(Entity entity) throws IOException {
    specialisations.remove(entity);
  }

  public List<Attribute> getAttributes() {
    return members.stream().filter(member -> member instanceof Attribute)
        .map(member -> (Attribute) member).toList();
  }

  public List<ReferenceRole> getReferenceRoles() {
    return members.stream().filter(member -> member instanceof ReferenceRole)
        .map(member -> (ReferenceRole) member).toList();
  }

  public List<ContainerRole> getContainerRoles() {
    return members.stream().filter(member -> member instanceof ContainerRole)
        .map(member -> (ContainerRole) member).toList();
  }

  public Entity getEntityOfPattern(String pattern) throws IOException {
    String up = "../";
    if (pattern == null) {
      return null;
    }
    if (pattern.startsWith(up)) {
      Entity check = this;
      while (!check.getEntityContainer().isPresent() && check.getSuperEntity().isPresent()) {
        check = check.getSuperEntity().get();
      }
      if (check.getEntityContainer().isPresent()) {
        return check.getEntityContainer().get().getContainer()
            .getEntityOfPattern(pattern.substring(up.length()));
      }
      return null;
    }
    int pos = pattern.indexOf("#/");
    Entity entity = this;
    if (pos >= 0) {
      pattern = pattern.substring(pos + 2);
      entity = getContainer().getModelRootEntity();
    }
    if (!pattern.isEmpty()) {
      String[] parts = pattern.split("/");
      for (int i = 0; i < parts.length; i += 2) {
        String name = parts[i];
        Optional<ContainerRole> contained =
            entity.getContainerRoles().stream().filter(c -> c.getName().equals(name)).findFirst();
        if (contained.isEmpty()) {
          return null;
        }
        entity = contained.get().getEntity().get();
        if (entity == null) {
          return null;
        }
      }
    }
    return entity;
  }

  public String getFullname() {
    if (getContainer() != null && getContainer().getPackageName() != null) {
      return getContainer().getPackageName() + "." + getName();
    }
    return getName();
  }

  public boolean isRootEntity() throws IOException {
    return getEntityContainer().get() == null && !isAbstractEntity();
  }

  public List<Member> getAllMembers() throws IOException {
    if (getSuperEntity().isPresent()) {
      List<Member> result = new ArrayList<>(getSuperEntity().get().getAllMembers());
      result.addAll(getMembers());
      List<Link<Entity, Member>> list = getPrependInOrder().getList();
      if (!list.isEmpty()) {
        Collections.reverse(list);
        for (Link<Entity, Member> link : list) {
          if (result.remove(link.get())) {
            result.add(0, link.get());
          } else {
            throw new IOException("member in prependedFor is not a member " + link.getPath()
                + " of entity " + getId());
          }
        }
      }
      return result;
    }
    return getMembers();
  }

  public boolean isA(Entity entity) throws IOException {
    Entity check = this;
    while (check != entity) { // there will always be one instance of each entity.
      if (check.getSuperEntity().isPresent()) {
        check = check.getSuperEntity().get();
      } else {
        return false;
      }
    }
    return true;
  }

}
