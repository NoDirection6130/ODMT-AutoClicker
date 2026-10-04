package pl.odmt.clicker.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import pl.odmt.clicker.config.ClickerConfig;
import pl.odmt.clicker.logic.ClickEngine;

/** Napis "Auto: ON / OFF" */
public final class ClickerHud {
    private static final int WHITE = 0xFFFFFF;
    private static final int GREEN = 0x55FF55;
    private static final int RED = 0xFF5555;

    private ClickerHud() {}

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || !ClickerConfig.get().showHud) return;

        boolean on = ClickEngine.isOn();
        float alpha = on ? 1f : ClickEngine.offAlpha();
        int a = Math.round(alpha * 255f);
        if (a < 10) return;

        String label = "Auto: ";
        String state = on ? "ON" : "OFF";

        Font font = mc.font;
        int labelWidth = font.width(label);
        int totalWidth = labelWidth + font.width(state);
        int x = graphics.guiWidth() / 2 - totalWidth / 2;
        int y = computeY(player, graphics.guiHeight());

        int alphaBits = a << 24;
        graphics.drawString(font, label, x, y, alphaBits | WHITE, true);
        graphics.drawString(font, state, x + labelWidth, y, alphaBits | (on ? GREEN : RED), true);
    }

    /** Liczy pozycje nad armorem, z uwzglednieniem dodatkowych rzedow serc (absorpcja itp.). */
    private static int computeY(LocalPlayer player, int screenHeight) {
        float totalHealth = player.getMaxHealth() + player.getAbsorptionAmount();
        int rows = Math.max(1, (int) Math.ceil(totalHealth / 2.0f / 10.0f));
        int rowHeight = Math.max(10 - (rows - 2), 3);
        int heartsTop = screenHeight - 39 - (rows - 1) * rowHeight;
        int armorTop = heartsTop - 10;
        // -72 trzyma napis nad nazwa trzymanego itemu (ta pojawia sie przy zmianie slotu)
        return Math.min(screenHeight - 72, armorTop - 12);
    }
}
