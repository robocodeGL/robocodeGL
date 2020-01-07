/**
 * Copyright (c) 2001-2019 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://robocode.sourceforge.io/license/epl-v10.html
 */
package net.sf.robocode.ui.battle;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Similar to java.util.concurrent.ArrayBlockingQueue, but provides access to inner conditions,
 * as well as resizing on the fly (blocks other operations)
 *
 * @author Xor (original)
 */
public final class MyBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {
	private final ReentrantLock lock = new ReentrantLock();
	private final Condition notEmpty = lock.newCondition();
	private final Condition notFull = lock.newCondition();

	private Object[] items;

	private int takeIndex;
	private int count;

	public MyBlockingQueue(int capacity) {
		items = new Object[capacity];
		takeIndex = 0;
		count = 0;
	}

	@SuppressWarnings("unchecked")
	private E itemAt(int i) {
		return (E) items[i];
	}

	private E dequeueImpl() {
		final Object[] items = this.items;
		final E ret = itemAt(takeIndex);
		items[takeIndex] = null;
		++takeIndex;
		--count;
		if (takeIndex == items.length) {
			takeIndex = 0;
		}
		notFull.signal();
		return ret;
	}

	private void enqueueImpl(E e) {
		final Object[] items = this.items;
		final int cap = items.length;

		int putIndex = takeIndex + count;
		if (putIndex >= cap) {
			putIndex -= cap;
		}
		items[putIndex] = e;
		++count;
		notEmpty.signal();
	}

	@Override
	public boolean offer(E e) {
		if (e == null)
			throw new NullPointerException();

		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			if (count < items.length) {
				enqueueImpl(e);

				return true;
			} else {
				return false;
			}
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void put(E e) throws InterruptedException {
		if (e == null)
			throw new NullPointerException();

		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			while (count == items.length) {
				notFull.await();
			}

			enqueueImpl(e);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * @param supplier supply a value to be put when notFull. supply null to cancel.
	 */
	public boolean putWithSupplier(MySupplier<E> supplier) throws InterruptedException {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			while (count == items.length) {
				notFull.await();
			}

			E e = supplier.get();

			if (e != null) {
				enqueueImpl(e);
				return true;
			} else {
				return false;
			}
		} finally {
			lock.unlock();
		}
	}

	@Override
	public E peek() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return itemAt(takeIndex);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public E poll() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			if (count > 0) {
				return dequeueImpl();
			} else {
				return null;
			}
		} finally {
			lock.unlock();
		}
	}

	@Override
	public E take() throws InterruptedException {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			while (count == 0) {
				notEmpty.await();
			}

			return dequeueImpl();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public int size() {
		return count;
	}

	@Override
	public int remainingCapacity() {
		final ReentrantLock lock = this.lock;
		lock.lock(); // locking necessary to support live resize
		try {
			return items.length - count;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void clear() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			int k = count;
			count = 0;

			clearCircular(items, takeIndex, k);

			while (k > 0 && lock.hasWaiters(notFull)) {
				--k;
				notFull.signal();
			}
		} finally {
			lock.unlock();
		}
	}

	private static void clearCircular(Object[] items, int takeIndex, int count) {
		int cap = items.length;
		int putIndex = takeIndex + count;
		if (putIndex > cap) {
			for (int i = takeIndex; i < cap; ++i) {
				items[i] = null;
			}
			putIndex -= cap;
			for (int i = 0; i < putIndex; ++i) {
				items[i] = null;
			}
		} else {
			for (int i = takeIndex; i < putIndex; ++i) {
				items[i] = null;
			}
		}
	}

	@Override
	public Iterator<E> iterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int drainTo(Collection<? super E> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int drainTo(Collection<? super E> c, int maxElements) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
		throw new UnsupportedOperationException();
	}

	@Override
	public E poll(long timeout, TimeUnit unit) throws InterruptedException {
		throw new UnsupportedOperationException();
	}
}
