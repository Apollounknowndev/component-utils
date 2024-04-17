package dev.worldgen.componentutils;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.item.TooltipType;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class ComponentUtilsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ItemTooltipCallback.EVENT.register(ComponentUtilsClient::appendComponentChangesTooltip);
    }

    private static void appendComponentChangesTooltip(ItemStack stack, Item.TooltipContext context, TooltipType type, List<Text> lines) {
        if (!lines.isEmpty()) {
            ComponentChanges changes = stack.getComponentChanges();
            if (!changes.isEmpty() && type.isAdvanced()) {
                lines.add(getTooltip(changes.size()));
            }
        }
    }

    private static Text getTooltip(int size) {
        return Text.translatable("component_utils.component_changes_tooltip", size).setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY));
    }
}
