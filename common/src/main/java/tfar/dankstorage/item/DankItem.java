package tfar.dankstorage.item;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import tfar.dankstorage.client.DankKeybinds;
import tfar.dankstorage.client.DankTooltip;
import tfar.dankstorage.init.ModDataComponentTypes;
import tfar.dankstorage.inventory.DankInventory;
import tfar.dankstorage.inventory.LimitedContainerData;
import tfar.dankstorage.inventory.TierDataSlot;
import tfar.dankstorage.menu.ChangeFrequencyMenu;
import tfar.dankstorage.menu.DankMenu;
import tfar.dankstorage.mixin.ItemUsageContextAccessor;
import tfar.dankstorage.network.server.C2SRequestContentsPacket;
import tfar.dankstorage.platform.Services;
import tfar.dankstorage.utils.*;
import tfar.dankstorage.world.ClientData;
import tfar.dankstorage.world.DankSavedData;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class DankItem extends Item {

    public final DankStats stats;

    public DankItem(Properties $$0, DankStats stats) {
        super($$0);
        this.stats = stats;
    }

    @Override
    public void appendHoverText(ItemStack bag, TooltipContext pContext, List<Component> tooltip, TooltipFlag pTooltipFlag) {
        super.appendHoverText(bag, pContext, tooltip, pTooltipFlag);


        int id = getFrequency(bag);
        tooltip.add(CommonUtils.literal("ID: " + id));

        if (!Screen.hasShiftDown()) {
            tooltip.add(CommonUtils.translatable("text.dankstorage.shift",
                    CommonUtils.literal("Shift").withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GRAY));
        } else {

            tooltip.add(CommonUtils.translatable("text.dankstorage.change_pickup_mode", DankKeybinds.PICKUP_MODE.getTranslatedKeyMessage().copy()
                    .withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GRAY));
            PickupMode pickupMode = getPickupMode(bag);
            tooltip.add(
                    CommonUtils.translatable("text.dankstorage.current_pickup_mode", pickupMode.translate().withStyle(ChatFormatting.YELLOW))
                            .withStyle(ChatFormatting.GRAY));


            tooltip.add(CommonUtils.translatable("text.dankstorage.changeusetype", DankKeybinds.CONSTRUCTION.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GRAY));
            UseType useType = getUseType(bag);
            tooltip.add(
                    CommonUtils.translatable("text.dankstorage.currentusetype", CommonUtils.translatable(
                            "dankstorage.usetype." + useType.name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GRAY));
            tooltip.add(
                    CommonUtils.translatable("text.dankstorage.stacklimit", CommonUtils.literal(stats.stacklimit + "").withStyle(ChatFormatting.GREEN)).withStyle(ChatFormatting.GRAY));
        }

        if (Services.PLATFORM.isDevelopmentEnvironment()) {
            appendDevOnly(bag, pContext, tooltip, pTooltipFlag);
        }
    }

    protected void appendDevOnly(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {

    }

   /* @Override
    public void appendHoverText(ItemStack bag, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (bag.hasTag()) {

            if (Services.PLATFORM.isDevelopmentEnvironment()) {
                String s = bag.getTag().toString();

                List<String> bits = new ArrayList<>();

                int length = s.length();

                if (s.length() > 10000) return;

                int itr = (int) Math.ceil(length / 40d);

                for (int i = 0; i < itr; i++) {

                    int end = (i + 1) * 40;

                    if ((i + 1) * 40 - 1 >= length) {
                        end = length;
                    }

                    String s1 = s.substring(i * 40, end);
                    bits.add(s1);
                }

                bits.forEach(s1 -> tooltip.add(CommonUtils.literal(s1)));

            }
        }

    }*/

    //this is called on the client
    @Override
    public InteractionResult interactLivingEntity(ItemStack bag, Player player, LivingEntity entity, InteractionHand hand) {
        if (!isConstruction(bag)) return InteractionResult.PASS;

        ItemStack toUse = getSelectedItem(bag);
        if (toUse.isEmpty()) return InteractionResult.PASS;
        player.setItemInHand(hand, toUse);
        InteractionResult result = toUse.getItem().interactLivingEntity(toUse, player, entity, hand);

        //the client doesn't have access to the full inventory
        if (!player.level().isClientSide) {
            DankInventory handler = getInventoryFrom(bag, player.getServer());
            //handler.setItemDank(getSelectedItem(bag), toUse);todo
        }

        player.setItemInHand(hand, bag);
        return result;
    }

    //  @Override
    public boolean canBeHurtBy(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getPickupMode(stack) != PickupMode.none;
    }

    public MenuProvider createProvider(ItemStack stack) {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return stack.getHoverName();
            }

            @Override
            public AbstractContainerMenu createMenu(int i, Inventory playerInventory, Player player) {
                MinecraftServer server = player.getServer();
                if (getFrequency(stack) == CommonUtils.INVALID) {
                    assignNextFreeId(server, stack);
                    DankSavedData tankSavedData = DankSavedData.getOrCreate(getFrequency(stack), server);
                    tankSavedData.setStats(stats);
                }


                DankInventory dankInventory = getInventoryFrom(stack, player.getServer());
                int defaults = stats.slots;


                int type = dankInventory.slotCount();

                if (defaults != type) {
                    if (defaults < type) {//if the default stats are lower than what saveddata reports, abort opening
                        return new ChangeFrequencyMenu(i, playerInventory, new LimitedContainerData(dankInventory, 3), new TierDataSlot(stats), stack);
                        //CommonUtils.warn(player, defaults, type);
                        //return null;
                    }
                    dankInventory.upgradeTo(stats);
                }

                return switch (stats) {
                    default -> DankMenu.t1s(i, playerInventory, dankInventory, stack);
                    case two -> DankMenu.t2s(i, playerInventory, dankInventory, stack);
                    case three -> DankMenu.t3s(i, playerInventory, dankInventory, stack);
                    case four -> DankMenu.t4s(i, playerInventory, dankInventory, stack);
                    case five -> DankMenu.t5s(i, playerInventory, dankInventory, stack);
                    case six -> DankMenu.t6s(i, playerInventory, dankInventory, stack);
                    case seven -> DankMenu.t7s(i, playerInventory, dankInventory, stack);
                };
            }
        };
    }

    public int getGlintColor(ItemStack stack) {
        PickupMode pickupMode = getPickupMode(stack);
        switch (pickupMode) {
            case none:
            default:
                return 0xffffffff;
            case pickup_all:
                return 0xff00ff00;
            case filtered_pickup:
                return 0xffffff00;
            case void_pickup:
                return 0xffff0000;
        }
    }

    @Nonnull
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        ItemStack bag = ctx.getItemInHand();
        Level level = ctx.getLevel();
        UseType useType = getUseType(bag);

        if (useType == UseType.bag) {
            return InteractionResult.PASS;
        }

        ItemStack selected = getSelectedItem(bag);

        //invalid slot
        if (selected.isEmpty()) {
            return InteractionResult.PASS;
        }

        ItemStack toPlace = getSelectedItem(bag);
        //todo: sync locked slots to client?
        if (/*toPlace.getCount() == 1 && handler.isLocked(selected)*/ false)
            return InteractionResult.PASS;

        UseOnContext ctx2 = new UseOnContext2(ctx.getLevel(), ctx.getPlayer(), ctx.getHand(), toPlace.copy(), ((ItemUsageContextAccessor) ctx).getHitResult());
        InteractionResult actionResultType = toPlace.getItem().useOn(ctx2);//ctx2.getItem().onItemUse(ctx);
        if (!level.isClientSide) {
            DankInventory dankInventory = getInventoryFrom(bag, level.getServer());
            //dankInventory.setItemDank(selected, ctx2.getItemInHand());

            //a wild assumption, lets see if it backfires
            int consumed = toPlace.getCount() - ctx2.getItemInHand().getCount();

            dankInventory.extractStackTarget(consumed, false, toPlace);
        }
        return actionResultType;
    }

    @Nonnull
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack bag = player.getItemInHand(hand);

        if (getUseType(bag) == UseType.bag) {
            if (!level.isClientSide) {
                player.openMenu(createProvider(bag));
            }
            return InteractionResultHolder.success(bag);
        } else {
            if (!level.isClientSide) {
                ItemStack toPlace = getSelectedItem(bag);
                EquipmentSlot hand1 = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                //handle empty
                if (toPlace.isEmpty()) {
                    return InteractionResultHolder.pass(bag);
                }

                //todo support other items?
                else {
                    ItemStack bagCopy = bag.copy();
                    player.setItemSlot(hand1, toPlace);
                    InteractionResultHolder<ItemStack> actionResult = toPlace.getItem().use(level, player, hand);
                    DankInventory handler = getInventoryFrom(bagCopy, level.getServer());
                    //a wild assumption, lets see if it backfires

                    handler.extractStackTarget(toPlace.getCount() - actionResult.getObject().getCount(), false, toPlace);
                    player.setItemSlot(hand1, bagCopy);
                }
            }
            return new InteractionResultHolder<>(InteractionResult.PASS, player.getItemInHand(hand));
        }
    }

    static class UseOnContext2 extends UseOnContext {

        protected UseOnContext2(Level $$0, @org.jetbrains.annotations.Nullable Player $$1, InteractionHand $$2, ItemStack $$3, BlockHitResult $$4) {
            super($$0, $$1, $$2, $$3, $$4);
        }
    }

    static long lastRequest;

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack itemStack) {

        int id = getFrequency(itemStack);

        if (id > CommonUtils.INVALID) {
            //don't spam the server with requests
            if (Util.getMillis() - lastRequest > 50) {
                //don't spam the server with requests
                C2SRequestContentsPacket.send(id);
                lastRequest = Util.getMillis();
            }

            if (ClientData.cachedItems != null) {
                NonNullList<ItemStack> nonNullList = NonNullList.create();
                nonNullList.addAll(ClientData.cachedItems);
                return Optional.of(new DankTooltip(nonNullList, getSelectedItem(itemStack)));
            }
        }
        return Optional.empty();
    }

    public static void assignNextFreeId(MinecraftServer server, ItemStack stack) {
        int id = 0;
        while (true) {
            DankSavedData tankSavedData = DankSavedData.get(id, server);
            if (tankSavedData == null) {
                stack.set(ModDataComponentTypes.FREQUENCY, id);
                return;
            }
            id++;
        }
    }

    @Override
    public void inventoryTick(ItemStack bag, Level level, Entity entity, int i, boolean equipped) {
        //there has to be a better way
        if (entity instanceof ServerPlayer player) {
            ItemStack sel = getSelectedItem(bag);
            if (!sel.isEmpty()) {
                DankInventory dankInventory = getInventoryFrom(bag, player.server);
                if (dankInventory != null) {
                    long amount = dankInventory.countItem(sel);
                    if (amount != sel.getCount()) {
                        setSelectedItem(bag, sel.copyWithCount((int) amount));
                    }
                }
            }
        }
    }

    /////////helpers
    public static void setSelectedItem(ItemStack bag, ItemStack item) {
        if (item.isEmpty()) {
            bag.remove(ModDataComponentTypes.SELECTED);
        } else {
            bag.set(ModDataComponentTypes.SELECTED, new ItemStackComponent(item));
        }
    }

    public static ItemStack getSelectedItem(ItemStack bag) {
        return bag.getOrDefault(ModDataComponentTypes.SELECTED, ItemStackComponent.EMPTY).itemStack();
    }


    public static void setFrequency(ItemStack bag, int frequency) {
        if (frequency < 0) {
            bag.remove(ModDataComponentTypes.FREQUENCY);
        } else {
            bag.set(ModDataComponentTypes.FREQUENCY, frequency);
        }
    }

    public static int getFrequency(ItemStack stack) {
        return stack.getOrDefault(ModDataComponentTypes.FREQUENCY, CommonUtils.INVALID);
    }

    public static DankInventory getInventoryFrom(ItemStack bag, MinecraftServer server) {
        int frequency = getFrequency(bag);
        if (frequency < 0) return null;
        return DankSavedData.get(frequency, server).getOrCreateInventory();
    }

    public static void changeSelectedItem(ItemStack mainHandItem, boolean right, ServerPlayer player) {
        DankInventory dankInventory = DankItem.getInventoryFrom(mainHandItem, player.server);
        ItemStack current = getSelectedItem(mainHandItem);
        if (dankInventory != null) {
            List<ItemStack> gathered = dankInventory.getUniqueItems();
            if (!gathered.isEmpty()) {
                int index = -1;

                for (int i = 0; i < gathered.size(); i++) {
                    if (ItemStack.isSameItemSameComponents(current, gathered.get(i))) {
                        index = i;
                        break;
                    }
                }


                if (index > -1) {
                    int next = -1;//index+1;

                    if (right) {
                        next = index + 1;
                    } else {
                        next = index - 1;
                    }

                    if (next >= gathered.size()) {
                        next = 0;
                    }

                    if (next == -1) {
                        next = gathered.size() - 1;
                    }

                    setSelectedItem(mainHandItem, gathered.get(next));
                } else {
                    setSelectedItem(mainHandItem, gathered.getFirst());
                }
            }
        }
    }

    public static PickupMode getPickupMode(ItemStack bag) {
        return bag.getOrDefault(ModDataComponentTypes.PICKUP_MODE, PickupMode.none);
    }

    public static void setPickupMode(ItemStack bag, PickupMode mode) {
        bag.set(ModDataComponentTypes.PICKUP_MODE, mode);
    }

    public static boolean isConstruction(ItemStack bag) {
        return getUseType(bag) == UseType.construction;
    }

    //0,1,2,3
    public static void cyclePickupMode(ItemStack bag, Player player) {
        PickupMode mode = getPickupMode(bag);
        PickupMode cycle = cycle(mode);
        setPickupMode(bag, cycle);
        player.displayClientMessage(CommonUtils.translatable("dankstorage.mode." + mode), true);
    }

    public static UseType getUseType(ItemStack bag) {
        return bag.getOrDefault(ModDataComponentTypes.USE_TYPE, UseType.bag);
    }

    //0,1,2
    public static void cyclePlacement(ItemStack bag, Player player) {
        UseType useType = getUseType(bag);
        UseType cycle = cycle(useType);
        setUseType(bag, cycle);
        player.displayClientMessage(CommonUtils.translatable("dankstorage.usetype." + cycle), true);
    }

    public static void setUseType(ItemStack bag, UseType useType) {
        bag.set(ModDataComponentTypes.USE_TYPE, useType);
    }


    public static <E extends Enum<E>> E cycle(E e) {
        E[] values = (E[]) e.getClass().getEnumConstants();
        return values[(e.ordinal() + 1) % values.length];
    }
}
