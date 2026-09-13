package cn.suiyi.relicward.item;
import cn.suiyi.relicward.ChapterContent;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.sounds.*;
public enum BellArmorMaterial implements ArmorMaterial {
    INSTANCE;
    public int getDurabilityForType(ArmorItem.Type t){return switch(t){case HELMET->330;case CHESTPLATE->480;case LEGGINGS->450;case BOOTS->390;};}
    public int getDefenseForType(ArmorItem.Type t){return switch(t){case HELMET->3;case CHESTPLATE->7;case LEGGINGS->6;case BOOTS->2;};}
    public int getEnchantmentValue(){return 14;}
    public SoundEvent getEquipSound(){return SoundEvents.ARMOR_EQUIP_IRON;}
    public Ingredient getRepairIngredient(){return Ingredient.of(ChapterContent.PLATE.get());}
    public String getName(){return "relicward:bell";}
    public float getToughness(){return 1;}
    public float getKnockbackResistance(){return .05F;}
}
