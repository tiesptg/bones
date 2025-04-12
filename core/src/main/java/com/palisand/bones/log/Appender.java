package com.palisand.bones.log;

import com.palisand.bones.log.Logger.Level;
import com.palisand.bones.log.Logger.Message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Appender {
  private Level level = null;

  public boolean isEnabled(Level level) {
    return this.level == null || this.level.ordinal() >= level.ordinal();
  }

  public abstract void log(Message msg);
}
