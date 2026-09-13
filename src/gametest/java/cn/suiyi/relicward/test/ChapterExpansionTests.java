package cn.suiyi.relicward.test;

import cn.suiyi.relicward.*;
import cn.suiyi.relicward.entity.*;
import cn.suiyi.relicward.block.*;
import cn.suiyi.relicward.world.CourtPopulation;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;

@GameTestHolder(RelicWard.ID) @PrefixGameTestTemplate(false)
public final class ChapterExpansionTests {
    @GameTest(template="resonant_court",timeoutTicks=60,batch="clearance")
    public static void templateAirClearsInteriorAndPreservesStairs(GameTestHelper h){
        var outer=new BlockPos(0,31,80);var above=new BlockPos(32,36,24);var inside=new BlockPos(32,9,24);
        for(var pos:java.util.List.of(outer,above,inside))h.setBlock(pos,Blocks.GOLD_BLOCK);
        var origin=h.absolutePos(new BlockPos(0,1,0));
        h.getLevel().getStructureManager().getOrCreate(new ResourceLocation("relicward","resonant_court")).placeInWorld(h.getLevel(),origin,origin,new StructurePlaceSettings().addProcessor(JigsawReplacementProcessor.INSTANCE),h.getLevel().random,2);
        h.assertTrue(h.getBlockState(outer).isAir()&&h.getBlockState(above).isAir(),"Air stored throughout template clears obstructing terrain");
        h.assertTrue(h.getBlockState(inside).isAir(),"Intentional combat clearance must still remove obstructions");
        for(int z=77;z<=80;z++)for(int x=26;x<=38;x++){
            var p=new BlockPos(x,82-z,z);h.assertTrue(h.getBlockState(p).is(Blocks.STONE_BRICK_STAIRS),"Stair run connects to the paving");
            for(int y=1;y<=2;y++)h.assertTrue(h.getBlockState(p.above(y)).getCollisionShape(h.getLevel(),h.absolutePos(p.above(y))).isEmpty(),"No foundation ring above stair");
        }
        var altar=(CourtAltarEntity)h.getBlockEntity(new BlockPos(32,6,41));var blocker=new BlockPos(30,5,78);h.setBlock(blocker,Blocks.CRACKED_STONE_BRICKS);altar.repairEntrance();h.assertTrue(h.getBlockState(blocker).isAir(),"Old entrance repaired on loading");h.succeed();
    }
    @GameTest(template="creature_room",timeoutTicks=40)
    public static void bindingRepairAndOwnershipSurviveSave(GameTestHelper h){
        var p=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"BondOwner"));p.setGameMode(GameType.SURVIVAL);h.getLevel().addNewPlayer(p);
        var mob=h.spawn(RelicWard.COURT_PUPPET.get(),6,2,6);var seal=new ItemStack(ChapterContent.BINDING_SEAL.get(),2);p.setItemInHand(InteractionHand.MAIN_HAND,seal);
        mob.mobInteract(p,InteractionHand.MAIN_HAND);
        h.assertTrue(mob.isOwnedBy(p)&&seal.getCount()==1&&mob.getMaxHealth()==32&&mob.getTarget()==null,"Binding consumes one seal and ends hostility");
        var paste=new ItemStack(ChapterContent.REPAIR_PASTE.get(),2);p.setItemInHand(InteractionHand.MAIN_HAND,paste);mob.setHealth(20);mob.mobInteract(p,InteractionHand.MAIN_HAND);mob.mobInteract(p,InteractionHand.MAIN_HAND);
        h.assertTrue(mob.getHealth()==32&&paste.getCount()==1,"Repair restores 12, full health costs nothing");
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);mob.mobInteract(p,InteractionHand.MAIN_HAND);
        h.assertTrue(mob.isOrderedToSit()&&!mob.canAttack(p),"Stay mode and owner immunity");
        var tag=new CompoundTag();mob.saveWithoutId(tag);var restored=RelicWard.COURT_PUPPET.get().create(h.getLevel());restored.load(tag);
        h.assertTrue(restored.isOwnedBy(p)&&restored.isOrderedToSit()&&restored.getMaxHealth()==32,"Owner and stay mode persist");
        var stranger=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"OtherOwner"));restored.mobInteract(stranger,InteractionHand.MAIN_HAND);
        h.assertTrue(restored.isOwnedBy(p)&&restored.isOrderedToSit(),"Other players cannot command a bound puppet");h.succeed();
    }
    @GameTest(template="creature_room",timeoutTicks=100)
    public static void companionAssistsOwnersAttack(GameTestHelper h){
        for(int x=3;x<=10;x++)for(int z=3;z<=12;z++)h.setBlock(x,1,z,Blocks.STONE);
        var p=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"CombatOwner"));p.setGameMode(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(6,2,6));p.moveTo(pos.getX(),pos.getY(),pos.getZ(),0,0);h.getLevel().addNewPlayer(p);
        var mob=h.spawn(RelicWard.COURT_PUPPET.get(),6,2,7);mob.tame(p);var cow=h.spawn(EntityType.COW,6,2,9);cow.setNoAi(true);
        h.runAfterDelay(5,()->{p.tickCount=30;p.setLastHurtMob(cow);});
        h.runAfterDelay(65,()->{h.assertTrue(cow.getHealth()<cow.getMaxHealth(),"Companion must acquire and strike owner's enemy; owner="+mob.getOwner()+", target="+mob.getTarget()+", pos="+mob.position()+", cow="+cow.position()+", action="+mob.attackKind()+", last="+p.getLastHurtMob()+", stamp="+p.getLastHurtMobTimestamp());h.assertTrue(p.getHealth()==p.getMaxHealth(),"Companion must not hurt owner");h.succeed();});
    }
    @GameTest(template="resonant_court",timeoutTicks=80,batch="population")
    public static void patrolSlotsRespawnWithoutDuplicatingCompanions(GameTestHelper h){
        h.getLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
        var origin=h.absolutePos(new BlockPos(0,1,0));
        h.getLevel().getStructureManager().getOrCreate(new ResourceLocation("relicward","resonant_court")).placeInWorld(h.getLevel(),origin,origin,new StructurePlaceSettings().addProcessor(JigsawReplacementProcessor.INSTANCE),h.getLevel().random,2);
        var altar=(CourtAltarEntity)h.getBlockEntity(new BlockPos(32,6,41));var l=h.getLevel();var bounds=new net.minecraft.world.phys.AABB(altar.center()).inflate(65);
        // Other test batches share this server. Remove only their synthetic actors from this fixture.
        for(var old:l.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,bounds)){
            if(old instanceof net.minecraft.world.entity.player.Player)old.setPos(altar.center().getX()+1000,altar.center().getY(),altar.center().getZ()+1000);
            else old.discard();
        }
        for(var player:new java.util.ArrayList<>(l.players()))if(bounds.contains(player.position()))player.setPos(altar.center().getX()+1000,altar.center().getY(),altar.center().getZ()+1000);
        altar.population().load(new CompoundTag());
        altar.population().tick(altar);var all=l.getEntitiesOfClass(CourtPuppet.class,bounds,m->altar.getBlockPos().equals(m.courtHome()));StringBuilder info=new StringBuilder();for(int[] o:new int[][]{{-18,35},{18,35},{-7,49},{7,49}}){var at=altar.local(o[0],0,o[1]);var probe=RelicWard.COURT_PUPPET.get().create(l);probe.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);info.append(" collision=").append(l.noCollision(probe)).append(" head=").append(l.getBlockState(at.above())).append(" shapes=");l.getBlockCollisions(probe,probe.getBoundingBox()).forEach(shape->info.append(shape.bounds()));info.append(at).append(" floor=").append(l.getBlockState(at.below())).append(" at=").append(l.getBlockState(at)).append(" player=").append(l.getNearestPlayer(at.getX(),at.getY(),at.getZ(),6,false)).append(" loaded=").append(l.hasChunkAt(at));}
        h.assertTrue(all.size()==4,"Four initial patrols; found "+all.size()+"; "+info);
        var m=all.get(0);var p=FakePlayerFactory.get(l,new GameProfile(UUID.randomUUID(),"PatrolOwner"));p.setGameMode(GameType.SURVIVAL);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ChapterContent.BINDING_SEAL.get()));m.mobInteract(p,InteractionHand.MAIN_HAND);
        var state=new CompoundTag();altar.population().save(state);long due=0;for(var raw:state.getList("PatrolSlots",10)){var slot=(CompoundTag)raw;if(!slot.hasUUID("Entity")){due=slot.getLong("Due");slot.putLong("Due",0);}}
        h.assertTrue(due==l.getGameTime()+CourtPopulation.RESPAWN_TICKS,"Binding vacates patrol slot with full cooldown");
        altar.population().tick(altar);h.assertTrue(l.getEntitiesOfClass(CourtPuppet.class,bounds,e->!e.isTame()&&altar.getBlockPos().equals(e.courtHome())).size()==3,"No early replacement");
        altar.population().load(state);altar.population().tick(altar);altar.population().tick(altar);
        h.assertTrue(l.getEntitiesOfClass(CourtPuppet.class,bounds,e->!e.isTame()&&altar.getBlockPos().equals(e.courtHome())).size()==4&&m.isTame(),"One replacement after cooldown; companion preserved");
        var saved=new CompoundTag();altar.population().save(saved);altar.population().load(saved);altar.population().tick(altar);
        h.assertTrue(l.getEntitiesOfClass(CourtPuppet.class,bounds,e->!e.isTame()&&altar.getBlockPos().equals(e.courtHome())).size()==4,"Reload retains slot occupancy");
        l.getEntitiesOfClass(CourtPuppet.class,bounds,e->!e.isTame()&&altar.getBlockPos().equals(e.courtHome())).get(0).kill();
        h.runAfterDelay(25,()->{var death=new CompoundTag();altar.population().save(death);int vacant=0;for(var raw:death.getList("PatrolSlots",10)){var e=(CompoundTag)raw;if(!e.hasUUID("Entity")){vacant++;h.assertTrue(e.getLong("Due")>l.getGameTime(),"Death starts respawn cooldown");}}h.assertTrue(vacant==1,"Exactly one dead patrol slot released");h.succeed();});
    }
    @GameTest(template="creature_room",timeoutTicks=30)
    public static void bellAnvilRepairsShieldAndNeverWears(GameTestHelper h){
        var pos=new BlockPos(5,2,5);h.setBlock(pos,ChapterContent.ANVIL.get());var player=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"Smith"));player.setGameMode(GameType.SURVIVAL);player.experienceLevel=30;
        var menu=new BellAnvilBlock.BellAnvilMenu(1,player.getInventory(),net.minecraft.world.inventory.ContainerLevelAccess.create(h.getLevel(),h.absolutePos(pos)));player.containerMenu=menu;
        var shield=new ItemStack(ChapterContent.SHIELD.get());shield.setDamageValue(100);menu.getSlot(0).set(shield);menu.getSlot(1).set(new ItemStack(ChapterContent.PLATE.get()));menu.createResult();
        h.assertTrue(!menu.getSlot(2).getItem().isEmpty()&&menu.getSlot(2).getItem().getDamageValue()==0,"Functional vanilla anvil repair recipe");
        var event=new net.minecraftforge.event.entity.player.AnvilRepairEvent(player,shield,new ItemStack(ChapterContent.PLATE.get()),menu.getSlot(2).getItem());net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);h.assertTrue(event.getBreakChance()==0,"Only custom anvil disables wear");
        menu.getSlot(2).onTake(player,menu.getSlot(2).getItem().copy());h.assertTrue(h.getBlockState(pos).is(ChapterContent.ANVIL.get())&&player.experienceLevel<30,"Taking output spends XP and preserves the anvil");h.succeed();
    }

    @GameTest(template="creature_room",timeoutTicks=30)
    public static void expandedLootAndRecipesAreAvailable(GameTestHelper h){
        var l=h.getLevel();var p=FakePlayerFactory.get(l,new GameProfile(UUID.randomUUID(),"LootTester"));p.setGameMode(GameType.SURVIVAL);l.addNewPlayer(p);
        var wild=h.spawn(RelicWard.COURT_PUPPET.get(),6,2,6);wild.setNoAi(true);wild.hurt(l.damageSources().playerAttack(p),100);
        var area=wild.getBoundingBox().inflate(3);var items=l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,area);
        h.assertTrue(items.stream().anyMatch(e->e.getItem().is(ChapterContent.GEAR.get())),"Player kill drops the new guaranteed gear material");
        int count=items.size();var pet=h.spawn(RelicWard.COURT_PUPPET.get(),6,2,6);pet.tame(p);pet.hurt(l.damageSources().playerAttack(p),100);
        h.assertTrue(l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,area).size()==count&&pet.getExperienceReward()==0,"Bound companions cannot be recycled into hostile loot");
        for(String name:java.util.List.of("resonant_plate","bell_shield","bell_helmet","bell_chestplate","bell_leggings","bell_boots","bell_anvil","chime_lantern","chiseled_bronze","binding_seal","repair_paste"))
            h.assertTrue(l.getRecipeManager().byKey(new ResourceLocation("relicward",name)).isPresent(),"Loaded crafting recipe "+name);
        h.succeed();
    }
    @GameTest(template="arena_test",timeoutTicks=65)
    public static void companionDamageCreditsOwnerAndBossCanHitPet(GameTestHelper h){
        h.getLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
        var p=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"BossOwner"));p.setGameMode(GameType.SURVIVAL);var at=h.absolutePos(new BlockPos(26,2,36));p.moveTo(at.getX(),at.getY(),at.getZ(),0,0);h.getLevel().addNewPlayer(p);
        var boss=h.spawn(RelicWard.BELL_WARDEN.get(),26,2,26);boss.startEncounter();boss.setAction(cn.suiyi.relicward.combat.WardenAction.WAVE);
        var pet=h.spawn(RelicWard.COURT_PUPPET.get(),26,2,32);pet.tame(p);pet.setNoAi(true);pet.setNoGravity(true);
        boss.hurt(h.getLevel().damageSources().mobAttack(pet),30);
        h.assertTrue(boss.qualifiedPlayers().contains(p.getUUID()),"Pet damage counts for participating owner's rewards");
        h.runAfterDelay(42,()->{h.assertTrue(pet.getHealth()<pet.getMaxHealth(),"Boss wave can damage companions");h.succeed();});
    }
}
