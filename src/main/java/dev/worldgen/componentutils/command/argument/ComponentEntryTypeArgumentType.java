package dev.worldgen.componentutils.command.argument;

import com.mojang.brigadier.context.CommandContext;
import dev.worldgen.componentutils.command.ComponentEntryType;
import net.minecraft.command.argument.EnumArgumentType;
import net.minecraft.server.command.ServerCommandSource;

public class ComponentEntryTypeArgumentType extends EnumArgumentType<ComponentEntryType> {
    public ComponentEntryTypeArgumentType() {
        super(ComponentEntryType.CODEC, ComponentEntryType::values);
    }

    public static EnumArgumentType<ComponentEntryType> componentEntryType() {
        return new ComponentEntryTypeArgumentType();
    }

    public static ComponentEntryType getComponentEntryType(CommandContext<ServerCommandSource> context, String id) {
        return context.getArgument(id, ComponentEntryType.class);
    }
}
