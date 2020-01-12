/**
 * Copyright (c) 2001-2020 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://robocode.sourceforge.io/license/epl-v10.html
 */
package net.sf.robocode.ui.dialog;


import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;


/**
 * @author Mathew A. Nelson (original)
 * @author Flemming N. Larsen (contributor)
 */
@SuppressWarnings("serial")
public class ResultsTableCellRenderer extends DefaultTableCellRenderer {

	private final boolean isBordered;

	public ResultsTableCellRenderer(boolean isBordered, boolean alignCenter) {
		super();
		this.isBordered = isBordered;
		if (alignCenter) {
			setHorizontalAlignment(SwingConstants.CENTER);
		}
		setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
			boolean hasFocus, int row, int column) {
		if (isBordered) {
			setBorder(new EtchedBorder(EtchedBorder.RAISED));
			if (isSelected) {
				setBackground(Color.lightGray);
			} else {
				setBackground(Color.white);
			}
			setForeground(Color.black);
		} else if (isSelected) {
			setBackground(Color.lightGray);
			setForeground(Color.black);
		} else {
			setBackground(Color.white);
			setForeground(Color.black);
		}
		setText(value.toString());

		return this;
	}
}
