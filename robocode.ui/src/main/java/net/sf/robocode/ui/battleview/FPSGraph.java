package net.sf.robocode.ui.battleview;

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
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayDeque;
import java.util.Queue;

import static java.lang.Math.abs;

final class FPSGraph {
	private static final int MAX_HISTORY = 120;
	private static final float FPS_PADDING = 5f;
	public static final int FPS_MARGIN = 15;

	private final Queue<Double> deltaHistory = new ArrayDeque<Double>(MAX_HISTORY);
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

	public void recordFrameDelta(double deltaNano) {
		if (deltaHistory.size() >= MAX_HISTORY) deltaHistory.remove();
		deltaHistory.add(deltaNano * 1e-6);
	}

	public void paint(Graphics2D g) {
		AffineTransform at = g.getTransform();
		g.setTransform(new AffineTransform());
		Rectangle2D rect = getFPSRect();

		g.setColor(new Color(0f,0f,0f,.8f));
		g.fill(rect);
		g.setColor(Color.RED);
		g.setStroke(new BasicStroke(1f));
		g.draw(rect);
		Font font = new Font("Arial", Font.PLAIN, 10);
		g.setFont(font);

		float contentW = fpsW - FPS_PADDING * 2f;
		float contentH = fpsH - FPS_PADDING * 2f;

		double avgDelta = 0.;
		double avgFps = 0.;
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		{
			int i = 0;
			int count = 0;
			for (double delta : deltaHistory) {
				avgDelta = 1. / (1 + i) * (avgDelta * i + delta);
				double fps = 1e3 / delta;
				if (isFinite(fps)) {
					avgFps = 1. / (1 + count) * (avgFps * count + fps);
					if (fps > max) max = fps;
					if (fps < min) min = fps;
					++count;
				}
				++i;
			}
		}

		double avg = 1e3 / avgDelta;

		{
			Path2D p = new Path2D.Double();

			double mid = 60; // 10. * Math.round(avg / 10.);

			double range = 1e-18 + 2 * Math.max(max - mid, mid - min);
			if (!isFinite(range)) {
				range = 10;
			} else {
				range = 10 * Math.ceil(.1 * range);
			}

			float baseX = fpsX + FPS_PADDING;
			float baseY = fpsY + FPS_PADDING + contentH * .5f;

			int i = 0;
			for (double delta : deltaHistory) {
				double fps = 1e3 / delta;
				if (!isFinite(fps)) {
					fps = mid;
				}
				double x = baseX + i / (1e-18 + MAX_HISTORY - 1.) * contentW;
				double y = baseY - contentH / range * (fps - mid);

				if (i == 0) p.moveTo(x, y);
				else p.lineTo(x, y);

				++i;
			}

			FontMetrics fm = g.getFontMetrics(font);
			float fontH = fm.getHeight() - fm.getDescent();

			g.setColor(Color.YELLOW);
			g.draw(new Line2D.Double(baseX, baseY, baseX + contentW, baseY));
			g.setColor(Color.RED);

			g.draw(p);
			g.drawString(String.format("%.1f", mid + .5 * range), baseX, fpsY + FPS_PADDING + fontH);
			// g.drawString(String.format("%.1f", mid), baseX, fpsY + FPS_PADDING + contentH * .5f + fontH * .5f);
			g.drawString(String.format("%.1f", mid - .5 * range), baseX, fpsY + FPS_PADDING + contentH);

			drawRightAlign(g, String.format("%.1f", avg), fpsX + FPS_PADDING + contentW, fpsY + FPS_PADDING + contentH, fm);
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

	private static boolean isFinite(double d) {
		return abs(d) <= Double.MAX_VALUE;
	}
}
