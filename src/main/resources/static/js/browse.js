/* Explore Recipes page. Same GET /api/recipes query-param contract as
   before (query, dietType, dietaryTag, maxCookingTime, maxCalories,
   maxCost, minSpeed, minDifficulty, cuisineRegion, foodType,
   minRating, sort, recommended) -- only the controls that build those
   params got richer. Sliders sit at their max position by default,
   which is treated as "no limit" and simply omitted from the query,
   exactly like the old blank-input/"Any"-select default did. */

const filterState = {
  dietType: "",
  dietaryTag: "",
  recommended: false,
  minRating: null,
};

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str == null ? "" : str;
  return div.innerHTML;
}

function starString(avg) {
  const rounded = Math.round(avg);
  return "★".repeat(rounded) + "☆".repeat(5 - rounded);
}

function renderCard(r) {
  const dietClass = r.dietaryLabel === "Vegetarian" ? "veg" : "non-veg";
  const badgeText = r.dietaryLabel === "Vegetarian" ? "Veg" : "Non-Veg";
  const img = r.imagePath ? `/uploads/${r.imagePath}` : null;
  const metaChips = [];
  if (r.cookingTimeMinutes) metaChips.push(`&#9201; ${r.cookingTimeMinutes} min`);
  if (r.cost != null) metaChips.push(`&#8377;${r.cost}`);

  return `
    <a class="recipe-card ${dietClass}" href="recipe.html?id=${r.id}">
      <div class="recipe-card-media">
        ${img ? `<img src="${img}" alt="${escapeHtml(r.recipeName)}" loading="lazy">` : ""}
        <span class="diet-badge"><span class="diet-dot"></span>${badgeText}</span>
      </div>
      <div class="recipe-card-body">
        <h3>${escapeHtml(r.recipeName)}</h3>
        <p class="by-line">By ${escapeHtml(r.creatorUsername)}</p>
        ${metaChips.length ? `<div class="recipe-meta-chips"><span>${metaChips.join("</span><span>")}</span></div>` : ""}
        <p class="rating-line">${starString(r.averageRating)} ${r.ratingCount} rating${r.ratingCount === 1 ? "" : "s"} &middot; Avg ${r.averageRating.toFixed(1)}</p>
      </div>
    </a>
  `;
}

function renderSkeleton(count = 6) {
  const card = `
    <div class="skeleton-card">
      <div class="skeleton-block"></div>
      <div class="skeleton-block skeleton-line"></div>
      <div class="skeleton-block skeleton-line short"></div>
    </div>
  `;
  return `<div class="skeleton-grid">${card.repeat(count)}</div>`;
}

function sliderIsAtMax(id) {
  const el = document.getElementById(id);
  return Number(el.value) >= Number(el.max);
}

function buildQuery() {
  const params = new URLSearchParams();
  const query = document.getElementById("query").value.trim();
  if (query) params.set("query", query);
  if (filterState.dietType) params.set("dietType", filterState.dietType);
  if (filterState.dietaryTag) params.set("dietaryTag", filterState.dietaryTag);

  const minSpeed = document.getElementById("min-speed").value;
  if (minSpeed) params.set("minSpeed", minSpeed);
  const minDifficulty = document.getElementById("min-difficulty").value;
  if (minDifficulty) params.set("minDifficulty", minDifficulty);

  if (!sliderIsAtMax("max-cooking-time")) params.set("maxCookingTime", document.getElementById("max-cooking-time").value);
  if (!sliderIsAtMax("max-cost")) params.set("maxCost", document.getElementById("max-cost").value);
  if (!sliderIsAtMax("max-calories")) params.set("maxCalories", document.getElementById("max-calories").value);

  const cuisineRegion = document.getElementById("cuisine-region").value.trim();
  if (cuisineRegion) params.set("cuisineRegion", cuisineRegion);
  const foodType = document.getElementById("food-type").value.trim();
  if (foodType) params.set("foodType", foodType);
  if (filterState.minRating) params.set("minRating", filterState.minRating);
  const sort = document.getElementById("sort").value;
  if (sort) params.set("sort", sort);
  if (filterState.recommended) params.set("recommended", "true");

  return params.toString();
}

function sliderLabel(id, suffix) {
  const el = document.getElementById(id);
  const valueEl = document.getElementById(`${id}-value`);
  valueEl.textContent = sliderIsAtMax(id) ? "No limit" : `Up to ${el.value}${suffix}`;
}

function renderActiveFilters() {
  const box = document.getElementById("active-filters");
  const chips = [];

  if (filterState.dietType) {
    chips.push({ label: filterState.dietType === "VEG" ? "Veg" : "Non-Veg", clear: () => setDietType("") });
  }
  if (filterState.dietaryTag) {
    chips.push({ label: filterState.dietaryTag[0] + filterState.dietaryTag.slice(1).toLowerCase(), clear: () => setDietaryTag("") });
  }
  if (!sliderIsAtMax("max-cooking-time")) {
    chips.push({ label: `≤ ${document.getElementById("max-cooking-time").value} min`, clear: () => resetSlider("max-cooking-time") });
  }
  if (!sliderIsAtMax("max-cost")) {
    chips.push({ label: `≤ ₹${document.getElementById("max-cost").value}`, clear: () => resetSlider("max-cost") });
  }
  if (!sliderIsAtMax("max-calories")) {
    chips.push({ label: `≤ ${document.getElementById("max-calories").value} kcal`, clear: () => resetSlider("max-calories") });
  }
  if (filterState.minRating) {
    chips.push({ label: `${filterState.minRating}+ stars`, clear: () => setMinRating(null) });
  }
  const minSpeedVal = document.getElementById("min-speed").value;
  if (minSpeedVal) {
    chips.push({ label: `Speed ${minSpeedVal}+`, clear: () => { document.getElementById("min-speed").value = ""; loadResults(); } });
  }
  const minDifficultyVal = document.getElementById("min-difficulty").value;
  if (minDifficultyVal) {
    chips.push({ label: `Difficulty ${minDifficultyVal}+`, clear: () => { document.getElementById("min-difficulty").value = ""; loadResults(); } });
  }
  const foodTypeVal = document.getElementById("food-type").value.trim();
  if (foodTypeVal) {
    chips.push({ label: foodTypeVal, clear: () => { document.getElementById("food-type").value = ""; loadResults(); } });
  }
  const cuisineVal = document.getElementById("cuisine-region").value.trim();
  if (cuisineVal) {
    chips.push({ label: cuisineVal, clear: () => { document.getElementById("cuisine-region").value = ""; loadResults(); } });
  }

  if (chips.length === 0) {
    box.innerHTML = "";
    return;
  }

  box.innerHTML = chips.map((c, i) => `<span class="active-filter-chip" data-idx="${i}">${escapeHtml(c.label)} <button type="button" aria-label="Remove filter">&times;</button></span>`).join("");
  box.querySelectorAll(".active-filter-chip button").forEach((btn, i) => {
    btn.addEventListener("click", () => chips[i].clear());
  });
}

async function loadResults() {
  const results = document.getElementById("results");
  const countEl = document.getElementById("result-count");
  results.innerHTML = renderSkeleton();
  countEl.textContent = "";
  renderActiveFilters();

  try {
    const recipes = await Api.get("/api/recipes?" + buildQuery());
    countEl.textContent = `${recipes.length} recipe${recipes.length === 1 ? "" : "s"} found`;
    if (recipes.length === 0) {
      results.innerHTML = `<div class="empty-state"><span class="empty-icon">&#127860;</span>No recipes match those filters yet.<br><button type="button" class="btn btn-secondary btn-sm mt-16" onclick="document.getElementById('clear-filters-btn').click()">Clear filters</button></div>`;
      return;
    }
    results.innerHTML = `<div class="recipe-grid">${recipes.map(renderCard).join("")}</div>`;
  } catch (err) {
    results.innerHTML = `<div class="alert alert-error">${err.message}</div>`;
  }
}

function setDietType(value) {
  filterState.dietType = value;
  document.querySelectorAll("#diet-chip-row .chip").forEach((c) => c.classList.toggle("active", c.dataset.diet === value));
  loadResults();
}

function setDietaryTag(value) {
  filterState.dietaryTag = value;
  document.querySelectorAll("#dietary-tag-chip-row .chip").forEach((c) => c.classList.toggle("active", c.dataset.tag === value));
  loadResults();
}

function setMinRating(score) {
  filterState.minRating = filterState.minRating === score ? null : score;
  const stars = document.querySelectorAll("#min-rating-stars span");
  stars.forEach((s) => {
    const active = filterState.minRating && Number(s.dataset.score) <= filterState.minRating;
    s.innerHTML = active ? "&#9733;" : "&#9734;";
  });
  loadResults();
}

function resetSlider(id) {
  const el = document.getElementById(id);
  el.value = el.max;
  sliderLabel(id, id === "max-cooking-time" ? " min" : id === "max-cost" ? "" : " kcal");
  loadResults();
}

function clearAllFilters() {
  document.getElementById("query").value = "";
  setDietType("");
  setDietaryTag("");
  filterState.recommended = false;
  document.getElementById("recommended-chip").classList.remove("active");
  setMinRating(null);
  document.getElementById("min-speed").value = "";
  document.getElementById("min-difficulty").value = "";
  document.getElementById("food-type").value = "";
  document.getElementById("cuisine-region").value = "";
  document.getElementById("sort").value = "popular";
  ["max-cooking-time", "max-cost", "max-calories"].forEach((id) => {
    const el = document.getElementById(id);
    el.value = el.max;
  });
  sliderLabel("max-cooking-time", " min");
  sliderLabel("max-cost", "");
  sliderLabel("max-calories", " kcal");
  loadResults();
}

document.getElementById("search-form").addEventListener("submit", (e) => {
  e.preventDefault();
  loadResults();
});

document.querySelectorAll("#diet-chip-row .chip").forEach((chip) => {
  chip.addEventListener("click", () => setDietType(chip.dataset.diet));
});
document.querySelectorAll("#dietary-tag-chip-row .chip").forEach((chip) => {
  chip.addEventListener("click", () => setDietaryTag(chip.dataset.tag));
});
document.getElementById("recommended-chip").addEventListener("click", () => {
  filterState.recommended = !filterState.recommended;
  document.getElementById("recommended-chip").classList.toggle("active", filterState.recommended);
  loadResults();
});
document.querySelectorAll("#min-rating-stars span").forEach((star) => {
  star.addEventListener("click", () => setMinRating(Number(star.dataset.score)));
  star.addEventListener("keydown", (e) => {
    if (e.key === "Enter" || e.key === " ") { e.preventDefault(); setMinRating(Number(star.dataset.score)); }
  });
});

document.getElementById("max-cooking-time").addEventListener("input", () => sliderLabel("max-cooking-time", " min"));
document.getElementById("max-cost").addEventListener("input", () => sliderLabel("max-cost", ""));
document.getElementById("max-calories").addEventListener("input", () => sliderLabel("max-calories", " kcal"));
["max-cooking-time", "max-cost", "max-calories"].forEach((id) => {
  document.getElementById(id).addEventListener("change", loadResults);
});

["min-speed", "min-difficulty", "cuisine-region", "food-type", "sort"].forEach((id) => {
  document.getElementById(id).addEventListener("change", loadResults);
});

document.getElementById("clear-filters-btn").addEventListener("click", clearAllFilters);

document.getElementById("filter-toggle-btn").addEventListener("click", () => {
  const sidebar = document.getElementById("filter-sidebar");
  const expanded = sidebar.classList.toggle("filter-sidebar-open");
  document.getElementById("filter-toggle-btn").setAttribute("aria-expanded", String(expanded));
});

(async () => {
  sliderLabel("max-cooking-time", " min");
  sliderLabel("max-cost", "");
  sliderLabel("max-calories", " kcal");

  try {
    await Api.get("/api/profile/me");
    document.getElementById("recommended-section").classList.remove("hidden");
  } catch (e) {
    // anonymous -- recommended stays hidden
  }
  loadResults();
})();
