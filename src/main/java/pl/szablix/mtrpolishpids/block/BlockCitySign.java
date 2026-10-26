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

public class BlockCitySign extends BlockExtension implements DirectionHelper, BlockWithEntity {

	public BlockCitySign() {
		super(BlockHelper.createBlockSettings(false, false));
		setDefaultState2(getDefaultState2().with(new Property<>(FACING.data), Direction.NORTH.data));
	}

	@Override
	public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		return IBlock.checkHoldingBrush(world, player, () -> {
			final BlockEntity entity = world.getBlockEntity(pos);
			if (entity != null && entity.data instanceof BlockEntityCitySign) {
				final BlockEntityCitySign citySignEntity = (BlockEntityCitySign) entity.data;
				if (player.isSneaking()) {
					citySignEntity.toggleEndOfTown();
					final boolean endOfTown = citySignEntity.isEndOfTown();
					if (!world.isClient()) {
						world.updateListeners(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
						player.sendMessage(Text.cast(TextHelper.translatable(endOfTown ? "message.mtr_polish_pids.city_sign_e18a" : "message.mtr_polish_pids.city_sign_e17a", citySignEntity.getCityName())), true);
					}
				} else {
					if (!world.isClient()) {
						citySignEntity.markDirty2();
						MTRPolishPIDS.REGISTRY.sendPacketToClient(ServerPlayerEntity.cast(player), new PacketOpenCitySignConfigScreen(pos, citySignEntity.getCityName(), citySignEntity.isEndOfTown()));
					}
				}
			}
		});
	}

	@Override
	public BlockState getPlacementState2(ItemPlacementContext ctx) {
		final Direction facing = ctx.getPlayerFacing().getOpposite();
		return getDefaultState2().with(new Property<>(FACING.data), facing.data);
	}

	@Override
	public void addBlockProperties(List<HolderBase<?>> properties) {
		properties.add(FACING);
	}

	@Override
	public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		final Direction facing = IBlock.getStatePropertySafe(state, FACING);
		return IBlock.getVoxelShapeByDirection(1, 0, 7, 15, 48, 9, facing);
	}

	@Override
	public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
		return new BlockEntityCitySign(blockPos, blockState);
	}
}
