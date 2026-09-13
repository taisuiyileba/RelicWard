package cn.suiyi.relicward.test;

import cn.suiyi.relicward.RelicWard;
import cn.suiyi.relicward.combat.*;
import cn.suiyi.relicward.entity.BellWarden;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.gametest.*;

@GameTestHolder(RelicWard.ID) @PrefixGameTestTemplate(false)
public final class GravityTests {
    @GameTest(template="arena_test",timeoutTicks=150,batch="gravity")
    public static void phaseTwoGravityLifecycle(GameTestHelper h){
        var l=h.getLevel();l.getServer().setDifficulty(Difficulty.NORMAL,true);
        var p=FakePlayerFactory.get(l,new GameProfile(UUID.randomUUID(),"GravityTest"));p.setGameMode(GameType.SURVIVAL);
        var at=h.absolutePos(new BlockPos(26,2,34));p.moveTo(at.getX(),at.getY(),at.getZ(),0,0);p.setInvulnerable(true);l.addNewPlayer(p);
        var boss=h.spawn(RelicWard.BELL_WARDEN.get(),26,2,26);
        double speed=p.getAttributeValue(Attributes.MOVEMENT_SPEED),gravity=p.getAttributeValue(ForgeMod.ENTITY_GRAVITY.get());
        h.assertTrue(boss.startEncounter(),"Encounter starts");SuperGravity.updateAura(p);h.assertTrue(!p.hasEffect(SuperGravity.EFFECT.get()),"Phase one has no gravity");
        boss.setAction(WardenAction.TRANSFORM);
        h.runAfterDelay(56,()->{
            h.assertTrue(boss.phase()==2,"Real transformation reaches phase two");boss.setNoAi(true);
            SuperGravity.updateAura(p);h.assertTrue(p.hasEffect(SuperGravity.EFFECT.get()),"Phase two applies aura");
            h.assertTrue(Math.abs(p.getAttributeValue(Attributes.MOVEMENT_SPEED)-speed*.60)<1e-6,"Movement reduced by 40 percent");
            h.assertTrue(Math.abs(p.getAttributeValue(ForgeMod.ENTITY_GRAVITY.get())-gravity*1.6)<1e-6,"Gravity increased by 60 percent");
            p.setDeltaMovement(.1,.9,.2);SuperGravity.jump(new LivingEvent.LivingJumpEvent(p));
            h.assertTrue(Math.abs(p.getDeltaMovement().y-.38)<1e-6&&Math.abs(p.getDeltaMovement().x-.1)<1e-6,"Jump boost capped without changing horizontal momentum");
            double y=0,v=.38,peak=0;for(int i=0;i<20;i++){y+=v;peak=Math.max(peak,y);v=(v-gravity*1.6)*.98;}
            h.assertTrue(peak>.65&&peak<.9,"Reduced jump still clears the existing ground wave");
            p.getAbilities().mayfly=true;p.getAbilities().flying=true;SuperGravity.suppressFlight(p);
            h.assertTrue(!p.getAbilities().flying&&p.getAbilities().mayfly&&p.getDeltaMovement().y<0,"Flight suppressed without revoking permission");
            p.startFallFlying();SuperGravity.suppressFlight(p);h.assertTrue(!p.isFallFlying(),"Elytra glide suppressed");
            p.removeEffect(SuperGravity.EFFECT.get());SuperGravity.updateAura(p);h.assertTrue(p.hasEffect(SuperGravity.EFFECT.get()),"Active field reapplies cleansed debuff");
            for(var mode:new GameType[]{GameType.CREATIVE,GameType.SPECTATOR,GameType.ADVENTURE}){
                p.setGameMode(mode);SuperGravity.updateAura(p);p.getAbilities().flying=true;SuperGravity.suppressFlight(p);
                h.assertTrue(!p.hasEffect(SuperGravity.EFFECT.get())&&p.getAbilities().flying,"Other modes immune: "+mode);
            }
            p.setGameMode(GameType.SURVIVAL);p.getAbilities().mayfly=false;p.getAbilities().flying=false;
            p.setPos(at.getX()+70,at.getY(),at.getZ());SuperGravity.updateAura(p);h.assertTrue(!p.hasEffect(SuperGravity.EFFECT.get()),"Leaving field removes aura");
            h.assertTrue(Math.abs(p.getAttributeValue(Attributes.MOVEMENT_SPEED)-speed)<1e-6&&Math.abs(p.getAttributeValue(ForgeMod.ENTITY_GRAVITY.get())-gravity)<1e-6,"Attributes restore exactly");
            p.setPos(at.getX(),at.getY(),at.getZ());SuperGravity.updateAura(p);h.assertTrue(p.hasEffect(SuperGravity.EFFECT.get()),"Reentry applies aura");
            boss.resetEncounter();SuperGravity.updateAura(p);h.assertTrue(!p.hasEffect(SuperGravity.EFFECT.get())&&!p.getAbilities().mayfly,"Reset clears gravity and never grants flight");
            p.setPos(at.getX()+1000,at.getY(),at.getZ()+1000);boss.discard();h.succeed();
        });
    }
}
