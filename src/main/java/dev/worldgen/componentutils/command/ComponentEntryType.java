package dev.worldgen.componentutils.command;

import com.mojang.serialization.Codec;
import dev.worldgen.componentutils.mixin.ComponentMapImplAccessor;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentMapImpl;
import net.minecraft.component.DataComponentType;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public enum ComponentEntryType implements StringIdentifiable {
    EXPLICIT("explicit", false, true),
    IMPLICIT("implicit", true, false),
    ALL("all", true, true);

    public static final Codec<ComponentEntryType> CODEC = StringIdentifiable.createCodec(ComponentEntryType::values);

    private final String name;
    private final Text text;
    private final boolean implicit;
    private final boolean explicit;
    ComponentEntryType(String name, boolean implicit, boolean explicit) {
        this.name = name;
        this.text = Text.translatable("component_utils.entry_type_text."+name);
        this.implicit = implicit;
        this.explicit = explicit;
    }

    @Override
    public String asString() {
        return this.name;
    }

    @Nullable
    public <T> Optional<? extends T> getEntry(ComponentMapImpl components, DataComponentType<T> type) {
        T entry;
        if (this == EXPLICIT) {
            return components.getChanges().get(type);
        }

        else if (this == IMPLICIT) {
            // Get explicit entry to see if the implicit entry has been removed
            @Nullable Optional<? extends T> explicitEntry = components.getChanges().get(type);
            boolean explicitlyRemoved = explicitEntry != null && explicitEntry.isEmpty();

            entry = ((ComponentMapImplAccessor)components).baseComponents().get(type);
            return entry != null && !explicitlyRemoved ? Optional.of(entry) : null;
        }

        entry = components.get(type);
        return entry != null ? Optional.of(entry) : null;
    }

    public Text getText() {
        return this.text;
    }
}
