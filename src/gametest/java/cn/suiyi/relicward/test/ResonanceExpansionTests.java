package cn.suiyi.relicward.test;

import cn.suiyi.relicward.*;
import cn.suiyi.relicward.block.TeachingBellEntity;
import cn.suiyi.relicward.combat.*;
import cn.suiyi.relicward.item.*;
import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraftforge.common.util.*;
import net.minecraftforge.gametest.*;

@GameTestHolder(RelicWard.ID) @PrefixGameTestTemplate(false)
public final class ResonanceExpansionTests {
    private static FakePlayer player(GameTestHelper h,int x,int z){
        var p=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"Resonance"));p.setGameMode(GameType.SURVIVAL);
        var at=h.absolutePos(new BlockPos(x,2,z));p.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);h.getLevel().addNewPlayer(p);return p;
    }
    @GameTest(template="creature_room",timeoutTicks=180,batch="fracture")
    public static void fractureRestoresArmorAndToughness(GameTestHelper h){
        var cow=h.spawn(EntityType.COW,6,2,6);cow.setNoAi(true);
        cow.getAttribute(Attributes.ARMOR).setBaseValue(20);cow.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(8);
        ArmorFracture.apply(cow);ArmorFracture.apply(cow);
        h.assertTrue(Math.abs(cow.getAttributeValue(Attributes.ARMOR)-14)<1e-6&&Math.abs(cow.getAttributeValue(Attributes.ARMOR_TOUGHNESS)-5.6)<1e-6,"Fracture reduces armor and toughness 30 percent, never stacks on refresh");
        h.runAfterDelay(165,()->{h.assertTrue(cow.getAttributeValue(Attributes.ARMOR)==20&&cow.getAttributeValue(Attributes.ARMOR_TOUGHNESS)==8,"Both attributes restore on expiration");h.succeed();});
    }
    @GameTest(template="creature_room",timeoutTicks=90,batch="bell_random")
    public static void teachingBellRandomScheduleAndContact(GameTestHelper h){
        var samples=new HashSet<Integer>();var random=net.minecraft.util.RandomSource.create(88);
        for(int i=0;i<100;i++){int t=TeachingBellEntity.randomInterval(random);h.assertTrue(t>=100&&t<=180,"Intervals stay within five to nine seconds");samples.add(t);}
        h.assertTrue(samples.size()>30,"Schedule actually varies");
        h.setBlock(6,2,6,RelicContent.TEACHING_BELL.get());var bell=(TeachingBellEntity)h.getBlockEntity(new BlockPos(6,2,6));
        var tag=bell.saveWithoutMetadata();tag.putLong("NextWave",h.getLevel().getGameTime()+1);bell.load(tag);
        var p=player(h,6,9);
        h.runAfterDelay(10,()->h.assertTrue(!p.hasEffect(ArmorFracture.EFFECT.get()),"Loading a bell never resumes an unseen damaging wave"));
        h.runAfterDelay(67,()->{
            h.assertTrue(p.hasEffect(ArmorFracture.EFFECT.get()),"Actual bell ring applies fracture on contact");
            var synced=bell.getUpdateTag();long start=synced.getLong("Started");h.assertTrue(bell.nextWaveTime()-start>=100&&bell.nextWaveTime()-start<=180,"Real firing schedules the next random interval");h.succeed();
        });
    }
    @GameTest(template="creature_room",timeoutTicks=55,batch="player_wave")
    public static void handbellWaveHitsOnceAndProtectsCompanions(GameTestHelper h){
        var p=player(h,6,6);p.setOnGround(true);var stack=new ItemStack(RelicContent.WAVE_BELL.get());p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var cow=h.spawn(EntityType.COW,6.5F,2,11.5F);cow.setNoAi(true);cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);cow.setHealth(40);
        var airborne=h.spawn(EntityType.COW,8,3,10);airborne.setNoAi(true);airborne.setNoGravity(true);
        var pet=h.spawn(RelicWard.COURT_PUPPET.get(),7,2,10);pet.tame(p);pet.setNoAi(true);
        h.assertTrue(RelicContent.WAVE_BELL.get().use(h.getLevel(),p,InteractionHand.MAIN_HAND).getResult().consumesAction(),"Grounded cast succeeds");
        var spare=new ItemStack(RelicContent.WAVE_BELL.get());p.setItemInHand(InteractionHand.MAIN_HAND,spare);
        h.assertTrue(RelicContent.WAVE_BELL.get().use(h.getLevel(),p,InteractionHand.MAIN_HAND).getResult()==InteractionResult.FAIL&&spare.getDamageValue()==0,"Swapping bells cannot bypass cooldown");
        h.runAfterDelay(22,()->{
            h.assertTrue(cow.getHealth()==32&&cow.hasEffect(ArmorFracture.EFFECT.get()),"Expanding ring hits once for eight and fractures armor");
            h.assertTrue(pet.getHealth()==pet.getMaxHealth()&&!pet.hasEffect(ArmorFracture.EFFECT.get()),"Own companion is unharmed");
            h.assertTrue(airborne.getHealth()==airborne.getMaxHealth(),"Ground ring can be jumped over");
            h.assertTrue(stack.getDamageValue()==2,"One cast costs two durability");
        });
        h.runAfterDelay(42,()->{h.assertTrue(h.getLevel().getEntitiesOfClass(cn.suiyi.relicward.entity.ResonantWave.class,p.getBoundingBox().inflate(30)).isEmpty(),"Wave expires");h.succeed();});
    }
    @GameTest(template="creature_room",timeoutTicks=40,batch="loot_tables")
    public static void bossLootTableSupportsModestLooting(GameTestHelper h){
        var p=player(h,6,6);var boss=h.spawn(RelicWard.BELL_WARDEN.get(),6,2,9);boss.setNoAi(true);
        var table=h.getLevel().getServer().getLootData().getLootTable(RelicWard.BELL_WARDEN.get().getDefaultLootTable());
        int[] totals=new int[2];
        for(int tier=0;tier<2;tier++){
            var sword=new ItemStack(Items.DIAMOND_SWORD);if(tier==1)sword.enchant(Enchantments.MOB_LOOTING,3);p.setItemInHand(InteractionHand.MAIN_HAND,sword);
            var params=new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.THIS_ENTITY,boss).withParameter(LootContextParams.ORIGIN,boss.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE,h.getLevel().damageSources().playerAttack(p)).withParameter(LootContextParams.KILLER_ENTITY,p)
                .withParameter(LootContextParams.DIRECT_KILLER_ENTITY,p).withParameter(LootContextParams.LAST_DAMAGE_PLAYER,p).create(LootContextParamSets.ENTITY);
            for(int i=1;i<=200;i++){
                var drops=table.getRandomItems(params,i);int core=0,fragments=0,bricks=0;
                for(var drop:drops){
                    h.assertTrue(!drop.is(RelicContent.TROPHY.get().asItem()),"Trophy excluded from loot table");
                    if(drop.is(RelicContent.CORE.get()))core+=drop.getCount();if(drop.is(RelicContent.FRAGMENT.get()))fragments+=drop.getCount();if(drop.is(RelicContent.BRICKS.get().asItem()))bricks+=drop.getCount();
                }
                h.assertTrue(core==1&&fragments>=6&&fragments<=8+tier*3&&bricks>=12&&bricks<=20+tier*3,"Loot stays in documented bounds; core is unaffected by Looting");totals[tier]+=fragments+bricks;
            }
        }
        h.assertTrue(totals[1]>totals[0]+300&&totals[1]<totals[0]+900,"Looting III increases average material yield modestly");boss.discard();h.succeed();
    }
    @GameTest(template="creature_room",timeoutTicks=35,batch="shield")
    public static void perfectBlockCleansesAndHasCooldown(GameTestHelper h){
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"ShieldTest")){
            void advanceUse(int ticks){useItemRemaining=useItem.getUseDuration()-ticks;}
        };
        p.setGameMode(GameType.SURVIVAL);var at=h.absolutePos(new BlockPos(6,2,6));p.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);h.getLevel().addNewPlayer(p);
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ChapterContent.SHIELD.get()));p.startUsingItem(InteractionHand.OFF_HAND);p.advanceUse(7);
        var attacker=h.spawn(EntityType.ZOMBIE,6,2,8);attacker.setNoAi(true);ArmorFracture.apply(p);
        h.runAfterDelay(7,()->{
            var event=new net.minecraftforge.event.entity.living.ShieldBlockEvent(p,h.getLevel().damageSources().mobAttack(attacker),6);BellShieldMechanics.block(event);
            h.assertTrue(!event.shieldTakesDamage()&&!p.hasEffect(ArmorFracture.EFFECT.get())&&attacker.hasEffect(ArmorFracture.EFFECT.get()),"Perfect block saves durability, cleanses self and fractures attacker");
            ArmorFracture.apply(p);var second=new net.minecraftforge.event.entity.living.ShieldBlockEvent(p,h.getLevel().damageSources().mobAttack(attacker),6);BellShieldMechanics.block(second);
            h.assertTrue(second.shieldTakesDamage()&&p.hasEffect(ArmorFracture.EFFECT.get()),"Repeated block cannot bypass mechanism cooldown");p.stopUsingItem();h.succeed();
        });
    }
    @GameTest(template="creature_room",timeoutTicks=25)
    public static void entitiesNeverShowGlowingOutline(GameTestHelper h){
        var boss=h.spawn(RelicWard.BELL_WARDEN.get(),6,2,6);var puppet=h.spawn(RelicWard.COURT_PUPPET.get(),8,2,6);
        boss.setGlowingTag(true);puppet.setGlowingTag(true);
        h.assertTrue(!boss.isCurrentlyGlowing()&&!puppet.isCurrentlyGlowing(),"External glowing flags cannot enable outlines");h.succeed();
    }
    @GameTest(template="creature_room",timeoutTicks=30,batch="instant_shield")
    public static void copperShieldBlocksImmediatelyAndWindowUsesRealTime(GameTestHelper h){
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"ImmediateShield")){
            void advanceUse(int ticks){useItemRemaining=useItem.getUseDuration()-BellShieldMechanics.START_OFFSET-ticks;}
        };
        p.setGameMode(GameType.SURVIVAL);var at=h.absolutePos(new BlockPos(6,2,6));p.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);h.getLevel().addNewPlayer(p);
        var attacker=h.spawn(EntityType.ZOMBIE,6,2,8);attacker.setNoAi(true);
        var source=h.getLevel().damageSources().mobAttack(attacker);
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ChapterContent.SHIELD.get()));
        ChapterContent.SHIELD.get().use(h.getLevel(),p,InteractionHand.OFF_HAND);
        h.assertTrue(p.isBlocking()&&p.isDamageSourceBlocked(source)&&BellShieldMechanics.elapsed(p)==0,"Copper shield blocks a frontal hit on the same tick use starts");
        p.advanceUse(10);var perfect=new net.minecraftforge.event.entity.living.ShieldBlockEvent(p,source,6);BellShieldMechanics.block(perfect);
        h.assertTrue(!perfect.shieldTakesDamage(),"Perfect window remains a full ten real ticks after startup offset");
        RelicEquipment.setCooldown(p,"Shield",0);p.advanceUse(11);
        var late=new net.minecraftforge.event.entity.living.ShieldBlockEvent(p,source,6);BellShieldMechanics.block(late);
        h.assertTrue(late.shieldTakesDamage()&&p.isBlocking(),"Late blocks are normal blocks, not perfect blocks");
        p.stopUsingItem();p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));Items.SHIELD.use(h.getLevel(),p,InteractionHand.OFF_HAND);
        h.assertTrue(!p.isBlocking(),"Other shields retain vanilla startup timing");p.stopUsingItem();h.succeed();
    }
}
