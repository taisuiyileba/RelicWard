package cn.suiyi.relicward.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

@OnlyIn(Dist.CLIENT)
public final class BellArmorClient implements IClientItemExtensions {
    public static final BellArmorClient INSTANCE = new BellArmorClient();
    private static boolean initialized = false;
    private static BellArmorModel ARMOR_MODEL;
    private static BellArmorModel LEGS_MODEL;

    private BellArmorClient() {}
    public static void resetModels(){initialized=false;ARMOR_MODEL=null;LEGS_MODEL=null;}

    private static void initModels() {
        if (!initialized) {
            EntityModelSet modelSet = Minecraft.getInstance().getEntityModels();
            ARMOR_MODEL = new BellArmorModel(modelSet.bakeLayer(BellArmorModel.LAYER));
            LEGS_MODEL = new BellArmorModel(modelSet.bakeLayer(BellArmorModel.LAYER_LEGS));
            initialized = true;
        }
    }

    @Override
    public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
        initModels();
        if (equipmentSlot == EquipmentSlot.LEGS) {
            return LEGS_MODEL;
        }
        return ARMOR_MODEL;
    }
}
