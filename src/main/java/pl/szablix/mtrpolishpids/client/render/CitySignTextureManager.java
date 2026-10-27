package pl.szablix.mtrpolishpids.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import org.mtr.mapping.holder.Identifier;
import pl.szablix.mtrpolishpids.MTRPolishPIDS;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ultra-high-fidelity texture generator for Polish road signs E-17a (Miejscowość)
 * and E-18a (Koniec miejscowości). Uses authentic Drogowskaz TrueType typography
 * and exact geometric specifications according to Polish traffic regulations.
 */
public class CitySignTextureManager {

	private static final Map<String, DynamicCitySignTexture> CACHE = new ConcurrentHashMap<>();
	private static Font FONT_DROGOWSKAZ;

	static {
		try (InputStream is = CitySignTextureManager.class.getResourceAsStream("/assets/mtr_polish_pack/font/drogowskaz.ttf")) {
			if (is != null) {
				FONT_DROGOWSKAZ = Font.createFont(Font.TRUETYPE_FONT, is);
			} else {
				throw new IllegalArgumentException("Drogowskaz font resource not found");
			}
		} catch (Exception e) {
			MTRPolishPIDS.LOGGER.error("Failed to load native Drogowskaz font for City Sign renderer", e);
			FONT_DROGOWSKAZ = new Font(Font.SANS_SERIF, Font.BOLD, 125);
		}
	}

	public static class DynamicCitySignTexture {
		public final Identifier identifier;
		public final NativeImageBackedTexture texture;
		public final int textureWidth;
		public final int textureHeight;
		public final float worldWidth;
		private long lastUsedTime;

		public DynamicCitySignTexture(Identifier identifier, NativeImageBackedTexture texture, int textureWidth, int textureHeight, float worldWidth) {
			this.identifier = identifier;
			this.texture = texture;
			this.textureWidth = textureWidth;
			this.textureHeight = textureHeight;
			this.worldWidth = worldWidth;
			this.lastUsedTime = System.currentTimeMillis();
		}

		public void touch() {
			this.lastUsedTime = System.currentTimeMillis();
		}

		public float getAspectRatio() {
			return textureHeight == 0 ? 1f : (float) textureWidth / (float) textureHeight;
		}
	}

	public static DynamicCitySignTexture getSignTexture(String rawCityName, boolean isEndOfTown) {
		final String cityName = (rawCityName == null || rawCityName.trim().isEmpty()) ? "Warszawa" : rawCityName.trim();
		final String cacheKey = (isEndOfTown ? "e18a:" : "e17a:") + cityName;

		final DynamicCitySignTexture existing = CACHE.get(cacheKey);
		if (existing != null) {
			existing.touch();
			return existing;
		}

		final float fontSize = 125f;
		final Font font = FONT_DROGOWSKAZ.deriveFont(Font.PLAIN, fontSize);

		// 1. Measure text width with Drogowskaz font
		final BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D gProbe = probe.createGraphics();
		gProbe.setFont(font);
		final FontMetrics fm = gProbe.getFontMetrics();
		final int textWidth = fm.stringWidth(cityName);
		gProbe.dispose();

		// 2. Calculate sign dimensions
		final int height = 256;
		final int paddingX = 85;
		int width = Math.max(450, textWidth + paddingX * 2);
		if ((width % 2) != 0) {
			width++; // ensure even pixel dimension
		}

		final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		setupQualityHints(g);

		// 3. Green background (Polish road traffic green #009846)
		final Color green = new Color(0, 152, 70);
		final int cornerRadius = 36;
		g.setColor(green);
		g.fill(new RoundRectangle2D.Float(0, 0, width, height, cornerRadius, cornerRadius));

		// 4. White border (8px stroke, 12px inset from edge)
		final int borderInset = 12;
		final int borderWidth = width - borderInset * 2;
		final int borderHeight = height - borderInset * 2;
		final int innerCornerRadius = 24;
		g.setColor(Color.WHITE);
		g.setStroke(new BasicStroke(8f));
		g.draw(new RoundRectangle2D.Float(borderInset, borderInset, borderWidth, borderHeight, innerCornerRadius, innerCornerRadius));

		// 5. City Name text in white
		g.setFont(font);
		final int textX = (width - textWidth) / 2;
		final int textY = (height - fm.getHeight()) / 2 + fm.getAscent() - 2;
		g.drawString(cityName, textX, textY);

		// 6. Optional E-18a End of Town diagonal red strike-through bar (#d40000)
		if (isEndOfTown) {
			final Shape oldClip = g.getClip();
			g.clip(new RoundRectangle2D.Float(borderInset, borderInset, borderWidth, borderHeight, innerCornerRadius, innerCornerRadius));
			g.setColor(new Color(212, 0, 0));
			g.setStroke(new BasicStroke(24f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.drawLine(borderInset, height - borderInset, width - borderInset, borderInset);
			g.setClip(oldClip);
		}

		g.dispose();

		// 7. Dynamic width in Minecraft world blocks: 256px height = 0.80 block height (sleeker, realistic proportions)
		final float worldWidth = (width / 256.0f) * 0.80f;

		return registerTexture(cacheKey, img, width, height, worldWidth);
	}

	private static void setupQualityHints(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
	}

	private static DynamicCitySignTexture registerTexture(String key, BufferedImage img, int width, int height, float worldWidth) {
		final NativeImage nativeImage = new NativeImage(width, height, false);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				final int argb = img.getRGB(x, y);
				final int a = (argb >> 24) & 0xFF;
				if (a == 0) {
					nativeImage.setColor(x, y, 0);
					continue;
				}
				final int r = (argb >> 16) & 0xFF;
				final int g = (argb >> 8) & 0xFF;
				final int b = argb & 0xFF;
				final int abgr = (a << 24) | (b << 16) | (g << 8) | r;
				nativeImage.setColor(x, y, abgr);
			}
		}

		final NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
		texture.upload();
		texture.setFilter(true, false);

		final String safeId = key.toLowerCase().replaceAll("[^a-z0-9_.-]", "_");
		final net.minecraft.util.Identifier mcId = MinecraftClient.getInstance().getTextureManager()
				.registerDynamicTexture("city_sign_" + safeId, texture);

		final Identifier mtrId = new Identifier(mcId.getNamespace(), mcId.getPath());
		final DynamicCitySignTexture result = new DynamicCitySignTexture(mtrId, texture, width, height, worldWidth);
		CACHE.put(key, result);

		if (CACHE.size() > 60) {
			cleanupCache();
		}

		return result;
	}

	private static void cleanupCache() {
		final long now = System.currentTimeMillis();
		CACHE.entrySet().removeIf(entry -> {
			if (now - entry.getValue().lastUsedTime > 600_000) {
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
