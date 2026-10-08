#!/usr/bin/env python3
"""Package pixel-identical lossless artwork; retain editable PNGs in git."""
import argparse
from concurrent.futures import ThreadPoolExecutor
import hashlib
import io
import json
from pathlib import Path
import zipfile
from PIL import Image


def fingerprint(image):
    rgba = image.convert('RGBA')
    return {'size': list(rgba.size), 'rgba_sha256': hashlib.sha256(rgba.tobytes()).hexdigest()}


def convert(path):
    original = path.read_bytes()
    with Image.open(io.BytesIO(original)) as image:
        if getattr(image, 'n_frames', 1) != 1:
            return None
        identity = fingerprint(image)
        encoded = io.BytesIO()
        image.convert('RGBA').save(encoded, format='WEBP', lossless=True, exact=True,
                                   quality=100, method=4, icc_profile=image.info.get('icc_profile', b''))
    data = encoded.getvalue()
    with Image.open(io.BytesIO(data)) as decoded:
        if fingerprint(decoded) != identity:
            raise ValueError(f'Pixel verification failed: {path}')
    output = path.with_suffix('.webp')
    if len(data) >= len(original) or output.exists():
        output = path
    else:
        output.write_bytes(data)
    return {'resource': output.parent.name + '/' + output.name,
            'original_resource': path.parent.name + '/' + path.name,
            'before_bytes': len(original), 'after_bytes': output.stat().st_size, **identity}


def optimize(resources, report_path):
    before = sum(p.stat().st_size for p in resources.rglob('*') if p.is_file())
    removed = []
    # These default-density copies are byte-identical to the active nodpi scenes.
    # Never remove different density artwork or files with different pixels.
    for name in ('holiday_independence.png', 'holiday_new_year.png'):
        default, nodpi = resources / 'drawable' / name, resources / 'drawable-nodpi' / name
        if default.exists() and nodpi.exists() and default.read_bytes() == nodpi.read_bytes():
            removed.append(str(default.relative_to(resources)))
            default.unlink()
    pngs = sorted(p for p in resources.glob('drawable*/*.png') if not p.name.endswith('.9.png'))
    with ThreadPoolExecutor(max_workers=4) as pool:
        images = [result for result in pool.map(convert, pngs) if result]
    for entry in images:
        if entry['resource'] != entry['original_resource']:
            (resources / entry['original_resource']).unlink()
    after = sum(p.stat().st_size for p in resources.rglob('*') if p.is_file())
    if after > before:
        raise ValueError('Optimization unexpectedly increased resource size')
    report = {'before_bytes': before, 'after_bytes': after, 'saved_bytes': before-after,
              'removed_exact_duplicates': removed, 'images': images}
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(json.dumps(report, indent=2) + '\n')
    print(f'Artwork verified: {len(images)} images; {before-after:,} resource bytes saved')


def verify_bundle(bundle, report_path):
    report = json.loads(report_path.read_text())
    with zipfile.ZipFile(bundle) as archive:
        if archive.testzip() is not None:
            raise ValueError('Bundle integrity failed')
        names = archive.namelist()
        for entry in report['images']:
            folder, filename = entry['resource'].split('/')
            candidates = [n for n in names if n.startswith('base/res/')
                          and n.rsplit('/', 1)[-1] == filename
                          and (n.split('/')[-2] == folder or n.split('/')[-2].startswith(folder + '-v'))]
            if len(candidates) != 1:
                raise ValueError(f'Missing or ambiguous artwork: {entry["resource"]}')
            with Image.open(io.BytesIO(archive.read(candidates[0]))) as image:
                if fingerprint(image) != {k: entry[k] for k in ('size', 'rgba_sha256')}:
                    raise ValueError(f'Packaged artwork changed: {entry["resource"]}')
        for removed in report['removed_exact_duplicates']:
            if 'base/res/' + removed in names:
                raise ValueError(f'Duplicate still packaged: {removed}')
        report['bundle_bytes'] = bundle.stat().st_size
        report['packaged_images_verified'] = len(report['images'])
    report_path.write_text(json.dumps(report, indent=2) + '\n')
    print(f'Packaged artwork verified: {len(report["images"])} images; bundle {bundle.stat().st_size:,} bytes')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--resources', type=Path, default=Path('app/src/main/res'))
    parser.add_argument('--report', type=Path, default=Path('build/asset-optimization.json'))
    parser.add_argument('--verify-bundle', type=Path)
    args = parser.parse_args()
    if args.verify_bundle:
        verify_bundle(args.verify_bundle, args.report)
    else:
        optimize(args.resources, args.report)
