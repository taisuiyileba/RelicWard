package cn.suiyi.relicward.world;

import cn.suiyi.relicward.RelicWard;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.JigsawReplacementProcessor;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=RelicWard.ID)
public final class CourtCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("relicward").requires(s->s.hasPermission(2))
            .then(Commands.literal("place_court").executes(c->{
                var source=c.getSource();var level=source.getLevel();
                // An explicit placement command creates a large structure in front of its operator.
                BlockPos origin=BlockPos.containing(source.getPosition()).offset(-32,-4,-86);
                var template=level.getStructureManager().getOrCreate(new ResourceLocation("relicward","resonant_court"));
                if(template.getSize().getY()==0)throw new IllegalStateException("Missing court template");
                for(int x=origin.getX()>>4;x<=(origin.getX()+64)>>4;x++)
                    for(int z=origin.getZ()>>4;z<=(origin.getZ()+80)>>4;z++)level.getChunk(x,z);
                template.placeInWorld(level,origin,origin,new StructurePlaceSettings().addProcessor(JigsawReplacementProcessor.INSTANCE),level.random,2);
                var altar=origin.offset(32,5,41);
                source.sendSuccess(()->Component.translatable("message.relicward.placed",altar.getX(),altar.getY(),altar.getZ()),true);
                return 1;
            })));
    }
}
