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

		textFieldCityName = new TextFieldWidgetExtension(0, 0, 0, 20, 60, TextCase.DEFAULT, null, null);
		checkboxEndOfTown = new CheckboxWidgetExtension(0, 0, 0, 20, TextHelper.literal(""), endOfTown, checked -> this.endOfTown = checked);
		checkboxTwoSided = new CheckboxWidgetExtension(0, 0, 0, 20, TextHelper.literal(""), twoSided, checked -> {
			this.twoSided = checked;
			updatePositions();
		});
		textFieldCityNameBack = new TextFieldWidgetExtension(0, 0, 0, 20, 60, TextCase.DEFAULT, null, null);
		checkboxEndOfTownBack = new CheckboxWidgetExtension(0, 0, 0, 20, TextHelper.literal(""), endOfTownBack, checked -> this.endOfTownBack = checked);

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

		addChild(new ClickableWidget(textFieldCityName));
		addChild(new ClickableWidget(checkboxEndOfTown));
		addChild(new ClickableWidget(checkboxTwoSided));
		addChild(new ClickableWidget(textFieldCityNameBack));
		addChild(new ClickableWidget(checkboxEndOfTownBack));
		addChild(new ClickableWidget(buttonSave));
		addChild(new ClickableWidget(buttonCancel));

		checkboxEndOfTown.setChecked(endOfTown);
		checkboxTwoSided.setChecked(twoSided);
		checkboxEndOfTownBack.setChecked(endOfTownBack);

		updatePositions();

		textFieldCityName.setEditableColor2(0xFFFFFFFF);
		textFieldCityName.setText2(cityName);
		textFieldCityName.setSelectionStart2(0);
		textFieldCityName.setSelectionEnd2(0);

		textFieldCityNameBack.setEditableColor2(0xFFFFFFFF);
		textFieldCityNameBack.setText2(cityNameBack);
		textFieldCityNameBack.setSelectionStart2(0);
		textFieldCityNameBack.setSelectionEnd2(0);
	}

	private void updatePositions() {
		final boolean isTwoSided = checkboxTwoSided.isChecked2();
		final int centerX = width / 2;
		final int centerY = height / 2;

		if (isTwoSided) {
			IDrawing.setPositionAndWidth(textFieldCityName, centerX - 100, centerY - 76, 200);
			IDrawing.setPositionAndWidth(checkboxEndOfTown, centerX - 100, centerY - 50, 20);
			IDrawing.setPositionAndWidth(checkboxTwoSided, centerX - 100, centerY - 26, 20);
			IDrawing.setPositionAndWidth(textFieldCityNameBack, centerX - 100, centerY + 10, 200);
			IDrawing.setPositionAndWidth(checkboxEndOfTownBack, centerX - 100, centerY + 36, 20);
			IDrawing.setPositionAndWidth(buttonSave, centerX - 100, centerY + 68, 95);
			IDrawing.setPositionAndWidth(buttonCancel, centerX + 5, centerY + 68, 95);

			textFieldCityNameBack.setVisibleMapped(true);
			checkboxEndOfTownBack.setVisibleMapped(true);
		} else {
			IDrawing.setPositionAndWidth(textFieldCityName, centerX - 100, centerY - 45, 200);
			IDrawing.setPositionAndWidth(checkboxEndOfTown, centerX - 100, centerY - 18, 20);
			IDrawing.setPositionAndWidth(checkboxTwoSided, centerX - 100, centerY + 8, 20);
			IDrawing.setPositionAndWidth(buttonSave, centerX - 100, centerY + 42, 95);
			IDrawing.setPositionAndWidth(buttonCancel, centerX + 5, centerY + 42, 95);

			textFieldCityNameBack.setVisibleMapped(false);
			checkboxEndOfTownBack.setVisibleMapped(false);
		}
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
		final boolean isTwoSided = checkboxTwoSided.isChecked2();

		if (isTwoSided) {
			graphicsHolder.drawCenteredText(TextHelper.translatable("gui.mtr_polish_pack.city_sign_title"), centerX, centerY - 106, -1);
			graphicsHolder.drawText(TextHelper.translatable("gui.mtr_polish_pack.city_name_front"), centerX - 100, centerY - 88, 0xFFA0A0A0, false, GraphicsHolder.getDefaultLight());
			graphicsHolder.drawText(TextHelper.translatable("gui.mtr_polish_pack.end_of_town_front"), centerX - 75, centerY - 44, 0xFFFFFFFF, false, GraphicsHolder.getDefaultLight());
			graphicsHolder.drawText(TextHelper.translatable("gui.mtr_polish_pack.two_sided"), centerX - 75, centerY - 20, 0xFFFFFFFF, false, GraphicsHolder.getDefaultLight());
			graphicsHolder.drawText(TextHelper.translatable("gui.mtr_polish_pack.city_name_back"), centerX - 100, centerY - 2, 0xFFA0A0A0, false, GraphicsHolder.getDefaultLight());
			graphicsHolder.drawText(TextHelper.translatable("gui.mtr_polish_pack.end_of_town_back"), centerX - 75, centerY + 42, 0xFFFFFFFF, false, GraphicsHolder.getDefaultLight());
		} else {
			graphicsHolder.drawCenteredText(TextHelper.translatable("gui.mtr_polish_pack.city_sign_title"), centerX, centerY - 72, -1);
			graphicsHolder.drawText(TextHelper.translatable("gui.mtr_polish_pack.city_name_front"), centerX - 100, centerY - 57, 0xFFA0A0A0, false, GraphicsHolder.getDefaultLight());
			graphicsHolder.drawText(TextHelper.translatable("gui.mtr_polish_pack.end_of_town_front"), centerX - 75, centerY - 12, 0xFFFFFFFF, false, GraphicsHolder.getDefaultLight());
			graphicsHolder.drawText(TextHelper.translatable("gui.mtr_polish_pack.two_sided"), centerX - 75, centerY + 14, 0xFFFFFFFF, false, GraphicsHolder.getDefaultLight());
		}

		super.render(graphicsHolder, mouseX, mouseY, tickDelta);
	}
}
