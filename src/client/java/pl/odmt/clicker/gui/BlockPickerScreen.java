package pl.odmt.clicker.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import static pl.odmt.clicker.gui.GuiDraw.CELL;
import static pl.odmt.clicker.gui.GuiDraw.TEXT;

/** Wybor bloku jak w Creative: wyszukiwarka i siatka wszystkich blokow z gry. */
public class BlockPickerScreen extends Screen {
    private static final int COLS = 9;
    private static final int ROWS = 6;
    private static final int SCROLL_W = 12;

    private record Entry(Block block, ItemStack stack, String name, String id) {}

    private final Screen parent;
    private final Consumer<Block> onPick;
    private final List<Entry> all = new ArrayList<>();
    private List<Entry> filtered = List.of();
    private EditBox search;
    private int scroll;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int gridX;
    private int gridY;

    public BlockPickerScreen(Screen parent, Consumer<Block> onPick) {
        super(Component.translatable("odmt-clicker.picker.title"));
        this.parent = parent;
        this.onPick = onPick;
    }

    @Override
    protected void init() {
        if (all.isEmpty()) {
            Set<Block> seen = new HashSet<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (!(item instanceof BlockItem blockItem)) continue;
                Block block = blockItem.getBlock();
                if (!seen.add(block)) continue;
                ItemStack stack = new ItemStack(item);
                all.add(new Entry(block, stack,
                        stack.getHoverName().getString().toLowerCase(Locale.ROOT),
                        BuiltInRegistries.BLOCK.getKey(block).getPath()));
            }
        }

        panelW = 8 + COLS * CELL + 4 + SCROLL_W + 8;
        panelH = 20 + 16 + 4 + ROWS * CELL + 8;
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH - 28) / 2;
        gridX = panelX + 8;
        gridY = panelY + 20 + 16 + 4;

        String previous = search != null ? search.getValue() : "";
        search = new EditBox(this.font, panelX + 8, panelY + 20, COLS * CELL + 4 + SCROLL_W, 16,
                Component.translatable("odmt-clicker.picker.search"));
        search.setHint(Component.translatable("odmt-clicker.picker.search").withStyle(ChatFormatting.GRAY));
        search.setValue(previous);
        search.setResponder(text -> {
            scroll = 0;
            refilter();
        });
        addRenderableWidget(search);
        setInitialFocus(search);

        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                .bounds(this.width / 2 - 100, panelY + panelH + 6, 200, 20)
                .build());

        refilter();
    }

    private void refilter() {
        String q = search.getValue().trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            filtered = all;
        } else {
            List<Entry> out = new ArrayList<>();
            for (Entry e : all) {
                if (e.name().contains(q) || e.id().contains(q)) out.add(e);
            }
            filtered = out;
        }
        scroll = Math.min(scroll, maxScroll());
    }

    private int maxScroll() {
        int rows = (filtered.size() + COLS - 1) / COLS;
        return Math.max(0, rows - ROWS);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        GuiDraw.panel(g, panelX, panelY, panelW, panelH);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawString(this.font, this.title, panelX + 8, panelY + 7, TEXT, false);
        Component count = Component.translatable("odmt-clicker.picker.count", filtered.size());
        g.drawString(this.font, count, panelX + panelW - 8 - this.font.width(count), panelY + 7, 0xFF707070, false);

        ItemStack hovered = null;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int x = gridX + col * CELL;
                int y = gridY + row * CELL;
                GuiDraw.slot(g, x, y);
                int idx = (scroll + row) * COLS + col;
                if (idx >= filtered.size()) continue;
                ItemStack stack = filtered.get(idx).stack();
                g.renderItem(stack, x + 1, y + 1);
                if (GuiDraw.inside(mouseX, mouseY, x, y, CELL, CELL)) {
                    GuiDraw.hover(g, x, y);
                    hovered = stack;
                }
            }
        }

        int trackX = gridX + COLS * CELL + 4;
        int trackH = ROWS * CELL;
        g.fill(trackX, gridY, trackX + SCROLL_W, gridY + trackH, 0xFF8B8B8B);
        g.fill(trackX, gridY, trackX + SCROLL_W - 1, gridY + 1, 0xFF373737);
        g.fill(trackX, gridY, trackX + 1, gridY + trackH - 1, 0xFF373737);
        int thumbH = 15;
        int max = maxScroll();
        int thumbY = gridY + 1 + (max == 0 ? 0 : scroll * (trackH - thumbH - 2) / max);
        g.fill(trackX + 1, thumbY, trackX + SCROLL_W - 1, thumbY + thumbH, 0xFF555555);
        g.fill(trackX + 1, thumbY, trackX + SCROLL_W - 2, thumbY + thumbH - 1, max == 0 ? 0xFFA0A0A0 : 0xFFE0E0E0);

        if (hovered != null) {
            g.setTooltipForNextFrame(this.font, hovered, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.button() != 0) return false;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (!GuiDraw.inside(event.x(), event.y(), gridX + col * CELL, gridY + row * CELL, CELL, CELL)) continue;
                int idx = (scroll + row) * COLS + col;
                if (idx >= filtered.size()) return false;
                GuiDraw.clickSound();
                onPick.accept(filtered.get(idx).block());
                this.minecraft.setScreen(parent);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
