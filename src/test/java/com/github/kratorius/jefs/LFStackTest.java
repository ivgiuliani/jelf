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

    Thread t1 = new Thread(new FixedValuePusherThread<>(stack, 10000000, 42));
    Thread t2 = new Thread(new FixedValuePusherThread<>(stack, 10000000, 42));

    t1.start();
    t2.start();
    t1.join();
    t2.join();

    assertFalse(stack.empty());
    for (int i = 0; i < 20000000; i++) {
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
      threads.add(new Thread(new FixedValuePusherThread<>(stack, 10000000, 42)));
    }
    for (Thread t : threads) {
      t.start();
    }
    for (Thread t : threads) {
      t.join();
    }

    assertFalse(stack.empty());
    for (int i = 0; i < logicalCores * 10000000; i++) {
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
      threads.add(new Thread(new FixedValuePusherThread<>(stack, 10000000, 42)));
    }
    for (Thread t : threads) {
      t.start();
    }
    for (Thread t : threads) {
      t.join();
    }

    assertFalse(stack.empty());
    for (int i = 0; i < logicalCores * 10000000; i++) {
      stack.pop();
    }
    assertTrue(stack.empty());
  }
}
