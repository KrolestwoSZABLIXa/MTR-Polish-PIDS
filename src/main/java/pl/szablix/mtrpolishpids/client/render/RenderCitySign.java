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

		final CitySignTextureManager.DynamicCitySignTexture tex = CitySignTextureManager.getSignTexture(cityName, endOfTown);
		if (tex == null) {
			return;
		}

		final float boardWidth = tex.worldWidth;
		final float halfW = boardWidth / 2.0f;
		final float postOffset = Math.max(0.35f, halfW * 0.65f);
		final float postR = 0.04f; // 8 cm wide steel posts

		// 1. Render steel posts, mounting channels, and sign backing sheet in EXTERIOR layer
		MainRenderer.scheduleRender(TEXTURE_WHITE, false, QueuedRenderLayer.EXTERIOR, (gh, offset) -> {
			gh.push();
			gh.translate(blockPos.getX() - offset.getXMapped() + 0.5, blockPos.getY() - offset.getYMapped(), blockPos.getZ() - offset.getZMapped() + 0.5);
			gh.rotateYDegrees(-facing.asRotation());

			// Left post (from ground y=0 to y=2.95)
			drawBoxShaded(gh, -postOffset - postR, 0.0f, -postR, -postOffset + postR, 2.95f, postR, light);

			// Right post (from ground y=0 to y=2.95)
			drawBoxShaded(gh, postOffset - postR, 0.0f, -postR, postOffset + postR, 2.95f, postR, light);

			// Horizontal aluminum mounting channels behind the board
			final float railLeft = -postOffset - 0.20f;
			final float railRight = postOffset + 0.20f;
			drawBox(gh, railLeft, 2.20f, 0.040f, railRight, 2.28f, 0.055f, COLOR_RAIL, light);
			drawBox(gh, railLeft, 2.70f, 0.040f, railRight, 2.78f, 0.055f, COLOR_RAIL, light);

			// Mounting clamps holding channels to posts
			drawBox(gh, -postOffset - 0.06f, 2.18f, -0.045f, -postOffset + 0.06f, 2.30f, 0.055f, COLOR_RAIL, light);
			drawBox(gh, -postOffset - 0.06f, 2.68f, -0.045f, -postOffset + 0.06f, 2.80f, 0.055f, COLOR_RAIL, light);
			drawBox(gh, postOffset - 0.06f, 2.18f, -0.045f, postOffset + 0.06f, 2.30f, 0.055f, COLOR_RAIL, light);
			drawBox(gh, postOffset - 0.06f, 2.68f, -0.045f, postOffset + 0.06f, 2.80f, 0.055f, COLOR_RAIL, light);

			// Sign backing sheet metal (galvanized steel)
			drawBox(gh, -halfW + 0.01f, 2.01f, 0.055f, halfW - 0.01f, 2.99f, 0.070f, COLOR_SIGN_BACK, light);

			gh.pop();
		});

		// 2. Render high-resolution front face in EXTERIOR cutout layer (sharp alpha cutout, proper lighting & CCW winding)
		MainRenderer.scheduleRender(tex.identifier, false, QueuedRenderLayer.EXTERIOR, (gh, offset) -> {
			gh.push();
			gh.translate(blockPos.getX() - offset.getXMapped() + 0.5, blockPos.getY() - offset.getYMapped(), blockPos.getZ() - offset.getZMapped() + 0.5);
			gh.rotateYDegrees(-facing.asRotation());

			// Front face with green road sign texture (counter-clockwise quad facing +Z / SOUTH towards viewer)
			// In drawTextureInWorld: V1=(u1, v2), V2=(u2, v2), V3=(u2, v1), V4=(u1, v1)
			// For right-side up, top vertices V1 and V2 must receive v2=0, and bottom vertices V3 and V4 must receive v1=1.
			gh.drawTextureInWorld(
					halfW, 3.00f, 0.071f,
					-halfW, 3.00f, 0.071f,
					-halfW, 2.00f, 0.071f,
					halfW, 2.00f, 0.071f,
					1f, 1f, 0f, 0f,
					Direction.SOUTH, 0xFFFFFFFF, light
			);

			gh.pop();
		});
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
