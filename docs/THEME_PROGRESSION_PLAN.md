# Photo Sweep progression plan

Status: agreed XP/token rules for after the testing phase. Not enabled in 0.1.21.

## Locked decisions

- Earn 5 XP the first time a real photo is reviewed, equally for keeping or trashing.
- Earn 10 bonus XP after every 10 newly reviewed photos. Keeps and trashes share one cumulative counter; milestones continue at 10, 20, 30, and onward.
- Every 500 XP adds one level, starting at Level 1 with zero XP.
- Every level reached from Level 2 onward awards one Theme Token, with no cutoff at Level 10.
- Save tokens and spend them on themes of the user's choice. Tokens do not expire.
- Keep the separate one-time tutorial reward: a free Tier 2 theme of the user's choice.
- Existing earned themes remain unlocked. Preserve testers' progress during migration.
- Repeated reviews and undo/re-swipe loops must not create additional XP, bonuses, or tokens. Undo must be safe and must not pressure the user to retain an unwanted decision.
- Show brief, non-blocking reward feedback. No streak requirements, daily pressure, or expiring rewards.

## Decisions still to finalize before enabling

- Theme prices: proposed Tier 2 = 1 token, Tier 3 = 2 tokens.
- Tier 3 eligibility: proposed Level 6; confirm whether any level gate is needed in addition to token cost.
- Exact migration accounting for pre-existing XP and tier-wide unlocks; never revoke existing access.
- Final anti-repeat and undo bookkeeping, including reinstall/account restoration behavior.

## Current testing release

0.1.21 changes the theme browser, not XP or unlock economics:

- Compact collection menu and one availability filter (All / Unlocked / Locked).
- Separate country and holiday cards for each still/animated variant.
- Visible tier, motion type, required level, and remaining XP on every card.
- Theme preview/details before selection.
- Search and swipe/motion controls in the More menu; Settings at the far right.
- Tutorial uses the collection menu to choose Tier 2.

Until the agreed system is enabled, current test builds retain 500 XP per level and tier-wide level unlocks at Levels 1, 3, and 6.


## 2026-10-15 revision under discussion

The owner now wants easier, more enjoyable theme rewards and suggested a playful roll for Silver, Gold, and Diamond tier tokens, with Diamond rarest. The proposed direction is an earned level-up token reveal, followed by choosing which theme to unlock. The roll chooses token quality, not a random unwanted theme.

Design goals: preserve existing earned themes and XP; reward Keep and Trash equally; keep ten-photo milestones; allow lower tokens to contribute toward higher tokens; include a guaranteed path to rare tokens; show a short optional reveal without blocking photo review. Tier 1 colors currently available for free must remain available.

Still to decide with the owner: tier-to-token mapping, XP/roll pace, normal roll odds, guaranteed rare-token interval, conversion ratios, and migration grants. A faster 200-XP level (about 34 photos at the planned 5 XP plus ten-photo bonuses) was suggested, but the owner responded with the colored-token idea rather than selecting an XP pace. These numbers are not approved or active.

0.1.32 updates the particle Keep sleeve. It does not enable the proposed random token economy; the current testing XP and unlock rules remain active while these choices are discussed.

