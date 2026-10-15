# Theme progression — 0.1.35

## Earning and spending

- 200 XP per level; each level awards one banked spin.
- Keep and successful Android-confirmed Trash give equal 5 XP for each previously unrewarded real photo. Every tenth cumulative unique rewarded photo gives an extra 10 XP. Tutorial sample photos award no XP.
- Every spin independently awards one reward bundle. There is no Diamond guarantee, miss counter, or history-based change in odds.

| Reward | Chance |
| --- | --- |
| 1 Silver token | 50% |
| 3 Silver tokens | 20% |
| 1 Gold token | 20% |
| 2 Gold tokens | 5% |
| 1 Diamond token | 5% |

The combined chances remain Silver 70%, Gold 25%, Diamond 5%. Larger Silver and Gold bundles increase the tokens earned without changing those category chances. No money or ads buy spins.

- Three Silver convert to one Gold; three Gold convert to one Diamond. Each exchange requires an explicit tap.
- Tier 1 stays free. One Gold unlocks any chosen Tier 2 theme; one Diamond unlocks any chosen Tier 3 theme. Ownership persists. The tutorial Tier 2 reward remains free.
- Themes → level panel opens reels, balances, odds and exchanges. Theme cards show their price; purchase requires confirmation.
- Admin mode → Testing menu → Give 5 levels adds five levels and five spins while preserving fractional level XP. Turn off Preview all themes in that menu to test locked cards and purchases.
- Undo restores photo decisions while retaining earned XP. Re-review cannot earn again. There are no streaks, deadlines or expiring tokens.

## Reel presentation and persistence

Three native Canvas reels cycle through all five reward bundles, with quantity badges for 3 Silver and 2 Gold. The reels stop in sequence at 1.65 / 2.10 / 2.55 seconds and reveal the single banked bundle. Three matching reel symbols do not multiply the payout; the result explicitly states the awarded amount.

The reward is banked once before animation begins. Repeated Spin taps and exchanges are disabled during the animation. Closing retains the banked reward; pausing finishes quietly. Destroying the Activity disposes its animator and dialog. Reduced motion or disabled system animations skip scrolling. Existing audio controls govern the reel-stop sounds and result chime. No added artwork or animation dependencies are needed.

XP, spins, token balances, ownership, last reward and rewarded-photo IDs use device-local preferences. No cloud-sync guarantee is made. Existing balances, ownership, tutorial rewards and spins survive this update. The previous last-result token ID is mapped to the matching new reward ID; the old Diamond miss counter is ignored and removed on the next save.

The earlier one-time migration from 500-XP levels preserves earned levels and their theme unlocks, scales fractional progress to 200-XP levels, and credits previously earned level-up spins. Admin preview does not grant ownership.

Reset local progress clears wallet, last reward, ownership, XP and review history and disables Admin. It preserves the existing tutorial reward, media permissions and Trash recovery timers.

## Verification

ReviewLogicCheck covers exact five-outcome probabilities, combined category odds, bundle quantities, reward labels, last-result migration, XP boundaries, five-level grants, initial progression migration, milestones, Undo reward retention and reset safety. TokenReelCheck checks all five rewards against every previous reel position, staggered stops, exact reward alignment, forward travel and deceleration. Android lint and signed APK/AAB builds run in CI; appearance still needs a physical-phone check.
