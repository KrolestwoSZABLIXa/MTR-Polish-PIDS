package pl.szablix.mtrpolishpids.client.render;

import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.BlockEntityRenderer;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import pl.szablix.mtrpolishpids.block.entity.BlockEntityCitySign;

public class RenderCitySign extends BlockEntityRenderer<BlockEntityCitySign> {

	private static final Identifier TEXTURE_WHITE = new Identifier("mtr", "textures/block/white.png");

	// Authentic colors for galvanized steel posts and mounting rails
	private static final int COLOR_POST_FRONT = 0xFF8A8D90;
	private static final int COLOR_POST_SIDE = 0xFF76797C;
	private static final int COLOR_POST_BACK = 0xFF686B6E;
	private static final int COLOR_RAIL = 0xFF9CA0A4;
	private static final int COLOR_SIGN_BACK = 0xFF7B7E82;

	private static final float BOARD_H = 0.80f;
	private static final float BOARD_Y_BOTTOM = 2.15f;
	private static final float BOARD_Y_TOP = BOARD_Y_BOTTOM + BOARD_H; // 2.95f

	public RenderCitySign(Argument dispatcher) {
		super(dispatcher);
	}

	@Override
	public void render(BlockEntityCitySign entity, float tickDelta, GraphicsHolder graphicsHolder, int light, int overlay) {
		final World world = entity.getWorld2();
		if (world == null) {
			return;
		}

		final BlockPos blockPos = entity.getPos2();
		final BlockState state = world.getBlockState(blockPos);
		final IBlock.EnumThird third = IBlock.getStatePropertySafe(state, IBlock.THIRD);
		if (third != IBlock.EnumThird.LOWER) {
			return;
		}

		final Direction facing = IBlock.getStatePropertySafe(state, DirectionHelper.FACING);
		final String cityName = entity.getCityName();
		final boolean endOfTown = entity.isEndOfTown();
		final boolean twoSided = entity.isTwoSided();
		final String cityNameBack = entity.getCityNameBack();
		final boolean endOfTownBack = entity.isEndOfTownBack();

		final CitySignTextureManager.DynamicCitySignTexture texFront = CitySignTextureManager.getSignTexture(cityName, endOfTown);
		if (texFront == null) {
			return;
		}

		final CitySignTextureManager.DynamicCitySignTexture texBack = twoSided
				? CitySignTextureManager.getSignTexture(cityNameBack, endOfTownBack)
				: null;

		final float halfWFront = texFront.worldWidth / 2.0f;
		final float halfWBack = (texBack != null) ? texBack.worldWidth / 2.0f : halfWFront;
		final float maxHalfW = Math.max(halfWFront, twoSided ? halfWBack : halfWFront);
		final float postOffset = Math.max(0.35f, maxHalfW * 0.65f);
		final float postR = 0.04f; // 8 cm wide steel posts

		// 1. Render steel posts, mounting channels, and sign backing sheet in EXTERIOR layer
		MainRenderer.scheduleRender(TEXTURE_WHITE, false, QueuedRenderLayer.EXTERIOR, (gh, offset) -> {
			gh.push();
			gh.translate(blockPos.getX() - offset.getXMapped() + 0.5, blockPos.getY() - offset.getYMapped(), blockPos.getZ() - offset.getZMapped() + 0.5);
			gh.rotateYDegrees(-facing.asRotation());

			// Left post (from ground y=0 to y=3.00)
			drawBoxShaded(gh, -postOffset - postR, 0.0f, -postR, -postOffset + postR, 3.00f, postR, light);

			// Right post (from ground y=0 to y=3.00)
			drawBoxShaded(gh, postOffset - postR, 0.0f, -postR, postOffset + postR, 3.00f, postR, light);

			// Horizontal aluminum mounting channels behind front board
			final float railLeftFront = -postOffset - 0.20f;
			final float railRightFront = postOffset + 0.20f;
			drawBox(gh, railLeftFront, 2.30f, 0.040f, railRightFront, 2.38f, 0.055f, COLOR_RAIL, light);
			drawBox(gh, railLeftFront, 2.70f, 0.040f, railRightFront, 2.78f, 0.055f, COLOR_RAIL, light);

			// Mounting clamps holding channels to posts (front)
			drawBox(gh, -postOffset - 0.055f, 2.28f, -0.045f, -postOffset + 0.055f, 2.40f, 0.055f, COLOR_RAIL, light);
			drawBox(gh, -postOffset - 0.055f, 2.68f, -0.045f, -postOffset + 0.055f, 2.80f, 0.055f, COLOR_RAIL, light);
			drawBox(gh, postOffset - 0.055f, 2.28f, -0.045f, postOffset + 0.055f, 2.40f, 0.055f, COLOR_RAIL, light);
			drawBox(gh, postOffset - 0.055f, 2.68f, -0.045f, postOffset + 0.055f, 2.80f, 0.055f, COLOR_RAIL, light);

			// Front sign backing sheet metal with rounded corners
			drawRoundedSheetMetal(gh, halfWFront, BOARD_Y_BOTTOM, BOARD_Y_TOP, 0.055f, 0.070f, COLOR_SIGN_BACK, light);

			// Optional back sign mounting structure & backing sheet
			if (twoSided) {
				final float railLeftBack = -postOffset - 0.20f;
				final float railRightBack = postOffset + 0.20f;
				drawBox(gh, railLeftBack, 2.30f, -0.055f, railRightBack, 2.38f, -0.040f, COLOR_RAIL, light);
				drawBox(gh, railLeftBack, 2.70f, -0.055f, railRightBack, 2.78f, -0.040f, COLOR_RAIL, light);

				drawBox(gh, -postOffset - 0.055f, 2.28f, -0.055f, -postOffset + 0.055f, 2.40f, 0.045f, COLOR_RAIL, light);
				drawBox(gh, -postOffset - 0.055f, 2.68f, -0.055f, -postOffset + 0.055f, 2.80f, 0.045f, COLOR_RAIL, light);
				drawBox(gh, postOffset - 0.055f, 2.28f, -0.055f, postOffset + 0.055f, 2.40f, 0.045f, COLOR_RAIL, light);
				drawBox(gh, postOffset - 0.055f, 2.68f, -0.055f, postOffset + 0.055f, 2.80f, 0.045f, COLOR_RAIL, light);

				drawRoundedSheetMetal(gh, halfWBack, BOARD_Y_BOTTOM, BOARD_Y_TOP, -0.070f, -0.055f, COLOR_SIGN_BACK, light);
			}

			gh.pop();
		});

		// 2. Render high-resolution front face in EXTERIOR cutout layer (sharp alpha cutout, proper lighting & CCW winding)
		MainRenderer.scheduleRender(texFront.identifier, false, QueuedRenderLayer.EXTERIOR, (gh, offset) -> {
			gh.push();
			gh.translate(blockPos.getX() - offset.getXMapped() + 0.5, blockPos.getY() - offset.getYMapped(), blockPos.getZ() - offset.getZMapped() + 0.5);
			gh.rotateYDegrees(-facing.asRotation());

			// Front face with green road sign texture (facing +Z / SOUTH towards viewer)
			gh.drawTextureInWorld(
					halfWFront, BOARD_Y_TOP, 0.071f,
					-halfWFront, BOARD_Y_TOP, 0.071f,
					-halfWFront, BOARD_Y_BOTTOM, 0.071f,
					halfWFront, BOARD_Y_BOTTOM, 0.071f,
					1f, 1f, 0f, 0f,
					Direction.SOUTH, 0xFFFFFFFF, light
			);

			gh.pop();
		});

		// 3. Render high-resolution back face in EXTERIOR cutout layer (if two-sided)
		if (twoSided && texBack != null) {
			MainRenderer.scheduleRender(texBack.identifier, false, QueuedRenderLayer.EXTERIOR, (gh, offset) -> {
				gh.push();
				gh.translate(blockPos.getX() - offset.getXMapped() + 0.5, blockPos.getY() - offset.getYMapped(), blockPos.getZ() - offset.getZMapped() + 0.5);
				gh.rotateYDegrees(-facing.asRotation());

				// Back face with green road sign texture (facing -Z / NORTH towards viewer behind the sign)
				// CCW winding facing NORTH: (-halfW, top) -> (halfW, top) -> (halfW, bottom) -> (-halfW, bottom)
				gh.drawTextureInWorld(
						-halfWBack, BOARD_Y_TOP, -0.071f,
						halfWBack, BOARD_Y_TOP, -0.071f,
						halfWBack, BOARD_Y_BOTTOM, -0.071f,
						-halfWBack, BOARD_Y_BOTTOM, -0.071f,
						1f, 1f, 0f, 0f,
						Direction.NORTH, 0xFFFFFFFF, light
				);

				gh.pop();
			});
		}
	}

	private static void drawRoundedSheetMetal(GraphicsHolder gh, float halfW, float y1, float y2, float z1, float z2, int color, int light) {
		final float c = 0.04f; // 4 cm corner curve matching the texture's corner radius

		// Main center body
		drawBox(gh, -halfW + c, y1, z1, halfW - c, y2, z2, color, light);

		// Left side between corner curves
		drawBox(gh, -halfW, y1 + c, z1, -halfW + c, y2 - c, z2, color, light);

		// Right side between corner curves
		drawBox(gh, halfW - c, y1 + c, z1, halfW, y2 - c, z2, color, light);

		// 4 corner chamfers closing the diagonal edges
		// Top-Left corner chamfer (-X, +Y)
		gh.drawTextureInWorld(-halfW + c, y2, z2, -halfW, y2 - c, z2, -halfW, y2 - c, z1, -halfW + c, y2, z1, 0f, 0f, 1f, 1f, Direction.UP, color, light);
		// Top-Right corner chamfer (+X, +Y)
		gh.drawTextureInWorld(halfW, y2 - c, z2, halfW - c, y2, z2, halfW - c, y2, z1, halfW, y2 - c, z1, 0f, 0f, 1f, 1f, Direction.UP, color, light);
		// Bottom-Left corner chamfer (-X, -Y)
		gh.drawTextureInWorld(-halfW, y1 + c, z2, -halfW + c, y1, z2, -halfW + c, y1, z1, -halfW, y1 + c, z1, 0f, 0f, 1f, 1f, Direction.DOWN, color, light);
		// Bottom-Right corner chamfer (+X, -Y)
		gh.drawTextureInWorld(halfW - c, y1, z2, halfW, y1 + c, z2, halfW, y1 + c, z1, halfW - c, y1, z1, 0f, 0f, 1f, 1f, Direction.DOWN, color, light);
	}

	private static void drawBoxShaded(GraphicsHolder gh, float x1, float y1, float z1, float x2, float y2, float z2, int light) {
		// Front face (+Z)
		gh.drawTextureInWorld(x2, y2, z2, x1, y2, z2, x1, y1, z2, x2, y1, z2, 0f, 0f, 1f, 1f, Direction.SOUTH, COLOR_POST_FRONT, light);
		// Back face (-Z)
		gh.drawTextureInWorld(x1, y2, z1, x2, y2, z1, x2, y1, z1, x1, y1, z1, 0f, 0f, 1f, 1f, Direction.NORTH, COLOR_POST_BACK, light);
		// Left face (-X)
		gh.drawTextureInWorld(x1, y2, z2, x1, y2, z1, x1, y1, z1, x1, y1, z2, 0f, 0f, 1f, 1f, Direction.WEST, COLOR_POST_SIDE, light);
		// Right face (+X)
		gh.drawTextureInWorld(x2, y2, z1, x2, y2, z2, x2, y1, z2, x2, y1, z1, 0f, 0f, 1f, 1f, Direction.EAST, COLOR_POST_SIDE, light);
		// Top face (+Y)
		gh.drawTextureInWorld(x2, y2, z1, x1, y2, z1, x1, y2, z2, x2, y2, z2, 0f, 0f, 1f, 1f, Direction.UP, COLOR_POST_FRONT, light);
		// Bottom face (-Y)
		gh.drawTextureInWorld(x2, y1, z2, x1, y1, z2, x1, y1, z1, x2, y1, z1, 0f, 0f, 1f, 1f, Direction.DOWN, COLOR_POST_BACK, light);
	}

	private static void drawBox(GraphicsHolder gh, float x1, float y1, float z1, float x2, float y2, float z2, int color, int light) {
		// Front face (+Z)
		gh.drawTextureInWorld(x2, y2, z2, x1, y2, z2, x1, y1, z2, x2, y1, z2, 0f, 0f, 1f, 1f, Direction.SOUTH, color, light);
		// Back face (-Z)
		gh.drawTextureInWorld(x1, y2, z1, x2, y2, z1, x2, y1, z1, x1, y1, z1, 0f, 0f, 1f, 1f, Direction.NORTH, color, light);
		// Left face (-X)
		gh.drawTextureInWorld(x1, y2, z2, x1, y2, z1, x1, y1, z1, x1, y1, z2, 0f, 0f, 1f, 1f, Direction.WEST, color, light);
		// Right face (+X)
		gh.drawTextureInWorld(x2, y2, z1, x2, y2, z2, x2, y1, z2, x2, y1, z1, 0f, 0f, 1f, 1f, Direction.EAST, color, light);
		// Top face (+Y)
		gh.drawTextureInWorld(x2, y2, z1, x1, y2, z1, x1, y2, z2, x2, y2, z2, 0f, 0f, 1f, 1f, Direction.UP, color, light);
		// Bottom face (-Y)
		gh.drawTextureInWorld(x2, y1, z2, x1, y1, z2, x1, y1, z1, x2, y1, z1, 0f, 0f, 1f, 1f, Direction.DOWN, color, light);
	}
}
