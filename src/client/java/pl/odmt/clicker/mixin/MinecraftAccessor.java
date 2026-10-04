package pl.odmt.clicker.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Claude napisał tak prosze państwa: Vanilla blokuje atak na pol sekundy po kliknieciu w powietrze,
 * dopoki nie puscisz przycisku. Przy autoclicku przycisk jest trzymany,
 * wiec resetujemy ten licznik tak, jakbys normalnie puszczal przycisk. Totalnie zajebiście prosze państwa.
 */
@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Accessor("missTime")
    void odmtclicker$setMissTime(int ticks);
}
