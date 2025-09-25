package fr.elias.extraTools;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import fr.elias.extraTools.utils.gson.*;
import com.google.common.collect.Lists;
import fr.minuskube.inv.SmartInvsPlugin;
import fr.elias.extraTools.customs.CustomItem;
import fr.elias.extraTools.customs.impl.CustomArmor;
import fr.elias.extraTools.customs.impl.CustomTool;
import fr.elias.extraTools.customs.impl.CustomWeapon;
import fr.elias.extraTools.utils.InventorySave;
import fr.elias.extraTools.utils.config.Cfgs;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public final class ExtraTools extends JavaPlugin {

    public static Gson gson;

    private final List<CustomItem> customItems = Lists.newArrayList();

    private int lastId;

    @Override
    public void onEnable() {
        instance = this;
        try {
            fr.minuskube.inv.SmartInvsPlugin.setPlugin(this);
            getLogger().info("SmartInvs bootstrap OK.");
        } catch (Throwable t) {
            getLogger().severe("SmartInvs bootstrap FAILED: " + t);
        }

        SmartInvsPlugin.setPlugin(this);

        // ---- Keys first (used by items & listeners)
        durabilityKey = new NamespacedKey(this, "extra_durability");
        itemNameKey   = new NamespacedKey(this, "item_name");

        // ---- Config
        lastId = Cfgs.of().get("lastId", 1);

        // ---- GSON: single setup (no re-definitions below)
        setupGson();

        // ---- Load items once (loadCustomItems() should clear list + de-dup by id)
        loadCustomItems();

        // ---- Command
        CustomCommand customCommand = new CustomCommand(this);
        PluginCommand pluginCommand = getCommand("customtool");
        if (pluginCommand == null) {
            getLogger().severe("Command 'customtool' not found. Is it in plugin.yml and packaged at jar root?");
        } else {
            pluginCommand.setExecutor(customCommand);
            pluginCommand.setTabCompleter(customCommand);
            getLogger().info("Registered /customtool (aliases: ct, customt, ctool).");
        }


        // ---- Listeners
        getServer().getPluginManager().registerEvents(new CustomListener(this), this);

        // ---- Periodic tasks
        getServer().getScheduler().runTaskTimer(this, () ->
                Bukkit.getOnlinePlayers().forEach(player -> {
                    ItemStack hand = player.getInventory().getItemInMainHand();
                    if (item(hand) instanceof CustomTool tool) {
                        applyHaste(player, tool);
                    }
                }), 0L, 100L
        );

        getServer().getScheduler().runTaskTimer(this, () ->
                Bukkit.getOnlinePlayers().forEach(player -> {
                    for (ItemStack armor : player.getInventory().getArmorContents()) {
                        CustomItem customItem = item(armor);
                        if (customItem instanceof CustomArmor) {
                            customItem.applyEffects(player);
                        }
                    }
                }), 0L, 100L
        );

        getLogger().info("ExtraTools enabled. Loaded " + items().size() + " custom items.");
    }



    @Override
    public void onDisable() {
        saveCustomItems(); // Save all items to the folder
        Cfgs.of().set("lastId", lastId).save(); // Save last ID
        getLogger().info("ExtraTools plugin disabled.");
    }

    public void setupGson() {
        var rta = RuntimeTypeAdapterFactory.of(CustomItem.class, "type")
                .registerSubtype(CustomArmor.class, "armor")
                .registerSubtype(CustomTool.class,  "tool")
                .registerSubtype(CustomWeapon.class,"weapon");

        gson = new GsonBuilder()
                .registerTypeAdapterFactory(rta)
                .registerTypeAdapter(PotionEffectType.class, new EffectTypeAdapter())
                .registerTypeAdapter(PotionEffect.class,     new PotionEffectAdapter())
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
    }


    public void saveCustomItems() {
        File folder = new File(getDataFolder(), "items");
        if (!folder.exists()) folder.mkdirs();

        for (CustomItem item : customItems) {
            saveItem(item);

            // remove legacy file if present
            File legacy = new File(folder, item.id() + ".json");
            File modern = file(item);
            if (legacy.exists() && !legacy.equals(modern)) {
                if (!legacy.delete()) {
                    getLogger().warning("Couldn't delete legacy file: " + legacy.getName());
                }
            }
        }
    }

    private void saveItem(CustomItem item) {
        File out = file(item);
        out.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(out)) {
            String json = gson.toJson(item);
            gson.fromJson(json, CustomItem.class); // quick validation
            writer.write(json);
        } catch (IOException | JsonSyntaxException e) {
            getLogger().severe("Failed to save custom item: " + item.name());
            e.printStackTrace();
        }
    }





    public void loadCustomItems() {
        customItems.clear();
        File folder = new File(getDataFolder(), "items");
        if (!folder.exists()) folder.mkdirs();

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (files == null) return;

        Map<Integer, CustomItem> unique = new LinkedHashMap<>();
        int maxId = 0;

        for (File f : files) {
            try (FileReader reader = new FileReader(f)) {
                CustomItem it = gson.fromJson(reader, CustomItem.class);
                if (it != null) {
                    unique.put(it.id(), it);     // last wins
                    if (it.id() > maxId) maxId = it.id();
                }
            } catch (IOException | JsonSyntaxException e) {
                getLogger().severe("Failed to load custom item from: " + f.getName());
                backupCorruptedFile(f);
                e.printStackTrace();
            }
        }
        customItems.addAll(unique.values());

        // ensure future IDs don’t collide even if config was stale
        lastId = Math.max(lastId, maxId + 1);

        getLogger().info("Loaded " + customItems.size() + " custom items from " + files.length + " files.");
    }



    private void backupCorruptedFile(File file) {
        File backupFolder = new File(getDataFolder(), "corrupted");
        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }

        File backupFile = new File(backupFolder, file.getName());
        if (file.renameTo(backupFile)) {
            getLogger().warning("Moved corrupted file to backup: " + backupFile.getName());
        } else {
            getLogger().severe("Failed to move corrupted file: " + file.getName());
        }
    }



    public List<CustomItem> items() {
        return customItems;
    }

    public void addItem(CustomItem item) {
        if (item == null) {
            getLogger().warning("Attempted to add a null CustomItem.");
            return;
        }
        // if an item with same id exists in-memory, replace it
        customItems.removeIf(ci -> ci.id() == item.id());
        customItems.add(item);
        saveItem(item);
    }





    public void delItem(CustomItem info) {
        // Check if the CustomItem is null
        if (info == null) {
            getLogger().warning("Attempted to delete a null CustomItem.");
            return;
        }

        // Try to remove the item from the list
        boolean removed = customItems.remove(info);
        if (!removed) {
            getLogger().warning("Failed to remove CustomItem from the list: " + info.id());
            return;
        }

        // Attempt to delete the associated file
        File file = file(info);
        if (file.exists()) {
            if (file.delete()) {
                getLogger().info("Successfully deleted CustomItem and its file: " + info.id());
            } else {
                getLogger().severe("Failed to delete the file for CustomItem: " + info.id());
            }
        } else {
            getLogger().warning("File for CustomItem does not exist: " + info.id());
        }
    }


    public int getId() {
        return lastId++;
    }

    public void applyHaste(Player player, CustomItem tool) {
        if (tool.material() == null) {
            getLogger().warning("CustomItem material is null for item: " + tool.name());
            return;
        }

        double base = 9D;
        String toolType = tool.material().name().split("_")[0];

        int lowest = switch (toolType) {
            case "WOODEN" -> 2;
            case "STONE" -> 4;
            case "IRON" -> 6;
            case "DIAMOND" -> 8;
            default -> 9;
        };

        if (lowest == base) return;

        double test = base / lowest;
        int levelNeeded = (int) (test * 5);
        int extra = (tool.extraSpeed() - 1) * 20;
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.HASTE, 120, levelNeeded + extra, true, false, false));
    }

    public CustomItem item(int id) {
        return customItems.stream().filter(ci -> ci.id() == id).findFirst().orElse(null);
    }

    public CustomItem item(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer id = pdc.get(itemNameKey, PersistentDataType.INTEGER);
        return (id == null) ? null : item(id);
    }
    public boolean isPluginItem(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return false;
        return stack.getItemMeta().getPersistentDataContainer()
                .has(itemNameKey, PersistentDataType.INTEGER);
    }


    public CustomItem itemByRecipe(ItemStack[] matrix) {
        return customItems.stream()
                .filter(Objects::nonNull)
                .filter(ci -> {
                    String recipeString = ci.recipe();
                    if (recipeString == null) {
                        getLogger().warning("CustomItem missing recipe: " + ci.name());
                        return false;
                    }
                    ItemStack[] recipe = InventorySave.fromBase64(recipeString);
                    return Arrays.equals(matrix, recipe);
                })
                .findFirst()
                .orElse(null);
    }

    public void giveItems(Player to, ItemStack... items) {
        items = Arrays.stream(items).filter(Objects::nonNull).toArray(ItemStack[]::new);
        Collection<ItemStack> drops = to.getInventory().addItem(items).values();

        if (!drops.isEmpty()) {
            drops.forEach(toDrop -> {
                Item item = to.getWorld().dropItem(to.getLocation(), toDrop);
                item.setOwner(to.getUniqueId());
            });
            to.sendMessage("Some items could not fit in your inventory and were dropped at your location.");
        }
    }

    private File file(CustomItem item) {
        File folder = new File(getDataFolder(), "items");
        String fileName = item.id() + "_" + item.strippedName() + ".json"; // Updated to .json extension
        return new File(folder, fileName);
    }




    // Keys
    public static NamespacedKey durabilityKey;
    public static NamespacedKey itemNameKey;

    private static ExtraTools instance;

    public static ExtraTools instance() {
        return instance;
    }

    public static final EnumSet<Material> TOOLS = EnumSet.of(Material.WOODEN_PICKAXE,
            Material.STONE_PICKAXE,
            Material.IRON_PICKAXE,
            Material.GOLDEN_PICKAXE,
            Material.DIAMOND_PICKAXE,
            Material.NETHERITE_PICKAXE,
            Material.WOODEN_SHOVEL,
            Material.STONE_SHOVEL,
            Material.IRON_SHOVEL,
            Material.GOLDEN_SHOVEL,
            Material.DIAMOND_SHOVEL,
            Material.NETHERITE_SHOVEL,
            Material.WOODEN_AXE,
            Material.STONE_AXE,
            Material.IRON_AXE,
            Material.GOLDEN_AXE,
            Material.DIAMOND_AXE,
            Material.NETHERITE_AXE,
            Material.WOODEN_HOE,
            Material.STONE_HOE,
            Material.IRON_HOE,
            Material.GOLDEN_HOE,
            Material.DIAMOND_HOE,
            Material.NETHERITE_HOE);

    public static final EnumSet<Material> ARMOR = EnumSet.of(Material.LEATHER_HELMET,
            Material.IRON_HELMET,
            Material.CHAINMAIL_HELMET,
            Material.GOLDEN_HELMET,
            Material.DIAMOND_HELMET,
            Material.NETHERITE_HELMET,
            Material.LEATHER_CHESTPLATE,
            Material.IRON_CHESTPLATE,
            Material.CHAINMAIL_CHESTPLATE,
            Material.GOLDEN_CHESTPLATE,
            Material.DIAMOND_CHESTPLATE,
            Material.NETHERITE_CHESTPLATE,
            Material.LEATHER_LEGGINGS,
            Material.IRON_LEGGINGS,
            Material.CHAINMAIL_LEGGINGS,
            Material.GOLDEN_LEGGINGS,
            Material.DIAMOND_LEGGINGS,
            Material.NETHERITE_LEGGINGS,
            Material.LEATHER_BOOTS,
            Material.IRON_BOOTS,
            Material.CHAINMAIL_BOOTS,
            Material.GOLDEN_BOOTS,
            Material.DIAMOND_BOOTS,
            Material.NETHERITE_BOOTS);

    public static final EnumSet<Material> WEAPONS = EnumSet.of(Material.WOODEN_SWORD,
            Material.STONE_SWORD,
            Material.IRON_SWORD,
            Material.GOLDEN_SWORD,
            Material.DIAMOND_SWORD,
            Material.NETHERITE_SWORD);

}

