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
import org.mtr.mod.InitClient;
import org.mtr.mod.block.BlockPIDSHorizontalBase;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.ArrivalsCacheClient;
import org.mtr.mod.data.IGui;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;

public class RenderPKPPIDS extends BlockEntityRenderer<BlockEntityPKPPIDS> implements IGui, Utilities {

	private static final float START_X = 0f;
	private static final float START_Y = 11.0f;
	private static final float START_Z = 6f;
	private static final boolean ROTATE_90 = true;
	private static final float SCALE = 16f;

	private static final Identifier TEXTURE_WHITE = new Identifier("mtr", "textures/block/white.png");
	private static final Identifier TEXTURE_CLOCK_FACE = new Identifier(MTRPolishPIDS.MOD_ID, "textures/block/pkp_clock_face.png");

	public static final int SWITCH_TEXT_TICKS = 60;
	private static final int STATIONS_PER_PAGE = 3;
	private static final int SWITCH_PAGE_TICKS = 120;

	// Colors from PKP specification
	private static final int COLOR_CASE = 0xFF1E2255;
	private static final int COLOR_SCREEN_BG = 0xFF181127;
	private static final int COLOR_BADGE_BG = 0xFF00075E;
	private static final int COLOR_RED_LINE = 0xFFFF0000;
	private static final int COLOR_WHITE = 0xFFFFFFFF;
	private static final int COLOR_BLACK = 0xFF000000;
	private static final int COLOR_CLOCK_HAND = 0xFF191E2F;

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
		MainRenderer.scheduleRender(QueuedRenderLayer.TEXT, (graphicsHolder, offset) -> {
			// Front side
			renderSide(entity, blockPos, facing, arrivalResponseList, graphicsHolder, offset, false);
			// Back side
			if (entity instanceof BlockPIDSHorizontalBase.BlockEntityHorizontalBase) {
				renderSide(entity, blockPos.offset(facing), facing.getOpposite(), arrivalResponseList, graphicsHolder, offset, true);
			}
		});
	}

	private void renderSide(BlockEntityPKPPIDS entity, BlockPos blockPos, Direction facing, ObjectArrayList<ArrivalResponse> arrivalResponseList, GraphicsHolder graphicsHolder, Vector3d offset, boolean isBackSide) {
		final World world = entity.getWorld2();
		if (world == null) {
			return;
		}

		final boolean clockConfigRight = entity.isClockOnRight();
		// If back side, flip clock locally so it stays on the exact same physical end of the board
		final boolean isClockOnRight = isBackSide ? !clockConfigRight : clockConfigRight;

		graphicsHolder.push();
		graphicsHolder.translate(blockPos.getX() - offset.getXMapped() + 0.5, blockPos.getY() - offset.getYMapped(), blockPos.getZ() - offset.getZMapped() + 0.5);
		graphicsHolder.rotateYDegrees((ROTATE_90 ? 90 : 0) - facing.asRotation());
		graphicsHolder.rotateZDegrees(180);
		graphicsHolder.translate((START_X - 8) / 16, -START_Y / 16, (START_Z - 8) / 16);
		graphicsHolder.scale(1 / SCALE, 1 / SCALE, 1);

		// 1. Outer case and inner screen background
		drawRect(entity, blockPos, facing, -0.625f, -0.625f, 33.25f, 10.25f, COLOR_CASE, 0);
		drawRect(entity, blockPos, facing, 0f, 0.75f, 32f, 7.625f, COLOR_SCREEN_BG, 1);

		// 2. Platform info header (top bar)
		final ArrivalResponse arrival = arrivalResponseList.isEmpty() ? null : arrivalResponseList.get(0);
		final String platformName = arrival != null ? arrival.getPlatformName() : "";
		renderText(graphicsHolder, "peron  " + platformName, 13.125f, 0.75f, 1.125f, COLOR_WHITE, 8f, HorizontalAlignment.LEFT, 5);
		renderText(graphicsHolder, "platform", 13.125f, 1.875f, 0.625f, COLOR_WHITE, 3.375f, HorizontalAlignment.LEFT, 5);

		// Layout coordinates based on clock position
		final float displayX = isClockOnRight ? 1.375f : 6.25f;
		final float textX = isClockOnRight ? 6.625f : 11.5f;
		final float clockCenterX = isClockOnRight ? 28.75f : 3.25f;
		final float clockCenterY = 4.5f;

		// 3. Main arrival box backgrounds
		drawRect(entity, blockPos, facing, displayX, 2.625f, 24.5f, 4.875f, COLOR_BADGE_BG, 2);
		drawRect(entity, blockPos, facing, displayX, 5.75f, 5.125f, 0.875f, COLOR_WHITE, 3);
		drawRect(entity, blockPos, facing, displayX, 6.75f, 24.5f, 0.75f, COLOR_RED_LINE, 3);

		// 4. Clock rendering
		renderAnalogClock(entity, blockPos, facing, clockCenterX, clockCenterY, 2.35f, graphicsHolder);

		// 5. Arrival data rendering
		final String customMessage = entity.getMessage(0);
		final boolean hideArrival = entity.getHideArrival(0);

		if (hideArrival || arrival == null) {
			if (!customMessage.isEmpty()) {
				renderText(graphicsHolder, customMessage, textX, 3.5f, 1.6f, COLOR_WHITE, 19.25f, HorizontalAlignment.LEFT, 4);
			}
		} else {
			// Arrival time countdown
			final long secondsUntilArrival = (arrival.getArrival() - ArrivalsCacheClient.INSTANCE.getMillisOffset() - System.currentTimeMillis()) / 1000;
			final String arrivalText;
			if (secondsUntilArrival >= 60) {
				arrivalText = (secondsUntilArrival / 60) + " min";
			} else if (secondsUntilArrival > 0) {
				arrivalText = secondsUntilArrival + " sec";
			} else {
				arrivalText = "0 min";
			}
			renderText(graphicsHolder, arrivalText, displayX, 2.75f, 1.75f, COLOR_WHITE, 5.125f, HorizontalAlignment.CENTER, 4);

			// Line name / train category
			final String lineName = arrival.getRouteNumber().isEmpty() ? arrival.getRouteName() : arrival.getRouteNumber();
			renderText(graphicsHolder, lineName, displayX + 0.2f, 5.75f, 0.875f, COLOR_BLACK, 4.725f, HorizontalAlignment.CENTER, 4);

			// Destination
			final String destination = arrival.getDestination();
			renderText(graphicsHolder, destination, textX, 2.625f, 1.9f, COLOR_WHITE, 19.25f, HorizontalAlignment.LEFT, 4);

			// Via label and stops
			renderText(graphicsHolder, "Przez/Via:", textX, 4.75f, 0.875f, COLOR_WHITE, 4.875f, HorizontalAlignment.LEFT, 4);

			final SimplifiedRoute simplifiedRoute = MinecraftClientData.getInstance().simplifiedRouteIdMap.get(arrival.getRouteId());
			if (simplifiedRoute != null) {
				final ObjectArrayList<String> viaStationNames = new ObjectArrayList<>();
				for (int j = simplifiedRoute.getPlatformIndex(arrival.getPlatformId()) + 1; j < simplifiedRoute.getPlatforms().size(); j++) {
					final SimplifiedRoutePlatform srp = simplifiedRoute.getPlatforms().get(j);
					if (srp != null) {
						viaStationNames.add(srp.getStationName().split("\\|")[0]);
					}
				}

				if (!viaStationNames.isEmpty()) {
					final int pageCount = (int) Math.max(1, Math.ceil(viaStationNames.size() / (float) STATIONS_PER_PAGE));
					final int currentPage = (int) (Math.floor(InitClient.getGameTick() / SWITCH_PAGE_TICKS) % pageCount);
					final StringBuilder sb = new StringBuilder();
					final int startIdx = currentPage * STATIONS_PER_PAGE;
					for (int j = startIdx; j < Math.min(startIdx + STATIONS_PER_PAGE, viaStationNames.size()); j++) {
						if (sb.length() > 0) {
							sb.append(" - ");
						}
						sb.append(viaStationNames.get(j));
					}
					renderText(graphicsHolder, sb.toString(), textX, 5.75f, 0.875f, COLOR_WHITE, 19.25f, HorizontalAlignment.LEFT, 4);
				}
			}
		}

		graphicsHolder.pop();
	}

	private void renderAnalogClock(BlockEntityPKPPIDS entity, BlockPos blockPos, Direction facing, float cx, float cy, float radius, GraphicsHolder graphicsHolder) {
		final World world = entity.getWorld2();
		if (world == null) {
			return;
		}

		// 1. Draw clock dial texture
		MainRenderer.scheduleRender(TEXTURE_CLOCK_FACE, false, QueuedRenderLayer.LIGHT, (gh, offset) -> {
			gh.push();
			gh.translate(blockPos.getX() - offset.getXMapped() + 0.5, blockPos.getY() - offset.getYMapped(), blockPos.getZ() - offset.getZMapped() + 0.5);
			gh.rotateYDegrees((ROTATE_90 ? 90 : 0) - facing.asRotation());
			gh.rotateZDegrees(180);
			gh.translate((START_X - 8) / 16, -START_Y / 16, (START_Z - 8) / 16 - SMALL_OFFSET * 4);
			gh.scale(1 / SCALE, 1 / SCALE, 1);
			IDrawing.drawTexture(gh, cx - radius, cy - radius, 0, cx + radius, cy + radius, 0, facing, 0xFFFFFFFF, GraphicsHolder.getDefaultLight());
			gh.pop();
		});

		// 2. Calculate time (Minecraft world time converted to 12-hour clock)
		final long timeOfDay = (world.getTime() + 6000) % 24000;
		final float hours = (timeOfDay / 1000f) % 12f;
		final float minutes = ((timeOfDay % 1000) * 60f / 1000f);
		final float hourAngle = (hours + minutes / 60f) * 30f;
		final float minuteAngle = minutes * 6f;

		// Draw clock hands
		drawClockHand(entity, blockPos, facing, cx, cy, hourAngle, radius * 0.55f, 0.28f, COLOR_CLOCK_HAND, 5);
		drawClockHand(entity, blockPos, facing, cx, cy, minuteAngle, radius * 0.82f, 0.18f, COLOR_CLOCK_HAND, 6);
	}

	private void drawClockHand(BlockEntityPKPPIDS entity, BlockPos blockPos, Direction facing, float cx, float cy, float angle, float length, float width, int color, int layer) {
		MainRenderer.scheduleRender(TEXTURE_WHITE, false, QueuedRenderLayer.LIGHT, (gh, offset) -> {
			gh.push();
			gh.translate(blockPos.getX() - offset.getXMapped() + 0.5, blockPos.getY() - offset.getYMapped(), blockPos.getZ() - offset.getZMapped() + 0.5);
			gh.rotateYDegrees((ROTATE_90 ? 90 : 0) - facing.asRotation());
			gh.rotateZDegrees(180);
			gh.translate((START_X - 8) / 16, -START_Y / 16, (START_Z - 8) / 16 - SMALL_OFFSET * (layer + 2));
			gh.scale(1 / SCALE, 1 / SCALE, 1);
			gh.translate(cx, cy, 0);
			gh.rotateZDegrees(angle);
			IDrawing.drawTexture(gh, -width / 2, -length, 0, width / 2, length * 0.15f, 0, facing, color, GraphicsHolder.getDefaultLight());
			gh.pop();
		});
	}

	private void drawRect(BlockEntityPKPPIDS entity, BlockPos blockPos, Direction facing, float x, float y, float width, float height, int color, int layer) {
		MainRenderer.scheduleRender(TEXTURE_WHITE, false, QueuedRenderLayer.LIGHT, (gh, offset) -> {
			gh.push();
			gh.translate(blockPos.getX() - offset.getXMapped() + 0.5, blockPos.getY() - offset.getYMapped(), blockPos.getZ() - offset.getZMapped() + 0.5);
			gh.rotateYDegrees((ROTATE_90 ? 90 : 0) - facing.asRotation());
			gh.rotateZDegrees(180);
			gh.translate((START_X - 8) / 16, -START_Y / 16, (START_Z - 8) / 16 - SMALL_OFFSET * (layer + 2));
			gh.scale(1 / SCALE, 1 / SCALE, 1);
			IDrawing.drawTexture(gh, x, y, 0, x + width, y + height, 0, facing, color, GraphicsHolder.getDefaultLight());
			gh.pop();
		});
	}

	private static void renderText(GraphicsHolder graphicsHolder, String text, float x, float y, float size, int color, float availableWidth, HorizontalAlignment align, int layer) {
		graphicsHolder.push();
		final float scale = size / 8f;
		graphicsHolder.translate(x, y, -SMALL_OFFSET * (layer + 2));
		final float textWidth = GraphicsHolder.getTextWidth(text) * scale;
		if (availableWidth < textWidth) {
			graphicsHolder.scale(textWidth == 0 ? 1 : availableWidth / textWidth, 1, 1);
		}
		if (align == HorizontalAlignment.CENTER) {
			graphicsHolder.translate(Math.max(0, availableWidth - textWidth) / 2, 0, 0);
		} else if (align == HorizontalAlignment.RIGHT) {
			graphicsHolder.translate(Math.max(0, availableWidth - textWidth), 0, 0);
		}
		graphicsHolder.scale(scale, scale, 1);
		graphicsHolder.drawText(text, 0, 0, color | ARGB_BLACK, false, GraphicsHolder.getDefaultLight());
		graphicsHolder.pop();
	}
}
