package com.github.kratorius.jefs;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LFBitSetTest {
  private static final int ONE_MB = 1024 * 1024;

  @Test
  public void testNewBitSetIsZero() {
    LFBitSet bs = new LFBitSet(ONE_MB);

    for (int i = 0; i < ONE_MB; i++) {
      assertFalse(bs.get(i));
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNegativeIndexingNotAllowed() {
    new LFBitSet(-1);
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void testOutOfBounds() {
    LFBitSet bs = new LFBitSet(ONE_MB);
    bs.get(ONE_MB + 1);
  }

  @Test
  public void testSetGet_singleThread() {
    LFBitSet bs = new LFBitSet(ONE_MB);

    for (int i = 0; i < ONE_MB; i++) {
      assertFalse(bs.get(i));
      bs.set(i);
      assertTrue(bs.get(i));
    }

    // make sure set(i) didn't change anything else
    for (int i = 0; i < ONE_MB; i++) {
      assertTrue(bs.get(i));
    }
  }

  @Test
  public void testFlip_singleThread() {
    LFBitSet bs = new LFBitSet(ONE_MB);

    for (int i = 0; i < ONE_MB; i++) {
      assertFalse(bs.get(i));
      bs.flip(i);
      assertTrue(bs.get(i));
    }
  }

  @Test
  public void testClear_singleThread() {
    LFBitSet bs = new LFBitSet(ONE_MB);

    for (int i = 0; i < ONE_MB; i++) {
      bs.set(i);
      assertTrue(bs.get(i));
    }

    for (int i = 0; i < ONE_MB; i++) {
      bs.clear(i);
      assertFalse(bs.get(i));
    }
  }

  @Test
  public void testClearWholeArray() {
    LFBitSet bs = new LFBitSet(ONE_MB);

    for (int i = 0; i < ONE_MB; i++) {
      bs.set(i);
      assertTrue(bs.get(i));
    }

    bs.clear();

    for (int i = 0; i < ONE_MB; i++) {
      assertFalse(bs.get(i));
    }
  }

  class FlipperRangeThread implements Runnable {
    public Exception exception;
    final LFBitSet bitSet;
    final int from;
    final int to;

    public FlipperRangeThread(LFBitSet bitSet, int from, int to) {
      this.bitSet = bitSet;
      this.from = from;
      this.to = to;
    }

    @Override
    public void run() {
      for (int i = from; i < to; i++) {
        try {
          bitSet.flip(i);
        } catch (Exception e) {
          exception = e;
          return;
        }
      }
    }
  }

  class FlipperThreadEven implements Runnable {
    public Exception exception;
    final LFBitSet bitSet;
    final int size;
    final boolean even;

    public FlipperThreadEven(LFBitSet bitSet, int size, boolean even) {
      this.bitSet = bitSet;
      this.size = size;
      this.even = even;
    }

    @Override
    public void run() {
      for (int i = 0; i < size; i++) {
        if ((even && ((i % 2) == 0)) || (!even && ((i % 2) != 0))) {
          try {
            bitSet.flip(i);
          } catch (Exception e) {
            exception = e;
          }
        }
      }
    }
  }

  @Test
  public void testFlip_twoThreads_noContention() throws InterruptedException {
    final int arraySize = ONE_MB * 256;
    final LFBitSet bs = new LFBitSet(arraySize);

    // first half of the array must be set to 1, the other to 0
    for (int i = 0; i < (arraySize / 2); i++) {
      bs.set(i);
    }

    // each thread will flip his own half
    FlipperRangeThread f1 = new FlipperRangeThread(bs, 0, (arraySize / 2));
    FlipperRangeThread f2 = new FlipperRangeThread(bs, (arraySize / 2), arraySize);
    Thread t1 = new Thread(f1);
    Thread t2 = new Thread(f2);

    t1.start();
    t2.start();
    t1.join();
    t2.join();

    assertNull(f1.exception);
    assertNull(f2.exception);

    for (int i = 0; i < (arraySize / 2); i++) {
      assertFalse(bs.get(i));
    }
    for (int i = (arraySize / 2); i < arraySize; i++) {
      assertTrue(bs.get(i));
    }
  }

  @Test
  public void testFlip_twoThreads_contention() throws InterruptedException {
    final int arraySize = ONE_MB * 256;
    final LFBitSet bs = new LFBitSet(arraySize);

    // every other item will be set to 1
    for (int i = 0; i < arraySize; i++) {
      if ((i % 2) == 0) {
        bs.set(i);
      }
    }

    // each thread will flip either even or odd positions
    FlipperThreadEven f1 = new FlipperThreadEven(bs, arraySize, true);
    FlipperThreadEven f2 = new FlipperThreadEven(bs, arraySize, false);
    Thread t1 = new Thread(f1);
    Thread t2 = new Thread(f2);

    t1.start();
    t2.start();
    t1.join();
    t2.join();

    // in the end all the items must be flipped
    for (int i = 0; i < arraySize; i++) {
      if ((i % 2) == 0) {
        assertFalse(bs.get(i));
      } else {
        assertTrue(bs.get(i));
      }
    }
  }

  class SetThread implements Runnable {
    private LFBitSet bitSet;
    private List<Integer> positions;

    public SetThread(LFBitSet bitSet, List<Integer> positions) {
      this.bitSet = bitSet;
      this.positions = positions;
    }

    @Override
    public void run() {
      for (int pos : positions) {
        bitSet.set(pos);
      }
    }
  }

  class ClearThread implements Runnable {
    private LFBitSet bitSet;
    private List<Integer> positions;

    public ClearThread(LFBitSet bitSet, List<Integer> positions) {
      this.bitSet = bitSet;
      this.positions = positions;
    }

    @Override
    public void run() {
      for (int pos : positions) {
        bitSet.clear(pos);
      }
    }
  }

  class FlipThread implements Runnable {
    private LFBitSet bitSet;
    private List<Integer> positions;

    public FlipThread(LFBitSet bitSet, List<Integer> positions) {
      this.bitSet = bitSet;
      this.positions = positions;
    }

    @Override
    public void run() {
      for (int pos : positions) {
        bitSet.flip(pos);
      }
    }
  }

  @Test
  public void testSet_asManyThreadsAsCores() throws InterruptedException {
    final int arraySize = ONE_MB * 32; // or the shuffle list won't fit in memory (of my machine)
    final LFBitSet bs = new LFBitSet(arraySize);
    int logicalCores = Runtime.getRuntime().availableProcessors();
    ArrayList<Thread> threads = new ArrayList<>(logicalCores);

    // change to something more memory-efficient so we can test larger bitsets
    List<Integer> items = new ArrayList<>(arraySize);
    for (int i = 0; i < arraySize; i++) {
      items.add(i);
    }
    Collections.shuffle(items);

    // each thread will have to set only a subset of this list
    int start = 0;
    int stop = arraySize / logicalCores;
    for (int i = 0; i < logicalCores; i++) {
      // add the remainder of items to the last thread
      if (i == (logicalCores - 1)) {
        stop += arraySize % logicalCores;
      }

      threads.add(new Thread(new SetThread(bs, items.subList(start, stop))));
      start = stop;
      stop += arraySize / logicalCores;
    }

    for (Thread t : threads) {
      t.start();
    }
    for (Thread t : threads) {
      t.join();
    }

    // in the end all the items must be set
    for (int i = 0; i < arraySize; i++) {
      assertTrue(bs.get(i));
    }
  }

  @Test
  public void testSet_moreThreadsThanCores() throws InterruptedException {
    final int arraySize = ONE_MB * 32; // or the shuffle list won't fit in memory (of my machine)
    final LFBitSet bs = new LFBitSet(arraySize);
    int logicalCores = Runtime.getRuntime().availableProcessors() + 1;
    ArrayList<Thread> threads = new ArrayList<>(logicalCores);

    // change to something more memory-efficient so we can test larger bitsets
    List<Integer> items = new ArrayList<>(arraySize);
    for (int i = 0; i < arraySize; i++) {
      items.add(i);
    }
    Collections.shuffle(items);

    // each thread will have to set only a subset of this list
    int start = 0;
    int stop = arraySize / logicalCores;
    for (int i = 0; i < logicalCores; i++) {
      // add the remainder of items to the last thread
      if (i == (logicalCores - 1)) {
        stop += arraySize % logicalCores;
      }

      threads.add(new Thread(new SetThread(bs, items.subList(start, stop))));
      start = stop;
      stop += arraySize / logicalCores;
    }

    for (Thread t : threads) {
      t.start();
    }
    for (Thread t : threads) {
      t.join();
    }

    // in the end all the items must be set
    for (int i = 0; i < arraySize; i++) {
      assertTrue(bs.get(i));
    }
  }

  @Test
  public void testClear_asManyThreadsAsCores() throws InterruptedException {
    final int arraySize = ONE_MB * 32; // or the shuffle list won't fit in memory (of my machine)
    final LFBitSet bs = new LFBitSet(arraySize);
    int logicalCores = Runtime.getRuntime().availableProcessors();
    ArrayList<Thread> threads = new ArrayList<>(logicalCores);

    // change to something more memory-efficient so we can test larger bitsets
    List<Integer> items = new ArrayList<>(arraySize);
    for (int i = 0; i < arraySize; i++) {
      bs.set(i);
      items.add(i);
    }
    Collections.shuffle(items);

    // each thread will have to set only a subset of this list
    int start = 0;
    int stop = arraySize / logicalCores;
    for (int i = 0; i < logicalCores; i++) {
      // add the remainder of items to the last thread
      if (i == (logicalCores - 1)) {
        stop += arraySize % logicalCores;
      }

      threads.add(new Thread(new ClearThread(bs, items.subList(start, stop))));
      start = stop;
      stop += arraySize / logicalCores;
    }

    for (Thread t : threads) {
      t.start();
    }
    for (Thread t : threads) {
      t.join();
    }

    // in the end all the items must be set
    for (int i = 0; i < arraySize; i++) {
      assertFalse(bs.get(i));
    }
  }

  @Test
  public void testClear_moreThreadsThanCores() throws InterruptedException {
    final int arraySize = ONE_MB * 32; // or the shuffle list won't fit in memory (of my machine)
    final LFBitSet bs = new LFBitSet(arraySize);
    int logicalCores = Runtime.getRuntime().availableProcessors() + 1;
    ArrayList<Thread> threads = new ArrayList<>(logicalCores);

    // change to something more memory-efficient so we can test larger bitsets
    List<Integer> items = new ArrayList<>(arraySize);
    for (int i = 0; i < arraySize; i++) {
      bs.set(i);
      items.add(i);
    }
    Collections.shuffle(items);

    // each thread will have to set only a subset of this list
    int start = 0;
    int stop = arraySize / logicalCores;
    for (int i = 0; i < logicalCores; i++) {
      // add the remainder of items to the last thread
      if (i == (logicalCores - 1)) {
        stop += arraySize % logicalCores;
      }

      threads.add(new Thread(new ClearThread(bs, items.subList(start, stop))));
      start = stop;
      stop += arraySize / logicalCores;
    }

    for (Thread t : threads) {
      t.start();
    }
    for (Thread t : threads) {
      t.join();
    }

    // in the end all the items must be set
    for (int i = 0; i < arraySize; i++) {
      assertFalse(bs.get(i));
    }
  }

  @Test
  public void testFlip_asManyThreadsAsCores() throws InterruptedException {
    final int arraySize = ONE_MB * 32; // or the shuffle list won't fit in memory (of my machine)
    final LFBitSet bs = new LFBitSet(arraySize);
    int logicalCores = Runtime.getRuntime().availableProcessors();
    ArrayList<Thread> threads = new ArrayList<>(logicalCores);

    // change to something more memory-efficient so we can test larger bitsets
    List<Integer> items = new ArrayList<>(arraySize);
    for (int i = 0; i < arraySize; i++) {
      items.add(i);
    }
    Collections.shuffle(items);

    // each thread will have to set only a subset of this list
    int start = 0;
    int stop = arraySize / logicalCores;
    for (int i = 0; i < logicalCores; i++) {
      // add the remainder of items to the last thread
      if (i == (logicalCores - 1)) {
        stop += arraySize % logicalCores;
      }

      threads.add(new Thread(new FlipThread(bs, items.subList(start, stop))));
      start = stop;
      stop += arraySize / logicalCores;
    }

    for (Thread t : threads) {
      t.start();
    }
    for (Thread t : threads) {
      t.join();
    }

    // in the end all the items must be set
    for (int i = 0; i < arraySize; i++) {
      assertTrue(bs.get(i));
    }
  }

  @Test
  public void testFlip_moreThreadsThanCores() throws InterruptedException {
    final int arraySize = ONE_MB * 32; // or the shuffle list won't fit in memory (of my machine)
    final LFBitSet bs = new LFBitSet(arraySize);
    int logicalCores = Runtime.getRuntime().availableProcessors() + 1;
    ArrayList<Thread> threads = new ArrayList<>(logicalCores);

    // change to something more memory-efficient so we can test larger bitsets
    List<Integer> items = new ArrayList<>(arraySize);
    for (int i = 0; i < arraySize; i++) {
      items.add(i);
    }
    Collections.shuffle(items);

    // each thread will have to set only a subset of this list
    int start = 0;
    int stop = arraySize / logicalCores;
    for (int i = 0; i < logicalCores; i++) {
      // add the remainder of items to the last thread
      if (i == (logicalCores - 1)) {
        stop += arraySize % logicalCores;
      }

      threads.add(new Thread(new FlipThread(bs, items.subList(start, stop))));
      start = stop;
      stop += arraySize / logicalCores;
    }

    for (Thread t : threads) {
      t.start();
    }
    for (Thread t : threads) {
      t.join();
    }

    // in the end all the items must be set
    for (int i = 0; i < arraySize; i++) {
      assertTrue(bs.get(i));
    }
  }

  static class MultiFlipThread implements Runnable {
    private final LFBitSet bitSet;
    private final int bsSize;
    private final int times;

    public MultiFlipThread(LFBitSet bitSet, int bsSize, int times) {
      this.bitSet = bitSet;
      this.bsSize = bsSize;
      this.times = times;
    }

    @Override
    public void run() {
      for (int count = 0; count < times; count++) {
        for (int i = 0; i < bsSize; i++) {
          bitSet.flip(i);
        }
      }
    }
  }

  @Test
  public void testFlip_multipleTimes_heavyContention() throws InterruptedException {
    final LFBitSet bs = new LFBitSet(64);
    int logicalCores = Runtime.getRuntime().availableProcessors();
    ArrayList<Thread> threads = new ArrayList<>(logicalCores);

    for (int i = 0; i < logicalCores; i++) {
      threads.add(new Thread(new MultiFlipThread(bs, 64, 100)));
    }

    for (Thread t : threads) {
      t.start();
    }

    for (Thread t : threads) {
      t.join();
    }

    for (int i = 0; i < 64; i++) {
      assertFalse(bs.get(i));
    }
  }
}
