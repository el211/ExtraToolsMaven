package fr.elias.extraTools.inv;


import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.elias.extraTools.ExtraTools;
import fr.elias.extraTools.utils.config.Msgs;
import fr.elias.extraTools.customs.CustomItem;
import fr.elias.extraTools.inv.elements.InvIcon;
import fr.elias.extraTools.inv.elements.ItemBuilder;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class ManageItemInventory implements InventoryProvider {

    public static SmartInventory INVENTORY(CustomItem item) {
        return SmartInventory.builder()
                .id("itemManager")
                .title("Manage an item")
                .size(3, 9)
                .provider(new ManageItemInventory(item))
                .build();
    }

    private final CustomItem item;

    private ManageItemInventory(CustomItem item) {
        this.item = item;
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        contents.fill(ItemBuilder.filler(DyeColor.BLACK).empty());

        contents.set(0, 4, ItemBuilder.start(item.item()).lore("<gray>" + item.effects().size() + " potion effects",
                "<gray>ID: <gold>" + item.id()).empty());

        contents.set(1, 2, ItemBuilder.start(Material.POTION).name("<aqua>Potions").lore("<aqua>Click to manage the potion effects").of(e -> {
            PotionViewer.INVENTORY(item).open(player);
        }));
        contents.set(1, 4, ItemBuilder.start(Material.CRAFTING_TABLE).name("<yellow>Recipe").lore("<yellow>Click to manage the recipe").of(e -> {
            RecipeInventory.INVENTORY(item).open(player);
        }));
        contents.set(1, 6, ItemBuilder.start(Material.TNT).name("<red>Delete").lore("<red>Click to delete this item").of(e -> {
            ExtraTools.instance().delItem(item);
            Msgs.of("<green>Item deleted").send(player);
            ListInventory.INVENTORY.open(player);
        }));

        contents.set(2, 4, InvIcon.CLOSE.named("<red>Close").of(e -> player.closeInventory()));
    }

    @Override
    public void update(Player player, InventoryContents contents) {
        // Empty implementation as no periodic updates are needed
    }
}
