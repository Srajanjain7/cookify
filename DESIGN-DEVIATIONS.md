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
- **NSFW filtering logic**, **email sending**, **auth/session handling**,
  **file upload validation** — service-layer concerns, not schema;
  covered by later phases (Phase 2+), not this domain-model phase.
