package pl.szablix.mtrpolishpids.client;

import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.Screen;
import pl.szablix.mtrpolishpids.client.screen.CitySignConfigScreen;
import pl.szablix.mtrpolishpids.client.screen.PKPPIDSConfigScreen;

public final class PKPClientPacketHelper {

	private PKPClientPacketHelper() {
	}

	public static void openPKPPIDSConfigScreen(BlockPos blockPos, int maxArrivals, boolean clockOnRight) {
		MinecraftClient.getInstance().openScreen(new Screen(new PKPPIDSConfigScreen(blockPos, maxArrivals, clockOnRight)));
	}

	public static void openCitySignConfigScreen(BlockPos blockPos, String cityName, boolean endOfTown) {
		MinecraftClient.getInstance().openScreen(new Screen(new CitySignConfigScreen(blockPos, cityName, endOfTown)));
	}
}
