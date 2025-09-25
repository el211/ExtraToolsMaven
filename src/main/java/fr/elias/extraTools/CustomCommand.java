package fr.elias.extraTools;


import fr.elias.extraTools.utils.config.Msgs;
import fr.elias.extraTools.customs.CustomItem;
import fr.elias.extraTools.inv.CreateInventory;
import fr.elias.extraTools.inv.ListInventory;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class CustomCommand implements CommandExecutor, TabCompleter {

    private final ExtraTools plugin;

    public CustomCommand(ExtraTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 3 && args[0].equalsIgnoreCase("give") && sender.hasPermission("extraTools.admin")) {
            handleGiveCommand(sender, args);
            return true;
        }

        if (!(sender instanceof Player player)) {
            Msgs.of("<red>Only players can use this command").send(sender);
            return true;
        }

        // /ct  (no args)
        if (args.length == 0) {
            if (sender.hasPermission("extraTools.admin")) {
                try {
                    CreateInventory.INVENTORY.open(player);
                } catch (Throwable t) {
                    ExtraTools.instance().getLogger().severe("Failed to open CreateInventory: " + t);
                    Msgs.of("<red>Could not open GUI. Check console for errors.").send(player);
                }
            } else {
                Msgs.of("<red>Unknown command").send(player);
            }
            return true;
        }

        // /ct <sub>
        String sub = args[0].toLowerCase(java.util.Locale.ROOT);
        switch (sub) {
            case "repair" -> {
                if (!sender.hasPermission("extraTools.repair")) {
                    Msgs.of("<red>You don't have permission.").send(player);
                    return true;
                }
                repairItem(player);
                return true;
            }
            case "create" -> {
                if (!sender.hasPermission("extraTools.admin")) {
                    Msgs.of("<red>You don't have permission.").send(player);
                    return true;
                }
                try {
                    CreateInventory.INVENTORY.open(player);
                } catch (Throwable t) {
                    ExtraTools.instance().getLogger().severe("Failed to open CreateInventory: " + t);
                    Msgs.of("<red>Could not open GUI. Check console for errors.").send(player);
                }
                return true;
            }
            case "view" -> {
                if (!sender.hasPermission("extraTools.admin")) {
                    Msgs.of("<red>You don't have permission.").send(player);
                    return true;
                }
                try {
                    ListInventory.INVENTORY.open(player);
                } catch (Throwable t) {
                    ExtraTools.instance().getLogger().severe("Failed to open ListInventory: " + t);
                    Msgs.of("<red>Could not open GUI. Check console for errors.").send(player);
                }
                return true;
            }
            default -> {
                Msgs.of("<red>Unknown command").send(player);
                return true;
            }
        }
    }


    private void handleGiveCommand(CommandSender sender, String[] args) {
        String playerName = args[1];
        Player target = Bukkit.getPlayerExact(playerName);

        if (target == null) {
            Msgs.of("<red>Invalid player").send(sender);
            return;
        }

        try {
            int itemId = Integer.parseInt(args[2]);
            CustomItem tool = plugin.item(itemId);

            if (tool == null) {
                Msgs.of("<red>Item not found").send(sender);
            } else {
                plugin.giveItems(target, tool.item());
                Msgs.of("<green>Item given successfully").send(sender);
            }
        } catch (NumberFormatException e) {
            Msgs.of("<red>Invalid item ID").send(sender);
        }
    }

    private void repairItem(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        CustomItem customItem = plugin.item(hand);

        if (customItem == null) {
            Msgs.of("<red>No custom item in hand").send(player);
            return;
        }

        ItemMeta meta = hand.getItemMeta();
        if (meta == null) {
            Msgs.of("<red>No item meta to repair").send(player);
            return;
        }

        // Reset vanilla damage
        if (meta instanceof Damageable dmg) {
            dmg.setDamage(0);
        }

        // Prepare lore, remove old durability line if we had one
        var pdc = meta.getPersistentDataContainer();
        boolean hadExtra = pdc.has(ExtraTools.durabilityKey, PersistentDataType.INTEGER);

        List<Component> lore = meta.lore();
        if (lore == null) lore = new ArrayList<>();
        if (hadExtra && !lore.isEmpty()) {
            lore.remove(lore.size() - 1);
        }

        // Re-apply custom extra durability
        int extraDurability = customItem.extraDurability();
        if (extraDurability > 0) {
            pdc.set(ExtraTools.durabilityKey, PersistentDataType.INTEGER, extraDurability);
            lore.add(Msgs.of("<aqua>Durability: <bold>%extra%/%extra%")
                    .var("extra", extraDurability)
                    .asComp());
        } else {
            pdc.remove(ExtraTools.durabilityKey);
        }

        meta.lore(lore);
        hand.setItemMeta(meta);

        Msgs.of("<green>Repaired").send(player);
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!command.getName().equalsIgnoreCase("customtool")) {
            return completions;
        }

        if (args.length == 1) {
            if (sender.hasPermission("extraTools.admin")) {
                completions.add("give");
                completions.add("create");
                completions.add("view");
            }
            if (sender.hasPermission("extraTools.repair")) {
                completions.add("repair");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give") && sender.hasPermission("extraTools.admin")) {
            Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getName()));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give") && sender.hasPermission("extraTools.admin")) {
            plugin.items().forEach(item -> completions.add(String.valueOf(item.id())));
        }

        String currentInput = args[args.length - 1];
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(currentInput.toLowerCase()))
                .toList();
    }
}

