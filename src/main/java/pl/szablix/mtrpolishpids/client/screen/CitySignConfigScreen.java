package pl.szablix.mtrpolishpids.client.screen;

import org.mtr.mapping.holder.BlockEntity;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.CheckboxWidgetExtension;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.ScreenExtension;
import org.mtr.mapping.mapper.TextFieldWidgetExtension;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mapping.tool.TextCase;
import org.mtr.mod.client.IDrawing;
import pl.szablix.mtrpolishpids.block.entity.BlockEntityCitySign;
import pl.szablix.mtrpolishpids.client.MTRPolishPIDSClient;
import pl.szablix.mtrpolishpids.packet.PacketUpdateCitySign;

public class CitySignConfigScreen extends ScreenExtension {

	private final BlockPos blockPos;
	private String cityName;
	private boolean endOfTown;

	private final TextFieldWidgetExtension textFieldCityName;
	private final CheckboxWidgetExtension checkboxEndOfTown;
	private final ButtonWidgetExtension buttonSave;
	private final ButtonWidgetExtension buttonCancel;

	public CitySignConfigScreen(BlockPos blockPos, String cityName, boolean endOfTown) {
		super(TextHelper.translatable("gui.mtr_polish_pack.city_sign_title"));
		this.blockPos = blockPos;
		this.cityName = (cityName == null || cityName.trim().isEmpty()) ? "Warszawa" : cityName.trim();
		this.endOfTown = endOfTown;

		textFieldCityName = new TextFieldWidgetExtension(0, 0, 0, 20, 60, TextCase.DEFAULT, null, "Warszawa");
		checkboxEndOfTown = new CheckboxWidgetExtension(0, 0, 0, 20, TextHelper.translatable("gui.mtr_polish_pack.end_of_town"), endOfTown, checked -> this.endOfTown = checked);
		buttonSave = new ButtonWidgetExtension(0, 0, 0, 20, TextHelper.translatable("gui.mtr_polish_pack.save"), button -> {
			String updatedName = textFieldCityName.getText2().trim();
			if (updatedName.isEmpty()) {
				updatedName = "Warszawa";
			}
			final boolean updatedEnd = checkboxEndOfTown.isChecked2();
			final BlockEntity entity = MinecraftClient.getInstance().getWorldMapped().getBlockEntity(blockPos);
			if (entity != null && entity.data instanceof BlockEntityCitySign) {
				final BlockEntityCitySign citySignEntity = (BlockEntityCitySign) entity.data;
				citySignEntity.setCityName(updatedName);
				citySignEntity.setEndOfTown(updatedEnd);
			}
			MTRPolishPIDSClient.REGISTRY_CLIENT.sendPacketToServer(new PacketUpdateCitySign(blockPos, updatedName, updatedEnd));
			onClose2();
		});
		buttonCancel = new ButtonWidgetExtension(0, 0, 0, 20, TextHelper.translatable("gui.cancel"), button -> onClose2());
	}

	@Override
	protected void init2() {
		super.init2();

		final BlockEntity entity = MinecraftClient.getInstance().getWorldMapped().getBlockEntity(blockPos);
		if (entity != null && entity.data instanceof BlockEntityCitySign) {
			final BlockEntityCitySign citySignEntity = (BlockEntityCitySign) entity.data;
			cityName = citySignEntity.getCityName();
			endOfTown = citySignEntity.isEndOfTown();
		}

		final int centerX = width / 2;
		final int centerY = height / 2;

		IDrawing.setPositionAndWidth(textFieldCityName, centerX - 100, centerY - 25, 200);
		textFieldCityName.setText2(cityName);
		addChild(new ClickableWidget(textFieldCityName));

		IDrawing.setPositionAndWidth(checkboxEndOfTown, centerX - 100, centerY + 5, 200);
		checkboxEndOfTown.setChecked(endOfTown);
		addChild(new ClickableWidget(checkboxEndOfTown));

		IDrawing.setPositionAndWidth(buttonSave, centerX - 100, centerY + 40, 95);
		addChild(new ClickableWidget(buttonSave));

		IDrawing.setPositionAndWidth(buttonCancel, centerX + 5, centerY + 40, 95);
		addChild(new ClickableWidget(buttonCancel));
	}

	@Override
	public void tick2() {
		super.tick2();
		textFieldCityName.tick2();
	}

	@Override
	public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta) {
		renderBackground(graphicsHolder);

		final int centerX = width / 2;
		final int centerY = height / 2;

		graphicsHolder.drawCenteredText(TextHelper.translatable("gui.mtr_polish_pack.city_sign_title"), centerX, centerY - 65, -1);
		graphicsHolder.drawText(TextHelper.translatable("gui.mtr_polish_pack.city_name"), centerX - 100, centerY - 38, 0xFFA0A0A0, false, GraphicsHolder.getDefaultLight());

		super.render(graphicsHolder, mouseX, mouseY, tickDelta);
	}
}
