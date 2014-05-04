package com.github.kratorius.jefs;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

class NotSafe {
  static Unsafe getUnsafe() {
    try {
      Field field = Unsafe.class.getDeclaredField("theUnsafe");
      field.setAccessible(true);
      return (Unsafe) field.get(null);
    } catch (Exception ex) {
      throw new RuntimeException();
    }
  }
}
