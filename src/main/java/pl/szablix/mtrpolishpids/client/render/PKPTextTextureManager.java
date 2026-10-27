package pl.szablix.mtrpolishpids.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import pl.szablix.mtrpolishpids.MTRPolishPIDS;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ultra-high-performance Java2D text texture generator and cache for PKP PLK PIDS displays.
 * Renders authentic OpenType typography (Roboto & Myriad Pro) with subpixel anti-aliasing,
 * native HarfBuzz kerning, and zero character-spacing distortion.
 */
public class PKPTextTextureManager {

	private static final Map<String, DynamicTextTexture> CACHE = new ConcurrentHashMap<>();

	private static Font FONT_ROBOTO_MEDIUM;
	private static Font FONT_ROBOTO_REGULAR;
	private static Font FONT_ROBOTO_ITALIC;
	private static Font FONT_MYRIAD_REGULAR;
	private static Font FONT_MYRIAD_ITALIC;

	static {
		try {
			FONT_ROBOTO_MEDIUM = loadFont("/assets/mtr_polish_pack/font/roboto_medium.ttf");
			FONT_ROBOTO_REGULAR = loadFont("/assets/mtr_polish_pack/font/roboto_regular.ttf");
			FONT_ROBOTO_ITALIC = loadFont("/assets/mtr_polish_pack/font/roboto_italic.ttf");
			FONT_MYRIAD_REGULAR = loadFont("/assets/mtr_polish_pack/font/myriad_pro_regular.ttf");
			FONT_MYRIAD_ITALIC = loadFont("/assets/mtr_polish_pack/font/myriad_pro_italic.ttf");
		} catch (Exception e) {
			MTRPolishPIDS.LOGGER.error("Failed to load native TrueType fonts for Java2D PIDS renderer", e);
			FONT_ROBOTO_MEDIUM = new Font(Font.SANS_SERIF, Font.BOLD, 36);
			FONT_ROBOTO_REGULAR = new Font(Font.SANS_SERIF, Font.PLAIN, 36);
			FONT_ROBOTO_ITALIC = new Font(Font.SANS_SERIF, Font.ITALIC, 36);
			FONT_MYRIAD_REGULAR = new Font(Font.SANS_SERIF, Font.PLAIN, 36);
			FONT_MYRIAD_ITALIC = new Font(Font.SANS_SERIF, Font.ITALIC, 36);
		}
	}

	private static Font loadFont(String path) throws Exception {
		try (InputStream is = PKPTextTextureManager.class.getResourceAsStream(path)) {
			if (is == null) {
				throw new IllegalArgumentException("Font resource not found: " + path);
			}
			return Font.createFont(Font.TRUETYPE_FONT, is);
		}
	}

	public static class DynamicTextTexture {
		public final Identifier identifier;
		public final NativeImageBackedTexture texture;
		public final int textureWidth;
		public final int textureHeight;
		public final float textWidthPixels;
		public final float textHeightPixels;
		private long lastUsedTime;

		public DynamicTextTexture(Identifier identifier, NativeImageBackedTexture texture, int textureWidth, int textureHeight, float textWidthPixels, float textHeightPixels) {
			this.identifier = identifier;
			this.texture = texture;
			this.textureWidth = textureWidth;
			this.textureHeight = textureHeight;
			this.textWidthPixels = textWidthPixels;
			this.textHeightPixels = textHeightPixels;
			this.lastUsedTime = System.currentTimeMillis();
		}

		public void touch() {
			this.lastUsedTime = System.currentTimeMillis();
		}

		public float getAspectRatio() {
			return textureHeight == 0 ? 1f : (float) textureWidth / (float) textureHeight;
		}
	}

	/**
	 * Extracts the first platform number/identifier before space, dot, slash, etc. (e.g. "1 tor 1" -> "1", "1.1" -> "1").
	 */
	public static String parsePlatformNumber(String raw) {
		if (raw == null) {
			return "";
		}
		final String trimmed = raw.trim();
		if (trimmed.isEmpty()) {
			return "";
		}
		final String firstToken = trimmed.split("[\\s.,/\\\\_\\-]+")[0];
		final java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(\\d+[a-zA-Z]?)").matcher(firstToken);
		if (m.find()) {
			return m.group(1);
		}
		final java.util.regex.Matcher anyNum = java.util.regex.Pattern.compile("(\\d+[a-zA-Z]?)").matcher(trimmed);
		if (anyNum.find()) {
			return anyNum.group(1);
		}
		return firstToken;
	}

	/**
	 * Renders casing plate text: "peron X" (Myriad Pro Regular) and "platform" (Myriad Pro Italic).
	 */
	public static DynamicTextTexture getCasingPlate(String platformName) {
		final String cleanPlatform = parsePlatformNumber(platformName);
		final String key = "casing_v4_" + cleanPlatform;
		final DynamicTextTexture existing = CACHE.get(key);
		if (existing != null) {
			existing.touch();
			return existing;
		}

		final String line1 = cleanPlatform.isEmpty() ? "peron" : "peron  " + cleanPlatform;
		final String line2 = "platform";

		final float size1 = 90f;
		final float size2 = 45f;
		final Font f1 = FONT_MYRIAD_REGULAR.deriveFont(Font.PLAIN, size1);
		final Font f2 = FONT_MYRIAD_ITALIC.deriveFont(Font.PLAIN, size2);

		final BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D gProbe = probe.createGraphics();
		gProbe.setFont(f1);
		final FontMetrics fm1 = gProbe.getFontMetrics();
		final int w1 = fm1.stringWidth(line1);

		gProbe.setFont(f2);
		final FontMetrics fm2 = gProbe.getFontMetrics();
		final int w2 = fm2.stringWidth(line2);
		gProbe.dispose();

		final int padX = 8;
		final int padY = 2;
		final int base1 = padY + fm1.getAscent();
		final int gap = 6;
		final int base2 = base1 + fm1.getDescent() + gap + fm2.getAscent();
		final int totalH = base2 + fm2.getDescent() + padY;
		final int totalW = Math.max(w1, w2) + padX * 2;

		final BufferedImage img = new BufferedImage(Math.max(16, totalW), totalH, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		setupQualityHints(g);

		g.setColor(Color.WHITE);
		g.setFont(f1);
		g.drawString(line1, padX, base1);

		g.setFont(f2);
		g.drawString(line2, padX, base2);

		g.dispose();

		return registerTexture(key, img, totalW, totalH);
	}

	/**
	 * Renders train category / line badge with an integrated solid white background and bold black text.
	 * Drawing the white background directly on the badge texture guarantees it can NEVER disappear
	 * or separate from the text at glancing viewing angles.
	 */
	public static DynamicTextTexture getTrainBadge(String lineName) {
		final String safeText = lineName == null ? "" : lineName.trim();
		final String key = "badge_v6_" + safeText;
		final DynamicTextTexture existing = CACHE.get(key);
		if (existing != null) {
			existing.touch();
			return existing;
		}

		final float fontSize = 46f;
		final Font font = FONT_ROBOTO_MEDIUM.deriveFont(Font.PLAIN, fontSize);

		final BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D gProbe = probe.createGraphics();
		gProbe.setFont(font);
		final FontMetrics fm = gProbe.getFontMetrics();
		final int textWidth = fm.stringWidth(safeText);
		final int textAscent = fm.getAscent();
		final int textDescent = fm.getDescent();
		gProbe.dispose();

		final int padX = 14;
		final int padY = 6;
		final int badgeH = textAscent + textDescent + padY * 2;
		final int badgeW = Math.max(badgeH * 2, textWidth + padX * 2);

		final BufferedImage img = new BufferedImage(badgeW, badgeH, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		setupQualityHints(g);

		// Solid white badge background
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, badgeW, badgeH);

		// Bold black line/category text centered
		g.setColor(Color.BLACK);
		g.setFont(font);
		final int textX = (badgeW - textWidth) / 2;
		final int baseline = padY + textAscent;
		g.drawString(safeText, textX, baseline);
		g.dispose();

		return registerTexture(key, img, badgeW, badgeH);
	}

	/**
	 * Renders text in a single font (e.g. Departure Time, Destination, Badge, Intermediate Stops, Delay).
	 */
	public static DynamicTextTexture getText(String keyPrefix, String text, Font font, float fontSize, Color color) {
		final String safeText = text == null ? "" : text;
		final String key = keyPrefix + "_" + (int) fontSize + "_" + safeText;
		final DynamicTextTexture existing = CACHE.get(key);
		if (existing != null) {
			existing.touch();
			return existing;
		}

		final Font derivedFont = font.deriveFont(Font.PLAIN, fontSize);
		final BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D gProbe = probe.createGraphics();
		gProbe.setFont(derivedFont);
		final FontMetrics fm = gProbe.getFontMetrics();
		final int padX = 6;
		final int stringWidth = Math.max(4, fm.stringWidth(safeText) + padX * 2);
		final int padY = 2;
		final int stringHeight = Math.max(16, fm.getAscent() + fm.getDescent() + padY * 2);
		final int baseline = padY + fm.getAscent();
		gProbe.dispose();

		final BufferedImage img = new BufferedImage(stringWidth, stringHeight, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		setupQualityHints(g);

		g.setColor(color);
		g.setFont(derivedFont);
		g.drawString(safeText, padX, baseline);
		g.dispose();

		return registerTexture(key, img, stringWidth, stringHeight);
	}

	public static Font getRobotoMedium() {
		return FONT_ROBOTO_MEDIUM;
	}

	public static Font getRobotoRegular() {
		return FONT_ROBOTO_REGULAR;
	}

	public static Font getRobotoItalic() {
		return FONT_ROBOTO_ITALIC;
	}

	public static Font getMyriadRegular() {
		return FONT_MYRIAD_REGULAR;
	}

	public static Font getMyriadItalic() {
		return FONT_MYRIAD_ITALIC;
	}

	private static void setupQualityHints(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
	}

	private static DynamicTextTexture registerTexture(String key, BufferedImage img, int width, int height) {
		final NativeImage nativeImage = new NativeImage(width, height, false);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				final int argb = img.getRGB(x, y);
				final int a = (argb >> 24) & 0xFF;
				if (a < 36) {
					nativeImage.setColor(x, y, 0);
					continue;
				}
				final int r = (argb >> 16) & 0xFF;
				final int g = (argb >> 8) & 0xFF;
				final int b = argb & 0xFF;
				// Convert to little-endian ABGR expected by Minecraft NativeImage
				final int abgr = (a << 24) | (b << 16) | (g << 8) | r;
				nativeImage.setColor(x, y, abgr);
			}
		}

		final NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
		texture.upload();
		texture.setFilter(true, false);

		final String safeId = key.toLowerCase()
				.replaceAll("[^a-z0-9_.-]", "_");
		final net.minecraft.util.Identifier mcId = MinecraftClient.getInstance().getTextureManager()
				.registerDynamicTexture("pkp_pids_" + safeId, texture);

		final Identifier mtrId = new Identifier(mcId.getNamespace(), mcId.getPath());
		final DynamicTextTexture result = new DynamicTextTexture(mtrId, texture, width, height, width, height);
		CACHE.put(key, result);

		// Clean up old entries if cache grows large
		if (CACHE.size() > 80) {
			cleanupCache();
		}

		return result;
	}

	private static void cleanupCache() {
		final long now = System.currentTimeMillis();
		CACHE.entrySet().removeIf(entry -> {
			if (now - entry.getValue().lastUsedTime > 90_000) { // 90 seconds unused
				try {
					final net.minecraft.util.Identifier mcId = new net.minecraft.util.Identifier(
							entry.getValue().identifier.getNamespace(),
							entry.getValue().identifier.getPath()
					);
					MinecraftClient.getInstance().getTextureManager().destroyTexture(mcId);
					entry.getValue().texture.close();
				} catch (Exception ignored) {}
				return true;
			}
			return false;
		});
	}
}
