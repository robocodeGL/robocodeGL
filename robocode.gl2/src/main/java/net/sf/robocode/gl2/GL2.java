/**
 * Copyright (c) 2001-2019 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://robocode.sourceforge.io/license/epl-v10.html
 */
package net.sf.robocode.gl2;

import org.jogamp.glg2d.GLG2DPanel;

import javax.swing.JComponent;

public final class GL2 implements IGL2 {
	@Override
	public GLG2DPanel getGL2Panel(JComponent component) {
		return new GLG2DPanel(component);
	}
}
