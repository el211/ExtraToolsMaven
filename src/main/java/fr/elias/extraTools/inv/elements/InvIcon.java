package fr.elias.extraTools.inv.elements;


import fr.minuskube.inv.ClickableItem;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.function.Consumer;

public enum InvIcon {
    NEXT("Next", Material.ARROW),
    ADD("Add", Material.GREEN_WOOL),
    PREV("Prev", Material.ARROW),
    CLOSE("Close", Material.BARRIER);

    private final String displayName;
    private final Material material;

    InvIcon(String displayName, Material material) {
        this.displayName = displayName;
        this.material = material;
    }

    public ItemBuilder named(String displayName) {
        return ItemBuilder.start(this.material).name(displayName);
    }

    public ClickableItem of(Consumer<InventoryClickEvent> consumer) {
        return this.named(this.displayName).of(consumer);
    }
}

