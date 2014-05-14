package com.github.kratorius.jefs;

import org.junit.Test;

import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SPSCQueueTest {
  @Test
  public void testAdd() {
    SPSCQueue<Integer> q = new SPSCQueue<>(100);
    for (int i = 0; i < q.actualCapacity(); i++) {
      assertTrue(q.add(i));
    }

    try {
      q.add(123);
      fail("more items than allowed added");
    } catch (IllegalStateException ex) {
      // expected
    }

    assertEquals(q.actualCapacity(), q.size());
  }

  @Test
  public void testPoll() {
    SPSCQueue<Integer> q = new SPSCQueue<>(100);
    for (int i = 0; i < q.actualCapacity(); i++) {
      q.add(i);
    }

    for (int i = 0; i < 100; i++) {
      assertEquals(i, (int)q.poll());
    }
  }

  @Test
  public void testRemove() {
    SPSCQueue<Integer> q = new SPSCQueue<>(100);
    for (int i = 0; i < q.actualCapacity(); i++) {
      q.add(i);
    }

    for (int i = 0; i < q.actualCapacity(); i++) {
      assertEquals(i, (int)q.remove());
    }

    try {
      q.remove();
      fail("removed item from an empty queue");
    } catch (NoSuchElementException ex) {
      // expected
    }
    assertEquals(0, q.size());
  }

  @Test
  public void testPeek() {
    SPSCQueue<Integer> q = new SPSCQueue<>(100);

    assertNull(q.peek());
    for (int i = 0; i < q.actualCapacity(); i++) {
      q.add(i);
    }
    for (int i = 0; i < q.actualCapacity(); i++) {
      assertEquals(i, (int)q.peek());
      q.remove();
    }
  }

  @Test
  public void testElement() {
    SPSCQueue<Integer> q = new SPSCQueue<>(100);

    try {
      q.element();
      fail("element() on an empty queue");
    } catch (NoSuchElementException ex) {
      // expected
    }

    for (int i = 0; i < q.actualCapacity(); i++) {
      q.add(i);
    }
    for (int i = 0; i < q.actualCapacity(); i++) {
      assertEquals(i, (int)q.element());
      q.remove();
    }
  }

  @Test
  public void testSize() {
    SPSCQueue<Integer> q = new SPSCQueue<>(100);
    for (int i = 0; i < q.actualCapacity(); i++) {
      assertTrue(q.add(i));
      assertEquals(i + 1, q.size());
    }
  }

}
