from PIL import Image,ImageDraw
import math,json

def generate(assets,data,js,png):
    # Original circular bronze motif, used as a translucent in-world decal.
    im=Image.new('RGBA',(128,128))
    for y in range(128):
        for x in range(128):
            r=math.hypot(x-63.5,y-63.5)
            a=int(180*math.exp(-((r-53)/1.1)**2)+38*math.exp(-((r-53)/4)**2)+160*math.exp(-((r-30)/.9)**2))
            im.putpixel((x,y),(255,255,255,min(255,a)))
    draw=ImageDraw.Draw(im)
    def pt(angle,r):return (64+math.sin(angle)*r,64+math.cos(angle)*r)
    for i in range(16):
        a=i*math.tau/16
        draw.line([pt(a-.045,35),pt(a-.045,46),pt(a+.06,46),pt(a+.06,40),pt(a+.015,40)],fill=(255,255,255,220),width=2)
        draw.line([pt(a,51),pt(a,57)],fill=(255,255,255,245),width=2)
    draw.line([(64,43),(84,64),(64,85),(44,64),(64,43)],fill=(255,255,255,180),width=2)
    draw.line([(57,56),(72,56),(72,71),(61,71),(61,63),(66,63)],fill=(255,255,255,220),width=2)
    png(assets/'textures/effect/resonance.png',im)
    elements=[]
    def box(a,b,texture):elements.append({'from':a,'to':b,'faces':{f:{'texture':texture} for f in ['north','south','east','west','up','down']}})
    box([1,0,1],[15,3,15],'#stone');box([3,3,3],[13,4,13],'#gold');box([4,4,4],[12,12,12],'#bronze')
    box([3,11,3],[13,14,13],'#gold');box([2,8,4],[4,13,11],'#gold');box([12,8,4],[14,13,11],'#gold')
    box([7,6,2],[9,12,4],'#gold');box([5,9,3],[7,10,4],'#light');box([9,9,3],[11,10,4],'#light')
    js(assets/'models/block/warden_trophy.json',{'textures':{'stone':'minecraft:block/polished_deepslate','bronze':'relicward:block/bronze_bricks','gold':'minecraft:block/cut_copper','light':'relicward:block/echo_lamp','particle':'relicward:block/bronze_bricks'},'elements':elements})
    js(assets/'blockstates/warden_trophy.json',{'variants':{'':{'model':'relicward:block/warden_trophy'}}})
    js(assets/'models/item/warden_trophy.json',{'parent':'relicward:block/warden_trophy'})
    js(data/'loot_tables/blocks/warden_trophy.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'relicward:warden_trophy'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    messages={
        'block.relicward.warden_trophy':('玄铎钟冠','Warden Crown Trophy'),
        'block.relicward.court_altar':('启战钟 · 重鸣台','Trial Bell / Resonance Altar'),
        'message.relicward.auto_rewards':('校律奖励已自动入包（%s 件）。','Trial rewards delivered to your inventory (%s items).'),
        'message.relicward.auto_full':('背包已满，余下奖励已保留；腾出位置后会自动补发。','Inventory full. Remaining rewards are saved and delivered automatically when space is available.'),
        'message.relicward.protected':('古庭钟律守护着这片战场，不能破坏或改建。','The court protects this battlefield from destruction or construction.'),
        'message.relicward.resummoning':('钟律正在重聚……','The warden is reforming...'),
        'message.relicward.victory':('钟止山息。奖励自动发放；玄铎将在重鸣冷却后归位。','The bell falls silent. Rewards are delivered automatically; the warden will return after the cooldown.'),
        'message.relicward.reward_claimed':('余响奖励已自动发放。','Echo rewards delivered automatically.'),
        'message.relicward.inventory_full':('余下奖励已保留，腾出背包位置后自动补发。','Remaining rewards will be delivered when inventory space is available.'),
    }
    for language,index in [('zh_cn',0),('en_us',1)]:
        path=assets/f'lang/{language}.json';text=json.loads(path.read_text(encoding='utf-8'));text.update({key:v[index] for key,v in messages.items()});js(path,text)
