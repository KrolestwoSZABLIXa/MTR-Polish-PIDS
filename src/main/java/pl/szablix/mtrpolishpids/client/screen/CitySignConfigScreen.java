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
	private boolean twoSided;
	private String cityNameBack;
	private boolean endOfTownBack;

	private final TextFieldWidgetExtension textFieldCityName;
	private final CheckboxWidgetExtension checkboxEndOfTown;
	private final CheckboxWidgetExtension checkboxTwoSided;
	private final TextFieldWidgetExtension textFieldCityNameBack;
	private final CheckboxWidgetExtension checkboxEndOfTownBack;
	private final ButtonWidgetExtension buttonSave;
	private final ButtonWidgetExtension buttonCancel;

	public CitySignConfigScreen(BlockPos blockPos, String cityName, boolean endOfTown, boolean twoSided, String cityNameBack, boolean endOfTownBack) {
		super(TextHelper.translatable("gui.mtr_polish_pack.city_sign_title"));
		this.blockPos = blockPos;
		this.cityName = (cityName == null || cityName.trim().isEmpty()) ? "Warszawa" : cityName.trim();
		this.endOfTown = endOfTown;
		this.twoSided = twoSided;
		this.cityNameBack = (cityNameBack == null || cityNameBack.trim().isEmpty()) ? this.cityName : cityNameBack.trim();
		this.endOfTownBack = endOfTownBack;

		textFieldCityName = new TextFieldWidgetExtension(0, 0, 0, 20, 60, TextCase.DEFAULT, null, "Warszawa");
		checkboxEndOfTown = new CheckboxWidgetExtension(0, 0, 0, 20, TextHelper.translatable("gui.mtr_polish_pack.end_of_town_front"), endOfTown, checked -> this.endOfTown = checked);
		checkboxTwoSided = new CheckboxWidgetExtension(0, 0, 0, 20, TextHelper.translatable("gui.mtr_polish_pack.two_sided"), twoSided, checked -> {
			this.twoSided = checked;
			updateVisibility();
		});
		textFieldCityNameBack = new TextFieldWidgetExtension(0, 0, 0, 20, 60, TextCase.DEFAULT, null, "Warszawa");
		checkboxEndOfTownBack = new CheckboxWidgetExtension(0, 0, 0, 20, TextHelper.translatable("gui.mtr_polish_pack.end_of_town_back"), endOfTownBack, checked -> this.endOfTownBack = checked);

		buttonSave = new ButtonWidgetExtension(0, 0, 0, 20, TextHelper.translatable("gui.mtr_polish_pack.save"), button -> {
			String updatedName = textFieldCityName.getText2().trim();
			if (updatedName.isEmpty()) {
				updatedName = "Warszawa";
			}
			final boolean updatedEnd = checkboxEndOfTown.isChecked2();
			final boolean updatedTwoSided = checkboxTwoSided.isChecked2();
			String updatedNameBack = textFieldCityNameBack.getText2().trim();
			if (updatedNameBack.isEmpty()) {
				updatedNameBack = updatedName;
			}
			final boolean updatedEndBack = checkboxEndOfTownBack.isChecked2();

			final BlockEntity entity = MinecraftClient.getInstance().getWorldMapped().getBlockEntity(blockPos);
			if (entity != null && entity.data instanceof BlockEntityCitySign) {
				final BlockEntityCitySign citySignEntity = (BlockEntityCitySign) entity.data;
				citySignEntity.setCityName(updatedName);
				citySignEntity.setEndOfTown(updatedEnd);
				citySignEntity.setTwoSided(updatedTwoSided);
				citySignEntity.setCityNameBack(updatedNameBack);
				citySignEntity.setEndOfTownBack(updatedEndBack);
			}
			MTRPolishPIDSClient.REGISTRY_CLIENT.sendPacketToServer(new PacketUpdateCitySign(blockPos, updatedName, updatedEnd, updatedTwoSided, updatedNameBack, updatedEndBack));
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
			twoSided = citySignEntity.isTwoSided();
			cityNameBack = citySignEntity.getCityNameBack();
			endOfTownBack = citySignEntity.isEndOfTownBack();
		}

		final int centerX = width / 2;
		final int centerY = height / 2;

		// Front section
		IDrawing.setPositionAndWidth(textFieldCityName, centerX - 100, centerY - 65, 200);
		textFieldCityName.setText2(cityName);
		addChild(new ClickableWidget(textFieldCityName));

		IDrawing.setPositionAndWidth(checkboxEndOfTown, centerX - 100, centerY - 40, 20);
		checkboxEndOfTown.setChecked(endOfTown);
		addChild(new ClickableWidget(checkboxEndOfTown));

		// Two sided checkbox
		IDrawing.setPositionAndWidth(checkboxTwoSided, centerX - 100, centerY - 15, 20);
		checkboxTwoSided.setChecked(twoSided);
		addChild(new ClickableWidget(checkboxTwoSided));

		// Back section
		IDrawing.setPositionAndWidth(textFieldCityNameBack, centerX - 100, centerY + 10, 200);
		textFieldCityNameBack.setText2(cityNameBack);
		addChild(new ClickableWidget(textFieldCityNameBack));

		IDrawing.setPositionAndWidth(checkboxEndOfTownBack, centerX - 100, centerY + 35, 20);
		checkboxEndOfTownBack.setChecked(endOfTownBack);
		addChild(new ClickableWidget(checkboxEndOfTownBack));

		// Save & Cancel buttons
		IDrawing.setPositionAndWidth(buttonSave, centerX - 100, centerY + 65, 95);
		addChild(new ClickableWidget(buttonSave));

		IDrawing.setPositionAndWidth(buttonCancel, centerX + 5, centerY + 65, 95);
		addChild(new ClickableWidget(buttonCancel));

		updateVisibility();
	}

	private void updateVisibility() {
		final boolean isTwoSided = checkboxTwoSided.isChecked2();
		textFieldCityNameBack.setVisibleMapped(isTwoSided);
		checkboxEndOfTownBack.setVisibleMapped(isTwoSided);
	}

	@Override
	public void tick2() {
		super.tick2();
		textFieldCityName.tick2();
		textFieldCityNameBack.tick2();
	}

	@Override
	public void render(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta) {
		renderBackground(graphicsHolder);

		final int centerX = width / 2;
		final int centerY = height / 2;

		graphicsHolder.drawCenteredText(TextHelper.translatable("gui.mtr_polish_pack.city_sign_title"), centerX, centerY - 95, -1);

		// Front labels (always drawn explicitly!)
		graphicsHolder.drawText(TextHelper.translatable("gui.mtr_polish_pack.city_name_front"), centerX - 100, centerY - 76, 0xFFA0A0A0, false, GraphicsHolder.getDefaultLight());
		graphicsHolder.drawText(TextHelper.translatable("gui.mtr_polish_pack.end_of_town_front"), centerX - 75, centerY - 34, 0xFFFFFFFF, false, GraphicsHolder.getDefaultLight());

		// Two sided label
		graphicsHolder.drawText(TextHelper.translatable("gui.mtr_polish_pack.two_sided"), centerX - 75, centerY - 9, 0xFFFFFFFF, false, GraphicsHolder.getDefaultLight());

		// Back labels (if two sided)
		if (checkboxTwoSided.isChecked2()) {
			graphicsHolder.drawText(TextHelper.translatable("gui.mtr_polish_pack.city_name_back"), centerX - 100, centerY + 0, 0xFFA0A0A0, false, GraphicsHolder.getDefaultLight());
			graphicsHolder.drawText(TextHelper.translatable("gui.mtr_polish_pack.end_of_town_back"), centerX - 75, centerY + 41, 0xFFFFFFFF, false, GraphicsHolder.getDefaultLight());
		}

		super.render(graphicsHolder, mouseX, mouseY, tickDelta);
	}
}
