package pl.szablix.mtrpolishpids.client.render;

import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.Vector3d;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.BlockEntityRenderer;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.client.IDrawing;
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
	private static final int COLOR_SIGN_EDGE = 0xFF4A4D50;

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
		final Direction facing = IBlock.getStatePropertySafe(world, blockPos, DirectionHelper.FACING);

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

		// 1. Render steel posts, mounting channels, and sign backing in EXTERIOR layer
		MainRenderer.scheduleRender(TEXTURE_WHITE, false, QueuedRenderLayer.EXTERIOR, (gh, offset) -> {
			gh.push();
			gh.translate(blockPos.getX() - offset.getXMapped() + 0.5, blockPos.getY() - offset.getYMapped(), blockPos.getZ() - offset.getZMapped() + 0.5);
			gh.rotateYDegrees(-facing.asRotation());

			// Left post (from ground y=0 to y=2.75)
			drawBoxShaded(gh, -postOffset - postR, 0.0f, -postR, -postOffset + postR, 2.75f, postR, light);

			// Right post (from ground y=0 to y=2.75)
			drawBoxShaded(gh, postOffset - postR, 0.0f, -postR, postOffset + postR, 2.75f, postR, light);

			// Horizontal aluminum mounting channels behind the board
			final float railLeft = -postOffset - 0.20f;
			final float railRight = postOffset + 0.20f;
			drawBox(gh, railLeft, 2.00f, 0.02f, railRight, 2.08f, 0.04f, COLOR_RAIL, light);
			drawBox(gh, railLeft, 2.50f, 0.02f, railRight, 2.58f, 0.04f, COLOR_RAIL, light);

			// Mounting clamps holding channels to posts
			drawBox(gh, -postOffset - 0.06f, 1.98f, -0.05f, -postOffset + 0.06f, 2.10f, 0.04f, COLOR_RAIL, light);
			drawBox(gh, -postOffset - 0.06f, 2.48f, -0.05f, -postOffset + 0.06f, 2.60f, 0.04f, COLOR_RAIL, light);
			drawBox(gh, postOffset - 0.06f, 1.98f, -0.05f, postOffset + 0.06f, 2.10f, 0.04f, COLOR_RAIL, light);
			drawBox(gh, postOffset - 0.06f, 2.48f, -0.05f, postOffset + 0.06f, 2.60f, 0.04f, COLOR_RAIL, light);

			// Sign backing sheet metal (galvanized steel)
			drawBox(gh, -halfW + 0.01f, 1.81f, 0.04f, halfW - 0.01f, 2.79f, 0.068f, COLOR_SIGN_BACK, light);

			gh.pop();
		});

		// 2. Render high-resolution front face with rounded corners in LIGHT_TRANSLUCENT layer
		MainRenderer.scheduleRender(tex.identifier, false, QueuedRenderLayer.LIGHT_TRANSLUCENT, (gh, offset) -> {
			gh.push();
			gh.translate(blockPos.getX() - offset.getXMapped() + 0.5, blockPos.getY() - offset.getYMapped(), blockPos.getZ() - offset.getZMapped() + 0.5);
			gh.rotateYDegrees(-facing.asRotation());

			IDrawing.drawTexture(gh, -halfW, 1.80f, 0.070f, halfW, 2.80f, 0.070f, 0f, 0f, 1f, 1f, Direction.UP, 0xFFFFFFFF, light);

			gh.pop();
		});
	}

	private static void drawBoxShaded(GraphicsHolder gh, float x1, float y1, float z1, float x2, float y2, float z2, int light) {
		// Front face (+Z)
		IDrawing.drawTexture(gh, x1, y1, z2, x2, y2, z2, 0f, 0f, 1f, 1f, Direction.UP, COLOR_POST_FRONT, light);
		// Back face (-Z)
		IDrawing.drawTexture(gh, x2, y1, z1, x1, y2, z1, 0f, 0f, 1f, 1f, Direction.UP, COLOR_POST_BACK, light);
		// Left face (-X)
		IDrawing.drawTexture(gh, x1, y1, z1, x1, y2, z2, 0f, 0f, 1f, 1f, Direction.UP, COLOR_POST_SIDE, light);
		// Right face (+X)
		IDrawing.drawTexture(gh, x2, y1, z2, x2, y2, z1, 0f, 0f, 1f, 1f, Direction.UP, COLOR_POST_SIDE, light);
		// Top face (+Y)
		IDrawing.drawTexture(gh, x1, y2, z1, x2, y2, z2, 0f, 0f, 1f, 1f, Direction.UP, COLOR_POST_FRONT, light);
		// Bottom face (-Y)
		IDrawing.drawTexture(gh, x1, y1, z2, x2, y1, z1, 0f, 0f, 1f, 1f, Direction.UP, COLOR_POST_BACK, light);
	}

	private static void drawBox(GraphicsHolder gh, float x1, float y1, float z1, float x2, float y2, float z2, int color, int light) {
		// Front face (+Z)
		IDrawing.drawTexture(gh, x1, y1, z2, x2, y2, z2, 0f, 0f, 1f, 1f, Direction.UP, color, light);
		// Back face (-Z)
		IDrawing.drawTexture(gh, x2, y1, z1, x1, y2, z1, 0f, 0f, 1f, 1f, Direction.UP, color, light);
		// Left face (-X)
		IDrawing.drawTexture(gh, x1, y1, z1, x1, y2, z2, 0f, 0f, 1f, 1f, Direction.UP, color, light);
		// Right face (+X)
		IDrawing.drawTexture(gh, x2, y1, z2, x2, y2, z1, 0f, 0f, 1f, 1f, Direction.UP, color, light);
		// Top face (+Y)
		IDrawing.drawTexture(gh, x1, y2, z1, x2, y2, z2, 0f, 0f, 1f, 1f, Direction.UP, color, light);
		// Bottom face (-Y)
		IDrawing.drawTexture(gh, x1, y1, z2, x2, y1, z1, 0f, 0f, 1f, 1f, Direction.UP, color, light);
	}
}
