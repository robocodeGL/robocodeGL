/**
 * Copyright (c) 2001-2020 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://robocode.sourceforge.io/license/epl-v10.html
 */
package net.sf.robocode.ui.dialog;


import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.PrintWriter;


/**
 * This is a class for window utilization.
 *
 * @author Mathew A. Nelson (original)
 * @author Flemming N. Larsen (contributor)
 */
public class WindowUtil {

	private static final Point origin = new Point(40, 40);
	private static final WindowPositionManager windowPositionManager = new WindowPositionManager();
	private static JLabel statusLabel;
	private static PrintWriter statusWriter;
	private static JLabel defaultStatusLabel;

	public static void center(Window w) {
		WindowUtil.center(null, w);
	}

	public static void center(Window main, Window w) {
		Point location = null;
		Dimension size = null;

		Rectangle windowRect = windowPositionManager.getWindowRect(w);
		if (windowRect != null) {
			location = new Point(windowRect.x, windowRect.y);
			size = new Dimension(windowRect.width, windowRect.height);
		}
		if (size != null) {
			w.setSize(size);
		}

		if (location == null && main == null) {
			location = getCenterScreenLocation(null, w);
		}

		if (location != null) {
			w.setLocation(location);
		} else {
			w.setLocationRelativeTo(main);
		}
	}

	public static void centerShow(Window main, Window window) {
		center(main, window);
		window.setVisible(true);
	}

	public static void setFixedSize(JComponent component, Dimension size) {
		component.setPreferredSize(size);
		component.setMinimumSize(size);
		component.setMaximumSize(size);
	}

	public static void error(JFrame frame, String msg) {
		Object[] options = { "OK"};

		JOptionPane.showOptionDialog(frame, msg, "Error", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null,
				options, options[0]);
	}

	public static void fitWindow(Window w) {
		// We don't want to receive the resize event for this pack!
		// ... yes we do!
		// w.removeComponentListener(windowPositionManager);
		w.pack();

		// center(null, w, false);
	}

	public static void packCenterShow(Window window) {
		// We don't want to receive the resize event for this pack!
		window.removeComponentListener(windowPositionManager);
		window.pack();
		center(window);
		window.setVisible(true);
	}

	public static void packCenterShow(Window main, Window window) {
		// We don't want to receive the resize event for this pack!
		window.removeComponentListener(windowPositionManager);
		window.pack();
		center(main, window);
		window.setVisible(true);
	}

	public static void packCenterScreenShowNoRemember(Window main, Window window) {
		window.pack();

		Point location = getCenterScreenLocation(main, window);

		if (location != null) {
			window.setLocation(location);
		} else {
			window.setLocationRelativeTo(null);
		}
		window.setVisible(true);
	}

	private static Point getCenterScreenLocation(Window main, Window window) {
		Point location = null;

		Point mainLocation;
		if (main != null) {
			mainLocation = main.getLocation();
		} else {
			Rectangle rect = windowPositionManager.getWindowRect(RobocodeFrame.class.getName());
			mainLocation = rect == null ? null : rect.getLocation();
		}

		if (mainLocation != null) {
			GraphicsConfiguration screen = WindowPositionManager.findDeviceContainingLocation(mainLocation);
			if (screen != null) {
				Rectangle screenBounds = screen.getBounds();

				Dimension size = window.getSize();

				location = new Point((int) screenBounds.getCenterX(), (int) screenBounds.getCenterY());
				location.x -= size.width / 2;
				location.y -= size.height / 2;

				if (location.y + size.height > screenBounds.y + screenBounds.height) {
					location.y = screenBounds.y + screenBounds.height - size.height;
				}
				if (location.y < screenBounds.y) {
					location.y = screenBounds.y;
				}
				if (location.x + size.width > screenBounds.x + screenBounds.width) {
					location.x = screenBounds.x + screenBounds.width - size.width;
				}
				if (location.x < screenBounds.x) {
					location.x = screenBounds.x;
				}
			}
		}
		return location;
	}

	public static void packPlaceShow(Window main, Window window) {
		window.pack();
		WindowUtil.place(main, window);
		window.setVisible(true);
	}

	public static void place(Window main, Window w) {
		// place windows as a stack

		Point screenOrigin = null;
		Dimension screenSize = null;
		if (main != null) {
			GraphicsConfiguration screen = WindowPositionManager.findDeviceContainingLocation(main.getLocation());
			if (screen != null) {
				Rectangle bounds = screen.getBounds();
				screenOrigin = bounds.getLocation();
				screenSize = new Dimension(bounds.width, bounds.height);
			}
		}
		if (screenSize == null) {
			screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		}

		Dimension size = w.getSize();

		if (size.height > screenSize.height) {
			size.height = screenSize.height;
		}
		if (size.width > screenSize.width) {
			size.width = screenSize.width;
		}

		if (origin.y + size.height > screenSize.height) {
			origin.y = 40;
			origin.x += 40;
		}
		if (origin.x + size.width > screenSize.width) {
			origin.x = 40;
		}

		Point location = new Point(origin);
		if (screenOrigin != null) {
			location.x += screenOrigin.x;
			location.y += screenOrigin.y;
		}

		w.setLocation(location);

		origin.y += 150;
	}

	public static void saveWindowPositions() {
		windowPositionManager.saveWindowPositions();
	}

	public static void message(String s) {
		JOptionPane.showMessageDialog(null, s, "Message", JOptionPane.INFORMATION_MESSAGE);
	}

	public static void messageWarning(String s) {
		JOptionPane.showMessageDialog(null, s, "Warning", JOptionPane.WARNING_MESSAGE);
	}

	public static void messageError(String s) {
		Toolkit.getDefaultToolkit().beep();
		JOptionPane.showMessageDialog(null, s, "Message", JOptionPane.ERROR_MESSAGE);
	}

	public static void setStatus(String s) {
		if (statusWriter != null) {
			statusWriter.println(s);
		}
		if (statusLabel != null) {
			statusLabel.setText(s);
		} else if (defaultStatusLabel != null) {
			defaultStatusLabel.setText(s);
		}
	}

	public static void setStatusLabel(JLabel label) {
		statusLabel = label;
	}

	public static void setDefaultStatusLabel(JLabel label) {
		defaultStatusLabel = label;
	}

	public static void setStatusWriter(PrintWriter out) {
		statusWriter = out;
	}
}
