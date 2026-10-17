# Halloween and Summer integration

Approved Slasher v4 remains integrated at ID 86. The picker now groups the original Halloween still (64), animated Halloween (65) and Slasher (86) under one Halloween card. Summer is appended at ID 87, Tier 3. Original IDs, ownership and unlock costs remain intact. The category is named Holidays & seasons; seasonal families are independent of background profile IDs and can accept future variants.

Halloween: the jack-o-lantern draws in and folds the photo behind its lower carved teeth; a draped ghost carries kept photos upward and right. Summer: a jaw-articulated shark advances through three bite positions, retaining tooth-shaped UV holes and emitting fragments sampled from the photo. Surf curls in, bends and transports the kept print, then recedes. Generated props retain transparent margins. Summer has a brighter coastal background and subtle lower-edge surf.

Both new profiles use the same staged release capture / actual-card commit path as the approved Slasher. No saw damage or shake during holding; cancellation and reduced motion are handled by SwipeVfxView. Slasher still stabs only after release, preserves the release orientation, and uses black ink.

Validation: host Java compilation; SeasonalCheck; ThemeCatalogCheck; HolidayThemesCheck; SlasherCheck; MaterialLifecycleCheck; MaterialArtCheck; tools/check_holiday_assets.py (all 88 parallel catalog entries); git diff --check. The preview raster adapter was corrected to clip source rectangles like Android Canvas, so sampled photo fragments render at their real size.

SeasonalPreview renders the production renderer into two side-by-side Trash / Keep panels. These are Java2D host previews, not emulator/device runs. No Android APK/AAB build or remote push was performed for this integration. Exact built-in imagegen prompts and project asset paths are saved in seasonal-art-prompts.json.
