"""Single-source cuboid model -> Forge geometry, pixel atlas, editable Blockbench project.

Python 3.10+, Pillow. Coordinates are model pixels (16 = one block), Java Y-down,
feet at Y=0. Never hand-edit the generated Java geometry or the exported bbmodel.
"""
from pathlib import Path
import base64
import json
import random
import uuid
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/relicward'
EDIT = ROOT / 'art/blockbench'
EDIT.mkdir(parents=True, exist_ok=True)
(ASSETS / 'textures/entity').mkdir(parents=True, exist_ok=True)

PALETTE = {
    'bronze': (119, 88, 48), 'patina': (53, 99, 85),
    'gold': (188, 146, 72),
    'dark': (35, 44, 42), 'iron': (65, 75, 68),
    'glow': (255, 214, 120), 'cloth': (50, 64, 59),
}
bones = []
cubes = []

def bone(name, parent, pivot):
    bones.append(dict(name=name, parent=parent, pivot=list(pivot)))
    return name

def cube(name, b, pos, size, material='bronze'):
    assert all(isinstance(v, int) and v > 0 for v in size)
    cubes.append(dict(name=name, bone=b, pos=list(pos), size=list(size), material=material))

root = bone('root', None, (0, 0, 0))
body = bone('body', root, (0, -40, 0))
head = bone('head', body, (0, -27, 0))
bell = bone('bell', body, (0, -21, -2))
clapper = bone('clapper', bell, (0, 10, 0))
right = bone('right_arm', body, (-22, -22, 0))
rf = bone('right_forearm', right, (0, 17, 0))
hammer = bone('hammer', rf, (0, 15, -1))
left = bone('left_arm', body, (22, -22, 0))
lf = bone('left_forearm', left, (0, 17, 0))
rl = bone('right_leg', root, (-10, -38, 0))
rs = bone('right_shin', rl, (0, 18, 0))
ll = bone('left_leg', root, (10, -38, 0))
ls = bone('left_shin', ll, (0, 18, 0))

# Hollow chest: a rear chassis and four framing rails expose a real suspended bell.
cube('backplate', body, (-16, -24, 5), (32, 24, 6), 'patina')
cube('left_chest_pier', body, (-18, -23, -8), (7, 23, 14), 'bronze')
cube('right_chest_pier', body, (11, -23, -8), (7, 23, 14), 'bronze')
cube('collar_lintel', body, (-18, -27, -9), (36, 6, 20), 'gold')
cube('collar_shadow', body, (-13, -28, -6), (26, 2, 13), 'dark')
cube('lower_chest_sill', body, (-17, -3, -9), (34, 5, 19), 'gold')
cube('waist_joint', body, (-10, 2, -5), (20, 4, 12), 'dark')
cube('hip_belt', body, (-16, 5, -8), (32, 5, 18), 'bronze')
cube('belt_boss', body, (-5, 5, -10), (10, 6, 3), 'gold')
cube('belt_glyph', body, (-1, 6, -11), (2, 4, 1), 'patina')
for sx in (-1, 1):
    x = -18 if sx < 0 else 14
    cube(f'chest_trim_{sx}', body, (x, -22, -10), (4, 22, 2), 'gold')
    for yy in (-19, -10):
        cube(f'chest_stud_{sx}_{yy}', body, (x+1, yy, -11), (2, 2, 1), 'dark')
    # Angular raised meander detailing gives the armour its ritual character.
    x = -16 if sx < 0 else 12
    cube(f'chest_rune_long_{sx}', body, (x, -18, -9), (2, 11, 1), 'patina')

# Suspended bronze bell, built as a hollow stepped shell, not a solid chest plate.
cube('bell_suspension', bell, (-2, -1, -2), (4, 4, 4), 'dark')
cube('bell_crown', bell, (-4, 2, -4), (8, 3, 8), 'gold')
cube('bell_shoulder', bell, (-6, 5, -5), (12, 4, 10), 'bronze')
cube('bell_front_wall', bell, (-7, 9, -6), (14, 6, 2), 'gold')
cube('bell_back_wall', bell, (-7, 9, 4), (14, 6, 2), 'bronze')
cube('bell_left_wall', bell, (-7, 9, -4), (2, 6, 8), 'bronze')
cube('bell_right_wall', bell, (5, 9, -4), (2, 6, 8), 'bronze')
cube('bell_lip_front', bell, (-9, 15, -8), (18, 2, 2), 'gold')
cube('bell_lip_back', bell, (-9, 15, 6), (18, 2, 2), 'gold')
cube('bell_lip_left', bell, (-9, 15, -6), (2, 2, 12), 'gold')
cube('bell_lip_right', bell, (7, 15, -6), (2, 2, 12), 'gold')
cube('bell_crack_a', bell, (-1, 7, -6.15), (1, 4, 1), 'glow')
cube('bell_crack_b', bell, (0, 10, -6.2), (2, 1, 1), 'glow')
cube('bell_crack_c', bell, (1, 10, -6.25), (1, 5, 1), 'glow')
cube('clapper_stem', clapper, (-1, -2, -1), (2, 8, 2), 'dark')
cube('clapper_weight', clapper, (-2, 6, -2), (4, 3, 4), 'glow')

# Mute mask with a broad brow and bifurcated crown. No mouth or copied creature face.
cube('neck_spindle', head, (-4, -1, -3), (8, 5, 8), 'dark')
cube('mask_core', head, (-8, -12, -6), (16, 13, 12), 'patina')
cube('mask_front', head, (-7, -11, -8), (14, 11, 3), 'bronze')
cube('brow_beam', head, (-9, -12, -9), (18, 3, 3), 'gold')
cube('nose_ridge', head, (-2, -9, -9), (4, 9, 3), 'gold')
cube('eye_socket_r', head, (-7, -8, -8.2), (5, 3, 1), 'dark')
cube('eye_socket_l', head, (2, -8, -8.2), (5, 3, 1), 'dark')
cube('eye_r', head, (-6, -7, -8.35), (4, 1, 1), 'glow')
cube('eye_l', head, (2, -7, -8.35), (4, 1, 1), 'glow')
cube('jaw_mask', head, (-6, -2, -8), (12, 4, 4), 'patina')
cube('crown_center', head, (-2, -15, -3), (4, 4, 7), 'gold')
for side in (-1, 1):
    x = -11 if side < 0 else 8
    cube(f'ear_plate_{side}', head, (x, -10, -3), (3, 12, 9), 'gold')
    cube(f'crown_prong_{side}', head, (x, -16, -1), (3, 7, 5), 'patina')
    cube(f'crown_cap_{side}', head, (x-1, -16, -2), (5, 2, 7), 'gold')

# Three broken, bell-like uprights rise over the rear of the shoulder line.
for i, (xx, height) in enumerate(((-14, 31), (0, 35), (14, 28))):
    cube(f'back_stanchion_{i}', body, (xx-2, -height, 11), (4, height+1, 4), 'dark')
    cube(f'back_verdig_rib_{i}', body, (xx-3, -height, 13), (6, height-3, 3), 'patina')
    cube(f'back_finial_{i}', body, (xx-4, -height, 10), (8, 3, 8), 'gold')
    cube(f'back_rib_band_{i}', body, (xx-4, -12, 11), (8, 3, 6), 'gold')
cube('rear_crossbeam', body, (-21, -21, 10), (42, 4, 6), 'bronze')
cube('rear_central_seal', body, (-6, -17, 15), (12, 12, 2), 'bronze')
cube('rear_seal_inset', body, (-4, -15, 17), (8, 8, 1), 'dark')
cube('rear_seal_line', body, (-1, -14, 18), (2, 6, 1), 'gold')

for b, f, side in ((right, rf, 'right'), (left, lf, 'left')):
    cube(f'{side}_shoulder_joint', b, (-5, -4, -5), (10, 10, 10), 'dark')
    cube(f'{side}_pauldron', b, (-9, -7, -9), (18, 9, 18), 'patina')
    cube(f'{side}_pauldron_crown', b, (-8, -9, -7), (16, 3, 14), 'bronze')
    cube(f'{side}_pauldron_trim', b, (-9, 1, -10), (18, 2, 20), 'gold')
    cube(f'{side}_shoulder_glyph_top', b, (-5, -5, -10), (10, 2, 1), 'gold')
    cube(f'{side}_shoulder_glyph_r', b, (-5, -3, -10), (2, 3, 1), 'gold')
    cube(f'{side}_shoulder_glyph_l', b, (3, -3, -10), (2, 3, 1), 'gold')
    cube(f'{side}_upper_arm', b, (-5, 4, -5), (10, 12, 10), 'bronze')
    cube(f'{side}_bicep_inlay', b, (-3, 6, -6), (6, 8, 1), 'patina')
    cube(f'{side}_elbow', f, (-4, -2, -4), (8, 6, 8), 'dark')
    cube(f'{side}_gauntlet', f, (-6, 3, -6), (12, 11, 12), 'patina')
    cube(f'{side}_gauntlet_band', f, (-7, 3, -7), (14, 3, 14), 'gold')
    cube(f'{side}_gauntlet_spine', f, (-2, 6, -7), (4, 7, 2), 'bronze')
    cube(f'{side}_wrist', f, (-4, 14, -4), (8, 3, 8), 'dark')
    cube(f'{side}_fist', f, (-5, 16, -5), (10, 6, 10), 'bronze')
    for fx in (-3, 0, 3):
        cube(f'{side}_knuckle_{fx}', f, (fx-1, 17, -6), (2, 3, 2), 'gold')

# The maul is held down the right flank; the stepped head nearly touches the ground.
cube('hammer_shaft', hammer, (-2, -20, -2), (4, 45, 4), 'dark')
cube('hammer_grip_top', hammer, (-3, -12, -3), (6, 3, 6), 'gold')
cube('hammer_grip_bottom', hammer, (-3, 7, -3), (6, 3, 6), 'gold')
cube('hammer_pommel', hammer, (-4, -23, -4), (8, 4, 8), 'bronze')
cube('hammer_head', hammer, (-12, 15, -7), (24, 11, 14), 'patina')
cube('hammer_head_spine', hammer, (-4, 14, -8), (8, 13, 16), 'bronze')
cube('hammer_face_rear', hammer, (-15, 16, -8), (4, 10, 16), 'gold')
cube('hammer_face_front', hammer, (11, 16, -8), (4, 10, 16), 'gold')
cube('hammer_front_seal', hammer, (-3, 17, -9), (6, 6, 2), 'dark')
cube('hammer_front_glyph_v', hammer, (-1, 18, -10), (2, 4, 1), 'glow')
cube('hammer_front_glyph_h', hammer, (-2, 19, -10), (4, 1, 1), 'glow')

# Rotate the ORIGINAL dual-face hammer 90 degrees around its vertical shaft.
# Bake the turn into the source geometry so every pose and the editor use the same orientation.
for c in cubes:
    if c['bone']==hammer:
        x,y,z=c['pos'];w,h,d=c['size']
        c['pos']=[z,y,-x-w]
        c['size']=[d,h,w]

for leg, shin, side in ((rl, rs, 'right'), (ll, ls, 'left')):
    cube(f'{side}_hip_joint', leg, (-5, -1, -5), (10, 5, 10), 'dark')
    cube(f'{side}_thigh', leg, (-6, 3, -6), (12, 13, 12), 'bronze')
    cube(f'{side}_skirt_lamella', leg, (-7, 0, -9), (14, 10, 3), 'patina')
    cube(f'{side}_skirt_trim', leg, (-7, 8, -10), (14, 2, 4), 'gold')
    cube(f'{side}_thigh_inset', leg, (-4, 11, -7), (8, 4, 2), 'dark')
    cube(f'{side}_knee_joint', shin, (-4, -2, -4), (8, 6, 8), 'dark')
    cube(f'{side}_knee_guard', shin, (-6, -1, -8), (12, 7, 3), 'gold')
    cube(f'{side}_knee_inlay', shin, (-3, 1, -9), (6, 3, 2), 'patina')
    cube(f'{side}_shin', shin, (-6, 5, -4), (12, 8, 8), 'patina')
    cube(f'{side}_shin_spine', shin, (-2, 6, -5), (4, 6, 2), 'bronze')
    foot=bone(side+'_foot',shin,(0,17,0))
    cube(f'{side}_ankle_band', foot, (-7, -3, -7), (14, 3, 14), 'gold')
    cube(f'{side}_foot', foot, (-7, 0, -11), (14, 3, 18), 'bronze')
    cube(f'{side}_toe_plate', foot, (-6, -1, -11), (12, 2, 7), 'patina')

# Hinged chest piers reveal the bell further during the second phase.
for side, pivot in [('left',(-18,-12,5)),('right',(18,-12,5))]:
    panel=bone('chest_'+side,body,pivot)
    sign='-1' if side=='left' else '1'
    selected=[side+'_chest_pier','chest_trim_'+sign,'chest_rune_long_'+sign]
    for c in cubes:
        if c['name'] in selected or c['name'].startswith('chest_stud_'+sign+'_'):
            c['bone']=panel
            c['pos']=[c['pos'][i]-pivot[i] for i in range(3)]

# Pack each cube's box UV on a dedicated patch, preserving editable face detail.
TEX = 512
u = v = row_height = 0
for c in cubes:
    w, h, d = c['size']
    width, height = 2*(w+d), h+d
    if u + width + 2 > TEX:
        u = 0
        v += row_height + 2
        row_height = 0
    assert v + height + 2 <= TEX, 'Atlas overflow'
    c['uv'] = [u+1, v+1]
    u += width+2
    row_height = max(row_height, height)

def face_uv(c):
    u, v = c['uv']; w, h, d = c['size']
    return {
        'west': [u, v+d, u+d, v+d+h],
        'north': [u+d, v+d, u+d+w, v+d+h],
        'east': [u+d+w, v+d, u+2*d+w, v+d+h],
        'south': [u+2*d+w, v+d, u+2*d+2*w, v+d+h],
        'up': [u+d, v, u+d+w, v+d],
        'down': [u+d+w, v, u+d+2*w, v+d],
    }

atlas = Image.new('RGBA', (TEX,TEX), (0,0,0,0))
glow = Image.new('RGBA', (TEX,TEX), (0,0,0,0))
rng = random.Random(8172)
for c in cubes:
    base = PALETTE[c['material']]
    for face, rect in face_uv(c).items():
        x0,y0,x1,y1=rect
        for y in range(y0,y1):
            for x in range(x0,x1):
                edge = min(x-x0,x1-1-x,y-y0,y1-1-y)
                shade = rng.choice((-7,-4,-2,0,0,2,4,6))
                if edge == 0:
                    shade += 14 if y == y0 or x == x0 else -17
                color = tuple(max(0,min(255,b+shade)) for b in base)
                if c['material'] in ('bronze','gold') and edge > 0 and rng.random() < 0.045:
                    color = (65+rng.randrange(10),101+rng.randrange(10),81+rng.randrange(10))
                atlas.putpixel((x,y),color+(255,))
                if c['material']=='glow': glow.putpixel((x,y),color+(255,))
        # Small incised, angular bronze marks on broad armour surfaces.
        if c['material']=='patina' and x1-x0>=8 and y1-y0>=8:
            dr=ImageDraw.Draw(atlas)
            dr.line([(x0+2,y0+3),(x1-3,y0+3),(x1-3,y0+5)], fill=(150,126,73,255))
            dr.line([(x0+3,y1-3),(x0+3,y1-5)],fill=(25,61,53,255))

atlas.save(ASSETS/'textures/entity/bell_warden.png')
glow.save(ASSETS/'textures/entity/bell_warden_glow.png')
atlas.save(EDIT/'bell_warden.png')

world = {}
for b in bones:
    par=world.get(b['parent'],[0,0,0])
    world[b['name']]=[par[i]+b['pivot'][i] for i in range(3)]
for c in cubes:
    c['world']=[world[c['bone']][i]+c['pos'][i] for i in range(3)]

# Blockbench generic, box-UV format: Y-up with pivots and hierarchy intact.
def uid(name): return str(uuid.uuid5(uuid.NAMESPACE_URL,'relicward/bell_warden/'+name))
elements=[]
for c in cubes:
    x,y,z=c['world']; w,h,d=c['size']
    elements.append(dict(name=c['name'],type='cube',uuid=uid(c['name']),
        box_uv=True,uv_offset=c['uv'],from_=[x,-y-h,z],to=[x+w,-y,z+d],
        autouv=0,color=2,faces={f:dict(uv=r,texture=0) for f,r in face_uv(c).items()}))
for e in elements: e['from']=e.pop('from_')

def outliner(name):
    p=world[name]
    return dict(name=name,uuid=uid('bone/'+name),origin=[p[0],-p[1],p[2]],rotation=[0,0,0],
        export=True,isOpen=True,visibility=True,
        children=[uid(c['name']) for c in cubes if c['bone']==name]+[
            outliner(b['name']) for b in bones if b['parent']==name])

bb=dict(meta=dict(format_version='4.10',model_format='free',box_uv=True),
    name='执钟者·玄铎 | Bell Warden',model_identifier='relicward:bell_warden',
    visible_box=[7,7,3.5],resolution=dict(width=TEX,height=TEX),
    elements=elements,outliner=[outliner('root')],
    textures=[dict(name='bell_warden.png',uuid=uid('texture'),id='0',width=TEX,height=TEX,
        uv_width=TEX,uv_height=TEX,particle=False,mode='bitmap',saved=True,
        source='data:image/png;base64,'+base64.b64encode((EDIT/'bell_warden.png').read_bytes()).decode())])
(EDIT/'bell_warden.bbmodel').write_text(json.dumps(bb,ensure_ascii=False,indent=2),encoding='utf-8')
(ROOT/'art/model_spec.json').write_text(json.dumps(dict(bones=bones,cubes=cubes,palette=PALETTE,texture_size=TEX),indent=2),encoding='utf-8')

def jf(x): return f'{float(x):.3f}F'
lines=[]
for b in bones:
    bname=b['name']; pivot=b['pivot'] if b['parent'] else [0,24,0]
    parent=b['parent'] or 'mesh.getRoot()'
    chain='CubeListBuilder.create()'
    for c in cubes:
        if c['bone']!=bname: continue
        u,v=c['uv']; vals=', '.join(jf(x) for x in c['pos']+c['size'])
        chain+=f'\n                /* {c["name"]} */ .texOffs({u}, {v}).addBox({vals})'
    lines.append(f'        PartDefinition {bname} = {parent}.addOrReplaceChild("{bname}", {chain},\n                PartPose.offset({", ".join(jf(x) for x in pivot)}));')

source='''package cn.suiyi.relicward.client;

import cn.suiyi.relicward.RelicWard;
import cn.suiyi.relicward.entity.BellWarden;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Generated by tools/generate_model.py. Edit the shared source, not this geometry. */
public final class BellWardenModel extends HierarchicalModel<BellWarden> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(RelicWard.ID, "bell_warden"), "main");
    private final ModelPart root, body, head, bell, clapper, rightArm, leftArm, rightLeg, leftLeg, rightShin, leftShin;
    private final WardenAnimator motion;

    public BellWardenModel(ModelPart baked) {
        root = baked.getChild("root");
        body = root.getChild("body");
        head = body.getChild("head");
        bell = body.getChild("bell");
        clapper = bell.getChild("clapper");
        rightArm = body.getChild("right_arm");
        leftArm = body.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
        rightShin = rightLeg.getChild("right_shin");
        leftShin = leftLeg.getChild("left_shin");
        motion = new WardenAnimator(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
GEOMETRY
        return LayerDefinition.create(mesh, 512, 512);
    }

    @Override public ModelPart root() { return root; }

    @Override
    public void setupAnim(BellWarden entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        float stride = Mth.cos(limbSwing * 0.55F);
        float walk = Math.min(limbSwingAmount, 0.8F);
        rightLeg.xRot = stride * 0.48F * walk;
        leftLeg.xRot = -stride * 0.48F * walk;
        rightShin.xRot = Math.max(0, -stride) * 0.20F * walk;
        leftShin.xRot = Math.max(0, stride) * 0.20F * walk;
        rightArm.xRot = -stride * 0.12F * walk;
        leftArm.xRot = stride * 0.32F * walk;
        rightArm.zRot = 0.035F;
        leftArm.zRot = -0.035F;
        body.y = -40.0F + Mth.sin(ageInTicks * 0.065F) * 0.12F - Math.abs(Mth.sin(limbSwing * 0.55F)) * walk * 0.4F;
        body.zRot = stride * walk * 0.018F;
        head.yRot = Mth.clamp(netHeadYaw, -35.0F, 35.0F) * Mth.DEG_TO_RAD;
        head.xRot = Mth.clamp(headPitch, -15.0F, 20.0F) * Mth.DEG_TO_RAD;
        bell.zRot = Mth.sin(ageInTicks * 0.085F) * (0.035F + walk * 0.055F);
        clapper.zRot = -Mth.sin(ageInTicks * 0.085F + 0.55F) * 0.10F;
        motion.apply(entity, ageInTicks - entity.tickCount, limbSwing, limbSwingAmount);
    }
}
'''.replace('GEOMETRY','\n'.join(lines))
(ROOT/'src/main/java/cn/suiyi/relicward/client/BellWardenModel.java').write_text(source,encoding='utf-8')
print(f'Generated {len(cubes)} cuboids, {len(bones)} bones, {TEX}x{TEX} atlas.')
