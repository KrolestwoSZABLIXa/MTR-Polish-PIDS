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
	private final boolean twoSided;
	private final String cityNameBack;
	private final boolean endOfTownBack;

	public PacketOpenCitySignConfigScreen(PacketBufferReceiver receiver) {
		this.blockPos = BlockPos.fromLong(receiver.readLong());
		this.cityName = receiver.readString();
		this.endOfTown = receiver.readBoolean();
		this.twoSided = receiver.readBoolean();
		this.cityNameBack = receiver.readString();
		this.endOfTownBack = receiver.readBoolean();
	}

	public PacketOpenCitySignConfigScreen(BlockPos blockPos, String cityName, boolean endOfTown, boolean twoSided, String cityNameBack, boolean endOfTownBack) {
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
	public void runClient() {
		PKPClientPacketHelper.openCitySignConfigScreen(blockPos, cityName, endOfTown, twoSided, cityNameBack, endOfTownBack);
	}
}
