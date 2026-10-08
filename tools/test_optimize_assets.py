import contextlib
import io
import json
from pathlib import Path
import tempfile
import unittest
import zipfile
from PIL import Image
from optimize_assets import fingerprint, optimize, verify_bundle


class AssetOptimizationCheck(unittest.TestCase):
    def test_transparent_pixels_and_sprite_dimensions_survive(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); resources = root / 'res'; folder = resources / 'drawable-nodpi'
            folder.mkdir(parents=True)
            image = Image.new('RGBA', (128, 64), (23, 45, 67, 0))
            image.putpixel((64, 32), (221, 88, 20, 179))
            image.save(folder / 'sprite.png')
            identity = fingerprint(image)
            with contextlib.redirect_stdout(io.StringIO()): optimize(resources, root / 'report.json')
            result = next(folder.iterdir())
            with Image.open(result) as decoded: self.assertEqual(identity, fingerprint(decoded))

    def test_only_identical_holiday_copy_is_removed_and_nine_patch_is_kept(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); resources = root / 'res'
            for folder in ('drawable', 'drawable-nodpi'): (resources / folder).mkdir(parents=True)
            red = Image.new('RGB', (32, 32), 'red'); blue = Image.new('RGB', (32, 32), 'blue')
            for folder in ('drawable', 'drawable-nodpi'): red.save(resources / folder / 'holiday_independence.png')
            red.save(resources / 'drawable' / 'holiday_new_year.png')
            blue.save(resources / 'drawable-nodpi' / 'holiday_new_year.png')
            nine = resources / 'drawable' / 'button.9.png'; red.save(nine); original = nine.read_bytes()
            report = root / 'report.json'
            with contextlib.redirect_stdout(io.StringIO()): optimize(resources, report)
            self.assertEqual(['drawable/holiday_independence.png'], json.loads(report.read_text())['removed_exact_duplicates'])
            self.assertEqual(original, nine.read_bytes())
            self.assertEqual(2, sum(1 for p in resources.rglob('*') if p.stem == 'holiday_new_year'))

    def test_bundle_guard_rejects_missing_or_changed_artwork(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); image = Image.new('RGB', (16, 16), 'red')
            report = root / 'report.json'; bundle = root / 'test.aab'
            report.write_text(json.dumps({'images': [{'resource': 'drawable-nodpi/test.webp', **fingerprint(image)}], 'removed_exact_duplicates': []}))
            with zipfile.ZipFile(bundle, 'w'): pass
            with self.assertRaises(ValueError): verify_bundle(bundle, report)
            wrong = io.BytesIO(); Image.new('RGB', (16, 16), 'blue').save(wrong, 'WEBP', lossless=True)
            with zipfile.ZipFile(bundle, 'w') as archive: archive.writestr('base/res/drawable-nodpi-v4/test.webp', wrong.getvalue())
            with self.assertRaises(ValueError): verify_bundle(bundle, report)


if __name__ == '__main__': unittest.main()
