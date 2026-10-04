package pl.odmt.clicker.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Vanillowy suwak Minecrafta, ktory operuje na liczbach calkowitych. */
public class IntSlider extends AbstractSliderButton {
    private final String translationKey;
    private final int min;
    private final int max;
    private final int step;
    private final String suffix;
    private final IntSupplier getter;
    private final IntConsumer setter;

    public IntSlider(int x, int y, int width, int height, String translationKey,
                     int min, int max, int step, String suffix,
                     IntSupplier getter, IntConsumer setter) {
        super(x, y, width, height, Component.empty(), toSliderValue(getter.getAsInt(), min, max));
        this.translationKey = translationKey;
        this.min = min;
        this.max = max;
        this.step = Math.max(1, step);
        this.suffix = suffix;
        this.getter = getter;
        this.setter = setter;
        updateMessage();
    }

    private static double toSliderValue(int v, int min, int max) {
        if (max <= min) return 0.0;
        double d = (v - min) / (double) (max - min);
        return Math.max(0.0, Math.min(1.0, d));
    }

    public int getIntValue() {
        int raw = min + (int) Math.round(this.value * (max - min));
        int snapped = Math.round(raw / (float) step) * step;
        return Math.max(min, Math.min(max, snapped));
    }

    /** Odswieza suwak po zmianie wartosci z zewnatrz (np. min CPS przepchnal max CPS). */
    public void syncFromConfig() {
        this.value = toSliderValue(getter.getAsInt(), min, max);
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        if (translationKey == null) return; // wywolanie z konstruktora rodzica
        setMessage(Component.translatable(translationKey, getIntValue() + suffix));
    }

    @Override
    protected void applyValue() {
        setter.accept(getIntValue());
    }
}
