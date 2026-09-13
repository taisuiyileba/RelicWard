package cn.suiyi.relicward.combat;

import cn.suiyi.relicward.RelicWard;
import cn.suiyi.relicward.entity.BellWarden;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;

/** Short-lived, server-authoritative encounter aura; never revokes mayfly permissions. */
@Mod.EventBusSubscriber(modid=RelicWard.ID)
public final class SuperGravity {
    public static final DeferredRegister<MobEffect> EFFECTS=DeferredRegister.create(ForgeRegistries.MOB_EFFECTS,RelicWard.ID);
    public static final RegistryObject<MobEffect> EFFECT=EFFECTS.register("super_gravity",()->new GravityEffect());
    public static final double RADIUS=24, HEIGHT=48, JUMP_VELOCITY=.38;

    private static final class GravityEffect extends MobEffect {
        GravityEffect(){
            super(MobEffectCategory.HARMFUL,0x8062B4);
            addAttributeModifier(Attributes.MOVEMENT_SPEED,"433f48f2-20f4-41c1-bffd-b74f4d31b012",-.40,AttributeModifier.Operation.MULTIPLY_TOTAL);
            addAttributeModifier(ForgeMod.ENTITY_GRAVITY.get(),"607c29a9-db72-4626-8e6e-947b63a70f62",.60,AttributeModifier.Operation.MULTIPLY_TOTAL);
        }
    }
    public static boolean survival(Player p){
        if(p instanceof ServerPlayer sp)return sp.gameMode.getGameModeForPlayer()==GameType.SURVIVAL;
        return !p.isCreative()&&!p.isSpectator()&&p.getAbilities().mayBuild;
    }
    public static boolean inField(Player p,BellWarden w){
        return w.isAlive()&&w.isEncounterActive()&&w.phase()==2
                &&CombatMath.horizontalSquared(p.position(),w.arenaCenter())<=RADIUS*RADIUS
                &&Math.abs(p.getY()-w.arenaCenter().y)<=HEIGHT;
    }
    public static void updateAura(ServerPlayer p){
        boolean active=p.isAlive()&&survival(p)&&p.level().getEntitiesOfClass(BellWarden.class,
                p.getBoundingBox().inflate(RADIUS+20,HEIGHT+20,RADIUS+20),w->inField(p,w)).size()>0;
        var current=p.getEffect(EFFECT.get());
        if(active){
            if(current==null){p.displayClientMessage(Component.translatable("message.relicward.super_gravity"),true);}
            if(current==null||current.getDuration()<=20)p.addEffect(new MobEffectInstance(EFFECT.get(),30,0,false,false,true));
        }else if(current!=null)p.removeEffect(EFFECT.get());
    }
    public static void suppressFlight(Player p){
        if(!survival(p)||!p.hasEffect(EFFECT.get()))return;
        boolean flying=p.getAbilities().flying,gliding=p.isFallFlying();
        if(flying){p.getAbilities().flying=false;if(p instanceof ServerPlayer sp)sp.onUpdateAbilities();}
        if(gliding)p.stopFallFlying();
        if(flying||gliding||p.hasEffect(MobEffects.LEVITATION)){
            var velocity=p.getDeltaMovement();p.setDeltaMovement(velocity.x,Math.min(velocity.y,-.30),velocity.z);p.hasImpulse=true;
        }
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){
        if(e.phase==TickEvent.Phase.START&&e.player instanceof ServerPlayer p
                &&(p.tickCount%5==0||(!survival(p)&&p.hasEffect(EFFECT.get()))))updateAura(p);
        // Both sides: prediction and server authority. Never change creative flight or grant mayfly on exit.
        suppressFlight(e.player);
    }
    @SubscribeEvent public static void jump(LivingEvent.LivingJumpEvent e){
        if(e.getEntity() instanceof Player p&&survival(p)&&p.hasEffect(EFFECT.get())){
            var velocity=p.getDeltaMovement();p.setDeltaMovement(velocity.x,Math.min(velocity.y,JUMP_VELOCITY),velocity.z);p.hasImpulse=true;
        }
    }
    @SubscribeEvent public static void applicable(MobEffectEvent.Applicable e){
        if(e.getEffectInstance().getEffect()==EFFECT.get()
                &&(!(e.getEntity() instanceof Player p)||!survival(p)))e.setResult(Event.Result.DENY);
    }
}
