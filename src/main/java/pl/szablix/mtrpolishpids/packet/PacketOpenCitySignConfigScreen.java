package pl.szablix.mtrpolishpids.packet;

import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;
import pl.szablix.mtrpolishpids.client.PKPClientPacketHelper;

public class PacketOpenCitySignConfigScreen extends PacketHandler {

	private final BlockPos blockPos;
	private final String cityName;
	private final boolean endOfTown;

	public PacketOpenCitySignConfigScreen(PacketBufferReceiver receiver) {
		this.blockPos = BlockPos.fromLong(receiver.readLong());
		this.cityName = receiver.readString();
		this.endOfTown = receiver.readBoolean();
	}

	public PacketOpenCitySignConfigScreen(BlockPos blockPos, String cityName, boolean endOfTown) {
		this.blockPos = blockPos;
		this.cityName = cityName;
		this.endOfTown = endOfTown;
	}

	@Override
	public void write(PacketBufferSender sender) {
		sender.writeLong(blockPos.asLong());
		sender.writeString(cityName);
		sender.writeBoolean(endOfTown);
	}

	@Override
	public void runClient() {
		PKPClientPacketHelper.openCitySignConfigScreen(blockPos, cityName, endOfTown);
	}
}
