/**
 * Copyright (c) 2001-2019 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://robocode.sourceforge.io/license/epl-v10.html
 */
package net.sf.robocode.async;

import javax.swing.SwingUtilities;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A simplified version of JavaScript Promise counterpart for async programming of Swing programs
 *
 * @author Xor
 */
public final class Promise {
	private static final Runnable RESOLVED = new Runnable() {
		@Override
		public void run() {
			throw new IllegalStateException("Re-resolve");
		}
	};

	private final AtomicReference<Runnable> state = new AtomicReference<Runnable>();

	public Promise(ResolveConsumer resolveConsumer) {
		Runnable resolve = new Runnable() {
			@Override
			public void run() {
				Runnable s = state.getAndSet(RESOLVED);
				if (s != null) {
					SwingUtilities.invokeLater(s);
				}
			}
		};

		resolveConsumer.accept(resolve);
	}

	public Promise then(final Runnable then) {
		return new Promise(new ResolveConsumer() {
			@Override
			public void accept(final Runnable resolve) {
				setThen(composeRunnable(then, resolve));
			}
		});
	}

	public Promise then(final PromiseSupplier then) {
		return new Promise(new ResolveConsumer() {
			@Override
			public void accept(final Runnable resolve) {
				setThen(composeRunnable(then, resolve));
			}
		});
	}

	public Promise delay(final int millis) {
		return then(new PromiseSupplier() {
			@Override
			public Promise get() {
				return Promise.delayed(millis);
			}
		});
	}

	private void setThen(Runnable runnable) {
		Runnable s;
		do {
			s = state.get();
			if (s == RESOLVED) {
				SwingUtilities.invokeLater(runnable);
				return;
			} else {
				if (state.compareAndSet(s, composeRunnable(s, runnable))) {
					return;
				}
			}
		} while (true);
	}

	private Runnable composeRunnable(final Runnable a, final Runnable b) {
		if (a == null) return b;

		return new Runnable() {
			@Override
			public void run() {
				a.run();
				b.run();
			}
		};
	}

	private Runnable composeRunnable(final PromiseSupplier a, final Runnable b) {
		if (a == null) return b;

		return new Runnable() {
			@Override
			public void run() {
				a.get().then(b);
			}
		};
	}

	public static Promise resolved() {
		return new Promise(new ResolveConsumer() {
			@Override
			public void accept(Runnable resolve) {
				resolve.run();
			}
		});
	}

	public static Promise delayed(final int millis) {
		return Promise.fromSync(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(millis);
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			}
		});
	}

	public static Promise fromSync(final Runnable runnable) {
		return new Promise(new ResolveConsumer() {
			@Override
			public void accept(final Runnable resolve) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						runnable.run();
						resolve.run();
					}
				}).start();
			}
		});
	}
}
