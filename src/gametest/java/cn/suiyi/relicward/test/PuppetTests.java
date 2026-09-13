package cn.suiyi.relicward.test;

import cn.suiyi.relicward.RelicWard;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(RelicWard.ID)
@PrefixGameTestTemplate(false)
public final class PuppetTests {
    @GameTest(template="creature_room",timeoutTicks=55)
    public static void puppetSwingHasWindupAndOneContact(GameTestHelper h){
        var puppet=h.spawn(RelicWard.COURT_PUPPET.get(),6,2,6);puppet.setNoAi(true);puppet.setNoGravity(true);
        var cow=h.spawn(EntityType.COW,6,2,8);cow.setNoAi(true);cow.setNoGravity(true);
        puppet.beginStrike(cow,false);
        h.runAfterDelay(10,()->h.assertTrue(cow.getHealth()==cow.getMaxHealth(),"Puppet must not damage during anticipation"));
        h.runAfterDelay(14,()->h.assertTrue(cow.getHealth()==cow.getMaxHealth()-4,"Puppet contact must occur at tick 12"));
        h.runAfterDelay(32,()->{h.assertTrue(cow.getHealth()==cow.getMaxHealth()-4&&puppet.attackKind()==0,"Recovery must finish without repeated damage");h.succeed();});
    }
    @GameTest(template="creature_room",timeoutTicks=50)
    public static void puppetBashLocksVictimAndDirection(GameTestHelper h){
        var puppet=h.spawn(RelicWard.COURT_PUPPET.get(),6,2,6);puppet.setNoAi(true);puppet.setNoGravity(true);
        var original=h.spawn(EntityType.COW,6,2,8);original.setNoAi(true);original.setNoGravity(true);
        var other=h.spawn(EntityType.COW,6,2,4);other.setNoAi(true);other.setNoGravity(true);
        puppet.beginStrike(original,true);puppet.setTarget(other);
        h.runAfterDelay(14,()->h.assertTrue(original.getHealth()==original.getMaxHealth(),"Bash must have its longer anticipation"));
        h.runAfterDelay(18,()->{
            h.assertTrue(original.getHealth()==original.getMaxHealth()-4,"Locked original victim must receive contact");
            h.assertTrue(other.getHealth()==other.getMaxHealth(),"Changing aggro must not redirect the active strike");h.succeed();
        });
    }
}
