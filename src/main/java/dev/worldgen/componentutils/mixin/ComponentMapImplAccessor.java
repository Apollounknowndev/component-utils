package dev.worldgen.componentutils.mixin;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentMapImpl;
import net.minecraft.component.DataComponentType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

@Mixin(ComponentMapImpl.class)
public interface ComponentMapImplAccessor {
    @Accessor("changedComponents")
    Reference2ObjectMap<DataComponentType<?>, Optional<?>> changedComponents();

    @Accessor("baseComponents")
    ComponentMap baseComponents();
}
