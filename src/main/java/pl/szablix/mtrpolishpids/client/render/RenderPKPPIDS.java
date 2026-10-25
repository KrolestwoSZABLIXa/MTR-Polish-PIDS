package pl.szablix.mtrpolishpids.client.render;

import pl.szablix.mtrpolishpids.MTRPolishPIDS;
import pl.szablix.mtrpolishpids.block.entity.BlockEntityPKPPIDS;
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
import org.mtr.mod.InitClient;
import org.mtr.mod.block.BlockPIDSHorizontalBase;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.ArrivalsCacheClient;
import org.mtr.mod.data.IGui;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;

import java.awt.Color;
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

	private static final float SCROLL_SPEED = 2.2f;

	// Ultra-flush layer offset to prevent z-fighting while appearing fully glued to the glass surface
	private static final float LAYER_OFFSET = 0.0001f;

	// Colors directly from authentic PKP PLK CSDIP specifications
	private static final int COLOR_SCREEN_BG = 0xFF08080C;  // Authentic PKP deep black screen glass
	private static final int COLOR_BADGE_BG = 0xFF00075E;   // color: 1886
	private static final int COLOR_RED_LINE = 0xFFFF0000;   // color: 16711680
	private static final int COLOR_WHITE = 0xFFFFFFFF;      // color: 16777215
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

		// 3. Dynamic Text (Java2D HarfBuzz typography with native kerning and zero distortion)
		renderSideDynamicText(entity, blockPos, facing, arrivalResponseList, false);
		if (entity instanceof BlockPIDSHorizontalBase.BlockEntityHorizontalBase) {
			renderSideDynamicText(entity, blockPos.offset(facing), facing.getOpposite(), arrivalResponseList, true);
		}
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

		// Layer 2: White box under time for line name & red accent line (only rendered when delayed)
		final ArrivalResponse arrival = arrivalResponseList.isEmpty() ? null : arrivalResponseList.get(0);
		final boolean hideArrival = entity.getHideArrival(0);
		final long delayMinutes = (arrival != null && !hideArrival && arrival.getDeviation() > 0)
				? (arrival.getDeviation() + 30000) / 60000
				: 0;

		graphicsHolder.push();
		graphicsHolder.translate(0, 0, -LAYER_OFFSET * 2);
		IDrawing.drawTexture(graphicsHolder, displayX, 5.65f, 0, displayX + 5.125f, 6.45f, 0, Direction.UP, COLOR_WHITE, GraphicsHolder.getDefaultLight());
		if (delayMinutes > 0) {
			IDrawing.drawTexture(graphicsHolder, displayX, 6.55f, 0, displayX + boxWidth, 7.20f, 0, Direction.UP, COLOR_RED_LINE, GraphicsHolder.getDefaultLight());
		}
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

	private void renderSideDynamicText(BlockEntityPKPPIDS entity, BlockPos blockPos, Direction facing, ObjectArrayList<ArrivalResponse> arrivalResponseList, boolean isBackSide) {
		final boolean clockConfigRight = entity.isClockOnRight();
		final boolean isClockOnRight = isBackSide ? !clockConfigRight : clockConfigRight;

		final float boxWidth = 23.5f;
		final float displayX = isClockOnRight ? 2.0f : 6.50f;
		final float textX = isClockOnRight ? 7.25f : 11.75f;

		final ArrivalResponse arrival = arrivalResponseList.isEmpty() ? null : arrivalResponseList.get(0);
		final String platformName = arrival != null ? arrival.getPlatformName() : "";

		// 1. Casing plate (Ipi-1): "peron X" and "platform" in Myriad Pro
		final PKPTextTextureManager.DynamicTextTexture casingTex = PKPTextTextureManager.getCasingPlate(platformName);
		renderStaticTexture(entity, blockPos, facing, isBackSide, casingTex, 13.125f, 0.85f, 1.85f, 8.0f, HorizontalAlignment.LEFT);

		final String customMessage = entity.getMessage(0);
		final boolean hideArrival = entity.getHideArrival(0);

		if (hideArrival || arrival == null) {
			if (!customMessage.isEmpty()) {
				final PKPTextTextureManager.DynamicTextTexture customTex = PKPTextTextureManager.getText("custom", customMessage, PKPTextTextureManager.getRobotoRegular(), 50f, Color.WHITE);
				renderScrollingOrStaticTexture(entity, blockPos, facing, isBackSide, customTex, textX, 3.60f, 1.75f, 18.25f, HorizontalAlignment.LEFT);
			}
		} else {
			// Departure Time
			final long depEpoch = arrival.getDeparture() > 0 ? arrival.getDeparture() : arrival.getArrival();
			final long localDepartureMillis = depEpoch - ArrivalsCacheClient.INSTANCE.getMillisOffset();
			final LocalTime depTime = Instant.ofEpochMilli(localDepartureMillis)
					.atZone(ZoneId.systemDefault())
					.toLocalTime();
			final String departureTimeText = String.format("%02d:%02d", depTime.getHour(), depTime.getMinute());

			final PKPTextTextureManager.DynamicTextTexture timeTex = PKPTextTextureManager.getText("time", departureTimeText, PKPTextTextureManager.getRobotoMedium(), 58f, Color.WHITE);
			renderStaticTexture(entity, blockPos, facing, isBackSide, timeTex, displayX, 2.85f, 2.15f, 5.125f, HorizontalAlignment.CENTER);

			// Train Line / Route Number on white badge (black text)
			final String lineName = arrival.getRouteNumber().isEmpty() ? arrival.getRouteName() : arrival.getRouteNumber();
			if (!lineName.isEmpty()) {
				final PKPTextTextureManager.DynamicTextTexture lineTex = PKPTextTextureManager.getText("badge", lineName, PKPTextTextureManager.getRobotoMedium(), 38f, Color.BLACK);
				renderScrollingOrStaticTexture(entity, blockPos, facing, isBackSide, lineTex, displayX + 0.2f, 5.67f, 0.76f, 4.725f, HorizontalAlignment.CENTER);
			}

			// Destination Station
			final String destination = arrival.getDestination();
			final PKPTextTextureManager.DynamicTextTexture destTex = PKPTextTextureManager.getText("dest", destination, PKPTextTextureManager.getRobotoMedium(), 58f, Color.WHITE);
			renderScrollingOrStaticTexture(entity, blockPos, facing, isBackSide, destTex, textX, 2.85f, 2.15f, 18.25f, HorizontalAlignment.LEFT);

			// Intermediate stops (Przez / Via)
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
					// "Przez / Via" label
					final PKPTextTextureManager.DynamicTextTexture viaLblTex = PKPTextTextureManager.getText("via_lbl", "Przez / Via", PKPTextTextureManager.getRobotoItalic(), 28f, Color.WHITE);
					renderStaticTexture(entity, blockPos, facing, isBackSide, viaLblTex, textX, 4.95f, 0.65f, 4.875f, HorizontalAlignment.LEFT);

					// Intermediate stops list
					final StringBuilder sb = new StringBuilder();
					for (int j = 0; j < viaStationNames.size(); j++) {
						if (j > 0) {
							sb.append(", ");
						}
						sb.append(viaStationNames.get(j));
					}
					final PKPTextTextureManager.DynamicTextTexture viaStopsTex = PKPTextTextureManager.getText("via_stops", sb.toString(), PKPTextTextureManager.getRobotoRegular(), 34f, Color.WHITE);
					renderScrollingOrStaticTexture(entity, blockPos, facing, isBackSide, viaStopsTex, textX, 5.66f, 0.78f, 18.25f, HorizontalAlignment.LEFT);
				}
			}

			// Red delay bar ticker (only rendered when delayed)
			final long delayMinutes = arrival.getDeviation() > 0 ? (arrival.getDeviation() + 30000) / 60000 : 0;
			if (delayMinutes > 0) {
				final StringBuilder redBarSb = new StringBuilder();
				redBarSb.append("Op\u00f3\u017aniony ").append(delayMinutes).append("min/delayed ").append(delayMinutes).append("min");
				if (!customMessage.isEmpty()) {
					redBarSb.append(" | ").append(customMessage);
				}
				final PKPTextTextureManager.DynamicTextTexture delayTex = PKPTextTextureManager.getText("delay", redBarSb.toString(), PKPTextTextureManager.getRobotoItalic(), 28f, Color.WHITE);
				renderMarqueeTexture(entity, blockPos, facing, isBackSide, delayTex, displayX + 0.35f, 6.57f, 0.60f, boxWidth - 0.70f);
			}
		}
	}

	private static void scheduleTextTexture(BlockEntityPKPPIDS entity, BlockPos blockPos, Direction facing, PKPTextTextureManager.DynamicTextTexture tex, float x, float y, float w, float h, float u1, float v1, float u2, float v2) {
		if (tex == null || w <= 0.001f || h <= 0.001f) {
			return;
		}

		MainRenderer.scheduleRender(tex.identifier, false, QueuedRenderLayer.LIGHT_TRANSLUCENT, (graphicsHolder, offset) -> {
			graphicsHolder.push();
			graphicsHolder.translate(blockPos.getX() - offset.getXMapped() + 0.5, blockPos.getY() - offset.getYMapped(), blockPos.getZ() - offset.getZMapped() + 0.5);
			graphicsHolder.rotateYDegrees((ROTATE_90 ? 90 : 0) - facing.asRotation());
			graphicsHolder.rotateZDegrees(180);
			graphicsHolder.translate((START_X - 8) / 16, -START_Y / 16, (START_Z - 8) / 16 - LAYER_OFFSET * 5);
			graphicsHolder.scale(1 / SCALE, 1 / SCALE, 1);

			IDrawing.drawTexture(graphicsHolder, x, y, 0f, x + w, y + h, 0f, u1, v1, u2, v2, Direction.UP, 0xFFFFFFFF, GraphicsHolder.getDefaultLight());

			graphicsHolder.pop();
		});
	}

	private static void renderStaticTexture(BlockEntityPKPPIDS entity, BlockPos blockPos, Direction facing, boolean isBackSide, PKPTextTextureManager.DynamicTextTexture tex, float x, float y, float targetH, float maxW, HorizontalAlignment align) {
		if (tex == null) {
			return;
		}

		float w = targetH * tex.getAspectRatio();
		float drawX = x;
		if (w > maxW) {
			w = maxW; // scale down horizontally to fit bounds exactly
		} else {
			if (align == HorizontalAlignment.CENTER) {
				drawX = x + (maxW - w) / 2f;
			} else if (align == HorizontalAlignment.RIGHT) {
				drawX = x + (maxW - w);
			}
		}

		scheduleTextTexture(entity, blockPos, facing, tex, drawX, y, w, targetH, 0f, 0f, 1f, 1f);
	}

	private static void renderMarqueeTexture(BlockEntityPKPPIDS entity, BlockPos blockPos, Direction facing, boolean isBackSide, PKPTextTextureManager.DynamicTextTexture tex, float windowX, float y, float targetH, float windowW) {
		if (tex == null) {
			return;
		}

		final float itemW = targetH * tex.getAspectRatio();
		final float gapW = 4.0f; // Gap between loop repetitions in grid units
		final float cycleW = itemW + gapW;

		final double timeSec = System.currentTimeMillis() / 1000.0;
		final float scrollOffset = (float) ((timeSec * SCROLL_SPEED) % cycleW);

		float copyStartX = -scrollOffset;
		while (copyStartX < windowW) {
			final float quadLeft = windowX + copyStartX;
			final float quadRight = quadLeft + itemW;

			if (quadRight > windowX && quadLeft < windowX + windowW) {
				final float clippedLeft = Math.max(quadLeft, windowX);
				final float clippedRight = Math.min(quadRight, windowX + windowW);

				final float u1 = (clippedLeft - quadLeft) / itemW;
				final float u2 = (clippedRight - quadLeft) / itemW;

				scheduleTextTexture(entity, blockPos, facing, tex, clippedLeft, y, clippedRight - clippedLeft, targetH, u1, 0f, u2, 1f);
			}
			copyStartX += cycleW;
		}
	}

	private static void renderScrollingOrStaticTexture(BlockEntityPKPPIDS entity, BlockPos blockPos, Direction facing, boolean isBackSide, PKPTextTextureManager.DynamicTextTexture tex, float windowX, float y, float targetH, float windowW, HorizontalAlignment alignIfStatic) {
		if (tex == null) {
			return;
		}

		final float itemW = targetH * tex.getAspectRatio();
		if (itemW <= windowW) {
			renderStaticTexture(entity, blockPos, facing, isBackSide, tex, windowX, y, targetH, windowW, alignIfStatic);
		} else {
			renderMarqueeTexture(entity, blockPos, facing, isBackSide, tex, windowX, y, targetH, windowW);
		}
	}
}
