/**
 * Copyright (c) 2001-2020 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://robocode.sourceforge.io/license/epl-v10.html
 */
package net.sf.robocode.ui.battleview;


import com.jogamp.common.util.IntObjectHashMap;
import com.jogamp.opengl.util.Animator;
import net.sf.robocode.battle.snapshot.RobotSnapshot;
import net.sf.robocode.robotpaint.Graphics2DSerialized;
import net.sf.robocode.robotpaint.IGraphicsProxy;
import net.sf.robocode.settings.ISettingsListener;
import net.sf.robocode.settings.ISettingsManager;
import net.sf.robocode.ui.IImageManager;
import net.sf.robocode.ui.IWindowManager;
import net.sf.robocode.ui.IWindowManagerExt;
import net.sf.robocode.ui.gfx.GraphicsState;
import net.sf.robocode.ui.gfx.RenderObject;
import net.sf.robocode.ui.gfx.RobocodeLogo;
import org.jogamp.glg2d.GLG2DCanvas;
import robocode.BattleRules;
import robocode.Rules;
import robocode.control.events.BattleAdaptor;
import robocode.control.events.BattleFinishedEvent;
import robocode.control.events.BattleStartedEvent;
import robocode.control.events.TurnEndedEvent;
import robocode.control.snapshot.IBulletSnapshot;
import robocode.control.snapshot.IRobotSnapshot;
import robocode.control.snapshot.ITurnSnapshot;
import robocode.util.Utils;

import javax.swing.JPanel;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Random;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static java.lang.Math.sqrt;


/**
 * @author Mathew A. Nelson (original)
 * @author Flemming N. Larsen (original)
 * @author Pavel Savara (contributor)
 */
@SuppressWarnings("serial")
public class BattleView extends GLG2DCanvas implements ScaleProvider {

	private static final String ROBOCODE_SLOGAN = "Build the best, destroy the rest!";

	private static final Color CANVAS_BG_COLOR = new Color(23, 23, 23); // new Color(12, 12, 12); // SystemColor.controlDkShadow;
	private static final Color GROUND_COLOR = new Color(23, 23, 23); // Color.BLACK
	private static final Color EDGE_COLOR = new Color(255, 255, 255, 32); // Color.RED;
	private static final Color LOGO_BG_COLOR = Color.BLACK; // Color.BLACK;

	private static final Area BULLET_AREA = new Area(new Ellipse2D.Double(-0.5, -0.5, 1, 1));

	private static final int ROBOT_TEXT_Y_OFFSET = 24;

	private static final Font SMALL_FONT = new Font("Dialog", Font.PLAIN, 10);
	private static final BasicStroke DEFAULT_STROKE = new BasicStroke();

	private BattleRules battleRules;

	// The battle and battlefield,
	private BattleField battleField;

	private boolean initialized;
	private double scale = 1.0;

	// Ground
	private int[][] groundTiles;

	private final int groundTileWidth = 64;
	private final int groundTileHeight = 64;

	private Image groundImage;

	// Draw option related things
	private boolean drawRobotName;
	private boolean drawRobotEnergy;
	private boolean drawScanArcs;
	private boolean drawExplosions;
	private boolean drawGround;
	private boolean drawExplosionDebris;
	private boolean allowScaleUp;

	private RenderingHints renderingHints;

	// Fonts and the like
	private Font smallFont;
	private FontMetrics smallFontMetrics;

	private final IImageManager imageManager;

	private final ISettingsManager properties;
	private final IWindowManagerExt windowManager;

	private final GeneralPath robocodeTextPath = new RobocodeLogo().getRobocodeText();

	private static final MirroredGraphics mirroredGraphics = new MirroredGraphics();

	private final GraphicsState graphicsState = new GraphicsState();
	private IGraphicsProxy[] robotGraphics;

	private final FPSGraph fpsGraph = new FPSGraph();

	private PreferredSizeMode preferredSizeMode = PreferredSizeMode.MINIMAL;
	private Dimension lastSize;

	public BattleView(ISettingsManager properties, IWindowManager windowManager, IImageManager imageManager) {

		this.properties = properties;
		this.windowManager = (IWindowManagerExt) windowManager;
		this.imageManager = imageManager;

		battleField = new BattleField(800, 600);

		new BattleObserver(windowManager);

		properties.addPropertyListener(new ISettingsListener() {
			public void settingChanged(String property) {
				loadDisplayOptions();
				if (property.startsWith("robocode.options.rendering")) {
					initialized = false;
					reinitialize();
				}
			}
		});

		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				initialized = false;
				reinitialize();
			}
		});

		setDrawableComponent(new MyPanel());
		setGLDrawing(true);
	}

	@Override
	public Dimension getPreferredSize() {
		if (preferredSizeMode == PreferredSizeMode.KEEP_CURRENT) {
			return lastSize == null ? getSize() : lastSize;
		} else if (allowScaleUp && preferredSizeMode == PreferredSizeMode.SHRINK_TO_FIT) {
			return getPreferredSizeShrinkToFit();
		} else {
			return getPreferredSizeMinimal();
		}
	}

	public void setPreferredSizeMode(PreferredSizeMode preferredSizeMode) {
		if (preferredSizeMode != this.preferredSizeMode) {
			if (preferredSizeMode == PreferredSizeMode.KEEP_CURRENT) {
				lastSize = getSize();
			} else {
				lastSize = null;
			}
			this.preferredSizeMode = preferredSizeMode;
		}
	}

	private Dimension getPreferredSizeShrinkToFit() {
		float scale = Math.min(1f * getWidth() / battleField.getWidth(), 1f * getHeight() / battleField.getHeight());

		return new Dimension((int) Math.ceil(battleField.getWidth() * scale), (int) Math.ceil(battleField.getHeight() * scale));
	}

	private Dimension getPreferredSizeMinimal() {
		int w = battleField.getWidth();
		int h = battleField.getHeight();

		if (w > 1000 || h > 1000) {
			float scale = Math.min(1000f / w, 1000f / h);
			w *= scale;
			h *= scale;
		}
		return new Dimension(w, h);
	}

	public Component init() {
		loadDisplayOptions();

		Component comp = (Component) getGLDrawable();
		comp.setEnabled(true);
		fpsGraph.init(comp);

		Animator animator = new Animator();
		animator.add(this.getGLDrawable());

		animator.start();

		return comp;
	}

	public BufferedImage getScreenshot() {
		BufferedImage screenshot = getGraphicsConfiguration().createCompatibleImage(getWidth(), getHeight());

		if (windowManager.getLastSnapshot() == null) {
			paintRobocodeLogo((Graphics2D) screenshot.getGraphics());
		} else {
			drawBattle((Graphics2D) screenshot.getGraphics(), windowManager.getLastSnapshot(), null, 1);
		}
		return screenshot;
	}

	private void loadDisplayOptions() {
		ISettingsManager props = properties;

		drawRobotName = props.getOptionsViewRobotNames();
		drawRobotEnergy = props.getOptionsViewRobotEnergy();
		drawScanArcs = props.getOptionsViewScanArcs();
		drawGround = props.getOptionsViewGround();
		drawExplosions = props.getOptionsViewExplosions();
		drawExplosionDebris = props.getOptionsViewExplosionDebris();
		allowScaleUp = props.getOptionsRenderingAllowScaleUp();
		fpsGraph.setVisible(props.getOptionsMiscFPSMeter());

		renderingHints = props.getRenderingHints();
	}

	private void reinitialize() {
		initialized = false;
	}

	public double getScale() {
		return scale;
	}

	private void initialize() {
		loadDisplayOptions();

		// If we are scaled...
		if (allowScaleUp || getWidth() < battleField.getWidth() || getHeight() < battleField.getHeight()) {
			// Use the smaller scale.
			// Actually we don't need this, since
			// the RobocodeFrame keeps our aspect ratio intact.

			scale = min((double) getWidth() / battleField.getWidth(), (double) getHeight() / battleField.getHeight());
		} else {
			scale = 1;
		}

		// Scale font
		smallFont = SMALL_FONT.deriveFont((float) (SMALL_FONT.getSize2D() / Math.min(1., scale)));
		smallFontMetrics = getGraphics().getFontMetrics(smallFont);

		// Initialize ground image
		if (drawGround) {
			createGroundImage();
		} else {
			groundImage = null;
		}

		initialized = true;
	}

	private void createGroundImage() {
		// Reinitialize ground tiles

		Random r = new Random(); // independent

		final int NUM_HORZ_TILES = battleField.getWidth() / groundTileWidth + 1;
		final int NUM_VERT_TILES = battleField.getHeight() / groundTileHeight + 1;

		if ((groundTiles == null) || (groundTiles.length != NUM_VERT_TILES) || (groundTiles[0].length != NUM_HORZ_TILES)) {

			groundTiles = new int[NUM_VERT_TILES][NUM_HORZ_TILES];
			for (int y = NUM_VERT_TILES - 1; y >= 0; y--) {
				for (int x = NUM_HORZ_TILES - 1; x >= 0; x--) {
					groundTiles[y][x] = (int) round(r.nextDouble() * 4);
				}
			}
		}

		// Create new buffered image with the ground pre-rendered

		int groundWidth = (int) (battleField.getWidth() * scale);
		int groundHeight = (int) (battleField.getHeight() * scale);

		groundImage = new BufferedImage(groundWidth, groundHeight, BufferedImage.TYPE_INT_RGB);

		Graphics2D groundGfx = (Graphics2D) groundImage.getGraphics();

		groundGfx.setRenderingHints(renderingHints);

		groundGfx.setTransform(AffineTransform.getScaleInstance(scale, scale));

		for (int y = NUM_VERT_TILES - 1; y >= 0; y--) {
			for (int x = NUM_HORZ_TILES - 1; x >= 0; x--) {
				Image img = imageManager.getGroundTileImage(groundTiles[y][x]);

				if (img != null) {
					groundGfx.drawImage(img, x * groundTileWidth, y * groundTileHeight, null);
				}
			}
		}
	}

	private void drawBattle(Graphics2D g, ITurnSnapshot snapShot, ITurnSnapshot lastSnapshot, double t) {
		// Save the graphics state
		graphicsState.save(g);

		// Reset transform
		g.setTransform(new AffineTransform());

		// Reset clip
		g.setClip(null);

		// Clear canvas
		g.setColor(CANVAS_BG_COLOR);
		g.fillRect(0, 0, getWidth(), getHeight());

		// Calculate border space
		double dx = (getWidth() - scale * battleField.getWidth()) / 2;
		double dy = (getHeight() - scale * battleField.getHeight()) / 2;

		// Scale and translate the graphics
		AffineTransform at = AffineTransform.getTranslateInstance(dx, dy);

		at.concatenate(AffineTransform.getScaleInstance(scale, scale));
		g.setTransform(at);

		// Set the clip rectangle
		g.setClip(0, 0, battleField.getWidth(), battleField.getHeight());

		// Draw ground
		drawGround(g);

		if (snapShot != null) {
			// Draw scan arcs
			drawScanArcs(g, snapShot);

			// Draw robots
			drawRobots(g, snapShot, lastSnapshot, t);

			// Draw robot (debug) paintings
			drawRobotPaint(g, snapShot);
		}

		// Draw the border of the battlefield
		drawBorderEdge(g);

		if (snapShot != null) {
			// Draw all bullets
			drawBullets(g, snapShot, lastSnapshot, t);

			// Draw all text
			drawText(g, snapShot, lastSnapshot, t);
		}

		// Restore the graphics state
		graphicsState.restore(g);
	}

	private void drawGround(Graphics2D g) {
		if (drawGround) {
			// Create pre-rendered ground image if it is not available
			if (groundImage == null) {
				createGroundImage();
			}
			// Draw the pre-rendered ground if it is available
			if (groundImage != null) {
				int groundWidth = (int) (battleField.getWidth() * scale) + 1;
				int groundHeight = (int) (battleField.getHeight() * scale) + 1;

				int dx = (getWidth() - groundWidth) / 2;
				int dy = (getHeight() - groundHeight) / 2;

				final AffineTransform savedTx = g.getTransform();

				g.setTransform(new AffineTransform());
				g.drawImage(groundImage, dx, dy, groundWidth, groundHeight, null);

				g.setTransform(savedTx);
			}
		} else {
			// Ground should not be drawn
			g.setColor(GROUND_COLOR);
			g.fillRect(0, 0, battleField.getWidth(), battleField.getHeight());
		}

		// Draw Sentry Border if it is enabled visually
		if (properties.getOptionsViewSentryBorder()) {
			drawSentryBorder(g);
		}
	}

	private void drawSentryBorder(Graphics2D g) {
		int borderSentrySize = battleRules.getSentryBorderSize();

		g.setColor(new Color(0xff, 0x00, 0x00, 0x80));
		g.fillRect(0, 0, borderSentrySize, battleField.getHeight());
		g.fillRect(battleField.getWidth() - borderSentrySize, 0, borderSentrySize, battleField.getHeight());
		g.fillRect(borderSentrySize, 0, battleField.getWidth() - 2 * borderSentrySize, borderSentrySize);
		g.fillRect(borderSentrySize, battleField.getHeight() - borderSentrySize,
			battleField.getWidth() - 2 * borderSentrySize, borderSentrySize);
	}

	private void drawBorderEdge(Graphics2D g) {
		final Shape savedClip = g.getClip();

		g.setClip(null);

		g.setColor(EDGE_COLOR);
		g.drawRect(-1, -1, battleField.getWidth() + 2, battleField.getHeight() + 2);

		g.setClip(savedClip);
	}

	private void drawScanArcs(Graphics2D g, ITurnSnapshot snapShot) {
		if (drawScanArcs) {
			for (IRobotSnapshot robotSnapshot : snapShot.getRobots()) {
				if (robotSnapshot.getState().isAlive()) {
					drawScanArc(g, robotSnapshot);
				}
			}
		}
	}

	private void drawRobots(Graphics2D g, ITurnSnapshot snapShot, ITurnSnapshot lastSnapshot, double t) {
		double x, y;
		AffineTransform at;
		int battleFieldHeight = battleField.getHeight();

		if (drawGround && drawExplosionDebris) {
			RenderObject explodeDebrise = imageManager.getExplosionDebriseRenderImage();

			for (IRobotSnapshot robotSnapshot : snapShot.getRobots()) {
				if (robotSnapshot.getState().isDead()) {
					x = robotSnapshot.getX();
					y = battleFieldHeight - robotSnapshot.getY();

					at = AffineTransform.getTranslateInstance(x, y);

					explodeDebrise.setTransform(at);
					explodeDebrise.paint(g);
				}
			}
		}

		IntObjectHashMap last = null;
		if (t != 1.) {
			last = getRobotMap(lastSnapshot);
		}

		for (IRobotSnapshot robotSnapshot : snapShot.getRobots()) {
			if (robotSnapshot.getState().isAlive()) {
				double rx = robotSnapshot.getX();
				double ry = robotSnapshot.getY();

				double bodyHeading = robotSnapshot.getBodyHeading();
				double gunHeading = robotSnapshot.getGunHeading();
				double radarHeading = robotSnapshot.getRadarHeading();

				if (t != 1.) {
					IRobotSnapshot l = (IRobotSnapshot) last.get(robotSnapshot.getRobotIndex());
					if (l != null) {
						rx = l.getX() * (1. - t) + rx * t;
						ry = l.getY() * (1. - t) + ry * t;

						double lBodyHeading = l.getBodyHeading();
						double lGunHeading = l.getGunHeading();
						double lRadarHeading = l.getRadarHeading();

						bodyHeading = lBodyHeading + Utils.normalRelativeAngle(bodyHeading - lBodyHeading) * t;
						gunHeading = lGunHeading + Utils.normalRelativeAngle(gunHeading - lGunHeading) * t;
						radarHeading = lRadarHeading + Utils.normalRelativeAngle(radarHeading - lRadarHeading) * t;
					}
				}

				x = rx;
				y = battleFieldHeight - ry;

				at = AffineTransform.getTranslateInstance(x, y);
				at.rotate(bodyHeading);

				RenderObject robotRenderImage = imageManager.getColoredBodyRenderImage(robotSnapshot.getBodyColor());

				robotRenderImage.setTransform(at);
				robotRenderImage.paint(g);

				at = AffineTransform.getTranslateInstance(x, y);
				at.rotate(gunHeading);

				RenderObject gunRenderImage = imageManager.getColoredGunRenderImage(robotSnapshot.getGunColor());

				gunRenderImage.setTransform(at);
				gunRenderImage.paint(g);

				if (!robotSnapshot.isDroid()) {
					at = AffineTransform.getTranslateInstance(x, y);
					at.rotate(radarHeading);

					RenderObject radarRenderImage = imageManager.getColoredRadarRenderImage(robotSnapshot.getRadarColor());

					radarRenderImage.setTransform(at);
					radarRenderImage.paint(g);
				}
			}
		}
	}

	private IntObjectHashMap getRobotMap(ITurnSnapshot lastSnapshot) {
		IntObjectHashMap last;
		last = new IntObjectHashMap();
		if (lastSnapshot != null) {
			for (IRobotSnapshot robot : lastSnapshot.getRobots()) {
				last.put(robot.getRobotIndex(), robot);
			}
		}
		return last;
	}

	private void drawText(Graphics2D g, ITurnSnapshot snapShot, ITurnSnapshot lastSnapshot, double t) {
		IntObjectHashMap last = null;
		if (t != 1.) {
			last = getRobotMap(lastSnapshot);
		}

		final Shape savedClip = g.getClip();

		g.setClip(null);

		for (IRobotSnapshot robotSnapshot : snapShot.getRobots()) {
			if (robotSnapshot.getState().isDead()) {
				continue;
			}
			double rx = robotSnapshot.getX();
			double ry = robotSnapshot.getY();

			if (t != 1.) {
				IRobotSnapshot l = (IRobotSnapshot) last.get(robotSnapshot.getRobotIndex());
				if (l != null) {
					rx = l.getX() * (1. - t) + rx * t;
					ry = l.getY() * (1. - t) + ry * t;
				}
			}

			float x = (float) rx;
			float y = battleField.getHeight() - (float) ry;

			if (drawRobotEnergy) {
				g.setColor(Color.white);
				int ll = (int) robotSnapshot.getEnergy();
				int rl = (int) ((robotSnapshot.getEnergy() - ll + .001) * 10.0);

				if (rl == 10) {
					rl = 9;
				}
				String energyString = ll + "." + rl;

				if (robotSnapshot.getEnergy() == 0 && robotSnapshot.getState().isAlive()) {
					energyString = "Disabled";
				}
				centerString(g, energyString, x, y - ROBOT_TEXT_Y_OFFSET - smallFontMetrics.getHeight() * .5f, smallFont,
					smallFontMetrics);
			}
			if (drawRobotName) {
				g.setColor(Color.white);
				centerString(g, robotSnapshot.getVeryShortName(), x,
					y + ROBOT_TEXT_Y_OFFSET + smallFontMetrics.getHeight() * .5f, smallFont, smallFontMetrics);
			}
		}

		g.setClip(savedClip);
	}

	private void drawRobotPaint(Graphics2D g, ITurnSnapshot turnSnapshot) {

		int robotIndex = 0;

		for (IRobotSnapshot robotSnapshot : turnSnapshot.getRobots()) {
			final Object graphicsCalls = ((RobotSnapshot) robotSnapshot).getGraphicsCalls();

			if (graphicsCalls == null || !robotSnapshot.isPaintEnabled()) {
				continue;
			}

			// Save the graphics state
			GraphicsState gfxState = new GraphicsState();

			gfxState.save(g);

			g.setBackground(Color.WHITE);
			g.setColor(Color.BLACK);
			g.setFont(SMALL_FONT);
			g.setStroke(DEFAULT_STROKE);

			g.setClip(null);
			g.setComposite(AlphaComposite.SrcAtop);

			IGraphicsProxy gfxProxy = getRobotGraphics(robotIndex);

			if (robotSnapshot.isSGPaintEnabled()) {
				gfxProxy.processTo(g, graphicsCalls);
			} else {
				mirroredGraphics.bind(g, battleField.getHeight());
				gfxProxy.processTo(mirroredGraphics, graphicsCalls);
				mirroredGraphics.release();
			}

			// Restore the graphics state
			gfxState.restore(g);

			robotIndex++;
		}
	}

	private IGraphicsProxy getRobotGraphics(int robotIndex) {
		if (robotGraphics[robotIndex] == null) {
			robotGraphics[robotIndex] = new Graphics2DSerialized();
			robotGraphics[robotIndex].setPaintingEnabled(true);
		}
		return robotGraphics[robotIndex];
	}

	private static final class IntPair {
		public final int a;
		public final int b;

		private IntPair(int a, int b) {
			this.a = a;
			this.b = b;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			IntPair intPair = (IntPair) o;
			return a == intPair.a &&
				b == intPair.b;
		}

		@Override
		public int hashCode() {
			int result = 1;

			result = 31 * result + a;
			result = 31 * result + b;

			return result;
		}
	}

	private void drawBullets(Graphics2D g, ITurnSnapshot snapShot, ITurnSnapshot lastSnapshot, double t) {
		HashMap<IntPair, IBulletSnapshot> last = null;
		if (t != 1.) {
			last = new HashMap<IntPair, IBulletSnapshot>();
			if (lastSnapshot != null) {
				for (IBulletSnapshot bullet : lastSnapshot.getBullets()) {
					last.put(new IntPair(bullet.getOwnerIndex(), bullet.getBulletId()), bullet);
				}
			}
		}

		IntObjectHashMap robotMap = null;

		final Shape savedClip = g.getClip();

		g.setClip(null);

		double x, y;

		for (IBulletSnapshot bulletSnapshot : snapShot.getBullets()) {
			double bx = bulletSnapshot.getPaintX();
			double by = bulletSnapshot.getPaintY();

			if (t != 1.) {
				if (bulletSnapshot.getBulletId() == 0) {
					if (robotMap == null) {
						robotMap = getRobotMap(lastSnapshot);
					}

					IRobotSnapshot l = (IRobotSnapshot) robotMap.get(bulletSnapshot.getOwnerIndex());
					if (l != null) {
						bx = l.getX() * (1. - t) + bx * t;
						by = l.getY() * (1. - t) + by * t;
					}
				} else {
					IBulletSnapshot l = last.get(new IntPair(bulletSnapshot.getOwnerIndex(), bulletSnapshot.getBulletId()));
					if (l != null) {
						bx = l.getPaintX() * (1. - t) + bx * t;
						by = l.getPaintY() * (1. - t) + by * t;
					}
				}
			}

			x = bx;
			y = battleField.getHeight() - by;

			AffineTransform at = AffineTransform.getTranslateInstance(x, y);

			if (bulletSnapshot.getState().isActive()) {

				// radius = sqrt(x^2 / 0.1 * power), where x is the width of 1 pixel for a minimum 0.1 bullet
				double scale = max(2 * sqrt(2.5 * bulletSnapshot.getPower()), 2 / this.scale);

				at.scale(scale, scale);
				Area bulletArea = BULLET_AREA.createTransformedArea(at);

				Color bulletColor;

				if (properties.getOptionsRenderingForceBulletColor()) {
					bulletColor = Color.WHITE;
				} else {
					bulletColor = new Color(bulletSnapshot.getColor());
				}
				g.setColor(bulletColor);
				g.fill(bulletArea);

			} else if (drawExplosions) {
				int explosionIndex = bulletSnapshot.getExplosionImageIndex();
				int frame = bulletSnapshot.getFrame();

				// Sanity check to avoid bug-354 - Replaying an XML record can cause an ArrayIndexOutOfBoundsException
				if (explosionIndex >= 0 && frame >= 0) {
					if (!bulletSnapshot.isExplosion()) {
						double scale = sqrt(1000 * bulletSnapshot.getPower()) / 128;
						at.scale(scale, scale);
					}
					RenderObject explosionRenderImage = imageManager.getExplosionRenderImage(explosionIndex, frame);
					explosionRenderImage.setTransform(at);
					explosionRenderImage.paint(g);
				}
			}
		}
		g.setClip(savedClip);
	}

	private void centerString(Graphics2D g, String s, float x, float y, Font font, FontMetrics fm) {
		g.setFont(font);

		int width = fm.stringWidth(s);
		int height = fm.getHeight();
		int descent = fm.getDescent();

		float left = x - width * .5f;
		float top = y - height * .5f;

		float scaledViewWidth = getWidth() / (float) scale;
		float scaledViewHeight = getHeight() / (float) scale;

		float borderWidth = (scaledViewWidth - battleField.getWidth()) * .5f;
		float borderHeight = (scaledViewHeight - battleField.getHeight()) * .5f;

		if (left + width > scaledViewWidth - borderWidth) {
			left = scaledViewWidth - borderWidth - width;
		}
		if (top + height > scaledViewHeight - borderHeight) {
			top = scaledViewHeight - borderHeight - height;
		}
		if (left < -borderWidth) {
			left = -borderWidth;
		}
		if (top < -borderHeight) {
			top = -borderHeight;
		}
		g.drawString(s, left, top + height - descent);
	}

	private void drawScanArc(Graphics2D g, IRobotSnapshot robotSnapshot) {
		Arc2D.Double scanArc = (Arc2D.Double) ((RobotSnapshot) robotSnapshot).getScanArc();

		if (scanArc == null) {
			return;
		}

		final Composite savedComposite = g.getComposite();

		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));

		scanArc.setAngleStart((360 - scanArc.getAngleStart() - scanArc.getAngleExtent()) % 360);
		scanArc.y = battleField.getHeight() - robotSnapshot.getY() - Rules.RADAR_SCAN_RADIUS;

		int scanColor = robotSnapshot.getScanColor();

		g.setColor(new Color(scanColor, true));

		if (abs(scanArc.getAngleExtent()) >= .5) {
			g.fill(scanArc);
		} else {
			g.draw(scanArc);
		}

		g.setComposite(savedComposite);
	}

	private void paintRobocodeLogo(Graphics2D g) {
		g.setBackground(LOGO_BG_COLOR);
		g.clearRect(0, 0, getWidth(), getHeight());

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g.transform(AffineTransform.getTranslateInstance((getWidth() - 320) / 2.0, (getHeight() - 46) / 2.0));
		g.setColor(new Color(0, 0x40, 0));
		g.fill(robocodeTextPath);

		Font font = new Font("Dialog", Font.BOLD, 14);
		int width = g.getFontMetrics(font).stringWidth(ROBOCODE_SLOGAN);

		g.setTransform(new AffineTransform());
		g.setFont(font);
		g.setColor(new Color(0, 0x50, 0));
		g.drawString(ROBOCODE_SLOGAN, (float) ((getWidth() - width) / 2.0), (float) (getHeight() / 2.0 + 50));
	}

	private long frameCount = 0L;

	private static boolean eq(double a, double b) {
		return eqZero(a - b);
	}

	private static boolean eqZero(double a) {
		return -0.1 < a && a < 0.1;
	}

	private class MyPanel extends JPanel {
		private double lastDesiredTPS = 0.;
		private long timeLastPaint = -1;

		@Override
		public void paint(Graphics g0) {
			Graphics2D g = (Graphics2D) g0;

			if (timeLastPaint == -1) {
				warmPaint(g, imageManager.getExplosionDebriseRenderImage());
				warmPaint(g, imageManager.getExplosionRenderImage(1, 1));
			}

			long now = System.nanoTime();
			long delta = timeLastPaint == -1 ? 0 : now - timeLastPaint;
			timeLastPaint = now;

			fpsGraph.recordFrameDelta(delta);

			double desiredTPS = properties.getOptionsBattleDesiredTPS();

			if (lastDesiredTPS != desiredTPS) {
				if (lastDesiredTPS != 0) {
					int mod0 = (int) Math.floor(60 / lastDesiredTPS + 0.001);
					int mod1 = (int) Math.floor(60 / desiredTPS + 0.001);

					frameCount = Math.round((double) frameCount / mod0 * mod1);
				}
				lastDesiredTPS = desiredTPS;
			}

			boolean updated = false;

			int mod = 1;
			if (eq(desiredTPS, 30)) {
				mod = 2;
				if ((frameCount & 1) == 0) {
					updated = windowManager.pollSnapshot();
				}
			} else if (desiredTPS >= 59.9) {
				updated = windowManager.pollSnapshot();
			} else {
				mod = (int) Math.floor(60 / desiredTPS + 0.001);
				if (mod == 0) {
					throw new IllegalStateException();
				} else {
					if ((frameCount % mod) == 0) {
						updated = windowManager.pollSnapshot();
					}
				}
			}

			if (updated) {
				frameCount = 0L;
			}
			frameCount += 1L;

			// windowManager.pollSnapshot();

			final ITurnSnapshot lastSnapshot = windowManager.getLastSnapshot();
			final ITurnSnapshot lastLastSnapshot = windowManager.getLastLastSnapshot();
			if (lastSnapshot != null) {
				update(lastSnapshot, lastLastSnapshot, g, Math.min(1., 1. * frameCount / mod));
			} else {
				paintRobocodeLogo(g);
			}

			fpsGraph.paint(g);
		}

		private void update(ITurnSnapshot snapshot, ITurnSnapshot lastSnapshot, Graphics g, double t) {
			if (!initialized) {
				initialize();
			}

			if (windowManager.isIconified() || !isDisplayable() || (getWidth() <= 0) || (getHeight() <= 0)) {
				return;
			}

			drawBattle((Graphics2D) g, snapshot, lastSnapshot, t);
		}
	}

	private void warmPaint(Graphics2D g, RenderObject image) {
		image.setTransform(AffineTransform.getTranslateInstance(0, 0));
		image.paint(g);
	}

	private class BattleObserver extends BattleAdaptor {
		public BattleObserver(IWindowManager windowManager) {
			windowManager.addBattleListener(this);
		}

		@Override
		public void onBattleStarted(BattleStartedEvent event) {
			frameCount = 0L;

			battleRules = event.getBattleRules();

			battleField = new BattleField(battleRules.getBattlefieldWidth(), battleRules.getBattlefieldHeight());

			initialized = false;
			setVisible(true);

			super.onBattleStarted(event);

			robotGraphics = new IGraphicsProxy[event.getRobotsCount()];
		}

		@Override
		public void onBattleFinished(BattleFinishedEvent event) {
			frameCount = 0L;

			super.onBattleFinished(event);
			robotGraphics = null;
		}

		public void onTurnEnded(final TurnEndedEvent event) {
		}
	}
}
