package pl.odmt.clicker.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import pl.odmt.clicker.config.ClickerConfig;

import java.util.List;
import java.util.Optional;

import static pl.odmt.clicker.gui.GuiDraw.CELL;
import static pl.odmt.clicker.gui.GuiDraw.TEXT;

/** Lista blokow, na ktorych auto-tool nie zmienia narzedzia. */
public class ExcludedBlocksScreen extends Screen {
    private static final int COLS = 9;
    private static final int ROWS = 6;

    private final Screen parent;
    private List<FormattedCharSequence> descLines = List.of();
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    public ExcludedBlocksScreen(Screen parent) {
        super(Component.translatable("odmt-clicker.excluded.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        descLines = this.font.split(Component.translatable("odmt-clicker.excluded.desc"), 300);
        panelW = 8 + COLS * CELL + 8;
        panelH = 20 + ROWS * CELL + 8;
        panelX = (this.width - panelW) / 2;
        int top = 24 + descLines.size() * 10 + 6;
        panelY = Math.max(top, (this.height - panelH - 28) / 2);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(this.width / 2 - 100, panelY + panelH + 6, 200, 20)
                .build());
    }

    private int cellX(int index) {
        return panelX + 8 + (index % COLS) * CELL;
    }

    private int cellY(int index) {
        return panelY + 20 + (index / COLS) * CELL;
    }

    private static ItemStack icon(String id) {
        Block block = ClickerConfig.blockById(id);
        return new ItemStack(block.asItem());
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        GuiDraw.panel(g, panelX, panelY, panelW, panelH);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);
        for (int i = 0; i < descLines.size(); i++) {
            g.drawCenteredString(this.font, descLines.get(i), this.width / 2, 24 + i * 10, 0xFFA0A0A0);
        }
        g.drawString(this.font, Component.translatable("odmt-clicker.excluded.hint"), panelX + 8, panelY + 7, TEXT, false);

        List<String> list = ClickerConfig.get().autoToolExcluded;
        ItemStack hoveredStack = null;
        boolean hoveredPlus = false;
        int total = Math.min(list.size() + 1, COLS * ROWS);
        for (int i = 0; i < total; i++) {
            int x = cellX(i);
            int y = cellY(i);
            GuiDraw.slot(g, x, y);
            boolean over = GuiDraw.inside(mouseX, mouseY, x, y, CELL, CELL);
            if (i < list.size()) {
                ItemStack stack = icon(list.get(i));
                g.renderItem(stack, x + 1, y + 1);
                if (over) hoveredStack = stack;
            } else {
                g.drawCenteredString(this.font, "+", x + 9, y + 5, 0xFF55FF55);
                if (over) hoveredPlus = true;
            }
            if (over) GuiDraw.hover(g, x, y);
        }

        if (hoveredStack != null) {
            g.setTooltipForNextFrame(this.font, List.of(hoveredStack.getHoverName(),
                    Component.translatable("odmt-clicker.excluded.remove")), Optional.empty(), mouseX, mouseY);
        } else if (hoveredPlus) {
            g.setTooltipForNextFrame(this.font, List.of(Component.translatable("odmt-clicker.excluded.add")),
                    Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.button() != 0) return false;
        ClickerConfig cfg = ClickerConfig.get();
        List<String> list = cfg.autoToolExcluded;
        int total = Math.min(list.size() + 1, COLS * ROWS);
        for (int i = 0; i < total; i++) {
            if (!GuiDraw.inside(event.x(), event.y(), cellX(i), cellY(i), CELL, CELL)) continue;
            GuiDraw.clickSound();
            if (i < list.size()) {
                list.remove(i);
            } else {
                this.minecraft.setScreen(new BlockPickerScreen(this, cfg::addExcluded));
            }
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        ClickerConfig.save();
        this.minecraft.setScreen(parent);
    }
}
