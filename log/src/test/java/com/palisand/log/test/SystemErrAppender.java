package com.palisand.log.test;

import com.palisand.bones.log.PrintStreamAppender;

public class SystemErrAppender extends PrintStreamAppender {

  public SystemErrAppender() {
    super(System.err);
  }
}
