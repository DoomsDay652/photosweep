# D.G.S. Radio — 0.1.4

Four original, locally synthesized tracks. No external recordings, sampled music,
streaming, permissions, ads or subscriptions are required for the radio.

| Track | Style | Duration |
| --- | --- | --- |
| Dreamy Sweep | Warm keys and dreamscape pads; original melody extended | 1:36 |
| Paper Lantern | Swinging lo-fi keys, bass and soft drums | 1:47 |
| Nebula Drift | Floating synth pads, bells and a gentle pulse | 2:00 |
| Midnight Polaroid | Mellow lo-fi groove and electric keys | 1:41 |

Each is a developed 32-bar arrangement. Circular note tails/delays preserve the
loop boundary. `tools/create_radio_music.py` regenerates the OGG assets and
`tools/radio_music_manifest.json` records duration, size and measured boundary.
Generation verifies finite samples, peak headroom, decoded duration and loop join.
The four compressed tracks total approximately 3.4 MB.

Options → Audio enables D.G.S. Radio, changes volume, skips to the next song or
loops the current song. The playlist cycles through all four tracks. Music stays
optional and uses the existing preference. One current and one next MediaPlayer
are prepared asynchronously; Android links them for continuous track changes.
The track and position persist when leaving the app. Audio focus pauses/ducks
music and the radio does not run in the background.

On an actual track start/change, a non-interactive bubble appears at the top-right
below the toolbar, holds for 3.8 seconds and fades over 2.4 seconds. It stays above
new menu roots without re-rendering the app; pause/destroy cancels its callbacks.

Phone checks: enable radio, skip several songs, let a song finish naturally, loop
a song over its boundary, change volume/theme/menu, rotate, leave and return,
test another audio app taking focus, and disable music. Check that swipes continue
under the bubble and that its animation never refreshes the photo page.
