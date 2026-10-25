package pl.szablix.mtrpolishpids.client.screen;

import pl.szablix.mtrpolishpids.block.entity.BlockEntityPKPPIDS;
import pl.szablix.mtrpolishpids.client.MTRPolishPIDSClient;
import pl.szablix.mtrpolishpids.packet.PacketUpdatePKPPIDSClock;
import org.mtr.mapping.holder.BlockEntity;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.Text;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.screen.PIDSConfigScreen;

public class PKPPIDSConfigScreen extends PIDSConfigScreen {

	private final BlockPos blockPos;
	private boolean clockOnRight;
	private ButtonWidgetExtension buttonClockSide;

	public PKPPIDSConfigScreen(BlockPos blockPos, int maxArrivals, boolean clockOnRight) {
		super(blockPos, maxArrivals);
		this.blockPos = blockPos;
		this.clockOnRight = clockOnRight;
	}

	@Override
	protected void init2() {
		super.init2();

		final BlockEntity entity = MinecraftClient.getInstance().getWorldMapped().getBlockEntity(blockPos);
		if (entity != null && entity.data instanceof BlockEntityPKPPIDS) {
			clockOnRight = ((BlockEntityPKPPIDS) entity.data).isClockOnRight();
		}

		buttonClockSide = new ButtonWidgetExtension(120, 60, 100, 20, TextHelper.translatable(getClockSideKey()), button -> {
			clockOnRight = !clockOnRight;
			buttonClockSide.setMessage2(Text.cast(TextHelper.translatable(getClockSideKey())));
			if (entity != null && entity.data instanceof BlockEntityPKPPIDS) {
				((BlockEntityPKPPIDS) entity.data).setClockOnRight(clockOnRight);
			}
			MTRPolishPIDSClient.REGISTRY_CLIENT.sendPacketToServer(new PacketUpdatePKPPIDSClock(blockPos, clockOnRight));
		});

		IDrawing.setPositionAndWidth(buttonClockSide, 120, 60, 100);
		addChild(new ClickableWidget(buttonClockSide));
	}

	@Override
	public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta) {
		super.render(graphicsHolder, mouseX, mouseY, tickDelta);
		graphicsHolder.drawText(TextHelper.translatable("gui.mtr_polish_pids.clock_side"), 120, 46, -1, false, GraphicsHolder.getDefaultLight());
	}

	private String getClockSideKey() {
		return clockOnRight ? "gui.mtr_polish_pids.clock_right" : "gui.mtr_polish_pids.clock_left";
	}
}
