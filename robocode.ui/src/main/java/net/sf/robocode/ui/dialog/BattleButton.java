/**
 * Copyright (c) 2001-2020 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://robocode.sourceforge.io/license/epl-v10.html
 */
package net.sf.robocode.ui.dialog;


import net.sf.robocode.ui.IWindowManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * @author Pavel Savara (original)
 */
public class BattleButton extends JButton implements ActionListener {
	private static final long serialVersionUID = 1L;

	private final BattleDialog battleDialog;
	private IWindowManager windowManager;

	public BattleButton(BattleDialog battleDialog, IWindowManager windowManager) {
		this.battleDialog = battleDialog;
		this.windowManager = windowManager;

		initialize();
	}

	public void actionPerformed(ActionEvent e) {
		attach();
		if (!battleDialog.isVisible()) { // || battleDialog.getState() != Frame.NORMAL) {
			WindowUtil.packPlaceShow(windowManager.getRobocodeFrame(), battleDialog);
		} else {
			battleDialog.setVisible(true);
		}
	}

	/**
	 * Initialize the class.
	 */
	private void initialize() {
		addActionListener(this);
		setPreferredSize(new Dimension(110, 25));
		setMinimumSize(new Dimension(110, 25));
		setMaximumSize(new Dimension(110, 25));
		setHorizontalAlignment(SwingConstants.CENTER);
		setMargin(new Insets(0, 0, 0, 0));
		setText("Main battle log");
		setToolTipText("Main battle log");
	}

	public void attach() {
		battleDialog.attach();
	}
}
