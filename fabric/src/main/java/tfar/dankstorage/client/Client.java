package tfar.dankstorage.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.lwjgl.glfw.GLFW;
import tfar.dankstorage.DankStorageFabric;
import tfar.dankstorage.client.screens.DankStorageScreen;
import tfar.dankstorage.container.DankMenu;
import tfar.dankstorage.container.DockMenu;
import tfar.dankstorage.event.FabricEvents;
import tfar.dankstorage.network.ClientDankPacketHandler;
import tfar.dankstorage.network.server.C2SButtonPacket;

public class Client {

    public static void client() {

        HudRenderCallback.EVENT.register(FabricEvents::renderStack);

        ClientDankPacketHandler.registerClientMessages();
        MenuScreens.register(DankStorageFabric.dank_1_container, (DockMenu container1, Inventory playerinventory1, Component component1) -> DankStorageScreen.t1(container1, playerinventory1, component1));
        MenuScreens.register(DankStorageFabric.portable_dank_1_container, (DankMenu container, Inventory playerinventory, Component component) -> DankStorageScreen.t1(container, playerinventory, component));
        MenuScreens.register(DankStorageFabric.dank_2_container, (DockMenu container, Inventory playerinventory, Component component) -> DankStorageScreen.t2(container, playerinventory, component));
        MenuScreens.register(DankStorageFabric.portable_dank_2_container, (DankMenu container, Inventory playerinventory, Component component) -> DankStorageScreen.t2(container, playerinventory, component));
        MenuScreens.register(DankStorageFabric.dank_3_container, (DockMenu container, Inventory playerinventory, Component component) -> DankStorageScreen.t3(container, playerinventory, component));
        MenuScreens.register(DankStorageFabric.portable_dank_3_container, (DankMenu container, Inventory playerinventory, Component component) -> DankStorageScreen.t3(container, playerinventory, component));
        MenuScreens.register(DankStorageFabric.dank_4_container, (DockMenu container, Inventory playerinventory, Component component) -> DankStorageScreen.t4(container, playerinventory, component));
        MenuScreens.register(DankStorageFabric.portable_dank_4_container, (DankMenu container, Inventory playerinventory, Component component) -> DankStorageScreen.t4(container, playerinventory, component));
        MenuScreens.register(DankStorageFabric.dank_5_container, (DockMenu container, Inventory playerinventory, Component component) -> DankStorageScreen.t5(container, playerinventory, component));
        MenuScreens.register(DankStorageFabric.portable_dank_5_container, (DankMenu container, Inventory playerinventory, Component component) -> DankStorageScreen.t5(container, playerinventory, component));
        MenuScreens.register(DankStorageFabric.dank_6_container, (DockMenu container, Inventory playerinventory, Component component) -> DankStorageScreen.t6(container, playerinventory, component));
        MenuScreens.register(DankStorageFabric.portable_dank_6_container, (DankMenu container, Inventory playerinventory, Component component) -> DankStorageScreen.t6(container, playerinventory, component));
        MenuScreens.register(DankStorageFabric.dank_7_container, (DockMenu container, Inventory playerinventory, Component component) -> DankStorageScreen.t7(container, playerinventory, component));
        MenuScreens.register(DankStorageFabric.portable_dank_7_container, (DankMenu container, Inventory playerinventory, Component component) -> DankStorageScreen.t7(container, playerinventory, component));

        DankKeybinds.CONSTRUCTION = new KeyMapping("key.dankstorage.construction", GLFW.GLFW_KEY_I, "key.categories.dankstorage");
        DankKeybinds.LOCK_SLOT = new KeyMapping("key.dankstorage.lock_slot", GLFW.GLFW_KEY_LEFT_CONTROL, "key.categories.dankstorage");
        DankKeybinds.PICKUP_MODE = new KeyMapping("key.dankstorage.pickup_mode", GLFW.GLFW_KEY_O, "key.categories.dankstorage");

        KeyBindingHelper.registerKeyBinding(DankKeybinds.CONSTRUCTION);
        KeyBindingHelper.registerKeyBinding(DankKeybinds.LOCK_SLOT);
        KeyBindingHelper.registerKeyBinding(DankKeybinds.PICKUP_MODE);
        ClientTickEvents.START_CLIENT_TICK.register(Client::keyPressed);
        TooltipComponentCallback.EVENT.register(Client::tooltipImage);
    }

    public static void keyPressed(Minecraft client) {
        if (DankKeybinds.CONSTRUCTION.consumeClick()) {
            C2SButtonPacket.send(C2SButtonPacket.Action.TOGGLE_USE_TYPE);
        }
        if (DankKeybinds.PICKUP_MODE.consumeClick()) {
            C2SButtonPacket.send(C2SButtonPacket.Action.TOGGLE_PICKUP);
        }
    }

    public static ClientTooltipComponent tooltipImage(TooltipComponent data) {
        if (data instanceof DankTooltip dankTooltip) {
            return new ClientDankTooltip(dankTooltip);
        }
        return null;
    }
}
