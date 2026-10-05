"""Validate parallel catalogue arrays and Android resource connections."""
from pathlib import Path
import json
import re
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
source = (ROOT/'app/src/main/java/com/dominic/photosweep/MainActivity.java').read_text()
manifest = json.loads((ROOT/'holiday-theme-manifest.json').read_text())
expected = 62 + len(manifest['holidays']) * 2
arrays = {}
for name in ['THEME_NAMES','THEME_COLORS','THEME_BACKDROP_IDS','THEME_FX_IDS']:
    body = re.search(r'\b'+name+r' = \{(.*?)\n    };', source, re.S).group(1)
    if name == 'THEME_NAMES': entries = re.findall(r'"[^"\n]*"', body)
    elif name == 'THEME_COLORS': entries = re.findall(r'\{[^{}]*\}', body)
    else: entries = [x.strip() for x in body.split(',') if x.strip()]
    assert len(entries) == expected, (name,len(entries),expected)
    arrays[name] = entries
for holiday in manifest['holidays']:
    for theme in [holiday['still_id'],holiday['animated_id']]:
        assert arrays['THEME_BACKDROP_IDS'][theme] == 'R.drawable.holiday_'+holiday['key']
        assert arrays['THEME_FX_IDS'][theme] == 'R.drawable.fx_holiday_'+holiday['key']
    with Image.open(ROOT/holiday['background']) as img:
        assert img.height > img.width and img.width >= 720, (holiday['key'],img.size)
        img.verify()
    with Image.open(ROOT/holiday['sprite']) as img:
        assert img.mode == 'RGBA' and img.getextrema()[3][0] == 0, holiday['key']
        img.load()
for resource in set(re.findall(r'R.drawable.(\w+)',source)):
    assert list((ROOT/'app/src/main/res').glob('drawable*/'+resource+'.*')), resource
print('PASS: all 86 catalogue entries align; 12 portrait paintings, 12 transparent motifs, and every drawable reference exist.')
