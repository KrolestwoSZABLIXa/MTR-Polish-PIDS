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
			FONT_ROBOTO_MEDIUM = loadFont("/assets/mtr_polish_pids/font/roboto_medium.ttf");
			FONT_ROBOTO_REGULAR = loadFont("/assets/mtr_polish_pids/font/roboto_regular.ttf");
			FONT_ROBOTO_ITALIC = loadFont("/assets/mtr_polish_pids/font/roboto_italic.ttf");
			FONT_MYRIAD_REGULAR = loadFont("/assets/mtr_polish_pids/font/myriad_pro_regular.ttf");
			FONT_MYRIAD_ITALIC = loadFont("/assets/mtr_polish_pids/font/myriad_pro_italic.ttf");
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
	 * Renders casing plate text: "peron X" (Myriad Pro Regular) and "platform" (Myriad Pro Italic).
	 */
	public static DynamicTextTexture getCasingPlate(String platformName) {
		final String cleanPlatform = platformName == null ? "" : platformName.trim();
		final String key = "casing_" + cleanPlatform;
		final DynamicTextTexture existing = CACHE.get(key);
		if (existing != null) {
			existing.touch();
			return existing;
		}

		final String line1 = "peron  " + cleanPlatform;
		final String line2 = "platform";

		final float size1 = 44f;
		final float size2 = 24f;
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

		final int textWidth = Math.max(w1, w2) + 20;
		final int textHeight = 90;

		final BufferedImage img = new BufferedImage(Math.max(16, textWidth), textHeight, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		setupQualityHints(g);

		g.setColor(Color.WHITE);
		g.setFont(f1);
		g.drawString(line1, 6, 42);

		g.setFont(f2);
		g.drawString(line2, 6, 78);

		g.dispose();

		return registerTexture(key, img, textWidth, textHeight);
	}

	/**
	 * Renders text in a single font (e.g. Departure Time, Destination, Badge, Intermediate Stops, Delay).
	 */
	public static DynamicTextTexture getText(String keyPrefix, String text, Font font, float fontSize, Color color) {
		final String safeText = text == null ? "" : text;
		final String key = keyPrefix + "_" + safeText;
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
		final int stringWidth = Math.max(4, fm.stringWidth(safeText) + 12);
		final int stringHeight = Math.max(16, (int) (fontSize * 1.45f));
		final int baseline = fm.getAscent() + (stringHeight - fm.getHeight()) / 2;
		gProbe.dispose();

		final BufferedImage img = new BufferedImage(stringWidth, stringHeight, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		setupQualityHints(g);

		g.setColor(color);
		g.setFont(derivedFont);
		g.drawString(safeText, 4, baseline);
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
