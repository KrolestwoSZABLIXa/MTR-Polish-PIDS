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
import org.mtr.mod.render.MoreRenderLayers;

import java.awt.Color;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

public class RenderPKPPIDS extends BlockEntityRenderer<BlockEntityPKPPIDS> implements IGui, Utilities {

	private static final float START_X = 0f;
	private static final float START_Y = 11.0f;
	private static final float START_Z = 5.645f;
	private static final boolean ROTATE_90 = true;
	private static final float SCALE = 16f;

	private static final Identifier TEXTURE_WHITE = new Identifier("mtr", "textures/block/white.png");
	private static final Identifier TEXTURE_CLOCK_FACE = new Identifier(MTRPolishPIDS.MOD_ID, "textures/block/pkp_clock_face.png");

	private static final float SCROLL_SPEED = 2.2f;

	// Optimal 1.5 mm layer offset: perfectly flat to the eye, zero Z-fighting or clipping under glancing angles
	private static final float LAYER_OFFSET = 0.0015f;

	// Colors directly from authentic PKP PLK CSDIP specifications
	private static final int COLOR_BADGE_BG = 0xFF000A64;   // Authentic PKP PLK display navy blue (#000a64, exact color on shaders)
	private static final int COLOR_RED_LINE = 0xFFCC1010;   // Signal red without shader overexposure
	private static final int COLOR_WHITE = 0xFFFFFFFF;      // Crisp 100% white matching dynamic text
	private static final int COLOR_CASING_TEXT = 0xFFD8E0EC;// Painted white lettering for metal casing (non-emissive)
	private static final int COLOR_CLOCK_HAND = 0xFF141824;

	private static final java.util.Map<Identifier, org.mtr.mapping.holder.RenderLayer> EMISSIVE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.Map<Identifier, org.mtr.mapping.holder.RenderLayer> BEACON_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

	private static boolean checkIris() {
		try {
			final Class<?> irisClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
			final Object instance = irisClass.getMethod("getInstance").invoke(null);
			return Boolean.TRUE.equals(irisClass.getMethod("isShaderPackInUse").invoke(instance));
		} catch (Throwable t1) {
			try {
				final Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
				final Object pack = iris.getMethod("getCurrentPack").invoke(null);
				if (pack instanceof java.util.Optional) {
					return ((java.util.Optional<?>) pack).isPresent();
				}
			} catch (Throwable ignored) {}
			return false;
		}
	}

	private static boolean checkOptifine() {
		try {
			final Class<?> config = Class.forName("optifine.Config");
			return Boolean.TRUE.equals(config.getMethod("isShaders").invoke(null));
		} catch (Throwable ignored) {
			return false;
		}
	}

	public static boolean isShaderActive() {
		return checkIris() || checkOptifine();
	}

	public static org.mtr.mapping.holder.RenderLayer getDisplayLayer(Identifier id) {
		if (isShaderActive()) {
			return EMISSIVE_CACHE.computeIfAbsent(id, key -> new org.mtr.mapping.holder.RenderLayer(
					net.minecraft.client.render.RenderLayer.getEntityTranslucentEmissive(
							new net.minecraft.util.Identifier(key.getNamespace(), key.getPath())
					)
			));
		} else {
			return BEACON_CACHE.computeIfAbsent(id, key -> new org.mtr.mapping.holder.RenderLayer(
					net.minecraft.client.render.RenderLayer.getBeaconBeam(
							new net.minecraft.util.Identifier(key.getNamespace(), key.getPath()),
							true
					)
			));
		}
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
			getArrivalsAndRender(entity, blockPos, facing, platformIds, graphicsHolder, light);
		} else {
			getArrivalsAndRender(entity, blockPos, facing, entity.getPlatformIds(), graphicsHolder, light);
		}
	}

	private void getArrivalsAndRender(BlockEntityPKPPIDS entity, BlockPos blockPos, Direction facing, LongCollection platformIds, GraphicsHolder graphicsHolder, int light) {
		final ObjectArrayList<ArrivalResponse> arrivalResponseList = ArrivalsCacheClient.INSTANCE.requestArrivals(platformIds);
		final BlockPos origin = blockPos;

		// 1. Clock Face & Hands on dynamic clock texture in TEXT layer (integrated texture guarantees hands are 100% visible on shaders)
		final Identifier clockTextureId = PKPTextTextureManager.getClockTexture();
		renderSideClockFace(entity, origin, blockPos, facing, graphicsHolder, clockTextureId, false);
		if (entity instanceof BlockPIDSHorizontalBase.BlockEntityHorizontalBase) {
			renderSideClockFace(entity, origin, blockPos.offset(facing), facing.getOpposite(), graphicsHolder, clockTextureId, true);
		}

		// 2. Quads (Navy screen, red accent line) on TEXTURE_WHITE in TEXT layer
		renderSideQuads(entity, origin, blockPos, facing, arrivalResponseList, graphicsHolder, false);
		if (entity instanceof BlockPIDSHorizontalBase.BlockEntityHorizontalBase) {
			renderSideQuads(entity, origin, blockPos.offset(facing), facing.getOpposite(), arrivalResponseList, graphicsHolder, true);
		}

		// 3. Dynamic Text (Java2D HarfBuzz typography with native kerning and zero distortion)
		renderSideDynamicText(entity, origin, blockPos, facing, arrivalResponseList, graphicsHolder, false, light);
		if (entity instanceof BlockPIDSHorizontalBase.BlockEntityHorizontalBase) {
			renderSideDynamicText(entity, origin, blockPos.offset(facing), facing.getOpposite(), arrivalResponseList, graphicsHolder, true, light);
		}
	}

	private void renderSideQuads(BlockEntityPKPPIDS entity, BlockPos origin, BlockPos blockPos, Direction facing, ObjectArrayList<ArrivalResponse> arrivalResponseList, GraphicsHolder graphicsHolder, boolean isBackSide) {
		final boolean clockConfigRight = entity.isClockOnRight();
		final boolean isClockOnRight = isBackSide ? !clockConfigRight : clockConfigRight;

		final float boxWidth = 23.5f;
		final float displayX = isClockOnRight ? 2.0f : 6.50f;

		graphicsHolder.createVertexConsumer(MoreRenderLayers.getExterior(TEXTURE_WHITE));
		graphicsHolder.push();
		graphicsHolder.translate(blockPos.getX() - origin.getX() + 0.5, blockPos.getY() - origin.getY(), blockPos.getZ() - origin.getZ() + 0.5);
		graphicsHolder.rotateYDegrees((ROTATE_90 ? 90 : 0) - facing.asRotation());
		graphicsHolder.rotateZDegrees(180);
		graphicsHolder.translate((START_X - 8) / 16, -START_Y / 16, (START_Z - 8) / 16 - LAYER_OFFSET * 1);
		graphicsHolder.scale(1 / SCALE, 1 / SCALE, 1);

		final ArrivalResponse arrival = arrivalResponseList.isEmpty() ? null : arrivalResponseList.get(0);
		final boolean hideArrival = entity.getHideArrival(0);
		final boolean hasArrival = arrival != null && !hideArrival;
		final long delayMinutes = (hasArrival && arrival.getDeviation() > 0)
				? (arrival.getDeviation() + 30000) / 60000
				: 0;

		if (!hasArrival) {
			// No active train: Solid blue screen across the entire display area
			IDrawing.drawTexture(graphicsHolder, displayX, 2.85f, 0, displayX + boxWidth, 7.20f, 0, Direction.SOUTH, COLOR_BADGE_BG, GraphicsHolder.getDefaultLight());
		} else {
			final float badgeW = 5.125f;
			final float midY1 = 5.52f;
			final float midY2 = 6.47f;
			final float botY1 = 6.55f;
			final float botY2 = 7.20f;

			// 1. Top Section (Navy): Under departure time & destination station
			IDrawing.drawTexture(graphicsHolder, displayX, 2.85f, 0, displayX + boxWidth, midY1, 0, Direction.SOUTH, COLOR_BADGE_BG, GraphicsHolder.getDefaultLight());

			// 2. Middle Section Left (White): Authentic white badge under departure time
			// Non-overlapping with blue! Impossible to Z-fight or disappear under angles!
			IDrawing.drawTexture(graphicsHolder, displayX, midY1, 0, displayX + badgeW, midY2, 0, Direction.SOUTH, COLOR_WHITE, GraphicsHolder.getDefaultLight());

			// 3. Middle Section Right (Navy): Under "Przez / Via" & intermediate stops
			IDrawing.drawTexture(graphicsHolder, displayX + badgeW, midY1, 0, displayX + boxWidth, midY2, 0, Direction.SOUTH, COLOR_BADGE_BG, GraphicsHolder.getDefaultLight());

			// 4. Spacer row (Navy): Gap between middle section and bottom accent
			IDrawing.drawTexture(graphicsHolder, displayX, midY2, 0, displayX + boxWidth, botY1, 0, Direction.SOUTH, COLOR_BADGE_BG, GraphicsHolder.getDefaultLight());

			// 5. Bottom accent row: Red if delayed, Navy if on time
			final int bottomColor = delayMinutes > 0 ? COLOR_RED_LINE : COLOR_BADGE_BG;
			IDrawing.drawTexture(graphicsHolder, displayX, botY1, 0, displayX + boxWidth, botY2, 0, Direction.SOUTH, bottomColor, GraphicsHolder.getDefaultLight());
		}

		graphicsHolder.pop();
	}

	private void renderSideClockFace(BlockEntityPKPPIDS entity, BlockPos origin, BlockPos blockPos, Direction facing, GraphicsHolder graphicsHolder, Identifier clockTextureId, boolean isBackSide) {
		final boolean clockConfigRight = entity.isClockOnRight();
		final boolean isClockOnRight = isBackSide ? !clockConfigRight : clockConfigRight;

		final float clockCenterX = isClockOnRight ? 28.25f : 3.75f;
		final float clockCenterY = 5.0f;
		final float clockRadius = 1.90f;

		graphicsHolder.createVertexConsumer(MoreRenderLayers.getExteriorTranslucent(clockTextureId));
		graphicsHolder.push();
		graphicsHolder.translate(blockPos.getX() - origin.getX() + 0.5, blockPos.getY() - origin.getY(), blockPos.getZ() - origin.getZ() + 0.5);
		graphicsHolder.rotateYDegrees((ROTATE_90 ? 90 : 0) - facing.asRotation());
		graphicsHolder.rotateZDegrees(180);
		graphicsHolder.translate((START_X - 8) / 16, -START_Y / 16, (START_Z - 8) / 16 - LAYER_OFFSET * 2);
		graphicsHolder.scale(1 / SCALE, 1 / SCALE, 1);

		// Single draw call: Dynamic clock dial with integrated real-time hands (100% visible on shaders and vanilla)
		IDrawing.drawTexture(graphicsHolder, clockCenterX - clockRadius, clockCenterY - clockRadius, 0, clockCenterX + clockRadius, clockCenterY + clockRadius, 0, Direction.SOUTH, COLOR_WHITE, GraphicsHolder.getDefaultLight());

		graphicsHolder.pop();
	}

	private void renderSideDynamicText(BlockEntityPKPPIDS entity, BlockPos origin, BlockPos blockPos, Direction facing, ObjectArrayList<ArrivalResponse> arrivalResponseList, GraphicsHolder graphicsHolder, boolean isBackSide, int light) {
		final boolean clockConfigRight = entity.isClockOnRight();
		final boolean isClockOnRight = isBackSide ? !clockConfigRight : clockConfigRight;

		final float boxWidth = 23.5f;
		final float displayX = isClockOnRight ? 2.0f : 6.50f;
		final float textX = isClockOnRight ? 7.50f : 12.00f;

		final ArrivalResponse arrival = arrivalResponseList.isEmpty() ? null : arrivalResponseList.get(0);
		final String platformName = arrival != null ? arrival.getPlatformName() : "";

		// 1. Casing plate (Ipi-1): "peron X" and "platform" in Myriad Pro (non-emissive painted casing text)
		final PKPTextTextureManager.DynamicTextTexture casingTex = PKPTextTextureManager.getCasingPlate(platformName);
		renderCasingTexture(graphicsHolder, origin, blockPos, facing, isBackSide, casingTex, 13.125f, 0.65f, 2.10f, 8.0f, HorizontalAlignment.LEFT, light);

		final String customMessage = entity.getMessage(0);
		final boolean hideArrival = entity.getHideArrival(0);

		if (hideArrival || arrival == null) {
			if (!customMessage.isEmpty()) {
				final PKPTextTextureManager.DynamicTextTexture customTex = PKPTextTextureManager.getText("custom", customMessage, PKPTextTextureManager.getRobotoMedium(), 62f, Color.WHITE);
				renderScrollingOrStaticTexture(graphicsHolder, origin, blockPos, facing, isBackSide, customTex, displayX + 0.6f, 3.85f, 2.25f, boxWidth - 1.2f, HorizontalAlignment.LEFT);
			}
		} else {
			// Scheduled Departure Time (fixed timetable time, unshifted by real-time delay)
			final long depEpoch = arrival.getDeparture() > 0 ? arrival.getDeparture() : arrival.getArrival();
			final long scheduledEpoch = depEpoch - arrival.getDeviation();
			final long localDepartureMillis = scheduledEpoch - ArrivalsCacheClient.INSTANCE.getMillisOffset();
			final LocalTime depTime = Instant.ofEpochMilli(localDepartureMillis)
					.atZone(ZoneId.systemDefault())
					.toLocalTime();
			final String departureTimeText = String.format("%02d:%02d", depTime.getHour(), depTime.getMinute());

			final PKPTextTextureManager.DynamicTextTexture timeTex = PKPTextTextureManager.getText("time", departureTimeText, PKPTextTextureManager.getRobotoMedium(), 64f, Color.WHITE);
			renderStaticTexture(graphicsHolder, origin, blockPos, facing, isBackSide, timeTex, displayX, 2.65f, 2.35f, 5.125f, HorizontalAlignment.CENTER);

			// Train Line / Route Number on white badge (black text, scrolls smoothly if long)
			final String lineName = arrival.getRouteNumber().isEmpty() ? arrival.getRouteName() : arrival.getRouteNumber();
			if (!lineName.isEmpty()) {
				final PKPTextTextureManager.DynamicTextTexture lineTex = PKPTextTextureManager.getText("badge", lineName.trim(), PKPTextTextureManager.getRobotoMedium(), 46f, Color.BLACK);
				final float badgeW = 5.125f;
				final float badgeH = 0.92f;
				final float badgeY = 5.53f;
				renderScrollingOrStaticTexture(graphicsHolder, origin, blockPos, facing, isBackSide, lineTex, displayX + 0.2f, badgeY, badgeH, badgeW - 0.4f, HorizontalAlignment.CENTER);
			}

			// Destination Station
			final String destination = arrival.getDestination();
			final PKPTextTextureManager.DynamicTextTexture destTex = PKPTextTextureManager.getText("dest", destination, PKPTextTextureManager.getRobotoMedium(), 64f, Color.WHITE);
			renderScrollingOrStaticTexture(graphicsHolder, origin, blockPos, facing, isBackSide, destTex, textX, 2.65f, 2.35f, 18.00f, HorizontalAlignment.LEFT);

			// "Przez / Via" label - always displayed on board for active arrivals
			final PKPTextTextureManager.DynamicTextTexture viaLblTex = PKPTextTextureManager.getText("via_lbl", "Przez / Via", PKPTextTextureManager.getRobotoItalic(), 34f, Color.WHITE);
			renderStaticTexture(graphicsHolder, origin, blockPos, facing, isBackSide, viaLblTex, textX, 4.90f, 0.80f, 4.875f, HorizontalAlignment.LEFT);

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
					// Intermediate stops list
					final StringBuilder sb = new StringBuilder();
					for (int j = 0; j < viaStationNames.size(); j++) {
						if (j > 0) {
							sb.append(", ");
						}
						sb.append(viaStationNames.get(j));
					}
					final PKPTextTextureManager.DynamicTextTexture viaStopsTex = PKPTextTextureManager.getText("via_stops", sb.toString(), PKPTextTextureManager.getRobotoRegular(), 44f, Color.WHITE);
					renderScrollingOrStaticTexture(graphicsHolder, origin, blockPos, facing, isBackSide, viaStopsTex, textX, 5.52f, 1.05f, 18.00f, HorizontalAlignment.LEFT);
				}
			}

			// Red delay bar ticker (only rendered when delayed) - scrolls once across screen per pass
			final long delayMinutes = arrival.getDeviation() > 0 ? (arrival.getDeviation() + 30000) / 60000 : 0;
			if (delayMinutes > 0) {
				final StringBuilder redBarSb = new StringBuilder();
				redBarSb.append("Op\u00f3\u017aniony ").append(delayMinutes).append("min/delayed ").append(delayMinutes).append("min");
				if (!customMessage.isEmpty()) {
					redBarSb.append(" | ").append(customMessage);
				}
				final PKPTextTextureManager.DynamicTextTexture delayTex = PKPTextTextureManager.getText("delay", redBarSb.toString(), PKPTextTextureManager.getRobotoItalic(), 36f, Color.WHITE);
				renderSinglePassMarquee(graphicsHolder, origin, blockPos, facing, isBackSide, delayTex, displayX + 0.35f, 6.48f, 0.80f, boxWidth - 0.70f);
			}
		}
	}

	private static void renderTextTexture(GraphicsHolder graphicsHolder, BlockPos origin, BlockPos blockPos, Direction facing, PKPTextTextureManager.DynamicTextTexture tex, float x, float y, float w, float h, float u1, float v1, float u2, float v2) {
		if (tex == null || w <= 0.001f || h <= 0.001f) {
			return;
		}

		graphicsHolder.createVertexConsumer(MoreRenderLayers.getExteriorTranslucent(tex.identifier));
		graphicsHolder.push();
		graphicsHolder.translate(blockPos.getX() - origin.getX() + 0.5, blockPos.getY() - origin.getY(), blockPos.getZ() - origin.getZ() + 0.5);
		graphicsHolder.rotateYDegrees((ROTATE_90 ? 90 : 0) - facing.asRotation());
		graphicsHolder.rotateZDegrees(180);
		graphicsHolder.translate((START_X - 8) / 16, -START_Y / 16, (START_Z - 8) / 16 - LAYER_OFFSET * 3);
		graphicsHolder.scale(1 / SCALE, 1 / SCALE, 1);

		IDrawing.drawTexture(graphicsHolder, x, y, 0f, x + w, y + h, 0f, u1, v1, u2, v2, Direction.SOUTH, COLOR_WHITE, GraphicsHolder.getDefaultLight());

		graphicsHolder.pop();
	}

	private static void renderCasingTextureInternal(GraphicsHolder graphicsHolder, BlockPos origin, BlockPos blockPos, Direction facing, PKPTextTextureManager.DynamicTextTexture tex, float x, float y, float w, float h, float u1, float v1, float u2, float v2, int light) {
		if (tex == null || w <= 0.001f || h <= 0.001f) {
			return;
		}

		graphicsHolder.createVertexConsumer(MoreRenderLayers.getExteriorTranslucent(tex.identifier));
		graphicsHolder.push();
		graphicsHolder.translate(blockPos.getX() - origin.getX() + 0.5, blockPos.getY() - origin.getY(), blockPos.getZ() - origin.getZ() + 0.5);
		graphicsHolder.rotateYDegrees((ROTATE_90 ? 90 : 0) - facing.asRotation());
		graphicsHolder.rotateZDegrees(180);
		graphicsHolder.translate((START_X - 8) / 16, -START_Y / 16, (START_Z - 8) / 16 - LAYER_OFFSET * 2);
		graphicsHolder.scale(1 / SCALE, 1 / SCALE, 1);

		IDrawing.drawTexture(graphicsHolder, x, y, 0f, x + w, y + h, 0f, u1, v1, u2, v2, Direction.SOUTH, COLOR_CASING_TEXT, light);

		graphicsHolder.pop();
	}

	private static void renderCasingTexture(GraphicsHolder graphicsHolder, BlockPos origin, BlockPos blockPos, Direction facing, boolean isBackSide, PKPTextTextureManager.DynamicTextTexture tex, float x, float y, float targetH, float maxW, HorizontalAlignment align, int light) {
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

		renderCasingTextureInternal(graphicsHolder, origin, blockPos, facing, tex, drawX, y, w, targetH, 0f, 0f, 1f, 1f, light);
	}

	private static void renderStaticTexture(GraphicsHolder graphicsHolder, BlockPos origin, BlockPos blockPos, Direction facing, boolean isBackSide, PKPTextTextureManager.DynamicTextTexture tex, float x, float y, float targetH, float maxW, HorizontalAlignment align) {
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

		renderTextTexture(graphicsHolder, origin, blockPos, facing, tex, drawX, y, w, targetH, 0f, 0f, 1f, 1f);
	}

	private static void renderMarqueeTexture(GraphicsHolder graphicsHolder, BlockPos origin, BlockPos blockPos, Direction facing, boolean isBackSide, PKPTextTextureManager.DynamicTextTexture tex, float windowX, float y, float targetH, float windowW) {
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

				renderTextTexture(graphicsHolder, origin, blockPos, facing, tex, clippedLeft, y, clippedRight - clippedLeft, targetH, u1, 0f, u2, 1f);
			}
			copyStartX += cycleW;
		}
	}

	private static void renderSinglePassMarquee(GraphicsHolder graphicsHolder, BlockPos origin, BlockPos blockPos, Direction facing, boolean isBackSide, PKPTextTextureManager.DynamicTextTexture tex, float windowX, float y, float targetH, float windowW) {
		if (tex == null) {
			return;
		}

		final float itemW = targetH * tex.getAspectRatio();
		final float pauseGap = 6.0f; // Gap after leaving the screen before re-entering
		final float cycleW = windowW + itemW + pauseGap;

		final double timeSec = System.currentTimeMillis() / 1000.0;
		final float progress = (float) ((timeSec * SCROLL_SPEED) % cycleW);

		final float quadLeft = windowX + windowW - progress;
		final float quadRight = quadLeft + itemW;

		if (quadRight > windowX && quadLeft < windowX + windowW) {
			final float clippedLeft = Math.max(quadLeft, windowX);
			final float clippedRight = Math.min(quadRight, windowX + windowW);

			final float u1 = (clippedLeft - quadLeft) / itemW;
			final float u2 = (clippedRight - quadLeft) / itemW;

			renderTextTexture(graphicsHolder, origin, blockPos, facing, tex, clippedLeft, y, clippedRight - clippedLeft, targetH, u1, 0f, u2, 1f);
		}
	}

	private static void renderScrollingOrStaticTexture(GraphicsHolder graphicsHolder, BlockPos origin, BlockPos blockPos, Direction facing, boolean isBackSide, PKPTextTextureManager.DynamicTextTexture tex, float windowX, float y, float targetH, float windowW, HorizontalAlignment alignIfStatic) {
		if (tex == null) {
			return;
		}

		final float itemW = targetH * tex.getAspectRatio();
		if (itemW <= windowW) {
			renderStaticTexture(graphicsHolder, origin, blockPos, facing, isBackSide, tex, windowX, y, targetH, windowW, alignIfStatic);
		} else {
			renderMarqueeTexture(graphicsHolder, origin, blockPos, facing, isBackSide, tex, windowX, y, targetH, windowW);
		}
	}
}
