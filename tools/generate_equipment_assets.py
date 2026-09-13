from PIL import Image,ImageDraw
import copy,json,random

def rotate_handle(elements):
    # Rotate geometry +90 degrees about its local Y axis through the handle (8, y, 8).
    sides={'north':'west','west':'south','south':'east','east':'north','up':'up','down':'down'}
    for e in elements:
        a,b=e['from'],e['to'];e['from']=[a[2],a[1],16-b[0]];e['to']=[b[2],b[1],16-a[0]]
        e['faces']={sides[k]:v for k,v in e['faces'].items()}
        for face in ['up','down']:
            if face in e['faces']:e['faces'][face]['rotation']=(e['faces'][face].get('rotation',0)+(90 if face=='up' else 270))%360

def generate(assets,js,png):
    rng=random.Random(603)
    for name,color in [('maul_bronze',(142,105,54)),('maul_grip',(65,51,36))]:
        im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
        for y in range(16):
            for x in range(16):
                n=rng.choice([-5,-3,0,0,3,5]);im.putpixel((x,y),tuple(v+n for v in color)+(255,))
        if name=='maul_bronze':
            d.rectangle((0,0,15,15),outline=(66,53,31));d.line((1,1,14,1),fill=(218,172,91));d.line((1,1,1,14),fill=(187,139,68))
            for x,y in [(3,3),(12,3),(3,12),(12,12)]:d.point((x,y),fill=(235,195,113));d.point((x,y+1),fill=(89,67,38))
        else:
            for y in (2,6,10,14):d.line((0,y,15,y),fill=(39,34,25));d.line((0,y+1,15,y+1),fill=(98,77,48))
        png(assets/f'textures/block/{name}.png',im)
    elements=[]
    def box(a,b,texture):elements.append({'from':a,'to':b,'faces':{side:{'texture':texture,'uv':[0,0,16,16]} for side in ['north','south','west','east','up','down']}})
    box([7,-4,7],[9,19,9],'#grip');box([6,0,6],[10,2,10],'#bronze');box([6,12,6],[10,14,10],'#bronze')
    box([4,16,2],[12,23,14],'#patina');box([3,15,0],[13,24,3],'#bronze');box([3,15,13],[13,24,16],'#bronze')
    box([3.5,17,6],[12.5,22,10],'#bronze');box([3,18,7],[3.5,21,9],'#dark')
    rotate_handle(elements)
    held={'gui_light':'front','textures':{'grip':'relicward:block/maul_grip','bronze':'relicward:block/maul_bronze','patina':'relicward:block/bronze_bricks','dark':'minecraft:block/polished_deepslate','particle':'relicward:block/maul_bronze'},'elements':elements,
        'display':{'gui':{'rotation':[25,225,0],'translation':[0,-2,0],'scale':[.62,.62,.62]},
        'firstperson_righthand':{'rotation':[0,-75,15],'translation':[0,-1,0],'scale':[.43,.43,.43]},
        'firstperson_lefthand':{'rotation':[0,75,-15],'translation':[0,-1,0],'scale':[.43,.43,.43]},
        'thirdperson_righthand':{'rotation':[0,-90,0],'translation':[0,2,0],'scale':[.7,.7,.7]},
        'thirdperson_lefthand':{'rotation':[0,90,0],'translation':[0,2,0],'scale':[.7,.7,.7]},
        'ground':{'translation':[0,3,0],'scale':[.4,.4,.4]},'fixed':{'rotation':[0,90,0],'scale':[.6,.6,.6]}}}
    js(assets/'models/item/echo_maul_held.json',held)
    js(assets/'models/item/echo_maul.json',{'parent':'relicward:item/echo_maul_held','overrides':[{'predicate':{'relicward:charging':1},'model':'relicward:item/echo_maul_charging'}]})
    charging={'parent':'relicward:item/echo_maul_held','display':{
        'firstperson_righthand':{'rotation':[-35,-65,-20],'translation':[-1,2,0],'scale':[.43,.43,.43]},
        'firstperson_lefthand':{'rotation':[-35,65,20],'translation':[-1,2,0],'scale':[.43,.43,.43]},
        'thirdperson_righthand':{'rotation':[-65,-90,0],'translation':[0,4,0],'scale':[.7,.7,.7]},
        'thirdperson_lefthand':{'rotation':[-65,90,0],'translation':[0,4,0],'scale':[.7,.7,.7]}}}
    js(assets/'models/item/echo_maul_charging.json',{'parent':'relicward:item/echo_maul_held'})
    js(assets/'models/item/echo_maul_icon.json',{'parent':'relicward:item/echo_maul_held'})
    for name in ['court_altar','teaching_bell']:
        js(assets/f'models/item/{name}.json',{'parent':f'relicward:block/{name}'})
    messages={
        'item.relicward.echo_maul':('回响钟锤','Echo Maul'),
        'tooltip.relicward.maul':('主手长按右键 0.9 秒自动捶地；提前松开取消。冷却 10 秒。','Hold main-hand use for 0.9s to slam automatically; release early to cancel. Cooldown: 10s.'),
        'tooltip.relicward.pendant':('Curios 项链槽装备；3 秒后就绪。一次 ≥6 点战斗伤害减少 30%，冷却 25 秒。副手无效。','Equip in a Curios necklace slot. Ready after 3s: reduce a hit of at least 6 damage by 30%, 25s cooldown. No offhand effect.'),
        'tooltip.relicward.trophy':('首次击败纪念摆件；可在主战庭院外摆放，发光等级 6，无战斗属性加成。','First-victory trophy. Place outside the protected court; light level 6, no combat stat bonuses.'),
        'message.relicward.maul_charge':('钟锤蓄力 %s%%','Maul charge %s%%'),
        'message.relicward.maul_cancel':('已取消蓄力','Charge cancelled'),
        'message.relicward.maul_ground':('前方没有可捶击的地面','No ground within slam reach'),
        'message.relicward.maul_slam':('回响震击！','Echo slam!'),
    }
    for lang,index in [('zh_cn',0),('en_us',1)]:
        path=assets/f'lang/{lang}.json';old=json.loads(path.read_text(encoding='utf-8'));old.update({k:v[index] for k,v in messages.items()});js(path,old)
