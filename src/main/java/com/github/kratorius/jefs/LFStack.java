package com.github.kratorius.jefs;

import sun.misc.Unsafe;

import java.util.NoSuchElementException;

public class LFStack<E> extends NotSafe {
  private final Unsafe unsafe;
  private Node<E> head = null;
  private long offset;

  static class Node<E> {
    Node<E> next;
    E val;
  }

  public LFStack() {
    unsafe = getUnsafe();
    try {
      offset = unsafe.objectFieldOffset(this.getClass().getDeclaredField("head"));
    } catch (NoSuchFieldException e) {
      throw new RuntimeException();
    }
  }

  public boolean empty() {
    /* '==' is atomic for reference variables as per documentation:
     * "Reads and writes are atomic for reference variables and for most
     * primitive variables (all types except long and double).
     */
    return head == null;
  }

  public E peek() {
    Node<E> pop;

    for (;;) {
      pop = head;
      if (unsafe.compareAndSwapObject(this, offset, pop, pop)) {
        if (pop == null) {
          throw new NoSuchElementException();
        }
        return pop.val;
      }
    }
  }

  public E pop() {
    Node<E> pop, newHead;

    for (;;) {
      if ((pop = head) == null) {
        throw new NoSuchElementException();
      }
      newHead = head.next;
      if (unsafe.compareAndSwapObject(this, offset, pop, newHead)) {
        return pop.val;
      }
    }
  }

  public void push(E item) {
    Node<E> node = new Node<>();
    node.val = item;

    for (;;) {
      node.next = head;
      if (unsafe.compareAndSwapObject(this, offset, node.next, node)) {
        break;
      }
    }
  }
}
