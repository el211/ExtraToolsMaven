package fr.elias.extraTools.customs.impl;


import fr.elias.extraTools.customs.CustomItem;
import org.bukkit.Material;

public class CustomTool extends CustomItem {

    private final String type = "tool";
    private final boolean vertical;
    private final int radius;

    // For item
    private CustomTool() {
        super();
        this.radius = 0;
        this.vertical = false;
    }

    public CustomTool(String name, String lore, Material type, int extraDurability, int extraSpeed, int radius, boolean vertical, int customModelData) {
        super(name, lore, type, extraDurability, extraSpeed, customModelData);
        this.vertical = vertical;
        this.radius = radius;
    }

    public int radius() {
        return radius;
    }

    public boolean vertical() {
        return vertical;
    }

}

