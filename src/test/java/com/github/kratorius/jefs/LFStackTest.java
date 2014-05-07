package com.github.kratorius.jefs;

import org.junit.Test;

import java.util.ArrayList;
import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LFStackTest {
  class FixedValuePusherThread<E> implements Runnable {
    final int max;
    final LFStack<E> stack;
    final E value;

    public FixedValuePusherThread(LFStack<E> stack, int max, E value) {
      this.max = max;
      this.stack = stack;
      this.value = value;
    }

    @Override
    public void run() {
      for (int i = 0; i < max; i++) {
        stack.push(value);
      }
    }
  }

  class PopperThread<E> implements Runnable {
    final int max;
    final LFStack<E> stack;
    volatile Exception exception = null;

    public PopperThread(LFStack<E> stack, int max) {
      this.stack = stack;
      this.max = max;
    }

    @Override
    public void run() {
      for (int i = 0; i < max; i++) {
        try {
          stack.pop();
        } catch (Exception e) {
          exception = e;
          return;
        }
      }
    }
  }

  @Test
  public void testSingleThread() {
    LFStack<Integer> stack = new LFStack<>();
    stack.push(1);
    stack.push(2);
    stack.push(3);
    stack.push(4);

    assertFalse(stack.empty());
    assertEquals(4, (int) stack.peek());
    assertEquals(4, (int) stack.pop());
    assertEquals(3, (int) stack.peek());
    assertEquals(3, (int) stack.pop());
    assertEquals(2, (int) stack.peek());
    assertEquals(2, (int) stack.pop());
    assertEquals(1, (int) stack.peek());
    assertEquals(1, (int) stack.pop());
    assertTrue(stack.empty());
  }

  @Test(expected = NoSuchElementException.class)
  public void testEmptyStack_peek() {
    LFStack<Integer> stack = new LFStack<>();
    stack.peek();
  }

  @Test(expected = NoSuchElementException.class)
  public void testEmptyStack_pop() {
    LFStack<Integer> stack = new LFStack<>();
    stack.pop();
  }

  @Test
  public void testPush_twoThreads() throws InterruptedException {
    LFStack<Integer> stack = new LFStack<>();

    Thread t1 = new Thread(new FixedValuePusherThread<>(stack, 1000000, 42));
    Thread t2 = new Thread(new FixedValuePusherThread<>(stack, 1000000, 42));

    t1.start();
    t2.start();
    t1.join();
    t2.join();

    assertFalse(stack.empty());
    assertEquals(2000000, stack.size());
    for (int i = 0; i < 2000000; i++) {
      stack.pop();
    }
    assertTrue(stack.empty());
  }

  @Test
  public void testPush_asManyThreadsAsCores() throws InterruptedException {
    int logicalCores = Runtime.getRuntime().availableProcessors();
    ArrayList<Thread> threads = new ArrayList<>(logicalCores);
    LFStack<Integer> stack = new LFStack<>();

    for (int i = 0; i < logicalCores; i++) {
      threads.add(new Thread(new FixedValuePusherThread<>(stack, 1000000, 42)));
    }
    for (Thread t : threads) {
      t.start();
    }
    for (Thread t : threads) {
      t.join();
    }

    assertFalse(stack.empty());
    assertEquals(logicalCores * 1000000, stack.size());
    for (int i = 0; i < logicalCores * 1000000; i++) {
      stack.pop();
    }
    assertTrue(stack.empty());
  }

  @Test
  public void testPush_moreThreadsThanCores() throws InterruptedException {
    int logicalCores = Runtime.getRuntime().availableProcessors() + 1;
    ArrayList<Thread> threads = new ArrayList<>(logicalCores);
    LFStack<Integer> stack = new LFStack<>();

    for (int i = 0; i < logicalCores; i++) {
      threads.add(new Thread(new FixedValuePusherThread<>(stack, 1000000, 42)));
    }
    for (Thread t : threads) {
      t.start();
    }
    for (Thread t : threads) {
      t.join();
    }

    assertFalse(stack.empty());
    assertEquals(logicalCores * 1000000, stack.size());
    for (int i = 0; i < logicalCores * 1000000; i++) {
      stack.pop();
    }
    assertTrue(stack.empty());
  }

  @Test
  public void testPop_twoThreads() throws Exception {
    LFStack<Integer> stack = new LFStack<>();
    for (int i = 0; i < 2000000; i++) {
      stack.push(i);
    }

    PopperThread r1 = new PopperThread<>(stack, 1000000);
    PopperThread r2 = new PopperThread<>(stack, 1000000);
    Thread t1 = new Thread(r1);
    Thread t2 = new Thread(r2);

    t1.start();
    t2.start();
    t1.join();
    t2.join();

    if (r1.exception != null) {
      throw r1.exception;
    }
    if (r2.exception != null) {
      throw r2.exception;
    }

    assertTrue(stack.empty());
  }

  @Test
  public void testPop_asManyThreadsAsCores() throws Exception {
    int logicalCores = Runtime.getRuntime().availableProcessors();
    ArrayList<PopperThread<Integer>> poppers = new ArrayList<>(logicalCores);
    ArrayList<Thread> threads = new ArrayList<>(logicalCores);
    LFStack<Integer> stack = new LFStack<>();

    for (int i = 0; i < logicalCores * 1000000; i++) {
      stack.push(i);
    }

    for (int i = 0; i < logicalCores; i++) {
      PopperThread<Integer> popper = new PopperThread<>(stack, 1000000);
      poppers.add(popper);
      threads.add(new Thread(popper));
    }
    for (Thread t : threads) {
      t.start();
    }
    for (Thread t : threads) {
      t.join();
    }

    for (PopperThread p : poppers) {
      if (p.exception != null) {
        throw p.exception;
      }
    }

    assertTrue(stack.empty());
  }

  @Test
  public void testPop_moreThreadsThanCores() throws Exception {
    int logicalCores = Runtime.getRuntime().availableProcessors() + 1;
    ArrayList<PopperThread<Integer>> poppers = new ArrayList<>(logicalCores);
    ArrayList<Thread> threads = new ArrayList<>(logicalCores);
    LFStack<Integer> stack = new LFStack<>();

    for (int i = 0; i < logicalCores * 1000000; i++) {
      stack.push(i);
    }

    for (int i = 0; i < logicalCores; i++) {
      PopperThread<Integer> popper = new PopperThread<>(stack, 1000000);
      poppers.add(popper);
      threads.add(new Thread(popper));
    }
    for (Thread t : threads) {
      t.start();
    }
    for (Thread t : threads) {
      t.join();
    }

    for (PopperThread p : poppers) {
      if (p.exception != null) {
        throw p.exception;
      }
    }

    assertTrue(stack.empty());
  }
}
