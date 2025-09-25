package fr.elias.extraTools.customs.impl;


import fr.elias.extraTools.ExtraTools;
import fr.elias.extraTools.customs.CustomItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CustomArmor extends CustomItem {

    private final String type = "armor";
    private final double extraHearts;
    private final double extraArmor;

    // For gson
    private CustomArmor() {
        super();
        this.extraHearts = 0;
        this.extraArmor = 0;
    }

    public CustomArmor(String name, String lore, Material material, int extraDurability, int extraSpeed,
                       double extraHearts, double extraArmor, int customModelData) {
        super(name, lore, material, extraDurability, extraSpeed, customModelData);
        this.extraHearts = extraHearts;
        this.extraArmor = extraArmor;
    }

    @Override
    public ItemStack item() {
        ItemStack item = super.item();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        EquipmentSlot slot = resolveSlot(material());
        EquipmentSlotGroup group = toGroup(slot);

        // ✅ Non-deprecated 1.21+ constructors use NamespacedKey
        AttributeModifier healthMod = new AttributeModifier(
                new NamespacedKey(ExtraTools.instance(), "extra_health"),
                extraHearts,
                AttributeModifier.Operation.ADD_NUMBER,
                group
        );

        AttributeModifier armorMod = new AttributeModifier(
                new NamespacedKey(ExtraTools.instance(), "extra_armor"),
                extraArmor,
                AttributeModifier.Operation.ADD_NUMBER,
                group
        );

        meta.addAttributeModifier(Attribute.MAX_HEALTH, healthMod);
        meta.addAttributeModifier(Attribute.ARMOR, armorMod);
        item.setItemMeta(meta);
        return item;
    }

    private static EquipmentSlot resolveSlot(Material mat) {
        if (mat == null) return null;
        String name = mat.name();
        if (name.endsWith("_HELMET"))     return EquipmentSlot.HEAD;
        if (name.endsWith("_CHESTPLATE")) return EquipmentSlot.CHEST;
        if (name.endsWith("_LEGGINGS"))   return EquipmentSlot.LEGS;
        if (name.endsWith("_BOOTS"))      return EquipmentSlot.FEET;
        return null; // not armor -> ANY group fallback
    }

    private static EquipmentSlotGroup toGroup(EquipmentSlot slot) {
        if (slot == null) return EquipmentSlotGroup.ANY;
        return switch (slot) {
            case HEAD     -> EquipmentSlotGroup.HEAD;
            case CHEST    -> EquipmentSlotGroup.CHEST;
            case LEGS     -> EquipmentSlotGroup.LEGS;
            case FEET     -> EquipmentSlotGroup.FEET;
            case HAND     -> EquipmentSlotGroup.MAINHAND;
            case OFF_HAND -> EquipmentSlotGroup.OFFHAND;
            default       -> EquipmentSlotGroup.ANY;
        };
    }

    public double extraHearts() {
        return extraHearts;
    }
}

