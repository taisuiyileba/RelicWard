"""Complete single-boss chapter: functional loot crafts, pixel art and selective clearance."""
from PIL import Image,ImageDraw
from pathlib import Path
import json,re,random

def generate(AS,DATA,js,png,blocks,put,fill):
    # Continuous entrance ascent: remove the foundation ring above the stairs.
    old_sign=blocks.get((32,5,77))
    if old_sign:put(24,5,76,*old_sign)
    for z,y in [(80,1),(79,2),(78,3),(77,4)]:
        fill(26,y+1,z,38,8,z,'minecraft:air')
        fill(26,y,z,38,y,z,'minecraft:stone_brick_stairs',{'facing':'north','half':'bottom','shape':'straight','waterlogged':'false'})
    names={
      'puppet_gear':('铜傀机轮','Puppet Gear'),'resonant_plate':('钟铸铜板','Resonant Plate'),
      'binding_seal':('契律印','Binding Seal'),'repair_paste':('铜傀修补膏','Puppet Repair Paste'),
      'bell_shield':('守律铜盾','Warden Shield'),'bell_helmet':('钟铸面甲','Bellforged Helmet'),
      'bell_chestplate':('钟铸胸甲','Bellforged Chestplate'),'bell_leggings':('钟铸护腿','Bellforged Leggings'),
      'bell_boots':('钟铸战靴','Bellforged Boots'),'bell_anvil':('钟铸砧','Bellforged Anvil'),
      'chime_lantern':('垂铃灯','Chime Lantern'),'chiseled_bronze':('回纹铜砖','Chiseled Bronze')}
    messages={
      'boss.relicward.phase_one':(' · 守律',' · First Toll'),'boss.relicward.phase_two':(' · 破律',' · Broken Toll'),
      'jade.relicward.armor':('钟甲：%s 层 · 减伤 %s%%','Clock armor: %s layers · %s%% damage reduction'),
      'config.jade.plugin_relicward.clock_armor':('玄铎钟甲','Clock Armor'),'config.jade.plugin_relicward.companion':('铜傀契约','Puppet Bond'),
      'jade.relicward.wild':('未契约 · 使用契律印收服','Unbound · Use a Binding Seal'),
      'jade.relicward.owner':('契主：%s','Owner: %s'),'message.relicward.bound':('契约已成：空手右键切换跟随/待命；修补膏恢复生命。','Bound: empty-hand use toggles follow/stay; repair paste restores health.'),
      'message.relicward.stay':('待命 · 停止跟随与攻击','Staying · No following or attacking'),'message.relicward.follow':('跟随 · 协助契主战斗','Following · Assists its owner'),
      'tooltip.relicward.binding_seal':('右键未契约铜傀：消耗 1 枚收服；伙伴生命 32，可跟随、护主与待命。','Use on a wild puppet: consumes one to bind. Companion health 32; follows, defends and stays.'),
      'tooltip.relicward.repair_paste':('右键自己的铜傀伙伴：恢复 12 点生命；满血不消耗。','Use on your puppet: restores 12 health; not consumed at full health.'),
      'tooltip.relicward.bell_shield':('耐久 768；右键格挡；用钟铸铜板维修。','768 durability; hold use to block; repair with Resonant Plates.'),
      'tooltip.relicward.bell_anvil':('固定式铁砧：修复、合并附魔与改名；不掉落、不磨损，经验费用遵循原版。','Anchored anvil: repair, combine enchantments and rename. No falling or wear; vanilla XP costs.'),
      'tooltip.relicward.bell_armor':('钟铸护甲：每件提供 5% 抗击退；用钟铸铜板维修。','Bellforged armor: 5% knockback resistance per piece; repaired with Resonant Plates.')}
    for lang,i in [('zh_cn',0),('en_us',1)]:
        p=AS/f'lang/{lang}.json';d=json.loads(p.read_text(encoding='utf-8'));d.update({('block' if n in ['bell_anvil','chime_lantern','chiseled_bronze'] else 'item')+'.relicward.'+n:v[i] for n,v in names.items()});d.update({k:v[i] for k,v in messages.items()});js(p,d)
    def recipe(name,pattern,key,out=1):js(DATA/f'recipes/{name}.json',{'type':'minecraft:crafting_shaped','pattern':pattern,'key':{k:{'item':v} for k,v in key.items()},'result':{'item':'relicward:'+name,'count':out}})
    recipe('resonant_plate',['IFI','FCF','IFI'],{'I':'minecraft:iron_ingot','F':'relicward:bronze_fragment','C':'relicward:bell_core'},8)
    for name,pattern in [('bell_helmet',['PPP','P P']),('bell_chestplate',['P P','PPP','PPP']),('bell_leggings',['PPP','P P','P P']),('bell_boots',['P P','P P'])]:recipe(name,pattern,{'P':'relicward:resonant_plate'})
    recipe('bell_shield',['PGP','PPP',' P '],{'P':'relicward:resonant_plate','G':'relicward:puppet_gear'})
    recipe('binding_seal',[' F ','GCG',' F '],{'F':'relicward:bronze_fragment','C':'relicward:bell_core','G':'relicward:puppet_gear'})
    recipe('repair_paste',['CGC'],{'C':'minecraft:copper_ingot','G':'minecraft:slime_ball'},4)
    recipe('bell_anvil',['PPP',' G ','PAP'],{'P':'relicward:resonant_plate','G':'relicward:puppet_gear','A':'minecraft:anvil'})
    recipe('chime_lantern',[' N ','NLN',' G '],{'N':'minecraft:copper_ingot','L':'minecraft:lantern','G':'relicward:puppet_gear'},2)
    recipe('chiseled_bronze',['BB','BB'],{'B':'relicward:bronze_bricks'},4)
    for name in ['resonant_plate','bell_shield','bell_helmet','bell_chestplate','bell_leggings','bell_boots','bell_anvil','chime_lantern','chiseled_bronze','binding_seal','repair_paste']:
        material='relicward:bell_core' if name in ['resonant_plate','binding_seal'] else 'relicward:bronze_bricks' if name=='chiseled_bronze' else 'minecraft:copper_ingot' if name in ['repair_paste','chime_lantern'] else 'relicward:resonant_plate'
        js(DATA/f'advancements/recipes/{name}.json',{'parent':'minecraft:recipes/root','criteria':{'has_material':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':[material]}]}}},'rewards':{'recipes':['relicward:'+name]}})
    pools=[]
    for name,lo,hi,chance in [('relicward:puppet_gear',1,2,1),('relicward:bronze_fragment',0,2,1),('minecraft:iron_nugget',2,5,1),('minecraft:copper_ingot',0,2,1),('relicward:repair_paste',1,1,.15)]:
        cond=[{'condition':'minecraft:killed_by_player'}]
        if chance<1:cond.append({'condition':'minecraft:random_chance','chance':chance})
        pools.append({'rolls':1,'conditions':cond,'entries':[{'type':'minecraft:item','name':name,'functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':lo,'max':hi}},{'function':'minecraft:looting_enchant','count':{'type':'minecraft:uniform','min':0,'max':1}}]}]})
    js(DATA/'loot_tables/entities/court_puppet.json',{'type':'minecraft:entity','pools':pools})
    # Original 16 px silhouettes; colors match bronze armour and copper mechanisms.
    dark=(36,44,39,255);gold=(192,145,67,255);light=(238,196,106,255);green=(52,103,84,255);shade=(103,75,41,255)
    for name in names:
        if name in ['bell_anvil','chime_lantern','chiseled_bronze']:continue
        im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
        def rect(box,c):d.rectangle(box,fill=c,outline=dark)
        if name=='puppet_gear':
            for b in [(6,1,9,14),(1,6,14,9),(3,3,12,12)]:rect(b,gold)
            d.rectangle((5,5,10,10),fill=shade);d.rectangle((6,6,9,9),fill=(0,0,0,0));d.line((4,4,11,4),fill=light)
        elif name=='resonant_plate':
            d.polygon([(3,3),(11,1),(14,11),(5,14),(1,11)],fill=dark);d.polygon([(4,4),(10,2),(12,10),(5,12),(2,10)],fill=green);d.line([(4,4),(10,2),(12,10)],fill=light);d.line([(5,10),(5,6),(9,5),(9,8)],fill=gold)
        elif name=='binding_seal':
            rect((6,1,9,4),gold);rect((3,4,12,12),green);d.rectangle((4,5,11,11),outline=gold);d.line([(6,9),(6,7),(9,7),(9,10)],fill=light);d.line((5,13,4,15),fill=gold);d.line((10,13,11,15),fill=gold)
        elif name=='repair_paste':
            rect((5,1,10,3),gold);rect((4,3,11,5),shade);rect((3,5,12,13),green);d.line((4,6,4,11),fill=(113,173,120));d.rectangle((5,8,10,11),fill=gold);d.line((6,9,9,9),fill=light)
        elif name in ['bell_helmet','bell_chestplate','bell_leggings','bell_boots']:
            from generate_armor_art import create_item_icons
            im=create_item_icons()[name]
        else:rect((3,1,12,12),green);rect((5,11,10,14),gold);d.rectangle((4,2,11,11),outline=gold)
        png(AS/f'textures/item/{name}.png',im);js(AS/f'models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'relicward:item/{name}'}})
    # Physical shield shares the bronze and patina materials, including blocking pose.
    def box(a,b,tex):return {'from':a,'to':b,'faces':{k:{'texture':tex,'uv':[0,0,16,16]} for k in ['up','down','north','south','east','west']}}
    from refine_shield083 import build as build_shield
    build_shield()
    # Anchored anvil shape follows vanilla collision; decorative trim is flush.
    elements=[box([2,0,2],[14,4,14],'#metal'),box([4,4,3],[12,5,13],'#gold'),box([6,5,4],[10,10,12],'#metal'),box([3,10,0],[13,16,16],'#metal'),box([3,10,1],[13,11,15],'#gold')]
    js(AS/'models/block/bell_anvil.json',{'parent':'minecraft:block/block','textures':{'metal':'relicward:block/bronze_bricks','gold':'relicward:block/maul_bronze','particle':'relicward:block/bronze_bricks'},'elements':elements})
    js(AS/'blockstates/bell_anvil.json',{'variants':{f'facing={k}':{'model':'relicward:block/bell_anvil','y':v} for k,v in [('north',0),('east',90),('south',180),('west',270)]}})
    js(AS/'models/block/chiseled_bronze.json',{'parent':'minecraft:block/cube_all','textures':{'all':'relicward:block/resonant_pillar'}});js(AS/'blockstates/chiseled_bronze.json',{'variants':{'':{'model':'relicward:block/chiseled_bronze'}}})
    # Lantern uses vanilla geometry with a compact bronze-framed original tile.
    im=Image.new('RGBA',(16,16),(0,0,0,0));d=ImageDraw.Draw(im);d.rectangle((0,0,15,15),fill=shade);d.rectangle((1,1,14,14),fill=gold);d.rectangle((3,3,12,12),fill=(142,195,136,255));d.line((7,3,7,12),fill=light,width=2);png(AS/'textures/block/chime_lantern.png',im)
    for suffix,parent in [('', 'template_lantern'),('_hanging','template_hanging_lantern')]:js(AS/f'models/block/chime_lantern{suffix}.json',{'parent':'minecraft:block/'+parent,'textures':{'lantern':'relicward:block/chime_lantern'}})
    js(AS/'blockstates/chime_lantern.json',{'variants':{'hanging=false':{'model':'relicward:block/chime_lantern'},'hanging=true':{'model':'relicward:block/chime_lantern_hanging'}}})
    for name in ['bell_anvil','chime_lantern','chiseled_bronze']:
        js(AS/f'models/item/{name}.json',{'parent':f'relicward:block/{name}'})
        js(DATA/f'loot_tables/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'relicward:'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    for path,vals in [('anvil',['relicward:bell_anvil']),('mineable/pickaxe',['relicward:bell_anvil','relicward:chime_lantern','relicward:chiseled_bronze']),('needs_iron_tool',['relicward:bell_anvil'])]:
        p=DATA.parent/'minecraft/tags/blocks'/f'{path}.json';old=json.loads(p.read_text()) if p.exists() else {'replace':False,'values':[]};old['values']=list(dict.fromkeys(old['values']+vals));js(p,old)
    # Bellforged Armour 3D textures matching Bell Warden boss aesthetic
    from generate_armor_art import create_layer_1, create_layer_2
    png(AS/'textures/models/armor/bell_layer_1.png', create_layer_1())
    png(AS/'textures/models/armor/bell_layer_2.png', create_layer_2())
    puppet_texture(AS,png)

def puppet_texture(AS,png):
    root=Path(__file__).resolve().parents[1];p=root/'src/main/java/cn/suiyi/relicward/client/CourtPuppetModel.java';s=p.read_text(encoding='utf-8')
    im=Image.new('RGBA',(128,128),(39,49,43,255));d=ImageDraw.Draw(im);rnd=random.Random(817208)
    x=y=rowh=index=0
    def cube(match):
        nonlocal x,y,rowh,index
        dims=[float(v.strip().rstrip('Ff')) for v in match.group(1).split(',')];w,h,depth=map(int,dims[3:6]);width=2*(w+depth);height=h+depth
        if x+width>128:x=0;y+=rowh+1;rowh=0
        assert y+height<=128
        u,v=x,y;x+=width+1;rowh=max(rowh,height)
        mat=(64,103,82) if index in [0,3,8,10,12,15,18,19] else (170,125,57)
        if index in [5,6]:mat=(245,214,130)
        if index==20:mat=(75,191,184)
        faces=[(u+depth,v,w,depth),(u+depth+w,v,w,depth),(u,v+depth,depth,h),(u+depth,v+depth,w,h),(u+depth+w,v+depth,depth,h),(u+2*depth+w,v+depth,w,h)]
        for fx,fy,fw,fh in faces:
            for yy in range(fy,fy+fh):
                for xx in range(fx,fx+fw):
                    n=rnd.choice([-8,-4,0,0,0,4,7]);im.putpixel((xx,yy),tuple(max(0,min(255,c+n)) for c in mat)+(255,))
            if fw>2 and fh>2:
                d.line((fx,fy,fx+fw-1,fy),fill=tuple(min(255,c+25) for c in mat));d.line((fx,fy+fh-1,fx+fw-1,fy+fh-1),fill=tuple(max(0,c-28) for c in mat))
                if mat[0]>100 and index not in [5,6] and fw>=4 and fh>=4:
                    # Copper catches a bright edge; small oxidized pits sit in the recesses.
                    d.rectangle((fx,fy,fx+fw-1,fy+fh-1),outline=(102,76,41))
                    d.line((fx+1,fy+1,fx+fw-2,fy+1),fill=(211,164,86))
                    for px,py in [(fx+1,fy+fh-2),(fx+fw-2,fy+fh-2)]:d.point((px,py),fill=(73,108,80))
                    if fw>=6 and fh>=6:
                        d.rectangle((fx+2,fy+2,fx+fw-3,fy+fh-3),outline=(137,98,46))
                        d.point((fx+2,fy+2),fill=(229,184,100))
                if mat[0]<100 and fw>=5 and fh>=5:
                    d.rectangle((fx+1,fy+1,fx+fw-2,fy+fh-2),outline=(169,126,62));d.point((fx+2,fy+2),fill=(226,180,94))
        index+=1
        return f'.texOffs({u},{v}).addBox({match.group(1)})'
    s=re.sub(r'\.texOffs\(\d+,\d+\)\.addBox\(([^)]+)\)',cube,s);p.write_text(s,encoding='utf-8');png(AS/'textures/entity/court_puppet.png',im)
    print(f'Puppet atlas: {index} independent box-UV islands, no overlapping materials.')
