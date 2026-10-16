# Material artwork revision 0.1.52

The three v2 PNG atlases in `app/src/main/res/drawable-nodpi` were generated with
the built-in image generation service in transparent-background generation mode.
They are production assets, not proposed screenshots. PhotoMaterialArt decodes
and isolates their subjects once; Keep snapshots share the resulting bitmaps.

Art direction: cinematic macro material photography, detailed optical surfaces,
bright specular lighting, no cartoon outlines. The finish sheet supplies fire,
iridescent water, fractured glacier ice, a glass vial with metal cap, a luminous
stellar burst, and cellophane candy. The debris sheet supplies flame, splashing
water, ice shards, viscous toxic ooze, an accretion disk and hard sugar fragments.
The additional candy sheet supplies wrapped candy, amber bonbon, candy corn and
spiral lollipop. Subject bounds are explicit where artwork crosses grid gutters.

Five review passes:

1. Inspect actual RGBA transparency and resolution; check shared decoded tiles.
2. Render real photo contact, frozen shard fractures, liquid highlights and fire.
3. Review finishing transformations; reduce evenly spaced flame origins, fade the
   sealed candy's underlying photo and animate ice UV highlights and edge glints.
4. Review at phone density: isolate candy subjects, remove the star ray crossing
   into the vial tile, fit the rolled photo inside the glass. The optical layer
   clips to surviving photo triangles, including on departing Keep snapshots.
5. Verify moving ice with a timed pixel comparison, fully removed-photo optics,
   snapshot art retention, persistent UV damage, lifecycle and 120 rapid Keeps.
   Run the release build, lint and packaged-art pixel verification in CI.

The host preview executes the production renderer. It is not an Android GPU or
phone performance test. Run MaterialArtCheck and MaterialLifecycleCheck using the
README compile command. The workflow runs these before converting the PNGs to
pixel-identical lossless WebP for Android packaging. No emulator is needed.
