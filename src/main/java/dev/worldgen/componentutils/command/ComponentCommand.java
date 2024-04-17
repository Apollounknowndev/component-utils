package dev.worldgen.componentutils.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import dev.worldgen.componentutils.command.argument.ComponentEntryTypeArgumentType;
import dev.worldgen.componentutils.command.argument.ComponentMapArgumentType;
import dev.worldgen.componentutils.mixin.RegistryKeyArgumentTypeInvoker;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.ItemSlotArgumentType;
import net.minecraft.command.argument.RegistryKeyArgumentType;
import net.minecraft.component.*;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.server.command.CommandManager.*;

public class ComponentCommand {
    private static final DynamicCommandExceptionType INVALID_DATA_COMPONENT_EXCEPTION = new DynamicCommandExceptionType((id) -> Text.stringifiedTranslatable("component_utils.data_component_invalid", id));

    private static final DynamicCommandExceptionType NO_SUCH_SLOT_SOURCE_EXCEPTION = new DynamicCommandExceptionType((slot) -> Text.stringifiedTranslatable("commands.item.source.no_such_slot", slot));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(literal("component_utils").then(
            argument("source", EntityArgumentType.entity()).then(
                argument("source_slot", ItemSlotArgumentType.itemSlot()).then(
                    literal("get").then(
                        argument("map_entry_type", ComponentEntryTypeArgumentType.componentEntryType()).then(
                            argument("component_type", RegistryKeyArgumentType.registryKey(RegistryKeys.DATA_COMPONENT_TYPE)).executes(
                                (context) -> ComponentCommand.get(
                                    context.getSource(),
                                    getStackReference(
                                        EntityArgumentType.getEntity(context, "source"),
                                        ItemSlotArgumentType.getItemSlot(context, "source_slot")
                                    ).get(),
                                    ComponentEntryTypeArgumentType.getComponentEntryType(context, "map_entry_type"),
                                    RegistryKeyArgumentTypeInvoker.getRegistryEntry(context, "component_type", RegistryKeys.DATA_COMPONENT_TYPE, INVALID_DATA_COMPONENT_EXCEPTION)
                                )
                            )
                        ).executes(
                            (context) -> ComponentCommand.get(
                                context.getSource(),
                                getStackReference(
                                    EntityArgumentType.getEntity(context, "source"),
                                    ItemSlotArgumentType.getItemSlot(context, "source_slot")
                                ).get(),
                                ComponentEntryTypeArgumentType.getComponentEntryType(context, "map_entry_type"),
                                null
                            )
                        )
                    )).then(
                    literal("merge").then(
                        argument("component_map", ComponentMapArgumentType.create(registryAccess)).executes(
                            (context) -> ComponentCommand.merge(
                                context.getSource(),
                                getStackReference(
                                    EntityArgumentType.getEntity(context, "source"),
                                    ItemSlotArgumentType.getItemSlot(context, "source_slot")
                                ).get(),
                                ComponentMapArgumentType.getComponentMapArgument(context, "component_map")
                            )
                        )
                    )).then(
                    literal("remove").then(
                        argument("component_type", RegistryKeyArgumentType.registryKey(RegistryKeys.DATA_COMPONENT_TYPE)).executes(
                            (context) -> ComponentCommand.remove(
                                context.getSource(),
                                getStackReference(
                                    EntityArgumentType.getEntity(context, "source"),
                                    ItemSlotArgumentType.getItemSlot(context, "source_slot")
                                ).get(),
                                RegistryKeyArgumentTypeInvoker.getRegistryEntry(context, "component_type", RegistryKeys.DATA_COMPONENT_TYPE, INVALID_DATA_COMPONENT_EXCEPTION)
                            )
                        )
                    )).then(
                    literal("clear").then(
                        argument("component_type", RegistryKeyArgumentType.registryKey(RegistryKeys.DATA_COMPONENT_TYPE)).executes(
                            (context) -> ComponentCommand.clear(
                                context.getSource(),
                                getStackReference(
                                    EntityArgumentType.getEntity(context, "source"),
                                    ItemSlotArgumentType.getItemSlot(context, "source_slot")
                                ).get(),
                                RegistryKeyArgumentTypeInvoker.getRegistryEntry(context, "component_type", RegistryKeys.DATA_COMPONENT_TYPE, INVALID_DATA_COMPONENT_EXCEPTION)
                            )
                        )).executes(
                        (context) -> ComponentCommand.clear(
                            context.getSource(),
                            getStackReference(
                                EntityArgumentType.getEntity(context, "source"),
                                ItemSlotArgumentType.getItemSlot(context, "source_slot")
                            ).get(),
                            null
                        ))
                    )
                )
            )
        );
    }

    private static StackReference getStackReference(Entity entity, int slot) throws CommandSyntaxException {
        StackReference stackReference = entity.getStackReference(slot);
        if (stackReference == StackReference.EMPTY) {
            throw NO_SUCH_SLOT_SOURCE_EXCEPTION.create(slot);
        } else {
            return stackReference;
        }
    }

    private static int clear(ServerCommandSource source, ItemStack stack,  @Nullable RegistryEntry.Reference<DataComponentType<?>> type) {
        ComponentMapImpl components = ((ComponentMapImpl)stack.getComponents());

        if (type != null) {
            return ComponentCommandUtils.resetSingleChange(source, components, type);
        }
        return ComponentCommandUtils.resetAllChange(source, components);
    }

    private static int merge(ServerCommandSource source, ItemStack stack, ComponentMap components) {
        stack.applyComponentsFrom(components);
        source.sendFeedback(() -> Text.translatable("commands.component_utils.merge.success"), false);
        return 1;
    }

    private static int remove(ServerCommandSource source, ItemStack stack, RegistryEntry.Reference<DataComponentType<?>> type) {
        ComponentMapImpl components = ((ComponentMapImpl)stack.getComponents());

        return ComponentCommandUtils.removeComponent(source, stack.getDefaultComponents(), components, type);
    }

    private static int get(ServerCommandSource source, ItemStack stack, ComponentEntryType entryType, RegistryEntry.Reference<DataComponentType<?>> componentType) {
        RegistryOps<NbtElement> ops = source.getServer().getCombinedDynamicRegistries().getCombinedRegistryManager().getOps(NbtOps.INSTANCE);
        if (componentType != null) {
            return ComponentCommandUtils.getSingleComponent(source, ops, (ComponentMapImpl) stack.getComponents(), entryType, componentType);
        }

        return ComponentCommandUtils.getAllComponents(source, ops, (ComponentMapImpl) stack.getComponents(), entryType);
    }
}
