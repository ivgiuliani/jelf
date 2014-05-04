package com.github.kratorius.jefs;

import org.junit.Test;

import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LFStackTest {
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
}
