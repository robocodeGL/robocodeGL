/**
 * Copyright (c) 2001-2019 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://robocode.sourceforge.io/license/epl-v10.html
 */
package net.sf.robocode.ui.dialog;

import javax.swing.JSlider;

/**
 * JSlider with higher adjusting precision
 *
 * @author Xor
 */
public final class DoubleJSlider extends JSlider {
	private final int scale;

	public DoubleJSlider(int min, int max, double scaledValue, int scale) {
		super(scaleValue(min, scale), scaleValue(max, scale), limit(scaleValue(min, scale), scaleValue(scaledValue, scale), scaleValue(max, scale)));
		this.scale = scale;
	}

	public int getScale() {
		return scale;
	}

	public double getScaledValue() {
		return 1. * super.getValue() / this.scale;
	}

	public void setScaledValue(double val) {
		super.setValue(limit(getMinimum(), scaleValue(val, scale), getMaximum()));
	}

	private static int scaleValue(double val, int scale) {
		return (int) (val * scale + .5);
	}

	private static int limit(int min, int v, int max) {
		if (v < min) return min;
		return Math.min(v, max);
	}
}
