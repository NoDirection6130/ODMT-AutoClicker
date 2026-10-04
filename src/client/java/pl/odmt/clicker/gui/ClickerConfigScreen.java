package pl.odmt.clicker.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import pl.odmt.clicker.OdmtClickerClient;
import pl.odmt.clicker.config.ClickerConfig;
import pl.odmt.clicker.logic.ClickEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Vanilla type shiit. */
public class ClickerConfigScreen extends Screen {
    private static final int COL_WIDTH = 150;
    private static final int GAP = 10;
    private static final int ROW_STEP = 24;
    private static final int TOP = 36;

    private final Screen parent;
    private final List<Runnable> refreshers = new ArrayList<>();
    private boolean listeningForKey = false;

    private Button bindButton;
    private Button holdButton;
    private Button returnButton;
    private Button protectButton;
    private Button excludedButton;
    private IntSlider minSlider;
    private IntSlider maxSlider;
    private IntSlider triggerSlider;
    private IntSlider windowSlider;
    private IntSlider timeoutSlider;

    public ClickerConfigScreen(Screen parent) {
        super(Component.translatable("odmt-clicker.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        refreshers.clear();
        ClickerConfig cfg = ClickerConfig.get();

        int left = this.width / 2 - COL_WIDTH - GAP / 2;
        int right = this.width / 2 + GAP / 2;
        int y = TOP;

        // Rzad 1: tryb + klawisz
        Button modeButton = addRenderableWidget(Button.builder(modeText(cfg), b -> {
                    cfg.mode = cfg.mode == ClickerConfig.Mode.TOGGLE ? ClickerConfig.Mode.AUTO : ClickerConfig.Mode.TOGGLE;
                    listeningForKey = false;
                    ClickEngine.onModeChanged();
                    refresh();
                })
                .bounds(left, y, COL_WIDTH, 20)
                .tooltip(Tooltip.create(Component.translatable("odmt-clicker.config.mode.tooltip")))
                .build());
        refreshers.add(() -> modeButton.setMessage(modeText(cfg)));

        bindButton = addRenderableWidget(Button.builder(bindText(), b -> {
                    listeningForKey = true;
                    refresh();
                })
                .bounds(right, y, COL_WIDTH, 20)
                .tooltip(Tooltip.create(Component.translatable("odmt-clicker.config.key.tooltip")))
                .build());
        refreshers.add(() -> bindButton.setMessage(bindText()));

        // Rzad 2: zakres CPS
        y += ROW_STEP;
        minSlider = addRenderableWidget(new IntSlider(left, y, COL_WIDTH, 20, "odmt-clicker.config.min_cps",
                ClickerConfig.CPS_MIN, ClickerConfig.CPS_MAX, 1, "",
                () -> cfg.minCps,
                v -> {
                    cfg.minCps = v;
                    if (cfg.maxCps < v) {
                        cfg.maxCps = v;
                        maxSlider.syncFromConfig();
                    }
                }));
        minSlider.setTooltip(Tooltip.create(Component.translatable("odmt-clicker.config.cps.tooltip")));

        maxSlider = addRenderableWidget(new IntSlider(right, y, COL_WIDTH, 20, "odmt-clicker.config.max_cps",
                ClickerConfig.CPS_MIN, ClickerConfig.CPS_MAX, 1, "",
                () -> cfg.maxCps,
                v -> {
                    cfg.maxCps = v;
                    if (cfg.minCps > v) {
                        cfg.minCps = v;
                        minSlider.syncFromConfig();
                    }
                }));
        maxSlider.setTooltip(Tooltip.create(Component.translatable("odmt-clicker.config.cps.tooltip")));

        // Rzad 3: trzymanie LPM + HUD
        y += ROW_STEP;
        holdButton = addToggle(left, y, "odmt-clicker.config.hold",
                () -> cfg.requireHold, v -> cfg.requireHold = v);
        addToggle(right, y, "odmt-clicker.config.hud",
                () -> cfg.showHud, v -> cfg.showHud = v);

        // Rzad 4: tryb AUTO - kliki startu + okno
        y += ROW_STEP;
        triggerSlider = addRenderableWidget(new IntSlider(left, y, COL_WIDTH, 20, "odmt-clicker.config.trigger",
                2, 5, 1, "",
                () -> cfg.autoTriggerClicks, v -> cfg.autoTriggerClicks = v));
        triggerSlider.setTooltip(Tooltip.create(Component.translatable("odmt-clicker.config.trigger.tooltip")));

        windowSlider = addRenderableWidget(new IntSlider(right, y, COL_WIDTH, 20, "odmt-clicker.config.window",
                100, 800, 10, " ms",
                () -> cfg.autoWindowMs, v -> cfg.autoWindowMs = v));
        windowSlider.setTooltip(Tooltip.create(Component.translatable("odmt-clicker.config.window.tooltip")));

        // Rzad 5: tryb AUTO - timeout + auto-tool
        y += ROW_STEP;
        timeoutSlider = addRenderableWidget(new IntSlider(left, y, COL_WIDTH, 20, "odmt-clicker.config.timeout",
                200, 2000, 50, " ms",
                () -> cfg.autoTimeoutMs, v -> cfg.autoTimeoutMs = v));
        timeoutSlider.setTooltip(Tooltip.create(Component.translatable("odmt-clicker.config.timeout.tooltip")));

        addToggle(right, y, "odmt-clicker.config.auto_tool",
                () -> cfg.autoTool, v -> cfg.autoTool = v);

        // Rzad 6: opcje auto-toola
        y += ROW_STEP;
        returnButton = addToggle(left, y, "odmt-clicker.config.return_slot",
                () -> cfg.autoToolReturn, v -> cfg.autoToolReturn = v);
        protectButton = addToggle(right, y, "odmt-clicker.config.protect",
                () -> cfg.protectTools, v -> cfg.protectTools = v);

        // Gotowe
        // Rzad 7: wykluczone bloki auto-toola
        y += ROW_STEP;
        excludedButton = addRenderableWidget(Button.builder(
                        Component.translatable("odmt-clicker.config.excluded", cfg.autoToolExcluded.size()),
                        b -> this.minecraft.setScreen(new ExcludedBlocksScreen(this)))
                .bounds(this.width / 2 - 100, y, 200, 20)
                .tooltip(Tooltip.create(Component.translatable("odmt-clicker.config.excluded.tooltip")))
                .build());

        y += ROW_STEP + 8;
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(this.width / 2 - 100, y, 200, 20)
                .build());

        refresh();
    }

    private Button addToggle(int x, int y, String key, BooleanSupplier getter, Consumer<Boolean> setter) {
        Button button = addRenderableWidget(Button.builder(onOffText(key, getter.getAsBoolean()), b -> {
                    setter.accept(!getter.getAsBoolean());
                    refresh();
                })
                .bounds(x, y, COL_WIDTH, 20)
                .tooltip(Tooltip.create(Component.translatable(key + ".tooltip")))
                .build());
        refreshers.add(() -> button.setMessage(onOffText(key, getter.getAsBoolean())));
        return button;
    }

    /** Refresh + szarzaenie przycisków. */
    private void refresh() {
        ClickerConfig cfg = ClickerConfig.get();
        boolean auto = cfg.mode == ClickerConfig.Mode.AUTO;

        for (Runnable r : refreshers) r.run();

        bindButton.active = !auto;
        holdButton.active = !auto;
        triggerSlider.active = auto;
        windowSlider.active = auto;
        timeoutSlider.active = auto;
        returnButton.active = cfg.autoTool;
        protectButton.active = cfg.autoTool;
        excludedButton.active = cfg.autoTool;
    }

    private static Component modeText(ClickerConfig cfg) {
        String modeKey = cfg.mode == ClickerConfig.Mode.TOGGLE ? "odmt-clicker.mode.toggle" : "odmt-clicker.mode.auto";
        return Component.translatable("odmt-clicker.config.mode", Component.translatable(modeKey));
    }

    private Component bindText() {
        Component keyName = OdmtClickerClient.toggleKey.getTranslatedKeyMessage();
        if (listeningForKey) {
            Component waiting = Component.literal("> ")
                    .append(keyName.copy().withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE))
                    .append(" <")
                    .withStyle(ChatFormatting.YELLOW);
            return Component.translatable("odmt-clicker.config.key", waiting);
        }
        return Component.translatable("odmt-clicker.config.key", keyName);
    }

    private static Component onOffText(String key, boolean value) {
        return Component.translatable(key, value ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (listeningForKey) {
            int code = event.key();
            InputConstants.Key key = code == GLFW.GLFW_KEY_ESCAPE
                    ? InputConstants.UNKNOWN
                    : InputConstants.Type.KEYSYM.getOrCreate(code);
            OdmtClickerClient.toggleKey.setKey(key);
            KeyMapping.resetMapping();
            if (this.minecraft != null) this.minecraft.options.save();
            listeningForKey = false;
            refresh();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font, Component.translatable("odmt-clicker.config.subtitle"),
                this.width / 2, 24, 0xFFA0A0A0);
    }

    @Override
    public void onClose() {
        ClickerConfig.save();
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }
}
