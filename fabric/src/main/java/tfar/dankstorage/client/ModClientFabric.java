package tfar.dankstorage.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.minecraft.client.Minecraft;
import tfar.dankstorage.network.server.C2SButtonPacket;
import tfar.dankstorage.utils.KeybindAction;

public class ModClientFabric {

    public static void client() {
        CommonClient.setup();

        KeyBindingHelper.registerKeyBinding(DankKeybinds.CONSTRUCTION);
        KeyBindingHelper.registerKeyBinding(DankKeybinds.LOCK_SLOT);
        KeyBindingHelper.registerKeyBinding(DankKeybinds.PICKUP_MODE);
        ClientTickEvents.START_CLIENT_TICK.register(ModClientFabric::keyPressed);
        TooltipComponentCallback.EVENT.register(CommonClient::tooltipImage);
    }

    public static void keyPressed(Minecraft client) {
        if (DankKeybinds.CONSTRUCTION.consumeClick()) {
            C2SButtonPacket.send(KeybindAction.TOGGLE_USE_TYPE);
        }
        if (DankKeybinds.PICKUP_MODE.consumeClick()) {
            C2SButtonPacket.send(KeybindAction.TOGGLE_PICKUP);
        }
    }
}
