package cn.suiyi.relicward.test;

import cn.suiyi.relicward.*;
import cn.suiyi.relicward.block.CourtAltarEntity;
import cn.suiyi.relicward.combat.WardenAction;
import cn.suiyi.relicward.reward.*;
import cn.suiyi.relicward.world.CourtProtection;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.gametest.*;

@GameTestHolder(RelicWard.ID)
@PrefixGameTestTemplate(false)
public final class ExperienceTests {
    @GameTest(template="creature_room",timeoutTicks=90)
    public static void brokenPillarCannotBeWashedAway(GameTestHelper h){
        var p=new BlockPos(6,3,6);h.setBlock(p,RelicContent.PILLAR.get().defaultBlockState().setValue(cn.suiyi.relicward.block.ResonantPillarBlock.BROKEN,true));
        h.setBlock(p.above(),Blocks.WATER);h.getLevel().scheduleTick(h.absolutePos(p.above()),net.minecraft.world.level.material.Fluids.WATER,1);
        h.runAfterDelay(40,()->{h.assertBlockPresent(RelicContent.PILLAR.get(),p);h.succeed();});
    }
    private static ServerPlayer player(GameTestHelper h,double x,double z){
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"TrialTest")){
            @Override public boolean isInvulnerableTo(DamageSource s){return false;}
            @Override public boolean hurt(DamageSource s,float amount){float before=getHealth();actuallyHurt(s,amount);return getHealth()<before;}
        };
        p.setGameMode(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(0,2,0));p.moveTo(pos.getX()+x,pos.getY(),pos.getZ()+z,0,0);h.getLevel().addNewPlayer(p);return p;
    }
    private static CourtAltarEntity altar(GameTestHelper h){
        h.setBlock(26,2,43,RelicContent.ALTAR.get());var a=(CourtAltarEntity)h.getBlockEntity(new BlockPos(26,2,43));CourtProtection.register(a);return a;
    }
    @GameTest(template="arena_test",timeoutTicks=40)
    public static void protectionBlocksMiningExplosionsAndPistons(GameTestHelper h){
        var a=altar(h);var p=player(h,26,30);var floor=h.absolutePos(new BlockPos(26,1,29));
        h.assertTrue(!p.gameMode.destroyBlock(floor),"Survival mining must be rejected");h.assertTrue(!h.getLevel().getBlockState(floor).isAir(),"Floor remains");
        h.getLevel().explode(null,floor.getX()+.5,floor.getY()+1,floor.getZ()+.5,3,Level.ExplosionInteraction.TNT);
        h.assertTrue(!h.getLevel().getBlockState(floor).isAir(),"TNT must not remove protected floor");
        var piston=new PistonEvent.Pre(h.getLevel(),a.local(0,0,24),Direction.NORTH,PistonEvent.PistonMoveType.EXTEND);MinecraftForge.EVENT_BUS.post(piston);
        h.assertTrue(piston.isCanceled(),"Piston outside the perimeter must not push into the arena");
        var outside=h.absolutePos(new BlockPos(52,1,52));h.assertTrue(p.gameMode.destroyBlock(outside),"Protection must not claim outside territory");h.succeed();
    }
    @GameTest(template="arena_test",timeoutTicks=30)
    public static void protectionSurvivesSaveAndRepairsOldHoles(GameTestHelper h){
        var a=altar(h);var center=a.center();var saved=CourtProtection.get(h.getLevel()).save(new CompoundTag());
        h.assertTrue(CourtProtection.load(saved).contains(center.below()),"Region protection must persist without a loaded altar entity");
        h.getLevel().setBlockAndUpdate(center.below(),Blocks.AIR.defaultBlockState());a.repairMissingFloor();
        h.assertTrue(!h.getLevel().getBlockState(center.below()).isAir(),"Previously damaged floor should repair");
        var p=player(h,26,30);p.setGameMode(GameType.CREATIVE);h.assertTrue(p.gameMode.destroyBlock(center.below()),"Creative maintenance remains possible");h.succeed();
    }
    @GameTest(template="arena_test",timeoutTicks=40)
    public static void rewardsAutomaticallyRetryAndTrophyIsUnique(GameTestHelper h){
        var p=player(h,26,30);var ledger=RewardLedger.get(h.getLevel());var court=UUID.randomUUID();
        for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Blocks.STONE,64));
        ledger.award(court,UUID.randomUUID(),p.getUUID());AutomaticRewards.deliver(p);
        h.assertTrue(ledger.hasPending(court,p.getUUID()),"Full bag must retain reward");
        p.getInventory().clearContent();MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));
        h.assertTrue(p.getInventory().countItem(RelicContent.CORE.get())==0,"Automatic trophy delivery does not also grant materials");
        h.assertTrue(p.getInventory().countItem(RelicContent.TROPHY.get().asItem())==1,"First clear trophy must be delivered");
        MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));
        h.assertTrue(p.getInventory().countItem(RelicContent.CORE.get())==0&&p.totalExperience==0,"Automatic retries must not duplicate items or XP");
        ledger.award(court,UUID.randomUUID(),p.getUUID());AutomaticRewards.deliver(p);
        h.assertTrue(p.getInventory().countItem(RelicContent.CORE.get())==0&&p.getInventory().countItem(RelicContent.TROPHY.get().asItem())==1,"Subsequent victories do not grant another trophy or direct materials");h.succeed();
    }
    @GameTest(template="arena_test",timeoutTicks=100)
    public static void respawnHasCeremonyAndReturnsDormant(GameTestHelper h){
        var a=altar(h);var tag=a.saveWithoutMetadata();tag.putBoolean("Completed",true);tag.putLong("ReadyAt",h.getLevel().getGameTime()+40);a.load(tag);
        a.advanceRespawn();h.assertTrue(a.completed()&&a.respawnAt()>0&&a.boss()==null,"Cooldown completion starts a ceremony before spawning");
        h.runAfterDelay(65,()->{
            a.advanceRespawn();var boss=a.boss();h.assertTrue(boss!=null&&!a.completed(),"Boss should return automatically");
            h.assertTrue(boss.action()==WardenAction.DORMANT,"Returned boss must not auto-attack");var id=boss.getUUID();a.advanceRespawn();
            h.assertTrue(a.boss().getUUID().equals(id),"Repeated tick must not create another guardian");h.succeed();
        });
    }
    @GameTest(template="arena_test",timeoutTicks=40)
    public static void sweepHitsOnlyTheVisibleFrontSector(GameTestHelper h){
        h.getLevel().getServer().setDifficulty(Difficulty.NORMAL,true);
        var boss=h.spawn(RelicWard.BELL_WARDEN.get(),26,2,26);var front=player(h,26.5,30);var rear=player(h,26.5,22);var outside=player(h,26.5,32);
        boss.startEncounter();boss.setTarget(front);boss.setYRot(0);boss.setAction(WardenAction.SWEEP);
        h.runAfterDelay(19,()->{h.assertTrue(Math.abs(front.getHealth()-6.4F)<.001,"Front target inside 4.5 blocks takes strengthened 13.6 damage");h.assertTrue(rear.getHealth()==20&&outside.getHealth()==20,"Rear and old oversized 6-block range must not be hit");h.succeed();});
    }
    @GameTest(template="arena_test",timeoutTicks=40)
    public static void sweepCannotTurnAfterItsLock(GameTestHelper h){
        h.getLevel().getServer().setDifficulty(Difficulty.NORMAL,true);var boss=h.spawn(RelicWard.BELL_WARDEN.get(),26,2,26);var p=player(h,26.5,30);
        boss.startEncounter();boss.setTarget(p);boss.setYRot(0);boss.setAction(WardenAction.SWEEP);
        float[] locked={0};
        h.runAfterDelay(12,()->{locked[0]=boss.attackYaw();var pos=boss.position().subtract(cn.suiyi.relicward.combat.CombatMath.forward(locked[0]).scale(4));p.moveTo(pos.x,pos.y,pos.z,0,0);});
        h.runAfterDelay(19,()->{h.assertTrue(Math.abs(boss.attackYaw()-locked[0])<.01&&p.getHealth()==20,"Locked strike cannot track the player behind it");h.succeed();});
    }
    @GameTest(template="arena_test",timeoutTicks=40)
    public static void altarBypassesCooldownWithBronzeFragments(GameTestHelper h){
        h.getLevel().getServer().setDifficulty(Difficulty.NORMAL,true);
        var a=altar(h);
        for(int i=0;i<3;i++){
            var pillar=a.pillar(i);
            for(BlockPos p:BlockPos.betweenClosed(pillar.offset(-1,0,-1),pillar.offset(1,5,1)))h.getLevel().setBlockAndUpdate(p,RelicContent.PILLAR.get().defaultBlockState());
        }
        var tag=a.saveWithoutMetadata();tag.putBoolean("Completed",true);tag.putLong("ReadyAt",h.getLevel().getGameTime()+6000);a.load(tag);
        h.assertTrue(a.completed(),"Altar must be in completed cooldown state");

        var p=player(h,26,42);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(RelicContent.FRAGMENT.get(),4));
        a.interact(p);
        h.assertTrue(a.completed(),"Altar must remain on cooldown with fewer than 5 fragments");
        h.assertTrue(p.getMainHandItem().getCount()==4,"Fragments must not be consumed if insufficient");

        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(RelicContent.FRAGMENT.get(),5));
        a.interact(p);
        h.assertTrue(!a.completed(),"Altar cooldown must be cleared after offering 5 fragments");
        h.assertTrue(p.getMainHandItem().isEmpty(),"All 5 fragments in hand should be consumed");
        var boss=a.boss();
        h.assertTrue(boss!=null,"Boss must be spawned immediately");
        h.assertTrue(boss.isEncounterActive(),"Encounter must be started");
        h.succeed();
    }
}
