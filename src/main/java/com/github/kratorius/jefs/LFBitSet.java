package com.github.kratorius.jefs;

import sun.misc.Unsafe;

/**
 * Represents a vector of a fixed number of bits.
 * This class differs from the normal {@link java.util.BitSet} class in that we only allow
 * a fixed number of bits.
 */
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

  /**
   * Creates a new bit set whose size is large enough to explicitely represent bits
   * with indices in the range {@code 0} through {@code nbits - 1}.
   * @param nbits the initial size of the bit set
   */
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

  /**
   * Sets all the bits in this bit set to {@code false}.
   */
  public void clear() {
    this.bitset = new long[getBucket(nbits) + 1];
  }

  /**
   * Sets the bit specified by the index to false.
   * @param bitIndex the index of the bit to be cleared
   */
  public void clear(int bitIndex) {
    int bucket = getBucket(bitIndex);
    if (bucket < 0 || bucket >= bitset.length) {
      throw new IndexOutOfBoundsException();
    }

    long v1, v2;
    do {
      v1 = bitset[bucket];
      v2 = v1 & ~(1L << bitIndex);
    } while (!unsafe.compareAndSwapLong(bitset, byteOffset(bucket), v1, v2));
  }

  /**
   * Sets the bit at the specified index to the complement of its current value.
   * @param bitIndex the index of the bit to flip
   */
  public void flip(int bitIndex) {
    int bucket = getBucket(bitIndex);
    if (bucket < 0 || bucket >= bitset.length) {
      throw new IndexOutOfBoundsException();
    }

    long v1, v2;
    do {
      v1 = bitset[bucket];
      v2 = v1 ^ (1L << bitIndex);
    } while (!unsafe.compareAndSwapLong(bitset, byteOffset(bucket), v1, v2));
  }

  /**
   * Sets the bit at the specified index to true.
   * @param bitIndex a bit index
   */
  public void set(int bitIndex) {
    int bucket = getBucket(bitIndex);
    if (bucket < 0 || bucket >= bitset.length) {
      throw new IndexOutOfBoundsException();
    }

    long v1, v2;
    do {
      v1 = bitset[bucket];
      v2 = v1 | (1L << bitIndex);
    } while (!unsafe.compareAndSwapLong(bitset, byteOffset(bucket), v1, v2));
  }

  /**
   * Sets the bit at the specified index to the specified value.
   * @param bitIndex a bit index
   * @param value    a boolean value to set
   */
  public void set(int bitIndex, boolean value) {
    if (value) {
      set(bitIndex);
    } else {
      clear(bitIndex);
    }
  }

  /**
   * Returns the value of the bit with the specified index.
   * The value is true if the bit with the index {@code bitIndex} is currently set
   * in this BitSet; otherwise, the result is {@code false}.
   * @param bitIndex a bit index
   * @return the value of the bit with the specified index
   * @throws java.lang.IndexOutOfBoundsException if the specified index is negative or
   *         exceeds the bit set length
   */
  public boolean get(int bitIndex) {
    int bucket = getBucket(bitIndex);
    if (bucket < 0 || bucket >= bitset.length) {
      throw new IndexOutOfBoundsException();
    }

    long v = unsafe.getLongVolatile(bitset, byteOffset(bucket));
    return (v & (1L << bitIndex)) != 0;
  }
}
