# Release history

## 1.1.0 — 2026-11-03

- Shorter focus animations while holding arrow keys on Home and Apps.
- Apps retains the latest requested position during row scrolling instead of ignoring arrow presses; faster row transitions.
- Home avoids restarting page and horizontal scroll animations when the destination has not changed.
- Continue playing cards enlarge to 1.1× on focus. Progress bars stay in place; episode details fade without sliding.
- Show season/episode numbers and estimated time remaining when Hotstar provides enough metadata.
- Reduce redundant thumbnail rebuilding and decoding; add an 8 MiB decoded poster cache.
- Reuse drawing geometry for focus borders and progress bars.

Validation: 325 unit tests, release lint and APK build passed. Navigation and held-key checks exercised on OnePlus Q1. Results on other TVs may differ.

## 1.0.0

First Ease TV release, based on the CleanYourTV launcher.

- New Ease TV identity and `com.ease.tv` package.
- Favourite apps shelf, separate Apps page, app menus and rearrangement.
- Hotstar-only Continue playing cards with cached images and progress.
- Clock/date styling and time-format controls.
- Original abstract 4K wallpapers, built-in image browsing, import removal and bounded in-memory previews.
- Remote focus and navigation refinements, plus the optional OnePlus Q1 Home-hold shortcut.

Verified on OnePlus Q1 with Android 11. Other TV models have not been physically validated.
