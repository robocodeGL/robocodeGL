/**
 * Copyright (c) 2001-2019 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://robocode.sourceforge.io/license/epl-v10.html
 */
package net.sf.robocode.async;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Xor
 */
public final class TestPromise extends JFrame {
	private TestPromise() {
		setPreferredSize(new Dimension(300, 300));

		JButton btn = new JButton("Test");
		btn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Promise.resolved()
					.then((Runnable) null)
					.then(new Runnable() {
						@Override
						public void run() {
							System.out.println("Here");
						}
					})
					.delay(1000)
					.then(new PromiseSupplier() {
						@Override
						public Promise get() {
							System.out.println("1s elapsed");

							return Promise
								.delayed(1000)
								.then(new PromiseSupplier() {
									@Override
									public Promise get() {
										System.out.println("2s elapsed");

										return Promise.delayed(1000);
									}
								});
						}
					})
					.then((Runnable) null)
					.then(new Runnable() {
						@Override
						public void run() {
							System.out.println("3s elapsed");
						}
					})
					.delay(1000)
					.then(new Runnable() {
						@Override
						public void run() {
							System.out.println("4s elapsed");
						}
					})
				;
			}
		});
		add(btn);

		pack();
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}

	public static void main(String[] args) {
		new TestPromise().setVisible(true);
	}
}
