package pl.szablix.mtrpolishpids.block;

import pl.szablix.mtrpolishpids.MTRPolishPIDS;
import pl.szablix.mtrpolishpids.block.entity.BlockEntityPKPPIDS;
import pl.szablix.mtrpolishpids.packet.PacketOpenPKPPIDSConfigScreen;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.Init;
import org.mtr.mod.block.BlockPIDSHorizontalBase;
import org.mtr.mod.block.IBlock;

public class BlockPKPPIDS extends BlockPIDSHorizontalBase {

	private static final int MAX_ARRIVALS = 1;

	public BlockPKPPIDS() {
		super(MAX_ARRIVALS);
	}

	@Override
	public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		return IBlock.checkHoldingBrush(world, player, () -> {
			final BlockPos newBlockPos = getBlockPosWithData.apply(world, pos);
			final BlockEntity entity = world.getBlockEntity(newBlockPos);
			if (entity != null && entity.data instanceof BlockEntityPKPPIDS) {
				final BlockEntityPKPPIDS pkpEntity = (BlockEntityPKPPIDS) entity.data;
				if (player.isSneaking()) {
					pkpEntity.toggleClockOrientation();
					final boolean right = pkpEntity.isClockOnRight();
					if (!world.isClient()) {
						world.updateListeners(newBlockPos, world.getBlockState(newBlockPos), world.getBlockState(newBlockPos), 3);
						if (!pos.equals(newBlockPos)) {
							world.updateListeners(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
						}
						player.sendMessage(Text.cast(TextHelper.translatable(right ? "message.mtr_polish_pack.clock_right" : "message.mtr_polish_pack.clock_left")), true);
					}
				} else {
					if (!world.isClient()) {
						pkpEntity.markDirty2();
						MTRPolishPIDS.REGISTRY.sendPacketToClient(ServerPlayerEntity.cast(player), new pl.szablix.mtrpolishpids.packet.PacketOpenPKPPIDSConfigScreen(newBlockPos, pkpEntity.maxArrivals, pkpEntity.isClockOnRight()));
					}
				}
			}
		});
	}

	@Override
	public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		final Direction facing = IBlock.getStatePropertySafe(state, FACING);
		final VoxelShape casing = IBlock.getVoxelShapeByDirection(5.65, 1.125, 0, 10.35, 11.75, 16, facing);
		final VoxelShape pole = IBlock.getVoxelShapeByDirection(7.25, 11.75, 11.75, 8.75, 16, 13.25, facing);
		return VoxelShapes.union(casing, pole);
	}

	@Override
	public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
		return new BlockEntityPKPPIDS(blockPos, blockState);
	}
}
