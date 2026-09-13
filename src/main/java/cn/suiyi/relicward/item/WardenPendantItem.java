package cn.suiyi.relicward.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class WardenPendantItem extends Item implements ICurioItem {
    public WardenPendantItem(){super(new Item.Properties().stacksTo(1));}
    @Override public boolean canEquip(SlotContext context,ItemStack stack){return context.identifier().equals("necklace");}
    @Override public boolean canEquipFromUse(SlotContext context,ItemStack stack){return true;}
    @Override public void onEquip(SlotContext context,ItemStack previous,ItemStack stack){if(context.entity() instanceof Player p)RelicEquipment.resetPendantWarmup(p);}
    @Override public void onUnequip(SlotContext context,ItemStack replacement,ItemStack stack){if(context.entity() instanceof Player p)RelicEquipment.resetPendantWarmup(p);}
}
