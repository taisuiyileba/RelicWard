package cn.suiyi.relicward.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

final class EffectRenderTypes extends RenderType {
    private EffectRenderTypes(){super("relicward_unused",DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.QUADS,256,false,true,()->{},()->{});}
    static final RenderType GLOW=create("relicward_glow",DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.QUADS,4096,false,true,
        CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
}
