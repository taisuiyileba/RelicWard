package cn.suiyi.relicward.test;

import cn.suiyi.relicward.RelicWard;
import cn.suiyi.relicward.combat.WardenAction;
import cn.suiyi.relicward.combat.GroundCracks;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;

@GameTestHolder(RelicWard.ID)
@PrefixGameTestTemplate(false)
public final class ImpactTests {
    @GameTest(template="arena_test",timeoutTicks=100)
    public static void slamCracksOnlyAtContactAndClears(GameTestHelper h){
        h.getLevel().getServer().setDifficulty(Difficulty.NORMAL,true);
        var boss=h.spawn(RelicWard.BELL_WARDEN.get(),26,2,26);
        var player=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"ImpactTest"));player.setGameMode(GameType.SURVIVAL);
        var pos=h.absolutePos(new BlockPos(26,2,30));player.moveTo(pos.getX(),pos.getY(),pos.getZ(),0,0);h.getLevel().addNewPlayer(player);
        h.assertTrue(boss.startEncounter(),"Start encounter");boss.setAction(WardenAction.SLAM);
        h.runAfterDelay(20,()->h.assertTrue(boss.activeCrackCount()==0,"No cracks before hammer contact"));
        h.runAfterDelay(27,()->h.assertTrue(boss.activeCrackCount()>20,"Impact should animate a patch of floor blocks"));
        h.runAfterDelay(62,()->{
            h.assertTrue(boss.activeCrackCount()==0,"Crack overlays must expire");
            for(int x=21;x<=31;x++)for(int z=21;z<=34;z++)h.assertBlockPresent(Blocks.STONE,x,1,z);
            h.succeed();
        });
    }
    @GameTest(template="arena_test",timeoutTicks=20)
    public static void crackEffectsDoNotReplaceBlocksAndCanCancel(GameTestHelper h){
        var cracks=new GroundCracks();var p=h.absolutePos(new BlockPos(26,2,26));
        cracks.begin(h.getLevel(),18000,Vec3.atBottomCenterOf(p),3.5);
        h.assertTrue(cracks.size()>20,"Cracks created");cracks.clear(h.getLevel());
        h.assertTrue(cracks.size()==0,"Cancellation removes every tracked crack");
        for(int x=22;x<=30;x++)for(int z=22;z<=30;z++)h.assertBlockPresent(Blocks.STONE,x,1,z);h.succeed();
    }
}
