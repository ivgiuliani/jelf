package com.github.kratorius.jefs;

import com.github.kratorius.jefs.internal.BitUtils;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nonnull;

/**
 * A single producer-single consumer lock free queue.
 *
 * This class is thread-safe only when a single consumer and a single
 * producer are adding/removing data.
 *
 * Multiple consumers and multiple producers are not supported and will
 * cause incorrect results.
 *
 * This class is based off a talk by Martin Thompson:
 * "Lock free Algorithms for Ultimate Performance"
 * (http://www.infoq.com/presentations/Lock-Free-Algorithms)
 *
 * @param <T>  type of objects that can be added to this queue.
 */
public class SPSCQueue<T> implements Queue<T> {
  private final T[] buffer;
  private final int mask;

  private final AtomicLong head = new PaddedAtomicLong(0);
  private final AtomicLong tail = new PaddedAtomicLong(0);

  static class PaddedAtomicLong extends AtomicLong {
    // Unused in practice, but here to provide padding so we get better cache alignment.
    @SuppressWarnings("unused")
    public volatile long p0 = 0L, p1 = 1L, p2 = 2L, p3 = 3L,
                         p4 = 4L, p5 = 5L, p6 = 6L, p7 = 7L;

    PaddedAtomicLong(long initialValue) {
      super(initialValue);
    }
  }

  /**
   * Creates a new single-producer/single-consumer queue.
   *
   * @param capacity  the suggested capacity of the queue; the actual queue size will
   *                  be the next (positive) power of two. To get the actual capacity
   *                  of the queue, use {@link #actualCapacity()}
   */
  @SuppressWarnings("unchecked")
  public SPSCQueue(final int capacity) {
    int actualSize = BitUtils.roundToNextPowerOfTwo(capacity);
    buffer = (T[]) new Object[actualSize];

    // use a mask to access actual buffer items, this works because
    // the buffer is always a power of 2 and using idx & mask is equivalent
    // of length % idx but way more efficient, i.e: i % 2^k == i & (2^k - 1)
    mask = actualSize - 1;
  }

  /**
   * Returns the actual capacity of the queue.
   *
   * Because we round up the queue bound size to the next power of two,
   * the actual capacity of the queue might be larger than what exactly
   * specified.
   *
   * @return the effective capacity of the queue
   */
  public int actualCapacity() {
    return buffer.length;
  }

  @Override
  public int size() {
    return (int) (tail.get() - head.get());
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public boolean contains(Object o) {
    if (o == null) {
      return false;
    }

    final long end = tail.get();
    for (long i = head.get(); i < end; i++) {
      if (o.equals(buffer[(int)i & mask])) {
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean add(T t) {
    if (!offer(t)) {
      throw new IllegalStateException("full queue");
    }
    return true;
  }

  @Override
  public boolean containsAll(@Nonnull Collection<?> objects) {
    for (Object o : objects) {
      if (!contains(o)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public boolean addAll(@Nonnull Collection<? extends T> items) {
    for (T item : items) {
      add(item);
    }
    return true;
  }

  public void clear() {
    final long head = this.head.getAndSet(0);
    final long tail = this.tail.getAndSet(0);

    // remove any reference to old objects so they can be garbage collected
    for (long i = head; i <= tail; i++) {
      buffer[(int)i & mask] = null;
    }
  }

  @Override
  public boolean offer(final T t) {
    if (t == null) {
      throw new IllegalStateException("queue doesn't support null items");
    }

    final long currentTail = tail.get();
    final long wrapPoint = currentTail - buffer.length;
    if (head.get() <= wrapPoint) {
      return false;
    }

    // we can use lazySet because there's only a single producer
    buffer[(int)currentTail & mask] = t;
    tail.lazySet(currentTail + 1);

    return true;
  }

  @Override
  public T remove() {
    final T t = poll();
    if (t == null) {
      throw new NoSuchElementException("empty queue");
    }

    return t;
  }

  @Override
  public T poll() throws NoSuchElementException {
    final long currentHead = head.get();
    if (currentHead >= tail.get()) {
      // empty queue
      return null;
    }

    final int index = (int)currentHead & mask;
    final T t = buffer[index];
    buffer[index] = null;
    head.lazySet(currentHead + 1);

    return t;
  }

  @Override
  public T element() {
    final T t = peek();
    if (t == null) {
      throw new NoSuchElementException("empty queue");
    }
    return t;
  }

  @Override
  public T peek() {
    return buffer[(int)head.get() & mask];
  }

  @Override @Nonnull
  public Iterator<T> iterator() {
    throw new UnsupportedOperationException();
  }

  @Override @Nonnull
  public Object[] toArray() {
    throw new UnsupportedOperationException();
  }

  @Override @Nonnull
  public <A> A[] toArray(@Nonnull A[] ts) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAll(@Nonnull Collection<?> objects) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean retainAll(@Nonnull Collection<?> objects) {
    throw new UnsupportedOperationException();
  }
}
