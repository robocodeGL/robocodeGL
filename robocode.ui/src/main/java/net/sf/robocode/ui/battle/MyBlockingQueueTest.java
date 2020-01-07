package net.sf.robocode.ui.battle;

public final class MyBlockingQueueTest {
	private static final int CAP = 10;

	public static void main(String[] args) throws InterruptedException {
		final MyBlockingQueue<Integer> queue = new MyBlockingQueue<Integer>(CAP);

		for (int i = 1; i <= CAP; ++i) {
			if (!queue.offer(i)) {
				throw new AssertionError("Failed to offer to queue");
			}
		}

		if (queue.offer(CAP + 1)) {
			throw new AssertionError("offer to full queue");
		}

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					System.out.println("before put -2");

					queue.put(-2);

					System.out.println("after put -2");
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}).start();

		System.out.println("before clear");

		Thread.sleep(100);

		queue.clear();

		System.out.println("after clear");

		int take = queue.take();
		if (take != -2) {
			throw new AssertionError("queue take incorrect after clear");
		}

		if (queue.size() != 0) {
			throw new AssertionError("queue size incorrect after take");
		}

		// noinspection ConstantConditions
		if (queue.peek() != null) {
			throw new AssertionError("peek not null after clear");
		}

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					int i = 1;

					while (true) {
						// Integer x = queue.poll();
						int x = queue.take();
						// if (x != null) {
						if (x == -1) {
							if (i == 1001) {
								Integer peek = queue.peek();
								if (peek != null) {
									throw new AssertionError("Non empty after end signal: " + peek);
								}

								System.out.println("done");
							} else {
								throw new AssertionError("i=" + i + " when terminate");
							}

							break;
						}

						if (i++ != x) {
							throw new AssertionError(i - 1 + " != " + x);
						}
						System.out.println("poll " + x);
						// } else {
						// 	// System.out.println("wait again");
						// 	Thread.sleep(0);
						// }
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}).start();


		for (int i = 1; i <= 1000; i++) {
			queue.put(i);
			// System.out.println("put " + i);
		}

		queue.put(-1);
	}
}
