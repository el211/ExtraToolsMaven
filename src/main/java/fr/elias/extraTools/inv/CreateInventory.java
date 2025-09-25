package fr.elias.extraTools.inv;


import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.elias.extraTools.ExtraTools;
import fr.elias.extraTools.utils.config.Msgs;
import fr.elias.extraTools.customs.CustomItem;
import fr.elias.extraTools.customs.impl.CustomArmor;
import fr.elias.extraTools.customs.impl.CustomTool;
import fr.elias.extraTools.customs.impl.CustomWeapon;
import fr.elias.extraTools.inv.elements.ItemBuilder;
import fr.elias.extraTools.utils.text.Prompts;
import fr.elias.extraTools.utils.ItemSelector;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class CreateInventory implements InventoryProvider {

    public static final SmartInventory INVENTORY = SmartInventory.builder()
            .id("createInv")
            .title(Msgs.of("<gradient:#ff8800:#00ff88>Choose a Type</gradient>").asLegacy()) // <- change
            .size(3, 9)
            .provider(new CreateInventory())
            .build();



    @Override
    public void init(Player player, InventoryContents contents) {
        contents.fill(ItemBuilder.filler(DyeColor.BLACK).empty());

        contents.set(1, 2, ItemBuilder.start(Material.NETHERITE_CHESTPLATE)
                .name("<gradient:#ff0000:#ff8800>Armor</gradient>")
                .of(e -> createArmor(player)));

        contents.set(1, 4, ItemBuilder.start(Material.NETHERITE_PICKAXE)
                .name("<gradient:#00ff88:#0077ff>Tools</gradient>")
                .of(e -> createTool(player)));

        contents.set(1, 6, ItemBuilder.start(Material.NETHERITE_SWORD)
                .name("<gradient:#ff00ff:#7700ff>Weapons</gradient>")
                .of(e -> createWeapon(player)));

        contents.set(2, 4, ItemBuilder.start(Material.BARRIER)
                .name("<gradient:#ff0000:#770000>Close</gradient>")
                .of(e -> player.closeInventory()));
    }


    @Override
    public void update(Player player, InventoryContents contents) {
        // Empty implementation as no periodic updates are needed
    }

    private void createWeapon(Player player) {
        player.closeInventory();
        ItemSelector.INVENTORY(player, ExtraTools.WEAPONS, type -> {
            if (type == null) return;

            Prompts.TEXT.start(player, "<gradient:#ff0000:#ff9900>Write a name</gradient>", name -> {
                if (name == null) {
                    Msgs.of("<gradient:#ff0000:#ff00ff>Invalid input, creation cancelled</gradient>").send(player);
                    return;
                }
                Prompts.TEXT.start(player, "<gradient:#ff9900:#ffff00>Write the lore, use '/n' to create a new line</gradient>", lore -> {
                    if (lore == null) {
                        Msgs.of("<gradient:#ff0000:#ff00ff>Invalid input, creation cancelled</gradient>").send(player);
                        return;
                    }
                    Prompts.NUM.start(player, "<gradient:#00ff00:#00ffff>Write the extra durability</gradient>", extraDurability -> {
                        if (extraDurability == null) {
                            Msgs.of("<gradient:#ff0000:#ff00ff>Invalid input, creation cancelled</gradient>").send(player);
                            return;
                        }
                        Prompts.NUM.start(player, "<gradient:#00ffff:#0000ff>Write the speed bonus</gradient>", extraSpeed -> {
                            if (extraSpeed == null) {
                                Msgs.of("<gradient:#ff0000:#ff00ff>Invalid input, creation cancelled</gradient>").send(player);
                                return;
                            }
                            Prompts.NUM.start(player, "<gradient:#9900ff:#ff00ff>Write the extra damage</gradient>", extraDamage -> {
                                if (extraDamage == null) {
                                    Msgs.of("<gradient:#ff0000:#ff00ff>Invalid input, creation cancelled</gradient>").send(player);
                                    return;
                                }
                                Prompts.NUM.start(player, "<gradient:#ff0000:#00ff00>Write the custom model data</gradient>", customModelData -> {
                                    if (customModelData == null) {
                                        Msgs.of("<gradient:#ff0000:#ff00ff>Invalid input, creation cancelled</gradient>").send(player);
                                    } else {
                                        CustomItem newItem = new CustomWeapon(
                                                name,
                                                lore,
                                                type,
                                                extraDurability.intValue(),
                                                extraSpeed.intValue(),
                                                extraDamage.doubleValue(),
                                                customModelData.intValue()
                                        );
                                        ExtraTools.instance().addItem(newItem);
                                        Msgs.of("<gradient:#00ff00:#0000ff>You created a new item!</gradient>").send(player);
                                        ListInventory.INVENTORY.open(player);
                                    }
                                });
                            });
                        });
                    });
                });
            });
        });
    }



    private void createTool(Player player) {
        player.closeInventory();
        ItemSelector.INVENTORY(player, ExtraTools.TOOLS, type -> {
            if (type == null) return;

            Prompts.TEXT.start(player, "<gradient:#ff007f:#ff7700>Write a name</gradient>", name -> {
                if (name == null) {
                    Msgs.of("<gradient:#ff0000:#ff5500>Invalid input, creation cancelled</gradient>").send(player);
                    return;
                }
                Prompts.TEXT.start(player, "<gradient:#7700ff:#0077ff>Write the lore, use '/n' to create a new line</gradient>", lore -> {
                    if (lore == null) {
                        Msgs.of("<gradient:#ff0000:#ff5500>Invalid input, creation cancelled</gradient>").send(player);
                        return;
                    }
                    Prompts.NUM.start(player, "<gradient:#00ff00:#007f00>Write the extra durability</gradient>", extraDurability -> {
                        if (extraDurability == null) {
                            Msgs.of("<gradient:#ff0000:#ff5500>Invalid input, creation cancelled</gradient>").send(player);
                            return;
                        }
                        Prompts.NUM.start(player, "<gradient:#00ffff:#0000ff>Write the speed bonus</gradient>", extraSpeed -> {
                            if (extraSpeed == null) {
                                Msgs.of("<gradient:#ff0000:#ff5500>Invalid input, creation cancelled</gradient>").send(player);
                                return;
                            }
                            Prompts.BOOL.start(player, "<gradient:#ffff00:#ff7700>Should it be a vertical radius?</gradient>", vertical -> {
                                if (vertical == null) {
                                    Msgs.of("<gradient:#ff0000:#ff5500>Invalid input, creation cancelled</gradient>").send(player);
                                    return;
                                }
                                Prompts.NUM.start(player, "<gradient:#ff00ff:#7700ff>Write the extra radius</gradient>", extraRadius -> {
                                    if (extraRadius == null) {
                                        Msgs.of("<gradient:#ff0000:#ff5500>Invalid input, creation cancelled</gradient>").send(player);
                                        return;
                                    }
                                    Prompts.NUM.start(player, "<gradient:#007fff:#00ff77>Write the custom model data</gradient>", customModelData -> {
                                        if (customModelData == null) {
                                            Msgs.of("<gradient:#ff0000:#ff5500>Invalid input, creation cancelled</gradient>").send(player);
                                        } else {
                                            CustomItem newItem = new CustomTool(
                                                    name,
                                                    lore,
                                                    type,
                                                    extraDurability.intValue(),
                                                    extraSpeed.intValue(),
                                                    extraRadius.intValue(),
                                                    vertical,
                                                    customModelData.intValue()
                                            );
                                            ExtraTools.instance().addItem(newItem);
                                            Msgs.of("<gradient:#00ff00:#007f00>You created a new item</gradient>").send(player);
                                            ListInventory.INVENTORY.open(player);
                                        }
                                    });
                                });
                            });
                        });
                    });
                });
            });
        });
    }



    private void createArmor(Player player) {
        player.closeInventory();
        ItemSelector.INVENTORY(player, ExtraTools.ARMOR, type -> {
            if (type == null) return;

            Prompts.TEXT.start(player, "<gradient:#ff0000:#ff8800>Write a name</gradient>", name -> {
                if (name == null) {
                    Msgs.of("<gradient:#ff0000:#ff5500>Invalid input, creation cancelled</gradient>").send(player);
                    return;
                }
                Prompts.TEXT.start(player, "<gradient:#00ff88:#0077ff>Write the lore, use '/n' to create a new line</gradient>", lore -> {
                    if (lore == null) {
                        Msgs.of("<gradient:#ff0000:#ff5500>Invalid input, creation cancelled</gradient>").send(player);
                        return;
                    }
                    Prompts.NUM.start(player, "<gradient:#8800ff:#7700ff>Write the extra durability</gradient>", extraDurability -> {
                        if (extraDurability == null || extraDurability.intValue() < 0) {
                            Msgs.of("<gradient:#ff0000:#ff5500>Invalid input, creation cancelled</gradient>").send(player);
                            return;
                        }
                        Prompts.NUM.start(player, "<gradient:#00ffff:#0000ff>Write the speed bonus</gradient>", extraSpeed -> {
                            if (extraSpeed == null) {
                                Msgs.of("<gradient:#ff0000:#ff5500>Invalid input, creation cancelled</gradient>").send(player);
                                return;
                            }
                            Prompts.NUM.start(player, "<gradient:#ff00ff:#7700ff>Write the extra health</gradient>", extraHealth -> {
                                if (extraHealth == null) {
                                    Msgs.of("<gradient:#ff0000:#ff5500>Invalid input, creation cancelled</gradient>").send(player);
                                    return;
                                }
                                Prompts.NUM.start(player, "<gradient:#ffff00:#ffaa00>Write the extra armor</gradient>", extraArmor -> {
                                    if (extraArmor == null) {
                                        Msgs.of("<gradient:#ff0000:#ff5500>Invalid input, creation cancelled</gradient>").send(player);
                                        return;
                                    }
                                    Prompts.NUM.start(player, "<gradient:#00ff00:#007f00>Write the custom model data</gradient>", customModelData -> {
                                        if (customModelData == null) {
                                            Msgs.of("<gradient:#ff0000:#ff5500>Invalid input, creation cancelled</gradient>").send(player);
                                        } else {
                                            CustomItem newItem = new CustomArmor(
                                                    name,
                                                    lore,
                                                    type,
                                                    extraDurability.intValue(),
                                                    extraSpeed.intValue(),
                                                    extraHealth.doubleValue(),
                                                    extraArmor.doubleValue(),
                                                    customModelData.intValue()
                                            );
                                            ExtraTools.instance().addItem(newItem);
                                            Msgs.of("<gradient:#00ff00:#007f00>You created a new item</gradient>").send(player);
                                            ListInventory.INVENTORY.open(player);
                                        }
                                    });
                                });
                            });
                        });
                    });
                });
            });
        });
    }
}

