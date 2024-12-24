package tfar.dankstorage.utils;

import com.mojang.datafixers.util.Pair;
import io.netty.buffer.Unpooled;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import tfar.dankstorage.DankStorage;
import tfar.dankstorage.init.ModDataComponentTypes;
import tfar.dankstorage.inventory.DankInventory;
import tfar.dankstorage.inventory.LimitedContainerData;
import tfar.dankstorage.item.DankItem;
import tfar.dankstorage.menu.DankMenu;
import tfar.dankstorage.menu.ChangeFrequencyMenu;
import tfar.dankstorage.world.DankSavedData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class CommonUtils {

    public static final int INVALID = -1;

    private static final DecimalFormat decimalFormat = new DecimalFormat("0.##");

    public static String formatLargeNumber(int number) {

        if (number >= 1000000000) return decimalFormat.format(number / 1000000000f) + "b";
        if (number >= 1000000) return decimalFormat.format(number / 1000000f) + "m";
        if (number >= 1000) return decimalFormat.format(number / 1000f) + "k";

        return Float.toString(number).replaceAll("\\.?0*$", "");
    }

    public static DankStats getStatsfromRows(int rows) {
        switch (rows) {
            case 1:
                return DankStats.one;
            case 2:
                return DankStats.two;
            case 3:
                return DankStats.three;
            case 4:
                return DankStats.four;
            case 5:
                return DankStats.five;
            case 6:
                return DankStats.six;
            case 9:
                return DankStats.seven;
        }
        throw new IllegalStateException(String.valueOf(rows));
    }

    private static List<CraftingRecipe> REVERSIBLE3x3 = new ArrayList<>();
    private static List<CraftingRecipe> REVERSIBLE2x2 = new ArrayList<>();
    private static boolean cached = false;

    public static void uncacheRecipes() {
        cached = false;
    }

    public static Pair<ItemStack, Integer> compress(ItemStack stack, RegistryAccess registryAccess) {

        for (CraftingRecipe recipe : REVERSIBLE3x3) {
            if (recipe.getIngredients().getFirst().test(stack)) {
                return Pair.of(recipe.getResultItem(registryAccess), 9);
            }
        }

        for (CraftingRecipe recipe : REVERSIBLE2x2) {
            if (recipe.getIngredients().getFirst().test(stack)) {
                return Pair.of(recipe.getResultItem(registryAccess), 4);
            }
        }
        return Pair.of(ItemStack.EMPTY, 0);
    }

    public static Pair<ItemStack,ItemStack> getCompressingResult(ItemStack stack, ServerLevel level) {
        if (!canCompress(level, stack)) {
            return Pair.of(stack,ItemStack.EMPTY);
        } else {
            Pair<ItemStack,Integer> result = compress(stack,level.registryAccess());
            ItemStack compressedStack = result.getFirst().copyWithCount(stack.getCount()/result.getSecond());
            ItemStack remainder = stack.copyWithCount(stack.getCount() % result.getSecond());
            return Pair.of(remainder,compressedStack);
        }
    }

    public static boolean canCompress(ServerLevel level, ItemStack stack) {
        if (!cached) {
            REVERSIBLE3x3 = findReversibles(level, 3);
            REVERSIBLE2x2 = findReversibles(level, 2);
            cached = true;
        }

        for (CraftingRecipe recipe : REVERSIBLE3x3) {
            if (recipe.getIngredients().getFirst().test(stack)) {
                return stack.getCount() == 9;
            }
        }

        for (CraftingRecipe recipe : REVERSIBLE2x2) {
            if (recipe.getIngredients().getFirst().test(stack)) {
                return stack.getCount() == 4;
            }
        }

        return false;
    }


    public static void setPickSlot(Level level, ItemStack bag, ItemStack stack) {

        DankInventory dankInterface = DankItem.getInventoryFrom(bag, level.getServer());

        if (dankInterface != null) {

        }
    }

    public static List<CraftingRecipe> findReversibles(ServerLevel level, int size) {
        List<CraftingRecipe> compactingRecipes = new ArrayList<>();
        List<RecipeHolder<CraftingRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING);

        for (RecipeHolder<CraftingRecipe> recipe : recipes) {
            if (recipe.value() instanceof ShapedRecipe shapedRecipe) {
                int x = shapedRecipe.getWidth();
                int y = shapedRecipe.getHeight();
                if (x == size && x == y) {

                    List<Ingredient> inputs = shapedRecipe.getIngredients();

                    Ingredient first = inputs.getFirst();
                    if (first != Ingredient.EMPTY) {
                        boolean same = true;
                        for (int i = 1; i < x * y; i++) {
                            Ingredient next = inputs.get(i);
                            if (next != first) {
                                same = false;
                                break;
                            }
                        }
                        if (same && shapedRecipe.getResultItem(level.registryAccess()).getCount() == 1) {
                            ItemStack stack = shapedRecipe.getResultItem(level.registryAccess());

                            level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, makeCraftInput(stack), level).ifPresent(reverseRecipe -> {
                                if (reverseRecipe.value().getResultItem(level.registryAccess()).getCount() == size * size) {
                                    compactingRecipes.add(shapedRecipe);
                                }
                            });
                        }
                    }
                }
            }
        }
        return compactingRecipes;
    }

    private static CraftingInput makeCraftInput(ItemStack stack) {
        return CraftingInput.of(1, 1, List.of(stack));
    }

    public static class DummyCraftingContainer implements CraftingContainer {
        private final NonNullList<ItemStack> items;
        private final int width;
        private final int height;

        public DummyCraftingContainer(int p_287629_, int p_287593_) {
            this(p_287629_, p_287593_, NonNullList.withSize(p_287629_ * p_287593_, ItemStack.EMPTY));
        }

        public DummyCraftingContainer(int p_287591_, int p_287609_, NonNullList<ItemStack> p_287695_) {
            this.items = p_287695_;
            this.width = p_287591_;
            this.height = p_287609_;
        }

        public int getContainerSize() {
            return this.items.size();
        }

        public boolean isEmpty() {
            for (ItemStack itemstack : this.items) {
                if (!itemstack.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        public ItemStack getItem(int slot) {
            return slot >= this.getContainerSize() ? ItemStack.EMPTY : this.items.get(slot);
        }

        public ItemStack removeItemNoUpdate(int slot) {
            return ContainerHelper.takeItem(this.items, slot);
        }

        public ItemStack removeItem(int i, int i1) {
            return ContainerHelper.removeItem(this.items, i, i1);
        }

        public void setItem(int slot, ItemStack stack) {
            this.items.set(slot, stack);
        }

        public void setChanged() {
        }

        public boolean stillValid(Player player) {
            return true;
        }

        public void clearContent() {
            this.items.clear();
        }

        public int getHeight() {
            return this.height;
        }

        public int getWidth() {
            return this.width;
        }

        public List<ItemStack> getItems() {
            return List.copyOf(this.items);
        }

        public void fillStackedContents(StackedContents contents) {
            for (ItemStack itemstack : this.items) {
                contents.accountSimpleStack(itemstack);
            }
        }
    }

    public static void merge(List<ItemStack> stacks, ItemStack toMerge) {
        for (ItemStack stack : stacks) {
            if (ItemStack.isSameItemSameComponents(stack, toMerge)) {
                int grow = Math.min(Integer.MAX_VALUE - stack.getCount(), toMerge.getCount());
                if (grow > 0) {
                    stack.grow(grow);
                    toMerge.shrink(grow);
                }
            }
        }
        if (!toMerge.isEmpty()) {
            stacks.add(toMerge);
        }
    }

    public static MutableComponent translatable(String s) {
        return Component.translatable(s);
    }

    public static MutableComponent translatable(String string, Object... objects) {
        return Component.translatable(string, objects);
    }

    public static MutableComponent literal(String s) {
        return Component.literal(s);
    }

    public static boolean oredict(ItemStack bag) {
        return bag.has(ModDataComponentTypes.OREDICT);
    }

    public static void warn(Player player, DankStats item, DankStats inventory) {
        player.sendSystemMessage(literal("Dank Item Level " + item.ordinal() + " cannot open Dank Inventory Level " + inventory.ordinal()));
    }

    public static int getNbtSize(@Nullable CompoundTag nbt) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeNbt(nbt);
        buffer.release();
        return buffer.writerIndex();
    }

    public static boolean isHoldingDank(@Nullable Player player) {

        if (player == null) return false;

        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof DankItem) return true;
        stack = player.getOffhandItem();
        return stack.getItem() instanceof DankItem;
    }

    @Nullable
    public static InteractionHand getHandWithDank(Player player) {
        if (player.getMainHandItem().getItem() instanceof DankItem) return InteractionHand.MAIN_HAND;
        else if (player.getOffhandItem().getItem() instanceof DankItem) return InteractionHand.OFF_HAND;
        return null;
    }

    public static ItemStack getDank(Player player) {
        InteractionHand hand = getHandWithDank(player);
        return hand == null ? ItemStack.EMPTY : player.getItemInHand(hand);
    }

    public static void toggleTagMode(ServerPlayer player) {
        ItemStack dank = getDank(player);
        if (!dank.isEmpty()) {
            boolean toggle = oredict(dank);
            setOredict(dank,!toggle);
        }
    }

    public static void setOredict(ItemStack bag, boolean active) {
        if (active) {
            bag.set(ModDataComponentTypes.OREDICT, Unit.INSTANCE);
        } else {
            bag.remove(ModDataComponentTypes.OREDICT);
        }
    }

    public static void togglePickupMode(ServerPlayer player) {
        ItemStack bag = getDank(player);
        if (!bag.isEmpty()) {
            DankItem.cyclePickupMode(bag, player);
        }
    }

    public static void toggleUseType(ServerPlayer player) {
        ItemStack dank = getDank(player);
        if (!dank.isEmpty()) {
            DankItem.cyclePlacement(dank, player);
        }
    }

    public static void setTxtColor(ServerPlayer player, int frequency, boolean set) {
        AbstractContainerMenu container = player.containerMenu;
        if (container instanceof DankMenu abstractDankMenu) {
            DankInventory inventory = abstractDankMenu.dankInventory;

            TxtColor textColor;

            if (frequency > INVALID) {
                if (frequency < DankStorage.firstFreeId(player.server)) {
                    DankInventory targetInventory = DankSavedData.get(frequency, player.server).getOrCreateInventory();

                    if (targetInventory.slotCount() == inventory.slotCount()) {

                        if (targetInventory.frequencyLocked()) {
                            textColor = TxtColor.LOCKED;
                        } else {
                            textColor = TxtColor.GOOD;
                            if (set) {
                                abstractDankMenu.setFrequency(frequency);
                                ItemStack bag = abstractDankMenu.bag;
                                if (bag.getItem() instanceof DankItem dankItem) {
                                    player.openMenu(dankItem.createProvider(bag));
                                }
                            }
                        }
                    } else {
                        textColor = TxtColor.DIFFERENT_TIER;
                    }
                } else {
                    //orange if it doesn't exist, yellow if it does but wrong tier
                    textColor = TxtColor.TOO_HIGH;
                }
            } else {
                textColor = TxtColor.INVALID;
            }
            inventory.setTextColor(textColor.color);
        } else if (container instanceof ChangeFrequencyMenu changeFrequencyMenu) {
            DankInventory inventory = (DankInventory) ((LimitedContainerData) changeFrequencyMenu.getContainerData()).getWrapped();

            TxtColor textColor;

            if (frequency > INVALID) {
                if (frequency < DankStorage.firstFreeId(player.server)) {
                    DankInventory targetInventory = DankSavedData.get(frequency, player.server).getOrCreateInventory();

                    if (targetInventory.slotCount() == DankStats.values()[changeFrequencyMenu.getCurrentTier()].slots) {

                        if (targetInventory.frequencyLocked()) {
                            textColor = TxtColor.LOCKED;
                        } else {
                            textColor = TxtColor.GOOD;
                            if (set) {
                                changeFrequencyMenu.setLinkedFrequency(frequency);
                                player.closeContainer();
                            }
                        }
                    } else {
                        textColor = TxtColor.DIFFERENT_TIER;
                    }
                } else {
                    //orange if it doesn't exist, yellow if it does but wrong tier
                    textColor = TxtColor.TOO_HIGH;
                }
            } else {
                textColor = TxtColor.INVALID;
            }
            inventory.setTextColor(textColor.color);
        }
    }

    public static DankStats getDefaultStats(ItemStack bag) {
        return ((DankItem) bag.getItem()).stats;
    }


        /*public static boolean areItemStacksConvertible(final ItemStack stack1, final ItemStack stack2) {
        if (stack1.hasTag() || stack2.hasTag()) return false;
        Collection<ResourceLocation> taglistofstack1 = getTags(stack1.getItem());
        Collection<ResourceLocation> taglistofstack2 = getTags(stack2.getItem());

        Set<ResourceLocation> commontags = new HashSet<>(taglistofstack1);
        commontags.retainAll(taglistofstack2);
        commontags.retainAll(taglist);
        return !commontags.isEmpty();
    }

    private static Collection<ResourceLocation> getTags(Item item) {
        return getTagsFor(ItemTags.getAllTags(), item);
    }

    /**
     * can't use TagGroup#getTagsFor because it's client only

    private static Collection<ResourceLocation> getTagsFor(TagCollection<Item> tagGroup, Item item) {
        return tagGroup.getAllTags().entrySet().stream()
                .filter(identifierTagEntry -> identifierTagEntry.getValue().contains(item))
                .map(Map.Entry::getKey).collect(Collectors.toList());
    }*/


}
