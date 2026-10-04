package pl.odmt.clicker.logic;

import net.minecraft.client.Minecraft;
import pl.odmt.clicker.config.ClickerConfig;
import pl.odmt.clicker.mixin.KeyMappingAccessor;
import pl.odmt.clicker.mixin.MinecraftAccessor;

import java.util.ArrayDeque;
import java.util.concurrent.ThreadLocalRandom;

/**
 * ALL
 * !!!!!zanim vanilla przetworzy kliki ataku.
 */
public final class ClickEngine {
    private static final long OFF_VISIBLE_MS = 1500L;
    private static final long OFF_FADE_START_MS = 1000L;
    private static final int MAX_CLICKS_PER_TICK = 4;

    private static boolean toggledOn = false;   // tryb Klawisz: czy wlaczony
    private static boolean autoActive = false;  // tryb AUTO: czy wykryto szybkie klikanie
    private static boolean firing = false;      // czy w tej chwili klikamy
    private static long nextClickAt = 0L;
    private static long lastManualClick = 0L;
    private static long offShownAt = 0L;
    private static final ArrayDeque<Long> recentManualClicks = new ArrayDeque<>();

    private ClickEngine() {}

    public static long now() {
        return System.nanoTime() / 1_000_000L;
    }

    public static void onTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            reset();
            return;
        }

        ClickerConfig cfg = ClickerConfig.get();
        long now = now();
        boolean inGame = mc.screen == null;
        KeyMappingAccessor attack = (KeyMappingAccessor) (Object) mc.options.keyAttack;

        // Ile razy NAPRAWDE kliknales lewym od ostatniego ticka <------ LEWY LEWY LEWY
        int manualClicks = inGame ? attack.odmtclicker$getClickCount() : 0;

        if (cfg.mode == ClickerConfig.Mode.AUTO) {
            updateAutoDetection(cfg, now, manualClicks, inGame);
        } else {
            autoActive = false;
            recentManualClicks.clear();
        }

        boolean wantsToClick;
        if (cfg.mode == ClickerConfig.Mode.TOGGLE) {
            wantsToClick = toggledOn && (!cfg.requireHold || mc.options.keyAttack.isDown());
        } else {
            wantsToClick = autoActive;
        }
        boolean shouldFire = inGame && !mc.player.isSpectator() && wantsToClick;

        if (!shouldFire) {
            firing = false;
            return;
        }

        if (!firing) {
            firing = true;
            nextClickAt = now; // pierwszy klik od razu
        }

        int clicks = 0;
        while (now >= nextClickAt && clicks < MAX_CLICKS_PER_TICK) {
            clicks++;
            nextClickAt += nextIntervalMs(cfg);
        }
        if (nextClickAt < now) {
            // gra lagowala - nie nadrabiamy serii klikow naraz
            nextClickAt = now + nextIntervalMs(cfg);
        }

        // Podmieniamy  kliki, zeby CPS trzymal sie ustawionego zakresu
        attack.odmtclicker$setClickCount(clicks);
        if (clicks > 0) {
            ((MinecraftAccessor) (Object) mc).odmtclicker$setMissTime(0);
        }
    }

    private static void updateAutoDetection(ClickerConfig cfg, long now, int manualClicks, boolean inGame) {
        if (!inGame) {
            autoActive = false;
            recentManualClicks.clear();
            return;
        }

        for (int i = 0; i < manualClicks; i++) {
            recentManualClicks.addLast(now);
        }
        if (manualClicks > 0) {
            lastManualClick = now;
        }
        while (!recentManualClicks.isEmpty() && now - recentManualClicks.peekFirst() > cfg.autoWindowMs) {
            recentManualClicks.pollFirst();
        }

        if (!autoActive && recentManualClicks.size() >= cfg.autoTriggerClicks) {
            autoActive = true;
        }
        if (autoActive && now - lastManualClick > cfg.autoTimeoutMs) {
            autoActive = false;
            recentManualClicks.clear();
        }
    }

    /** Losowy odstep miedzy klikami - kazdy klik ma inny CPS z zakresu min-max. */
    private static long nextIntervalMs(ClickerConfig cfg) {
        double cps = cfg.minCps + ThreadLocalRandom.current().nextDouble() * (cfg.maxCps - cfg.minCps);
        if (cps < 1.0) cps = 1.0;
        return Math.max(1L, Math.round(1000.0 / cps));
    }

    // --- Sterowanie ---

    public static void onToggleKey() {
        if (ClickerConfig.get().mode != ClickerConfig.Mode.TOGGLE) return;
        toggledOn = !toggledOn;
        offShownAt = toggledOn ? 0L : now();
    }

    public static void onModeChanged() {
        reset();
    }

    public static void reset() {
        toggledOn = false;
        autoActive = false;
        firing = false;
        offShownAt = 0L;
        recentManualClicks.clear();
    }

    // --- Dla HUD i auto-toola ---

    public static boolean isFiring() {
        return firing;
    }

    /** Czy pokazac "Auto: ON". */
    public static boolean isOn() {
        return ClickerConfig.get().mode == ClickerConfig.Mode.TOGGLE ? toggledOn : autoActive;
    }

    /** Przezroczystosc napisu "Auto: OFF" (0 = niewidoczny). */
    public static float offAlpha() {
        if (offShownAt == 0L) return 0f;
        long since = now() - offShownAt;
        if (since >= OFF_VISIBLE_MS) {
            offShownAt = 0L;
            return 0f;
        }
        if (since < OFF_FADE_START_MS) return 1f;
        return 1f - (float) (since - OFF_FADE_START_MS) / (OFF_VISIBLE_MS - OFF_FADE_START_MS);
    }
}
