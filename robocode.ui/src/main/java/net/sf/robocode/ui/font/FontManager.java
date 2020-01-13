package net.sf.robocode.ui.font;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Xor (original)
 */
public final class FontManager implements IFontManager {
	private static final String[] MONOSPACED = {"Consolas", "Monaco", Font.MONOSPACED};

	private boolean initialized = false;
	private String monospacedFont = null;

	public void init() {
		if (initialized) return;

		Set<String> fonts = new HashSet<String>(Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));

		monospacedFont = getFontName(fonts, MONOSPACED);

		initialized = true;
	}

	@Override
	public String getMonospacedFont() {
		if (!initialized) init();
		return monospacedFont;
	}

	private static String getFontName(Set<String> fonts, String[] fontNames) {
		String fontName = null;
		for (String s : fontNames) {
			fontName = s;
			if (fonts.contains(s)) {
				break;
			}
		}
		return fontName;
	}

	public static void main(String[] args) {
		System.out.println(new FontManager().getMonospacedFont());
	}
}
