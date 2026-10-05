# PhotoSweep holiday theme format

This collection extends the format established in MainActivity.java and
theme-art-prompts.json. Use this contract for later theme additions.

## Design contract

- Keep each saved theme ID stable. Append new IDs; never reorder existing themes.
- Five palette roles, in order: background, panel, positive/accent, negative, gold/highlight.
  The existing controls, Settings, Undo and feedback styling consume these roles.
- Portrait, full-bleed, polished painted game-environment artwork. Reserve about
  65% as a dark, quiet center for photos and text. Place recognizable scenery
  in the upper quarter and decorations in outer edges and lower corners.
  No baked-in controls, words, logos or year numerals.
- Tier 2 Still unlocks at level 3; Tier 3 Animated unlocks at level 6.
  One holiday card opens both variants. Holidays appear in All and Holidays,
  without duplicate entries in the tier tabs or Countries.
- Animated scenery stays behind the UI and mostly in side margins. Reduced
  motion stops background decoration, touch borders, swipe effects and idle bounce.
- A Tier 3 touch border fits the visible photo bounds and uses the existing
  bottom-center growth mask: bottom, corners, sides, then top center. It starts
  on touch, grows for 850 ms, cools for 650 ms after release, and is removed after
  the photo decision. Each holiday uses its own palette and native motif.
- Swipe style, speed and intensity remain per-theme preferences. A theme
  change or rotation retains review state, selected month and scroll position.
- Holiday choices remain available year-round; selecting one does not change
  automatically with the date.

## Included collection

All 11 nationwide recurring federal holidays from the OPM schedule, plus
Halloween as requested. Presidents' Day is the user-facing name for
Washington's Birthday; search accepts either. OPM source:
https://www.opm.gov/policy-data-oversight/pay-leave/federal-holidays/

| Holiday | Still scene | Animated motif | Saved IDs (Still / Animated) |
| --- | --- | --- | --- |
| Christmas | Snowy village lights | Drifting snowflakes | 62 / 63 |
| Halloween | Cozy pumpkin night | Bobbing pumpkin lights | 64 / 65 |
| Thanksgiving | Autumn harvest evening | Falling harvest leaves | 66 / 67 |
| July 4th | Waterfront fireworks | Occasional red, white and blue fireworks | 68 / 69 |
| New Year's Day | Midnight gold celebration | Gold stars/confetti and gentle fireworks | 70 / 71 |
| Martin Luther King Jr. Day | Peaceful bridge of light | Slow floating doves | 72 / 73 |
| Presidents' Day | Winter civic gardens | Sapphire and gold laurel glints | 74 / 75 |
| Memorial Day | Quiet remembrance garden | Slow drifting poppies | 76 / 77 |
| Juneteenth | Community celebration | Rising celebration stars | 78 / 79 |
| Labor Day | Golden summer town | Swaying summer blooms | 80 / 81 |
| Columbus Day | Twilight sailing horizon | Sailing emblems and ocean sway | 82 / 83 |
| Veterans Day | Lanterns of gratitude | Gentle laurel lights | 84 / 85 |

Inauguration Day is a regional, occasional federal employee holiday, rather
than one of the 11 nationwide annual holidays in this collection.

## Sources and validation

Backgrounds: built-in image generation. Every exact prompt is in
theme-art-prompts.json. Every final painting is under
app/src/main/res/drawable-nodpi/holiday_*.png.
Native border/swipe motifs: editable SVG sources under art/holiday-motifs,
rendered transparent PNGs under app/src/main/res/drawable-nodpi/fx_holiday_*.png.
The rendering script is tools/create_holiday_motifs.py.
holiday-theme-manifest.json records palettes, resources and IDs.

The initial base is commit c803389 (0.1.16). That base's MainActivity.java was
truncated mid-method; this change recovers only the missing ending from the
last successful 0.1.15 commit f2a3e82, preserving newer month-layout changes.
Compare current main before applying: another chat may continue development.

Run tools/check_holiday_assets.py and tests/HolidayThemesCheck.java in addition
to the existing review logic checks. A full Android build and on-device visual
review are still required before a Play release. Creating these source files
does not publish a release to testers or production.
