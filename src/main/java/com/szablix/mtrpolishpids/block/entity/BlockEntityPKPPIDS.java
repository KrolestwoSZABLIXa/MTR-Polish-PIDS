package com.szablix.mtrpolishpids.block.entity;

import com.szablix.mtrpolishpids.MTRPolishPIDS;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.CompoundTag;
import org.mtr.mod.block.BlockPIDSHorizontalBase;

public class BlockEntityPKPPIDS extends BlockPIDSHorizontalBase.BlockEntityHorizontalBase {

	private static final int MAX_ARRIVALS = 1;
	private static final String KEY_CLOCK_ON_RIGHT = "clock_on_right";
	private boolean clockOnRight = true;

	public BlockEntityPKPPIDS(BlockPos pos, BlockState state) {
		super(MAX_ARRIVALS, MTRPolishPIDS.BLOCK_ENTITY_TYPE_PKP_PIDS.get(), pos, state);
	}

	@Override
	public void readCompoundTag(CompoundTag compoundTag) {
		super.readCompoundTag(compoundTag);
		if (compoundTag.contains(KEY_CLOCK_ON_RIGHT)) {
			clockOnRight = compoundTag.getBoolean(KEY_CLOCK_ON_RIGHT);
		} else {
			clockOnRight = true;
		}
	}

	@Override
	public void writeCompoundTag(CompoundTag compoundTag) {
		super.writeCompoundTag(compoundTag);
		compoundTag.putBoolean(KEY_CLOCK_ON_RIGHT, clockOnRight);
	}

	public boolean isClockOnRight() {
		return clockOnRight;
	}

	public void setClockOnRight(boolean clockOnRight) {
		this.clockOnRight = clockOnRight;
		markDirty2();
	}

	public void toggleClockOrientation() {
		this.clockOnRight = !this.clockOnRight;
		markDirty2();
	}

	@Override
	public boolean showArrivalNumber() {
		return false;
	}

	@Override
	public int textColor() {
		return 0xFFFFFF;
	}

	@Override
	public int textColorArrived() {
		return 0xFFD700;
	}
}
