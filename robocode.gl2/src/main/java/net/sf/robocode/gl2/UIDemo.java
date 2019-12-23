package net.sf.robocode.gl2;

import org.jogamp.glg2d.GLG2DPanel;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import java.awt.Dimension;
import java.awt.Graphics;

public final class UIDemo extends JPanel {
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.drawRect(100, 100, 50, 50);
	}

	public static void main(String[] args) throws Exception {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		JFrame frame = new JFrame("Swing Demo");

		JPopupMenu.setDefaultLightWeightPopupEnabled(false);

		frame.setContentPane(new GLG2DPanel(new UIDemo()));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setPreferredSize(new Dimension(800, 600));
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
}
