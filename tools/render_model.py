"""Run with Blender --background --python tools/render_model.py.
Renders the exact generated cuboids and pixel atlas, not concept art.
"""
from pathlib import Path
import json
import math
import bpy
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT/'art/preview'
OUT.mkdir(parents=True,exist_ok=True)
spec=json.loads((ROOT/'art/model_spec.json').read_text())
bpy.ops.object.select_all(action='SELECT')
bpy.ops.object.delete(use_global=False)

atlas=bpy.data.images.load(str(ROOT/'art/blockbench/bell_warden.png'))
atlas.pack()
materials={}
for kind in spec['palette']:
    mat=bpy.data.materials.new('Warden_'+kind)
    mat.use_nodes=True
    bsdf=next(n for n in mat.node_tree.nodes if n.type=='BSDF_PRINCIPLED')
    bsdf.inputs['Roughness'].default_value=.78
    bsdf.inputs['Metallic'].default_value=.25 if kind not in ('glow','cloth') else 0
    tex=mat.node_tree.nodes.new('ShaderNodeTexImage')
    tex.image=atlas
    tex.interpolation='Closest'
    mat.node_tree.links.new(tex.outputs['Color'],bsdf.inputs['Base Color'])
    if kind=='glow':
        mat.node_tree.links.new(tex.outputs['Color'],bsdf.inputs['Emission Color'])
        bsdf.inputs['Emission Strength'].default_value=1.2
    materials[kind]=mat

def uvrect(c):
    u,v=c['uv']; w,h,d=c['size']
    return dict(west=[u,v+d,u+d,v+d+h],north=[u+d,v+d,u+d+w,v+d+h],
        east=[u+d+w,v+d,u+2*d+w,v+d+h],south=[u+2*d+w,v+d,u+2*d+2*w,v+d+h],
        up=[u+d,v,u+d+w,v+d],down=[u+d+w,v,u+d+2*w,v+d])

for c in spec['cubes']:
    x,y,z=c['world']; w,h,d=c['size']
    x0,x1=x/16,(x+w)/16
    y0,y1=-(z+d)/16,-z/16
    z0,z1=-(y+h)/16,-y/16
    verts=[(x0,y0,z0),(x1,y0,z0),(x1,y1,z0),(x0,y1,z0),
           (x0,y0,z1),(x1,y0,z1),(x1,y1,z1),(x0,y1,z1)]
    fs=[('west',(0,4,7,3)),('east',(1,2,6,5)),('north',(3,7,6,2)),
        ('south',(0,1,5,4)),('up',(4,5,6,7)),('down',(0,3,2,1))]
    mesh=bpy.data.meshes.new(c['name'])
    mesh.from_pydata(verts,[],[f[1] for f in fs])
    mesh.update()
    obj=bpy.data.objects.new(c['name'],mesh)
    bpy.context.collection.objects.link(obj)
    mesh.materials.append(materials[c['material']])
    uv=mesh.uv_layers.new(name='MinecraftBoxUV')
    rects=uvrect(c)
    for poly,(face,_) in zip(mesh.polygons,fs):
        a,b,cc,dd=rects[face]
        points=[(a/512,1-dd/512),(a/512,1-b/512),(cc/512,1-b/512),(cc/512,1-dd/512)]
        for li,point in zip(poly.loop_indices,points): uv.data[li].uv=point

def plainmat(name,color,metal=0):
    mat=bpy.data.materials.new(name); mat.use_nodes=True
    p=next(n for n in mat.node_tree.nodes if n.type=='BSDF_PRINCIPLED')
    p.inputs['Base Color'].default_value=(*color,1)
    p.inputs['Roughness'].default_value=.85
    p.inputs['Metallic'].default_value=metal
    return mat

ground=plainmat('Charcoal backdrop',(.025,.037,.038))
bpy.ops.mesh.primitive_plane_add(size=200,location=(0,0,-.20))
bpy.context.object.data.materials.append(ground)
bpy.ops.mesh.primitive_cylinder_add(vertices=64,radius=3,depth=.16,location=(0,0,-.09))
bpy.context.object.data.materials.append(plainmat('Display plinth',(.060,.083,.077)))

world=bpy.context.scene.world
world.use_nodes=True
background=next(n for n in world.node_tree.nodes if n.type=='BACKGROUND')
background.inputs[0].default_value=(.085,.11,.12,1)
background.inputs[1].default_value=.4

def aim(obj,target): obj.rotation_euler=(Vector(target)-obj.location).to_track_quat('-Z','Y').to_euler()
def area(name,loc,power,color,size):
    data=bpy.data.lights.new(name,'AREA'); data.energy=power; data.color=color; data.shape='DISK'; data.size=size
    obj=bpy.data.objects.new(name,data); bpy.context.collection.objects.link(obj); obj.location=loc; aim(obj,(0,0,2.5))
area('Warm key',(-5,7,10),1600,(1,.82,.58),7)
area('Soft frontal fill',(5,6,5),1000,(.66,.85,1),6)
area('Copper edge',(2,-5,8),1800,(.68,1,.87),5)

cam_data=bpy.data.cameras.new('Camera'); cam=bpy.data.objects.new('Camera',cam_data)
bpy.context.collection.objects.link(cam); bpy.context.scene.camera=cam
cam_data.type='ORTHO'; cam_data.ortho_scale=7.5
scene=bpy.context.scene
scene.render.engine='CYCLES'
scene.cycles.samples=32
scene.cycles.use_denoising=True
scene.render.resolution_x=1050
scene.render.resolution_y=1100
scene.render.resolution_percentage=100
scene.render.image_settings.file_format='PNG'
scene.view_settings.view_transform='AgX'
for name,loc in [('front',(8,15,8)),('rear',(-9,-15,7)),('straight',(0,16,4.8))]:
    cam.location=loc; aim(cam,(-.1,0,2.5))
    scene.render.filepath=str(OUT/f'{name}.png')
    bpy.ops.render.render(write_still=True)
bpy.ops.wm.save_as_mainfile(filepath=str(ROOT/'art/bell_warden_preview.blend'))
