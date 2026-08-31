function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str == null ? "" : str;
  return div.innerHTML;
}

function formatTime(iso) {
  return new Date(iso).toLocaleString(undefined, { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" });
}

let currentUser = null;
let activePartner = new URLSearchParams(window.location.search).get("with") || null;
let pollHandle = null;

async function requireLogin() {
  try {
    currentUser = await Api.get("/api/profile/me");
  } catch (e) {
    window.location.href = "login.html";
  }
}

async function loadConversations() {
  const list = document.getElementById("conversation-list");
  const conversations = await Api.get("/api/chat/conversations");

  if (activePartner && !conversations.some((c) => c.otherUsername === activePartner)) {
    conversations.unshift({ otherUsername: activePartner, lastMessage: "", lastMessageAt: null });
  }

  if (conversations.length === 0) {
    list.innerHTML = `<p class="empty-state">No conversations yet. Message someone from their profile.</p>`;
    return;
  }

  list.innerHTML = conversations.map((c) => `
    <div class="conversation-item ${c.otherUsername === activePartner ? "active" : ""}" data-user="${escapeHtml(c.otherUsername)}">
      <span class="partner">${escapeHtml(c.otherUsername)}</span>
      <span class="snippet">${escapeHtml(c.lastMessage || "")}</span>
    </div>
  `).join("");

  list.querySelectorAll(".conversation-item").forEach((el) => {
    el.addEventListener("click", () => openConversation(el.dataset.user));
  });
}

async function loadMessages() {
  if (!activePartner) return;
  const messages = await Api.get(`/api/chat/${encodeURIComponent(activePartner)}/messages`);
  const container = document.getElementById("messages");
  container.innerHTML = messages.map((m) => `
    <div class="bubble ${m.senderUsername === currentUser.username ? "mine" : "theirs"}">
      ${escapeHtml(m.text)}
      <span class="time">${formatTime(m.createdAt)}</span>
    </div>
  `).join("");
  container.scrollTop = container.scrollHeight;
}

function openConversation(username) {
  activePartner = username;
  document.getElementById("thread-header").textContent = username;
  document.getElementById("send-btn").disabled = false;
  document.querySelectorAll(".conversation-item").forEach((el) => {
    el.classList.toggle("active", el.dataset.user === username);
  });
  loadMessages();
}

document.getElementById("send-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  if (!activePartner) return;
  const input = document.getElementById("message-text");
  const text = input.value.trim();
  if (!text) return;
  try {
    await Api.post(`/api/chat/${encodeURIComponent(activePartner)}/messages`, { text });
    input.value = "";
    await loadMessages();
    await loadConversations();
  } catch (err) {
    alert(err.message);
  }
});

(async () => {
  await requireLogin();
  if (!currentUser) return;
  await loadConversations();
  if (activePartner) {
    openConversation(activePartner);
  }
  // Thin-slice chat: poll rather than WebSocket -- see DESIGN-DEVIATIONS.md.
  pollHandle = setInterval(() => {
    if (activePartner) loadMessages();
    loadConversations();
  }, 4000);
})();

window.addEventListener("beforeunload", () => {
  if (pollHandle) clearInterval(pollHandle);
});
