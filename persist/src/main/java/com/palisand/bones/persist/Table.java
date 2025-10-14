package com.palisand.bones.persist;

import com.palisand.bones.persist.Database.Id;
import com.palisand.bones.persist.Database.Mapped;
import com.palisand.bones.persist.Database.Version;
import lombok.Data;

@Data
@Mapped
public class Table {

  @Id(generated = true)
  private long oid;

  @Version
  private int oversion;

}

