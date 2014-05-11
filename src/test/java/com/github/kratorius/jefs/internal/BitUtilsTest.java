package com.github.kratorius.jefs.internal;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BitUtilsTest {
  @Test
  public void testPowerOfTwoCalculation() {
    int power = 2;

    // test for every possible input
    assertEquals(0, BitUtils.roundToNextPowerOfTwo(0));
    for (int i = 2; i < Integer.MAX_VALUE; i++) {
      assertEquals(power, BitUtils.roundToNextPowerOfTwo(i));

      if (power == i) {
        power <<= 1;
      }
    }
  }
}
