package tfar.dankstorage.world;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import tfar.dankstorage.DankStorage;
import tfar.dankstorage.utils.DankStats;
import tfar.dankstorage.utils.ItemHandlerHelper;
import tfar.dankstorage.utils.ItemStackWrapper;
import tfar.dankstorage.utils.Utils;

import java.util.*;
import java.util.stream.IntStream;

public class DankInventory extends ItemStackHandler implements ContainerData {

    public DankStats dankStats;
    protected NonNullList<ItemStack> ghostItems;
    protected int id;
    private boolean locked = true;
    protected int textColor = -1;

    public DankInventory(DankStats stats, int id) {
        super(stats.slots);
        this.dankStats = stats;
        this.ghostItems = NonNullList.withSize(stats.slots,ItemStack.EMPTY);
        this.id = id;
    }

    @Override
    public void setSize(int size) {
        super.setSize(size);
        ghostItems = NonNullList.withSize(size,ItemStack.EMPTY);
    }

    public void upgradeTo(DankStats stats) {
        //can't downgrade inventories
        if (stats.ordinal() <= dankStats.ordinal()) {
            return;
        }
        setTo(stats);
    }

    //like upgradeTo, but can go backwards, should only be used by commands
    public void setTo(DankStats stats) {
        if (stats != dankStats) {
            DankStorage.LOGGER.debug("Upgrading dank #{} from tier {} to {}", id, dankStats.name(), stats.name());
        }
        this.dankStats = stats;
        copyItems();
    }

    private void copyItems() {

        NonNullList<ItemStack> newStacks = NonNullList.withSize(dankStats.slots, ItemStack.EMPTY);
        NonNullList<ItemStack> newGhostStacks = NonNullList.withSize(dankStats.slots, ItemStack.EMPTY);

        //don't copy nonexistent items
        int oldSlots = getSlots();
        int max = Math.min(oldSlots, dankStats.slots);
        for (int i = 0; i < max;i++) {
            ItemStack oldStack = getStackInSlot(i);
            ItemStack oldGhost = getGhostItem(i);
            newStacks.set(i,oldStack);
            newGhostStacks.set(i,oldGhost);
        }

        //caution, will void all current items
        setSize(dankStats.slots);

        for (int i = 0; i < max; i++) {
            stacks = newStacks;
            ghostItems = newGhostStacks;
        }
        onContentsChanged(0);
    }

    @Override
    public ItemStack extractItem(int slot, int amount,boolean simulate) {
            return super.extractItem(slot, amount,simulate);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (hasGhostItem(slot) && getGhostItem(slot).getItem() != stack.getItem()) {
            return stack;
        }
        return super.insertItem(slot, stack, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return dankStats.stacklimit;
    }

    @Override
    protected int getStackLimit(int slot, @NotNull ItemStack stack) {
        return getSlotLimit(slot);
    }

    public NonNullList<ItemStack> getContents() {
        return stacks;
    }

    public boolean noValidSlots() {
        return IntStream.range(0, getSlots())
                .mapToObj(this::getStackInSlot)
                .allMatch(stack -> stack.isEmpty() || stack.is(Utils.BLACKLISTED_USAGE));
    }

    public boolean hasGhostItem(int slot) {
        return !ghostItems.get(slot).isEmpty();
    }

    public ItemStack getGhostItem(int slot) {
        return ghostItems.get(slot);
    }

    public void setGhostItem(int slot,Item item) {
        ghostItems.set(slot,new ItemStack(item));
    }

    public void toggleGhostItem(int slot) {
        boolean loc = !ghostItems.get(slot).isEmpty();
        if (!loc) {
            ghostItems.set(slot,ItemHandlerHelper.copyStackWithSize(getStackInSlot(slot),1));
        } else {
            ghostItems.set(slot,ItemStack.EMPTY);
        }
        onContentsChanged(slot);
    }

    //paranoia
    @Override
    public boolean isItemValid(int slot,ItemStack stack) {
        boolean checkGhostItem = !hasGhostItem(slot) || getGhostItem(slot).getItem() == stack.getItem();
        return !stack.is(Utils.BLACKLISTED_STORAGE)
                && checkGhostItem
                && super.isItemValid(slot, stack);
    }

    //returns the portion of the itemstack that was NOT placed into the storage
    public CompoundTag save() {
        ListTag nbtTagList = new ListTag();
        for (int i = 0; i < this.getContents().size(); i++) {
            if (!getContents().get(i).isEmpty()) {
                int realCount = Math.min(dankStats.stacklimit, getContents().get(i).getCount());
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                getContents().get(i).save(itemTag);
                itemTag.putInt("ExtendedCount", realCount);
                nbtTagList.add(itemTag);
            }
        }


        ListTag ghostItemNBT = new ListTag();
        for (int i = 0; i < this.getContents().size(); i++) {
            if (!ghostItems.get(i).isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                ghostItems.get(i).save(itemTag);
                ghostItemNBT.add(itemTag);
            }
        }


        CompoundTag nbt = new CompoundTag();
        nbt.put("Items", nbtTagList);
        nbt.put(GHOST,ghostItemNBT);
        nbt.putString("DankStats", dankStats.name());
        nbt.putInt(Utils.ID, id);
        nbt.putBoolean("locked", locked);
        return nbt;
    }

    static final String GHOST = "GhostItems";

    public void read(CompoundTag nbt) {
        DankStats stats = DankStats.valueOf(nbt.getString("DankStats"));
        upgradeTo(stats);
        ListTag tagList = nbt.getList("Items", Tag.TAG_COMPOUND);
        readItems(tagList);
        ListTag ghostItemList = nbt.getList(GHOST,Tag.TAG_COMPOUND);
        readGhostItems(ghostItemList);
        locked = nbt.getBoolean("locked");
        validate();
    }

    protected void readItems(ListTag listTag) {
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag itemTags = listTag.getCompound(i);
            int slot = itemTags.getInt("Slot");
            if (slot >= 0 && slot < getSlots()) {
                if (itemTags.contains("StackList", Tag.TAG_LIST)) {
                    ItemStack stack = ItemStack.EMPTY;
                    ListTag stackTagList = itemTags.getList("StackList", Tag.TAG_COMPOUND);
                    for (int j = 0; j < stackTagList.size(); j++) {
                        CompoundTag itemTag = stackTagList.getCompound(j);
                        ItemStack temp = ItemStack.of(itemTag);
                        if (!temp.isEmpty()) {
                            if (stack.isEmpty()) stack = temp;
                            else stack.grow(temp.getCount());
                        }
                    }
                    if (!stack.isEmpty()) {
                        int count = stack.getCount();
                        count = Math.min(count, getSlotLimit(slot));
                        stack.setCount(count);

                        this.setStackInSlot(slot, stack);
                    }
                } else {
                    ItemStack stack = ItemStack.of(itemTags);
                    if (itemTags.contains("ExtendedCount", Tag.TAG_INT)) {
                        stack.setCount(itemTags.getInt("ExtendedCount"));
                    }
                    this.setStackInSlot(slot, stack);
                }
            }
        }
    }

    protected void readGhostItems(ListTag listTag) {
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag itemTags = listTag.getCompound(i);
            int slot = itemTags.getInt("Slot");
            if (slot >= 0 && slot < getSlots()) {
                if (itemTags.contains("StackList", Tag.TAG_LIST)) {
                    ItemStack stack = ItemStack.EMPTY;
                    ListTag stackTagList = itemTags.getList("StackList", Tag.TAG_COMPOUND);
                    for (int j = 0; j < stackTagList.size(); j++) {
                        CompoundTag itemTag = stackTagList.getCompound(j);
                        ItemStack temp = ItemStack.of(itemTag);
                        if (!temp.isEmpty()) {
                            if (stack.isEmpty()) stack = temp;
                            else stack.grow(temp.getCount());
                        }
                    }
                    if (!stack.isEmpty()) {
                        this.ghostItems.set(slot, stack);
                    }
                } else {
                    ItemStack stack = ItemStack.of(itemTags);
                    this.ghostItems.set(slot, stack);
                }
            }
        }
    }

    protected void setGhostItems(NonNullList<ItemStack> newGhosts) {
        ghostItems = newGhosts;
    }

    protected void validate() {
        if (dankStats == DankStats.zero) {
            throw new RuntimeException("dank has no stats?");
        } else if (getSlots() == 0) {
            throw new RuntimeException("dank is empty?");
        } else {
            if (ghostItems.size() != getSlots()) {
                throw new RuntimeException("inequal size");
            }
        }
    }

    public int calcRedstone() {
        int numStacks = 0;
        float f = 0F;

        for (int slot = 0; slot < this.getSlots(); slot++) {
            ItemStack stack = this.getStackInSlot(slot);

            if (!stack.isEmpty()) {
                f += (float) stack.getCount() / (float) this.getSlotLimit(slot);
                numStacks++;
            }
        }

        f /= this.getSlots();
        return Mth.floor(f * 14F) + (numStacks > 0 ? 1 : 0);
    }
    @Override
    public void onContentsChanged(int slot) {
        super.onContentsChanged(slot);
        if (DankStorage.instance.data != null) {
            DankStorage.instance.data.setDirty();
        }
    }

    public int getFrequencySlot() {
        return getSlots();
    }

    public int getTextColor() {
        return get(1);
    }

    public void setTextColor(int color) {
        set(1, color);
    }

    public boolean frequencyLocked() {
        return get(2) == 1;
    }

    public void toggleFrequencyLock() {
        boolean loc = get(getFrequencySlot() + 2) == 1;
        setFrequencyLock(!loc);
    }

    public void setFrequencyLock(boolean lock) {
        set(2, lock ? 1 : 0);
    }

    @Override
    public int get(int slot) {
        return switch (slot) {
            case 0 -> id;
            case 1 -> textColor;
            case 2 -> locked ? 1 : 0;
            default -> AbstractContainerMenu.SLOT_CLICKED_OUTSIDE;
        };
    }

    public int getFrequency() {
        return get(0);
    }

    @Override
    public void set(int slot, int value) {
        switch (slot) {
            case 0 -> id = value;
            case 1 -> textColor = value;
            case 2 -> locked = value == 1;
        }
        onContentsChanged(slot);
    }

    public void compress(ServerLevel level) {
        sort();
        int freeSlots = 0;

        Map<Item, Pair<Integer, Integer>> groups = new HashMap<>();

        for (int i = 0; i < getSlots(); i++) {
            ItemStack stack = getStackInSlot(i);
            if (!stack.isEmpty()) {

                if (Utils.canCompress(level, stack)) {

                    Item item = stack.getItem();
                    Pair<Integer, Integer> pair;
                    if (groups.containsKey(item)) {
                        pair = Pair.of(groups.get(item).getFirst(), i);
                    } else {
                        pair = Pair.of(i, i);
                    }
                    groups.put(item, pair);
                }
            } else {
                freeSlots = getSlots() - i;
                break;
            }
        }

        List<Item> unsafeSlots = new ArrayList<>();
        for (Map.Entry<Item, Pair<Integer, Integer>> entry : groups.entrySet()) {
            Item item = entry.getKey();
            Pair<Integer, Integer> pair = entry.getValue();
            Pair<ItemStack, Integer> stackIntegerPair = Utils.compress(level, new ItemStack(item));
            int count = 0;

            for (int i = pair.getFirst(); i < pair.getSecond() + 1; i++) {
                count += getStackInSlot(i).getCount();
            }
            if (count <= getSlots() && count % stackIntegerPair.getSecond() != 0) {
                unsafeSlots.add(item);
            } else {
                ItemStack compressionResult = stackIntegerPair.getFirst();
                int compressedCount = count / stackIntegerPair.getSecond();
                int remainder = count % stackIntegerPair.getSecond();


                //clear out old items
                for (int i = pair.getFirst(); i < pair.getSecond() + 1; i++) {
                    extractItem(i, getSlots(),false);
                }
                int fullStacks = compressedCount / getSlots();

                int partialStack = compressedCount % getSlots();

                //set max stacksize items
                for (int i = pair.getFirst(); i < pair.getFirst() + fullStacks; i++) {
                    setStackInSlot(i, new ItemStack(compressionResult.getItem(), getSlots()));
                }

                //set partial stack of compressed items
                if (partialStack > 0) {
                    setStackInSlot(pair.getFirst() + fullStacks, new ItemStack(compressionResult.getItem(), partialStack));
                }

                if (remainder > 0) {
                    setStackInSlot(pair.getFirst() + fullStacks + 1, new ItemStack(item, partialStack));
                }
            }
        }
        sort();


        for (Item item : unsafeSlots) {
            if (freeSlots <= 0) {
                break;
            } else {
                for (int i = 0; i < getSlots(); i++) {
                    ItemStack stack = getStackInSlot(i);
                    if (stack.getItem() == item) {

                        Pair<ItemStack, Integer> stackIntegerPair = Utils.compress(level, new ItemStack(item));

                        ItemStack compressionResult = stackIntegerPair.getFirst();
                        int compressedCount = stack.getCount() / stackIntegerPair.getSecond();
                        int remainder = stack.getCount() % stackIntegerPair.getSecond();

                        setStackInSlot(i, new ItemStack(compressionResult.getItem(), compressedCount));
                        setStackInSlot(getSlots() - freeSlots, new ItemStack(item, remainder));
                        freeSlots--;
                        break;
                    }
                }
            }
        }
        sort();
    }

    public void sort() {
        List<ItemStack> stacks = new ArrayList<>();

        for (ItemStack stack : getContents()) {
            if (!stack.isEmpty()) {
                Utils.merge(stacks, stack.copy());
            }
        }

        List<ItemStackWrapper> wrappers = Utils.wrap(stacks);

        Collections.sort(wrappers);

        for (int i = 0; i < getSlots();i++) {
            setStackInSlot(i,ItemStack.EMPTY);
            ghostItems.set(i,ItemStack.EMPTY);
        }

        //split up the stacks and add them to the slot

        int slotId = 0;

        for (int i = 0; i < wrappers.size(); i++) {
            ItemStack stack = wrappers.get(i).stack;
            int count = stack.getCount();
            if (count > dankStats.stacklimit) {
                int fullStacks = count / dankStats.stacklimit;
                int partialStack = count - fullStacks * dankStats.stacklimit;

                for (int j = 0; j < fullStacks;j++) {
                    setStackInSlot(slotId, ItemHandlerHelper.copyStackWithSize(stack, dankStats.stacklimit));
                    slotId++;
                }
                if (partialStack > 0) {
                    setStackInSlot(slotId,  ItemHandlerHelper.copyStackWithSize(stack, partialStack));
                    slotId++;
                }
            } else {
                setStackInSlot(slotId,stack);
                slotId++;
            }
            //setStackInSlot(i, stack);
        }
    }

    public void clearContent() {
        stacks.clear();
        onContentsChanged(0);
    }

    public enum TxtColor {
        INVALID(0xffff0000), TOO_HIGH(0xffff8000), DIFFERENT_TIER(0xffffff00), GOOD(0xff00ff00), LOCKED(0xff0000ff);
        public final int color;

        TxtColor(int color) {
            this.color = color;
        }
    }

    //0 - 80 are locked slots, 81 is the id, 82 is text color, and 83 is global lock

    @Override
    public int getCount() {
        return getSlots() + 3;
    }
}
