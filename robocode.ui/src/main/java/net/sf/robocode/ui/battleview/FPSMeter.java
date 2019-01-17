/**
 * Copyright (c) 2001-2019 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://robocode.sourceforge.io/license/epl-v10.html
 */
package net.sf.robocode.ui.battleview;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
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

/**
 * A simple widget displaying realtime FPS on battle view
 *
 * @author Xor
 */
final class FPSMeter {
	private static final int MAX_HISTORY = 120;
	private static final int SHORT_HISTORY = 3;
	private static final float FPS_PADDING = 5f;
	private static final int FPS_MARGIN = 15;

	private static final Color MAIN_COLOR = new Color(0f, 1f, 0f, .7f);
	private static final Color BACK_COLOR = new Color(0f, 0f, 0f, .7f);
	private static final Font FONT = new Font("Arial", Font.PLAIN, 10);
	private static final float STICK_MARGIN = 5f;

	private final Queue<Double> deltaHistory = new ArrayDeque<Double>(MAX_HISTORY);
	private final Queue<Double> deltaHistoryShort = new ArrayDeque<Double>(SHORT_HISTORY);

	private float fpsX = FPS_MARGIN;
	private float fpsY = FPS_MARGIN;
	private final float fpsW = 130f;
	private final float fpsH = 80f;

	private boolean fpsDrag = false;
	private float fpsDX;
	private float fpsDY;

	private float oldWidth = 0;
	private float oldHeight = 0;

	private Component component;
	private boolean visible;
	private int turn;

	public void init(Component c) {
		this.component = c;

		oldWidth = component.getWidth();
		oldHeight = component.getHeight();

		component.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (!visible) return;

				if (getFPSRect().contains(e.getX(), e.getY())) {
					fpsDrag = true;
					fpsDX = fpsX - e.getX();
					fpsDY = fpsY - e.getY();
					e.consume();
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (!visible) return;

				if (fpsDrag) {
					fpsDrag = false;
					e.consume();
				}
			}
		});

		component.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				if (!visible) return;

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
				if (!visible) return;

				limitFPSRect();

				oldWidth = component.getWidth();
				oldHeight = component.getHeight();
			}
		});
	}

	public void recordFrameDelta(double deltaNano) {
		if (!visible) return;

		if (deltaHistoryShort.size() >= SHORT_HISTORY) deltaHistoryShort.remove();
		deltaHistoryShort.add(deltaNano * 1e-6);

		double avgDelta = 0.;
		{
			int i = 0;
			for (double delta : deltaHistoryShort) {
				avgDelta = 1. / (1 + i) * (avgDelta * i + delta);
				++i;
			}
		}

		if (deltaHistory.size() >= MAX_HISTORY) deltaHistory.remove();
		deltaHistory.add(avgDelta);
	}

	public void setVisible(boolean visible) {
		// System.out.println("FPSMeter.setVisible " + visible);
		if (visible != this.visible) {
			this.visible = visible;

			if (!visible) {
				reset();
			}
		}
	}

	private void reset() {
		fpsX = FPS_MARGIN;
		fpsY = FPS_MARGIN;
		deltaHistoryShort.clear();
		deltaHistory.clear();
		fpsDrag = false;
	}

	public void paint(Graphics2D g) {
		if (!visible) return;

		AffineTransform at = g.getTransform();
		g.setTransform(new AffineTransform());
		Rectangle2D rect = getFPSRect();

		g.setColor(BACK_COLOR);
		g.fill(rect);
		g.setColor(MAIN_COLOR);
		g.setStroke(new BasicStroke(1f));
		g.draw(rect);
		g.setFont(FONT);

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

			int n = deltaHistory.size();

			int i = 0;
			for (double delta : deltaHistory) {
				double fps = 1e3 / delta;
				if (!isFinite(fps)) {
					fps = mid;
				}
				double x = baseX + (i + MAX_HISTORY - n) / (1e-18 + MAX_HISTORY - 1.) * contentW;
				double y = baseY - contentH / range * (fps - mid);

				if (i == 0) p.moveTo(x, y);
				else p.lineTo(x, y);

				++i;
			}

			FontMetrics fm = g.getFontMetrics(FONT);
			float fontH = fm.getHeight() - fm.getDescent();

			g.setColor(Color.YELLOW);
			g.draw(new Line2D.Double(baseX, baseY, baseX + contentW, baseY));
			g.setColor(MAIN_COLOR);

			g.draw(p);
			g.drawString(String.format("%.1f", mid + .5 * range), baseX, fpsY + FPS_PADDING + fontH);
			// g.drawString(String.format("%.1f", mid), baseX, fpsY + FPS_PADDING + contentH * .5f + fontH * .5f);
			g.drawString(String.format("%.1f", mid - .5 * range), baseX, fpsY + FPS_PADDING + contentH);

			drawRightAlign(g, String.format("%.1f", .1 * Math.round(avg * 10)), fpsX + FPS_PADDING + contentW, fpsY + FPS_PADDING + contentH, fm);

			drawRightAlign(g, String.format("%d", turn), fpsX + FPS_PADDING + contentW, fpsY + FPS_PADDING + fontH, fm);
		}

		g.setTransform(at);
	}

	private void drawRightAlign(Graphics2D g, String real, float dx, float dy, FontMetrics fm) {
		g.drawString(real,
			dx - fm.stringWidth(real), dy);
	}

	private void limitFPSRect() {
		float x10 = oldWidth - FPS_MARGIN - fpsW;
		float y10 = oldHeight - FPS_MARGIN - fpsH;

		float x1 = component.getWidth() - FPS_MARGIN - fpsW;
		float y1 = component.getHeight() - FPS_MARGIN - fpsH;

		if (oldWidth > fpsW + (FPS_MARGIN + STICK_MARGIN + 1) * 2 && fpsX > x10 - STICK_MARGIN) fpsX = x1;
		else if (fpsX > x1) fpsX = x1;
		if (oldHeight > fpsH + (FPS_MARGIN + STICK_MARGIN + 1) * 2 && fpsY > y10 - STICK_MARGIN) fpsY = y1;
		else if (fpsY > y1) fpsY = y1;
		if (fpsX < FPS_MARGIN + STICK_MARGIN) fpsX = FPS_MARGIN;
		if (fpsY < FPS_MARGIN + STICK_MARGIN) fpsY = FPS_MARGIN;
	}

	private Rectangle2D getFPSRect() {
		return new Rectangle2D.Double(fpsX, fpsY, fpsW, fpsH);
	}

	public void setTurnId(int turn) {
		this.turn = turn;
	}

	private static boolean isFinite(double d) {
		return abs(d) <= Double.MAX_VALUE;
	}
}
