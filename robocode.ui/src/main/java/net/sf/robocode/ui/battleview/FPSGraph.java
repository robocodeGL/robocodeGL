package net.sf.robocode.ui.battleview;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayDeque;
import java.util.Queue;

final class FPSGraph {
	private final Queue<Double> fpsHistory = new ArrayDeque<Double>(1000);
	private JComponent component;

	private float fpsX = 15;
	private float fpsY = 15;
	private final float fpsW = 100;
	private final float fpsH = 75;

	private boolean fpsDrag = false;
	private float fpsDX;
	private float fpsDY;

	public void init(JComponent component) {
		this.component = component;

		component.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (getFPSRect().contains(e.getX(), e.getY())) {
					fpsDrag = true;
					fpsDX = fpsX - e.getX();
					fpsDY = fpsY - e.getY();
					e.consume();
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (fpsDrag) {
					fpsDrag = false;
					e.consume();
				}
			}
		});

		component.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				if (fpsDrag) {
					fpsX = e.getX() + fpsDX;
					fpsY = e.getY() + fpsDY;
					limitFPSRect();
					e.consume();
				}
			}
		});

		component.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				limitFPSRect();
			}
		});
	}

	public void recordFPS(double fps) {
		if (fpsHistory.size() >= 1000) fpsHistory.remove();
		fpsHistory.add(fps);
	}

	public void paint(Graphics2D g) {
		AffineTransform at = g.getTransform();
		g.setTransform(new AffineTransform());

		g.setColor(Color.RED);
		g.setStroke(new BasicStroke(1f));
		g.draw(getFPSRect());

		g.setTransform(at);
	}

	private void limitFPSRect() {
		float x1 = component.getWidth() - fpsW;
		float y1 = component.getHeight() - fpsH;
		if (fpsX > x1) fpsX = x1;
		if (fpsY > y1) fpsY = y1;
		if (fpsX < 0) fpsX = 0;
		if (fpsY < 0) fpsY = 0;
	}

	private Rectangle2D getFPSRect() {
		return new Rectangle2D.Double(fpsX, fpsY, fpsW, fpsH);
	}
}
