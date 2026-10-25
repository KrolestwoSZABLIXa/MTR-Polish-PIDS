package pl.szablix.mtrpolishpids.packet;

import pl.szablix.mtrpolishpids.block.entity.BlockEntityPKPPIDS;
import org.mtr.mapping.holder.BlockEntity;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;
import org.mtr.mod.Init;
import org.mtr.mod.block.IBlock;

public class PacketUpdatePKPPIDSClock extends PacketHandler {

	private final BlockPos blockPos;
	private final boolean clockOnRight;

	public PacketUpdatePKPPIDSClock(PacketBufferReceiver receiver) {
		this.blockPos = BlockPos.fromLong(receiver.readLong());
		this.clockOnRight = receiver.readBoolean();
	}

	public PacketUpdatePKPPIDSClock(BlockPos blockPos, boolean clockOnRight) {
		this.blockPos = blockPos;
		this.clockOnRight = clockOnRight;
	}

	@Override
	public void write(PacketBufferSender sender) {
		sender.writeLong(blockPos.asLong());
		sender.writeBoolean(clockOnRight);
	}

	@Override
	public void runServer(MinecraftServer server, ServerPlayerEntity player) {
		final World world = player.getEntityWorld();
		if (world == null || !Init.isChunkLoaded(world, blockPos)) {
			return;
		}

		final BlockEntity entity = world.getBlockEntity(blockPos);
		if (entity != null && entity.data instanceof BlockEntityPKPPIDS) {
			final BlockEntityPKPPIDS pkpEntity = (BlockEntityPKPPIDS) entity.data;
			pkpEntity.setClockOnRight(clockOnRight);
			world.updateListeners(blockPos, world.getBlockState(blockPos), world.getBlockState(blockPos), 3);

			final Direction facing = IBlock.getStatePropertySafe(world, blockPos, DirectionHelper.FACING);
			final BlockPos otherPos = blockPos.offset(facing);
			if (Init.isChunkLoaded(world, otherPos)) {
				world.updateListeners(otherPos, world.getBlockState(otherPos), world.getBlockState(otherPos), 3);
			}
		}
	}
}
