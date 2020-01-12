/**
 * Copyright (c) 2001-2020 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://robocode.sourceforge.io/license/epl-v10.html
 */
package net.sf.robocode.ui.dialog;


import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;


/**
 * @author Mathew A. Nelson (original)
 * @author Flemming N. Larsen (contributor)
 */
@SuppressWarnings("serial")
public final class ConsoleTableCellRenderer extends DefaultTableCellRenderer {
	private static final Color selectedGray = new Color(96, 96, 96);

	public ConsoleTableCellRenderer() {
		super();
		setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
		setBorder(BorderFactory.createEmptyBorder());
		setFont(new Font("Consolas", Font.PLAIN, 13));
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
	                                               boolean hasFocus, int row, int column) {
		if (isSelected) {
			setBackground(selectedGray);
			setForeground(Color.white);
		} else {
			setBackground(Color.darkGray);
			setForeground(Color.white);
		}
		setText(value.toString());

		return this;
	}
}
