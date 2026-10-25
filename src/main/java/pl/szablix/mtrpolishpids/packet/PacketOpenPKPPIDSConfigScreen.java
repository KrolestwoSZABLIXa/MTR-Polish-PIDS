package pl.szablix.mtrpolishpids.packet;

import pl.szablix.mtrpolishpids.client.PKPClientPacketHelper;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

public class PacketOpenPKPPIDSConfigScreen extends PacketHandler {

	private final BlockPos blockPos;
	private final int maxArrivals;
	private final boolean clockOnRight;

	public PacketOpenPKPPIDSConfigScreen(PacketBufferReceiver receiver) {
		this.blockPos = BlockPos.fromLong(receiver.readLong());
		this.maxArrivals = receiver.readInt();
		this.clockOnRight = receiver.readBoolean();
	}

	public PacketOpenPKPPIDSConfigScreen(BlockPos blockPos, int maxArrivals, boolean clockOnRight) {
		this.blockPos = blockPos;
		this.maxArrivals = maxArrivals;
		this.clockOnRight = clockOnRight;
	}

	@Override
	public void write(PacketBufferSender sender) {
		sender.writeLong(blockPos.asLong());
		sender.writeInt(maxArrivals);
		sender.writeBoolean(clockOnRight);
	}

	@Override
	public void runClient() {
		PKPClientPacketHelper.openPKPPIDSConfigScreen(blockPos, maxArrivals, clockOnRight);
	}
}
