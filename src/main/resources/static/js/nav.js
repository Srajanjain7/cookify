/* Populates #nav-right with either Login/Sign Up links or the
   current user + Logout, based on whether a session is active.
   Included on every page. Same /api/profile/me + /api/auth/logout
   calls and #logout-btn id as before -- only the markup got richer. */

function navEscapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str == null ? "" : str;
  return div.innerHTML;
}

async function initNav() {
  const el = document.getElementById("nav-right");
  if (!el) return;

  try {
    const me = await Api.get("/api/profile/me");
    const avatar = me.profilePicturePath
      ? `<img class="nav-avatar" src="/uploads/${me.profilePicturePath}" alt="">`
      : `<span class="nav-avatar">${navEscapeHtml((me.username || "?")[0].toUpperCase())}</span>`;
    el.innerHTML = `
      <a href="recipe-form.html" class="btn btn-primary btn-sm">+ Upload Recipe</a>
      <span class="nav-user"><a href="profile.html?user=${encodeURIComponent(me.username)}">${avatar}${navEscapeHtml(me.username)}</a></span>
      <button class="logout-link" id="logout-btn">Logout</button>
    `;
    document.getElementById("logout-btn").addEventListener("click", async () => {
      try {
        await Api.post("/api/auth/logout");
      } finally {
        window.location.href = "index.html";
      }
    });
  } catch (e) {
    el.innerHTML = `
      <a href="login.html" class="btn btn-secondary btn-sm">Login</a>
      <a href="signup.html" class="btn btn-primary btn-sm">Sign Up</a>
    `;
  }

  markActiveNavLink();
}

/** Purely visual: highlights the current page's nav link. No behavior change. */
function markActiveNavLink() {
  const current = window.location.pathname.split("/").pop() || "index.html";
  document.querySelectorAll(".nav-links a").forEach((a) => {
    if (a.getAttribute("href") === current) {
      a.classList.add("active");
    }
  });
}

document.addEventListener("DOMContentLoaded", initNav);
