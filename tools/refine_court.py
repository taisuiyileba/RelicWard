"""Court furnishing and a trophy derived from the actual editable boss geometry."""
import json,shutil
from model_surfaces import clean_model
from pathlib import Path

def generate_trophy(assets,js):
    root=Path(__file__).resolve().parents[1]
    bb=json.loads((root/'art/blockbench/bell_warden.bbmodel').read_text(encoding='utf-8'))
    def group(nodes,name):
        for n in nodes:
            if isinstance(n,dict):
                if n['name']==name:return n
                found=group(n.get('children',[]),name)
                if found:return found
    shutil.copyfile(assets/'textures/entity/bell_warden.png',assets/'textures/block/warden_trophy.png')
    ids=set(group(bb['outliner'],'head')['children'])
    elements=[]
    for e in bb['elements']:
        if e['uuid'] not in ids:continue
        def point(v):return [round(8+v[0]*.55,4),round(3+(v[1]-65)*.55,4),round(8+v[2]*.55,4)]
        faces={k:{'texture':'#warden','uv':[v*16/bb['resolution']['width'] for v in f['uv']]} for k,f in e['faces'].items()}
        elements.append({'from':point(e['from']),'to':point(e['to']),'faces':faces})
    for a,b,tex in [([1,0,1],[15,2,15],'#stone'),([2,2,2],[14,3,14],'#trim')]:
        elements.append({'from':a,'to':b,'faces':{k:{'texture':tex,'uv':[0,0,16,16]} for k in ['up','down','north','south','east','west']}})
    js(assets/'models/block/warden_trophy.json',{'parent':'minecraft:block/block','textures':{'warden':'relicward:block/warden_trophy','stone':'minecraft:block/polished_deepslate','trim':'relicward:block/maul_bronze','particle':'relicward:block/maul_bronze'},'elements':clean_model({'elements':elements})['elements']})
    js(assets/'blockstates/warden_trophy.json',{'variants':{f'facing={d}':{'model':'relicward:block/warden_trophy','y':r} for d,r in [('north',0),('east',90),('south',180),('west',270)]}})
    return ids

def generate(assets,js,blocks,put,fill):
    root=Path(__file__).resolve().parents[1]
    ids=generate_trophy(assets,js)
    stone='minecraft:stone_bricks';dark='minecraft:polished_deepslate';tile='minecraft:deepslate_tiles'
    slab='minecraft:stone_brick_slab';bronze='relicward:bronze_bricks'
    # Solid-topped offering tables and plinths: no gap beneath pots or candles.
    for (x,y,z),(name,props,nbt) in list(blocks.items()):
        if name.endswith('candle') or name=='minecraft:flower_pot':
            below=blocks.get((x,y-1,z))
            if below and below[0].endswith('_slab') and below[1].get('type')=='bottom':
                put(x,y-1,z,below[0],dict(below[1],type='top'))
    # Gallery inner walls: framed recessed bays and a continuous timber cornice.
    for x,facing in [(10,'east'),(54,'west')]:
        inward=1 if x==10 else -1
        for z in [11,20,30,38]:
            fill(x,6,z-1,x,9,z+1,bronze)
            put(x,8,z,'relicward:echo_lamp')
            for dz in [-2,2]:fill(x+inward,5,z+dz,x+inward,10,z+dz,dark)
            fill(x+inward,10,z-2,x+inward,10,z+2,'minecraft:chiseled_stone_bricks')
        fill(x,12,4,x,12,43,'minecraft:dark_oak_log',{'axis':'z'})
    # Shallow corbels below gallery eaves and bronze roof ridges.
    for x in [5,9,55,59]:
        for z in [7,17,29,40]:
            put(x,13,z,'minecraft:dark_oak_stairs',{'facing':'east' if x<32 else 'west','half':'top','shape':'straight','waterlogged':'false'})
    for x in [7,57]:
        for z in range(5,43,4):put(x,17,z,'minecraft:deepslate_tile_slab',{'type':'bottom','waterlogged':'false'})
    # Low corner shrines with rear panels; preserve the main circular combat lane.
    for x,z in [(16,8),(48,8),(16,40),(48,40)]:
        back=z-2 if z<24 else z+2
        fill(x-2,5,back,x+2,7,back,dark)
        fill(x-1,8,back,x+1,8,back,'minecraft:deepslate_tile_slab',{'type':'bottom','waterlogged':'false'})
        put(x,6,back,bronze);put(x,7,back,'relicward:echo_lamp')
    # Concentric, flush paving and cardinal markers define the combat circle.
    for x in range(13,52):
        for z in range(5,44):
            r=((x-32)**2+(z-24)**2)**.5
            if 15.6<r<16.4:put(x,4,z,'minecraft:polished_deepslate')
            if 16.4<=r<17 and (x+z)%3==0:put(x,4,z,bronze)
            if 5.7<r<6.3:put(x,4,z,'minecraft:chiseled_stone_bricks')
    # A proper entry portal between the teaching walk and main court.
    for x in [25,39]:
        fill(x,5,45,x,11,46,dark)
        fill(x-1,12,44,x+1,12,47,bronze)
    fill(24,13,44,40,13,47,tile);fill(26,14,45,38,14,46,tile)
    # Lattice windows in the side rooms, stone frames and warm lamp niches.
    for x0,x1 in [(3,24),(40,61)]:
        for z in [49,67]:
            for x in range(x0+3,x1-2):
                if (x-x0)%6 in [1,2,3]:
                    put(x,6,z,'minecraft:iron_bars',{'north':'false','south':'false','east':'true','west':'true','waterlogged':'false'})
                    put(x,7,z,'minecraft:iron_bars',{'north':'false','south':'false','east':'true','west':'true','waterlogged':'false'})
        for x in [x0+2,x1-2]:put(x,8,50,'relicward:echo_lamp')
    # Paved approach with planted islands, curb stones, and paired standing lamps.
    for x in [25,39]:
        for z in range(51,73):put(x,4,z,dark)
    for x in [19,45]:
        for z in [70,76]:
            put(x,5,z,'minecraft:chiseled_stone_bricks');put(x,6,z,'relicward:echo_lamp')
            put(x,7,z,'minecraft:deepslate_tile_slab',{'type':'bottom','waterlogged':'false'})
    for x in [8,16,48,56]:
        for dx,dz in [(-1,0),(1,0),(0,-1),(0,1)]:put(x+dx,5,72+dz,'minecraft:azalea_leaves',{'distance':'1','persistent':'true','waterlogged':'false'})
    # Build-time support regression: every candle and flower pot sits on a full top.
    for (x,y,z),(name,props,nbt) in blocks.items():
        if name.endswith('candle') or name=='minecraft:flower_pot':
            support=blocks.get((x,y-1,z))
            assert support and support[0]!='minecraft:air',(x,y,z,'unsupported decoration')
            assert not (support[0].endswith('_slab') and support[1].get('type')=='bottom'),(x,y,z,'floating decoration')
    print(f'Court refinement: trophy uses {len(ids)} original head cubes; all candles/pots supported.')
