package net.sf.robocode.gl2;

import org.jogamp.glg2d.GLG2DPanel;

import javax.swing.JComponent;

public final class GL2 implements IGL2 {
	@Override
	public GLG2DPanel getGL2Panel(JComponent component) {
		return new GLG2DPanel(component);
	}
}
