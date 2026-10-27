package pl.szablix.mtrpolishpids.packet;

import org.mtr.mapping.holder.BlockEntity;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;
import org.mtr.mod.Init;
import pl.szablix.mtrpolishpids.block.entity.BlockEntityCitySign;

public class PacketUpdateCitySign extends PacketHandler {

	private final BlockPos blockPos;
	private final String cityName;
	private final boolean endOfTown;
	private final boolean twoSided;
	private final String cityNameBack;
	private final boolean endOfTownBack;

	public PacketUpdateCitySign(PacketBufferReceiver receiver) {
		this.blockPos = BlockPos.fromLong(receiver.readLong());
		this.cityName = receiver.readString();
		this.endOfTown = receiver.readBoolean();
		this.twoSided = receiver.readBoolean();
		this.cityNameBack = receiver.readString();
		this.endOfTownBack = receiver.readBoolean();
	}

	public PacketUpdateCitySign(BlockPos blockPos, String cityName, boolean endOfTown, boolean twoSided, String cityNameBack, boolean endOfTownBack) {
		this.blockPos = blockPos;
		this.cityName = cityName;
		this.endOfTown = endOfTown;
		this.twoSided = twoSided;
		this.cityNameBack = cityNameBack;
		this.endOfTownBack = endOfTownBack;
	}

	@Override
	public void write(PacketBufferSender sender) {
		sender.writeLong(blockPos.asLong());
		sender.writeString(cityName);
		sender.writeBoolean(endOfTown);
		sender.writeBoolean(twoSided);
		sender.writeString(cityNameBack);
		sender.writeBoolean(endOfTownBack);
	}

	@Override
	public void runServer(MinecraftServer server, ServerPlayerEntity player) {
		final World world = player.getEntityWorld();
		if (world == null || !Init.isChunkLoaded(world, blockPos)) {
			return;
		}

		final BlockEntity entity = world.getBlockEntity(blockPos);
		if (entity != null && entity.data instanceof BlockEntityCitySign) {
			final BlockEntityCitySign citySignEntity = (BlockEntityCitySign) entity.data;
			citySignEntity.setCityName(cityName);
			citySignEntity.setEndOfTown(endOfTown);
			citySignEntity.setTwoSided(twoSided);
			citySignEntity.setCityNameBack(cityNameBack);
			citySignEntity.setEndOfTownBack(endOfTownBack);
			world.updateListeners(blockPos, world.getBlockState(blockPos), world.getBlockState(blockPos), 3);
			world.updateListeners(blockPos.up(), world.getBlockState(blockPos.up()), world.getBlockState(blockPos.up()), 3);
			world.updateListeners(blockPos.up(2), world.getBlockState(blockPos.up(2)), world.getBlockState(blockPos.up(2)), 3);
		}
	}
}
