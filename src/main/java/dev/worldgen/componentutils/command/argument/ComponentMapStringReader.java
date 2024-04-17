package dev.worldgen.componentutils.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import net.minecraft.command.CommandSource;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class ComponentMapStringReader {
    static final DynamicCommandExceptionType UNKNOWN_COMPONENT_EXCEPTION = new DynamicCommandExceptionType((id) -> {
        return Text.stringifiedTranslatable("arguments.item.component.unknown", id);
    });
    static final Dynamic2CommandExceptionType MALFORMED_COMPONENT_EXCEPTION = new Dynamic2CommandExceptionType((type, error) -> {
        return Text.stringifiedTranslatable("arguments.item.component.malformed", type, error);
    });
    static final SimpleCommandExceptionType COMPONENT_EXPECTED_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("arguments.item.component.expected"));
    static final DynamicCommandExceptionType REPEATED_COMPONENT_EXCEPTION = new DynamicCommandExceptionType((type) -> {
        return Text.stringifiedTranslatable("arguments.item.component.repeated", type);
    });
    private static final DynamicCommandExceptionType MALFORMED_ITEM_EXCEPTION = new DynamicCommandExceptionType((error) -> {
        return Text.stringifiedTranslatable("arguments.item.malformed", error);
    });
    public static final char OPEN_SQUARE_BRACKET = '[';
    public static final char CLOSED_SQUARE_BRACKET = ']';
    public static final char COMMA = ',';
    public static final char EQUAL_SIGN = '=';
    static final Function<SuggestionsBuilder, CompletableFuture<Suggestions>> SUGGEST_DEFAULT = SuggestionsBuilder::buildFuture;
    final DynamicOps<NbtElement> nbtOps;

    public ComponentMapStringReader(RegistryWrapper.WrapperLookup registriesLookup) {
        this.nbtOps = registriesLookup.getOps(NbtOps.INSTANCE);
    }

    public ComponentMap consume(StringReader reader) throws CommandSyntaxException {
        final ComponentMap.Builder builder = ComponentMap.builder();
        this.consume(reader, new Callbacks() {

            public <T> void onComponent(DataComponentType<T> type, T value) {
                builder.add(type, value);
            }
        });
        ComponentMap componentMap = builder.build();
        validate(reader, componentMap);
        return componentMap;
    }

    private static void validate(StringReader reader, ComponentMap components) throws CommandSyntaxException {
        DataResult<Unit> dataResult = ItemStack.validateComponents(components);
        dataResult.getOrThrow((error) -> MALFORMED_ITEM_EXCEPTION.createWithContext(reader, error));
    }

    public void consume(StringReader reader, ComponentMapStringReader.Callbacks callbacks) throws CommandSyntaxException {
        int i = reader.getCursor();

        try {
            (new ComponentMapStringReader.Reader(reader, callbacks)).read();
        } catch (CommandSyntaxException var5) {
            reader.setCursor(i);
            throw var5;
        }
    }

    public CompletableFuture<Suggestions> getSuggestions(SuggestionsBuilder builder) {
        StringReader stringReader = new StringReader(builder.getInput());
        stringReader.setCursor(builder.getStart());
        ComponentMapStringReader.SuggestionCallbacks suggestionCallbacks = new ComponentMapStringReader.SuggestionCallbacks();
        ComponentMapStringReader.Reader reader = new ComponentMapStringReader.Reader(stringReader, suggestionCallbacks);

        try {
            reader.read();
        } catch (CommandSyntaxException ignored) {

        }

        return suggestionCallbacks.getSuggestions(builder, stringReader);
    }

    public interface Callbacks {
        default <T> void onComponent(DataComponentType<T> type, T value) {
        }

        default void setSuggestor(Function<SuggestionsBuilder, CompletableFuture<Suggestions>> suggestor) {
        }
    }

    private class Reader {
        private final StringReader reader;
        private final ComponentMapStringReader.Callbacks callbacks;

        Reader(final StringReader reader, final ComponentMapStringReader.Callbacks callbacks) {
            this.reader = reader;
            this.callbacks = callbacks;
        }

        public void read() throws CommandSyntaxException {
            this.callbacks.setSuggestor(this::suggestBracket);
            if (this.reader.canRead() && this.reader.peek() == OPEN_SQUARE_BRACKET) {
                this.callbacks.setSuggestor(ComponentMapStringReader.SUGGEST_DEFAULT);
                this.readComponents();
            }

        }

        private void readComponents() throws CommandSyntaxException {
            this.reader.expect(OPEN_SQUARE_BRACKET);
            this.callbacks.setSuggestor(this::suggestComponentType);
            Set<DataComponentType<?>> set = new ReferenceArraySet<>();

            while(this.reader.canRead() && this.reader.peek() != CLOSED_SQUARE_BRACKET) {
                this.reader.skipWhitespace();
                DataComponentType<?> dataComponentType = readComponentType(this.reader);
                if (!set.add(dataComponentType)) {
                    throw ComponentMapStringReader.REPEATED_COMPONENT_EXCEPTION.create(dataComponentType);
                }

                this.callbacks.setSuggestor(this::suggestEqual);
                this.reader.skipWhitespace();
                this.reader.expect(EQUAL_SIGN);
                this.callbacks.setSuggestor(ComponentMapStringReader.SUGGEST_DEFAULT);
                this.reader.skipWhitespace();
                this.readComponentValue(dataComponentType);
                this.reader.skipWhitespace();
                this.callbacks.setSuggestor(this::suggestEndOfComponent);
                if (!this.reader.canRead() || this.reader.peek() != COMMA) {
                    break;
                }

                this.reader.skip();
                this.reader.skipWhitespace();
                this.callbacks.setSuggestor(this::suggestComponentType);
                if (!this.reader.canRead()) {
                    throw ComponentMapStringReader.COMPONENT_EXPECTED_EXCEPTION.createWithContext(this.reader);
                }
            }

            this.reader.expect(CLOSED_SQUARE_BRACKET);
            this.callbacks.setSuggestor(ComponentMapStringReader.SUGGEST_DEFAULT);
        }

        public static DataComponentType<?> readComponentType(StringReader reader) throws CommandSyntaxException {
            if (!reader.canRead()) {
                throw ComponentMapStringReader.COMPONENT_EXPECTED_EXCEPTION.createWithContext(reader);
            } else {
                int i = reader.getCursor();
                Identifier identifier = Identifier.fromCommandInput(reader);
                DataComponentType<?> dataComponentType = Registries.DATA_COMPONENT_TYPE.get(identifier);
                if (dataComponentType != null && !dataComponentType.shouldSkipSerialization()) {
                    return dataComponentType;
                } else {
                    reader.setCursor(i);
                    throw ComponentMapStringReader.UNKNOWN_COMPONENT_EXCEPTION.createWithContext(reader, identifier);
                }
            }
        }

        private <T> void readComponentValue(DataComponentType<T> type) throws CommandSyntaxException {
            int i = this.reader.getCursor();
            NbtElement nbtElement = (new StringNbtReader(this.reader)).parseElement();
            DataResult<T> dataResult = type.getCodecOrThrow().parse(ComponentMapStringReader.this.nbtOps, nbtElement);
            this.callbacks.onComponent(type, dataResult.getOrThrow((error) -> {
                this.reader.setCursor(i);
                return ComponentMapStringReader.MALFORMED_COMPONENT_EXCEPTION.createWithContext(this.reader, type.toString(), error);
            }));
        }

        private CompletableFuture<Suggestions> suggestBracket(SuggestionsBuilder builder) {
            if (builder.getRemaining().isEmpty()) {
                builder.suggest(String.valueOf(OPEN_SQUARE_BRACKET));
            }

            return builder.buildFuture();
        }

        private CompletableFuture<Suggestions> suggestEndOfComponent(SuggestionsBuilder builder) {
            if (builder.getRemaining().isEmpty()) {
                builder.suggest(String.valueOf(COMMA));
                builder.suggest(String.valueOf(CLOSED_SQUARE_BRACKET));
            }

            return builder.buildFuture();
        }

        private CompletableFuture<Suggestions> suggestEqual(SuggestionsBuilder builder) {
            if (builder.getRemaining().isEmpty()) {
                builder.suggest(String.valueOf(EQUAL_SIGN));
            }

            return builder.buildFuture();
        }

        private CompletableFuture<Suggestions> suggestComponentType(SuggestionsBuilder builder) {
            String string = builder.getRemaining().toLowerCase(Locale.ROOT);
            CommandSource.forEachMatching(Registries.DATA_COMPONENT_TYPE.getEntrySet(), string, (entry) -> entry.getKey().getValue(), (entry) -> {
                DataComponentType<?> dataComponentType = entry.getValue();
                if (dataComponentType.getCodec() != null) {
                    Identifier identifier = entry.getKey().getValue();
                    builder.suggest(identifier.toString() + EQUAL_SIGN);
                }

            });
            return builder.buildFuture();
        }
    }

    private static class SuggestionCallbacks implements ComponentMapStringReader.Callbacks {
        private Function<SuggestionsBuilder, CompletableFuture<Suggestions>> suggestor;

        SuggestionCallbacks() {
            this.suggestor = ComponentMapStringReader.SUGGEST_DEFAULT;
        }

        public void setSuggestor(Function<SuggestionsBuilder, CompletableFuture<Suggestions>> suggestor) {
            this.suggestor = suggestor;
        }

        public CompletableFuture<Suggestions> getSuggestions(SuggestionsBuilder builder, StringReader reader) {
            return this.suggestor.apply(builder.createOffset(reader.getCursor()));
        }
    }
}
