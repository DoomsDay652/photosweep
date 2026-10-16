# Photo material visual checks

The Java files share this test folder; their package declarations place compiled
classes under the appropriate Android and app packages. They are excluded from APK sources.

These host Java2D adapters execute PhotoMaterialRenderer to inspect its photo geometry,
color, alpha and trajectories. They are not an Android emulator and do not validate
Android GPU rendering, device frame rate, input latency or the complete app UI.
Mesh colors are averaged per triangle by the adapter; Android interpolates them per pixel.

Run from the repository root with Java 17:

```
java com.sun.tools.javac.Main -d /tmp/photo-preview $(find tools/material-preview -name '*.java') app/src/main/java/com/dominic/photosweep/{SeasonalRenderer,SeasonalThemes,ThemeCatalog,PlayPolicy,SlasherMotion,SlasherRenderer,PhotoMaterialArt,PhotoMaterialMotion,PhotoMaterialRenderer,PhotoContact,PhotoInteraction,SwipeVfxView,SwipeTheme,SwipeMotion,HolidayThemes}.java tests/PhotoMaterialPolishCheck.java
java -cp /tmp/photo-preview com.dominic.photosweep.MaterialPreview /tmp/photo-preview-frames
```

The output sequences show contact, pulling away with accumulated damage, keeping the
damaged photo, then a pristine next photo. The checked-in tutorial image is the fixture.

Run `java -cp /tmp/photo-preview com.dominic.photosweep.MaterialLifecycleCheck` for
actual View lifecycle checks using the host UI adapters. The adapters implement only
the material path used by these checks; legacy layer compositing is not modeled.

`MaterialArtCheck` verifies transparent atlas decoding, all four candy tiles, shared
art snapshots, time-varying ice lighting, and damage masking under the new optics.

SlasherCheck verifies its staged release lifecycle. SlasherPreview renders the actual
photo/knife and hinged-door sequences without an emulator.

SeasonalCheck verifies release lifecycle, Halloween lifecycle and reduced motion. SeasonalPreview renders the actual Halloween code using the generated transparent art.

ToxicMeltCheck verifies contact-only hanging stock, photo-textured runoff, tilted pool intersection and zero saw shake. ToxicMeltPreview renders the real View through feeding, holding and pulling away.
