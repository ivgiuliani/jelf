package com.github.kratorius.jefs;

import sun.misc.Unsafe;

public class LFBitSet extends NotSafe {
  private static final int INT_MSB = 5;

  private static Unsafe unsafe = getUnsafe();
  private static final int base = unsafe.arrayBaseOffset(int[].class);
  private static final int shift;

  static {
    int scale = Integer.numberOfLeadingZeros(unsafe.arrayIndexScale(int[].class));
    shift = (1 << INT_MSB) - scale - 1;
  }

  /*
   * An array of longs would probably be more efficient, however writes to
   * longs are not atomic, whereas writes to ints are.
   * (the volatile is needed because clear() creates a whole new array)
   */
  private volatile int[] bitset;
  private final int nbits;

  public LFBitSet(int nbits) {
    if (nbits < 0) {
      throw new IllegalArgumentException();
    }

    this.nbits = nbits;
    this.bitset = new int[getBucket(nbits) + 1];
  }

  private long byteOffset(int idx) {
    return ((long) idx << shift) + base;
  }

  private int getBucket(int bit) {
    //noinspection NumericOverflow
    return ((bit - 1) >> INT_MSB) + 1;
  }

  public void clear() {
    this.bitset = new int[getBucket(nbits) + 1];
  }

  public void clear(int bitIndex) {
    int bucket = getBucket(bitIndex);
    if (bucket >= bitset.length) {
      throw new IndexOutOfBoundsException();
    }

    int v1, v2;
    for (;;) {
      v1 = bitset[bucket];
      v2 = v1 & ~(1 << bitIndex);
      if (unsafe.compareAndSwapInt(bitset, byteOffset(bucket), v1, v2)) {
        return;
      }
    }
  }

  public void flip(int bitIndex) {
    int bucket = getBucket(bitIndex);
    if (bucket >= bitset.length) {
      throw new IndexOutOfBoundsException();
    }

    int v1, v2;
    for (;;) {
      v1 = bitset[bucket];
      v2 = v1 ^ (1 << bitIndex);
      if (unsafe.compareAndSwapInt(bitset, byteOffset(bucket), v1, v2)) {
        return;
      }
    }
  }

  public void set(int bitIndex) {
    int bucket = getBucket(bitIndex);
    if (bucket >= bitset.length) {
      throw new IndexOutOfBoundsException();
    }

    int v1, v2;
    for (;;) {
      v1 = bitset[bucket];
      v2 = v1 | (1 << bitIndex);
      if (unsafe.compareAndSwapInt(bitset, byteOffset(bucket), v1, v2)) {
        return;
      }
    }
  }

  public boolean get(int bitIndex) {
    int bucket = getBucket(bitIndex);
    if (bucket >= bitset.length) {
      throw new IndexOutOfBoundsException();
    }

    int v = unsafe.getIntVolatile(bitset, byteOffset(bucket));
    return (v & (1 << bitIndex)) != 0;
  }
}
