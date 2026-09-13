"""Extract opaque axis-aligned JSON model surfaces without overlapping quads.

Later solids own shared exterior planes. UVs are cropped, never stretched.
Full cuboid depth is retained on face-only elements for vanilla face baking.
"""
from copy import deepcopy
from itertools import combinations

# normal axis/sign, texture U axis/sign, texture V axis/sign (vanilla BlockElement)
FACES = {
    'west': (0,-1,2,1,1,-1), 'east': (0,1,2,-1,1,-1),
    'down': (1,-1,0,1,2,-1), 'up': (1,1,0,1,2,1),
    'north': (2,-1,0,-1,1,-1), 'south': (2,1,0,1,1,-1),
}
EPS = 1e-7

def subtract(rect, cut):
    x0,y0,x1,y1=rect
    a,b,c,d=max(x0,cut[0]),max(y0,cut[1]),min(x1,cut[2]),min(y1,cut[3])
    if c-a<=EPS or d-b<=EPS:return [rect]
    return [r for r in [(x0,y0,a,y1),(c,y0,x1,y1),(a,y0,c,b),(a,d,c,y1)]
            if r[2]-r[0]>EPS and r[3]-r[1]>EPS]

def clean_model(model):
    result=deepcopy(model)
    solids=model['elements']
    output=[]
    for i,e in enumerate(solids):
        if 'rotation' in e:raise ValueError('Rotated solids require a different surface extractor')
        for face,attrs in e['faces'].items():
            if attrs.get('rotation',0):raise ValueError('Rotated UVs are not supported')
            axis,sign,u,us,v,vs=FACES[face]
            plane=e['to' if sign>0 else 'from'][axis]
            rect=[e['from'][u],e['from'][v],e['to'][u],e['to'][v]]
            fragments=[rect]
            for j,other in enumerate(solids):
                if i==j:continue
                lo,hi=other['from'][axis],other['to'][axis]
                covered=(lo<=plane+EPS and hi>plane+EPS) if sign>0 else (lo<plane-EPS and hi>=plane-EPS)
                shared=abs((hi if sign>0 else lo)-plane)<EPS and j>i
                if covered or shared:
                    cut=[other['from'][u],other['from'][v],other['to'][u],other['to'][v]]
                    fragments=[part for r in fragments for part in subtract(r,cut)]
            for r in fragments:
                part=deepcopy(e)
                part['name']=e.get('name',f'solid_{i:02}')
                part['from'][u],part['from'][v],part['to'][u],part['to'][v]=r
                uv=attrs['uv']
                def crop(a,b,low,high,begin,end,direction):
                    t0,t1=(a-low)/(high-low),(b-low)/(high-low)
                    if direction<0:t0,t1=1-t1,1-t0
                    return [round(begin+(end-begin)*t,8) for t in [t0,t1]]
                ux=crop(r[0],r[2],rect[0],rect[2],uv[0],uv[2],us)
                vy=crop(r[1],r[3],rect[1],rect[3],uv[1],uv[3],vs)
                part['faces']={face:{**attrs,'uv':[ux[0],vy[0],ux[1],vy[1]]}}
                output.append(part)
    result['elements']=output
    assert_no_overlaps(result)
    return result

def assert_no_overlaps(model):
    surfaces=[]
    for e in model['elements']:
        for face in e['faces']:
            axis,sign,u,_,v,_=FACES[face]
            surfaces.append((axis,e['to' if sign>0 else 'from'][axis],
                             [e['from'][u],e['from'][v],e['to'][u],e['to'][v]]))
    for a,b in combinations(surfaces,2):
        if a[0]!=b[0] or abs(a[1]-b[1])>EPS:continue
        r,s=a[2],b[2]
        assert min(r[2],s[2])-max(r[0],s[0])<=EPS or min(r[3],s[3])-max(r[1],s[1])<=EPS, (a,b)

if __name__=='__main__':
    # Adjoining cubes lose both internal faces; same-plane overlap has one owner.
    def cube(a,b):return {'from':a,'to':b,'faces':{f:{'texture':'#test','uv':[0,0,16,16]} for f in FACES}}
    adjacent=clean_model({'elements':[cube([0,0,0],[1,1,1]),cube([1,0,0],[2,1,1])]})
    assert len(adjacent['elements'])==10
    identical=clean_model({'elements':[cube([0,0,0],[1,1,1])]*2})
    assert len(identical['elements'])==6
    overlap=clean_model({'elements':[cube([0,0,0],[2,2,1]),cube([1,1,0],[3,3,1])]})
    area=sum((e['to'][0]-e['from'][0])*(e['to'][1]-e['from'][1]) for e in overlap['elements'] if 'north' in e['faces'])
    assert area==7
    assert any(e['faces'].get('north',{}).get('uv')==[8.0,0.0,16.0,16.0] for e in overlap['elements'])
    print('PASS: shared faces, identical solids, overlap area and cropped north UVs')
