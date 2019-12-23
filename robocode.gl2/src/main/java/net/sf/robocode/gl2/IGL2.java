package net.sf.robocode.gl2;

import org.jogamp.glg2d.GLG2DPanel;

import javax.swing.JComponent;
import javax.swing.JPanel;

public interface IGL2 {
	GLG2DPanel getGL2Panel(JComponent component);
}
