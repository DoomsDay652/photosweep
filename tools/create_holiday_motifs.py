"""Render the small native holiday motifs used by borders and swipes.

Run with Python + cairosvg. Background paintings are built-in imagegen assets;
their production prompts are recorded separately in theme-art-prompts.json.
"""
from pathlib import Path
import math
import cairosvg

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'app/src/main/res/drawable-nodpi'
SOURCE = ROOT / 'art/holiday-motifs'
SOURCE.mkdir(parents=True, exist_ok=True)

def star(cx, cy, outer, inner, color):
    points = []
    for i in range(10):
        a = -math.pi / 2 + i * math.pi / 5
        r = outer if i % 2 == 0 else inner
        points.append(f'{cx+math.cos(a)*r:.2f},{cy+math.sin(a)*r:.2f}')
    return f'<polygon points="{" ".join(points)}" fill="{color}" stroke="#fff3d1" stroke-width="2"/>'

snow = '<g stroke="#d7f5ff" stroke-width="5" stroke-linecap="round">'
for angle in range(0,360,60):
    snow += f'<g transform="rotate({angle} 64 64)"><path d="M64 64V20M64 34L51 25M64 34L77 25M64 47L54 40M64 47L74 40"/></g>'
snow += '</g><circle cx="64" cy="64" r="7" fill="#fffdf1"/>'
leaf = '<path d="M63 112L58 84 27 91 35 74 15 57 40 56 34 34 54 44 63 15 74 44 95 32 90 57 114 56 94 75 104 89 70 84Z" fill="url(#amber)" stroke="#ffe0a1" stroke-width="3"/><path d="M63 106L64 40M64 76L37 61M64 67L89 51" stroke="#ba6035" stroke-width="3" fill="none"/>'
pumpkin = '<path d="M61 32Q52 13 70 15L72 35" fill="#75ae76"/><ellipse cx="44" cy="72" rx="28" ry="37" fill="#e78039"/><ellipse cx="83" cy="72" rx="28" ry="37" fill="#e78039"/><ellipse cx="64" cy="72" rx="25" ry="39" fill="url(#amber)" stroke="#ffd294" stroke-width="2"/><path d="M33 68L48 51 54 70ZM76 70L84 52 96 68ZM35 83L49 87 53 81 65 91 77 82 82 87 94 82 84 99 46 99Z" fill="#fff0a0"/>'
dove = '<path d="M55 76Q28 76 16 102L45 98Q31 68 24 27Q48 35 61 62Q64 32 88 15Q98 51 83 68Q101 61 106 75L118 78 106 84Q100 103 77 102Q64 97 55 76Z" fill="url(#ivory)" stroke="#c1dbe8" stroke-width="2"/><circle cx="103" cy="76" r="2" fill="#3a526b"/>'
laurel = '<path d="M43 111Q15 61 57 18M85 111Q112 61 72 18" fill="none" stroke="#dfc482" stroke-width="4"/>'
for side in [-1,1]:
    for j in range(5):
        x=64+side*(27-j*3); y=92-j*15
        laurel += f'<ellipse cx="{x}" cy="{y}" rx="7" ry="14" fill="url(#sage)" stroke="#f1deb0" stroke-width="1.5" transform="rotate({side*40} {x} {y})"/>'
poppy = '<g fill="#d8666d" stroke="#ffc5b8" stroke-width="2"><ellipse cx="47" cy="51" rx="25" ry="29" transform="rotate(-25 47 51)"/><ellipse cx="81" cy="49" rx="25" ry="29" transform="rotate(25 81 49)"/><ellipse cx="49" cy="82" rx="25" ry="27"/><ellipse cx="82" cy="80" rx="25" ry="27"/></g><circle cx="65" cy="66" r="17" fill="#39464e"/><circle cx="65" cy="66" r="8" fill="#f6d596"/>'
bloom = '<g fill="#f6e6bb" stroke="#fff4db" stroke-width="2">'
for angle in range(0,360,45):
    bloom += f'<ellipse cx="64" cy="38" rx="10" ry="23" transform="rotate({angle} 64 64)"/>'
bloom += '</g><circle cx="64" cy="64" r="18" fill="url(#amber)"/>'
sail = '<path d="M28 89H107L93 110H47Z" fill="#a5754b" stroke="#f5d6a7" stroke-width="3"/><path d="M65 22V91" stroke="#f4deae" stroke-width="4"/><path d="M58 27L21 82H58Z" fill="#ecdbb5" stroke="#fff2d9" stroke-width="2"/><path d="M72 29L100 81H72Z" fill="#b5d9e8" stroke="#fff2d9" stroke-width="2"/><path d="M16 114Q30 105 44 114T73 114T106 114" fill="none" stroke="#8bd2e7" stroke-width="4" stroke-linecap="round"/>'
motifs = {
    'christmas':snow, 'halloween':pumpkin, 'thanksgiving':leaf,
    'independence':star(43,69,28,12,'#e08b94')+star(83,44,24,10,'#91c7f3')+star(87,92,17,7,'#f4efe4'),
    'new_year':star(64,64,43,18,'url(#amber)')+'<path d="M18 29L28 20M101 19L111 28M20 103L29 113M100 112L111 102" stroke="#efdfc4" stroke-width="4"/>',
    'mlk':dove, 'presidents':laurel+star(64,65,22,9,'#94bff2'),
    'memorial':poppy, 'juneteenth':star(64,64,43,19,'#89bee7')+star(64,64,23,10,'#fff4db'),
    'labor':bloom, 'columbus':sail, 'veterans':laurel+'<path d="M49 100L41 117 64 111 87 117 79 100" fill="#a7cfcc" stroke="#f4e3b8" stroke-width="2"/>'
}
defs = '<defs><linearGradient id="amber" x2="0" y2="1"><stop stop-color="#fff0b5"/><stop offset="1" stop-color="#e69845"/></linearGradient><linearGradient id="ivory" x2="0" y2="1"><stop stop-color="#fffdf1"/><stop offset="1" stop-color="#b6d9e8"/></linearGradient><linearGradient id="sage" x2="0" y2="1"><stop stop-color="#e7e6ac"/><stop offset="1" stop-color="#7faa9b"/></linearGradient></defs>'
for key, content in motifs.items():
    svg = f'<svg xmlns="http://www.w3.org/2000/svg" width="128" height="128" viewBox="0 0 128 128">{defs}{content}</svg>'
    (SOURCE / f'{key}.svg').write_text(svg)
    cairosvg.svg2png(bytestring=svg.encode(),write_to=str(OUT/f'fx_holiday_{key}.png'),output_width=192,output_height=192)
print('Rendered 12 transparent holiday motifs; kept editable SVG sources.')
