package cn.suiyi.relicward.item;

import cn.suiyi.relicward.RelicContent;
import cn.suiyi.relicward.RelicWard;
import cn.suiyi.relicward.combat.CombatMath;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A heavy tool, not a sword/trident: 18-tick charge, 10-tick downswing, contact, 10-tick recovery; early release cancels. */
public final class EchoMaulItem extends TieredItem {
    public static final int CHARGE_TICKS=18,DOWN_TICKS=10,CONTACT_TICK=CHARGE_TICKS+DOWN_TICKS,END_TICK=CONTACT_TICK+10,COOLDOWN=100;
    private final Multimap<Attribute,AttributeModifier> attributes=ImmutableMultimap.<Attribute,AttributeModifier>builder()
        .put(Attributes.ATTACK_DAMAGE,new AttributeModifier(BASE_ATTACK_DAMAGE_UUID,"Maul damage",10,AttributeModifier.Operation.ADDITION))
        .put(Attributes.ATTACK_SPEED,new AttributeModifier(BASE_ATTACK_SPEED_UUID,"Maul speed",-2.9,AttributeModifier.Operation.ADDITION)).build();
    public EchoMaulItem(){super(Tiers.IRON,new Item.Properties().durability(960));}
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){consumer.accept(cn.suiyi.relicward.client.MaulChargeClient.INSTANCE);}
    @Override public Multimap<Attribute,AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot){return slot==EquipmentSlot.MAINHAND?attributes:super.getDefaultAttributeModifiers(slot);}
    @Override public int getUseDuration(ItemStack stack){return 72000;}
    @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.NONE;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        ItemStack stack=player.getItemInHand(hand);
        if(hand!=InteractionHand.MAIN_HAND||player.getCooldowns().isOnCooldown(this)||(!level.isClientSide&&RelicEquipment.cooldown(player,"Maul")>0))return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);
        if(!level.isClientSide)level.playSound(null,player.blockPosition(),SoundEvents.ARMOR_EQUIP_IRON,SoundSource.PLAYERS,.5F,.65F);
        return InteractionResultHolder.consume(stack);
    }
    @Override public InteractionResult useOn(UseOnContext context){
        if(context.getPlayer()==null)return InteractionResult.PASS;
        return use(context.getLevel(),context.getPlayer(),context.getHand()).getResult();
    }
    @Override public void onUseTick(Level level,LivingEntity user,ItemStack stack,int remaining){
        if(!(user instanceof Player player)||level.isClientSide)return;
        int charged=getUseDuration(stack)-remaining;
        if(charged<CHARGE_TICKS){if(charged%4==0)player.displayClientMessage(Component.translatable("message.relicward.maul_charge",Math.min(100,charged*100/CHARGE_TICKS)),true);return;}
        if(charged>=CONTACT_TICK&&RelicEquipment.cooldown(player,"Maul")==0)slam(level,player,stack);
        if(charged>=END_TICK)player.stopUsingItem();
    }
    @Override public void releaseUsing(ItemStack stack,Level level,LivingEntity user,int remaining){
        if(user instanceof Player p&&!level.isClientSide){
            if(getUseDuration(stack)-remaining>=CONTACT_TICK)slam(level,p,stack);
            else p.displayClientMessage(Component.translatable("message.relicward.maul_cancel"),true);
        }
    }
    private Vec3 groundPoint(Level level,Player player){
        Vec3 ahead=player.position().add(CombatMath.forward(player.getYRot()).scale(1.8));
        BlockPos top=BlockPos.containing(ahead).above();
        for(int d=0;d<5;d++){
            BlockPos p=top.below(d);var shape=level.getBlockState(p).getCollisionShape(level,p);
            if(!shape.isEmpty())return new Vec3(ahead.x,p.getY()+shape.max(net.minecraft.core.Direction.Axis.Y),ahead.z);
        }return null;
    }
    private void slam(Level level,Player player,ItemStack stack){
        if(!(level instanceof ServerLevel server)||!player.isAlive()||player.getMainHandItem()!=stack||RelicEquipment.cooldown(player,"Maul")>0)return;
        Vec3 impact=groundPoint(level,player);
        if(impact==null){player.displayClientMessage(Component.translatable("message.relicward.maul_ground"),true);return;}
        RelicEquipment.setCooldown(player,"Maul",COOLDOWN);player.getCooldowns().addCooldown(this,COOLDOWN);
        for(var target:level.getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().inflate(5,2,5))){
            if(!cn.suiyi.relicward.combat.PlayerCombat.canHit(player,target)||!player.hasLineOfSight(target)||Math.abs(target.getY()-player.getY())>2.5)continue;
            if(target instanceof Player other&&(!server.getServer().isPvpAllowed()||!player.canHarmPlayer(other)))continue;
            float damage=14+net.minecraft.world.item.enchantment.EnchantmentHelper.getDamageBonus(stack,target.getMobType());
            if(CombatMath.cone(player.position(),player.getYRot(),target.position(),5,120)&&target.hurt(level.damageSources().playerAttack(player),damage)){
                cn.suiyi.relicward.combat.ArmorFracture.apply(target,100);target.knockback(.7,player.getX()-target.getX(),player.getZ()-target.getZ());
            }
        }
        var pulse=RelicWard.MAUL_PULSE.get().create(level);if(pulse!=null){pulse.moveTo(impact.x,impact.y+.01,impact.z,player.getYRot(),0);level.addFreshEntity(pulse);}
        server.sendParticles(ParticleTypes.CRIT,impact.x,impact.y+.15,impact.z,22,1,.1,1,.15);
        level.playSound(null,player.blockPosition(),SoundEvents.ANVIL_LAND,SoundSource.PLAYERS,.7F,.65F);
        player.displayClientMessage(Component.translatable("message.relicward.maul_slam"),true);
        stack.hurtAndBreak(3,player,p->p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
    }
    @Override public boolean hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker){stack.hurtAndBreak(1,attacker,p->p.broadcastBreakEvent(EquipmentSlot.MAINHAND));return true;}
    @Override public boolean isValidRepairItem(ItemStack stack,ItemStack ingredient){return ingredient.is(RelicContent.FRAGMENT.get());}
    @Override public boolean canApplyAtEnchantingTable(ItemStack stack,net.minecraft.world.item.enchantment.Enchantment enchantment){return enchantment instanceof net.minecraft.world.item.enchantment.DamageEnchantment||enchantment==net.minecraft.world.item.enchantment.Enchantments.MOB_LOOTING||enchantment==net.minecraft.world.item.enchantment.Enchantments.UNBREAKING||enchantment==net.minecraft.world.item.enchantment.Enchantments.MENDING;}
}
