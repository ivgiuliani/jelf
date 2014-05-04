package com.github.kratorius.jefs;

import sun.misc.Unsafe;

public class LFBitSet extends NotSafe {
  private static Unsafe unsafe = getUnsafe();
  private static final int base = unsafe.arrayBaseOffset(long[].class);
  private static final int shift;

  static {
    int scale = Integer.numberOfLeadingZeros(unsafe.arrayIndexScale(long[].class));
    shift = 31 - scale;
  }

  // The volatile is needed because clear() creates a whole new array
  private volatile long[] bitset;
  private final int nbits;

  public LFBitSet(int nbits) {
    if (nbits < 0) {
      throw new IllegalArgumentException();
    }

    this.nbits = nbits;
    this.bitset = new long[getBucket(nbits) + 1];
  }

  private long byteOffset(int idx) {
    return ((long) idx << shift) + base;
  }

  private int getBucket(int bit) {
    //noinspection NumericOverflow
    return ((bit - 1) >> 6) + 1;
  }

  public void clear() {
    this.bitset = new long[getBucket(nbits) + 1];
  }

  public void clear(int bitIndex) {
    int bucket = getBucket(bitIndex);
    if (bucket >= bitset.length) {
      throw new IndexOutOfBoundsException();
    }

    long v1, v2;
    for (;;) {
      v1 = bitset[bucket];
      v2 = v1 & ~(1L << bitIndex);
      if (unsafe.compareAndSwapLong(bitset, byteOffset(bucket), v1, v2)) {
        return;
      }
    }
  }

  public void flip(int bitIndex) {
    int bucket = getBucket(bitIndex);
    if (bucket >= bitset.length) {
      throw new IndexOutOfBoundsException();
    }

    long v1, v2;
    for (;;) {
      v1 = bitset[bucket];
      v2 = v1 ^ (1L << bitIndex);
      if (unsafe.compareAndSwapLong(bitset, byteOffset(bucket), v1, v2)) {
        return;
      }
    }
  }

  public void set(int bitIndex) {
    int bucket = getBucket(bitIndex);
    if (bucket >= bitset.length) {
      throw new IndexOutOfBoundsException();
    }

    long v1, v2;
    for (;;) {
      v1 = bitset[bucket];
      v2 = v1 | (1L << bitIndex);
      if (unsafe.compareAndSwapLong(bitset, byteOffset(bucket), v1, v2)) {
        return;
      }
    }
  }

  public boolean get(int bitIndex) {
    int bucket = getBucket(bitIndex);
    if (bucket >= bitset.length) {
      throw new IndexOutOfBoundsException();
    }

    long v = unsafe.getLongVolatile(bitset, byteOffset(bucket));
    return (v & (1L << bitIndex)) != 0;
  }
}
