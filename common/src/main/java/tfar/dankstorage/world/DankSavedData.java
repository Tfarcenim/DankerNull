package tfar.dankstorage.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;
import tfar.dankstorage.DankStorage;
import tfar.dankstorage.inventory.DankInventory;
import tfar.dankstorage.platform.Services;
import tfar.dankstorage.utils.CommonUtils;
import tfar.dankstorage.utils.DankStats;

public class DankSavedData extends SavedData {

    protected final ServerLevel level;
    private final int frequency;
    DankInventory cache;
    DankStats stats = DankStats.zero;
    CompoundTag tag = new CompoundTag();

    public DankSavedData(ServerLevel level, int frequency) {
        this.level = level;
        this.frequency = frequency;
    }


    public static SavedData.Factory<DankSavedData> factory(ServerLevel pLevel,int frequency) {
        return new SavedData.Factory<>(() -> new DankSavedData(pLevel, frequency), (tag, p_324123_) -> loadStatic(tag, pLevel,frequency), DataFixTypes.SAVED_DATA_RAIDS);
    }

    @Override
    public CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider provider) {
        compoundTag.putString("Stats",stats.name());
        if (cache != null) {
            compoundTag.put("contents", cache.save(provider));
        }
        return compoundTag;
    }

    public static DankSavedData getOrCreate(int id, MinecraftServer server) {
        DankSavedData tankSavedData = get(id,server);
        if (tankSavedData != null) {
            return tankSavedData;
        }

        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage()
                .computeIfAbsent(DankSavedData.factory(overworld,id), DankStorage.MODID+"/"+id);
    }

    @Nullable
    public static DankSavedData get(int id, MinecraftServer server) {
        if (id <= CommonUtils.INVALID) throw new RuntimeException("Invalid frequency: "+id);
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage()
                .get(DankSavedData.factory(overworld,id), DankStorage.MODID+"/"+id);
    }

    public DankInventory getOrCreateInventory() {
        if (cache == null) {
            cache = Services.PLATFORM.createInventory(stats,this);
            cache.load(level.registryAccess(), tag);
        }
        if (cache.items.size() != stats.slots) {
            cache.setTo(stats);
        }
        return cache;
    }

    public void setStats(DankStats stats) {
        this.stats = stats;
        setDirty();
    }

    public DankStats getStats() {
        return stats;
    }
    protected void load(CompoundTag compoundTag) {
        stats = compoundTag.contains("Stats") ? DankStats.valueOf(compoundTag.getString("Stats")) : DankStats.zero;
        tag = compoundTag.getCompound("contents");
    }

    public static DankSavedData loadStatic(CompoundTag compoundTag, ServerLevel level,int frequency) {
        DankSavedData tankSavedData = new DankSavedData(level,frequency);
        tankSavedData.load(compoundTag);
        return tankSavedData;
    }

    public boolean clear() {
        return true;
    }
}
