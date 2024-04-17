package dev.worldgen.componentutils.command;

import com.mojang.serialization.DataResult;
import dev.worldgen.componentutils.ComponentUtils;
import net.minecraft.component.Component;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentMapImpl;
import net.minecraft.component.DataComponentType;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class ComponentCommandUtils {

    // GET
    public static int getSingleComponent(ServerCommandSource source, RegistryOps<NbtElement> ops, ComponentMapImpl components, ComponentEntryType entryType, RegistryEntry.Reference<DataComponentType<?>> type) {
        String typeKey = type.registryKey().getValue().toString();
        @Nullable Optional<?> value = entryType.getEntry(components, type.value());

        if (value != null && value.isPresent()) {
            NbtElement nbt = encodeComponentToNbt(type.value(), value.get(), ops);

            MutableText text = Text.empty();

            text.append(Text.translatable("commands.component_utils.get.success_single", typeKey, NbtHelper.toPrettyPrintedText(nbt)));
            text.append(ScreenTexts.SPACE);
            text.append(Text.translatable("component_utils.copy").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, nbt.asString()))));

            source.sendFeedback(() -> text, false);
            return 1;
        }

        source.sendError(Text.translatable("commands.component_utils.get.component_not_on_item", type.registryKey().getValue().toString(), entryType.getText()));
        return 0;
    }

    public static int getAllComponents(ServerCommandSource source, RegistryOps<NbtElement> ops, ComponentMapImpl components, ComponentEntryType entryType) {
        MutableText componentText = Text.empty();
        componentText.append("[");
        List<Component<?>> componentEntries = components.stream().toList();
        boolean firstCommaSkipped = false;
        for (Component<?> component : componentEntries) {
            @Nullable Optional<?> componentValue = entryType.getEntry(components, component.type());
            if (componentValue != null) {
                if (!firstCommaSkipped) {
                    firstCommaSkipped = true;
                } else {
                    componentText.append(",");
                }

                componentText.append(componentValue.isEmpty() ? "!": "");
                componentText.append(Util.registryValueToString(Registries.DATA_COMPONENT_TYPE, component.type()));
                componentText.append("=");
                componentText.append(componentValue.isEmpty() ? Text.literal("{}") : NbtHelper.toPrettyPrintedText(component.encode(ops).getOrThrow()));
            }
        }
        componentText.append("]");

        MutableText text = Text.empty();
        text.append(Text.translatable("commands.component_utils.get.success_all", componentText));
        text.append(ScreenTexts.SPACE);
        text.append(Text.translatable("component_utils.copy").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, componentText.getString()))));

        source.sendFeedback(() -> text, false);
        return 1;
    }

    private static <T> NbtElement encodeComponentToNbt(DataComponentType<T> type, Object value, RegistryOps<NbtElement> ops) {
        return type.getCodecOrThrow().encodeStart(ops, (T)value).result().get();
    }

    // REMOVE
    public static int removeComponent(ServerCommandSource source, ComponentMap defaultComponents, ComponentMapImpl currentComponents, RegistryEntry.Reference<DataComponentType<?>> type) {
        String typeKey = type.registryKey().getValue().toString();
        @Nullable Optional<?> changedValue = currentComponents.getChanges().get(type.value());

        // If changedValue is not null, it's either explicitly added or explicitly removed
        if (changedValue != null) {
            if (changedValue.isPresent()) {
                return resetSingleChange(source, currentComponents, type);
            } else {
                source.sendError(Text.translatable("commands.component_utils.remove.component_already_removed", typeKey));
                return 0;
            }
        }

        // If the component isn't in base components *or* component changes, throw an error
        if (!defaultComponents.contains(type.value())) {
            source.sendError(Text.translatable("commands.component_utils.remove.component_not_present", typeKey));
            return 0;
        }

        currentComponents.remove(type.value());
        source.sendFeedback(() -> Text.translatable("commands.component_utils.remove.success", typeKey), false);
        return 1;
    }


    // RESET

    public static int resetSingleChange(ServerCommandSource source, ComponentMapImpl currentComponents, RegistryEntry.Reference<DataComponentType<?>> type) {
        String typeKey = type.registryKey().getValue().toString();
        ComponentChanges changes = currentComponents.getChanges();

        // If component isn't already changed from the base, we can't reset it
        @Nullable Optional<?> changeValue = changes.get(type.value());
        if (changeValue == null) {
            source.sendError(Text.translatable("commands.component_utils.clear.single.cant_clear", typeKey));
            return 0;
        }

        // Create a new ComponentChanges instance without the provided data component type and apply it to the current component map
        ComponentChanges updatedChanges = changes.withRemovedIf(componentType -> componentType == type.value());
        currentComponents.setChanges(updatedChanges);

        source.sendFeedback(() -> Text.translatable("commands.component_utils.clear.single.success", typeKey), false);
        return 1;
    }

    public static int resetAllChange(ServerCommandSource source, ComponentMapImpl currentComponents) {
        if (currentComponents.getChanges().isEmpty()) {
            source.sendError(Text.translatable("commands.component_utils.clear.all.cant_clear"));
            return 0;
        }

        currentComponents.setChanges(ComponentChanges.EMPTY);
        source.sendFeedback(() -> Text.translatable("commands.component_utils.clear.all.success"), false);
        return 1;
    }
}
