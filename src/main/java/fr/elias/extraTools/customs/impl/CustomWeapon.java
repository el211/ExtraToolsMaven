package fr.elias.extraTools.customs.impl;


import fr.elias.extraTools.customs.CustomItem;
import org.bukkit.Material;

public class CustomWeapon extends CustomItem {

    private final String type = "weapon";
    private final double extraDamage;

    // For weapon
    private CustomWeapon() {
        super();
        extraDamage = 10.0D;
    }

    public CustomWeapon(String name, String lore, Material material, int extraDurability, int extraSpeed, double extraDamage, int customModelData) {
        super(name, lore, material, extraDurability, extraSpeed, customModelData);
        this.extraDamage = extraDamage;
    }

    public double extraDamage() {
        return extraDamage;
    }

}

