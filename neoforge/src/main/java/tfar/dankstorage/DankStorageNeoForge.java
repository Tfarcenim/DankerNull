package tfar.dankstorage;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tfar.dankstorage.client.ModClientForge;
import tfar.dankstorage.command.DankCommands;
import tfar.dankstorage.init.ModBlockEntityTypes;
import tfar.dankstorage.init.ModItems;
import tfar.dankstorage.item.DankItem;
import tfar.dankstorage.item.DankItemCapability;
import tfar.dankstorage.datagen.ModDatagen;
import tfar.dankstorage.network.DankPacketHandlerNeoForge;
import tfar.dankstorage.platform.TomlConfigs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Mod(DankStorage.MODID)
public class DankStorageNeoForge {

    public static final Logger LOGGER = LogManager.getLogger(DankStorage.MODID);
    public static Map<Registry<?>, List<Pair<ResourceLocation, Supplier<?>>>> registerLater = new HashMap<>();

    public DankStorageNeoForge(IEventBus bus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
        modContainer.registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
        NeoForge.EVENT_BUS.addListener(this::entityInvulnerabilityCheck);
        bus.addListener(ModDatagen::setupDataGenerator);
        bus.addListener(this::registerObjs);
        bus.addListener(this::onInitialize);
        bus.addListener(DankPacketHandlerNeoForge::register);
        bus.addListener(this::capabilities);
        if (FMLEnvironment.dist.isClient()) {
            bus.addListener(this::onInitializeClient);
            bus.addListener(ModClientForge::keybinds);
            bus.addListener(ModClientForge::clientTool);
            bus.addListener(ModClientForge::renderStack);
        }
        DankStorage.init();
    }


    //1543200 nanos
    public void registerObjs(RegisterEvent event) {
        Registry<?> registry = event.getRegistry();
        List<Pair<ResourceLocation,Supplier<?>>> list = registerLater.get(registry);
        if (list != null) {
            for (Pair<ResourceLocation,Supplier<?>> pair : list) {
                event.register((ResourceKey<? extends Registry<Object>>)registry.key(),pair.getLeft(),(Supplier<Object>)pair.getValue());
            }
        }
    }

    private void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntityTypes.dock,(object, context) -> (IItemHandler) object.getInventory());
        event.registerItem(Capabilities.ItemHandler.ITEM,(stack,context) -> DankItemCapability.lookup(stack), ModItems.DANKS.values().toArray(DankItem[]::new));
    }

    public void onInitialize(FMLCommonSetupEvent e) {
        registerLater.clear();
    }

    public void onInitializeClient(FMLClientSetupEvent e) {
        ModClientForge.client();
    }

    public void onServerStarted(ServerStartedEvent e) {
        DankStorage.onServerStart(e.getServer());
    }

    public void onServerStopped(ServerStoppedEvent e) {
        DankStorage.onServerShutDown(e.getServer());
    }

    public void registerCommands(RegisterCommandsEvent e) {
        DankCommands.register(e.getDispatcher());
    }

    private void entityInvulnerabilityCheck(EntityInvulnerabilityCheckEvent event) {
        if (event.getEntity() instanceof ItemEntity itemEntity) {
            if (itemEntity.getItem().getItem() instanceof DankItem && !event.getSource().is(Tags.DamageTypes.IS_TECHNICAL)) {
                event.setInvulnerable(true);
            }
        }
    }


  public static final TomlConfigs.ClientConfig CLIENT;
  public static final ModConfigSpec CLIENT_SPEC;

  public static final TomlConfigs.ServerConfig SERVER;
  public static final ModConfigSpec SERVER_SPEC;

  static {
    final Pair<TomlConfigs.ClientConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(TomlConfigs.ClientConfig::new);
    CLIENT_SPEC = specPair.getRight();
    CLIENT = specPair.getLeft();
    final Pair<TomlConfigs.ServerConfig, ModConfigSpec> specPair2 = new ModConfigSpec.Builder().configure(TomlConfigs.ServerConfig::new);
    SERVER_SPEC = specPair2.getRight();
    SERVER = specPair2.getLeft();
  }

}
