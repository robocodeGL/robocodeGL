/**
 * Copyright (c) 2001-2019 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://robocode.sourceforge.io/license/epl-v10.html
 */
package net.sf.robocode.ui.battle;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

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

	private int pollIndex;
	private int count;

	public MyBlockingQueue(int capacity) {
		items = new Object[capacity];
		pollIndex = 0;
		count = 0;
	}

	@SuppressWarnings("unchecked")
	private E itemAt(int i) {
		return (E) items[i];
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

	private E dequeueImpl() {
		final Object[] items = this.items;
		final E ret = itemAt(pollIndex);
		++pollIndex;
		--count;
		if (pollIndex == items.length) {
			pollIndex = 0;
		}
		notFull.signal();
		return ret;
	}

	private void enqueueImpl(E e) {
		final Object[] items = this.items;
		final int cap = items.length;

		int putIndex = pollIndex + count;
		if (putIndex > cap) {
			putIndex -= cap;
		}
		items[putIndex] = e;
		++count;
		notEmpty.signal();
	}

	@Override
	public E peek() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return itemAt(pollIndex);
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
	public Iterator<E> iterator() {
		throw new NotImplementedException();
	}

	@Override
	public int size() {
		return count;
	}

	@Override
	public void put(E e) throws InterruptedException {
		throw new NotImplementedException();
	}

	@Override
	public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
		throw new NotImplementedException();
	}

	@Override
	public E take() throws InterruptedException {
		throw new NotImplementedException();
	}

	@Override
	public E poll(long timeout, TimeUnit unit) throws InterruptedException {
		throw new NotImplementedException();
	}

	@Override
	public int remainingCapacity() {
		throw new NotImplementedException();
	}

	@Override
	public int drainTo(Collection<? super E> c) {
		throw new NotImplementedException();
	}

	@Override
	public int drainTo(Collection<? super E> c, int maxElements) {
		throw new NotImplementedException();
	}
}
