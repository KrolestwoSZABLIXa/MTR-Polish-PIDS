package com.szablix.mtrpolishpids.block;

import com.szablix.mtrpolishpids.block.entity.BlockEntityPKPPIDS;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.Init;
import org.mtr.mod.block.BlockPIDSHorizontalBase;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.packet.PacketOpenPIDSConfigScreen;

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
					player.sendMessage(Text.cast(TextHelper.translatable(right ? "message.mtr_polish_pids.clock_right" : "message.mtr_polish_pids.clock_left")), true);
				} else {
					pkpEntity.markDirty2();
					Init.REGISTRY.sendPacketToClient(ServerPlayerEntity.cast(player), new PacketOpenPIDSConfigScreen(newBlockPos, pkpEntity.maxArrivals));
				}
			}
		});
	}

	@Override
	public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		final Direction facing = IBlock.getStatePropertySafe(state, FACING);
		final VoxelShape casing = IBlock.getVoxelShapeByDirection(6, 2, 0, 10, 11, 16, facing);
		final VoxelShape pole = IBlock.getVoxelShapeByDirection(7.25, 11, 11.5, 8.75, 16, 13.5, facing);
		return VoxelShapes.union(casing, pole);
	}

	@Override
	public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
		return new BlockEntityPKPPIDS(blockPos, blockState);
	}
}
