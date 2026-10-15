# Theme progression — implemented in 0.1.33

- 200 XP per level; every level awards one banked spin.
- Keep and successful Android-confirmed Trash each give 5 XP for a previously unrewarded real photo. Every tenth cumulative unique rewarded photo gives another 10 XP. Tutorial sample photos give no XP.
- Spins award a token: Silver 70%, Gold 25%, Diamond 5%. After nine consecutive non-Diamond spins, the next spin guarantees Diamond. The guarantee resets on any Diamond. No money, ads or purchases buy spins.
- Three Silver convert to one Gold; three Gold convert to one Diamond. Conversions require an explicit tap.
- Tier 1 remains free. One Gold buys any chosen Tier 2 theme; one Diamond buys any chosen Tier 3 theme. Ownership persists. The tutorial Tier 2 choice remains free.
- Themes → level panel opens spins, balances, exchanges and odds. Cards show their token price before opening. Purchase requires confirmation.
- Admin mode → Testing menu → Give 5 levels adds exactly five levels and five spins, preserving fractional level XP. Spins produce the same token rewards as earned spins. Turn off Preview all themes inside this menu to test locked cards and purchases while retaining Admin tools.
- Undo restores photo decisions without removing earned XP or rewards. Re-review cannot award again. There are no streaks, deadlines or expiring tokens.

## Migration and persistence

A one-time migration scales old 500-XP levels to 200-XP levels while preserving the displayed level and fractional progress (rounded down). It permanently records every theme accessible through the old earned level. Admin preview does not grant ownership. Existing tutorial rewards remain valid. Previously earned level-ups receive spins. Device-local preferences atomically persist XP, spins, token balances, guarantee count, ownership and rewarded-photo IDs. No cloud-sync guarantee is made.

Reset local progress clears the wallet, guarantee count, ownership, XP and review history, and disables Admin. It preserves the existing tutorial reward, media permissions and Trash recovery timers.

## Verification

ReviewLogicCheck covers XP boundaries, five-level grants, migration level, milestone bonuses, exact 70/25/5 distribution, the ten-spin guarantee, Undo reward retention, token reset policy and photo occlusion geometry. Android lint and signed APK/AAB compilation run in CI. Visual behavior still requires checking on a physical phone.
