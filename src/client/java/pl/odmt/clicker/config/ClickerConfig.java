package pl.odmt.clicker.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import pl.odmt.clicker.OdmtClickerClient;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ClickerConfig {
    public enum Mode { TOGGLE, AUTO }

    // --- Ustawienia ---
    public Mode mode = Mode.TOGGLE;
    public int minCps = 10;
    public int maxCps = 14;
    public boolean requireHold = true;
    public boolean showHud = true;

    public int autoTriggerClicks = 2;
    public int autoWindowMs = 300;
    public int autoTimeoutMs = 500;

    public boolean autoTool = true;
    public boolean autoToolReturn = true;
    public boolean protectTools = true;
    /** Bloki, na ktorych auto-tool nic nie robi. */
    public List<String> autoToolExcluded = new ArrayList<>();

    public static final int MAX_EXCLUDED = 54;

    // --- Limity ---
    public static final int CPS_MIN = 1;
    public static final int CPS_MAX = 20;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("odmt-clicker.json");
    private static ClickerConfig instance;

    public static ClickerConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        ClickerConfig loaded = null;
        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
                loaded = GSON.fromJson(reader, ClickerConfig.class);
            } catch (Exception e) {
                OdmtClickerClient.LOGGER.warn("Nie udalo sie wczytac configu, uzywam domyslnego", e);
            }
        }
        instance = loaded != null ? loaded : new ClickerConfig();
        instance.sanitize();
    }

    public static void save() {
        if (instance == null) return;
        instance.sanitize();
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(instance, writer);
            }
        } catch (Exception e) {
            OdmtClickerClient.LOGGER.warn("Nie udalo sie zapisac configu", e);
        }
    }

    public void sanitize() {
        if (autoToolExcluded == null) autoToolExcluded = new ArrayList<>();
        autoToolExcluded.removeIf(id -> id == null || blockById(id) == Blocks.AIR);
        while (autoToolExcluded.size() > MAX_EXCLUDED) autoToolExcluded.remove(autoToolExcluded.size() - 1);
        if (mode == null) mode = Mode.TOGGLE;
        minCps = clamp(minCps, CPS_MIN, CPS_MAX);
        maxCps = clamp(maxCps, CPS_MIN, CPS_MAX);
        if (minCps > maxCps) {
            int tmp = minCps;
            minCps = maxCps;
            maxCps = tmp;
        }
        autoTriggerClicks = clamp(autoTriggerClicks, 2, 5);
        autoWindowMs = clamp(autoWindowMs, 100, 800);
        autoTimeoutMs = clamp(autoTimeoutMs, 200, 2000);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    // --- Wykluczone bloki auto-toola ---

    public static String blockId(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    public static Block blockById(String id) {
        Identifier key = Identifier.tryParse(id);
        return key == null ? Blocks.AIR : BuiltInRegistries.BLOCK.getValue(key);
    }

    public boolean isExcluded(Block block) {
        return !autoToolExcluded.isEmpty() && autoToolExcluded.contains(blockId(block));
    }

    public void addExcluded(Block block) {
        String id = blockId(block);
        if (!autoToolExcluded.contains(id) && autoToolExcluded.size() < MAX_EXCLUDED) autoToolExcluded.add(id);
    }
}
