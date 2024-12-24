package tfar.dankstorage;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tfar.dankstorage.init.*;
import tfar.dankstorage.mixin.MinecraftServerAccess;
import tfar.dankstorage.platform.Services;
import tfar.dankstorage.utils.CommonUtils;
import tfar.dankstorage.world.DankSavedData;
import tfar.dankstorage.world.MaxId;

import java.io.File;

// This class is part of the common project meaning it is shared between all supported loaders. Code written here can only
// import and access the vanilla codebase, libraries used by vanilla, and optionally third party libraries that provide
// common compatible binaries. This means common code can not directly use loader specific concepts such as Forge events
// however it will be compatible with all supported mod loaders.
public class DankStorage {

    public static final String MODID = "dankstorage";
    public static final String MOD_NAME = "Dank-Storage";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);
   // public static MaxId maxId;

    public static int MAX = 1000000000;

    public static int firstFreeId(MinecraftServer server) {
        int id = 0;
        while (id < MAX) {
            DankSavedData tankSavedData = DankSavedData.get(id, server);
            if (tankSavedData == null) {
                return id;
            }
            id++;
        }
        return CommonUtils.INVALID;
    }

    // The loader specific projects are able to import and use any code from the common project. This allows you to
    // write the majority of your code here and load it from your loader specific projects. This example has some
    // code that gets invoked by the entry point of the loader specific projects.
    public static void init() {
        Services.PLATFORM.registerAll(ModBlocks.class,BuiltInRegistries.BLOCK, Block.class);

        Class<BlockEntityType<?>> typeClass =(Class<BlockEntityType<?>>)(Object) BlockEntityType.class;
        Class<MenuType<?>> typeClass1 =(Class<MenuType<?>>)(Object) MenuType.class;
        Class<RecipeSerializer<?>> typeClass2 =(Class<RecipeSerializer<?>>)(Object) RecipeSerializer.class;
        Class<DataComponentType<?>> typeClass3 =(Class<DataComponentType<?>>)(Object) DataComponentType.class;

        Services.PLATFORM.registerAll(ModBlockEntityTypes.class,BuiltInRegistries.BLOCK_ENTITY_TYPE, typeClass);
        Services.PLATFORM.unfreeze(BuiltInRegistries.ITEM);
        Services.PLATFORM.registerAll(ModItems.getAll(), BuiltInRegistries.ITEM, Item.class);
        Services.PLATFORM.registerAll(ModCreativeTabs.class,BuiltInRegistries.CREATIVE_MODE_TAB, CreativeModeTab.class);
        Services.PLATFORM.registerAll(ModMenuTypes.class,BuiltInRegistries.MENU, typeClass1);
        Services.PLATFORM.registerAll(ModRecipeSerializers.class,BuiltInRegistries.RECIPE_SERIALIZER,typeClass2);
        Services.PLATFORM.registerAll(ModDataComponentTypes.class,BuiltInRegistries.DATA_COMPONENT_TYPE, typeClass3);
    //    Constants.LOG.info("Hello from Common init on {}! we are currently in a {} environment!", Services.PLATFORM.getPlatformName(), Services.PLATFORM.getEnvironmentName());
    //    Constants.LOG.info("The ID for diamonds is {}", BuiltInRegistries.ITEM.getKey(Items.DIAMOND));

        // It is common for all supported loaders to provide a similar feature that can not be used directly in the
        // common code. A popular way to get around this is using Java's built-in service loader feature to create
        // your own abstraction layer. You can learn more about this in our provided services class. In this example
        // we have an interface in the common code and use a loader specific implementation to delegate our call to
        // the platform specific approach.
      //  if (Services.PLATFORM.isModLoaded("examplemod")) {

       //     LOG.info("Hello to examplemod");
      //  }
    }

    public static MaxId getMaxId(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(MaxId.factory(server.overworld()), DankStorage.MODID+"_max_id");
    }

    public static void onServerShutDown(MinecraftServer server) {
        //maxId = null;
        CommonUtils.uncacheRecipes();
    }

    public static void onServerStart(MinecraftServer server) {
        LevelStorageSource.LevelStorageAccess storageSource = ((MinecraftServerAccess)server).getStorageSource();
        File file = storageSource.getDimensionPath(server.overworld().dimension())
                .resolve("data/"+ DankStorage.MODID).toFile();
        file.mkdirs();

        //DankStorage.maxId = DankStorage.getMaxId(server);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID,path);
    }
}