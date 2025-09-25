package fr.elias.extraTools.utils;


import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.inventory.ItemStack;

import java.util.Base64;

public class InventorySave {

    public static String toBase64(ItemStack[] items) {
        ByteArrayDataOutput os = ByteStreams.newDataOutput();

        os.writeInt(items.length);
        for (ItemStack item : items) {
            if (item != null) {
                byte[] itemBytes = item.serializeAsBytes();
                os.writeInt(itemBytes.length);
                os.write(itemBytes);
            } else {
                os.writeInt(-1);
            }
        }

        return Base64.getEncoder().encodeToString(os.toByteArray());
    }

    public static ItemStack[] fromBase64(String data) {

        ByteArrayDataInput os = ByteStreams.newDataInput(Base64.getDecoder().decode(data));
        int size = os.readInt();
        ItemStack[] items = new ItemStack[size];

        for (int i = 0; i < size; i++) {
            int arrSize = os.readInt();
            if (arrSize == -1) {
                items[i] = null;
            } else {
                byte[] arr = new byte[arrSize];
                os.readFully(arr);
                items[i] = ItemStack.deserializeBytes(arr);
            }
        }

        return items;
    }

}
