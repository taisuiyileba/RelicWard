package cn.suiyi.relicward.item;

import cn.suiyi.relicward.RelicContent;
import cn.suiyi.relicward.RelicWard;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=RelicWard.ID)
public final class RelicEquipment {
    private static CompoundTag data(Player player) {
        var root=player.getPersistentData();
        if(!root.contains("RelicEquipment"))root.put("RelicEquipment",new CompoundTag());
        return root.getCompound("RelicEquipment");
    }
    private static long time(Player p) { return p.level().getServer()==null?p.level().getGameTime():p.level().getServer().overworld().getGameTime(); }
    public static long cooldown(Player p,String key) { return Math.max(0,data(p).getLong(key)-time(p)); }
    public static void setCooldown(Player p,String key,int ticks) { data(p).putLong(key,time(p)+ticks); }
    public static boolean pendantEquipped(Player p){
        return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).map(h->h.findFirstCurio(RelicContent.PENDANT.get()).isPresent()).orElse(false);
    }
    public static void resetPendantWarmup(Player p){if(!p.level().isClientSide)data(p).putInt("Equipped",0);}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||event.player.level().isClientSide)return;
        var player=event.player;var tag=data(player);
        tag.putInt("Equipped",pendantEquipped(player)?Math.min(60,tag.getInt("Equipped")+1):0);
        if(player.tickCount%20==0) {
            long wave=cooldown(player,"Wave");
            if(wave>0&&!player.getCooldowns().isOnCooldown(RelicContent.WAVE_BELL.get()))player.getCooldowns().addCooldown(RelicContent.WAVE_BELL.get(),(int)wave);
            long remain=cooldown(player,"Maul");
            if(remain>0&&!player.getCooldowns().isOnCooldown(RelicContent.MAUL.get()))player.getCooldowns().addCooldown(RelicContent.MAUL.get(),(int)remain);
        }
    }
    @SubscribeEvent public static void absorb(LivingDamageEvent event) {
        if(!(event.getEntity() instanceof Player p)||p.level().isClientSide||event.getAmount()<6||!(event.getSource().getEntity() instanceof LivingEntity))return;
        if(!pendantEquipped(p)||data(p).getInt("Equipped")<60||cooldown(p,"Pendant")>0)return;
        event.setAmount(event.getAmount()*.7F);setCooldown(p,"Pendant",500);
        p.getCooldowns().addCooldown(RelicContent.PENDANT.get(),500);p.playSound(SoundEvents.BELL_RESONATE,.8F,1.7F);
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) {
        event.getEntity().getPersistentData().put("RelicEquipment",data(event.getOriginal()).copy());data(event.getEntity()).putInt("Equipped",0);
    }
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if(item!=null&&item.getNamespace().equals("relicward")){
            var name=item.getPath();
            if(java.util.Set.of("binding_seal","repair_paste","bell_shield","bell_anvil","resonant_bell").contains(name))event.getToolTip().add(Component.translatable("tooltip.relicward."+name));
            if(event.getItemStack().getItem() instanceof net.minecraft.world.item.ArmorItem armor&&armor.getMaterial()==BellArmorMaterial.INSTANCE)event.getToolTip().add(Component.translatable("tooltip.relicward.bell_armor"));
        }
        if(event.getItemStack().is(RelicContent.MAUL.get()))event.getToolTip().add(Component.translatable("tooltip.relicward.maul"));
        if(event.getItemStack().is(RelicContent.PENDANT.get()))event.getToolTip().add(Component.translatable("tooltip.relicward.pendant"));
        if(event.getItemStack().is(RelicContent.TROPHY.get().asItem()))event.getToolTip().add(Component.translatable("tooltip.relicward.trophy"));
    }
}
