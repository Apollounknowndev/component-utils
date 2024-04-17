package dev.worldgen.componentutils;

import dev.worldgen.componentutils.command.ComponentCommand;
import dev.worldgen.componentutils.command.argument.ComponentMapArgumentType;
import dev.worldgen.componentutils.command.argument.ComponentEntryTypeArgumentType;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComponentUtils implements ModInitializer {
	public static final String MOD_ID = "component_utils";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> ComponentCommand.register(dispatcher, registryAccess)));

		ArgumentTypeRegistry.registerArgumentType(
			new Identifier(MOD_ID, "component_map"),
			ComponentMapArgumentType.class,
			ConstantArgumentSerializer.of(ComponentMapArgumentType::new)
		);

		ArgumentTypeRegistry.registerArgumentType(
			new Identifier(MOD_ID, "map_entry_type"),
			ComponentEntryTypeArgumentType.class,
			ConstantArgumentSerializer.of(ComponentEntryTypeArgumentType::new)
		);
	}
}