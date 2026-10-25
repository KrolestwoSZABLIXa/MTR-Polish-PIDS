package pl.szablix.mtrpolishpids.client.render;

import pl.szablix.mtrpolishpids.MTRPolishPIDS;
import pl.szablix.mtrpolishpids.block.entity.BlockEntityPKPPIDS;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.mtr.core.data.SimplifiedRoute;
import org.mtr.core.data.SimplifiedRoutePlatform;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.core.tool.Utilities;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongCollection;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityRenderer;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.InitClient;
import org.mtr.mod.block.BlockPIDSHorizontalBase;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.ArrivalsCacheClient;
import org.mtr.mod.data.IGui;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

public class RenderPKPPIDS extends BlockEntityRenderer<BlockEntityPKPPIDS> implements IGui, Utilities {

	private static final float START_X = 0f;
	private static final float START_Y = 11.0f;
	private static final float START_Z = 6f;
	private static final boolean ROTATE_90 = true;
	private static final float SCALE = 16f;

	private static final Identifier TEXTURE_WHITE = new Identifier("mtr", "textures/block/white.png");
	private static final Identifier TEXTURE_CLOCK_FACE = new Identifier(MTRPolishPIDS.MOD_ID, "textures/block/pkp_clock_face.png");

	// Authentic PKP PLK Typography:
	// - Screen CSDIP displays: Roboto (Medium, Regular, Italic)
	// - Static Casing Plate (Ipi-1): Myriad Pro (Regular, Italic)
	private static final Identifier FONT_ROBOTO_MEDIUM = new Identifier(MTRPolishPIDS.MOD_ID, "roboto_medium");
	private static final Identifier FONT_ROBOTO_REGULAR = new Identifier(MTRPolishPIDS.MOD_ID, "roboto_regular");
	private static final Identifier FONT_ROBOTO_ITALIC = new Identifier(MTRPolishPIDS.MOD_ID, "roboto_italic");
	private static final Identifier FONT_MYRIAD_REGULAR = new Identifier(MTRPolishPIDS.MOD_ID, "myriad_pro_regular");
	private static final Identifier FONT_MYRIAD_ITALIC = new Identifier(MTRPolishPIDS.MOD_ID, "myriad_pro_italic");

	private static final float SCROLL_SPEED = 2.2f;

	// Ultra-flush layer offset to prevent z-fighting while appearing fully glued to the glass surface
	private static final float LAYER_OFFSET = 0.0001f;

	// Colors directly from the user's JSON specifications
	private static final int COLOR_SCREEN_BG = 0xFF08080C;  // Authentic PKP deep black screen glass
	private static final int COLOR_BADGE_BG = 0xFF00075E;   // color: 1886
	private static final int COLOR_RED_LINE = 0xFFFF0000;   // color: 16711680
	private static final int COLOR_WHITE = 0xFFFFFFFF;      // color: 16777215
	private static final int COLOR_BLACK = 0xFF000000;      // color: 0
	private static final int COLOR_CLOCK_HAND = 0xFF141824;

	private static final Field FIELD_MATRIX_STACK;

	static {
		Field ms = null;
		try {
			for (Field f : GraphicsHolder.class.getDeclaredFields()) {
				if (f.getType().equals(MatrixStack.class)) {
					ms = f;
					ms.setAccessible(true);
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		FIELD_MATRIX_STACK = ms;
	}

	public RenderPKPPIDS(Argument dispatcher) {
		super(dispatcher);
	}

	@Override
	public void render(BlockEntityPKPPIDS entity, float tickDelta, GraphicsHolder graphicsHolder, int light, int overlay) {
		final World world = entity.getWorld2();
		if (world == null) {
			return;
		}

		final BlockPos blockPos = entity.getPos2();
		if (!entity.canStoreData.test(world, blockPos)) {
			return;
		}

		final Direction facing = IBlock.getStatePropertySafe(world, blockPos, DirectionHelper.FACING);

		if (entity.getPlatformIds().isEmpty()) {
			final LongArrayList platformIds = new LongArrayList();
			InitClient.findClosePlatform(entity.getPos2().down(4), 5, platform -> platformIds.add(platform.getId()));
			getArrivalsAndRender(entity, blockPos, facing, platformIds);
		} else {
			getArrivalsAndRender(entity, blockPos, facing, entity.getPlatformIds());
		}
	}

	private void getArrivalsAndRender(BlockEntityPKPPIDS entity, BlockPos blockPos, Direction facing, LongCollection platformIds) {
		final ObjectArrayList<ArrivalResponse> arrivalResponseList = ArrivalsCacheClient.INSTANCE.requestArrivals(platformIds);

		// 1. Quads (Rectangles and Clock Hands) on TEXTURE_WHITE in LIGHT layer
		MainRenderer.scheduleRender(TEXTURE_WHITE, false, QueuedRenderLayer.LIGHT, (graphicsHolder, offset) -> {
			renderSideQuads(entity, blockPos, facing, arrivalResponseList, graphicsHolder, offset, false);
			if (entity instanceof BlockPIDSHorizontalBase.BlockEntityHorizontalBase) {
				renderSideQuads(entity, blockPos.offset(facing), facing.getOpposite(), arrivalResponseList, graphicsHolder, offset, true);
			}
		});

		// 2. Clock Face on TEXTURE_CLOCK_FACE in LIGHT layer
		MainRenderer.scheduleRender(TEXTURE_CLOCK_FACE, false, QueuedRenderLayer.LIGHT, (graphicsHolder, offset) -> {
			renderSideClockFace(entity, blockPos, facing, graphicsHolder, offset, false);
			if (entity instanceof BlockPIDSHorizontalBase.BlockEntityHorizontalBase) {
				renderSideClockFace(entity, blockPos.offset(facing), facing.getOpposite(), graphicsHolder, offset, true);
			}
		});

		// 3. Text on QueuedRenderLayer.TEXT
		MainRenderer.scheduleRender(QueuedRenderLayer.TEXT, (graphicsHolder, offset) -> {
			renderSideText(entity, blockPos, facing, arrivalResponseList, graphicsHolder, offset, false);
			if (entity instanceof BlockPIDSHorizontalBase.BlockEntityHorizontalBase) {
				renderSideText(entity, blockPos.offset(facing), facing.getOpposite(), arrivalResponseList, graphicsHolder, offset, true);
			}
		});
	}

	private void renderSideQuads(BlockEntityPKPPIDS entity, BlockPos blockPos, Direction facing, ObjectArrayList<ArrivalResponse> arrivalResponseList, GraphicsHolder graphicsHolder, Vector3d offset, boolean isBackSide) {
		final boolean clockConfigRight = entity.isClockOnRight();
		final boolean isClockOnRight = isBackSide ? !clockConfigRight : clockConfigRight;

		final float boxWidth = 23.5f;
		final float displayX = isClockOnRight ? 2.0f : 6.50f;
		final float clockCenterX = isClockOnRight ? 28.25f : 3.75f;
		final float clockCenterY = 5.0f;
		final float clockRadius = 1.90f;

		graphicsHolder.push();
		graphicsHolder.translate(blockPos.getX() - offset.getXMapped() + 0.5, blockPos.getY() - offset.getYMapped(), blockPos.getZ() - offset.getZMapped() + 0.5);
		graphicsHolder.rotateYDegrees((ROTATE_90 ? 90 : 0) - facing.asRotation());
		graphicsHolder.rotateZDegrees(180);
		graphicsHolder.translate((START_X - 8) / 16, -START_Y / 16, (START_Z - 8) / 16);
		graphicsHolder.scale(1 / SCALE, 1 / SCALE, 1);

		// Layer 0: Screen background
		graphicsHolder.push();
		graphicsHolder.translate(0, 0, -LAYER_OFFSET * 0);
		IDrawing.drawTexture(graphicsHolder, 0.2f, 0.75f, 0, 31.8f, 8.375f, 0, Direction.UP, COLOR_SCREEN_BG, GraphicsHolder.getDefaultLight());
		graphicsHolder.pop();

		// Layer 1: Blue arrival badge: w = 23.5, h = 4.35 (vertically centered with clock at Y = 5.0)
		graphicsHolder.push();
		graphicsHolder.translate(0, 0, -LAYER_OFFSET * 1);
		IDrawing.drawTexture(graphicsHolder, displayX, 2.85f, 0, displayX + boxWidth, 7.20f, 0, Direction.UP, COLOR_BADGE_BG, GraphicsHolder.getDefaultLight());
		graphicsHolder.pop();

		// Layer 2: White box under time for line name & red accent line
		graphicsHolder.push();
		graphicsHolder.translate(0, 0, -LAYER_OFFSET * 2);
		IDrawing.drawTexture(graphicsHolder, displayX, 5.65f, 0, displayX + 5.125f, 6.45f, 0, Direction.UP, COLOR_WHITE, GraphicsHolder.getDefaultLight());
		IDrawing.drawTexture(graphicsHolder, displayX, 6.55f, 0, displayX + boxWidth, 7.20f, 0, Direction.UP, COLOR_RED_LINE, GraphicsHolder.getDefaultLight());
		graphicsHolder.pop();

		// Layer 4: Real-time clock hands
		final LocalTime now = LocalTime.now();
		final float hours = now.getHour() % 12f;
		final float minutes = now.getMinute();
		final float seconds = now.getSecond();
		final float hourAngle = (hours + minutes / 60f + seconds / 3600f) * 30f;
		final float minuteAngle = (minutes + seconds / 60f) * 6f;

		graphicsHolder.push();
		graphicsHolder.translate(0, 0, -LAYER_OFFSET * 4);

		// Hour hand
		graphicsHolder.push();
		graphicsHolder.translate(clockCenterX, clockCenterY, 0);
		graphicsHolder.rotateZDegrees(hourAngle);
		IDrawing.drawTexture(graphicsHolder, -0.11f, -clockRadius * 0.55f, 0, 0.11f, clockRadius * 0.12f, 0, Direction.UP, COLOR_CLOCK_HAND, GraphicsHolder.getDefaultLight());
		graphicsHolder.pop();

		// Minute hand
		graphicsHolder.push();
		graphicsHolder.translate(clockCenterX, clockCenterY, 0);
		graphicsHolder.rotateZDegrees(minuteAngle);
		IDrawing.drawTexture(graphicsHolder, -0.08f, -clockRadius * 0.82f, 0, 0.08f, clockRadius * 0.12f, 0, Direction.UP, COLOR_CLOCK_HAND, GraphicsHolder.getDefaultLight());
		graphicsHolder.pop();

		graphicsHolder.pop();

		graphicsHolder.pop();
	}

	private void renderSideClockFace(BlockEntityPKPPIDS entity, BlockPos blockPos, Direction facing, GraphicsHolder graphicsHolder, Vector3d offset, boolean isBackSide) {
		final boolean clockConfigRight = entity.isClockOnRight();
		final boolean isClockOnRight = isBackSide ? !clockConfigRight : clockConfigRight;

		final float clockCenterX = isClockOnRight ? 28.25f : 3.75f;
		final float clockCenterY = 5.0f;
		final float clockRadius = 1.90f;

		graphicsHolder.push();
		graphicsHolder.translate(blockPos.getX() - offset.getXMapped() + 0.5, blockPos.getY() - offset.getYMapped(), blockPos.getZ() - offset.getZMapped() + 0.5);
		graphicsHolder.rotateYDegrees((ROTATE_90 ? 90 : 0) - facing.asRotation());
		graphicsHolder.rotateZDegrees(180);
		graphicsHolder.translate((START_X - 8) / 16, -START_Y / 16, (START_Z - 8) / 16 - LAYER_OFFSET * 3);
		graphicsHolder.scale(1 / SCALE, 1 / SCALE, 1);

		IDrawing.drawTexture(graphicsHolder, clockCenterX - clockRadius, clockCenterY - clockRadius, 0, clockCenterX + clockRadius, clockCenterY + clockRadius, 0, Direction.UP, 0xFFFFFFFF, GraphicsHolder.getDefaultLight());

		graphicsHolder.pop();
	}

	private void renderSideText(BlockEntityPKPPIDS entity, BlockPos blockPos, Direction facing, ObjectArrayList<ArrivalResponse> arrivalResponseList, GraphicsHolder graphicsHolder, Vector3d offset, boolean isBackSide) {
		final boolean clockConfigRight = entity.isClockOnRight();
		final boolean isClockOnRight = isBackSide ? !clockConfigRight : clockConfigRight;

		final float boxWidth = 23.5f;
		final float displayX = isClockOnRight ? 2.0f : 6.50f;
		final float textX = isClockOnRight ? 7.25f : 11.75f;

		graphicsHolder.push();
		graphicsHolder.translate(blockPos.getX() - offset.getXMapped() + 0.5, blockPos.getY() - offset.getYMapped(), blockPos.getZ() - offset.getZMapped() + 0.5);
		graphicsHolder.rotateYDegrees((ROTATE_90 ? 90 : 0) - facing.asRotation());
		graphicsHolder.rotateZDegrees(180);
		graphicsHolder.translate((START_X - 8) / 16, -START_Y / 16, (START_Z - 8) / 16);
		graphicsHolder.scale(1 / SCALE, 1 / SCALE, 1);

		// Platform info header (top bar) strictly matching PKP Ipi-1 (Myriad Pro) with clearance from casing
		final ArrivalResponse arrival = arrivalResponseList.isEmpty() ? null : arrivalResponseList.get(0);
		final String platformName = arrival != null ? arrival.getPlatformName() : "";
		renderText(graphicsHolder, "peron  " + platformName, 13.125f, 0.95f, 1.0f, COLOR_WHITE, 8f, HorizontalAlignment.LEFT, FONT_MYRIAD_REGULAR);
		renderText(graphicsHolder, "platform", 13.125f, 2.05f, 0.55f, COLOR_WHITE, 3.375f, HorizontalAlignment.LEFT, FONT_MYRIAD_ITALIC);

		final String customMessage = entity.getMessage(0);
		final boolean hideArrival = entity.getHideArrival(0);

		if (hideArrival || arrival == null) {
			if (!customMessage.isEmpty()) {
				renderScrollingOrStaticText(graphicsHolder, customMessage, textX, 3.65f, 1.35f, COLOR_WHITE, 18.25f, FONT_ROBOTO_REGULAR);
			}
		} else {
			// Departure Time and Destination aligned on identical vertical coordinate and size
			final long depEpoch = arrival.getDeparture() > 0 ? arrival.getDeparture() : arrival.getArrival();
			final long localDepartureMillis = depEpoch - ArrivalsCacheClient.INSTANCE.getMillisOffset();
			final LocalTime depTime = Instant.ofEpochMilli(localDepartureMillis)
					.atZone(ZoneId.systemDefault())
					.toLocalTime();
			final String departureTimeText = String.format("%02d:%02d", depTime.getHour(), depTime.getMinute());

			final float headerY = 2.95f;
			final float headerSize = 1.50f;

			// Departure time at (displayX, headerY, w: 5.125, h: 2, align: center)
			renderText(graphicsHolder, departureTimeText, displayX, headerY, headerSize, COLOR_WHITE, 5.125f, HorizontalAlignment.CENTER, FONT_ROBOTO_MEDIUM);

			// Line name at (displayX, 5.65, w: 5.125, h: 0.80, color: black)
			final String lineName = arrival.getRouteNumber().isEmpty() ? arrival.getRouteName() : arrival.getRouteNumber();
			renderText(graphicsHolder, lineName, displayX + 0.2f, 5.70f, 0.75f, COLOR_BLACK, 4.725f, HorizontalAlignment.CENTER, FONT_ROBOTO_MEDIUM);

			// Destination station aligned with departure time
			final String destination = arrival.getDestination();
			renderScrollingOrStaticText(graphicsHolder, destination, textX, headerY, headerSize, COLOR_WHITE, 18.25f, FONT_ROBOTO_MEDIUM);

			// Intermediate stops (strictly excluding final destination station)
			final SimplifiedRoute simplifiedRoute = MinecraftClientData.getInstance().simplifiedRouteIdMap.get(arrival.getRouteId());
			if (simplifiedRoute != null) {
				final ObjectArrayList<String> viaStationNames = new ObjectArrayList<>();
				final int currentIdx = simplifiedRoute.getPlatformIndex(arrival.getPlatformId());
				final int lastIdx = simplifiedRoute.getPlatforms().size() - 1;

				for (int j = currentIdx + 1; j < lastIdx; j++) {
					final SimplifiedRoutePlatform srp = simplifiedRoute.getPlatforms().get(j);
					if (srp != null) {
						final String stationName = srp.getStationName().split("\\|")[0];
						if (!stationName.equalsIgnoreCase(destination)) {
							viaStationNames.add(stationName);
						}
					}
				}

				if (!viaStationNames.isEmpty()) {
					// "Przez / Via" with spaces around slash and refined size matching CSDIP
					renderText(graphicsHolder, "Przez / Via", textX, 4.88f, 0.52f, COLOR_WHITE, 4.875f, HorizontalAlignment.LEFT, FONT_ROBOTO_ITALIC);

					// Combine all intermediate stops into one comma-separated line with continuous marquee scrolling (Roboto Regular)
					final StringBuilder sb = new StringBuilder();
					for (int j = 0; j < viaStationNames.size(); j++) {
						if (j > 0) {
							sb.append(", ");
						}
						sb.append(viaStationNames.get(j));
					}
					renderScrollingOrStaticText(graphicsHolder, sb.toString(), textX, 5.75f, 0.58f, COLOR_WHITE, 18.25f, FONT_ROBOTO_REGULAR);
				}
			}

			// Red accent bar: real-time delay ("Opóźniony Xmin/delayed Xmin") and announcement message
			final StringBuilder redBarSb = new StringBuilder();
			final long delayMinutes = arrival.getDeviation() > 0 ? (arrival.getDeviation() + 30000) / 60000 : 0;
			if (delayMinutes > 0) {
				redBarSb.append("Op\u00f3\u017aniony ").append(delayMinutes).append("min/delayed ").append(delayMinutes).append("min");
			}
			if (!customMessage.isEmpty()) {
				if (redBarSb.length() > 0) {
					redBarSb.append(" | ");
				}
				redBarSb.append(customMessage);
			}

			final String redBarText = redBarSb.toString();
			if (!redBarText.isEmpty()) {
				renderScrollingOrStaticText(graphicsHolder, redBarText, displayX + 0.35f, 6.58f, 0.50f, COLOR_WHITE, boxWidth - 0.70f, FONT_ROBOTO_ITALIC);
			}
		}

		graphicsHolder.pop();
	}

	private static String applyKerning(String text) {
		if (text == null || text.isEmpty()) {
			return text;
		}
		// Fix known sans-serif colliding pairs (such as 'uj' and 'Ry' in Polish station names) using dedicated hair-space provider
		return text.replace("uj", "u\u200Aj")
				   .replace("uJ", "u\u200AJ")
				   .replace("Uj", "U\u200Aj")
				   .replace("UJ", "U\u200AJ")
				   .replace("Ry", "R\u200Ay")
				   .replace("ry", "r\u200Ay")
				   .replace("RY", "R\u200AY")
				   .replace("rY", "r\u200AY");
	}

	private static MutableText getStyledText(String text, Identifier font) {
		final Style style = Style.getEmptyMapped().withFont(font);
		return TextHelper.setStyle(TextHelper.literal(applyKerning(text)), style);
	}

	private static void drawClippedText(GraphicsHolder graphicsHolder, String text, float x, float y, float size, int color, Identifier font, float minX, float maxX) {
		if (text == null || text.isEmpty() || FIELD_MATRIX_STACK == null) {
			return;
		}

		try {
			final MatrixStack matrixStack = (MatrixStack) FIELD_MATRIX_STACK.get(graphicsHolder);
			if (matrixStack == null) {
				return;
			}

			final float scale = size / 8f;
			final float clipMin = minX / scale;
			final float clipMax = maxX / scale;

			final VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
			final ClippingVertexConsumerProvider clippingProvider = new ClippingVertexConsumerProvider(immediate, clipMin, clipMax);
			final TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

			matrixStack.push();
			matrixStack.translate(x, y, -LAYER_OFFSET * 5.0f);
			matrixStack.scale(scale, scale, 1.0f);

			textRenderer.draw(
					(net.minecraft.text.Text) getStyledText(text, font).data,
					0f,
					0f,
					color | ARGB_BLACK,
					false,
					matrixStack.peek().getPositionMatrix(),
					clippingProvider,
					TextRenderer.TextLayerType.NORMAL,
					0,
					GraphicsHolder.getDefaultLight()
			);

			immediate.draw();
			matrixStack.pop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void renderScrollingOrStaticText(GraphicsHolder graphicsHolder, String text, float x, float y, float size, int color, float availableWidth, Identifier font) {
		if (text == null || text.isEmpty()) {
			return;
		}

		final float scale = size / 8f;
		final float textWidth = GraphicsHolder.getTextWidth(getStyledText(text, font)) * scale;

		// If it fits within the display width, render statically with normal size (no squashing)
		if (textWidth <= availableWidth) {
			renderText(graphicsHolder, text, x, y, size, color, availableWidth, HorizontalAlignment.LEFT, font);
			return;
		}

		// Text exceeds available width: continuous marquee ticker with clear separation gap
		final float spaceWidth = GraphicsHolder.getTextWidth(getStyledText(" ", font)) * scale;
		final float gapWidth = spaceWidth * 6.0f;
		final float cycleWidth = textWidth + gapWidth;

		final double timeSec = System.currentTimeMillis() / 1000.0;
		final float scrollOffset = (float) ((timeSec * SCROLL_SPEED) % cycleWidth);

		float copyStartX = -scrollOffset;
		while (copyStartX < availableWidth) {
			if (copyStartX + textWidth > 0) {
				drawClippedText(graphicsHolder, text, x + copyStartX, y, size, color, font, -copyStartX, availableWidth - copyStartX);
			}
			copyStartX += cycleWidth;
		}
	}

	private static void renderText(GraphicsHolder graphicsHolder, String text, float x, float y, float size, int color, float availableWidth, HorizontalAlignment align, Identifier font) {
		final float scale = size / 8f;
		final float textWidth = GraphicsHolder.getTextWidth(getStyledText(text, font)) * scale;
		float drawX = x;
		graphicsHolder.push();
		graphicsHolder.translate(0, 0, -LAYER_OFFSET * 5.0f);
		if (availableWidth < textWidth) {
			graphicsHolder.translate(drawX, y, 0);
			graphicsHolder.scale(textWidth == 0 ? 1 : availableWidth / textWidth, 1, 1);
			graphicsHolder.scale(scale, scale, 1);
			graphicsHolder.drawText(getStyledText(text, font), 0, 0, color | ARGB_BLACK, false, GraphicsHolder.getDefaultLight());
		} else {
			if (align == HorizontalAlignment.CENTER) {
				drawX += (availableWidth - textWidth) / 2;
			} else if (align == HorizontalAlignment.RIGHT) {
				drawX += (availableWidth - textWidth);
			}
			graphicsHolder.translate(drawX, y, 0);
			graphicsHolder.scale(scale, scale, 1);
			graphicsHolder.drawText(getStyledText(text, font), 0, 0, color | ARGB_BLACK, false, GraphicsHolder.getDefaultLight());
		}
		graphicsHolder.pop();
	}

	private static class ClippingVertexConsumerProvider implements VertexConsumerProvider {
		private final VertexConsumerProvider parent;
		private final float minX;
		private final float maxX;

		public ClippingVertexConsumerProvider(VertexConsumerProvider parent, float minX, float maxX) {
			this.parent = parent;
			this.minX = minX;
			this.maxX = maxX;
		}

		@Override
		public VertexConsumer getBuffer(net.minecraft.client.render.RenderLayer layer) {
			return new ClippingVertexConsumer(parent.getBuffer(layer), minX, maxX);
		}
	}

	private static class ClippingVertexConsumer implements VertexConsumer {
		private final VertexConsumer parent;
		private final float clipMinX;
		private final float clipMaxX;

		private static class Vertex {
			Matrix4f matrix;
			float x, y, z;
			int r, g, b, a;
			float u, v;
			int light;
		}

		private final Vertex[] quad = new Vertex[] { new Vertex(), new Vertex(), new Vertex(), new Vertex() };
		private int vertexIndex = 0;

		public ClippingVertexConsumer(VertexConsumer parent, float clipMinX, float clipMaxX) {
			this.parent = parent;
			this.clipMinX = clipMinX;
			this.clipMaxX = clipMaxX;
		}

		@Override
		public VertexConsumer vertex(double x, double y, double z) {
			if (vertexIndex < 4) {
				quad[vertexIndex].matrix = null;
				quad[vertexIndex].x = (float) x;
				quad[vertexIndex].y = (float) y;
				quad[vertexIndex].z = (float) z;
			}
			return this;
		}

		@Override
		public VertexConsumer vertex(Matrix4f matrix, float x, float y, float z) {
			if (vertexIndex < 4) {
				quad[vertexIndex].matrix = matrix;
				quad[vertexIndex].x = x;
				quad[vertexIndex].y = y;
				quad[vertexIndex].z = z;
			}
			return this;
		}

		@Override
		public VertexConsumer color(int red, int green, int blue, int alpha) {
			if (vertexIndex < 4) {
				quad[vertexIndex].r = red;
				quad[vertexIndex].g = green;
				quad[vertexIndex].b = blue;
				quad[vertexIndex].a = alpha;
			}
			return this;
		}

		@Override
		public VertexConsumer color(float red, float green, float blue, float alpha) {
			return color((int)(red * 255), (int)(green * 255), (int)(blue * 255), (int)(alpha * 255));
		}

		@Override
		public VertexConsumer color(int argb) {
			return color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >> 24) & 0xFF);
		}

		@Override
		public VertexConsumer texture(float u, float v) {
			if (vertexIndex < 4) {
				quad[vertexIndex].u = u;
				quad[vertexIndex].v = v;
			}
			return this;
		}

		@Override
		public VertexConsumer overlay(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer overlay(int overlay) {
			return this;
		}

		@Override
		public VertexConsumer light(int u, int v) {
			if (vertexIndex < 4) {
				quad[vertexIndex].light = (v << 16) | (u & 0xFFFF);
			}
			return this;
		}

		@Override
		public VertexConsumer light(int light) {
			if (vertexIndex < 4) {
				quad[vertexIndex].light = light;
			}
			return this;
		}

		@Override
		public VertexConsumer normal(float x, float y, float z) {
			return this;
		}

		@Override
		public VertexConsumer normal(org.joml.Matrix3f matrix, float x, float y, float z) {
			return this;
		}

		@Override
		public void fixedColor(int red, int green, int blue, int alpha) {}

		@Override
		public void unfixColor() {}

		@Override
		public void next() {
			vertexIndex++;
			if (vertexIndex == 4) {
				clipAndEmit();
				vertexIndex = 0;
			}
		}

		private void clipAndEmit() {
			final float glyphMinX = quad[0].x;
			final float glyphMaxX = quad[2].x;
			final float glyphMinU = quad[0].u;
			final float glyphMaxU = quad[2].u;

			// If completely outside the visible display window, do not emit any vertices
			if (glyphMaxX <= clipMinX || glyphMinX >= clipMaxX) {
				return;
			}

			final float w = glyphMaxX - glyphMinX;
			if (w <= 0.0001f) {
				return;
			}

			// Sub-pixel clip against left display boundary
			if (glyphMinX < clipMinX) {
				final float factor = (clipMinX - glyphMinX) / w;
				final float clippedU = glyphMinU + factor * (glyphMaxU - glyphMinU);
				quad[0].x = clipMinX;
				quad[0].u = clippedU;
				quad[1].x = clipMinX;
				quad[1].u = clippedU;
			}

			// Sub-pixel clip against right display boundary
			if (glyphMaxX > clipMaxX) {
				final float factor = (glyphMaxX - clipMaxX) / w;
				final float clippedU = glyphMaxU - factor * (glyphMaxU - glyphMinU);
				quad[2].x = clipMaxX;
				quad[2].u = clippedU;
				quad[3].x = clipMaxX;
				quad[3].u = clippedU;
			}

			for (int i = 0; i < 4; i++) {
				Vertex v = quad[i];
				if (v.matrix != null) {
					parent.vertex(v.matrix, v.x, v.y, v.z);
				} else {
					parent.vertex(v.x, v.y, v.z);
				}
				parent.color(v.r, v.g, v.b, v.a)
					  .texture(v.u, v.v)
					  .light(v.light & 0xFFFF, (v.light >> 16) & 0xFFFF)
					  .next();
			}
		}
	}
}
