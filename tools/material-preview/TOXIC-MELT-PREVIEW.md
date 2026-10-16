# Toxic contact melt

Toxic and the other material themes no longer receive the saw-shake callback. The contacted UV stock develops bounded sag and visible corrosion, while narrow photo-textured viscous strands drain down the left sludge surface. Strands come only from touched material, taper along their fall and end in heavy beads / a small spread on the pool. Photo material stretches locally and breaks off on pull-away; missing UV stock stays missing. The next photo resets independently.

The Toxic wall is painted behind the material so it cannot obscure the hanging stock and streams. MainActivity converts the actual gyro-tilted background LiquidPlane into the swipe stage's coordinates and supplies it to the material renderer. The preview uses a fixed tilted pool plane to inspect the production View and material renderer at 2x density.

Validation: ToxicMeltCheck (contact-only droop, no untouched emission, streams reaching a tilted pool, zero saw shake); PhotoDamageRetentionCheck; five groups in PhotoMaterialPolishCheck; MaterialLifecycleCheck; SlasherCheck; catalog/resource alignment and git diff --check. MainActivity plane-coordinate wiring was inspected; no Android build or emulator run.

## Sludge curtain v3

The previous atlas edge has been replaced for Toxic with toxic_sludge_curtain_v1.png, a continuous glossy green liquid sheet. The collision outline is extracted from this new artwork. Its outer silhouette stays fixed while internal folds and heavy globs flow downward at slow viscous speeds. The curtain clips against the same tilted pool plane as the photo runoff, with a wet fold merging into the pool. Other theme art and collision profiles are retained.

Additional validation: the curtain paints a substantial surface, changes visually over time while its contact profile stays exactly fixed, and does not extend below the pool merge. Exact built-in imagegen prompt and asset path are in toxic-curtain-art.json. The v3 GIF/MP4 render the production View at 2x density, with contact, drooping, runoff, pull-away and further feeding. No Android build, emulator or remote push for this preview revision.

## Strand replay correction

Strand sources now remain stable for each live emission. Once a contacted UV separates from the sludge, its strand stays detached and drains from the original contact. Returning the photo near the wall cannot reconnect it or select older corrosion / missing material as a new emitter. Only recent direct contact with remaining stock can begin a new stream. Emitter history resets for a fresh photo. ToxicMeltCheck compares runoff pixels against a control that stays away, checks expired contact cannot replay, and confirms new-photo emission after reset. The regression fails against the previous renderer and passes with the fix; retention and View lifecycle checks also pass.
