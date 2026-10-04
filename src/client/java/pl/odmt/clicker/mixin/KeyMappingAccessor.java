package pl.odmt.clicker.mixin;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Daje dostep do licznika klikniec klawisza ataku.
 * Dzieki temu widzimy Twoje prawdziwe kliki (wykrywanie AUTO)
 * i mozemy podmienic je na kliki autoclickera.
 */
@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor("clickCount")
    int odmtclicker$getClickCount();

    @Accessor("clickCount")
    void odmtclicker$setClickCount(int count);
}
