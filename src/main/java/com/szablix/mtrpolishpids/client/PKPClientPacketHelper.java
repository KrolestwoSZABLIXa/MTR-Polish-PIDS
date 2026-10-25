package com.szablix.mtrpolishpids.client;

import com.szablix.mtrpolishpids.client.screen.PKPPIDSConfigScreen;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.Screen;

public final class PKPClientPacketHelper {

	private PKPClientPacketHelper() {
	}

	public static void openPKPPIDSConfigScreen(BlockPos blockPos, int maxArrivals, boolean clockOnRight) {
		MinecraftClient.getInstance().openScreen(new Screen(new PKPPIDSConfigScreen(blockPos, maxArrivals, clockOnRight)));
	}
}
