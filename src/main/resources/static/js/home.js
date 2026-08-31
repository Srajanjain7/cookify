/* Home page sections -- each is a real GET /api/recipes call with a
   different sort param (same endpoint browse.js uses), not mock data.
   "Trending" = sort=popular (views+date), "Top Rated" = sort=topRated,
   "Latest" = sort=newest, "Recommended For You" = recommended=true
   (only for a logged-in user, same as the Explore page's chip). */

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

async function loadSection(targetId, query, limit = 4) {
  const el = document.getElementById(targetId);
  el.innerHTML = `<div class="skeleton-grid">${'<div class="skeleton-card"><div class="skeleton-block"></div><div class="skeleton-block skeleton-line"></div></div>'.repeat(limit)}</div>`;
  try {
    const recipes = await Api.get(`/api/recipes?${query}`);
    if (recipes.length === 0) {
      el.innerHTML = `<p class="empty-state">Nothing here yet -- <a href="recipe-form.html">be the first to share a recipe</a>.</p>`;
      return;
    }
    el.innerHTML = `<div class="recipe-grid">${recipes.slice(0, limit).map(renderCard).join("")}</div>`;
  } catch (err) {
    el.innerHTML = `<div class="alert alert-error">${err.message}</div>`;
  }
}

document.getElementById("hero-search-form").addEventListener("submit", (e) => {
  e.preventDefault();
  const q = document.getElementById("hero-search-input").value.trim();
  window.location.href = q ? `browse.html?query=${encodeURIComponent(q)}` : "browse.html";
});

(async () => {
  loadSection("trending-home-results", "sort=popular");
  loadSection("top-rated-home-results", "sort=topRated");
  loadSection("latest-home-results", "sort=newest");

  try {
    await Api.get("/api/profile/me");
    document.getElementById("recommended-home-section").classList.remove("hidden");
    loadSection("recommended-home-results", "sort=popular&recommended=true");
  } catch (e) {
    // anonymous -- no personalized section
  }
})();
