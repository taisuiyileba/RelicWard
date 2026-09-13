"""Preserve the existing armour silhouette; separate UV faces and add stepped metal shading."""
from pathlib import Path
from PIL import Image,ImageDraw
import re,math,json
ROOT=Path(__file__).resolve().parents[1];AS=ROOT/'src/main/resources/assets/relicward'

def ramp(base,t):
    t=round(t/7)*7
    return tuple(max(0,min(255,int(c+t))) for c in base)+(255,)

def face(im,x,y,w,h,base,kind='',back=False):
    d=ImageDraw.Draw(im)
    for yy in range(h):
        for xx in range(w):
            light=19-28*yy/max(1,h-1)-12*xx/max(1,w-1)
            im.putpixel((x+xx,y+yy),ramp(base,light))
    if w>=3 and h>=3:
        d.line((x,y,x+w-1,y),fill=ramp(base,38));d.line((x,y,x,y+h-1),fill=ramp(base,20))
        d.line((x,y+h-1,x+w-1,y+h-1),fill=ramp(base,-36));d.line((x+w-1,y,x+w-1,y+h-1),fill=ramp(base,-24))
    if w>=5 and h>=5:
        for dx,dy in [(1,1),(w-2,1),(1,h-2),(w-2,h-2)]:d.point((x+dx,y+dy),fill=(196,158,89,255))
        if back:
            for yy in range(y+2,y+h-1,3):d.line((x+1,yy,x+w-2,yy),fill=(29,46,42,255))
        elif 'bell' in kind or 'crest' in kind or 'inlay' in kind:
            cx=x+w//2;d.line((cx,y+2,cx,y+h-3),fill=(74,187,158,255));d.point((cx,y+2),fill=(182,240,205,255))

def box(im,u,v,w,h,dep,kind):
    gold=any(k in kind for k in ['brow','cap','roof','trim','collar','cuff','belt','knee_guard','crossbar'])
    base=(161,118,58) if gold else (51,91,77)
    if any(k in kind for k in ['back','spine','rear']):base=(46,68,61)
    if any(k in kind for k in ['back_rib','spine_rail','back_crossbar','back_lumbar']):base=(140,107,57)
    for x,y,ww,hh,back,shade in [(u+dep,v,w,dep,False,22),(u+dep+w,v,w,dep,False,-24),(u,v+dep,dep,h,False,-12),(u+dep,v+dep,w,h,False,0),(u+dep+w,v+dep,dep,h,False,-6),(u+2*dep+w,v+dep,w,h,True,-18)]:
        face(im,x,y,ww,hh,tuple(max(0,c+shade) for c in base),kind,back)

def main():
    p=ROOT/'src/main/java/cn/suiyi/relicward/client/BellArmorModel.java';s=p.read_text(encoding='utf-8')
    layers=[Image.new('RGBA',(128,128)) for _ in range(2)]
    for im in layers:
        for u,v,w,h,dep,name in [(0,0,8,8,8,'helmet'),(16,16,8,12,4,'body'),(40,16,4,12,4,'arm'),(0,16,4,12,4,'leg')]:box(im,u,v,w,h,dep,name)
    d=ImageDraw.Draw(layers[0]);d.rectangle((9,11,14,13),fill=(16,27,25,255));d.line((9,12,10,12),fill=(137,217,173));d.line((13,12,14,12),fill=(137,217,173));d.line((11,10,11,15),fill=(168,126,63))
    # Back of helmet is riveted nape plating; no visor, eyes or front emblem.
    d.rectangle((25,9,30,14),fill=(35,57,51));d.line((25,11,30,11),fill=(95,113,86));d.line((25,14,30,14),fill=(136,103,59))
    positions=[[0,34,0],[0,34,0]];records=[]
    boundary=s.index('public static LayerDefinition createLegsLayer')
    pattern=r'(\w+\.addOrReplaceChild\("([^"]+)",\s*CubeListBuilder\.create\(\)\.texOffs\()\d+,\s*\d+(\)(?:\.mirror\(\))?\.addBox\()([^)]*)(\))'
    def edit(m):
        idx=1 if m.start()>boundary else 0;parts=m.group(4).split(',');dims=[float(q.strip().rstrip('Ff')) for q in parts[:6]];w,h,dep=map(int,dims[3:6]);width=2*(w+dep);height=h+dep
        x,y,row=positions[idx]
        if x+width>128:x=0;y+=row+1;row=0
        assert y+height<=128,(m.group(2),y,height)
        box(layers[idx],x,y,w,h,dep,m.group(2));positions[idx]=[x+width+1,y,max(row,height)]
        records.append({'part':m.group(2),'layer':idx+1,'uv':[x,y],'size':[w,h,dep]})
        return m.group(1)+f'{x}, {y}'+m.group(3)+m.group(4)+m.group(5)
    s=re.sub(pattern,edit,s);s=s.replace('LayerDefinition.create(meshdefinition, 64, 64)','LayerDefinition.create(meshdefinition, 128, 128)');p.write_text(s,encoding='utf-8')
    assert len(records)>=35,len(records)
    for i,im in enumerate(layers):im.save(AS/f'textures/models/armor/bell_layer_{i+1}.png')
    (ROOT/'art/armor081_uv.json').write_text(json.dumps(records,indent=2),encoding='utf-8')
    # Existing silhouettes are retained; shade each connected color island independently.
    icons=[]
    for name in ['bell_helmet','bell_chestplate','bell_leggings','bell_boots','puppet_gear','binding_seal','resonant_plate','repair_paste']:
        im=Image.open(ROOT/f'art/source081/{name}.png').convert('RGBA');src=im.copy();seen=set()
        for y in range(im.height):
            for x in range(im.width):
                if (x,y) in seen or src.getpixel((x,y))[3]==0:continue
                color=src.getpixel((x,y));todo=[(x,y)];island=[];seen.add((x,y))
                while todo:
                    at=todo.pop();island.append(at)
                    for dx,dy in [(1,0),(-1,0),(0,1),(0,-1)]:
                        nxt=(at[0]+dx,at[1]+dy)
                        if 0<=nxt[0]<im.width and 0<=nxt[1]<im.height and nxt not in seen and src.getpixel(nxt)==color:seen.add(nxt);todo.append(nxt)
                x0=min(a for a,b in island);x1=max(a for a,b in island);y0=min(b for a,b in island);y1=max(b for a,b in island)
                for xx,yy in island:
                    light=22-33*(yy-y0)/max(1,y1-y0)-12*(xx-x0)/max(1,x1-x0)
                    if max(color[:3])<55:light*=.3
                    base=color[:3]
                    if name.startswith('bell_'):
                        if base[1]>base[0]*1.12:base=tuple(int(v*.8) for v in base)
                        elif min(base)>155:base=(223,180,97)
                    im.putpixel((xx,yy),ramp(base,light))
        if name in ['puppet_gear','resonant_plate']:
            im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
            if name=='puppet_gear':
                d.polygon([(5,1),(9,1),(9,3),(12,3),(12,5),(14,5),(14,9),(12,9),(12,12),(10,12),(10,14),(6,14),(6,12),(3,12),(3,10),(1,10),(1,6),(3,6),(3,3),(5,3)],fill=(92,65,32))
                d.polygon([(5,4),(10,4),(12,6),(12,10),(10,12),(5,12),(3,10),(3,6)],fill=(181,132,61))
                d.rectangle((6,6,9,9),fill=(0,0,0,0));d.line((5,5,10,5),fill=(238,194,109));d.line((5,6,5,9),fill=(207,168,90));d.line((6,10,10,10),fill=(80,59,33))
            else:
                d.polygon([(1,5),(10,1),(14,10),(14,12),(5,15),(1,12)],fill=(76,61,36))
                d.polygon([(2,5),(10,2),(13,10),(5,13),(2,11)],fill=(53,99,81))
                d.line([(2,5),(10,2),(13,10)],fill=(207,165,86));d.line([(5,13),(13,10)],fill=(125,92,48))
                d.line([(5,10),(5,7),(9,5),(10,8),(8,9)],fill=(148,122,68));d.point((4,6),fill=(234,189,110));d.point((11,10),fill=(205,162,82))
            for yy in range(16):
                for xx in range(16):
                    c=im.getpixel((xx,yy))
                    if c[3]:im.putpixel((xx,yy),ramp(c[:3],14-2*yy-.7*xx))
        if name=='repair_paste':
            d=ImageDraw.Draw(im);d.line((4,6,4,9),fill=(164,211,159));d.point((5,6),fill=(215,230,178));d.line((7,2,9,2),fill=(216,175,96))
        im.save(AS/f'textures/item/{name}.png');icons.append((name,im))
    lamp=Image.new('RGBA',(16,16));d=ImageDraw.Draw(lamp)
    for y in range(16):
        for x in range(16):
            edge=min(x,y,15-x,15-y)
            c=(155,112,53) if edge<3 else (91,152,101)
            light=28-3*y-1.1*x+(30 if edge>=3 and 6<=x<=9 else 0)
            lamp.putpixel((x,y),ramp(c,light))
    d.rectangle((0,0,15,15),outline=(52,46,31));d.rectangle((2,2,13,13),outline=(201,154,78));d.line((7,3,7,12),fill=(242,215,129));d.line((8,4,8,11),fill=(189,215,145));lamp.save(AS/'textures/block/chime_lantern.png')
    sheet=Image.new('RGB',(len(icons)*144,176),(28,34,32));sd=ImageDraw.Draw(sheet)
    for i,(name,im) in enumerate(icons):sheet.paste(im.resize((112,112),Image.Resampling.NEAREST),(i*144+16,12),im.resize((112,112),Image.Resampling.NEAREST));sd.text((i*144+3,143),name,fill='white')
    sheet.save(ROOT/'art/preview/materials081.png');print(f'PASS: {len(records)} custom armour UV islands; 128px atlases, 8 shaded icons, lantern.')
if __name__=='__main__':main()
