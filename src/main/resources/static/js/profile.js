function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str == null ? "" : str;
  return div.innerHTML;
}

function starString(avg) {
  const rounded = Math.round(avg);
  return "★".repeat(rounded) + "☆".repeat(5 - rounded);
}

function recipeCard(r) {
  const dietClass = r.dietaryLabel === "Vegetarian" ? "veg" : "non-veg";
  const badgeText = r.dietaryLabel === "Vegetarian" ? "Veg" : "Non-Veg";
  const img = r.imagePath ? `/uploads/${r.imagePath}` : null;
  return `
    <a class="recipe-card ${dietClass}" href="recipe.html?id=${r.id}">
      <div class="recipe-card-media">
        ${img ? `<img src="${img}" alt="${escapeHtml(r.recipeName)}" loading="lazy">` : ""}
        <span class="diet-badge"><span class="diet-dot"></span>${badgeText}</span>
      </div>
      <div class="recipe-card-body">
        <h3>${escapeHtml(r.recipeName)}</h3>
        <p class="rating-line">${starString(r.averageRating)} ${r.ratingCount} rating${r.ratingCount === 1 ? "" : "s"}</p>
      </div>
    </a>
  `;
}

const params = new URLSearchParams(window.location.search);
const targetUsername = params.get("user");

async function toggleSubscribe(btn) {
  const subscribed = btn.dataset.subscribed === "true";
  btn.disabled = true;
  try {
    if (subscribed) {
      await Api.del(`/api/users/${encodeURIComponent(targetUsername)}/subscribe`);
    } else {
      await Api.post(`/api/users/${encodeURIComponent(targetUsername)}/subscribe`);
    }
    render();
  } catch (err) {
    alert(err.message);
    btn.disabled = false;
  }
}

async function saveProfile(e) {
  e.preventDefault();
  const alertBox = document.getElementById("edit-alert");
  alertBox.innerHTML = "";
  try {
    await Api.put("/api/profile", {
      firstName: document.getElementById("edit-first-name").value.trim() || null,
      lastName: document.getElementById("edit-last-name").value.trim() || null,
      age: document.getElementById("edit-age").value ? Number(document.getElementById("edit-age").value) : null,
      gender: document.getElementById("edit-gender").value.trim() || null,
      bio: document.getElementById("edit-bio").value.trim() || null,
      twoFactorEnabled: document.getElementById("edit-2fa").checked,
    });
    alertBox.innerHTML = `<div class="alert alert-success">Profile updated</div>`;
    render();
  } catch (err) {
    alertBox.innerHTML = `<div class="alert alert-error">${err.message}</div>`;
  }
}

async function uploadPicture(e) {
  const file = e.target.files[0];
  if (!file) return;
  const form = new FormData();
  form.append("file", file);
  try {
    await Api.postForm("/api/profile/picture", form);
    render();
  } catch (err) {
    alert(err.message);
  }
}

async function render() {
  const content = document.getElementById("content");
  let currentUser = null;
  try {
    currentUser = await Api.get("/api/profile/me");
  } catch (e) {
    // anonymous
  }

  const username = targetUsername || (currentUser ? currentUser.username : null);
  if (!username) {
    content.innerHTML = `<p class="empty-state">Log in to view your profile, or open someone's profile from a recipe page.</p>`;
    return;
  }

  let profile;
  try {
    profile = await Api.get(`/api/users/${encodeURIComponent(username)}`);
  } catch (err) {
    content.innerHTML = `<div class="alert alert-error">${err.message}</div>`;
    return;
  }

  const isOwnProfile = currentUser && currentUser.username === username;
  const avatarImg = profile.profilePicturePath
    ? `<img class="avatar" src="/uploads/${profile.profilePicturePath}" alt="">`
    : `<div class="avatar">${escapeHtml(username[0].toUpperCase())}</div>`;

  let actionHtml = "";
  if (isOwnProfile) {
    actionHtml = `<label class="btn btn-secondary btn-sm" for="picture-input">Change Picture</label>
      <input type="file" id="picture-input" accept="image/*" class="hidden" />`;
  } else if (currentUser) {
    actionHtml = `<button class="btn ${profile.subscribedByMe ? "btn-secondary" : "btn-primary"} btn-sm" id="subscribe-btn" data-subscribed="${profile.subscribedByMe}">
      ${profile.subscribedByMe ? "Subscribed" : "Subscribe"}
    </button>
    <a class="btn btn-secondary btn-sm" href="chat.html?with=${encodeURIComponent(username)}">Message</a>`;
  }

  const recipes = await Api.get(`/api/users/${encodeURIComponent(username)}/recipes`);

  content.innerHTML = `
    <div class="profile-header">
      ${avatarImg}
      <div>
        <h2 style="margin:0 0 4px;">${escapeHtml(profile.firstName || "")} ${escapeHtml(profile.lastName || "")}</h2>
        <p style="color:var(--text-muted);margin:0 0 8px;">@${escapeHtml(profile.username)}</p>
        ${profile.bio ? `<p style="max-width:480px;">${escapeHtml(profile.bio)}</p>` : ""}
        <div class="profile-stats">
          <div><strong>${profile.followerCount}</strong><span>Followers</span></div>
          <div><strong>${profile.followingCount}</strong><span>Following</span></div>
          <div><strong>${profile.uploadedRecipeCount}</strong><span>Recipes</span></div>
        </div>
      </div>
      <div style="margin-left:auto;">${actionHtml}</div>
    </div>

    ${isOwnProfile ? `
      <div class="explore-panel">
        <h3>Edit Profile</h3>
        <div id="edit-alert"></div>
        <form id="edit-form">
          <div class="filters-grid">
            <div><label>First Name</label><input class="input-pill" id="edit-first-name" value="${escapeHtml(profile.firstName || "")}"></div>
            <div><label>Last Name</label><input class="input-pill" id="edit-last-name" value="${escapeHtml(profile.lastName || "")}"></div>
            <div><label>Age</label><input class="input-pill" type="number" id="edit-age" min="0"></div>
            <div><label>Gender</label><input class="input-pill" id="edit-gender"></div>
          </div>
          <div class="form-group mt-16">
            <label>Bio</label>
            <textarea class="input-pill" id="edit-bio" rows="2">${escapeHtml(profile.bio || "")}</textarea>
          </div>
          <div class="checkbox-row">
            <input type="checkbox" id="edit-2fa" ${currentUser.twoFactorEnabled ? "checked" : ""}>
            <label for="edit-2fa" style="margin:0;">Enable two-factor authentication (email OTP on login)</label>
          </div>
          <button type="submit" class="btn btn-primary">Save Changes</button>
        </form>
      </div>
    ` : ""}

    <h3 class="section-title">&#127859; Uploaded Recipes</h3>
    ${recipes.length ? `<div class="recipe-grid">${recipes.map(recipeCard).join("")}</div>` : `<p class="empty-state"><span class="empty-icon">&#127860;</span>No recipes uploaded yet.</p>`}
  `;

  if (isOwnProfile) {
    document.getElementById("edit-form").addEventListener("submit", saveProfile);
    document.getElementById("picture-input").addEventListener("change", uploadPicture);
  } else if (currentUser) {
    document.getElementById("subscribe-btn").addEventListener("click", (e) => toggleSubscribe(e.target));
  }
}

render();
