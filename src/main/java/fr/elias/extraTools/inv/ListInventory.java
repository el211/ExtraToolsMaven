package fr.elias.extraTools.inv;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.minuskube.inv.content.Pagination;
import fr.minuskube.inv.content.SlotIterator;
import fr.elias.extraTools.ExtraTools;
import fr.elias.extraTools.customs.CustomItem;
import fr.elias.extraTools.inv.elements.InvIcon;
import fr.elias.extraTools.inv.elements.ItemBuilder;
import org.bukkit.entity.Player;

import java.util.List;

public class ListInventory implements InventoryProvider {

    public static final SmartInventory INVENTORY = SmartInventory.builder()
            .id("listInventory")
            .size(6, 9)
            .title("Custom Items")
            .provider(new ListInventory())
            .build();

    @Override
    public void init(Player player, InventoryContents contents) {

        List<CustomItem> customItems = ExtraTools.instance().items();

        Pagination pagination = contents.pagination();
        ClickableItem[] items = new ClickableItem[customItems.size()];

        int i = 0;
        for (CustomItem info : customItems) {
            items[i++] = ItemBuilder.start(info.item())
                    .lore("<gray>Click to get this item", "<gray>Right click to manage this item",
                            "",
                            "<gray>" + info.effects().size() + " potion effects",
                            "<gray>ID: <gold>" + info.id())
                    .of(e -> {
                        if (e.isRightClick()) {
                            ManageItemInventory.INVENTORY(info).open(player);
                        } else {
                            ExtraTools.instance().giveItems(player, info.item());
                        }
                    });
        }

        pagination.setItems(items);
        pagination.setItemsPerPage(45);

        pagination.addToIterator(contents.newIterator(SlotIterator.Type.HORIZONTAL, 0, 0));

        contents.set(5, 4, InvIcon.CLOSE
                .of(e -> player.closeInventory()));

        if (!pagination.isFirst()) {
            contents.set(5, 2,
                    InvIcon.PREV.of(e -> INVENTORY.open(player, pagination.previous().getPage())));
        }

        if (!pagination.isLast()) {
            contents.set(5, 6,
                    InvIcon.NEXT.of(e -> INVENTORY.open(player, pagination.next().getPage())));
        }

    }

    @Override
    public void update(Player player, InventoryContents contents) {
        // Empty implementation as no periodic updates are needed
    }

}

