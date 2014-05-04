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
        if (pop.val == null) {
          throw new NoSuchElementException();
        }
        return pop.val;
      }
    }
  }

  public E pop() {
    Node<E> pop, newHead;

    for (;;) {
      pop = head;
      newHead = head.next;
      if (unsafe.compareAndSwapObject(this, offset, pop, newHead)) {
        if (pop.val == null) {
          throw new NoSuchElementException();
        }
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

  public static void main(String[] args) {
    LFStack<Integer> stack = new LFStack<>();
    stack.push(1);
    stack.push(2);
    stack.push(3);
    stack.push(4);

    System.out.println(stack.empty());
    System.out.println(stack.peek());
    System.out.println(stack.pop());
    System.out.println(stack.peek());
    System.out.println(stack.pop());
    System.out.println(stack.peek());
    System.out.println(stack.pop());
    System.out.println(stack.peek());
    System.out.println(stack.pop());
    System.out.println(stack.empty());
  }
}
