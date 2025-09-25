package fr.elias.extraTools.customs;


import com.google.common.collect.Sets;
import fr.elias.extraTools.ExtraTools;
import fr.elias.extraTools.utils.config.Msgs;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CustomItem {

    private final Set<PotionEffect> effects = Sets.newHashSet();
    private final int extraDurability;
    private final Material material;
    private final String loreLine;
    private final int extraSpeed;
    private String recipePerm;
    private final String name;
    private String recipe;
    private final int id;
    private final int customModelData; // New field for custom model data

    public CustomItem() {
        extraDurability = 0;
        material = null;
        extraSpeed = 0;
        loreLine = "";
        name = "";
        id = -1;
        customModelData = 0; // Default value

    }

    public CustomItem(String name, String lore, Material material, int extraDurability, int extraSpeed, int customModelData) {
        this.name = name;
        this.loreLine = lore;
        this.material = material;
        this.extraSpeed = extraSpeed;
        this.extraDurability = extraDurability;
        this.id = ExtraTools.instance().getId();
        this.customModelData = customModelData; // Assign custom model data

    }

    public ItemStack item() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Msgs.of(name).asComp());

        List<String> lore = Arrays.stream(Msgs.of(loreLine).asLegacy().split("/n")).collect(Collectors.toList());
        meta.getPersistentDataContainer().set(ExtraTools.itemNameKey, PersistentDataType.INTEGER, id());

        if (extraDurability > 0) {
            meta.getPersistentDataContainer().set(ExtraTools.durabilityKey, PersistentDataType.INTEGER, extraDurability);
            lore.add(Msgs.of("<aqua>Durability: <bold>%extra%/%extra%").var("extra", extraDurability).asLegacy());
        }

        if (customModelData > 0) { // Add custom model data if specified
            meta.setCustomModelData(customModelData);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public Set<PotionEffect> effects() {
        return effects;
    }

    public void removeEffect(PotionEffectType type) {
        effects.removeIf(e -> e.getType() == type);
    }

    public void addEffect(PotionEffect effect) {
        effects.add(effect);
    }

    public void clearEffects(Player player) {
        effects.stream().map(PotionEffect::getType).forEach(player::removePotionEffect);
    }
    public int getCustomModelData() {
        return customModelData;
    }

    public String recipe() {
        return recipe;
    }

    public String recipePerm() {
        return recipePerm;
    }

    public void recipe(String recipe) {
        this.recipe = recipe;
    }

    public void recipePerm(String recipePerm) {
        this.recipePerm = recipePerm;
    }

    public void applyEffects(Player player) {
        effects.stream().map(e -> e.withDuration(300)).forEach(player::addPotionEffect);
    }

    public String strippedName() {
        return Msgs.of(name).asPlain().trim();
    }

    public int extraSpeed() {
        return extraSpeed;
    }

    public String name() {
        return name;
    }

    public Material material() {
        return material;
    }

    public int extraDurability() {
        return extraDurability;
    }

    public int id() {
        return id;
    }

}

