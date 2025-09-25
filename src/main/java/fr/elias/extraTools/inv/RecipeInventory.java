package fr.elias.extraTools.inv;


import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.InventoryListener;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.elias.extraTools.utils.config.Msgs;
import fr.elias.extraTools.customs.CustomItem;
import fr.elias.extraTools.inv.elements.InvIcon;
import fr.elias.extraTools.inv.elements.ItemBuilder;
import fr.elias.extraTools.utils.text.Prompts;
import fr.elias.extraTools.utils.InventorySave;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Objects;

public class RecipeInventory implements InventoryProvider {

    public static SmartInventory INVENTORY(CustomItem customItem) {
        RecipeInventory vault = new RecipeInventory(customItem);
        return SmartInventory.builder()
                .id("recipeManager")
                .provider(vault)
                .listener(new InventoryListener<>(InventoryCloseEvent.class, vault::onClose))
                .size(5, 9)
                .title("Recipes")
                .build();
    }

    private ItemStack[] items = new ItemStack[9];
    private final CustomItem customItem;
    private boolean loaded;

    private RecipeInventory(CustomItem customItem) {
        this.customItem = customItem;
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        contents.fill(ItemBuilder.filler(DyeColor.BLACK).empty());

        String gift = customItem.recipe();
        if (gift != null && !loaded) {
            items = InventorySave.fromBase64(gift);
            loaded = true;
        }

        ItemBuilder glassFiller = ItemBuilder.filler(DyeColor.LIGHT_GRAY).name("<red>");

        int index = 0;
        for (int row = 1; row < 4; row++) {
            for (int column = 1; column < 4; column++) {
                contents.set(row, column, from(items[index] == null ? glassFiller : ItemBuilder.start(items[index]), player, contents, row, column, index));
                index++;
            }
        }

        String currentPerm = customItem.recipePerm();
        contents.set(2, 7, ItemBuilder.start(Material.NAME_TAG)
                .name("<yellow>Set the permission")
                .lore("<gray>Permission: <gold>" + (currentPerm == null ? "none" : currentPerm),
                        "",
                        "<yellow>Click to set the permission",
                        "<yellow>Right click to clear the permission").of(e -> {
                    if (e.isLeftClick()) {
                        player.closeInventory();
                        Prompts.TEXT.start(player, "<gold>Write the permission", text -> {
                            if (text != null) {
                                customItem.recipePerm(text);
                                Msgs.of("<green>Permission set to " + text).send(player);
                            }
                            RecipeInventory.INVENTORY(customItem).open(player);
                        });
                    } else {
                        customItem.recipePerm(null);
                    }
                }));

        contents.set(4, 4, InvIcon.CLOSE
                .named("<green>Done")
                .lore(Msgs.of("<green>Click to save").asComp())
                .of(e -> ManageItemInventory.INVENTORY(customItem).open(player)));
    }

    @Override
    public void update(Player player, InventoryContents contents) {
        // Empty implementation as no periodic updates are needed
    }

    public void onClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        ItemStack[] content = new ItemStack[9];

        int index = 0;
        for (int row = 1; row < 4; row++) {
            for (int column = 1; column < 4; column++) {
                int slot = (row * 9) + column;
                ItemStack item = inventory.getItem(slot);
                if (item != null)
                    content[index++] = item.getType() == Material.LIGHT_GRAY_STAINED_GLASS_PANE ? null : item;
            }
        }

        boolean empty = Arrays.stream(content).allMatch(Objects::isNull);

        customItem.recipe(empty ? null : InventorySave.toBase64(content));
        Msgs.of("<green>Recipe saved").send(event.getPlayer());
    }

    private ClickableItem from(ItemBuilder builder, Player player, InventoryContents contents, int row, int column, int index) {
        return builder.of(e -> {
            ItemStack cursor = e.getCursor();
            ItemStack current = contents.get(row, column).get().getItem();

            boolean dflt = current.getType() == Material.LIGHT_GRAY_STAINED_GLASS_PANE;

            if (cursor == null || cursor.getType() == Material.AIR) {
                if (!dflt) {
                    e.setCursor(current);
                    items[index] = null;
                }
                init(player, contents);
                return;
            }

            contents.set(row, column, from(ItemBuilder.start(cursor), player, contents, row, column, index));
            items[index] = cursor;
            e.setCursor(dflt ? null : current);
            init(player, contents);

        });
    }

}

