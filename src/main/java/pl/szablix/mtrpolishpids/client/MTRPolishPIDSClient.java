package pl.szablix.mtrpolishpids.client;

import pl.szablix.mtrpolishpids.MTRPolishPIDS;
import pl.szablix.mtrpolishpids.client.render.RenderCitySign;
import pl.szablix.mtrpolishpids.client.render.RenderPKPPIDS;
import net.fabricmc.api.ClientModInitializer;
import org.mtr.mapping.registry.RegistryClient;

public class MTRPolishPIDSClient implements ClientModInitializer {

	public static final RegistryClient REGISTRY_CLIENT = new RegistryClient(MTRPolishPIDS.REGISTRY);

	@Override
	public void onInitializeClient() {
		REGISTRY_CLIENT.setupPackets(new org.mtr.mapping.holder.Identifier(MTRPolishPIDS.MOD_ID, "packet"));
		REGISTRY_CLIENT.registerBlockEntityRenderer(MTRPolishPIDS.BLOCK_ENTITY_TYPE_PKP_PIDS, RenderPKPPIDS::new);
		REGISTRY_CLIENT.registerBlockEntityRenderer(MTRPolishPIDS.BLOCK_ENTITY_TYPE_CITY_SIGN, RenderCitySign::new);
		REGISTRY_CLIENT.init();
	}
}
