package com.github.kratorius.jefs;

import sun.misc.Unsafe;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a last-in-first-out (LIFO) unbounded stack of objects.
 * The usual {@code push} and {@code pop} operations are provided, as well as
 * a {@code peek} method to peek at the top of the stack.
 *
 * @param <E> the type of elements held in this collection
 */
public class LFStack<E> extends NotSafe {
  private static final Unsafe unsafe = getUnsafe();
  private volatile Node<E> head = null;
  private long headOffset;
  private AtomicInteger itemCount = new AtomicInteger();

  static class Node<E> {
    // include padding so that the node class uses 64 bytes, enough to fill
    // a whole cache line and avoid false sharing
    @SuppressWarnings("UnusedDeclaration")
    long p0, p1, p2, p3, p4, p5 = 6;

    final E val;
    volatile Node<E> next;

    public Node(E val) {
      this.val = val;
    }
  }

  /**
   * Creates an empty stack.
   */
  public LFStack() {
    try {
      headOffset = unsafe.objectFieldOffset(this.getClass().getDeclaredField("head"));
    } catch (NoSuchFieldException e) {
      throw new RuntimeException();
    }
  }

  public void clear() {
    itemCount.lazySet(0);
    head = null;
  }

  /**
   * Tests if the stack is empty.
   * @return {@code true} if and only if this stack contains no items;
   *         {@code false} otherwise
   */
  public boolean empty() {
    /* '==' is atomic for reference variables as per documentation:
     * "Reads and writes are atomic for reference variables and for most
     * primitive variables (all types except long and double).
     * (http://docs.oracle.com/javase/tutorial/essential/concurrency/atomic.html)
     */
    return head == null;
  }

  /**
   * Looks at the object at the top of the stack without removing it.
   * @return the object at the top of this stack
   * @throws java.util.NoSuchElementException if the stack is empty
   */
  public E peek() throws NoSuchElementException {
    final Node<E> pop;
    if ((pop = head) == null) {
      throw new NoSuchElementException();
    }

    return pop.val;
  }

  /**
   * Removes the object at the top of the stack and returns that object as the
   * value of this function.
   * @return the object at the top of the stack
   * @throws NoSuchElementException if the stack is empty
   */
  public E pop() throws NoSuchElementException {
    final E pop = remove();
    if (pop == null) {
      throw new NoSuchElementException();
    }
    return pop;
  }

  /**
   * Removes the object at the top of the stack and returns that object as the
   * value of this function.
   * @return the object at the top of the stack or null if the stack is empty
   */
  public E remove() {
    Node<E> pop, newHead;

    do {
      if ((pop = head) == null) {
        return null;
      }
      newHead = pop.next;
    } while (!unsafe.compareAndSwapObject(this, headOffset, pop, newHead));

    itemCount.decrementAndGet();
    return pop.val;
  }

  /**
   * Pushes an item onto the top of the stack.
   * @param item the item to be pushed onto this stack.
   */
  public void push(E item) {
    if (item == null) {
      throw new IllegalArgumentException();
    }

    final Node<E> node = new Node<>(item);
    do {
      node.next = head;
    } while (!unsafe.compareAndSwapObject(this, headOffset, node.next, node));

    itemCount.incrementAndGet();
  }

  /**
   * Pushes an item onto the top of the stack.
   * @param item the item to be pushed onto this stack.
   * @return {@code true}
   */
  public boolean add(E item) {
    push(item);
    return true;
  }

  /**
   * Counts the number of elements currently in the stack.
   * @return the number of elements in the stack.
   */
  public int size() {
    return itemCount.get();
  }
}
