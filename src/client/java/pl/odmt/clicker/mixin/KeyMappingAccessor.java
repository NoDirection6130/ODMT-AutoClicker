package pl.odmt.clicker.mixin;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Podmienia klik na autoclick mapuje
 */
@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor("clickCount")
    int odmtclicker$getClickCount();

    @Accessor("clickCount")
    void odmtclicker$setClickCount(int count);
}
