"""Build the chapter's original pixel assets, data pack entries and 65x42x81 structure."""
from pathlib import Path
import gzip, json, random, struct
from PIL import Image, ImageDraw
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
AS=RES/'assets/relicward'
DATA=RES/'data/relicward'
rng=random.Random(8172)
def js(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf-8')
def png(path,image):path.parent.mkdir(parents=True,exist_ok=True);image.save(path)

# Pixel material tiles, hand-defined geometry and deterministic wear.
for name,base in [('bronze_bricks',(56,94,79)),('resonant_pillar',(110,89,46)),('echo_lamp',(69,134,113)),('court_altar',(124,94,49)),('teaching_bell',(149,115,54))]:
    im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
    for y in range(16):
        for x in range(16):
            n=rng.randint(-9,9);im.putpixel((x,y),tuple(max(0,min(255,c+n)) for c in base)+(255,))
    if name=='bronze_bricks':
        for y in (0,8):d.line((0,y,15,y),fill=(30,48,40));d.line((4 if y==0 else 12,y,4 if y==0 else 12,y+7),fill=(30,48,40))
    else:
        d.rectangle((0,0,15,15),outline=(57,60,43),width=1);d.rectangle((2,2,13,13),outline=(190,151,74),width=1)
        d.line([(5,11),(5,5),(10,5),(10,10),(8,10),(8,8)],fill=(241,202,107),width=1)
        if name=='echo_lamp':d.rectangle((4,4,11,11),fill=(165,215,160));d.line((7,4,7,11),fill=(245,214,132),width=2)
    png(AS/f'textures/block/{name}.png',im)
    js(AS/f'models/block/{name}.json',{'parent':'minecraft:block/cube_all','textures':{'all':f'relicward:block/{name}'}})
    js(AS/f'models/item/{name}.json',{'parent':f'relicward:block/{name}'})
    if name=='resonant_pillar':variants={'broken=false':{'model':'relicward:block/resonant_pillar'},'broken=true':{'model':'relicward:block/broken_pillar'}}
    elif name=='court_altar':variants={f'facing={f}':{'model':'relicward:block/court_altar','y':rot} for f,rot in [('south',0),('west',90),('north',180),('east',270)]}
    else:variants={'':{'model':f'relicward:block/{name}'}}
    js(AS/f'blockstates/{name}.json',{'variants':variants})
js(AS/'models/block/broken_pillar.json',{'textures':{'particle':'relicward:block/resonant_pillar'},'elements':[]})

# A visible miniature bell on a solid ritual plinth.
for name in ['court_altar','teaching_bell']:
    elements=[]
    for a,b in [([0,0,0],[16,4,16]),([1,4,3],[3,14,13]),([13,4,3],[15,14,13]),([1,13,3],[15,16,13]),([5,8,5],[11,12,11]),([4,6,4],[12,8,12]),([7,5,7],[9,6,9])]:
        elements.append({'from':a,'to':b,'faces':{f:{'texture':'#all'} for f in ['north','south','east','west','up','down']}})
    js(AS/f'models/block/{name}.json',{'textures':{'all':f'relicward:block/{name}','particle':f'relicward:block/{name}'},'elements':elements})

for name in ['bell_core','bronze_fragment','echo_maul','warden_pendant']:
    im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
    gold=(209,170,83);dark=(51,63,49);green=(64,114,92);light=(250,215,133)
    if name=='echo_maul':
        d.line((8,27,24,7),fill=dark,width=4);d.line((9,27,25,7),fill=gold,width=1)
        d.polygon([(11,7),(18,0),(30,9),(23,16)],fill=green,outline=dark)
        d.line((12,7,24,16),fill=gold,width=3);d.line((18,1,30,10),fill=gold,width=3);d.line((20,6,23,10),fill=light,width=2)
    elif name=='bronze_fragment':
        d.polygon([(7,8),(22,4),(28,20),(18,27),(5,23)],fill=green,outline=gold)
        d.line([(10,20),(10,11),(21,11),(21,16),(17,16)],fill=gold,width=2)
    else:
        if name=='warden_pendant':d.arc((5,0,26,19),0,350,fill=gold,width=2)
        y=12 if name=='warden_pendant' else 5
        d.rectangle((12,y,19,y+3),fill=dark);d.rectangle((10,y+3,21,y+6),fill=gold)
        d.polygon([(10,y+6),(21,y+6),(24,y+16),(7,y+16)],fill=gold,outline=dark)
        d.rectangle((6,y+15,25,y+18),fill=gold,outline=dark);d.line([(15,y+5),(15,y+10),(18,y+10),(18,y+15)],fill=light,width=2)
    png(AS/f'textures/item/{name}.png',im)
    js(AS/f'models/item/{name}.json',{'parent':'minecraft:item/handheld' if name=='echo_maul' else 'minecraft:item/generated','textures':{'layer0':f'relicward:item/{name}'}})
js(AS/'models/item/court_puppet_spawn_egg.json',{'parent':'minecraft:item/template_spawn_egg'})
im=Image.new('RGBA',(64,64),(60,94,74,255));d=ImageDraw.Draw(im)
for y in range(64):
    for x in range(64):
        n=rng.randint(-8,8);im.putpixel((x,y),(65+n,96+n,73+n,255))
for y in range(0,64,8):d.line((0,y,63,y),fill=(148,117,61),width=2)
d.rectangle((40,20,46,23),fill=(244,212,125))
png(AS/'textures/entity/court_puppet.png',im)

names={
 'entity.relicward.court_puppet':('巡庭铜傀','Court Puppet'),
 'item.relicward.court_puppet_spawn_egg':('巡庭铜傀刷怪蛋','Court Puppet Spawn Egg'),
 'item.relicward.bell_core':('沉钟之核','Sunken Bell Core'),
 'item.relicward.bronze_fragment':('铜律碎片','Bronze Fragment'),
 'item.relicward.echo_maul':('回响钟槌','Echo Maul'),
 'item.relicward.warden_pendant':('守钟坠','Warden Pendant'),
 'block.relicward.bronze_bricks':('青铜纹砖','Engraved Bronze Bricks'),
 'block.relicward.echo_lamp':('余响灯','Echo Lamp'),
 'block.relicward.resonant_pillar':('谐振柱','Resonant Pillar'),
 'block.relicward.court_altar':('启战钟 · 余响龛','Court Bell / Reward Altar'),
 'block.relicward.teaching_bell':('校律钟','Teaching Bell'),
 'tooltip.relicward.model_preview':('休眠守卫 · 生存模式右键挑战；古庭内击败可领奖','Dormant guardian. Right-click in survival to challenge; court victories grant rewards.'),
 'tooltip.relicward.maul':('主手蓄力 0.9 秒后松开：前方震击，冷却 10 秒','Charge in main hand for 0.9s, then release: frontal shockwave, 10s cooldown.'),
 'tooltip.relicward.pendant':('副手装备 3 秒后：一次 ≥6 点战斗伤害减少 30%，冷却 25 秒','Equip offhand for 3s: reduce a combat hit of at least 6 damage by 30%. Cooldown: 25s.'),
 'message.relicward.begin':('玄铎苏醒了。跃过低钟波，借冲撞击碎铜柱！','The Bell Warden awakens. Jump over low waves and lure its charge into the pillars!'),
 'message.relicward.use_altar':('请在主庭入口右键启战钟。','Use the Court Bell at the arena entrance.'),
 'message.relicward.survival_required':('请在生存/冒险模式、非和平难度下敲钟挑战。','Challenge in survival/adventure mode with peaceful difficulty disabled.'),
 'message.relicward.already_active':('校律正在进行，可入庭助战。','The trial is active. Enter the court to join.'),
 'message.relicward.obstructed':('场地受阻：%s, %s, %s。请修复地面或清理标记处。','Arena obstructed at %s, %s, %s. Restore the floor or clear the marked block.'),
 'message.relicward.victory':('钟止山息。参战者可右键入口启战钟领取各自余响。','The bell falls silent. Participants may claim personal rewards at the Court Bell.'),
 'message.relicward.reward_claimed':('已领取本次余响。','Your rewards have been claimed.'),
 'message.relicward.inventory_full':('背包空间不足，剩余奖励已保留，稍后再次领取。','Inventory full. Remaining rewards are saved for later.'),
 'message.relicward.cooldown':('重鸣尚需 %s 秒。','The next trial will be ready in %s seconds.'),
 'message.relicward.placed':('沉钟古庭已放置；启战钟位于 %s, %s, %s。','Court placed. Court Bell: %s, %s, %s.'),
 'boss.relicward.phase_one':(' · 守律〔钟甲 %s 层〕',' · First Toll [%s armor layers]'),
 'boss.relicward.phase_two':(' · 破律〔钟甲 %s 层〕',' · Broken Toll [%s armor layers]'),
 'boss.relicward.transform':(' · 重整钟律',' · Reforging the rhythm'),
 'boss.relicward.stagger':(' · 失衡！',' · Staggered!'),
 'death.attack.relicward.resonance':('%1$s 被 %2$s 的钟鸣震碎了','%1$s was shattered by %2$s\'s resonance'),
 'advancement.relicward.victory':('钟止山息','The Bell Falls Silent'),
 'advancement.relicward.victory.desc':('完成古庭校律并领取个人奖励','Complete the court trial and claim your rewards')}
for lang,index in [('zh_cn',0),('en_us',1)]:
    path=AS/f'lang/{lang}.json';old=json.loads(path.read_text(encoding='utf-8'));old.update({k:v[index] for k,v in names.items()});js(path,old)

js(DATA/'damage_type/resonance.json',{'message_id':'relicward.resonance','scaling':'never','exhaustion':.1})
js(DATA/'damage_type/impact.json',{'message_id':'relicward.resonance','scaling':'never','exhaustion':.1})
js(RES/'data/minecraft/tags/damage_type/bypasses_shield.json',{'replace':False,'values':['relicward:resonance']})
for name in ['bronze_bricks','echo_lamp']:
    js(DATA/f'loot_tables/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'relicward:{name}'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
js(DATA/'loot_tables/entities/court_puppet.json',{'type':'minecraft:entity','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'minecraft:copper_ingot','functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':0,'max':2}}]}]}]})
js(DATA/'loot_tables/entities/bell_warden.json',{'type':'minecraft:entity','pools':[]})
js(DATA/'loot_tables/chests/court.json',{'type':'minecraft:chest','pools':[{'rolls':5,'entries':[{'type':'minecraft:item','name':name,'weight':weight,'functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':low,'max':high}}]} for name,weight,low,high in [('minecraft:iron_ingot',3,2,4),('minecraft:copper_ingot',4,4,8),('minecraft:bread',4,2,5),('relicward:bronze_bricks',2,4,8)]]}]})
js(DATA/'recipes/echo_maul.json',{'type':'minecraft:crafting_shaped','pattern':['FCF','FIF',' S '],'key':{'F':{'item':'relicward:bronze_fragment'},'C':{'item':'relicward:bell_core'},'I':{'item':'minecraft:iron_ingot'},'S':{'item':'minecraft:stick'}},'result':{'item':'relicward:echo_maul'}})
js(DATA/'recipes/warden_pendant.json',{'type':'minecraft:crafting_shapeless','ingredients':[{'item':n} for n in ['relicward:bell_core']+['relicward:bronze_fragment']*4+['minecraft:string']*2],'result':{'item':'relicward:warden_pendant'}})
js(DATA/'recipes/bronze_bricks.json',{'type':'minecraft:crafting_shapeless','ingredients':[{'item':'relicward:bronze_fragment'},{'item':'minecraft:stone_bricks'}],'result':{'item':'relicward:bronze_bricks','count':8}})
js(DATA/'recipes/echo_lamp.json',{'type':'minecraft:crafting_shapeless','ingredients':[{'item':'relicward:bronze_fragment'},{'item':'minecraft:glowstone'}],'result':{'item':'relicward:echo_lamp','count':4}})
js(DATA/'advancements/bell_silenced.json',{'display':{'icon':{'item':'relicward:bell_core'},'title':{'translate':'advancement.relicward.victory'},'description':{'translate':'advancement.relicward.victory.desc'},'background':'minecraft:textures/block/deepslate_tiles.png','frame':'challenge','show_toast':True,'announce_to_chat':True,'hidden':False},'criteria':{'victory':{'trigger':'minecraft:impossible'}}})
js(DATA/'tags/worldgen/biome/has_court.json',{'replace':False,'values':['minecraft:forest','minecraft:birch_forest','minecraft:old_growth_birch_forest','minecraft:taiga','minecraft:old_growth_spruce_taiga','minecraft:old_growth_pine_taiga','minecraft:flower_forest','minecraft:plains','minecraft:sunflower_plains','minecraft:savanna','minecraft:meadow','minecraft:dark_forest']})
js(DATA/'worldgen/structure/resonant_court.json',{'type':'relicward:resonant_court','biomes':'#relicward:has_court','step':'surface_structures','spawn_overrides':{},'terrain_adaptation':'beard_thin'})
js(DATA/'worldgen/structure_set/resonant_court.json',{'structures':[{'structure':'relicward:resonant_court','weight':1}],'placement':{'type':'relicward:court_spread','salt':8172401}})
js(DATA/'worldgen/template_pool/court/start.json',{'name':'relicward:court/start','fallback':'minecraft:empty','elements':[{'weight':1,'element':{'element_type':'minecraft:single_pool_element','location':'relicward:resonant_court','projection':'rigid','processors':'minecraft:empty'}}]})

# Template layout. Y=4 is the floor; altar (32,5,41) looks south; arena center (32,5,24).
blocks={}
def put(x,y,z,name,props=None,nbt=None):
    if 0<=x<65 and 0<=y<42 and 0<=z<81:blocks[x,y,z]=(name,props or {},nbt)
def fill(x0,y0,z0,x1,y1,z1,name,props=None):
    for y in range(y0,y1+1):
        for z in range(z0,z1+1):
            for x in range(x0,x1+1):put(x,y,z,name,props)
stone='minecraft:stone_bricks';dark='minecraft:deepslate_tiles';gold='relicward:bronze_bricks';air='minecraft:air'
fill(0,0,0,64,41,80,air)
for y,inset in [(0,4),(1,3),(2,2),(3,1),(4,2)]:fill(inset,y,1,64-inset,y,78,stone if y>2 else 'minecraft:cobbled_deepslate')
for (x,y,z),(name,props,tag) in list(blocks.items()):
    if name==stone and rng.random()<.14:put(x,y,z,rng.choice(['minecraft:mossy_stone_bricks','minecraft:cracked_stone_bricks']))
# Arena inlays, no dangerous floor pits.
for x in range(12,53):
    for z in range(4,45):
        radius=((x-32)**2+(z-24)**2)**.5
        if 17.2<radius<18.5 or radius<3 or abs(radius-10)<.45:put(x,4,z,gold)
        elif x==32 or z==24:put(x,4,z,'minecraft:polished_deepslate')
# Low perimeter walls and roofed side galleries.
for x0,z0,x1,z1 in [(10,2,54,3),(10,45,54,46),(10,4,11,44),(53,4,54,44)]:
    fill(x0,5,z0,x1,10,z1,stone);fill(x0,10,z0,x1,10,z1,gold);fill(x0,11,z0,x1,11,z1,dark)
fill(27,5,45,37,10,46,air)
for x in (7,57):
    for z in (7,17,29,40):
        fill(x-1,5,z-1,x+1,5,z+1,gold);fill(x,6,z,x,13,z,'minecraft:stripped_dark_oak_log');put(x,12,z+1,'minecraft:lantern',{'hanging':'true'})
    fill(x-3,14,4,x+3,14,43,dark);fill(x-2,15,4,x+2,15,43,dark);fill(x-1,16,4,x+1,16,43,gold)
for x,z in [(12,5),(52,5),(12,43),(52,43)]:
    fill(x-1,5,z-1,x+1,12,z+1,stone);fill(x-2,13,z-2,x+2,13,z+2,dark);put(x,14,z,'relicward:echo_lamp')
# The three functional pillars are separate protected segments with a broken state.
for x,z in [(32,11),(21,31),(43,31)]:
    fill(x-2,4,z-2,x+2,4,z+2,gold)
    fill(x-1,5,z-1,x+1,10,z+1,'relicward:resonant_pillar',{'broken':'false'})
# Rear broken bell tower, hollow interior and heavy layered eaves.
for y in range(5,31):
    for x,z in [(27,1),(37,1),(27,6),(37,6)]:fill(x,y,z,x+1,y,z+1,stone if y%5 else gold)
for y in (13,22,31):
    fill(25,y,0,40,y,9,dark);fill(26,y+1,0,39,y+1,8,dark);fill(28,y+2,1,37,y+2,6,gold)
fill(30,25,2,35,28,5,'minecraft:cut_copper');fill(29,24,1,36,24,6,'minecraft:oxidized_cut_copper')
fill(31,29,3,34,30,4,'minecraft:chain',{'axis':'y','waterlogged':'false'})
fill(31,34,3,33,39,5,stone);fill(31,40,3,34,40,5,gold)
# Side courts with tiled pavilions.
for x0,x1 in [(3,24),(40,61)]:
    for a,b,c,d in [(x0,49,x1,49),(x0,67,x1,67),(x0,50,x0,66),(x1,50,x1,66)]:fill(a,5,b,c,8,d,stone)
    fill(x0,9,49,x1,9,49,dark);fill(x0,9,67,x1,9,67,dark)
    inner=24 if x0==3 else 40;fill(inner,5,55,inner,8,61,air)
    for x in (x0+3,x1-3):
        for z in (52,64):fill(x,5,z,x,11,z,'minecraft:stripped_dark_oak_log');put(x,10,z+1,'minecraft:lantern',{'hanging':'true'})
    fill(x0+1,12,50,x1-1,12,66,dark);fill(x0+3,13,51,x1-3,13,65,dark);fill(x0+5,14,52,x1-5,14,64,gold)
    # Skylight keeps the optional loot rooms readable.
    fill(x0+7,12,56,x1-7,14,60,air)
    put((x0+x1)//2,5,64,'minecraft:chest',{'facing':'north','type':'single','waterlogged':'false'}, {'id':'minecraft:chest','LootTable':'relicward:chests/court'})
fill(11,5,53,18,5,54,'minecraft:oxidized_cut_copper');fill(11,6,53,13,6,54,'minecraft:cut_copper')
for x in range(46,56,3):put(x,5,53,'minecraft:anvil',{'facing':'north'})
# Central teaching corridor and entrance gate.
for z in (49,60,70):
    for x in (27,37):fill(x,5,z,x,11,z,stone);put(x,10,z-1,'minecraft:lantern',{'hanging':'true'})
    fill(26,12,z,38,12,z,dark)
put(32,5,64,'relicward:teaching_bell',nbt={'id':'relicward:teaching_bell'})
for x in (22,42):fill(x,5,74,x+1,14,75,stone);fill(x-1,14,73,x+2,15,76,gold)
fill(21,15,73,44,15,76,dark);fill(23,16,73,42,16,76,dark);fill(25,17,74,40,17,75,gold)
for z,y in [(78,3),(79,2),(80,1)]:fill(26,y,z,38,y,z,'minecraft:stone_brick_stairs',{'facing':'north','half':'bottom','shape':'straight','waterlogged':'false'})
put(32,5,41,'relicward:court_altar',{'facing':'south'}, {'id':'relicward:court_altar'})
for x,z in [(26,41),(38,41),(15,18),(49,18),(32,48),(29,72),(35,72)]:put(x,4,z,'relicward:echo_lamp')
def sign(x,z,lines):
    messages=[json.dumps({'text':line},ensure_ascii=False) for line in lines]
    put(x,5,z,'minecraft:dark_oak_sign',{'rotation':'0','waterlogged':'false'}, {'id':'minecraft:sign','front_text':{'messages':messages,'color':'white','has_glowing_text':True}})
sign(30,43,['启战钟','右键开始校律','全员撤离十秒','重置后可重试'])
sign(34,43,['引冲撞至铜柱','横移避开重击','柱碎则钟甲衰','失衡时可反击'])
sign(30,67,['鸣纹将至','跃而避之','两侧可绕行','先观察再前进'])
sign(32,77,['沉钟古庭','钟止而山未息','守者不得离庭',''])
# Named jigsaw anchors world generation at the arena center. It becomes a floor tile.
from polish_assets import items as polish_items, architecture as polish_architecture
polish_items(AS,js,png)
polish_architecture(blocks,put,fill)
from generate_combat_assets import generate as combat_assets
combat_assets(AS,DATA,js,png)
from generate_equipment_assets import generate as equipment_assets
equipment_assets(AS,js,png)
from refine_court import generate as refine_court
refine_court(AS,js,blocks,put,fill)
from expand_chapter import generate as expand_chapter
expand_chapter(AS,DATA,js,png,blocks,put,fill)
put(32,4,24,'minecraft:jigsaw',{'orientation':'up_north'}, {'id':'minecraft:jigsaw','name':'relicward:center','target':'minecraft:empty','pool':'minecraft:empty','final_state':'relicward:bronze_bricks','joint':'rollable'})

# Minimal NBT writer for typed structure data.
def string(s):b=s.encode('utf-8');return struct.pack('>H',len(b))+b
def named(t,n,p):return bytes([t])+string(n)+p
def integer(n):return struct.pack('>i',n)
def tag(n,v):
    if isinstance(v,bool):return named(1,n,bytes([int(v)]))
    if isinstance(v,int):return named(3,n,integer(v))
    if isinstance(v,str):return named(8,n,string(v))
    if isinstance(v,dict):return named(10,n,compound(v))
    if isinstance(v,list):
        typ=3 if v and isinstance(v[0],int) else 8 if v and isinstance(v[0],str) else 10
        f=integer if typ==3 else string if typ==8 else compound
        return named(9,n,bytes([typ])+integer(len(v))+b''.join(f(i) for i in v))
    raise TypeError(v)
def compound(d):return b''.join(tag(n,v) for n,v in d.items())+b'\0'
# Keep every intentional air voxel within the 65x42x81 template.
palette=[];indices={};records=[]
for pos,(name,properties,nbt) in blocks.items():
    key=(name,tuple(sorted(properties.items())))
    if key not in indices:
        indices[key]=len(palette);state={'Name':name}
        if properties:state['Properties']=properties
        palette.append(state)
    record={'pos':list(pos),'state':indices[key]}
    if nbt:record['nbt']=nbt
    records.append(record)
template={'DataVersion':3465,'size':[65,42,81],'palette':palette,'blocks':records,'entities':[]}
path=DATA/'structures/resonant_court.nbt';path.parent.mkdir(parents=True,exist_ok=True)
path.write_bytes(gzip.compress(named(10,'',compound(template)),mtime=0))
print(f'Chapter assets written. Court: {len(records)} blocks, {len(palette)} states, {path.stat().st_size} bytes.')

# Final equipment pass preserves the revised custom armour and material ramps.
from refine_armor081 import main as refine_armor081
refine_armor081()

# Preserve arsenal mechanics and standard JER-readable boss loot.
from generate_088_resources import main as generate_088
generate_088()
