package cn.suiyi.relicward.test;

import cn.suiyi.relicward.*;
import cn.suiyi.relicward.combat.WardenAction;
import cn.suiyi.relicward.item.RelicEquipment;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;

@GameTestHolder(RelicWard.ID)
@PrefixGameTestTemplate(false)
public final class GearTests {
    @GameTest(template="creature_room",timeoutTicks=40)
    public static void maulAutomaticallyFiresOnlyAfterCharge(GameTestHelper h){
        var p=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"MaulTest"));p.setGameMode(GameType.SURVIVAL);
        var pos=h.absolutePos(new BlockPos(6,2,6));p.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);h.getLevel().addNewPlayer(p);h.setBlock(6,1,8,Blocks.STONE);
        var cow=h.spawn(EntityType.COW,6.5F,2,9);cow.setNoAi(true);cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(40);cow.setHealth(40);var stack=new ItemStack(RelicContent.MAUL.get());p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        RelicContent.MAUL.get().use(h.getLevel(),p,InteractionHand.MAIN_HAND);
        for(int t=0;t<28;t++)RelicContent.MAUL.get().onUseTick(h.getLevel(),p,stack,72000-t);
        h.assertTrue(cow.getHealth()==cow.getMaxHealth()&&stack.getDamageValue()==0,"No premature slam");
        RelicContent.MAUL.get().onUseTick(h.getLevel(),p,stack,71972);
        h.assertTrue(cow.getHealth()==cow.getMaxHealth()-14&&stack.getDamageValue()==3&&p.isUsingItem(),"Impact occurs after the downswing and retains the recovery pose");
        RelicContent.MAUL.get().onUseTick(h.getLevel(),p,stack,71962);
        h.assertTrue(!p.isUsingItem(),"Use ends after recovery");
        RelicContent.MAUL.get().releaseUsing(stack,h.getLevel(),p,71960);
        h.assertTrue(stack.getDamageValue()==3,"Mouse release after automatic slam cannot fire again");h.succeed();
    }
    @GameTest(template="creature_room",timeoutTicks=30)
    public static void earlyReleaseCancelsAndOffhandPendantIsInactive(GameTestHelper h){
        var p=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"CancelTest"));p.setGameMode(GameType.SURVIVAL);
        var stack=new ItemStack(RelicContent.MAUL.get());p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        RelicContent.MAUL.get().releaseUsing(stack,h.getLevel(),p,71990);
        h.assertTrue(stack.getDamageValue()==0&&RelicEquipment.cooldown(p,"Maul")==0,"Early cancellation must be free");
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(RelicContent.PENDANT.get()));
        h.assertTrue(!RelicEquipment.pendantEquipped(p),"Offhand must not count as necklace equipment");h.succeed();
    }
    @GameTest(template="arena_test",timeoutTicks=45)
    public static void chargeHeadingTracksThenLocks(GameTestHelper h){
        h.getLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
        var boss=h.spawn(RelicWard.BELL_WARDEN.get(),26,2,26);
        var p=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"ChargeTest"));p.setGameMode(GameType.SURVIVAL);
        var pos=h.absolutePos(new BlockPos(26,2,37));p.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);h.getLevel().addNewPlayer(p);
        boss.startEncounter();boss.setTarget(p);boss.setYRot(0);boss.setAction(WardenAction.CHARGE);
        h.runAfterDelay(5,()->{var v=h.absolutePos(new BlockPos(36,2,31));p.moveTo(v.getX()+.5,v.getY(),v.getZ()+.5,0,0);});
        h.runAfterDelay(16,()->h.assertTrue(Math.abs(boss.attackYaw())>10&&Math.abs(boss.attackYaw()-boss.getYRot())<.01,"Body must turn during the warning"));
        float[] locked={0};h.runAfterDelay(23,()->{locked[0]=boss.attackYaw();var v=h.absolutePos(new BlockPos(17,2,34));p.moveTo(v.getX()+.5,v.getY(),v.getZ()+.5,0,0);});
        h.runAfterDelay(28,()->{h.assertTrue(Math.abs(boss.attackYaw()-locked[0])<.01&&Math.abs(boss.getYRot()-locked[0])<.01,"Heading and path must stop tracking together");h.succeed();});
    }
    @GameTest(template="arena_test",timeoutTicks=40)
    public static void headTracksPlayerDuringBellWave(GameTestHelper h){
        h.getLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
        var boss=h.spawn(RelicWard.BELL_WARDEN.get(),26,2,26);
        var p=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"GazeTest"));p.setGameMode(GameType.SURVIVAL);
        var v=h.absolutePos(new BlockPos(33,2,34));p.moveTo(v.getX()+.5,v.getY(),v.getZ()+.5,0,0);h.getLevel().addNewPlayer(p);
        boss.startEncounter();boss.setTarget(p);boss.setYRot(0);boss.setYBodyRot(0);boss.setAction(WardenAction.WAVE);
        h.runAfterDelay(15,()->{
            h.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(boss.yHeadRot-boss.yBodyRot))>10,"Head turns independently toward player during stationary bell move");
            h.assertTrue(boss.getXRot()>5,"Tall boss looks down at player eye height");h.succeed();
        });
    }
    @GameTest(template="creature_room",timeoutTicks=20)
    public static void configurablePlacementRetainsVanillaLocateContract(GameTestHelper h){
        var registry=h.getLevel().registryAccess();
        var set=registry.registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE_SET).get(new net.minecraft.resources.ResourceLocation("relicward","resonant_court"));
        var placement=(cn.suiyi.relicward.world.CourtPlacement)set.placement();
        h.assertTrue(placement.spacing()==WorldgenPreferences.spacingChunks(),"Config controls actual placement grid");
        for(int x:new int[]{-1000,-1,0,999}){
            var candidate=placement.getPotentialStructureChunk(8172,x,-x);
            h.assertTrue(candidate.equals(placement.getPotentialStructureChunk(8172,candidate.x,candidate.z)),"Locate and generation agree including negative coordinates");
        }
        int previous=WorldgenPreferences.COURT_SPACING.get();
        try {
            WorldgenPreferences.COURT_SPACING.set(4000);
            var sparse=new cn.suiyi.relicward.world.CourtPlacement(8172401);
            h.assertTrue(sparse.spacing()==113,"Changing distance config must change actual candidate spacing");
        }finally{WorldgenPreferences.COURT_SPACING.set(previous);}
        h.succeed();
    }
}
