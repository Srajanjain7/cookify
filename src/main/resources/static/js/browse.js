let dietFilter = "";
let recommended = false;

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
  return `
    <div class="recipe-card ${dietClass}">
      <div class="recipe-card-top">
        ${img ? `<img class="recipe-thumb" src="${img}" alt="">` : `<div class="recipe-thumb"></div>`}
        <span class="diet-badge"><span class="diet-dot"></span>${badgeText}</span>
      </div>
      <h3>${escapeHtml(r.recipeName)}</h3>
      <p class="by-line">By ${escapeHtml(r.creatorUsername)}</p>
      <p class="rating-line">${starString(r.averageRating)} ${r.ratingCount} rating${r.ratingCount === 1 ? "" : "s"} | Avg ${r.averageRating.toFixed(1)}</p>
      <a class="btn btn-primary btn-block btn-sm" href="recipe.html?id=${r.id}">Open recipe</a>
    </div>
  `;
}

function buildQuery() {
  const params = new URLSearchParams();
  const query = document.getElementById("query").value.trim();
  if (query) params.set("query", query);
  if (dietFilter) params.set("dietType", dietFilter);

  const minSpeed = document.getElementById("min-speed").value;
  if (minSpeed) params.set("minSpeed", minSpeed);
  const minDifficulty = document.getElementById("min-difficulty").value;
  if (minDifficulty) params.set("minDifficulty", minDifficulty);
  const maxCookingTime = document.getElementById("max-cooking-time").value;
  if (maxCookingTime) params.set("maxCookingTime", maxCookingTime);
  const cuisineRegion = document.getElementById("cuisine-region").value.trim();
  if (cuisineRegion) params.set("cuisineRegion", cuisineRegion);
  const foodType = document.getElementById("food-type").value.trim();
  if (foodType) params.set("foodType", foodType);
  const minRating = document.getElementById("min-rating").value;
  if (minRating) params.set("minRating", minRating);
  const sort = document.getElementById("sort").value;
  if (sort) params.set("sort", sort);
  if (recommended) params.set("recommended", "true");

  return params.toString();
}

async function loadResults() {
  const results = document.getElementById("results");
  results.innerHTML = `<p class="empty-state">Loading...</p>`;
  try {
    const recipes = await Api.get("/api/recipes?" + buildQuery());
    if (recipes.length === 0) {
      results.innerHTML = `<p class="empty-state">No recipes match those filters yet.</p>`;
      return;
    }
    results.innerHTML = `<div class="recipe-grid">${recipes.map(renderCard).join("")}</div>`;
  } catch (err) {
    results.innerHTML = `<div class="alert alert-error">${err.message}</div>`;
  }
}

document.getElementById("search-form").addEventListener("submit", (e) => {
  e.preventDefault();
  loadResults();
});

document.querySelectorAll(".chip[data-diet]").forEach((chip) => {
  chip.addEventListener("click", () => {
    document.querySelectorAll(".chip[data-diet]").forEach((c) => c.classList.remove("active"));
    chip.classList.add("active");
    dietFilter = chip.dataset.diet;
    loadResults();
  });
});
document.querySelector('.chip[data-diet=""]').classList.add("active");

document.getElementById("recommended-chip").addEventListener("click", () => {
  recommended = !recommended;
  document.getElementById("recommended-chip").classList.toggle("active", recommended);
  loadResults();
});

["min-speed", "min-difficulty", "max-cooking-time", "cuisine-region", "food-type", "min-rating", "sort"].forEach((id) => {
  document.getElementById(id).addEventListener("change", loadResults);
});

(async () => {
  try {
    await Api.get("/api/profile/me");
    document.getElementById("recommended-chip").classList.remove("hidden");
  } catch (e) {
    // anonymous -- recommended stays hidden
  }
  loadResults();
})();
