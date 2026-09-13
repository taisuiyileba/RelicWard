package cn.suiyi.relicward.test;

import cn.suiyi.relicward.RelicContent;
import cn.suiyi.relicward.RelicWard;
import cn.suiyi.relicward.block.CourtAltarEntity;
import cn.suiyi.relicward.combat.CombatMath;
import cn.suiyi.relicward.combat.WardenAction;
import cn.suiyi.relicward.entity.BellWarden;
import cn.suiyi.relicward.reward.RewardLedger;
import cn.suiyi.relicward.item.RelicEquipment;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.common.util.FakePlayerFactory;
import com.mojang.authlib.GameProfile;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;

@GameTestHolder(RelicWard.ID)
@PrefixGameTestTemplate(false)
public final class ChapterTests {
    private static CourtAltarEntity arena(GameTestHelper h) {
        h.setBlock(new BlockPos(26,2,43),RelicContent.ALTAR.get());
        var altar=(CourtAltarEntity)h.getBlockEntity(new BlockPos(26,2,43));
        for(int i=0;i<3;i++) {
            var a=altar.pillar(i);
            for(BlockPos p:BlockPos.betweenClosed(a.offset(-1,0,-1),a.offset(1,5,1)))h.getLevel().setBlockAndUpdate(p,RelicContent.PILLAR.get().defaultBlockState());
        }
        h.getLevel().getServer().setDifficulty(Difficulty.NORMAL,true);
        return altar;
    }
    private static ServerPlayer player(GameTestHelper h,int x,int z) {
        var p=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"CourtTest"));p.setGameMode(GameType.SURVIVAL);
        var pos=h.absolutePos(new BlockPos(x,2,z));p.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        h.getLevel().addNewPlayer(p);
        p.setInvulnerable(true);return p;
    }
    @GameTest(template="arena_test",timeoutTicks=40)
    public static void altarValidatesFloorAndLayers(GameTestHelper h) {
        var altar=arena(h);var w=altar.ensureBoss();
        h.assertTrue(altar.obstruction()==null,"Pristine arena blocked at "+altar.obstruction()+" / "+(altar.obstruction()==null?"clear":h.getLevel().getBlockState(altar.obstruction())));
        h.assertTrue(w!=null&&w.pillars()==3,"All three pillars must bind to warden");
        var p=player(h,26,33);h.assertTrue(w.startEncounter(),"Survival player must start encounter");
        w.setNoAi(true);w.setAction(WardenAction.IDLE);
        w.hurt(h.getLevel().damageSources().playerAttack(p),10);
        h.assertTrue(Math.abs(w.getHealth()-474.82F)<.01,"Pillars and ten armor reduce incoming damage");
        h.assertTrue(altar.breakPillar(0),"First charge may break pillar");h.assertTrue(!altar.breakPillar(0),"Cannot break same pillar twice");
        w.stagger();w.invulnerableTime=0;w.hurt(h.getLevel().damageSources().playerAttack(p),8);
        h.assertTrue(Math.abs(w.getHealth()-464.82F)<.01,"Stagger must disable all armor and multiply damage by 1.25");
        h.assertTrue(w.pillars()==2,"One destroyed pillar permanently removes one layer");
        h.setBlock(new BlockPos(30,1,26),Blocks.AIR);
        h.assertTrue(altar.obstruction()!=null,"Missing floor must block a new encounter");h.succeed();
    }
    @GameTest(template="arena_test",timeoutTicks=130)
    public static void chargeBreaksPillarBeforePlayer(GameTestHelper h) {
        var altar=arena(h);var w=altar.ensureBoss();var p=player(h,26,9);
        h.assertTrue(w.startEncounter(),"Start encounter");w.setTarget(p);w.setYRot(180);w.setAction(WardenAction.CHARGE);
        h.runAfterDelay(62,()-> {
            h.assertTrue(altar.intactPillars()==2,"Charge must reach and break the north pillar");
            h.assertTrue(w.action()==WardenAction.STAGGER,"Collision must stop charge and trigger stagger");
            h.assertTrue(w.getZ()>altar.pillar(0).getZ(),"Warden must stop in front of pillar");h.succeed();
        });
    }
    @GameTest(template="arena_test",timeoutTicks=220)
    public static void staggerPreservedAcrossPhaseThreshold(GameTestHelper h) {
        var altar=arena(h);var w=altar.ensureBoss();player(h,26,33);w.startEncounter();w.setHealth(150);w.stagger();
        h.runAfterDelay(70,()->h.assertTrue(w.action()==WardenAction.STAGGER&&w.phase()==1,"Low health must not cancel stagger"));
        h.runAfterDelay(158,()-> {h.assertTrue(w.phase()==2,"Must enter phase two after full stagger and transition");h.succeed();});
    }
    @GameTest(template="arena_test",timeoutTicks=270)
    public static void emptyArenaResetsWithoutRewards(GameTestHelper h) {
        var altar=arena(h);var w=altar.ensureBoss();var p=player(h,26,33);w.startEncounter();altar.breakPillar(1);
        w.setHealth(200);var far=h.absolutePos(new BlockPos(51,2,51));p.moveTo(far.getX(),far.getY(),far.getZ(),0,0);
        h.runAfterDelay(215,()->{
            h.assertTrue(w.action()==WardenAction.DORMANT&&w.getHealth()==480,"All players leaving must reset boss: action="+w.action()+", health="+w.getHealth()+", removed="+w.isRemoved()+", player="+p.position()+", center="+w.arenaCenter());
            h.assertTrue(altar.intactPillars()==3&&!altar.completed(),"Reset restores pillars and gives no victory: count="+altar.intactPillars()+", completed="+altar.completed()+", altarPresent="+(h.getLevel().getBlockEntity(altar.getBlockPos())==altar)+", north="+h.getLevel().getBlockState(altar.pillar(0))+", west="+h.getLevel().getBlockState(altar.pillar(1))+", east="+h.getLevel().getBlockState(altar.pillar(2)));h.succeed();
        });
    }
    @GameTest(template="arena_test",timeoutTicks=50)
    public static void rewardsPersistAndDoNotDuplicate(GameTestHelper h) {
        var p=player(h,26,33);var ledger=new RewardLedger();UUID court=UUID.randomUUID(),encounter=UUID.randomUUID();
        ledger.award(court,encounter,p.getUUID());ledger.award(court,encounter,p.getUUID());
        for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Blocks.STONE,64));
        h.assertTrue(ledger.claim(court,p)==0&&ledger.hasPending(court,p.getUUID()),"Full inventory must preserve all rewards");
        var saved=ledger.save(new CompoundTag());var restored=RewardLedger.load(saved);p.getInventory().clearContent();
        h.assertTrue(restored.claim(court,p)==1,"Only the first-clear trophy is delivered directly");
        h.assertTrue(restored.claim(court,p)==0&&!restored.hasPending(court,p.getUUID()),"Second claim must be empty");
        h.assertTrue(p.getInventory().countItem(RelicContent.CORE.get())==0,"Materials never delivered directly");h.succeed();
    }
    @GameTest(template="arena_test",timeoutTicks=150)
    public static void legitimateVictoryCreatesPersonalReward(GameTestHelper h) {
        var altar=arena(h);var w=altar.ensureBoss();var p=player(h,26,33);w.startEncounter();w.setNoAi(true);w.setAction(WardenAction.IDLE);
        w.setNoAi(false);w.setAction(WardenAction.TRANSFORM);
        h.runAfterDelay(55,()->{
            w.setNoAi(true);w.setAction(WardenAction.STAGGER);
            w.hurt(h.getLevel().damageSources().playerAttack(p),10000);
        });
        h.runAfterDelay(140,()-> {
            h.assertTrue(altar.completed(),"Player kill must complete encounter after death animation");
            var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,w.getBoundingBox().inflate(8));
            h.assertTrue(drops.stream().anyMatch(e->e.getItem().is(RelicContent.CORE.get())),"Core is a ground drop");
            h.assertTrue(drops.stream().noneMatch(e->e.getItem().is(RelicContent.TROPHY.get().asItem())),"Trophy never appears in ground loot");
            h.assertTrue(p.getInventory().countItem(RelicContent.TROPHY.get().asItem())==1,"Contributing player receives first trophy automatically");
            h.assertTrue(!RewardLedger.get(h.getLevel()).hasPending(altar.courtId(),p.getUUID()),"No extra altar click should be necessary");h.succeed();
        });
    }
    @GameTest(template="arena_test",timeoutTicks=150)
    public static void adminKillNeverAwardsLoot(GameTestHelper h) {
        var altar=arena(h);var w=altar.ensureBoss();player(h,26,33);w.startEncounter();w.kill();
        h.runAfterDelay(90,()->{h.assertTrue(!altar.completed(),"Admin kill must not complete encounter");h.succeed();});
    }
    @GameTest(template="creature_room",timeoutTicks=30)
    public static void geometryAndEquipmentRules(GameTestHelper h) {
        Vec3 origin=Vec3.ZERO;
        h.assertTrue(CombatMath.wave(origin,new Vec3(0,0,5),0,5),"Standing on ring must be hit");
        h.assertTrue(!CombatMath.wave(origin,new Vec3(0,.8,5),0,5),"A well timed jump must clear ring");
        h.assertTrue(CombatMath.cone(origin,0,new Vec3(0,0,5),6,150),"Front inside cone");
        h.assertTrue(!CombatMath.cone(origin,0,new Vec3(0,0,-5),6,150),"Rear outside cone");
        h.assertTrue(Math.abs(CombatMath.healthForPlayers(2)-768)<.001&&Math.abs(CombatMath.healthForPlayers(4)-1344)<.001,"Multiplayer scaling");
        h.assertTrue(new ItemStack(RelicContent.MAUL.get()).getMaxDamage()==960,"Maul durability");h.succeed();
    }
    @GameTest(template="creature_room",timeoutTicks=40)
    public static void maulDealsDirectionalDamageAndSharesCooldown(GameTestHelper h) {
        var p=player(h,6,6);p.setYRot(0);
        h.setBlock(6,1,8,Blocks.STONE);
        var front=h.spawn(EntityType.COW,6.5F,2,9.5F);front.setNoAi(true);front.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(40);front.setHealth(40);
        var rear=h.spawn(EntityType.COW,6.5F,2,3.5F);rear.setNoAi(true);
        var stack=new ItemStack(RelicContent.MAUL.get());p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        stack.getItem().releaseUsing(stack,h.getLevel(),p,71972);
        h.assertTrue(front.getHealth()==front.getMaxHealth()-14,"Charged maul must damage a visible front target");
        h.assertTrue(rear.getHealth()==rear.getMaxHealth(),"Charged maul must not hit behind the wielder");
        h.assertTrue(stack.getDamageValue()==3&&RelicEquipment.cooldown(p,"Maul")>0,"Successful skill spends durability and starts cooldown");
        front.invulnerableTime=0;var second=new ItemStack(RelicContent.MAUL.get());p.setItemInHand(InteractionHand.MAIN_HAND,second);
        second.getItem().releaseUsing(second,h.getLevel(),p,71972);
        h.assertTrue(front.getHealth()==front.getMaxHealth()-14&&second.getDamageValue()==0,"Another maul cannot bypass cooldown");h.succeed();
    }
    @GameTest(template="creature_room",timeoutTicks=40)
    public static void pendantReducesOneHitAndKeepsCooldown(GameTestHelper h) {
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"PendantTest")) {
            @Override public boolean isInvulnerableTo(DamageSource source){return false;}
            // FakePlayer never ticks its spawn protection. Exercise the vanilla armor/event pipeline directly.
            @Override public boolean hurt(DamageSource source,float amount){float before=getHealth();actuallyHurt(source,amount);return getHealth()<before;}
        };
        p.setGameMode(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(6,2,6));p.moveTo(pos.getX(),pos.getY(),pos.getZ(),0,0);h.getLevel().addNewPlayer(p);
        var curios=top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).orElseThrow(()->new IllegalStateException("No Curios inventory"));
        var necklace=curios.getStacksHandler("necklace").orElseThrow(()->new IllegalStateException("No necklace slot")).getStacks();
        necklace.setStackInSlot(0,new ItemStack(RelicContent.PENDANT.get()));
        for(int i=0;i<60;i++)RelicEquipment.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p));
        var w=h.spawn(RelicWard.BELL_WARDEN.get(),6,2,10);
        p.hurt(w.resonanceDamage(),10);
        h.assertTrue(p.getHealth()==13,"Pendant must reduce a 10-point hit to 7");
        p.invulnerableTime=0;p.setHealth(20);necklace.setStackInSlot(0,new ItemStack(RelicContent.PENDANT.get()));
        p.hurt(w.resonanceDamage(),10);
        h.assertTrue(p.getHealth()==10&&RelicEquipment.cooldown(p,"Pendant")>0,"Swapping pendant must preserve cooldown");h.succeed();
    }
}
