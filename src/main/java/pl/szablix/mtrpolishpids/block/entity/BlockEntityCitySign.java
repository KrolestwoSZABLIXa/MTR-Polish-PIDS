package pl.szablix.mtrpolishpids.block.entity;

import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.CompoundTag;
import org.mtr.mapping.mapper.BlockEntityExtension;
import pl.szablix.mtrpolishpids.MTRPolishPIDS;

public class BlockEntityCitySign extends BlockEntityExtension {

	private static final String KEY_CITY_NAME = "city_name";
	private static final String KEY_END_OF_TOWN = "end_of_town";
	private static final String KEY_TWO_SIDED = "two_sided";
	private static final String KEY_CITY_NAME_BACK = "city_name_back";
	private static final String KEY_END_OF_TOWN_BACK = "end_of_town_back";

	private String cityName = "Warszawa";
	private boolean endOfTown = false;
	private boolean twoSided = false;
	private String cityNameBack = "Warszawa";
	private boolean endOfTownBack = true;

	public BlockEntityCitySign(BlockPos pos, BlockState state) {
		super(MTRPolishPIDS.BLOCK_ENTITY_TYPE_CITY_SIGN.get(), pos, state);
	}

	@Override
	public void readCompoundTag(CompoundTag compoundTag) {
		super.readCompoundTag(compoundTag);
		cityName = compoundTag.contains(KEY_CITY_NAME) ? compoundTag.getString(KEY_CITY_NAME) : "Warszawa";
		endOfTown = compoundTag.contains(KEY_END_OF_TOWN) && compoundTag.getBoolean(KEY_END_OF_TOWN);
		twoSided = compoundTag.contains(KEY_TWO_SIDED) && compoundTag.getBoolean(KEY_TWO_SIDED);
		cityNameBack = compoundTag.contains(KEY_CITY_NAME_BACK) ? compoundTag.getString(KEY_CITY_NAME_BACK) : cityName;
		endOfTownBack = !compoundTag.contains(KEY_END_OF_TOWN_BACK) || compoundTag.getBoolean(KEY_END_OF_TOWN_BACK);
	}

	@Override
	public void writeCompoundTag(CompoundTag compoundTag) {
		super.writeCompoundTag(compoundTag);
		compoundTag.putString(KEY_CITY_NAME, cityName);
		compoundTag.putBoolean(KEY_END_OF_TOWN, endOfTown);
		compoundTag.putBoolean(KEY_TWO_SIDED, twoSided);
		compoundTag.putString(KEY_CITY_NAME_BACK, cityNameBack);
		compoundTag.putBoolean(KEY_END_OF_TOWN_BACK, endOfTownBack);
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

	public boolean isTwoSided() {
		return twoSided;
	}

	public void setTwoSided(boolean twoSided) {
		this.twoSided = twoSided;
		markDirty2();
	}

	public String getCityNameBack() {
		return (cityNameBack == null || cityNameBack.trim().isEmpty()) ? cityName : cityNameBack;
	}

	public void setCityNameBack(String cityNameBack) {
		this.cityNameBack = cityNameBack;
		markDirty2();
	}

	public boolean isEndOfTownBack() {
		return endOfTownBack;
	}

	public void setEndOfTownBack(boolean endOfTownBack) {
		this.endOfTownBack = endOfTownBack;
		markDirty2();
	}

	public float getPostOffset() {
		final float halfWFront = estimateHalfWidth(cityName);
		final float halfWBack = twoSided ? estimateHalfWidth(cityNameBack) : halfWFront;
		final float maxHalfW = Math.max(halfWFront, halfWBack);
		return Math.max(0.38f, maxHalfW * 0.65f);
	}

	public float getHalfWidth() {
		final float halfWFront = estimateHalfWidth(cityName);
		final float halfWBack = twoSided ? estimateHalfWidth(cityNameBack) : halfWFront;
		return Math.max(halfWFront, halfWBack);
	}

	public static float estimateHalfWidth(String name) {
		if (name == null || name.trim().isEmpty()) {
			name = "Warszawa";
		}
		int textPx = 0;
		for (char c : name.trim().toCharArray()) {
			if ("ijlftI1r.':,;|! ".indexOf(c) >= 0) {
				textPx += 40;
			} else if ("mwMW@%#".indexOf(c) >= 0) {
				textPx += 95;
			} else if (Character.isUpperCase(c)) {
				textPx += 75;
			} else {
				textPx += 65;
			}
		}
		final int totalPx = Math.max(450, textPx + 170);
		return (totalPx / 256.0f * 0.80f) / 2.0f;
	}
}
