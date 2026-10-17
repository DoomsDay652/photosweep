# Beach Day: overhead anatomy and living shoreline

Draft only; builds remain at 0.1.55 until reviewed.

- Replaced the three shark sprites with dorsal-view artwork: edge-on vertical caudal fins, thin dorsal ridges, paired pectoral/pelvic fins. Updated per-species crop registration and snout anchor.
- Continuous pursuit now varies horizontal/vertical circuit radii, lateral sweep, approach depth and lane drift on different frequencies. Heading still follows actual travel; body flex and tail propulsion follow the turn and speed.
- Overhead sand replaces the horizon painting. A reusable production background renderer washes textured surf upward across the lower half, then draws it back down. There is no independent decorative wave that disagrees with object physics.
- Twelve reusable sprites cycle through beach balls, towels, chairs, sandals, sunglasses and straw hats. They tumble downward, float at the swash, drift sideways and move with incoming water and backwash. Reduced motion freezes props and shoreline; elapsed-time steps are bounded on resume.
- Photo contact, persistent bite holes, release-only clamp/haul, and the accepted full-width Keep surf remain intact.

Preview: `BeachDayPreview` runs the actual production background and seasonal renderers through held Trash, release and the next Keep. It is a host-rendered preview, not a device recording.

Validation: BeachSharkCheck, BeachSwimCheck, BeachKeepCheck, BeachBackgroundCheck, SeasonalCheck, SlasherCheck, ToxicMeltCheck, MaterialLifecycleCheck, MaterialArtCheck, holiday asset validation, and git diff whitespace checks. No emulator used.

Art generation: builtin image_gen.imagegen; exact prompts, paths and anatomy references are recorded in `beach-shore-art-v8.json`.

## Immediate next-photo handoff

Keep and Trash in Summer, Halloween, Slasher and the six material themes now commit on release rather than at the end of the visual animation. The released effect moves into a host-owned layer behind the fresh card, preserving its bitmap, stage coordinates, captured rotation and damage. A new effect owns the next card immediately. Material Trash continues moving its own captured transform into the left receiver; material Keep keeps its entire independent flight lifetime.

The host retains at most eight outgoing effects and removes each effect and its cleanup callback on expiry, navigation away, theme change or Activity destruction. Android media-operation results still own the existing success/cancel/Undo accounting. No photo is marked successfully trashed merely because an animation started.

`RapidReleaseCheck` covers 18 outgoing Keep/Trash cases with 54 fresh-card bindings before those effects finish, reparenting survival, isolated bitmaps/damage, material left travel, Keep lifetime and final detach cleanup. It is part of the CI host-renderer checks. The existing v8 visual preview predates this handoff change.
