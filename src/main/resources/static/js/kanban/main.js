// ✅ main.js – Điểm khởi tạo toàn bộ Kanban Board
import { renderDashboard } from "./board.js";
import { loadNotifications, connectNotificationSocket } from "./notifications.js";
import { apiFetch, showToast } from "./utils.js";

let currentUser = null;
let currentRole = null;
let PROJECT_ID = null;

async function loadCurrentUser() {
  try {
    const res = await apiFetch("/api/auth/me");
    const data = await res.json();
    currentUser = data.user || data;
    console.log("✅ Current user:", currentUser);
  } catch (err) {
    console.warn("⚠️ loadCurrentUser failed:", err.message);
    currentUser = {
      userId: null,
      name: "Guest",
      email: "",
      avatarUrl: "https://i.pravatar.cc/100?u=guest",
      roles: [],
      isPM: false
    };
  }
  updateUserUI();
}

function updateUserUI() {
  const nameEl = document.getElementById("userName");
  const emailEl = document.getElementById("userEmail");
  const avatarEl = document.getElementById("userAvatar");
  if (nameEl) nameEl.textContent = currentUser.name;
  if (emailEl) emailEl.textContent = currentUser.email || "";
  if (avatarEl) avatarEl.src = currentUser.avatarUrl;
}

async function fetchProjectRole(projectId) {
  try {
    const res = await apiFetch(`/api/projects/${projectId}/role`);
    const role = await res.text();
    currentRole = role || "MEMBER";
    console.log("🎭 Current project role:", currentRole);
  } catch (err) {
    console.warn("⚠️ fetchProjectRole failed:", err.message);
    currentRole = "MEMBER";
  }
}

async function initializeKanban() {
  PROJECT_ID = new URLSearchParams(window.location.search).get("projectId") || 1;

  console.log("🚀 Initializing Kanban for Project:", PROJECT_ID);
  await loadCurrentUser();
  await fetchProjectRole(PROJECT_ID);

  await Promise.allSettled([
    renderDashboard(PROJECT_ID),
    loadNotifications(PROJECT_ID)
  ]);

  connectNotificationSocket(PROJECT_ID);

  // Event bindings
  const shareBtn = document.getElementById("share-board-btn");
  if (shareBtn) shareBtn.addEventListener("click", () => {
    document.getElementById("share-board-popup").classList.remove("hidden");
  });

  const notifBtn = document.getElementById("open-notif-btn");
  if (notifBtn) notifBtn.addEventListener("click", toggleNotifPanel);

  console.log("✅ Kanban initialized completely");
}

function toggleNotifPanel() {
  const panel = document.getElementById("notif-panel");
  if (panel) panel.classList.toggle("hidden");
}

// 🧱 Entry point
document.addEventListener("DOMContentLoaded", async () => {
  try {
    await initializeKanban();
  } catch (err) {
    console.error("❌ Failed to init Kanban:", err);
    showToast("Cannot load Kanban board. Please refresh.", "error");
  }
});

// Exports (nếu cần import ở nơi khác)
export { currentUser, currentRole, PROJECT_ID };
