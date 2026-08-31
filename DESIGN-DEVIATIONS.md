# Design deviations from the assignment PDF

The assignment's database tables, ERD, and prototypes disagree with each
other in places, and several required features have no database
representation anywhere in the source material. Per the agreed approach,
the implementation follows the **ERD** (page 3) where it conflicts with
the raw table listing (page 2), and adds the columns/entities needed to
satisfy the functional requirements and test table. Every deviation is
listed below with its source conflict or gap.

## Resolved conflicts between the raw tables and the ERD

- **Rating storage.** The raw `Recipe_Rating` table has five counter
  columns (`rate_1`..`rate_5`); the ERD's `Recipe_Rating` entity has a
  single `score : int`. Implemented as the ERD's single score, with one
  `Rating` row per `(recipe, user)` pair so scores can be correctly
  averaged (test case 9: "ratings average out"). See `Rating.java`.
- **Comments.** The raw table stores one `comments : varchar` column on
  the rating row (one comment per rating). The functional requirements
  need a full comment section — many comments per recipe, NSFW
  filtering, owner notification (test case 8). Implemented as a
  standalone `Comment` entity/table, decoupled from `Rating`.
- **`Recipe_category` / `Recipe_Category`.** Both the table and the ERD
  give this entity the *same primary key* as `Recipe` (1:1), so it is
  folded directly into the `Recipe` entity rather than kept as a
  separate table — splitting a 1:1-same-PK relation adds a join with no
  relational benefit. Field names follow the ERD (`veg`, `preference_tag`
  → renamed `dietaryTag`) over the raw table's misspellings (`Protin`).
- **`User_preference` list columns.** `Pref_recipe_id : varchar` and
  `Uploaded_recipe : varchar` (raw table) / `preferred_recipe_ids :
  list<int>`, `uploaded_recipe_ids : list<int>` (ERD) both store lists in
  a single column, which isn't queryable relationally. Replaced with:
  - `uploaded_recipe_ids` → derived from `Recipe.creator` (already a
    foreign key; storing it again on the user is redundant).
  - `preferred_recipe_ids` → a proper join entity, `SavedRecipe`
    (`user_id`, `recipe_id`), backing the Profile Page's "Saved Recipes"
    carousel.
- **Typos/casing normalized:** `Protin` → `protein`, `calory` → `calories`,
  `veg_noveg`/`veg_nonVeg` → `vegOnly`/diet-type discriminator (see
  below), `Int` → `int`/`Long` as appropriate.

## Entities added with no source table, ERD entry, or prototype

Required by the functional spec and/or the 13-case test table, but absent
from every design artifact in the PDF:

- **`Subscription`** — required by "subscription system that emails users
  when someone they follow uploads a new recipe" and test case 13.
  Models subscriber → creator as a many-to-many self-join on `User`.
- **`Warning`** — required by test cases 8 (NSFW comment → email warning,
  "warning is stored in the user database") and 11 (ban after 3
  warnings). `User.warningCount` is the running counter; each `Warning`
  row records why.
- **`SavedRecipe`** — see above; backs a prototype UI element (Profile
  Page "Saved Recipes") with no corresponding requirement text or DB
  design.

## Columns added to existing entities

Required by test cases, pseudocode, or prototypes, but missing from the
DB design entirely:

- `User`: `phone` (collected at sign-up per the Sign Up pseudocode),
  `profilePicturePath`, `bio` (both required by test case 1), `warningCount`,
  `accountStatus` (test case 11 — ban/suspend/delete), `twoFactorEnabled`
  (2FA is "optional" per the brief), `createdAt`.
- `Recipe`: `views`, `uploadDate` (both required for popularity sorting,
  test case 6; shown on the Recipe View prototype), `cost` (on the
  Recipe Upload prototype, no DB field), `speedRating`, `difficultyRating`
  (both on the Recipe Upload prototype and required as search filters,
  test case 4), `cuisineRegion`, `foodType` (both required filters, test
  case 4, absent everywhere else), `imagePath` (the raw table only has
  `recipe_video`, but the brief and prototypes require photo *and* video
  attachments).

## Diet modeling: class hierarchy + a separate tag field

The UML class diagram specifies exactly two `Recipe` subclasses —
`VegRecipe` and `NonVegRecipe` — and the assignment explicitly calls for
"OOP for recipe categorization (inheritance, classes, polymorphism)".
That two-way split is implemented as real Java inheritance
(`Recipe` abstract → `VegRecipe`, `NonVegRecipe`, JPA `SINGLE_TABLE`
inheritance with a discriminator column), with `getDietaryLabel()` and
`matchesDietPreference()` overridden per subclass and driving actual
filtering logic — not just present for decoration.

Test case 5 additionally requires finer dietary filtering ("eggetarian,
pescetarian, jain etc.") that the two-subclass UML model can't express as
separate classes without contradicting the diagram. This is handled with
a `DietaryTag` enum field on `Recipe` (`VEGAN`, `VEGETARIAN`,
`EGGETARIAN`, `PESCETARIAN`, `JAIN`, `NON_VEGETARIAN`) layered on top of,
not replacing, the class hierarchy.

## Not yet modeled (later phases)

- **Chat** — no table, flowchart, pseudocode, prototype, or test case
  exists anywhere in the source material. Deferred to its own phase.
- **NSFW filtering logic** — service-layer concern, covered when
  comments are built (Phase 5), not this domain-model phase.

## Phase 2: auth & accounts

- **Password reset requires an emailed token before a new password can
  be set.** The Login pseudocode's forgot-password branch explicitly
  says "SEND password reset mail... DISPLAY Reset Email Sent", but the
  Password Reset prototype screen shows only an identifier + New
  Password + Confirm New Password with no token/OTP field at all —
  taken literally, that would let anyone reset any account's password
  just by knowing their username, no verification (an account-takeover
  vulnerability). Implemented per the pseudocode instead: the emailed
  token/code is required to confirm a reset, and the prototype's "New
  Password" fields are read as the screen a user lands on *after*
  following the emailed link (the token travels as a parameter the
  static mockup doesn't depict), not as a same-screen bypass.
  `AuthService.requestPasswordReset()` also always returns the same
  "Reset Email Sent" response whether or not the account exists, to
  avoid leaking which identifiers are registered — a security addition
  beyond what the pseudocode specifies, not a contradiction of it.
- **2FA is a two-step API flow** (`POST /api/auth/login` → if the
  account has 2FA on, `TWO_FACTOR_REQUIRED` → `POST
  /api/auth/verify-2fa`) rather than one combined submission. The Login
  prototype shows identifier, password, and a 2FA OTP field together on
  one screen, but also notes "User is emailed a 6 digit code" — implying
  the code must already exist by the time it's entered, which requires
  a first step (password check) to have already run. The two-step API
  still supports a single visual screen: the frontend can reveal the
  OTP field in place once step one succeeds.
- **Login identifier email-format validation is conditional.** The
  pseudocode calls `validateEmailFormat(UserID)` unconditionally, but
  the same field accepts username/email/phone (per the Login
  prototype). Applied only when the identifier contains "@" (i.e. it's
  being used as an email), so a username or phone number login isn't
  wrongly rejected as an "Invalid Email ID".
- **"Restricted symbols"** (password rule) is undefined anywhere in the
  source material. Interpreted as: no whitespace/control characters,
  everything else allowed — see the javadoc on `Validators`.

## Phase 3: recipe CRUD + media

- **Veg/Non-Veg can't be changed on edit.** Phase 1 modeled diet as a
  real Java class hierarchy (`Recipe` abstract → `VegRecipe`,
  `NonVegRecipe`) using JPA single-table inheritance, per the UML
  diagram and the assignment's explicit "OOP for recipe categorization
  (inheritance, polymorphism)" requirement. That ties each database row
  to a fixed discriminator/Java class at creation time -- there's no
  in-place way to turn a `VegRecipe` row into a `NonVegRecipe` row
  without deleting and recreating it (which would orphan any ratings/
  comments already attached). `RecipeUpdateRequest` therefore has no
  `dietType` field; every other field, including the finer-grained
  `dietaryTag` (vegan/eggetarian/pescetarian/jain/etc.), remains
  editable. Not called for explicitly in the source material, which
  never depicts an edit flow in detail -- flagged here as a real
  consequence of the OOP design choice, not a silent gap.
- **No delete-recipe endpoint.** The Overall Structure Diagram lists
  "Edit recipe" but not delete, and no test case covers it. Left out
  to match the specified scope rather than added speculatively;
  straightforward to add later (would need to decide what happens to
  existing ratings/comments/saved-recipe rows referencing it).
- **Recipe.method stays a single text field** (as in the ERD), not a
  structured list of numbered steps, even though the Recipe Upload and
  Recipe View prototypes show steps 1-9 individually. Schema was
  already fixed in Phase 1; the frontend can format/number a
  newline-joined string without a schema change.
- **`Recipe.ingredients` and `Recipe.method` changed from `@Lob` to a
  plain long-text column.** Phase 4's keyword search needs `LOWER()`
  on these fields, and H2 can't run `LOWER()` on a CLOB without an
  explicit cast. Switching to a plain text column sidesteps that and
  is arguably closer to the ERD anyway (`ingredient : varchar`,
  `recipe_method : string`, not an explicit large-object type).

## Phase 4: search, filters & recommendation

- **`Recipe.requiredEquipment` added** (free text, e.g. "steamer,
  blender"), filtered by keyword match alongside the main search
  query. "Required utensils/equipment" is a named filter in the
  assignment's functional brief but has no column in any table, the
  ERD, the upload prototype, or a test case — flagged in the original
  requirements analysis (section N) as a gap, and closed here rather
  than left unimplemented, the same way the brief's other filters
  (cost, cooking time, calories, veg/non-veg, popularity) already had
  homes.
- **The Recipe Search pseudocode's "empty query = error" is not
  enforced.** Read literally, `GET /api/recipes` with no `query` would
  have to reject every filter-only browse (e.g. "just show me Veg
  recipes under 30 minutes"), but the Browse Page prototype's own UI
  (speed/difficulty sliders, a Veg/Non-Veg toggle) clearly supports
  browsing without typing a keyword, and Phase 3's plain recipe list
  already relied on query-less browsing. An absent/blank `query` is
  treated as "no keyword filter", not an error.
- **"Filter by ... Tags"** (pseudocode) is served by the existing
  `dietaryTag`, `cuisineRegion`, and `foodType` fields (Phase 1's fold
  of `Recipe_Category` into `Recipe`) rather than a separate freeform
  tag entity, since nothing in the source material defines a distinct
  tag model beyond those category-like fields.
- **Rating-based filtering/sorting (`minRating`, `sort=topRated`)
  happens in the service layer, not the database query.** Average
  rating was deliberately kept out of `Recipe` as a stored/denormalized
  column (Phase 1 followed the ERD's separate `Recipe_Rating` entity),
  so it can't be pushed into the same JPA Specification as the other
  filters; candidates are fetched via Specification first, then scored
  and (optionally) filtered/sorted in Java. Fine at this project's
  scale; would need a real aggregate query or a materialized rating
  column to stay efficient at a much larger scale.
- **"Recommended" (test case 13) is a simple boost, not a scored
  algorithm:** results from creators the current user follows are
  moved ahead of everyone else, preserving the chosen sort within each
  group. The assignment specifies no ranking formula or weighting
  (flagged as unspecified in the original requirements analysis,
  section H) — implemented as the smallest thing that visibly
  satisfies "the subscriber account is recommended more of their
  posts". Silently ignored for anonymous requests rather than erroring,
  since it's a soft personalization flag, not a gated action. Actually
  subscribing arrives in Phase 6; the query-side logic was written and
  tested now against a manually inserted `Subscription` row since the
  repository already existed from Phase 1.
- **Fixed a real (non-test-only) bug found while restarting the app
  during this phase's testing:** `PersistentTokenRepository`'s
  `setCreateTableOnStartup(true)` (Phase 2) issues an unconditional
  `CREATE TABLE persistent_logins` with no `IF NOT EXISTS` guard --
  every earlier verification run had wiped the H2 data directory
  before restarting, which hid that it would crash the app on any
  restart against a database that already had the table. Replaced with
  an idempotent `CREATE TABLE IF NOT EXISTS` run once via `JdbcTemplate`
  before the repository bean is constructed.

## Phase 5: ratings, comments, moderation & ban

- **Re-rating updates the existing score rather than erroring or
  duplicating.** The Rating pseudocode doesn't address a user rating
  the same recipe twice; the DB design already enforces one rating per
  `(recipe, user)` pair (Phase 1's unique constraint), so "rate again"
  reads most naturally as "change your rating" -- matches ordinary
  rating-widget UX and lets test case 9's averaging reflect a user's
  latest opinion rather than silently rejecting the second attempt.
- **NSFW filtering is a small keyword blocklist, not a real
  moderation model.** No word list, API, or algorithm is specified
  anywhere in the source material. `ModerationService.isSafe()` is
  written as a single seam so a real moderation service could replace
  it without touching `CommentService`.
- **The two pseudocode function names for the same check
  (`validateComment` in Commenting, `validateCommentSafe` in
  Subscription) are implemented as one method,** `ModerationService.
  isSafe()`, reused by both -- the two blocks describe the same
  content-safety check, not two different ones.
- **Blocked comments are never persisted** (matches the Commenting
  flowchart's "Invalid Comment" branch returning to input without an
  upload), but the resulting warning is: test case 8 requires the
  warning stored in the user database regardless of what happens to
  the comment text itself.
- **Ban is implemented as `accountStatus = BANNED` plus cascading
  deletion of content**, not literal row deletion of the `User`
  itself. Test case 11 says the account is "temporarily / permanently
  suspended / deleted" -- with no admin/unban flow specified anywhere
  (flagged as unspecified in the original requirements analysis),
  automatic banning after 3 warnings is treated as permanent for this
  system. Keeping the `User` row (rather than deleting it) avoids
  reopening the username/email for immediate reuse and sidesteps any
  FK cleanup the row's own presence doesn't require, while `Login`
  already rejects `BANNED` accounts (Phase 2) -- so the two features
  compose without new code. "Interaction history" is read as content
  the user generated (comments, ratings, recipes, saved recipes,
  subscriptions in both directions); `Warning` rows are deliberately
  kept as the audit trail for why the ban happened.
- **Found and fixed a transactional-boundary bug during this phase's
  testing, not a hypothetical one:** `CommentService.addComment()`
  calls `ModerationService.issueWarning()` and then throws
  `ApiException` to reject the comment. Both methods were
  `@Transactional` with default (REQUIRED) propagation, so
  `issueWarning()` ran inside the *same* transaction as `addComment()`
  -- and Spring rolls back the whole transaction on any unchecked
  exception escaping a `@Transactional` method. The result: throwing
  to reject the NSFW comment silently undid the warning and ban that
  were supposed to survive it. First-hand confirmed live (posted 3
  NSFW comments; the "banned" email logged, but the account, comment,
  and rating were all untouched) before being traced to the shared
  transaction and fixed with `@Transactional(propagation =
  REQUIRES_NEW)` on `issueWarning()`, then re-verified end-to-end
  against two scenarios: a user with no recipes of their own, and a
  user whose own recipe (with another user's comment and rating on
  it) had to be fully removed as part of the same ban.

## Phase 6: subscriptions & notifications

- **The Subscription pseudocode's `validateCommentSafe()` gate is not
  implemented.** A subscribe action has no text/comment input anywhere
  to validate -- the Subscription and Commenting pseudocode blocks
  look like they share a copy-pasted safety check that only makes
  sense for the latter (flagged as an ambiguity in the original
  requirements analysis, section N). Subscribing is unconditional
  (subject only to the not-yourself and user-exists checks below).
- **Two distinct notification paths, not one:** the pseudocode's own
  "NOTIFY Recipe Owner via Email" (on gaining a subscriber) is one
  email, sent from `SubscriptionService.subscribe()`. The separately
  and explicitly named key feature "subscription system that emails
  users when someone they follow uploads a new recipe" is the other
  -- implemented in `RecipeService.notifySubscribers()`, called once
  per `createRecipe()` (not `updateRecipe()`: "uploads a new recipe"
  describes the upload action, editing an existing one isn't a new
  upload). No source artifact ties these two together; they're kept
  separate since they notify different people about different events.
- **Subscribing is idempotent** (subscribing twice is a silent no-op,
  no duplicate row or repeat email) and **self-subscription is
  rejected** ("You can't subscribe to yourself") -- neither is
  specified, but both are ordinary follow-button behavior and match
  the SUBSCRIBE-button UI shown on the Profile Page and Recipe View
  prototypes without changing what test case 13 checks for.
- **Added a public profile endpoint** (`GET /api/users/{username}`)
  with follower/following/uploaded-recipe counts and the viewer's own
  subscribed state. Not itself a named requirement, but subscribing
  "to another account" (test case 13) has nowhere to happen without
  some way to view that account first, and the Profile Page prototype
  already shows Followers/Following counts with no endpoint that could
  produce them before this phase.
- **`SavedRecipe` (Phase 1's entity backing the Profile Page's "Saved
  Recipes" carousel) still has no save/unsave endpoint.** Out of scope
  for a phase about subscriptions specifically; noted here so it
  doesn't read as an oversight when the frontend phase reaches that
  carousel.
