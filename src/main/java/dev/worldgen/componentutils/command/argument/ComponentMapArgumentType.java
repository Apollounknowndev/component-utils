package dev.worldgen.componentutils.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.command.argument.ItemStringReader;
import net.minecraft.component.Component;
import net.minecraft.component.ComponentMap;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

public class ComponentMapArgumentType implements ArgumentType<ComponentMap> {
    private final ComponentMapStringReader reader;

    public ComponentMapArgumentType(CommandRegistryAccess registryAccess) {
        this.reader = new ComponentMapStringReader(registryAccess);
    }

    public static ComponentMapArgumentType create(CommandRegistryAccess registryAccess) {
        return new ComponentMapArgumentType(registryAccess);
    }

    public ComponentMap parse(StringReader stringReader) throws CommandSyntaxException {
        return this.reader.consume(stringReader);
    }

    public static <S> ComponentMap getComponentMapArgument(CommandContext<S> context, String name) {
        return context.getArgument(name, ComponentMap.class);
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return this.reader.getSuggestions(builder);
    }

}
