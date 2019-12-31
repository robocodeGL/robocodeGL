package net.sf.robocode.ui.battleview;

import sun.misc.DoubleConsts;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayDeque;
import java.util.Queue;

final class FPSGraph {
	private static final int MAX_HISTORY = 120;
	private static final float FPS_PADDING = 5f;
	public static final int FPS_MARGIN = 15;

	private final Queue<Double> fpsHistory = new ArrayDeque<Double>(MAX_HISTORY);
	private JComponent component;

	private float fpsX = FPS_MARGIN;
	private float fpsY = FPS_MARGIN;
	private final float fpsW = 130f;
	private final float fpsH = 80f;

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
		if (fpsHistory.size() >= MAX_HISTORY) fpsHistory.remove();
		fpsHistory.add(fps);
	}

	public void paint(Graphics2D g) {
		AffineTransform at = g.getTransform();
		g.setTransform(new AffineTransform());

		g.setColor(Color.RED);
		g.setStroke(new BasicStroke(1f));
		g.draw(getFPSRect());
		Font font = new Font("Arial", Font.PLAIN, 10);
		g.setFont(font);

		float contentW = fpsW - FPS_PADDING * 2f;
		float contentH = fpsH - FPS_PADDING * 2f;

		double avg = 0.;
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		{
			int count = 0;
			for (double fps : fpsHistory) {
				if (isFinite(fps)) {
					avg = 1. / (1 + count) * (avg * count + fps);
					if (fps > max) max = fps;
					if (fps < min) min = fps;
					++count;
				}
			}
		}

		{
			Path2D p = new Path2D.Double();

			double realAvg = avg;

			avg = 10. * Math.round(avg / 10.);

			double range = 1e-18 + 2 * Math.max(max - avg, avg - min);
			if (!isFinite(range)) {
				range = 10;
			} else {
				range = 10 * Math.ceil(.1 * range);
			}

			int i = 0;
			for (double fps : fpsHistory) {
				if (!isFinite(fps)) {
					fps = avg;
				}
				double x = fpsX + FPS_PADDING + i / (1e-18 + MAX_HISTORY - 1.) * contentW;
				double y = fpsY + FPS_PADDING + contentH * .5 - contentH / range * (fps - avg);

				if (i == 0) p.moveTo(x, y);
				else p.lineTo(x, y);

				++i;
			}

			FontMetrics fm = g.getFontMetrics(font);
			float fontH = fm.getHeight() - fm.getDescent();

			g.draw(p);
			g.drawString(String.format("%.1f", avg + .5 * range), fpsX + FPS_PADDING, fpsY + FPS_PADDING + fontH);
			g.drawString(String.format("%.1f", avg), fpsX + FPS_PADDING, fpsY + FPS_PADDING + contentH * .5f + fontH * .5f);
			g.drawString(String.format("%.1f", avg - .5 * range), fpsX + FPS_PADDING, fpsY + FPS_PADDING + contentH);

			drawRightAlign(g, String.format("%.1f", realAvg), fpsX + FPS_PADDING + contentW, fpsY + FPS_PADDING + contentH, fm);
		}

		g.setTransform(at);
	}

	private void drawRightAlign(Graphics2D g, String real, float dx, float dy, FontMetrics fm) {
		g.drawString(real,
			dx - fm.stringWidth(real), dy);
	}

	private void limitFPSRect() {
		float x1 = component.getWidth() - FPS_MARGIN - fpsW;
		float y1 = component.getHeight() - FPS_MARGIN - fpsH;
		if (fpsX > x1) fpsX = x1;
		if (fpsY > y1) fpsY = y1;
		if (fpsX < FPS_MARGIN) fpsX = FPS_MARGIN;
		if (fpsY < FPS_MARGIN) fpsY = FPS_MARGIN;
	}

	private Rectangle2D getFPSRect() {
		return new Rectangle2D.Double(fpsX, fpsY, fpsW, fpsH);
	}

	public static boolean isFinite(double d) {
		return Math.abs(d) <= DoubleConsts.MAX_VALUE;
	}
}
