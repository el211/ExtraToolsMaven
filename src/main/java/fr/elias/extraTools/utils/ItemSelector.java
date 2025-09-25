package fr.elias.extraTools.utils;


import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.InventoryListener;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.minuskube.inv.content.Pagination;
import fr.minuskube.inv.content.SlotIterator;
import fr.elias.extraTools.utils.config.Msgs;
import fr.elias.extraTools.inv.elements.InvIcon;
import fr.elias.extraTools.inv.elements.ItemBuilder;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.EnumSet;
import java.util.function.Consumer;

public class ItemSelector implements InventoryProvider {

    public static void INVENTORY(Player player, EnumSet<Material> available, Consumer<Material> consumer) {
        INVENTORY(consumer, available).open(player);
    }

    private static SmartInventory INVENTORY(Consumer<Material> future, EnumSet<Material> available) {
        ItemSelector menu = new ItemSelector(future, available);
        return SmartInventory.builder()
                .id("itemSelector")
                .provider(menu)
                .listener(new InventoryListener<>(InventoryCloseEvent.class, menu::onClose))
                .size(3, 9)
                .title(Msgs.of("Select an item").asLegacy())
                .build();
    }

    private final Consumer<Material> future;
    private final EnumSet<Material> available;

    public ItemSelector(Consumer<Material> future, EnumSet<Material> available) {
        this.available = available;
        this.future = future;
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        contents.fill(ItemBuilder.filler(DyeColor.BLACK).empty());
        contents.set(2, 4, InvIcon.CLOSE.of(e -> player.closeInventory()));

        Pagination pagination = contents.pagination();
        ClickableItem[] items = new ClickableItem[available.size()];

        int slot = 0;
        for (Material other : available) {
            items[slot++] = ItemBuilder.start(other).of(e -> {
                future.accept(other);
                player.closeInventory();
            });
        }

        pagination.setItems(items);
        pagination.setItemsPerPage(7);
        pagination.addToIterator(contents.newIterator(SlotIterator.Type.HORIZONTAL, 1, 1));

        if (!pagination.isFirst())
            contents.set(2, 1,
                    InvIcon.PREV.of(e -> INVENTORY(future, available).open(player, pagination.previous().getPage())));

        if (!pagination.isLast())
            contents.set(2, 7,
                    InvIcon.NEXT.of(e -> INVENTORY(future, available).open(player, pagination.next().getPage())));
    }

    @Override
    public void update(Player player, InventoryContents contents) {
        // Empty implementation as no periodic updates are needed
    }

    private void onClose(InventoryCloseEvent event) {
        future.accept(null);
    }
}
