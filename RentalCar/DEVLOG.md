# Fake Uber – Dev log: challenges, explorations, takeaways

Raw notes for the report's "Challenges, explorations and takeaways" section and the time log.
Everything below really happened while building the app.
Format: **Problem → Cause → Fix → Takeaway**.

---

## Time log (approximate)

| Date | Work |
|---|---|
| 04/11/2026 | Read the brief, made the plan (Google Sheet), first UI/UX research |
| 05–06/11 | Sketch iterations v4 → v8 (14 screens), Figma version, accessibility notes |
| 07/11 | Final UI/UX report (essay, 27 sources), UX Design doc (user stories, use cases), market research for 5 cars + free photos, low-fi Layouts A & B |
| 07/11 22:20 | Created the Android Studio project (Empty Views Activity, Kotlin DSL, minSdk 24) |
| 07/11 22:25 | Stage 1: Gradle (viewBinding), model classes, in-memory repository, images |
| 07/11 22:30 | Stage 2: colours (light/dark), theme, 6 styles |
| 07/11 22:37 | Stage 3: Home screen + MainViewModel + dark-mode toggle |
| 08/11 00:00 | Stage 4: Rent screen, Intent + Parcelable, Activity Result API, validation, Photo Picker |
| 08/11 00:18 | Stage 5: favourites, search, sort, My rentals + cancel booking; manual test of all features passed |
| 08/11 00:49–01:09 | Stage 6: Espresso tests (10). First run: 9/10 passed in 8 min 5 s. After fixes #20–#22: **10/10 passed in 43 s** |

---

## Design / planning challenges

### 1. Colour contrast failed WCAG in the first design
- **Problem:** the orange buttons with white text, and the grey helper text, looked fine but failed contrast checks.
  - White on #F97316 ≈ 2.8:1. Grey helper text < 4.5:1.
- **Fix:**
  - Darker orange #EA580C with **dark** text on buttons (4.9:1).
  - Secondary text changed to #625D57 (5.8:1).
  - A separate #C2410C for small orange text.
  - Brighter green and red variants for dark mode.
- **Takeaway:** check contrast with numbers, not by eye. Dark mode needs its own status colours.

### 2. Toast cannot be positioned any more
- **Problem:** the sketch first showed the booking message at the top of the screen.
- **Cause:** `Toast.setGravity()` does nothing for text toasts since Android 11.
- **Fix:** Snackbar at the bottom.
  - On the Rent screen it is anchored above the Back/Save bar.
  - It uses a two-line format: bold title + detail.
- **Takeaway:** check platform behaviour changes before designing around a widget.

### 3. How to collect driver documents
- **Options:** text fields (ID number), the camera, storage permission + gallery, or the system Photo Picker.
- **Choice:** two tiles that open the **Photo Picker** (`PickVisualMedia`).
  - It needs no permissions; the user only shares the one photo they pick.
  - It still gives "required field" validation.
- **Exploration:** this was new to me. The brief only requires "required fields", and photos make the flow realistic.

### 4. Choosing realistic, free car data and images
- **Problem:** the brief asks for free images only, and believable data.
- **Prices:** researched Vietnamese self-drive rental prices (2026) for common rental models and converted them to credits (1 credit = 10,000 VND).
- **Images:** Wikimedia Commons, CC BY-SA 4.0, credited in IMAGE_CREDITS.txt.
- **Edge cases:** the prices were chosen so they happen naturally: Fortuner × 4 days = 440 > 400.

### 5. Design deviation: "Gearbox" spec card
- **Problem:** the sketch showed Seats / Gearbox / Fuel.
- **Cause:** every car in the data is automatic, so Gearbox shows the same value on every car and adds nothing.
- **Fix:** replaced it with **Type** (Sedan / SUV / MPV).

### 6. Figma usage limit
- **Problem:** the Figma Starter plan's API/plugin limit was reached mid-way.
- **Fix:** exported the low-fi layouts as SVG and imported them into Figma by drag-and-drop.

---

## Build challenges

### 7. Parcelable: plugin version vs writing it by hand
- **Problem:** `@Parcelize` needs the `kotlin-parcelize` plugin. Its version must match the Kotlin version bundled with the new AGP 9 built-in Kotlin.
- **Fix:** implemented `Parcelable` manually (`writeToParcel`, a private constructor that reads the Parcel, and `CREATOR`).
- **Takeaways:**
  - Fields must be read in **exactly the same order** they were written. Swap two Ints and the year silently becomes the kilometres.
  - A Parcelable inside a Parcelable (`Rental` contains `Car`) is written by calling the inner object's `writeToParcel`.

### 8. Big photos in `drawable/` waste memory
- **Problem:** 1024-px JPGs in `res/drawable` are treated as mdpi.
  - On a 420-dpi emulator Android scales them about 2.6×.
  - That is roughly 19 MB of RAM per photo.
- **Fix:** moved them to `drawable-nodpi/`.

### 9. Dark mode wipes the screen state
- **Problem:** `AppCompatDelegate.setDefaultNightMode()` **destroys and recreates** the Activity.
  - Balance, rentals, favourites and the current car would reset.
  - So would the chosen days and photos on Rent.
- **Fix:** all state lives in `MainViewModel` / `RentViewModel`, which survive recreation. The UI is redrawn with one `render()` function.
- **Takeaway:** "user action → ViewModel → render()" keeps the UI in sync and makes recreation harmless. It is still memory-only, as the brief requires.

### 10. SearchView fires again after recreation
- **Problem:** after toggling dark mode while a search was active, the card jumped back to the first car.
- **Cause:** SearchView restores its text and calls `onQueryTextChange` again; that call reset the index.
- **Fix:** `setQuery()` ignores text that hasn't changed.

### 11. Slider ↔ render() loop, and the `fromUser` flag
- **Problem:** `render()` sets the slider value, and the slider's change listener calls `render()`. That is a potential loop.
- **First fix:** only react when `fromUser == true`.
  - This later blocked Espresso, which sets the value from code.
- **Final fix:** react only when the value differs from the ViewModel.
- **Takeaway:** guard loops on the data, not on who made the change.

### 12. Material 3 default colours leaking through
- **Problem:** the slider's inactive track was lavender (Material's default).
- **Cause:** the theme didn't define `colorSecondaryContainer`.
- **Fix:** mapped all Material 3 colour roles to our palette in one theme. Slider, Switch, ProgressIndicator and Snackbar now pick the right colours in both modes.

### 13. Edge-to-edge: content under the status bar
- **Problem:** on scroll, the search bar slid under the clock and battery icons.
- **Cause:** system-bar insets were applied as padding with `clipToPadding="false"`.
- **Fix:** `clipToPadding="true"` on the home screen.

### 14. Loading the picked photos efficiently
- **Problem:** phone photos can be 4000 px wide; decoding them for a 56-dp tile wastes memory.
  - The tile was also reloaded on every slider move.
- **Fix:**
  - Read the image size first, then decode a sampled-down copy (`inSampleSize`, ~200 px).
  - Remember the shown Uri in the view's `tag` so it isn't reloaded.

### 15. Validation design: live vs on Save
- **Decision:**
  - Credit problems (over 400, not enough balance) show **live** while the slider moves: red bar, red balance, error box.
  - Missing photos show only **after Save**, as red "Required" tiles plus a Snackbar. This avoids shouting at the user before they've had a chance.
- **Order:** the 400 limit is checked first, then the balance.
- **Brief requirement:** Save returns null, so the screen stays open until the problem is fixed or Back is pressed.

### 16. Emulator has no photos to pick
- **Problem:** the Photo Picker opened an empty gallery.
- **Fix:** drag-and-drop image files onto the emulator window to add them.

### 24. Switching to light mode on the Rent screen made the screen flicker forever
- **Problem:** in dark mode, opening Rent and switching to light made the screen flash black/dark in an endless loop (found while taking report screenshots).
- **Cause:**
  - `setDefaultNightMode()` recreates **both** Activities: Rent on screen and Main in the back stack.
  - When an Activity is recreated, Android restores each view's saved state *after* `onCreate()`. That includes the Switch's old checked value, and our listener was already attached.
  - Main restored "dark = on", which switched the app back to dark. Rent then restored "dark = off", which switched it to light again. The two screens kept flipping each other.
- **Fix (two layers):**
  - `android:saveEnabled="false"` on the Switch, so it never restores a stale value.
  - The listener only acts when the new value differs from the mode currently shown.
- **Takeaway:** view state restoration happens after `onCreate()`. Any listener attached in `onCreate()` can fire with old values, so global side effects (like changing the night mode) need a guard.

---

## Testing challenges (Espresso)

### 17. `scrollTo()` doesn't work with NestedScrollView
- **Fix:** a custom `ViewAction` that finds the nearest NestedScrollView and scrolls the target into the upper part of the screen.

### 18. No Espresso action for Material Slider
- **Fix:** a custom `setSliderValue()` ViewAction (see #11 for why the listener had to change).

### 19. Testing the Photo Picker
- **Problem:** a test can't tap through the system gallery.
- **Fix:** `espresso-intents` stubs the picker Intent and instantly returns a picture from the app's own resources.

### 20. Every test took ~47 s
- **Problem:** 10 tests took 8 minutes.
- **Cause:** the stub matched **every** Intent leaving the app (`not(isInternal())`).
  - It also caught ActivityScenario's own helper Activity.
  - So each test waited for a 45-second timeout when closing.
- **Fix:** stub only the picker actions (`ACTION_PICK_IMAGES` + fallbacks).
- **Takeaway:** make test stubs as narrow as possible.

### 21. Snackbar covered the "Cancel booking" button (a real UX bug found by a test)
- **Problem:** `cancelBooking` test failed; the "booked" Snackbar was still showing.
- **Cause:**
  - The button sits at the very bottom of the page, exactly where the Snackbar appears.
  - Espresso's click landed on the Snackbar.
  - A real user would have the same problem for about 3 s.
- **Fix:**
  - **App:** 120 dp of extra bottom space, so the last button can scroll above the Snackbar.
  - **Test:** wait for the Snackbar to disappear first.

### 22. Snackbars queue
- **Problem:** a new Snackbar only appears after the previous one has slid away, so checking immediately can read the old one.
- **Fix:** `checkSnackbar()` re-checks every 100 ms for up to 5 s (a conditional wait instead of a fixed `sleep`).

### 23. Removing redundancy in tests (brief requirement)
- **Fix:** the **Robot pattern**.
  - All Espresso calls live in `FakeUberRobot`.
  - Tests read like user steps, e.g. `next(4); bookCurrentCar(days = 3); checkCredits(170)`.
  - A UI change means fixing one place.
- RatingBar is deliberately not tested, as the brief notes.

---

## Explorations (beyond the weekly labs)

- Activity Result API (`registerForActivityResult`), replacing the deprecated `startActivityForResult`.
- Photo Picker (`PickVisualMedia`): permission-less media access.
- ViewModel for surviving configuration changes.
- Material 3 colour roles + `values-night` for dark mode with a single theme.
- ViewBinding with `<include>` (re-usable stat chips, spec cards, document tiles).
- RecyclerView + Adapter/ViewHolder for the Favourites row.
- Accessibility:
  - 48 dp touch targets.
  - Content descriptions.
  - Headings.
  - A custom TalkBack action for long-press.
  - Live region for errors.
- `espresso-intents`, custom ViewActions, the Robot pattern.

## Takeaways (draft)

- Design decisions are easier to code when the sketch already names the widgets.
- Keep state out of Activities. Recreation (dark mode, rotation) is normal, not an edge case.
- Accessibility is cheapest when it is part of the colour palette and dimensions from day one.
- UI tests find real UX bugs (#21), not just code bugs.
- Narrow, specific test setup (stubs, waits) beats broad shortcuts.
