package com.szablix.mtrpolishpids;

import com.szablix.mtrpolishpids.block.BlockPKPPIDS;
import com.szablix.mtrpolishpids.block.entity.BlockEntityPKPPIDS;
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
	public static final Logger LOGGER = LogManager.getLogger("MTR Polish PIDS");
	public static final Registry REGISTRY = new Registry();

	public static final BlockRegistryObject BLOCK_PKP_PIDS = REGISTRY.registerBlockWithBlockItem(
			new Identifier(MOD_ID, "pkp_pids_horizontal"),
			() -> new Block(new BlockPKPPIDS())
	);

	public static final CreativeModeTabHolder CREATIVE_TAB = REGISTRY.createCreativeModeTabHolder(
			new Identifier(MOD_ID, "creative_tab"),
			() -> new ItemStack(new org.mtr.mapping.holder.ItemConvertible(BLOCK_PKP_PIDS.get().data))
	);

	public static final BlockEntityTypeRegistryObject<BlockEntityPKPPIDS> BLOCK_ENTITY_TYPE_PKP_PIDS = REGISTRY.registerBlockEntityType(
			new Identifier(MOD_ID, "pkp_pids_horizontal"),
			BlockEntityPKPPIDS::new,
			BLOCK_PKP_PIDS::get
	);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing MTR Polish PIDS mod...");
		REGISTRY.init();
	}
}
