"""Native pixel art and architectural details for the chapter generator (no scaled vector icons)."""
from PIL import Image, ImageDraw
import random

def items(AS,js,png):
    palette={'.':(0,0,0,0),'d':(39,40,33,255),'b':(82,60,39,255),'c':(133,91,49,255),
        'g':(186,137,66,255),'h':(227,184,103,255),'w':(250,220,150,255),
        't':(40,79,65,255),'s':(63,111,87,255),'l':(104,148,113,255)}
    art={
        'bell_core':[
        '................','......dddd......','.....dghhgd.....','.....dcttcd.....','....dcghhgcd....',
        '....dghwwhgd....','...dcgwhhggcd...','...dghhghcgcd...','...dghgwhcgcd...',
        '..dcghgwhgcgcd..','..dghhggwhggcd..','..dhggcggggccd..','..dddddddddddd..',
        '.....dcgcd......','......ddd.......','................'],
        'bronze_fragment':[
        '................','................','.........ddd....','......dddhlgd...',
        '....ddhlgsscd...','...dhgltssscd...','..dhltsttsscd...','..dglshgctsbd...',
        '...dsshwgtbbd...','...dlsgctbbd....','....dsgcbbd.....','....dgcbbd......',
        '.....dbbd.......','......dd........','................','................'],
        'warden_pendant':[
        '......dddd......','.....dc..gd.....','....dc....gd....','....dg....hd....',
        '.....dg..hd.....','......dhhd......','......dccd......','.....dghhgd.....',
        '.....dhwwgd.....','....dghwhgcd....','....dghgwhcd....','...dghgwwggcd...',
        '...dhgggcgccd...','...dddddddddd...','......dgd.......','.......d........'],
        'echo_maul':[
        '........dddd....','.......dghhgd...','......dglssggd..','.....dglsttggcd.',
        '.....dhsttggccd.','......dgtggccd..','.......dggccd...', '......ddgddd....',
        '.....dghd.......','....dcgd........','...dcbd.........','..dcbd..........',
        '.dghd...........','.dgd............','..d.............','................']}
    for name,rows in art.items():
        assert len(rows)==16 and all(len(r)==16 for r in rows),(name,[len(r) for r in rows])
        im=Image.new('RGBA',(16,16));im.putdata([palette[p] for row in rows for p in row]);png(AS/f'textures/item/{name}.png',im)
        model={'parent':'minecraft:item/generated','textures':{'layer0':f'relicward:item/{name}'}}
        if name=='warden_pendant':
            model['display']={f'firstperson_{side}hand':{'rotation':[0,0,0],'translation':[0,-1,0],'scale':[.42,.42,.42]} for side in ['left','right']}
        js(AS/f'models/item/{name}.json',model)
    # Inventory stays a 16px sprite; held maul is a compact, genuinely three-dimensional tool.
    elements=[]
    def box(a,b,texture):elements.append({'from':a,'to':b,'faces':{face:{'texture':texture,'uv':[0,0,16,16]} for face in ['north','south','east','west','up','down']}})
    box([7,-5,7],[9,18,9],'#wood');box([6,0,6],[10,2,10],'#band');box([6,12,6],[10,14,10],'#band')
    box([1,16,5],[15,23,11],'#metal');box([0,16,4],[3,23,12],'#band');box([13,16,4],[16,23,12],'#band');box([6,15,4],[10,24,12],'#metal')
    for element in elements:
        x,y,z=element['from'];xx,yy,zz=element['to']
        element['from']=[z,y,16-xx];element['to']=[zz,yy,16-x]
    held={'textures':{'wood':'minecraft:block/stripped_dark_oak_log','band':'minecraft:block/cut_copper','metal':'relicward:block/bronze_bricks'},'elements':elements,
        'display':{'thirdperson_righthand':{'rotation':[0,-90,0],'translation':[0,2,0],'scale':[.7,.7,.7]},
            'thirdperson_lefthand':{'rotation':[0,90,0],'translation':[0,2,0],'scale':[.7,.7,.7]},
            'firstperson_righthand':{'rotation':[0,-75,15],'translation':[0,-1,0],'scale':[.42,.42,.42]},
            'firstperson_lefthand':{'rotation':[0,75,-15],'translation':[0,-1,0],'scale':[.42,.42,.42]},
            'ground':{'translation':[0,3,0],'scale':[.45,.45,.45]}}}
    js(AS/'models/item/echo_maul_held.json',held)
    js(AS/'models/item/echo_maul_icon.json',{'parent':'minecraft:item/generated','textures':{'layer0':'relicward:item/echo_maul'}})
    js(AS/'models/item/echo_maul.json',{'loader':'forge:separate_transforms','textures':{'particle':'relicward:block/bronze_bricks'},
        'base':{'parent':'relicward:item/echo_maul_held'},'perspectives':{k:{'parent':'relicward:item/echo_maul_icon'} for k in ['gui','fixed']}})
    tex=Image.new('RGBA',(128,128),(43,53,42,255));draw=ImageDraw.Draw(tex);rnd=random.Random(718)
    for x,y,w,h,color in [(0,0,32,16,(57,94,74)),(0,20,20,9,(151,111,55)),(32,0,38,13,(161,121,60)),
        (0,32,32,16,(109,84,48)),(40,18,40,13,(156,119,63)),(0,52,22,13,(57,94,74)),(24,52,22,13,(64,105,83)),
        (54,40,32,16,(74,56,35)),(64,56,24,10,(149,110,53)),(0,68,16,8,(64,72,53)),(20,68,20,11,(102,85,51))]:
        for yy in range(y,y+h):
            for xx in range(x,x+w):
                n=rnd.choice([-5,-3,0,0,3,5]);tex.putpixel((xx,yy),tuple(c+n for c in color)+(255,))
        draw.line((x,y,x+w-1,y),fill=tuple(min(c+24,255) for c in color));draw.line((x,y+h-1,x+w-1,y+h-1),fill=tuple(max(c-24,0) for c in color))
    draw.rectangle((120,120,127,123),fill=(237,198,110,255))
    draw.rectangle((8,40,15,46),fill=(48,49,36,255))
    draw.line((9,41,14,41),fill=(167,124,65,255))
    draw.line([(2,22),(7,22),(7,25),(5,25)],fill=(74,65,42,255),width=1)
    draw.rectangle((64,52,85,63),fill=(61,102,81,255))
    draw.rectangle((69,57,74,62),outline=(198,151,77,255),width=1)
    png(AS/'textures/entity/court_puppet.png',tex)
    sheet=Image.new('RGB',(640,200),(35,39,35));sd=ImageDraw.Draw(sheet)
    for i,name in enumerate(art):
        sprite=Image.open(AS/f'textures/item/{name}.png').resize((128,128),Image.Resampling.NEAREST)
        sheet.paste(sprite,(i*160+16,20),sprite);sd.text((i*160+12,165),name,fill=(218,216,193))
    png(AS.parents[4]/'art/preview/item_pixels_0.3.png',sheet)

def architecture(blocks,put,fill):
    stone='minecraft:stone_bricks';slab='minecraft:stone_brick_slab';tile='minecraft:deepslate_tile_slab';bronze='relicward:bronze_bricks'
    # Recessed bays, bronze reliefs and buttresses break up the featureless arena wall.
    for z in (11,20,30,38):
        for x in (10,54):
            fill(x,6,z-2,x,8,z+2,'minecraft:chiseled_stone_bricks')
            put(x,7,z,'minecraft:oxidized_cut_copper')
            for dz in (-3,3):fill(x,5,z+dz,x,11,z+dz,'minecraft:polished_deepslate')
    for x in (18,25,40,47):
        fill(x-2,6,3,x+2,9,3,'minecraft:chiseled_stone_bricks');put(x,8,3,'relicward:echo_lamp')
    # Furnished outer galleries: offerings, benches, display niches and hanging chains.
    for x in (5,59):
        for z in (12,23,34):
            fill(x,5,z-1,x,5,z+1,slab,{'type':'bottom','waterlogged':'false'})
            put(x,6,z,'minecraft:flower_pot')
    for x in (7,57):
        for z in (7,17,29,40):
            put(x,12,z+1,'minecraft:chain',{'axis':'y','waterlogged':'false'})
            put(x,13,z+1,'minecraft:chain',{'axis':'y','waterlogged':'false'})
            put(x,11,z+1,'minecraft:lantern',{'hanging':'true','waterlogged':'false'})
    # Four small shrine groups in the square corners, outside the circular fighting lanes.
    for x,z in ((16,8),(48,8),(16,40),(48,40)):
        fill(x-1,5,z-1,x+1,5,z+1,slab,{'type':'bottom','waterlogged':'false'})
        put(x,5,z,'minecraft:chiseled_stone_bricks');put(x,6,z,'minecraft:flower_pot')
        for dx in (-1,1):put(x+dx,6,z,'minecraft:light_gray_candle',{'candles':'2','lit':'true','waterlogged':'false'})
    # Floor mosaic radiates from the bell; all inset details stay flush with the arena floor.
    for x in range(13,52):
        for z in range(5,44):
            d=((x-32)**2+(z-24)**2)**.5
            if 3.5<d<16 and (abs(x-32)==abs(z-24) or (int(d)%4==0 and (x+z)%7==0)):
                put(x,4,z,'minecraft:chiseled_stone_bricks')
    # Workshop: workbenches, shelves, material piles and hanging tools.
    for x in (44,47,50,53,56):
        put(x,5,51,'minecraft:barrel',{'facing':'up','open':'false'})
        put(x,6,51,'minecraft:spruce_trapdoor',{'facing':'north','half':'bottom','open':'false','powered':'false','waterlogged':'false'})
    for x in (45,48,51,54):
        put(x,5,65,'minecraft:smithing_table');put(x,6,65,'minecraft:flower_pot')
    for x in (43,59):
        fill(x,5,55,x,7,60,'minecraft:bookshelf');fill(x,8,55,x,8,60,'minecraft:dark_oak_slab',{'type':'bottom','waterlogged':'false'})
    for x in (47,53):
        fill(x,5,56,x+1,5,57,'minecraft:cut_copper');put(x,6,56,'minecraft:oxidized_cut_copper')
    # West archive and offering hall, with collapsed stonework and fragmented bell pedestal.
    fill(5,5,51,5,7,64,'minecraft:bookshelf');fill(6,5,51,11,5,51,'minecraft:dark_oak_stairs',{'facing':'north','half':'bottom','shape':'straight','waterlogged':'false'})
    fill(7,5,58,10,5,59,'minecraft:dark_oak_slab',{'type':'top','waterlogged':'false'})
    put(8,6,58,'minecraft:lectern',{'facing':'south','powered':'false','has_book':'false'})
    for x,z in ((17,56),(19,57),(18,60),(21,63)):
        put(x,5,z,'minecraft:mossy_stone_brick_slab',{'type':'bottom','waterlogged':'false'})
        put(x,4,z,'minecraft:mossy_stone_bricks')
    # Correct previously unsupported pavilion lanterns, then add ceiling beams.
    for x0,x1 in ((3,24),(40,61)):
        for x in (x0+3,x1-3):
            for z in (52,64):
                put(x,10,z+1,'minecraft:air');put(x,11,z+1,'minecraft:lantern',{'hanging':'true','waterlogged':'false'})
        fill(x0+2,11,54,x1-2,11,54,'minecraft:dark_oak_log',{'axis':'x'})
        fill(x0+2,11,62,x1-2,11,62,'minecraft:dark_oak_log',{'axis':'x'})
    # Enclosed teaching passage has a roof rhythm instead of isolated empty gate frames.
    for z in range(48,72):
        if z%3==0:fill(27,12,z,37,12,z,'minecraft:dark_oak_log',{'axis':'x'})
        for x in (26,38):put(x,13,z,tile,{'type':'bottom','waterlogged':'false'})
    for z in (49,60,70):
        for x in (27,37):
            put(x,10,z-1,'minecraft:air');put(x+1,11,z,'minecraft:lantern',{'hanging':'true','waterlogged':'false'})
    # Entrance watch niches and garden islands provide intermediate silhouettes.
    for x in (8,16,48,56):
        fill(x-1,4,71,x+1,4,73,'minecraft:moss_block');put(x,5,72,'minecraft:azalea')
        put(x-2,5,72,'minecraft:cobblestone_wall',{'up':'true','north':'none','south':'none','east':'none','west':'none','waterlogged':'false'})
        put(x-2,6,72,'minecraft:lantern',{'hanging':'false','waterlogged':'false'})
    # Tower bells gain a crown, lip, clapper and damaged cornice.
    fill(30,28,2,35,29,5,'minecraft:cut_copper');fill(29,24,1,36,25,6,'minecraft:oxidized_cut_copper')
    fill(32,22,3,33,24,4,'minecraft:chain',{'axis':'y','waterlogged':'false'})
    put(32,21,3,'minecraft:cut_copper')
    for x,z in ((25,0),(26,1),(39,8),(40,9)):put(x,31,z,'minecraft:air')
    # Weathered margins, never cluttering the playable circle or pillar volume.
    rnd=random.Random(3321)
    for x in range(3,62):
        for z in range(3,78):
            if x in (3,61) or z==77:
                if rnd.random()<.18:put(x,5,z,'minecraft:moss_carpet')
