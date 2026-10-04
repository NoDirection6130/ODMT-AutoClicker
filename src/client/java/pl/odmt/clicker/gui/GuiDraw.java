package pl.odmt.clicker.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/** Rysowanie w stylu okien Minecrafta */
final class GuiDraw {
    static final int TEXT = 0xFF404040;
    static final int CELL = 18;

    private GuiDraw() {}

    static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF000000);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFC6C6C6);
        g.fill(x + 1, y + 1, x + w - 2, y + 3, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + 3, y + h - 2, 0xFFFFFFFF);
        g.fill(x + 2, y + h - 3, x + w - 1, y + h - 1, 0xFF555555);
        g.fill(x + w - 3, y + 2, x + w - 1, y + h - 1, 0xFF555555);
    }

    static void slot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + CELL, y + CELL, 0xFF8B8B8B);
        g.fill(x, y, x + CELL - 1, y + 1, 0xFF373737);
        g.fill(x, y, x + 1, y + CELL - 1, 0xFF373737);
        g.fill(x + 1, y + CELL - 1, x + CELL, y + CELL, 0xFFFFFFFF);
        g.fill(x + CELL - 1, y + 1, x + CELL, y + CELL, 0xFFFFFFFF);
    }

    static void hover(GuiGraphics g, int x, int y) {
        g.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, 0x80FFFFFF);
    }

    static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    static void clickSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
