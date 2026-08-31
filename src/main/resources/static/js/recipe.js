function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str == null ? "" : str;
  return div.innerHTML;
}

function starString(avg) {
  const rounded = Math.round(avg);
  return "★".repeat(rounded) + "☆".repeat(5 - rounded);
}

function formatDate(iso) {
  return new Date(iso).toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });
}

function stepsHtml(method) {
  const lines = method.split(/\r?\n/).map((l) => l.trim()).filter((l) => l.length > 0);
  if (lines.length <= 1) {
    return `<p>${escapeHtml(method)}</p>`;
  }
  return `<ol class="steps-list">${lines.map((l) => `<li>${escapeHtml(l)}</li>`).join("")}</ol>`;
}

const params = new URLSearchParams(window.location.search);
const recipeId = params.get("id");
let currentUser = null;

async function loadCurrentUser() {
  try {
    currentUser = await Api.get("/api/profile/me");
  } catch (e) {
    currentUser = null;
  }
}

function ratingWidget(recipe) {
  if (!currentUser) {
    return `<p class="rating-line">${starString(recipe.averageRating)} ${recipe.ratingCount} rating(s), avg ${recipe.averageRating.toFixed(1)} -- <a href="login.html">log in to rate</a></p>`;
  }
  const my = recipe.myRating || 0;
  const stars = [1, 2, 3, 4, 5].map(
    (n) => `<span data-score="${n}">${n <= my ? "★" : "☆"}</span>`
  ).join("");
  return `
    <p class="rating-line">${starString(recipe.averageRating)} ${recipe.ratingCount} rating(s), avg ${recipe.averageRating.toFixed(1)}</p>
    <div class="stars interactive" id="rating-widget">${stars}</div>
    <p id="rating-status" style="font-size:0.8rem;color:var(--text-muted);"></p>
  `;
}

function attachRatingHandlers() {
  const widget = document.getElementById("rating-widget");
  if (!widget) return;
  widget.querySelectorAll("span").forEach((span) => {
    span.addEventListener("click", () => submitRating(Number(span.dataset.score)));
  });
}

async function submitRating(score) {
  try {
    // Use the rating endpoint's own response rather than re-fetching the
    // recipe: a full re-fetch (a) hits the same GET that increments the
    // view counter, spuriously inflating it on every rating, and (b) was
    // rebuilding the whole page including the comment compose box mid-type.
    const summary = await Api.post(`/api/recipes/${recipeId}/ratings`, { score });
    window.__recipe.averageRating = summary.averageRating;
    window.__recipe.ratingCount = summary.ratingCount;
    window.__recipe.myRating = summary.myRating;
    document.getElementById("rating-section").innerHTML = ratingWidget(window.__recipe);
    attachRatingHandlers();
    document.getElementById("rating-status").textContent = `You rated this ${score}/5.`;
    document.getElementById("rating-status").style.color = "var(--veg-text)";
  } catch (err) {
    const status = document.getElementById("rating-status");
    status.textContent = err.message;
    status.style.color = "var(--nonveg-text)";
  }
}

async function subscribeSection(recipe) {
  if (!currentUser || currentUser.username === recipe.creatorUsername) {
    return "";
  }
  try {
    const profile = await Api.get(`/api/users/${encodeURIComponent(recipe.creatorUsername)}`);
    const subscribed = profile.subscribedByMe;
    return `<button class="btn ${subscribed ? "btn-secondary" : "btn-primary"} btn-sm" id="subscribe-btn" data-subscribed="${subscribed}">
      ${subscribed ? "Subscribed" : "Subscribe"}
    </button> <span style="font-size:0.8rem;color:var(--text-muted);">${profile.followerCount} follower(s)</span>`;
  } catch (e) {
    return "";
  }
}

async function toggleSubscribe(btn, creatorUsername) {
  const subscribed = btn.dataset.subscribed === "true";
  try {
    if (subscribed) {
      await Api.del(`/api/users/${encodeURIComponent(creatorUsername)}/subscribe`);
    } else {
      await Api.post(`/api/users/${encodeURIComponent(creatorUsername)}/subscribe`);
    }
    renderRecipe(window.__recipe);
  } catch (err) {
    alert(err.message);
  }
}

function metaPills(r) {
  const pills = [];
  if (r.cookingTimeMinutes) pills.push(`⏱ ${r.cookingTimeMinutes} min`);
  if (r.calories) pills.push(`${r.calories} kcal`);
  if (r.protein) pills.push(`${r.protein}g protein`);
  if (r.cost != null) pills.push(`₹${r.cost}`);
  if (r.speedRating) pills.push(`Speed ${r.speedRating}/5`);
  if (r.difficultyRating) pills.push(`Difficulty ${r.difficultyRating}/5`);
  if (r.cuisineRegion) pills.push(escapeHtml(r.cuisineRegion));
  if (r.foodType) pills.push(escapeHtml(r.foodType));
  if (r.dietaryTag) pills.push(escapeHtml(r.dietaryTag));
  if (r.requiredEquipment) pills.push(`Needs: ${escapeHtml(r.requiredEquipment)}`);
  return pills.map((p) => `<span class="meta-pill">${p}</span>`).join("");
}

async function renderComments() {
  const list = document.getElementById("comments-list");
  const comments = await Api.get(`/api/recipes/${recipeId}/comments`);
  if (comments.length === 0) {
    list.innerHTML = `<p class="empty-state">No comments yet.</p>`;
    return;
  }
  list.innerHTML = comments.map((c) => `
    <div class="comment-item">
      <span class="comment-author">${escapeHtml(c.authorUsername)}</span>
      <span class="comment-time">${formatDate(c.createdAt)}</span>
      <p>${escapeHtml(c.text)}</p>
    </div>
  `).join("");
}

async function renderRecipe(recipe) {
  window.__recipe = recipe;
  const content = document.getElementById("content");
  const img = recipe.imagePath ? `<img src="/uploads/${recipe.imagePath}" alt="">` : "";
  const video = recipe.videoUrl ? `<video controls src="/uploads/${recipe.videoUrl}"></video>` : "";
  const editLink = currentUser && currentUser.username === recipe.creatorUsername
    ? `<a class="btn btn-secondary btn-sm" href="recipe-form.html?id=${recipe.id}">Edit recipe</a>` : "";
  const subscribeHtml = await subscribeSection(recipe);

  content.innerHTML = `
    <div class="recipe-detail">
      <div class="recipe-detail-header">
        <div>
          <h1 style="margin-bottom:4px;">${escapeHtml(recipe.recipeName)}</h1>
          <p style="color:var(--text-muted);margin:0;">By <a href="profile.html?user=${encodeURIComponent(recipe.creatorUsername)}">${escapeHtml(recipe.creatorUsername)}</a>
            &middot; ${formatDate(recipe.uploadDate)} &middot; ${recipe.views} view(s)</p>
        </div>
        <div style="display:flex;gap:8px;align-items:center;">${subscribeHtml}${editLink}</div>
      </div>

      <div class="recipe-media">${img}${video}</div>

      <div class="recipe-meta-row">${metaPills(recipe)}</div>

      <h3>Ingredients</h3>
      <p>${escapeHtml(recipe.ingredients)}</p>

      <h3>Method</h3>
      ${stepsHtml(recipe.method)}

      <h3>Rating</h3>
      <div id="rating-section">${ratingWidget(recipe)}</div>
    </div>

    <div class="recipe-detail">
      <h3>Comments</h3>
      <div id="comments-list"><p class="empty-state">Loading...</p></div>
      <div id="comment-form-area"></div>
    </div>
  `;

  if (currentUser) {
    attachRatingHandlers();
    const subBtn = document.getElementById("subscribe-btn");
    if (subBtn) {
      subBtn.addEventListener("click", () => toggleSubscribe(subBtn, recipe.creatorUsername));
    }
    document.getElementById("comment-form-area").innerHTML = `
      <div id="comment-alert"></div>
      <form id="comment-form">
        <textarea class="input-pill" id="comment-text" rows="2" placeholder="Add a comment..." required></textarea>
        <button type="submit" class="btn btn-primary btn-sm mt-16">Post Comment</button>
      </form>
    `;
    document.getElementById("comment-form").addEventListener("submit", async (e) => {
      e.preventDefault();
      const text = document.getElementById("comment-text").value.trim();
      const alertBox = document.getElementById("comment-alert");
      alertBox.innerHTML = "";
      try {
        await Api.post(`/api/recipes/${recipeId}/comments`, { text });
        document.getElementById("comment-text").value = "";
        renderComments();
      } catch (err) {
        alertBox.innerHTML = `<div class="alert alert-error">${err.message}</div>`;
      }
    });
  } else {
    document.getElementById("comment-form-area").innerHTML = `<p class="empty-state"><a href="login.html">Log in</a> to comment.</p>`;
  }

  renderComments();
}

async function loadRecipe() {
  try {
    const recipe = await Api.get(`/api/recipes/${recipeId}`);
    renderRecipe(recipe);
  } catch (err) {
    document.getElementById("content").innerHTML = `<div class="alert alert-error">${err.message}</div>`;
  }
}

(async () => {
  if (!recipeId) {
    document.getElementById("content").innerHTML = `<p class="empty-state">No recipe specified.</p>`;
    return;
  }
  await loadCurrentUser();
  loadRecipe();
})();
