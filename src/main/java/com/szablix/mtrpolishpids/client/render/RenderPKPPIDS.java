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

	private static final int STATIONS_PER_PAGE = 4;
	private static final int SWITCH_PAGE_TICKS = 120;

	// Colors matching Polish PKP standards
	private static final int COLOR_SCREEN_BG = 0xFF0A0F1E;
	private static final int COLOR_RED_BAR = 0xFFD81B1B;
	private static final int COLOR_WHITE = 0xFFFFFFFF;
	private static final int COLOR_SECONDARY_TEXT = 0xFFB0C4DE;
	private static final int COLOR_BADGE_BG = 0xFFFFFFFF;
	private static final int COLOR_BADGE_TEXT = 0xFF001F5C;
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

		// 1. Recessed LCD Screen background (strictly within screen boundaries so it never sticks out)
		final float screenX = 1.0f;
		final float screenY = 1.0f;
		final float screenW = 30.0f;
		final float screenH = 7.0f;
		drawRect(entity, blockPos, facing, screenX, screenY, screenW, screenH, COLOR_SCREEN_BG, 0);

		// 2. Geometry calculations based on clock position (right vs left)
		final float clockRadius = 2.3f;
		final float clockCy = 4.5f;
		final float clockCx = isClockOnRight ? 27.5f : 4.5f;

		final float infoX = isClockOnRight ? 1.6f : 7.6f;
		final float infoW = 22.8f;

		// 3. Render Analog Clock
		renderAnalogClock(entity, blockPos, facing, clockCx, clockCy, clockRadius, graphicsHolder);

		// 4. Render Information Content
		final ArrivalResponse arrival = arrivalResponseList.isEmpty() ? null : arrivalResponseList.get(0);
		final String customMessage = entity.getMessage(0);
		final boolean hideArrival = entity.getHideArrival(0);

		final float redBarY = 6.25f;
		final float redBarH = 0.5f;

		if (hideArrival || arrival == null) {
			if (!customMessage.isEmpty()) {
				renderText(graphicsHolder, customMessage, infoX, 3.2f, 1.4f, COLOR_WHITE, infoW, HorizontalAlignment.CENTER, 3);
			} else {
				renderText(graphicsHolder, "Brak odjazd\u00f3w / No departures", infoX, 3.2f, 1.1f, COLOR_SECONDARY_TEXT, infoW, HorizontalAlignment.CENTER, 3);
			}
			drawRect(entity, blockPos, facing, infoX, redBarY, infoW, redBarH, COLOR_RED_BAR, 1);
		} else {
			// Departure Time formatted as HH:mm
			final long depEpoch = arrival.getDeparture() > 0 ? arrival.getDeparture() : arrival.getArrival();
			final long localDepartureMillis = depEpoch - ArrivalsCacheClient.INSTANCE.getMillisOffset();
			final LocalTime depTime = Instant.ofEpochMilli(localDepartureMillis)
					.atZone(ZoneId.systemDefault())
					.toLocalTime();
			final String departureTimeText = String.format("%02d:%02d", depTime.getHour(), depTime.getMinute());

			// Top Row: Departure Time + Destination
			final float timeWidth = 5.2f;
			renderText(graphicsHolder, departureTimeText, infoX, 1.4f, 1.85f, COLOR_WHITE, timeWidth, HorizontalAlignment.LEFT, 3);

			final String destination = arrival.getDestination();
			final float destX = infoX + 5.6f;
			final float destW = infoW - 5.8f;
			renderText(graphicsHolder, destination, destX, 1.4f, 1.85f, COLOR_WHITE, destW, HorizontalAlignment.LEFT, 3);

			// Middle Row Left: Train Number + Line/Category Badge
			final String rawRouteNum = arrival.getRouteNumber();
			final String rawRouteName = arrival.getRouteName();
			final String trainNum;
			final String lineBadge;

			if (!rawRouteNum.isEmpty() && !rawRouteName.isEmpty() && !rawRouteNum.equalsIgnoreCase(rawRouteName)) {
				trainNum = rawRouteNum;
				lineBadge = rawRouteName;
			} else if (!rawRouteNum.isEmpty()) {
				trainNum = rawRouteNum;
				lineBadge = "SKA";
			} else if (!rawRouteName.isEmpty()) {
				trainNum = rawRouteName;
				lineBadge = "REG";
			} else {
				trainNum = "Os " + (Math.abs(arrival.getRouteId() % 90000) + 10000);
				lineBadge = "PKP";
			}

			// Train Number (e.g. Os 33417)
			renderText(graphicsHolder, trainNum, infoX, 3.5f, 0.82f, COLOR_WHITE, timeWidth, HorizontalAlignment.LEFT, 3);

			// Line Badge (e.g. SKA3)
			final float badgeW = 4.4f;
			final float badgeH = 1.0f;
			drawRect(entity, blockPos, facing, infoX, 4.6f, badgeW, badgeH, COLOR_BADGE_BG, 1);
			renderText(graphicsHolder, lineBadge, infoX + 0.1f, 4.65f, 0.8f, COLOR_BADGE_TEXT, badgeW - 0.2f, HorizontalAlignment.CENTER, 3);

			// Middle Row Right: "Przez / Via" + Intermediate Stations
			renderText(graphicsHolder, "Przez / Via", destX, 3.5f, 0.65f, COLOR_SECONDARY_TEXT, 12f, HorizontalAlignment.LEFT, 3);

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
							sb.append(", ");
						}
						sb.append(viaStationNames.get(j));
					}
					renderText(graphicsHolder, sb.toString(), destX, 4.6f, 0.82f, COLOR_WHITE, destW, HorizontalAlignment.LEFT, 3);
				}
			}

			// Bottom Row: Red accent bar & notification
			drawRect(entity, blockPos, facing, infoX, redBarY, infoW, redBarH, COLOR_RED_BAR, 1);
			if (!customMessage.isEmpty()) {
				renderText(graphicsHolder, customMessage, infoX + 0.4f, redBarY + 0.06f, 0.42f, COLOR_WHITE, infoW - 0.8f, HorizontalAlignment.LEFT, 3);
			} else {
				renderText(graphicsHolder, "UWAGA! PROSZ\u0118 ZACHOWA\u0106 OSTRO\u017bNO\u015a\u0106", infoX + 0.4f, redBarY + 0.06f, 0.42f, COLOR_WHITE, infoW - 0.8f, HorizontalAlignment.RIGHT, 3);
			}
		}

		graphicsHolder.pop();
	}

	private void renderAnalogClock(BlockEntityPKPPIDS entity, BlockPos blockPos, Direction facing, float cx, float cy, float radius, GraphicsHolder graphicsHolder) {
		final World world = entity.getWorld2();
		if (world == null) {
			return;
		}

		// 1. Clock dial with crisp 1..12 numerals
		MainRenderer.scheduleRender(TEXTURE_CLOCK_FACE, false, QueuedRenderLayer.LIGHT, (gh, offset) -> {
			gh.push();
			gh.translate(blockPos.getX() - offset.getXMapped() + 0.5, blockPos.getY() - offset.getYMapped(), blockPos.getZ() - offset.getZMapped() + 0.5);
			gh.rotateYDegrees((ROTATE_90 ? 90 : 0) - facing.asRotation());
			gh.rotateZDegrees(180);
			gh.translate((START_X - 8) / 16, -START_Y / 16, (START_Z - 8) / 16 - SMALL_OFFSET * 2);
			gh.scale(1 / SCALE, 1 / SCALE, 1);
			IDrawing.drawTexture(gh, cx - radius, cy - radius, 0, cx + radius, cy + radius, 0, facing, 0xFFFFFFFF, GraphicsHolder.getDefaultLight());
			gh.pop();
		});

		// 2. Real-time hands using computer/local machine time
		final LocalTime now = LocalTime.now();
		final float hours = now.getHour() % 12f;
		final float minutes = now.getMinute();
		final float seconds = now.getSecond();
		final float hourAngle = (hours + minutes / 60f + seconds / 3600f) * 30f;
		final float minuteAngle = (minutes + seconds / 60f) * 6f;

		drawClockHand(entity, blockPos, facing, cx, cy, hourAngle, radius * 0.56f, 0.32f, COLOR_CLOCK_HAND, 4);
		drawClockHand(entity, blockPos, facing, cx, cy, minuteAngle, radius * 0.82f, 0.20f, COLOR_CLOCK_HAND, 5);
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
