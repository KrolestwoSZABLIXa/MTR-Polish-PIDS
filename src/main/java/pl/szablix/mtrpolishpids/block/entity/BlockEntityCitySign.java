package pl.szablix.mtrpolishpids.block.entity;

import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.CompoundTag;
import org.mtr.mapping.mapper.BlockEntityExtension;
import pl.szablix.mtrpolishpids.MTRPolishPIDS;

public class BlockEntityCitySign extends BlockEntityExtension {

	private static final String KEY_CITY_NAME = "city_name";
	private static final String KEY_END_OF_TOWN = "end_of_town";

	private String cityName = "Warszawa";
	private boolean endOfTown = false;

	public BlockEntityCitySign(BlockPos pos, BlockState state) {
		super(MTRPolishPIDS.BLOCK_ENTITY_TYPE_CITY_SIGN.get(), pos, state);
	}

	@Override
	public void readCompoundTag(CompoundTag compoundTag) {
		super.readCompoundTag(compoundTag);
		if (compoundTag.contains(KEY_CITY_NAME)) {
			cityName = compoundTag.getString(KEY_CITY_NAME);
		} else {
			cityName = "Warszawa";
		}

		if (compoundTag.contains(KEY_END_OF_TOWN)) {
			endOfTown = compoundTag.getBoolean(KEY_END_OF_TOWN);
		} else {
			endOfTown = false;
		}
	}

	@Override
	public void writeCompoundTag(CompoundTag compoundTag) {
		super.writeCompoundTag(compoundTag);
		compoundTag.putString(KEY_CITY_NAME, cityName);
		compoundTag.putBoolean(KEY_END_OF_TOWN, endOfTown);
	}

	public String getCityName() {
		return cityName;
	}

	public void setCityName(String cityName) {
		this.cityName = cityName;
		markDirty2();
	}

	public boolean isEndOfTown() {
		return endOfTown;
	}

	public void setEndOfTown(boolean endOfTown) {
		this.endOfTown = endOfTown;
		markDirty2();
	}

	public void toggleEndOfTown() {
		this.endOfTown = !this.endOfTown;
		markDirty2();
	}
}
