package com.github.kratorius.jefs.internal;

public class BitUtils {
  /**
   * Returns the closest power of two for the given value.
   *
   * @param value original input value
   * @return the closest power of two for the given value
   */
  public static int roundToNextPowerOfTwo(int value) {
    if (value < 0) {
      throw new UnsupportedOperationException();
    }

    // Alternatively this works too but our implementation seems to be more
    // efficent in terms of CPU operations executed (numberOfLeadingZeros does
    // exactly this, plus other things). However, numberOfLeadingZeros will
    // return '1' for value=0 which is arguably more correct:
    //
    // return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));

    value--;
    value |= value >>> 1;
    value |= value >>> 2;
    value |= value >>> 4;
    value |= value >>> 8;
    value |= value >>> 16;

    return ++value;
  }
}
