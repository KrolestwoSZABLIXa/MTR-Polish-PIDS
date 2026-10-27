package pl.szablix.mtrpolishpids.block.entity;

import pl.szablix.mtrpolishpids.MTRPolishPIDS;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared geometric calculations for Polish road signs E-17a and E-18a.
 * Used identically by both block entities, collision hitboxes, and renderers
 * to ensure 100% exact alignment between models and hitboxes.
 */
public class CitySignGeometry {

	private static final Map<String, Float> HALF_WIDTH_CACHE = new ConcurrentHashMap<>();
	private static Font FONT_DROGOWSKAZ;

	static {
		try (InputStream is = CitySignGeometry.class.getResourceAsStream("/assets/mtr_polish_pack/font/drogowskaz.ttf")) {
			if (is != null) {
				FONT_DROGOWSKAZ = Font.createFont(Font.TRUETYPE_FONT, is);
			} else {
				throw new IllegalArgumentException("Drogowskaz font resource not found");
			}
		} catch (Exception e) {
			MTRPolishPIDS.LOGGER.error("Failed to load native Drogowskaz font for CitySignGeometry", e);
			FONT_DROGOWSKAZ = new Font(Font.SANS_SERIF, Font.BOLD, 125);
		}
	}

	public static Font getFont() {
		return FONT_DROGOWSKAZ;
	}

	public static int getPixelWidth(String rawName) {
		final String name = (rawName == null || rawName.trim().isEmpty()) ? "Warszawa" : rawName.trim();
		final float fontSize = 125f;
		final Font font = FONT_DROGOWSKAZ.deriveFont(Font.PLAIN, fontSize);

		int textWidth;
		try {
			final BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D gProbe = probe.createGraphics();
			gProbe.setFont(font);
			final FontMetrics fm = gProbe.getFontMetrics();
			textWidth = fm.stringWidth(name);
			gProbe.dispose();
		} catch (Exception e) {
			textWidth = name.length() * 80;
		}

		final int paddingX = 85;
		int width = Math.max(450, textWidth + paddingX * 2);
		if ((width % 2) != 0) {
			width++; // ensure even pixel dimension
		}
		return width;
	}

	/**
	 * Calculates the half-width of the sign in Minecraft world blocks
	 * using exact font metrics of the native Drogowskaz font.
	 */
	public static float getSignHalfWidth(String rawName) {
		final String name = (rawName == null || rawName.trim().isEmpty()) ? "Warszawa" : rawName.trim();
		final Float cached = HALF_WIDTH_CACHE.get(name);
		if (cached != null) {
			return cached;
		}

		final int width = getPixelWidth(name);
		final float worldWidth = (width / 256.0f) * 0.80f;
		final float halfWidth = worldWidth / 2.0f;
		HALF_WIDTH_CACHE.put(name, halfWidth);
		return halfWidth;
	}

	/**
	 * Computes dynamic post offset from center in world blocks.
	 */
	public static float getPostOffset(float maxHalfW) {
		return Math.max(0.38f, maxHalfW * 0.65f);
	}
}
