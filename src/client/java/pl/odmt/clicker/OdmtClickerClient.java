package pl.odmt.clicker;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.odmt.clicker.config.ClickerConfig;
import pl.odmt.clicker.gui.ClickerConfigScreen;
import pl.odmt.clicker.hud.ClickerHud;
import pl.odmt.clicker.logic.AutoTool;
import pl.odmt.clicker.logic.ClickEngine;

public class OdmtClickerClient implements ClientModInitializer {
    public static final String MOD_ID = "odmt-clicker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static KeyMapping openConfigKey;
    public static KeyMapping toggleKey;

    @Override
    public void onInitializeClient() {
        ClickerConfig.load();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main"));

        // Strzalka w dol = menu moda
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.odmt-clicker.open_config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_DOWN, category));

        // Klawisz ON/OFF - domyslnie niezbindowany, ustawiasz go w menu moda
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.odmt-clicker.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category));

        // Poczatek ticka: zanim vanilla przetworzy kliki ataku
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            AutoTool.onTick(client);
            ClickEngine.onTick(client);
        });

        // Koniec ticka: obsluga naszych klawiszy
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new ClickerConfigScreen(null));
                }
            }
            while (toggleKey.consumeClick()) {
                ClickEngine.onToggleKey();
            }
        });

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "status"), ClickerHud::render);

        LOGGER.info("ODMT Clicker zaladowany");
    }
}
