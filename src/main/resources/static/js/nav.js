/* Populates #nav-right with either Login/Sign Up links or the
   current user + Logout, based on whether a session is active.
   Included on every page. */

async function initNav() {
  const el = document.getElementById("nav-right");
  if (!el) return;

  try {
    const me = await Api.get("/api/profile/me");
    el.innerHTML = `
      <a href="recipe-form.html" class="btn btn-primary btn-sm">Upload Recipe</a>
      <span class="nav-user"><a href="profile.html?user=${encodeURIComponent(me.username)}">${me.username}</a></span>
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
}

document.addEventListener("DOMContentLoaded", initNav);
