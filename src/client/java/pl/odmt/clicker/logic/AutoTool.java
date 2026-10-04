package pl.odmt.clicker.logic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import pl.odmt.clicker.config.ClickerConfig;

/**
 * Gdy trzymasz LPM na bloku, wybiera z paska najlepsze narzedzie.
 * Server can't see this shit.
 */
public final class AutoTool {
    private static final int RETURN_DELAY_TICKS = 5;
    private static final int LOW_DURABILITY = 3;

    private static int previousSlot = -1; // slot sprzed auto-toola
    private static int ourSlot = -1;      // slot, ktory ustawil auto-tool
    private static int idleTicks = 0;

    private AutoTool() {}

    public static void onTick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            previousSlot = -1;
            ourSlot = -1;
            return;
        }

        ClickerConfig cfg = ClickerConfig.get();
        Inventory inv = player.getInventory();
        BlockPos target = findMiningTarget(mc, cfg);

        if (target != null) {
            idleTicks = 0;
            BlockState state = mc.level.getBlockState(target);
            if (cfg.isExcluded(state.getBlock())) return; // blok z listy wykluczen
            float hardness = state.getDestroySpeed(mc.level, target);
            if (hardness <= 0.0f) return; // niszczy sie od razu albo jest niezniszczalny

            int current = inv.getSelectedSlot();
            if (ourSlot != -1 && current != ourSlot) {
                // jak zmieni ktoś  slota samemu w trakcie kopania - zapominanie
                previousSlot = -1;
                ourSlot = -1;
            }

            int best = findBestSlot(inv, state, current, cfg.protectTools);
            if (best != current) {
                if (previousSlot == -1) previousSlot = current;
                inv.setSelectedSlot(best);
                ourSlot = best;
            }
        } else if (previousSlot != -1) {
            idleTicks++;
            if (idleTicks >= RETURN_DELAY_TICKS) {
                if (cfg.autoToolReturn && inv.getSelectedSlot() == ourSlot) {
                    inv.setSelectedSlot(previousSlot);
                }
                previousSlot = -1;
                ourSlot = -1;
                idleTicks = 0;
            }
        }
    }

    private static BlockPos findMiningTarget(Minecraft mc, ClickerConfig cfg) {
        if (!cfg.autoTool || mc.screen != null || mc.gameMode == null) return null;
        LocalPlayer player = mc.player;
        if (player.isSpectator() || player.isCreative()) return null;
        if (ClickEngine.isFiring()) return null; // nie zabieramy miecza w trakcie walki <----- 8====O
        if (!mc.options.keyAttack.isDown()) return null;

        if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            return hit.getBlockPos();
        }
        return null;
    }

    private static int findBestSlot(Inventory inv, BlockState state, int current, boolean protect) {
        int best = current;
        float bestScore = score(inv.getItem(current), state, protect);
        for (int slot = 0; slot < Inventory.SELECTION_SIZE; slot++) {
            float s = score(inv.getItem(slot), state, protect);
            if (s > bestScore + 0.01f) {
                bestScore = s;
                best = slot;
            }
        }
        return best;
    }

    private static float score(ItemStack stack, BlockState state, boolean protect) {
        if (protect && stack.isDamageableItem()
                && stack.getMaxDamage() - stack.getDamageValue() <= LOW_DURABILITY) {
            return -1f;
        }
        float speed = stack.getDestroySpeed(state);
        boolean drops = !state.requiresCorrectToolForDrops() || stack.isCorrectToolForDrops(state);
        // Najpierw: czy blok w ogole cos wydropi. Potem: predkosc kopania.
        return (drops ? 1000f : 0f) + speed;
    }
}
