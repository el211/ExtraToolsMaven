package fr.elias.extraTools;

import com.destroystokyo.paper.MaterialTags;
import de.Linus122.DropEdit.DataStore.DropConfigurationData;
import de.Linus122.DropEdit.DropContainer;
import de.Linus122.DropEdit.Main;
import de.Linus122.DropInfo.KeyGetter;
import de.Linus122.DropInfo.KeyInfo;
import fr.elias.extraTools.customs.CustomItem;
import fr.elias.extraTools.customs.impl.CustomArmor;
import fr.elias.extraTools.customs.impl.CustomTool;
import fr.elias.extraTools.customs.impl.CustomWeapon;
import fr.elias.extraTools.utils.config.Cfgs;
import fr.elias.extraTools.utils.config.Msgs;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;

import java.util.*;
import java.util.function.Consumer;

public class CustomListener implements Listener {

    private final ExtraTools plugin;

    public CustomListener(ExtraTools plugin) {
        this.plugin = plugin;
    }
//
    /* ------------------------ Durability (extra pool) ------------------------ */

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemDamage(PlayerItemDamageEvent event) {
        ItemStack item = event.getItem();
        int damage = event.getDamage();
        int newDamage = damage;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (pdc.has(ExtraTools.durabilityKey, PersistentDataType.INTEGER)) {
            CustomItem customItem = plugin.item(item);
            List<Component> newLore = meta.lore();
            if (newLore == null) newLore = new ArrayList<>();
            else if (!newLore.isEmpty()) newLore.remove(newLore.size() - 1);

            Integer extraDurability = pdc.get(ExtraTools.durabilityKey, PersistentDataType.INTEGER);
            extraDurability = (extraDurability == null) ? 0 : extraDurability;

            if (damage >= extraDurability) {
                newDamage = damage - extraDurability;
                pdc.remove(ExtraTools.durabilityKey);
            } else {
                newDamage = 0;
                int result = extraDurability - damage;
                pdc.set(ExtraTools.durabilityKey, PersistentDataType.INTEGER, result);
                if (customItem != null) {
                    newLore.add(Msgs.of("<aqua>Durability: <bold>%result%/%extra%")
                            .var("result", result)
                            .var("extra", customItem.extraDurability())
                            .asComp());
                }
            }

            meta.lore(newLore);
            item.setItemMeta(meta);
        }

        event.setDamage(newDamage);
    }

    /* ------------------------ Weapon extra damage ------------------------ */

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        ItemStack hand = player.getInventory().getItemInMainHand();
        CustomItem item = plugin.item(hand);
        if (!(item instanceof CustomWeapon customWeapon)) return;

        event.setDamage(event.getDamage() + customWeapon.extraDamage());
    }
    // --- WorldGuard integration (soft) ---
// Soft check
    private boolean hasWorldGuard() {
        return org.bukkit.Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
    }

    private boolean canBreakHere(Player player, Block block) {
        if (!hasWorldGuard()) return true;
        try {
            // WG 7.0.14: create a query and ask it
            com.sk89q.worldguard.bukkit.WorldGuardPlugin wg =
                    com.sk89q.worldguard.bukkit.WorldGuardPlugin.inst();
            com.sk89q.worldguard.bukkit.ProtectionQuery pq = wg.createProtectionQuery();
            return pq.testBlockBreak(player, block);
            // (equivalently: new com.sk89q.worldguard.bukkit.ProtectionQuery().testBlockBreak(player, block))
        } catch (Throwable t) {
            plugin.getLogger().warning("[ExtraTools] WorldGuard check failed: " + t.getClass().getSimpleName());
            return true; // fail-open rather than crash
        }
    }

    // call this just before you break the AOE block
// One-shot crack flash + sound, then reset a moment later
    private void flashBreak(Player p, Block b) {
        try {
            final org.bukkit.Location loc = b.getLocation();

            // show full crack immediately
            p.sendBlockDamage(loc, 1.0f);

            // play the block's native break sound
            var sg = b.getBlockData().getSoundGroup();
            b.getWorld().playSound(loc, sg.getBreakSound(), 1f, 1f);

            // reset so next flashes still render
            Bukkit.getScheduler().runTaskLater(ExtraTools.instance(), () -> {
                try {
                    // -1f clears the damage indicator for this player
                    p.sendBlockDamage(loc, -1f);
                } catch (Throwable ignored) {}
            }, 2L);
        } catch (Throwable ignored) {
            // Non-Paper or older API: silently skip
        }
    }



    /* ------------------------ Mining: vertical / square ------------------------ */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (p.getGameMode() == org.bukkit.GameMode.CREATIVE) return;

        ItemStack hand = p.getInventory().getItemInMainHand();
        CustomItem ci = plugin.item(hand);
        if (!(ci instanceof CustomTool tool)) return;

        // Tool families that get AOE
        boolean isPickaxe = MaterialTags.PICKAXES.isTagged(hand.getType());
        boolean isShovel  = MaterialTags.SHOVELS.isTagged(hand.getType());
        boolean isAxe     = MaterialTags.AXES.isTagged(hand.getType());
        if (!isPickaxe && !isShovel && !isAxe) return;

        // Choose correct mineable/* tag
        Tag<Material> breakTag =
                isPickaxe ? org.bukkit.Tag.MINEABLE_PICKAXE :
                        isShovel  ? org.bukkit.Tag.MINEABLE_SHOVEL :
                                org.bukkit.Tag.MINEABLE_AXE;

        // Let vanilla handle the center block (so normal durability event fires)
        Block center = e.getBlock();

        // Only expand AOE if center matches tool family or is an IA block
        boolean centerIsIA = isItemsAdderBlock(center);
        if (!centerIsIA && (breakTag == null || !breakTag.isTagged(center.getType()))) return;

        int r = Math.max(1, tool.radius());
        int range = r - 1; // radius=2 -> 3x3, etc.
        if (range <= 0) return;

        if (tool.vertical()) {
            // Aimed block + range below (skip protected blocks)
            for (int i = 1; i <= range; i++) {
                Block b = center.getRelative(BlockFace.DOWN, i);
                if (b.isEmpty() || b.getType().isAir()) continue;
                if (!canBreakHere(p, b)) continue; // WorldGuard guard

                // IA branch (visual flash + break)
                if (isItemsAdderBlock(b)) {
                    flashBreak(p, b);
                    if (tryBreakItemsAdder(p, b, hand)) {
                        consumeExtraDurability(hand, tool, 1);
                        continue;
                    }
                    // If IA failed for some reason, fall through to vanilla tag check
                }

                // Then vanilla blocks valid for this tool family
                if (breakTag != null && breakTag.isTagged(b.getType())) {
                    flashBreak(p, b);
                    consumeExtraDurability(hand, tool, 1);
                    safeCallDrop(p, hand, b);
                }
            }
            return;
        }

        // Horizontal square in the plane you hit (perpendicular to face)
        org.bukkit.util.RayTraceResult hit = p.rayTraceBlocks(5.0);
        BlockFace face = (hit != null) ? hit.getHitBlockFace() : p.getFacing();

        for (int a = -range; a <= range; a++) {
            for (int bOff = -range; bOff <= range; bOff++) {
                if (a == 0 && bOff == 0) continue; // center handled by vanilla

                Block target = switch (face) {
                    case UP, DOWN     -> center.getRelative(a, 0, bOff);
                    case NORTH, SOUTH -> center.getRelative(a, bOff, 0);
                    case EAST, WEST   -> center.getRelative(0, bOff, a);
                    default           -> center.getRelative(a, 0, bOff);
                };

                if (target.isEmpty() || target.getType().isAir()) continue;
                if (!canBreakHere(p, target)) continue; // WorldGuard guard

                // IA branch (visual flash + break)
                if (isItemsAdderBlock(target)) {
                    flashBreak(p, target);
                    if (tryBreakItemsAdder(p, target, hand)) {
                        consumeExtraDurability(hand, tool, 1);
                        continue;
                    }
                    // If IA failed for some reason, fall through to vanilla tag check
                }

                // Then vanilla blocks valid for this tool family
                if (breakTag != null && breakTag.isTagged(target.getType())) {
                    flashBreak(p, target);
                    consumeExtraDurability(hand, tool, 1);
                    safeCallDrop(p, hand, target);
                }
            }
        }
    }




    private boolean hasItemsAdder() {
        return Bukkit.getPluginManager().isPluginEnabled("ItemsAdder");
    }

    /**
     * Try to break an ItemsAdder custom block at this location.
     * @return true if it was an IA block and we handled the break; false otherwise.
     */
// replace your current tryBreakItemsAdder(...) with this version
    private boolean tryBreakItemsAdder(Player player, Block b, ItemStack tool) {
        if (!hasItemsAdder()) return false;
        try {
            dev.lone.itemsadder.api.CustomBlock cb = dev.lone.itemsadder.api.CustomBlock.byAlreadyPlaced(b);
            if (cb == null) return false;

            // Gather loot using the current tool (includeSelfBlock = true if you want the block item too)
            java.util.List<org.bukkit.inventory.ItemStack> loot = cb.getLoot(tool, true);

            // FX, remove the block from the world
            cb.playBreakEffect();
            cb.remove(); // IA will restore the base block; no need to set AIR yourself

            // Drop the items
            org.bukkit.World w = b.getWorld();
            org.bukkit.Location loc = b.getLocation();
            for (org.bukkit.inventory.ItemStack drop : loot) {
                if (drop != null && drop.getType() != org.bukkit.Material.AIR) {
                    w.dropItemNaturally(loc, drop);
                }
            }
            return true;

        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            plugin.getLogger().warning("[ExtraTools] ItemsAdder API not found / version mismatch, skipping IA break.");
        } catch (Throwable t) {
            plugin.getLogger().warning("[ExtraTools] ItemsAdder break failed: " + t.getClass().getSimpleName());
        }
        return false;
    }


    private boolean isItemsAdderBlock(Block b) {
        if (!hasItemsAdder()) return false;
        try {
            return dev.lone.itemsadder.api.CustomBlock.byAlreadyPlaced(b) != null;
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    /* ------------------------ Optional DropEdit/DropInfo integration ------------------------ */

    private boolean hasDropPlugins() {
        return Bukkit.getPluginManager().isPluginEnabled("DropEdit")
                && Bukkit.getPluginManager().isPluginEnabled("DropInfo");
    }

    private void safeCallDrop(Player player, ItemStack tool, Block block) {
        if (block == null || block.getType().isAir()) return;

        try {
            if (hasDropPlugins()) {
                callDrop(player, tool, block);
            } else {
                block.breakNaturally(tool);
            }
        } catch (NoClassDefFoundError err) {
            plugin.getLogger().warning("[ExtraTools] DropInfo/DropEdit missing; falling back to vanilla drops.");
            block.breakNaturally(tool);
        } catch (Throwable t) {
            plugin.getLogger().warning("[ExtraTools] Drop integration failed (" + t.getClass().getSimpleName() + "); falling back to vanilla.");
            block.breakNaturally(tool);
        }
    }

    private void callDrop(Player player, ItemStack hand, Block block) {
        Location loc = block.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        Material blockMat = block.getType();
        String key = KeyGetter.getKey(blockMat, (byte) 0, null);

        Collection<ItemStack> drops;

        DropConfigurationData dropConfig = Main.main.getData().getDropConfiguration();
        if (dropConfig.entityInfoMap.containsKey(key)) {
            KeyInfo keyInfo = dropConfig.entityInfoMap.get(key);
            if (keyInfo != null) {
                DropContainer dropContainer = Main.main.getDrops(keyInfo, null, Main.getDropMultiplier(player));
                drops = dropContainer.getItemDrops();
            } else {
                drops = block.getDrops(hand, player); // fallback
            }
        } else {
            drops = block.getDrops(hand, player); // fallback
        }

        drops.stream().filter(Objects::nonNull).forEach(it -> world.dropItemNaturally(loc, it));
        block.setType(Material.AIR);
    }

    /* ------------------------ Custom durability helper for extra blocks ------------------------ */

    private void consumeExtraDurability(ItemStack tool, CustomItem citem, int amount) {
        ItemMeta meta = tool.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer extra = pdc.get(ExtraTools.durabilityKey, PersistentDataType.INTEGER);
        if (extra == null) return; // no custom pool → nothing to consume

        int newExtra = Math.max(0, extra - amount);
        if (newExtra == 0) {
            pdc.remove(ExtraTools.durabilityKey);
        } else {
            pdc.set(ExtraTools.durabilityKey, PersistentDataType.INTEGER, newExtra);
        }

        // Update last lore line like onItemDamage()
        List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        if (!lore.isEmpty()) lore.remove(lore.size() - 1);
        lore.add(
                Msgs.of("<aqua>Durability: <bold>%result%/%extra%")
                        .var("result", newExtra)
                        .var("extra", citem.extraDurability())
                        .asComp()
        );
        meta.lore(lore);
        tool.setItemMeta(meta);
    }

    /* ------------------------ Hoe AOE interactions ------------------------ */

    private final Set<Block> blacklist = new HashSet<>();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack hand = event.getItem();
        if (hand == null) return;

        if (!MaterialTags.HOES.isTagged(hand.getType())) return;

        CustomItem item = plugin.item(hand);
        if (!(item instanceof CustomTool tool)) return;

        int radius = tool.radius();
        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        if (blacklist.contains(clicked)) {
            blacklist.remove(clicked);
            return;
        }

        // Coarse dirt -> dirt
        if (clicked.getType() == Material.COARSE_DIRT) {
            apply(clicked, radius, true, false, true, relative -> {
                if (relative.getType() != Material.COARSE_DIRT) return;
                blacklist.add(relative);
                PlayerInteractEvent synthetic = new PlayerInteractEvent(event.getPlayer(), event.getAction(), hand, relative, event.getBlockFace(), event.getHand());
                Bukkit.getPluginManager().callEvent(synthetic);
                if (!synthetic.isCancelled()) {
                    relative.setType(Material.DIRT);
                }
            });
        }

        // Rooted dirt -> drop hanging roots + dirt
        if (clicked.getType() == Material.ROOTED_DIRT) {
            apply(clicked, radius, true, false, true, relative -> {
                if (relative.getType() != Material.ROOTED_DIRT) return;
                blacklist.add(relative);
                PlayerInteractEvent synthetic = new PlayerInteractEvent(event.getPlayer(), event.getAction(), hand, relative, event.getBlockFace(), event.getHand());
                Bukkit.getPluginManager().callEvent(synthetic);
                if (!synthetic.isCancelled()) {
                    Location loc = relative.getLocation();
                    loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.HANGING_ROOTS));
                    relative.setType(Material.DIRT);
                }
            });
        }

        // Tilling: dirt/path/grass -> farmland
        EnumSet<Material> tills = EnumSet.of(Material.DIRT, Material.DIRT_PATH, Material.GRASS_BLOCK);
        if (tills.contains(clicked.getType())) {
            apply(clicked, radius, true, false, true, relative -> {
                if (!tills.contains(relative.getType())) return;
                blacklist.add(relative);
                PlayerInteractEvent synthetic = new PlayerInteractEvent(event.getPlayer(), event.getAction(), hand, relative, event.getBlockFace(), event.getHand());
                Bukkit.getPluginManager().callEvent(synthetic);
                if (!synthetic.isCancelled()) {
                    relative.setType(Material.FARMLAND);
                }
            });
        }
    }

    private void apply(Block center, int radius, boolean isX, boolean isY, boolean isZ, Consumer<Block> consumer) {
        for (int i = -radius; i <= radius; i++) {
            for (int j = -radius; j <= radius; j++) {
                int x = isX ? i : 0;
                int y = isY ? (isX ? j : i) : 0;
                int z = isZ ? j : 0;
                Block relative = center.getRelative(x, y, z);
                if (!relative.equals(center)) consumer.accept(relative);
            }
        }
    }

    /* ------------------------ Held / break / drop / craft events ------------------------ */

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        PlayerInventory inv = player.getInventory();
        ItemStack newItem = inv.getItem(event.getNewSlot());
        ItemStack oldItem = inv.getItem(event.getPreviousSlot());

        CustomItem newCustom = plugin.item(newItem);
        CustomItem oldCustom = plugin.item(oldItem);

        if (oldCustom != null && !(oldCustom instanceof CustomArmor)) oldCustom.clearEffects(player);
        if (newCustom != null && !(newCustom instanceof CustomArmor)) newCustom.applyEffects(player);

        if (oldCustom instanceof CustomTool && !(newCustom instanceof CustomTool)) {
            player.removePotionEffect(PotionEffectType.HASTE);
        }
        if (newCustom instanceof CustomTool && !(oldCustom instanceof CustomTool)) {
            plugin.applyHaste(player, newCustom);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemBreak(PlayerItemBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack broken = event.getBrokenItem();
        CustomItem customItem = plugin.item(broken);
        if (customItem != null) customItem.clearEffects(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack dropped = event.getItemDrop().getItemStack();
        CustomItem customItem = plugin.item(dropped);
        if (customItem != null) customItem.clearEffects(event.getPlayer());
    }

    @EventHandler
    public void onPrepare(PrepareItemCraftEvent event) {
        CraftingInventory inventory = event.getInventory();
        if (event.getViewers().isEmpty()) return;

        Player player = (Player) event.getViewers().get(0);
        ItemStack[] matrix = inventory.getMatrix();
        CustomItem customItem = plugin.itemByRecipe(matrix);
        if (customItem != null) {
            String perm = customItem.recipePerm();
            if (perm == null || player.hasPermission(perm)) {
                inventory.setResult(customItem.item());
            }
        }
    }

    private Tag<Material> fromTool(String tool) {
        return Bukkit.getTag("blocks", NamespacedKey.minecraft("mineable/" + tool.toLowerCase()), Material.class);
    }

    @EventHandler
    public void onDrop(BlockDropItemEvent event) {
        if (!Cfgs.of().get("debug.logBlockDrops", false)) return; // default off
        plugin.getLogger().info("[debug] drop: " + event.getBlockState().getType());
    }

}
