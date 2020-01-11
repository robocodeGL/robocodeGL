/**
 * Copyright (c) 2001-2019 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://robocode.sourceforge.io/license/epl-v10.html
 */
package net.sf.robocode.ui;


import net.sf.robocode.settings.ISettingsManager;
import net.sf.robocode.ui.gfx.ImageAtlas;
import net.sf.robocode.ui.gfx.ImageUtil;
import net.sf.robocode.ui.gfx.RenderImage;
import net.sf.robocode.ui.gfx.RenderImageRegion;
import net.sf.robocode.ui.gfx.RenderObject;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Mathew A. Nelson (original)
 * @author Flemming N. Larsen (contributor)
 * @author Titus Chen (contributor)
 */
public class ImageManager implements IImageManager {
	private static final boolean USE_GL2_IMAGE = true;
	private static final double GL2_ROBOT_SCALE = .18;

	private final ISettingsManager properties;

	private Image[] groundImages;

	private RenderObject[][] explosionRenderImages;
	private RenderImage debriseRenderImage;

	private Image gl2RobotImage;
	private Image bodyImage;
	private Image gunImage;
	private Image radarImage;

	private static final int MAX_NUM_COLORS = 256;

	private HashMap<Integer, Image> gl2RobotImageCache;

	private HashMap<Integer, RenderObject> robotBodyImageCache;
	private HashMap<Integer, RenderObject> robotGunImageCache;
	private HashMap<Integer, RenderObject> robotRadarImageCache;

	public ImageManager(ISettingsManager properties) {
		this.properties = properties;
	}

	public void initialize() {
		// Note that initialize could be called in order to reset all images (image buffering)

		// Reset image cache
		groundImages = new Image[5];
		explosionRenderImages = null;
		debriseRenderImage = null;
		bodyImage = null;
		gunImage = null;
		radarImage = null;
		gl2RobotImageCache = new RenderCache<Integer, Image>();
		robotBodyImageCache = new RenderCache<Integer, RenderObject>();
		robotGunImageCache = new RenderCache<Integer, RenderObject>();
		robotRadarImageCache = new RenderCache<Integer, RenderObject>();

		// Read images into the cache
		getBodyImage();
		getGunImage();
		getRadarImage();
		getExplosionRenderImage(0, 0);
	}

	public Image getGroundTileImage(int index) {
		if (groundImages[index] == null) {
			groundImages[index] = getImage("/net/sf/robocode/ui/images/ground/blue_metal/blue_metal_" + index + ".png");
		}
		return groundImages[index];
	}

	public RenderObject getExplosionRenderImage(int which, int frame) {
		if (explosionRenderImages == null) {
			int numExplosion, numFrame;
			String filename;

			List<List<RenderObject>> explosions = new ArrayList<List<RenderObject>>();

			boolean done = false;

			ImageAtlas atlas;
			Image img;
			if (USE_GL2_IMAGE) {
				try {
					String name = "/net/sf/robocode/ui/images/gl2/explosions.png";
					URL url = ImageManager.class.getResource(name);
					if (url == null) {
						throw new IOException("Invalid: " + name);
					} else {
						img = ImageIO.read(url);
						// System.out.println("before Parse");
						atlas = ImageAtlas.parse("/net/sf/robocode/ui/images/gl2/explosions.atlas");
						// System.out.println("after Parse");
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			for (numExplosion = 1; !done; numExplosion++) {
				List<RenderObject> frames = new ArrayList<RenderObject>();

				for (numFrame = 1;; numFrame++) {
					if (USE_GL2_IMAGE) {
						ImageAtlas.Region region = atlas.findRegion("explosion" + numExplosion + '-' + numFrame);

						if (region == null) {
							if (numFrame == 1) {
								done = true;
							} else {
								explosions.add(frames);
							}
							break;
						}

						frames.add(region.toImageRegion(img, 1));
					} else {
						filename = "/net/sf/robocode/ui/images/explosion/explosion" + numExplosion + '-' + numFrame + ".png";

						if (ImageManager.class.getResource(filename) == null) {
							if (numFrame == 1) {
								done = true;
							} else {
								explosions.add(frames);
							}
							break;
						}

						frames.add(new RenderImage(getImage(filename)));
					}
				}
			}

			numExplosion = explosions.size();
			explosionRenderImages = new RenderObject[numExplosion][];

			for (int i = numExplosion - 1; i >= 0; i--) {
				explosionRenderImages[i] = explosions.get(i).toArray(new RenderObject[explosions.size()]);
			}
		}
		return explosionRenderImages[which][frame];
	}

	public RenderImage getExplosionDebriseRenderImage() {
		if (debriseRenderImage == null) {
			debriseRenderImage = new RenderImage(getImage("/net/sf/robocode/ui/images/ground/explode_debris.png"));
		}
		return debriseRenderImage;
	}

	private Image getImage(String filename) {
		Image image = ImageUtil.getImage(filename);

		if (properties.getOptionsRenderingBufferImages()) {
			image = ImageUtil.getBufferedImage(image);
		}
		return image;
	}


	private Image getGl2RobotImage() {
		if (gl2RobotImage == null) {
			gl2RobotImage = getImage("/net/sf/robocode/ui/images/gl2/robot.png");
		}
		return gl2RobotImage;
	}


	/**
	 * Gets the body image
	 * Loads from disk if necessary.
	 *
	 * @return the body image
	 */
	private Image getBodyImage() {
		if (bodyImage == null) {
			bodyImage = getImage("/net/sf/robocode/ui/images/body.png");
		}
		return bodyImage;
	}

	/**
	 * Gets the gun image
	 * Loads from disk if necessary.
	 *
	 * @return the gun image
	 */
	private Image getGunImage() {
		if (gunImage == null) {
			gunImage = getImage("/net/sf/robocode/ui/images/turret.png");
		}
		return gunImage;
	}

	/**
	 * Gets the radar image
	 * Loads from disk if necessary.
	 *
	 * @return the radar image
	 */
	private Image getRadarImage() {
		if (radarImage == null) {
			radarImage = getImage("/net/sf/robocode/ui/images/radar.png");
		}
		return radarImage;
	}

	private Image getColoredGl2RobotImage(Integer color) {
		Image img = gl2RobotImageCache.get(color);
		if (img == null) {
			img = ImageUtil.createColouredRobotImage(getGl2RobotImage(), new Color(color, true));
			gl2RobotImageCache.put(color, img);
		}
		return img;
	}

	public RenderObject getColoredBodyRenderImage(Integer color) {
		RenderObject img = robotBodyImageCache.get(color);
		if (img == null) {
			if (USE_GL2_IMAGE) {
				img = new RenderImageRegion(getColoredGl2RobotImage(color),
					2, 100, 205, 230, GL2_ROBOT_SCALE);
			} else {
				img = new RenderImage(ImageUtil.createColouredRobotImage(getBodyImage(), new Color(color, true)));
			}
			robotBodyImageCache.put(color, img);
		}
		return img;
	}

	public RenderObject getColoredGunRenderImage(Integer color) {
		RenderObject img = robotGunImageCache.get(color);

		if (img == null) {
			if (USE_GL2_IMAGE) {
				img = new RenderImageRegion(getColoredGl2RobotImage(color),
					209, 30, 121, 300, GL2_ROBOT_SCALE);
			} else {
				img = new RenderImage(ImageUtil.createColouredRobotImage(getGunImage(), new Color(color, true)));
			}
			robotGunImageCache.put(color, img);
		}
		return img;
	}

	public RenderObject getColoredRadarRenderImage(Integer color) {
		RenderObject img = robotRadarImageCache.get(color);

		if (img == null) {
			if (USE_GL2_IMAGE) {
				img = new RenderImageRegion(getColoredGl2RobotImage(color),
					2, 2, 151, 96, GL2_ROBOT_SCALE);
			} else {
				img = new RenderImage(ImageUtil.createColouredRobotImage(getRadarImage(), new Color(color, true)));
			}
			robotRadarImageCache.put(color, img);
		}
		return img;
	}

	/**
	 * Class used for caching rendered robot parts in various colors.
	 *
	 * @author Titus Chen
	 */
	@SuppressWarnings("serial")
	private static class RenderCache<K, V> extends LinkedHashMap<K, V> {

		/* Note about initial capacity:
		 * To avoid rehashing (inefficient though probably unavoidable), initial
		 * capacity must be at least 1 greater than the maximum capacity.
		 * However, initial capacities are set to the smallest power of 2 greater
		 * than or equal to the passed argument, resulting in 512 with this code.
		 * I was not aware of this before, but notice: the current implementation
		 * behaves similarly.  The simple solution would be to set maximum capacity
		 * to 255, but the problem with doing so is that in a battle of 256 robots
		 * of different colors, the net result would end up being real-time
		 * rendering due to the nature of access ordering.  However, 256 robot
		 * battles are rarely fought.
		 */
		private static final int INITIAL_CAPACITY = MAX_NUM_COLORS + 1;

		private static final float LOAD_FACTOR = 1;

		public RenderCache() {

			/* The "true" parameter needed for access-order:
			 * when cache fills, the least recently accessed entry is removed
			 */
			super(INITIAL_CAPACITY, LOAD_FACTOR, true);
		}

		@Override
		protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
			return size() > MAX_NUM_COLORS;
		}
	}
}
