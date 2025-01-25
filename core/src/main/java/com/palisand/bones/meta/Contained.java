package com.palisand.bones.meta;

import com.palisand.bones.tt.Link;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Contained extends Item<Entity> {
  private Link<Contained,Entity> entity = Link.newLink(this, ".*#/entities/.*",entity -> entity.getEntityContainer());
  private boolean multiple = true;
}
