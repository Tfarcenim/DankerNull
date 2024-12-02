package tfar.dankstorage.inventory;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import tfar.dankstorage.DankStorage;
import tfar.dankstorage.ModTags;
import tfar.dankstorage.platform.Services;
import tfar.dankstorage.utils.CommonUtils;
import tfar.dankstorage.utils.DankStats;
import tfar.dankstorage.utils.SerializationHelper;
import tfar.dankstorage.world.DankSavedData;

import java.util.*;
import java.util.stream.IntStream;

public class DankInventory implements ContainerData {

    private final DankSavedData data;
    protected static String GHOST = "GhostItems";
    public NonNullList<ItemStack> items;
    protected NonNullList<ItemStack> ghostItems;
    public int capacity;
    private boolean frequencyLocked = true;
    protected int textColor = -1;


    public static final int TXT_COLOR = 0;
    public static final int FREQ_LOCK = 1;
    public static final int SORTING_TYPE = 2;
    public static final int AUTO_SORT = 3;
    public static final int DATA_SLOTS = 4;
    private SortingType sortingType = SortingType.descending;
    private boolean autoSort = true;
    protected boolean needsSort;

    public DankInventory(DankStats stats, DankSavedData data) {
        this(stats.slots, stats.stacklimit, data);
    }

    public DankInventory(int slots, int capacity, @Nullable DankSavedData data) {
        items = NonNullList.withSize(slots, ItemStack.EMPTY);
        ghostItems = NonNullList.withSize(slots, ItemStack.EMPTY);
        this.capacity = capacity;
        this.data = data;
    }

    public static DankInventory createDummy(DankStats dankStats) {
        return Services.PLATFORM.createInventory(dankStats, null);
    }


    public void setSortingType(SortingType sortingType) {
        this.sortingType = sortingType;
        setDirty(true);
    }

    public SortingType getSortingType() {
        return sortingType;
    }

    public ItemStack getGhostItem(int slot) {
        return getGhostItems().get(slot);
    }

    public boolean frequencyLocked() {
        return frequencyLocked;
    }

    public int textColor() {
        return get(TXT_COLOR);
    }

    public void setTextColor(int color) {
        set(TXT_COLOR, color);
    }

    public void toggleFrequencyLock() {
        frequencyLocked = !frequencyLocked;
        setDirty(false);
    }

    public void setItemDank(int slot, ItemStack stack) {
        if (inBounds(slot)) {
            this.items.set(slot, stack);
            setDirty(true);
        } else {
            warnOutOfBounds(slot);
        }
    }

    boolean inBounds(int index) {
        return index >= 0 && index < slotCount();
    }

    void warnOutOfBounds(int slot) {
        DankStorage.LOG.warn("Index out of bounds accessed, {} in size {}",slot,slotCount());
    }

    public ItemStack getItemDank(int slot) {
        if (inBounds(slot)) {
            return items.get(slot);
        } else {
            warnOutOfBounds(slot);
            return ItemStack.EMPTY;
        }
    }

    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (!inBounds(slot)) return false;
        boolean checkGhostItem = !hasGhostItem(slot) || getGhostItem(slot).getItem() == stack.getItem();
        return !stack.is(ModTags.BLACKLISTED_STORAGE)
                && checkGhostItem;
    }

    public void setGhostItem(int slot, ItemStack stack) {
        if (inBounds(slot)) {
            getGhostItems().set(slot, stack);
        } else {
            warnOutOfBounds(slot);
        }
    }

    public NonNullList<ItemStack> getContents() {
        return items;
    }

    public void setItemsDank(NonNullList<ItemStack> stacks) {
        this.items = stacks;
    }

    protected NonNullList<ItemStack> getGhostItems() {
        return ghostItems;
    }

    public void setGhostItems(NonNullList<ItemStack> ghostItems) {
        this.ghostItems = ghostItems;
    }

    @Override
    public int get(int slot) {
        return switch (slot) {
            case TXT_COLOR -> textColor;
            case FREQ_LOCK -> frequencyLocked ? 1 : 0;
            case SORTING_TYPE -> sortingType.ordinal();
            case AUTO_SORT -> autoSort ? 1 : 0;
            default -> AbstractContainerMenu.SLOT_CLICKED_OUTSIDE;
        };
    }

    @Override
    public void set(int slot, int value) {
        switch (slot) {
            case TXT_COLOR -> textColor = value;
            case FREQ_LOCK -> frequencyLocked = value != 0;
            case SORTING_TYPE -> sortingType = SortingType.values()[value];
            case AUTO_SORT -> autoSort = value != 0;
        }
        setDirty(false);
    }

    //0 is the id, 1 is text color, and 2 is frequency lock
    @Override
    public int getCount() {
        return DATA_SLOTS;
    }

    ItemStack addItemDank(int slot, ItemStack stack) {
        return insertStack(slot, stack, false);
    }


    public ItemStack insertStack(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        } else if (!this.canPlaceItem(slot, stack)) {
            return stack;
        } else {
            // this.validateSlotIndex(slot);
            ItemStack existing = this.items.get(slot);
            int limit = this.getMaxStackSizeSensitive(stack);
            if (!existing.isEmpty()) {
                if (!ItemStack.isSameItemSameComponents(stack, existing)) {
                    return stack;
                }

                limit -= existing.getCount();
            }

            if (limit <= 0) {
                return stack;
            } else {
                boolean reachedLimit = stack.getCount() > limit;
                if (!simulate) {
                    if (existing.isEmpty()) {
                        this.items.set(slot, reachedLimit ? stack.copyWithCount(limit) : stack);
                    } else {
                        existing.grow(reachedLimit ? limit : stack.getCount());
                    }
                    setDirty(true);
                    //this.onContentsChanged(slot);
                }

                return reachedLimit ? stack.copyWithCount(stack.getCount() - limit) : ItemStack.EMPTY;
            }
        }
    }

    public ItemStack extractStack(int slot, int amount, boolean simulate) {
        if (amount == 0) {
            return ItemStack.EMPTY;
        } else {
            // this.validateSlotIndex(slot);
            ItemStack existing = this.items.get(slot);
            if (existing.isEmpty()) {
                return ItemStack.EMPTY;
            } else {
                int toExtract = Math.min(amount, existing.getMaxStackSize());
                if (existing.getCount() <= toExtract) {
                    if (!simulate) {
                        this.items.set(slot, ItemStack.EMPTY);
                        setDirty(true);
                        //this.onContentsChanged(slot);
                        return existing;
                    } else {
                        return existing.copy();
                    }
                } else {
                    if (!simulate) {
                        this.items.set(slot, existing.copyWithCount(existing.getCount() - toExtract));
                        setDirty(true);
                        //this.onContentsChanged(slot);
                    }

                    return existing.copyWithCount(toExtract);
                }
            }
        }
    }

    public ItemStack extractStackTarget(int amount, boolean simulate, ItemStack target) {
        if (amount == 0 || target.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            int remaining = amount;
            int extracted = 0;
            for (int i = 0; i < slotCount(); i++) {
                ItemStack slotStack = getItemDank(i);
                if (ItemStack.isSameItemSameComponents(target, slotStack)) {
                    ItemStack stack = extractStack(i, remaining, simulate);
                    remaining -= stack.getCount();
                    extracted += stack.getCount();
                    if (remaining <= 0) {
                        break;
                    }
                }
            }
            return target.copyWithCount(extracted);
        }
    }

    ItemStack insertStackGeneral(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack remainder = stack.copy();
        for (int i = 0; i < slotCount(); i++) {
            remainder = insertStack(i, stack, simulate);
            if (remainder.isEmpty()) {
                break;
            }
        }
        return remainder;
    }

    public int getMaxStackSizeSensitive(ItemStack stack) {
        return stack.is(ModTags.UNSTACKABLE) ? 1 : getMaxStackSizeDank();
    }

    public void sort() {
        List<ItemStack> gathered = new ArrayList<>();
        Set<ItemStack> lockedItems = new HashSet<>();

        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            ItemStack ghost = ghostItems.get(i);
            if (!stack.isEmpty()) {
                CommonUtils.merge(gathered, stack.copy());
                boolean unique = true;
                for (ItemStack stack1 : lockedItems) {
                    if (ItemStack.isSameItemSameComponents(stack1, stack)) {
                        unique = false;
                        break;
                    }
                }
                if (unique && !ghost.isEmpty()) {
                    lockedItems.add(stack.copyWithCount(1));
                }
            }
        }

        gathered.sort(sortingType.comparator);
        items.clear();
        ghostItems.clear();
        //split up the gathered and add them to the slot
        int slotId = 0;

        for (int i = 0; i < gathered.size(); i++) {
            ItemStack stack = gathered.get(i);
            int count = stack.getCount();

            int tankSize = capacity;

            if (count > tankSize) {
                int fullStacks = count / tankSize;
                int partialStack = count - fullStacks * tankSize;

                for (int j = 0; j < fullStacks; j++) {
                    items.set(slotId, stack.copyWithCount(tankSize));

                    if (anyMatch(stack, lockedItems)) {
                        ghostItems.set(slotId, stack.copyWithCount(1));
                    }

                    slotId++;
                }
                if (partialStack > 0) {
                    items.set(slotId, stack.copyWithCount(partialStack));

                    if (anyMatch(stack, lockedItems)) {
                        ghostItems.set(slotId, stack.copyWithCount(1));
                    }

                    slotId++;
                }
            } else {
                items.set(slotId, stack);
                if (anyMatch(stack, lockedItems)) {
                    ghostItems.set(slotId, stack.copyWithCount(1));
                }

                slotId++;
            }
        }
        setDirty(false);
    }

    static boolean anyMatch(ItemStack stack, Set<ItemStack> stacks) {
        for (ItemStack stack1 : stacks) {
            if (ItemStack.isSameItemSameComponents(stack1, stack)) {
                return true;
            }
        }
        return false;
    }

    public void compress(ServerLevel level, @Nullable ServerPlayer player) {
        List<ItemStack> allItems = getUniqueItems();
        List<Pair<ItemStack, ItemStack>> craftingResults = new ArrayList<>();

        for (int i = 0; i < allItems.size(); i++) {
            ItemStack stack = allItems.get(i);
            Pair<ItemStack, ItemStack> result = CommonUtils.getCompressingResult(stack, level);
            craftingResults.add(result);
        }

        for (int i = 0; i < slotCount(); i++) {
            items.set(i, ItemStack.EMPTY);
            ghostItems.set(i, ItemStack.EMPTY);
        }

        List<ItemStack> leftovers = new ArrayList<>();

        for (Pair<ItemStack, ItemStack> pair : craftingResults) {
            ItemStack stack1 = insertStackGeneral(pair.getFirst(), false);
            ItemStack stack2 = insertStackGeneral(pair.getSecond(), false);
            if (!stack1.isEmpty()) {
                leftovers.add(stack1);
            }
            if (!stack2.isEmpty()) {
                leftovers.add(stack2);
            }
        }

        for (ItemStack remainder : leftovers) {
            if (!remainder.isEmpty()) {
                if (player != null) {
                    player.addItem(remainder);
                    if (!remainder.isEmpty()) {
                        player.drop(remainder, false);
                    }
                }
            }
        }
        sort();
    }


    void readItems(HolderLookup.Provider provider, ListTag listTag,List<ItemStack> list) {
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag itemTags = listTag.getCompound(i);
            int slot = itemTags.getInt("Slot");
            if (inBounds(slot)) {
                if (itemTags.contains("StackList", Tag.TAG_LIST)) {
                    ItemStack stack = ItemStack.EMPTY;
                    ListTag stackTagList = itemTags.getList("StackList", Tag.TAG_COMPOUND);
                    for (int j = 0; j < stackTagList.size(); j++) {
                        CompoundTag itemTag = stackTagList.getCompound(j);
                        ItemStack temp = SerializationHelper.decodeLargeItemStack(provider, itemTag);
                        if (!temp.isEmpty()) {
                            if (stack.isEmpty()) stack = temp;
                            else stack.grow(temp.getCount());
                        }
                    }
                    if (!stack.isEmpty()) {
                        int count = stack.getCount();
                        count = Math.min(count, getMaxStackSizeDank());
                        stack.setCount(count);

                        list.set(slot, stack);
                    }
                } else {
                    ItemStack stack = SerializationHelper.decodeLargeItemStack(provider, itemTags);
                    list.set(slot, stack);
                }
            }
        }
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        ListTag nbtTagList = new ListTag();
        for (int i = 0; i < this.getContents().size(); i++) {
            ItemStack stack = getContents().get(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                //getContents().get(i).save(provider,itemTag);
                itemTag.putInt("Slot", i);
                nbtTagList.add(SerializationHelper.encodeLargeStack(stack, provider, itemTag));
            }
        }


        ListTag ghostItemNBT = new ListTag();
        for (int i = 0; i < this.getContents().size(); i++) {
            ItemStack ghost = getGhostItem(i);
            if (!ghost.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);

                ghostItemNBT.add(ghost.save(provider, itemTag));
            }
        }


        CompoundTag nbt = new CompoundTag();
        nbt.put("Items", nbtTagList);
        nbt.put(GHOST, ghostItemNBT);
        nbt.putBoolean("locked", frequencyLocked());
        nbt.putString("SortingType", sortingType.name());
        nbt.putBoolean("AutoSort", autoSort);
        return nbt;
    }

    public void load(HolderLookup.Provider provider, CompoundTag nbt) {
        ListTag tagList = nbt.getList("Items", Tag.TAG_COMPOUND);
        readItems(provider, tagList,items);
        ListTag ghostItemList = nbt.getList(GHOST, Tag.TAG_COMPOUND);
        readItems(provider, ghostItemList,ghostItems);
        if (nbt.contains("locked")) {
            frequencyLocked = nbt.getBoolean("locked");
        }

        try {
            if (nbt.contains("SortingType")) sortingType = SortingType.valueOf(nbt.getString("SortingType"));
        } catch (Exception e) {}
        if (nbt.contains("AutoSort")) {
            autoSort = nbt.getBoolean("AutoSort");
        }
    }

    public int calcRedstone() {
        int numStacks = 0;
        float f = 0F;

        for (int slot = 0; slot < this.slotCount(); slot++) {
            ItemStack stack = this.getItemDank(slot);

            if (!stack.isEmpty()) {
                f += (float) stack.getCount() / (float) this.getMaxStackSizeDank();
                numStacks++;
            }
        }

        f /= this.slotCount();
        return Mth.floor(f * 14F) + (numStacks > 0 ? 1 : 0);
    }

    public boolean noValidSlots() {
        return IntStream.range(0, slotCount())
                .mapToObj(this::getItemDank)
                .allMatch(stack -> stack.isEmpty() || stack.is(ModTags.BLACKLISTED_USAGE));
    }

    public void upgradeTo(DankStats stats) {
        if (stats.slots > slotCount()) {
            setTo(stats);
        }
    }

    //like upgradeTo, but can go backwards, should only be used by commands
    public void setTo(DankStats stats) {
        NonNullList<ItemStack> newStacks = NonNullList.withSize(stats.slots, ItemStack.EMPTY);
        NonNullList<ItemStack> newGhostStacks = NonNullList.withSize(stats.slots, ItemStack.EMPTY);

        //don't copy nonexistent items
        int oldSlots = slotCount();
        int max = Math.min(oldSlots, stats.slots);
        for (int i = 0; i < max; i++) {
            ItemStack oldStack = getItemDank(i);
            ItemStack oldGhost = getGhostItem(i);
            newStacks.set(i, oldStack);
            newGhostStacks.set(i, oldGhost);
        }

        //caution, will void all current items
        setItemsDank(newStacks);
        setGhostItems(newGhostStacks);
        data.setStats(stats);
        capacity = stats.stacklimit;
    }


    public boolean hasGhostItem(int slot) {
        return !getGhostItem(slot).isEmpty();
    }

    public int getMaxStackSizeDank() {
        return capacity;
    }

    public void toggleGhostItem(int slot) {
        boolean loc = hasGhostItem(slot);
        if (loc) {
            setGhostItem(slot, ItemStack.EMPTY);
        } else {
            setGhostItem(slot, getItemDank(slot).copyWithCount(1));
        }
        setDirty(false);
    }

    public int slotCount() {
        return items.size();
    }

    public void setDirty(boolean needsSort) {
        if (data != null) {
            data.setDirty();
        }
        if (needsSort) {
            if (autoSort) {
                sort();
            }
        }
    }

    public List<ItemStack> getUniqueItems() {
        List<ItemStack> gathered = new ArrayList<>();
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                boolean matched = false;
                for (ItemStack itemStack : gathered) {
                    if (ItemStack.isSameItemSameComponents(itemStack, stack)) {
                        itemStack.grow(stack.getCount());
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    gathered.add(stack.copy());
                }
            }
        }
        return gathered;
    }

    public long countItem(ItemStack sel) {
        if (sel.isEmpty()) return 0;
        long amount = items.stream().filter(stack -> ItemStack.isSameItemSameComponents(sel, stack)).mapToLong(ItemStack::getCount).sum();
        return amount;
    }

    public boolean autoSort() {
        return autoSort;
    }

    public void toggleAutoSort() {
        autoSort = !autoSort;
        setDirty(false);
    }
}
