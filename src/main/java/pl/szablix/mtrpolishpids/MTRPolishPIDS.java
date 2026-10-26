package pl.szablix.mtrpolishpids;

import pl.szablix.mtrpolishpids.block.BlockCitySign;
import pl.szablix.mtrpolishpids.block.BlockPKPPIDS;
import pl.szablix.mtrpolishpids.block.entity.BlockEntityCitySign;
import pl.szablix.mtrpolishpids.block.entity.BlockEntityPKPPIDS;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mtr.mapping.holder.Block;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.ItemStack;
import org.mtr.mapping.registry.BlockEntityTypeRegistryObject;
import org.mtr.mapping.registry.BlockRegistryObject;
import org.mtr.mapping.registry.CreativeModeTabHolder;
import org.mtr.mapping.registry.Registry;

public class MTRPolishPIDS implements ModInitializer {

	public static final String MOD_ID = "mtr_polish_pids";
	public static final Logger LOGGER = LogManager.getLogger("MTR Polish Pack");
	public static final Registry REGISTRY = new Registry();

	private static ItemStack getTabIcon() {
		return new ItemStack(new org.mtr.mapping.holder.ItemConvertible(BLOCK_PKP_PIDS.get().data));
	}

	public static final CreativeModeTabHolder CREATIVE_TAB = REGISTRY.createCreativeModeTabHolder(
			new Identifier(MOD_ID, "creative_tab"),
			MTRPolishPIDS::getTabIcon
	);

	public static final BlockRegistryObject BLOCK_PKP_PIDS = REGISTRY.registerBlockWithBlockItem(
			new Identifier(MOD_ID, "pkp_pids_horizontal"),
			() -> new Block(new BlockPKPPIDS()),
			CREATIVE_TAB
	);

	public static final BlockEntityTypeRegistryObject<BlockEntityPKPPIDS> BLOCK_ENTITY_TYPE_PKP_PIDS = REGISTRY.registerBlockEntityType(
			new Identifier(MOD_ID, "pkp_pids_horizontal"),
			BlockEntityPKPPIDS::new,
			BLOCK_PKP_PIDS::get
	);

	public static final BlockRegistryObject BLOCK_CITY_SIGN = REGISTRY.registerBlockWithBlockItem(
			new Identifier(MOD_ID, "city_sign"),
			() -> new Block(new BlockCitySign()),
			CREATIVE_TAB
	);

	public static final BlockEntityTypeRegistryObject<BlockEntityCitySign> BLOCK_ENTITY_TYPE_CITY_SIGN = REGISTRY.registerBlockEntityType(
			new Identifier(MOD_ID, "city_sign"),
			BlockEntityCitySign::new,
			BLOCK_CITY_SIGN::get
	);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing MTR Polish Pack mod...");
		REGISTRY.setupPackets(new Identifier(MOD_ID, "packet"));
		REGISTRY.registerPacket(pl.szablix.mtrpolishpids.packet.PacketOpenPKPPIDSConfigScreen.class, pl.szablix.mtrpolishpids.packet.PacketOpenPKPPIDSConfigScreen::new);
		REGISTRY.registerPacket(pl.szablix.mtrpolishpids.packet.PacketUpdatePKPPIDSClock.class, pl.szablix.mtrpolishpids.packet.PacketUpdatePKPPIDSClock::new);
		REGISTRY.registerPacket(pl.szablix.mtrpolishpids.packet.PacketOpenCitySignConfigScreen.class, pl.szablix.mtrpolishpids.packet.PacketOpenCitySignConfigScreen::new);
		REGISTRY.registerPacket(pl.szablix.mtrpolishpids.packet.PacketUpdateCitySign.class, pl.szablix.mtrpolishpids.packet.PacketUpdateCitySign::new);
		REGISTRY.init();
	}
}
