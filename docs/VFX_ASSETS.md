# Swipe VFX refresh

All active effect sprites were regenerated using the built-in image generation tool, including country motifs and candy physics sprites. The runtime loads 256px transparent sprites rather than full resolution country artwork. Backdrops retain their original art except the new Yin Yang garden.

Generated project assets live in `app/src/main/res/drawable-nodpi/`: `fx_*.png`, `candy_sprites.png`, and `theme_yin_yang.jpg`. Atlas extraction preserves generated alpha. Atlas source cells are evenly cropped with ImageMagick and resized for runtime use; no hand-painted replacements or procedural substitutes were used for raster assets.

## Generation prompts

- Primary 4x4 atlas: transparent hand-painted luminous nebula, water curl, spirit orb, descending silk spider, gold glint, thunder, gamma flare, shield ring, scarlet petal, stealth spark, solar burst, wrapped candy, toxic droplet, shooting star, flame, ice crystal. Isolated equal cells, no text or background.
- Country/extra 4x4 atlas: earth leaf, lightning, Japanese lantern, Mexican marigold, American red/white/blue star, Spanish fan, Brazilian palm, French lavender, Italian olive, Korean lantern, white yin wisp, charcoal yang wisp, bubble, sparkle, cyan ring, yin-yang medallion. True transparent alpha, distinct isolated cells, respectful motifs, no lettering.
- Candy 4x2 atlas: pink/blue wrapped sweet, rainbow spiral disk, lavender gumdrop, mint striped sweet, peach gummy heart, yellow wrapped sweet, blue/white marshmallow, pink/cream peppermint. Transparent isolated cells, glossy hand-painted style.
- Yin Yang backdrop: portrait ink-and-ivory mountain koi garden, black and white koi around the upper pond, bamboo at edges, dark open center/lower area for text, subtle silver ripples and restrained gold highlights, no UI or lettering.

## Motion behavior

`SwipeVfxView` handles mirrored directional ribbons, theme sprites, country flags, drag easing, action labels, and release bursts on a fixed stage above the departing photo. `SwipeMotion` owns tested direction/timing math. Keep and Trash have separate previews. Theme preferences persist independently. Tier 2 Yin Yang has still art and interactive swipe effects; ambient animation remains Tier 3.

Runtime validation should include portrait/landscape, rapid alternating swipes, cancelled drags, both preview buttons, Off, density/speed extremes, permission-cancelled Trash, and candy tilt. Build and pure logic checks do not replace device testing.
