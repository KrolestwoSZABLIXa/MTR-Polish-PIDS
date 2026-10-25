package com.szablix.mtrpolishpids.client.render;

import com.szablix.mtrpolishpids.MTRPolishPIDS;
import com.szablix.mtrpolishpids.block.entity.BlockEntityPKPPIDS;
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
	private static final Identifier FONT_MTR = new Identifier("mtr", "mtr");

	private static final float SCROLL_SPEED = 2.2f;

	// Layer offset to prevent z-fighting while maintaining a flush, non-floating appearance
	private static final float LAYER_OFFSET = 0.0015f;

	// Colors directly from the user's JSON specifications
	private static final int COLOR_SCREEN_BG = 0xFF08080C;  // Authentic PKP deep black screen glass
	private static final int COLOR_BADGE_BG = 0xFF00075E;   // color: 1886
	private static final int COLOR_RED_LINE = 0xFFFF0000;   // color: 16711680
	private static final int COLOR_WHITE = 0xFFFFFFFF;      // color: 16777215
	private static final int COLOR_BLACK = 0xFF000000;      // color: 0
	private static final int COLOR_CLOCK_HAND = 0xFF141824;

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

		// 3. Balanced coordinates leaving a subtle black glass margin from the casing edges
		final float boxWidth = 23.5f;
		final float displayX = isClockOnRight ? 2.0f : 6.50f;
		final float clockCenterX = isClockOnRight ? 28.25f : 3.75f;
		final float clockCenterY = 4.5f;
		final float clockRadius = 1.85f;

		graphicsHolder.push();
		graphicsHolder.translate(blockPos.getX() - offset.getXMapped() + 0.5, blockPos.getY() - offset.getYMapped(), blockPos.getZ() - offset.getZMapped() + 0.5);
		graphicsHolder.rotateYDegrees((ROTATE_90 ? 90 : 0) - facing.asRotation());
		graphicsHolder.rotateZDegrees(180);
		graphicsHolder.translate((START_X - 8) / 16, -START_Y / 16, (START_Z - 8) / 16);
		graphicsHolder.scale(1 / SCALE, 1 / SCALE, 1);

		// Layer 0: Screen background strictly matching JSON (0.2, 0.75, 31.6, 7.625)
		graphicsHolder.push();
		graphicsHolder.translate(0, 0, -LAYER_OFFSET * 0);
		IDrawing.drawTexture(graphicsHolder, 0.2f, 0.75f, 0, 31.8f, 8.375f, 0, Direction.UP, COLOR_SCREEN_BG, GraphicsHolder.getDefaultLight());
		graphicsHolder.pop();

		// Layer 1: Blue arrival badge: w = 23.5, h = 4.875
		graphicsHolder.push();
		graphicsHolder.translate(0, 0, -LAYER_OFFSET * 1);
		IDrawing.drawTexture(graphicsHolder, displayX, 2.625f, 0, displayX + boxWidth, 2.625f + 4.875f, 0, Direction.UP, COLOR_BADGE_BG, GraphicsHolder.getDefaultLight());
		graphicsHolder.pop();

		// Layer 2: White box under time for line name & red accent line
		graphicsHolder.push();
		graphicsHolder.translate(0, 0, -LAYER_OFFSET * 2);
		IDrawing.drawTexture(graphicsHolder, displayX, 5.75f, 0, displayX + 5.125f, 5.75f + 0.875f, 0, Direction.UP, COLOR_WHITE, GraphicsHolder.getDefaultLight());
		IDrawing.drawTexture(graphicsHolder, displayX, 6.75f, 0, displayX + boxWidth, 6.75f + 0.75f, 0, Direction.UP, COLOR_RED_LINE, GraphicsHolder.getDefaultLight());
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
		final float clockCenterY = 4.5f;
		final float clockRadius = 1.85f;

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

		final float displayX = isClockOnRight ? 2.0f : 6.50f;
		final float textX = isClockOnRight ? 7.25f : 11.75f;

		graphicsHolder.push();
		graphicsHolder.translate(blockPos.getX() - offset.getXMapped() + 0.5, blockPos.getY() - offset.getYMapped(), blockPos.getZ() - offset.getZMapped() + 0.5);
		graphicsHolder.rotateYDegrees((ROTATE_90 ? 90 : 0) - facing.asRotation());
		graphicsHolder.rotateZDegrees(180);
		graphicsHolder.translate((START_X - 8) / 16, -START_Y / 16, (START_Z - 8) / 16);
		graphicsHolder.scale(1 / SCALE, 1 / SCALE, 1);

		// Platform info header (top bar) strictly matching JSON (x: 13.125, y: 0.75 / 1.875)
		final ArrivalResponse arrival = arrivalResponseList.isEmpty() ? null : arrivalResponseList.get(0);
		final String platformName = arrival != null ? arrival.getPlatformName() : "";
		renderText(graphicsHolder, "peron  " + platformName, 13.125f, 0.75f, 1.0f, COLOR_WHITE, 8f, HorizontalAlignment.LEFT, false);
		renderText(graphicsHolder, "platform", 13.125f, 1.875f, 0.55f, COLOR_WHITE, 3.375f, HorizontalAlignment.LEFT, true);

		final String customMessage = entity.getMessage(0);
		final boolean hideArrival = entity.getHideArrival(0);

		if (hideArrival || arrival == null) {
			if (!customMessage.isEmpty()) {
				renderScrollingOrStaticText(graphicsHolder, customMessage, textX, 3.5f, 1.35f, COLOR_WHITE, 18.25f, false);
			}
		} else {
			// Departure Time formatted as HH:mm
			final long depEpoch = arrival.getDeparture() > 0 ? arrival.getDeparture() : arrival.getArrival();
			final long localDepartureMillis = depEpoch - ArrivalsCacheClient.INSTANCE.getMillisOffset();
			final LocalTime depTime = Instant.ofEpochMilli(localDepartureMillis)
					.atZone(ZoneId.systemDefault())
					.toLocalTime();
			final String departureTimeText = String.format("%02d:%02d", depTime.getHour(), depTime.getMinute());

			// Departure time at (displayX, 2.85, w: 5.125, h: 2, align: center)
			renderText(graphicsHolder, departureTimeText, displayX, 2.85f, 1.5f, COLOR_WHITE, 5.125f, HorizontalAlignment.CENTER, false);

			// Line name at (displayX, 5.75, w: 5.125, h: 0.875, color: black)
			final String lineName = arrival.getRouteNumber().isEmpty() ? arrival.getRouteName() : arrival.getRouteNumber();
			renderText(graphicsHolder, lineName, displayX + 0.2f, 5.8f, 0.75f, COLOR_BLACK, 4.725f, HorizontalAlignment.CENTER, false);

			// Destination station with marquee scrolling when exceeding width
			final String destination = arrival.getDestination();
			renderScrollingOrStaticText(graphicsHolder, destination, textX, 2.7f, 1.55f, COLOR_WHITE, 18.25f, false);

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
					// Przez/Via: in italic (slightly smaller, authentic typography)
					renderText(graphicsHolder, "Przez/Via:", textX, 4.85f, 0.60f, COLOR_WHITE, 4.875f, HorizontalAlignment.LEFT, true);

					// Combine all intermediate stops into one comma-separated line with continuous marquee scrolling
					final StringBuilder sb = new StringBuilder();
					for (int j = 0; j < viaStationNames.size(); j++) {
						if (j > 0) {
							sb.append(", ");
						}
						sb.append(viaStationNames.get(j));
					}
					renderScrollingOrStaticText(graphicsHolder, sb.toString(), textX, 5.85f, 0.60f, COLOR_WHITE, 18.25f, false);
				}
			}
		}

		graphicsHolder.pop();
	}

	private static MutableText getStyledText(String text, boolean italic) {
		Style style = Style.getEmptyMapped().withFont(FONT_MTR);
		if (italic) {
			style = style.withItalic(Boolean.TRUE);
		}
		return TextHelper.setStyle(TextHelper.literal(text), style);
	}

	private static void renderScrollingOrStaticText(GraphicsHolder graphicsHolder, String text, float x, float y, float size, int color, float availableWidth, boolean italic) {
		if (text == null || text.isEmpty()) {
			return;
		}

		final float scale = size / 8f;
		final MutableText fullStyledText = getStyledText(text, italic);
		final float textWidth = GraphicsHolder.getTextWidth(fullStyledText) * scale;

		// If it fits within the display width, render statically with normal size (no squashing)
		if (textWidth <= availableWidth) {
			renderText(graphicsHolder, text, x, y, size, color, availableWidth, HorizontalAlignment.LEFT, italic);
			return;
		}

		// Text exceeds available width: continuous marquee ticker with ~1 to 1.5 characters gap
		final float charSpaceWidth = GraphicsHolder.getTextWidth(getStyledText(" ", italic)) * scale;
		final float gapWidth = charSpaceWidth * 2.5f; // ~1.3 to 1.5 characters gap
		final float cycleWidth = textWidth + gapWidth;

		final double timeSec = System.currentTimeMillis() / 1000.0;
		final float scrollOffset = (float) ((timeSec * SCROLL_SPEED) % cycleWidth);

		final int len = text.length();
		final float[] prefixWidths = new float[len + 1];
		prefixWidths[0] = 0f;
		for (int i = 1; i <= len; i++) {
			prefixWidths[i] = GraphicsHolder.getTextWidth(getStyledText(text.substring(0, i), italic)) * scale;
		}

		graphicsHolder.push();
		graphicsHolder.translate(x, y, -LAYER_OFFSET * 5);

		float copyStartX = -scrollOffset;
		while (copyStartX < availableWidth) {
			if (copyStartX + textWidth > 0) {
				renderVisibleSlice(graphicsHolder, text, prefixWidths, copyStartX, scale, color, availableWidth, italic);
			}
			copyStartX += cycleWidth;
		}

		graphicsHolder.pop();
	}

	private static void renderVisibleSlice(GraphicsHolder graphicsHolder, String text, float[] prefixWidths, float copyStartX, float scale, int color, float availableWidth, boolean italic) {
		final int len = text.length();
		int firstChar = -1;
		int lastChar = -1;

		for (int i = 0; i < len; i++) {
			final float charLeft = copyStartX + prefixWidths[i];
			final float charRight = copyStartX + prefixWidths[i + 1];

			if (charRight > 0 && charLeft >= -0.5f && charLeft < availableWidth && charRight <= availableWidth + 0.4f) {
				if (firstChar == -1) {
					firstChar = i;
				}
				lastChar = i + 1;
			}
		}

		if (firstChar != -1 && lastChar > firstChar) {
			final String visibleText = text.substring(firstChar, lastChar);
			final MutableText styledSlice = getStyledText(visibleText, italic);
			final float drawX = copyStartX + prefixWidths[firstChar];

			graphicsHolder.push();
			graphicsHolder.translate(drawX, 0, 0);
			graphicsHolder.scale(scale, scale, 1);
			graphicsHolder.drawText(styledSlice, 0, 0, color | ARGB_BLACK, false, GraphicsHolder.getDefaultLight());
			graphicsHolder.pop();
		}
	}

	private static void renderText(GraphicsHolder graphicsHolder, String text, float x, float y, float size, int color, float availableWidth, HorizontalAlignment align, boolean italic) {
		graphicsHolder.push();
		final float scale = size / 8f;
		graphicsHolder.translate(x, y, -LAYER_OFFSET * 5);
		final MutableText mutableText = getStyledText(text, italic);
		final float textWidth = GraphicsHolder.getTextWidth(mutableText) * scale;
		if (availableWidth < textWidth) {
			graphicsHolder.scale(textWidth == 0 ? 1 : availableWidth / textWidth, 1, 1);
		}
		if (align == HorizontalAlignment.CENTER) {
			graphicsHolder.translate(Math.max(0, availableWidth - textWidth) / 2, 0, 0);
		} else if (align == HorizontalAlignment.RIGHT) {
			graphicsHolder.translate(Math.max(0, availableWidth - textWidth), 0, 0);
		}
		graphicsHolder.scale(scale, scale, 1);
		graphicsHolder.drawText(mutableText, 0, 0, color | ARGB_BLACK, false, GraphicsHolder.getDefaultLight());
		graphicsHolder.pop();
	}
}
