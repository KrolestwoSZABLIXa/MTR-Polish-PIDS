package pl.szablix.mtrpolishpids.block;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockExtension;
import org.mtr.mapping.mapper.BlockHelper;
import org.mtr.mapping.mapper.BlockWithEntity;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.block.IBlock;
import pl.szablix.mtrpolishpids.MTRPolishPIDS;
import pl.szablix.mtrpolishpids.block.entity.BlockEntityCitySign;
import pl.szablix.mtrpolishpids.packet.PacketOpenCitySignConfigScreen;

import java.util.List;

public class BlockCitySign extends BlockExtension implements DirectionHelper, BlockWithEntity, IBlock {

	public BlockCitySign() {
		super(BlockHelper.createBlockSettings(false, false));
		setDefaultState2(getDefaultState2()
				.with(new Property<>(FACING.data), Direction.NORTH.data)
				.with(new Property<>(THIRD.data), IBlock.EnumThird.LOWER));
	}

	@Override
	public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		final IBlock.EnumThird third = IBlock.getStatePropertySafe(state, THIRD);
		final BlockPos rootPos;
		switch (third) {
			case UPPER:
				rootPos = pos.down(2);
				break;
			case MIDDLE:
				rootPos = pos.down();
				break;
			case LOWER:
			default:
				rootPos = pos;
				break;
		}

		return IBlock.checkHoldingBrush(world, player, () -> {
			final BlockEntity entity = world.getBlockEntity(rootPos);
			if (entity != null && entity.data instanceof BlockEntityCitySign) {
				final BlockEntityCitySign citySignEntity = (BlockEntityCitySign) entity.data;
				if (player.isSneaking()) {
					citySignEntity.toggleEndOfTown();
					final boolean endOfTown = citySignEntity.isEndOfTown();
					if (!world.isClient()) {
						world.updateListeners(rootPos, world.getBlockState(rootPos), world.getBlockState(rootPos), 3);
						world.updateListeners(rootPos.up(), world.getBlockState(rootPos.up()), world.getBlockState(rootPos.up()), 3);
						world.updateListeners(rootPos.up(2), world.getBlockState(rootPos.up(2)), world.getBlockState(rootPos.up(2)), 3);
						player.sendMessage(Text.cast(TextHelper.translatable(endOfTown ? "message.mtr_polish_pack.city_sign_e18a" : "message.mtr_polish_pack.city_sign_e17a", citySignEntity.getCityName())), true);
					}
				} else {
					if (!world.isClient()) {
						citySignEntity.markDirty2();
						MTRPolishPIDS.REGISTRY.sendPacketToClient(ServerPlayerEntity.cast(player), new PacketOpenCitySignConfigScreen(
								rootPos,
								citySignEntity.getCityName(),
								citySignEntity.isEndOfTown(),
								citySignEntity.isTwoSided(),
								citySignEntity.getCityNameBack(),
								citySignEntity.isEndOfTownBack()
						));
					}
				}
			}
		});
	}

	@Override
	public BlockState getPlacementState2(ItemPlacementContext ctx) {
		final Direction facing = ctx.getPlayerFacing().getOpposite();
		return IBlock.isReplaceable(ctx, Direction.UP, 3)
				? getDefaultState2().with(new Property<>(FACING.data), facing.data).with(new Property<>(THIRD.data), IBlock.EnumThird.LOWER)
				: null;
	}

	@Override
	public void onPlaced2(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
		if (!world.isClient()) {
			final Direction facing = IBlock.getStatePropertySafe(state, FACING);
			world.setBlockState(pos.up(), getDefaultState2().with(new Property<>(FACING.data), facing.data).with(new Property<>(THIRD.data), IBlock.EnumThird.MIDDLE), 3);
			world.setBlockState(pos.up(2), getDefaultState2().with(new Property<>(FACING.data), facing.data).with(new Property<>(THIRD.data), IBlock.EnumThird.UPPER), 3);
			world.updateNeighbors(pos, Blocks.getAirMapped());
			state.updateNeighbors(new WorldAccess(world.data), pos, 3);
		}
	}

	@Override
	public BlockState getStateForNeighborUpdate2(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
		final IBlock.EnumThird third = IBlock.getStatePropertySafe(state, THIRD);
		if (direction == Direction.UP && third != IBlock.EnumThird.UPPER) {
			if (!neighborState.isOf(new Block(this))) {
				return Blocks.getAirMapped().getDefaultState();
			}
		}
		if (direction == Direction.DOWN && third != IBlock.EnumThird.LOWER) {
			if (!neighborState.isOf(new Block(this))) {
				return Blocks.getAirMapped().getDefaultState();
			}
		}
		return state;
	}

	@Override
	public void onBreak2(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		final IBlock.EnumThird third = IBlock.getStatePropertySafe(state, THIRD);
		final BlockPos rootPos;
		switch (third) {
			case UPPER:
				rootPos = pos.down(2);
				break;
			case MIDDLE:
				rootPos = pos.down();
				break;
			case LOWER:
			default:
				rootPos = pos;
				break;
		}

		if (!world.isClient() && player != null && player.isCreative()) {
			IBlock.onBreakCreative(world, player, rootPos);
			IBlock.onBreakCreative(world, player, rootPos.up());
			IBlock.onBreakCreative(world, player, rootPos.up(2));
		}
		super.onBreak2(world, pos, state, player);
	}

	@Override
	public void addBlockProperties(List<HolderBase<?>> properties) {
		properties.add(FACING);
		properties.add(THIRD);
	}

	@Override
	public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		final Direction facing = IBlock.getStatePropertySafe(state, FACING);
		final IBlock.EnumThird third = IBlock.getStatePropertySafe(state, THIRD);

		final BlockPos rootPos;
		switch (third) {
			case UPPER:
				rootPos = pos.down(2);
				break;
			case MIDDLE:
				rootPos = pos.down();
				break;
			case LOWER:
			default:
				rootPos = pos;
				break;
		}

		float halfW = pl.szablix.mtrpolishpids.block.entity.CitySignGeometry.getSignHalfWidth("Warszawa");
		float postOffset = pl.szablix.mtrpolishpids.block.entity.CitySignGeometry.getPostOffset(halfW);
		final BlockEntity entity = world.getBlockEntity(rootPos);
		if (entity != null && entity.data instanceof BlockEntityCitySign) {
			final BlockEntityCitySign citySignEntity = (BlockEntityCitySign) entity.data;
			postOffset = citySignEntity.getPostOffset();
			halfW = citySignEntity.getHalfWidth();
		}

		switch (third) {
			case UPPER:
				// Sign board dynamically scales with text width (centered at x = 8)
				final double halfWPx = Math.max(16.0, halfW * 16.0);
				return IBlock.getVoxelShapeByDirection(8.0 - halfWPx, 2, 4, 8.0 + halfWPx, 16, 12, facing);
			case MIDDLE:
			case LOWER:
			default:
				// Two solid posts matched dynamically to postOffset (never ghosts, open passage in middle)
				final double offsetPx = postOffset * 16.0;
				final VoxelShape post1 = IBlock.getVoxelShapeByDirection(8.0 - offsetPx - 0.8, 0, 6, 8.0 - offsetPx + 0.8, 16, 10, facing);
				final VoxelShape post2 = IBlock.getVoxelShapeByDirection(8.0 + offsetPx - 0.8, 0, 6, 8.0 + offsetPx + 0.8, 16, 10, facing);
				return VoxelShapes.union(post1, post2);
		}
	}

	@Override
	public VoxelShape getCollisionShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return getOutlineShape2(state, world, pos, context);
	}

	@Override
	public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
		final IBlock.EnumThird third = IBlock.getStatePropertySafe(blockState, THIRD);
		if (third == IBlock.EnumThird.LOWER) {
			return new BlockEntityCitySign(blockPos, blockState);
		}
		return null;
	}
}
