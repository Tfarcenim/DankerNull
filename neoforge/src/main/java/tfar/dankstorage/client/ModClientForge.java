package tfar.dankstorage.client;


import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import tfar.dankstorage.DankStorage;
import tfar.dankstorage.events.ClientEvents;
import tfar.dankstorage.item.DankItem;
import tfar.dankstorage.network.server.C2SOpenMenuPacket;
import tfar.dankstorage.utils.CommonUtils;
import tfar.dankstorage.utils.KeybindAction;
import tfar.dankstorage.network.server.C2SButtonPacket;
import tfar.dankstorage.utils.UseType;

public class ModClientForge {

    public static void client() {

        NeoForge.EVENT_BUS.addListener(ModClientForge::onPickBlock);
        NeoForge.EVENT_BUS.addListener(ModClientForge::onScroll);
        NeoForge.EVENT_BUS.addListener(ModClientForge::keyPressed);
        NeoForge.EVENT_BUS.addListener(ModClientForge::rightClick);
        CommonClient.setup();
    }

    public static void rightClick(PlayerInteractEvent.RightClickItem event) {
        InteractionHand hand = event.getHand();
        Player player = event.getEntity();
        ItemStack stack = player.getItemInHand(hand);
        if (player.level().isClientSide && stack.getItem() instanceof DankItem && Screen.hasAltDown() && DankItem.getUseType(stack)!= UseType.bag) {
            C2SOpenMenuPacket.send(hand);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    public static void keybinds(RegisterKeyMappingsEvent e) {
        e.register(DankKeybinds.CONSTRUCTION);
        e.register(DankKeybinds.LOCK_SLOT);
        e.register(DankKeybinds.PICKUP_MODE);

        DankKeybinds.CONSTRUCTION.setKeyConflictContext(HoldingItemContext.INSTANCE);
        DankKeybinds.LOCK_SLOT.setKeyConflictContext(KeyConflictContext.GUI);
        DankKeybinds.PICKUP_MODE.setKeyConflictContext(HoldingItemContext.INSTANCE);

    }

    public static void clientTool(RegisterClientTooltipComponentFactoriesEvent e) {
        e.register(DankTooltip.class, CommonClient::tooltipImage);
    }

    public static void keyPressed(ClientTickEvent.Pre client) {
        if (DankKeybinds.CONSTRUCTION.consumeClick()) {
            C2SButtonPacket.send(KeybindAction.TOGGLE_USE_TYPE);
        }
        if (DankKeybinds.PICKUP_MODE.consumeClick()) {
            C2SButtonPacket.send(KeybindAction.TOGGLE_PICKUP);
        }
    }

    public static void renderStack(RegisterGuiLayersEvent e) {
        e.registerBelow(VanillaGuiLayers.CHAT, DankStorage.id("hud"), ClientEvents::renderSelectedItem);
    }

    public static void onPickBlock(InputEvent.InteractionKeyMappingTriggered e) {
        if (e.isPickBlock()) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (CommonUtils.isHoldingDank(player) && mc.hitResult != null && mc.hitResult.getType() != HitResult.Type.MISS) {
                HitResult result = player.pick(player.blockInteractionRange(),0,false);
                if (result instanceof BlockHitResult) {
                    C2SButtonPacket.send(KeybindAction.PICK_BLOCK);
                    e.setCanceled(true);
                }
            }
        }
    }

    public static void onScroll(InputEvent.MouseScrollingEvent e) {
        if (ClientEvents.onScroll(e.getScrollDeltaY()))e.setCanceled(true);
    }
}
