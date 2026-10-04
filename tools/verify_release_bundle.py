"""Verify embedded R8 mapping and report native-symbol coverage honestly."""
import sys
import zipfile
from pathlib import Path

bundle = Path(sys.argv[1])
with zipfile.ZipFile(bundle) as archive:
    names = archive.namelist()
    mapping = 'BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map'
    if mapping not in names or not archive.read(mapping).strip():
        raise SystemExit('Release bundle is missing its R8 mapping file.')
    libraries = [name for name in names if '/lib/' in name and name.endswith('.so')]
    symbols = [name for name in names
               if name.startswith('BUNDLE-METADATA/com.android.tools.build.debugsymbols/')]
    lines = ['R8 mapping: embedded in the AAB.',
             f'Native libraries: {len(libraries)}', f'Native symbol files: {len(symbols)}']
    missing = []
    for library in libraries:
        abi, filename = library.split('/')[-2:]
        covered = any(f'/{abi}/{filename}.' in name for name in symbols)
        lines.append(f'{abi}/{filename}: ' + ('symbols embedded' if covered else 'no symbols supplied'))
        if not covered:
            missing.append(library)
    if missing:
        lines.append('Some dependency libraries supply stripped binaries. Missing debug information '
                     'cannot be reconstructed; Google Play may retain a native-symbol warning.')
    report = Path('app/build/outputs/release-diagnostics.txt')
    report.parent.mkdir(parents=True, exist_ok=True)
    report.write_text('\n'.join(lines) + '\n')
    print(report.read_text())
