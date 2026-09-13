package cn.suiyi.relicward.item;

import cn.suiyi.relicward.RelicWard;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class BellArmorItem extends ArmorItem {
    public BellArmorItem(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(cn.suiyi.relicward.client.BellArmorClient.INSTANCE);
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        if (slot == EquipmentSlot.LEGS) {
            return RelicWard.ID + ":textures/models/armor/bell_layer_2.png";
        }
        return RelicWard.ID + ":textures/models/armor/bell_layer_1.png";
    }
}
