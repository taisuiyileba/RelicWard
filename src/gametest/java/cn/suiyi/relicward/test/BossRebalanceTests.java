package cn.suiyi.relicward.test;

import cn.suiyi.relicward.*;
import cn.suiyi.relicward.combat.WardenAction;
import cn.suiyi.relicward.entity.*;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;

@GameTestHolder(RelicWard.ID) @PrefixGameTestTemplate(false)
public final class BossRebalanceTests {
    private static BellWarden encounter(GameTestHelper h){
        var l=h.getLevel();l.getServer().setDifficulty(Difficulty.NORMAL,true);
        var p=FakePlayerFactory.get(l,new GameProfile(UUID.randomUUID(),"Rebalance"));p.setGameMode(GameType.SURVIVAL);
        var pos=h.absolutePos(new BlockPos(26,2,34));p.moveTo(pos.getX(),pos.getY(),pos.getZ(),0,0);p.setInvulnerable(true);l.addNewPlayer(p);
        var boss=h.spawn(RelicWard.BELL_WARDEN.get(),26,2,26);h.assertTrue(boss.startEncounter(),"Encounter starts");return boss;
    }
    @GameTest(template="arena_test",timeoutTicks=100,batch="rebalance")
    public static void reinforcementLifecycle(GameTestHelper h){
        var boss=encounter(h);boss.setAction(WardenAction.TRANSFORM);
        h.runAfterDelay(55,()->{
            boss.setNoAi(true);
            var summons=h.getLevel().getEntitiesOfClass(CourtPuppet.class,boss.getBoundingBox().inflate(24),p->p.isReinforcementOf(boss));
            h.assertTrue(summons.size()==4,"Exactly four copper reinforcements spawn on real transformation");
            var player=(net.minecraft.world.entity.player.Player)boss.getTarget();
            player.setInvulnerable(false);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ChapterContent.BINDING_SEAL.get()));
            for(var p:summons){
                p.setNoAi(true);
                h.assertTrue(p.getMaxHealth()==36&&p.getExperienceReward()==0,"Summon stats and no XP farming");
                h.assertTrue(p.canAttack(player)&&!p.canAttack(boss)&&!p.canAttack(summons.get(0)),"Targets players, never commander or fellow reinforcements");
                h.assertTrue(h.getLevel().noCollision(p),"Summon has clear space");
                p.mobInteract(player,InteractionHand.MAIN_HAND);
                h.assertTrue(!p.isTame()&&player.getMainHandItem().getCount()==1,"Reinforcements cannot be captured; seal is not spent");
            }
            player.setInvulnerable(true);
            boss.setNoAi(false);boss.setAction(WardenAction.TRANSFORM);
            // A stale/deserialized reinforcement from another encounter must self-remove.
            CompoundTag saved=new CompoundTag();summons.get(0).addAdditionalSaveData(saved);saved.putUUID("Encounter",UUID.randomUUID());
            var orphan=h.spawn(RelicWard.COURT_PUPPET.get(),8,2,8);orphan.readAdditionalSaveData(saved);
            h.runAfterDelay(3,()->h.assertTrue(orphan.isRemoved(),"Reloaded stale summon is removed"));
            h.runAfterDelay(8,()->{
                h.assertTrue(h.getLevel().getEntitiesOfClass(CourtPuppet.class,boss.getBoundingBox().inflate(24),p->p.isReinforcementOf(boss)).size()==4,"No per-tick duplicate summons");
                boss.resetEncounter();h.assertTrue(summons.stream().allMatch(CourtPuppet::isRemoved),"Reset immediately removes all reinforcements");boss.discard();h.succeed();
            });
        });
    }
    @GameTest(template="arena_test",timeoutTicks=120,batch="rebalance")
    public static void burstProtectionAndDeathCleanup(GameTestHelper h){
        var boss=encounter(h);boss.setAction(WardenAction.IDLE);
        var source=h.getLevel().damageSources().playerAttack((net.minecraft.world.entity.player.Player)boss.getTarget());
        boss.hurt(source,10000);h.assertTrue(Math.abs(boss.getHealth()-452.4F)<.01,"Phase one caps raw burst at 30 before armor");
        boss.invulnerableTime=0;boss.stagger();boss.hurt(source,10000);
        h.assertTrue(boss.getHealth()==240&&boss.isAlive(),"Stagger burst cannot skip half-health phase gate");
        boss.setAction(WardenAction.TRANSFORM);h.assertTrue(!boss.hurt(source,10000),"Transformation remains protected");
        h.runAfterDelay(55,()->{
            boss.setNoAi(true);boss.setAction(WardenAction.IDLE);boss.invulnerableTime=0;boss.hurt(source,10000);
            h.assertTrue(Math.abs(boss.getHealth()-217.92F)<.01,"Phase two caps raw burst at 24 before armor");
            var summons=h.getLevel().getEntitiesOfClass(CourtPuppet.class,boss.getBoundingBox().inflate(24),p->p.isReinforcementOf(boss));
            h.assertTrue(summons.size()==4,"Summons present before kill");
            boss.invulnerableTime=0;boss.stagger();boss.hurt(source,10000);
            h.assertTrue(!boss.isAlive()&&summons.stream().allMatch(CourtPuppet::isRemoved),"Exposed phase two can die and cleans summons immediately");
            boss.discard();h.succeed();
        });
    }
    @GameTest(template="arena_test",timeoutTicks=120,batch="reinforcement_attack")
    public static void reinforcementAttacksCompanion(GameTestHelper h){
        var boss=encounter(h);boss.setAction(WardenAction.TRANSFORM);
        h.runAfterDelay(55,()->{
            boss.setNoAi(true);
            var summons=h.getLevel().getEntitiesOfClass(CourtPuppet.class,boss.getBoundingBox().inflate(24),p->p.isReinforcementOf(boss));
            h.assertTrue(summons.size()==4,"Four reinforcements exist");summons.forEach(p->p.setNoAi(true));
            var attacker=summons.get(0);var pet=h.spawn(RelicWard.COURT_PUPPET.get(),26,2,34);
            pet.tame((net.minecraft.world.entity.player.Player)boss.getTarget());pet.setNoAi(true);
            pet.moveTo(attacker.getX(),attacker.getY(),attacker.getZ()+1.5,0,0);attacker.setYRot(0);attacker.beginStrike(pet,false);
            h.runAfterDelay(14,()->{
                h.assertTrue(pet.getHealth()==pet.getMaxHealth()-6,"Summoned copper puppet executes its six-damage contact hit");
                boss.discard();h.assertTrue(summons.stream().allMatch(CourtPuppet::isRemoved),"Discard cleans reinforcements");pet.discard();h.succeed();
            });
        });
    }
}
