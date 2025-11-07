// ✅ notifications.js – Quản lý thông báo (panel + WebSocket realtime)
import { escapeHtml, showToast } from "./utils.js";
import { API_BASE_URL, AUTH_TOKEN } from "./main.js";

let notifications = [];
let socket = null;

/* ---------------------- 🧱 LOAD PANEL ---------------------- */
export async function loadNotifications() {
  try {
    const res = await fetch(`${API_BASE_URL}/api/notifications`, {
      headers: { Authorization: `Bearer ${AUTH_TOKEN}` },
    });
    if (!res.ok) throw new Error();
    notifications = await res.json();
    renderNotifications(notifications);
  } catch (err) {
    console.error("❌ loadNotifications failed:", err);
  }
}

/* ---------------------- 🔔 RENDER PANEL ---------------------- */
function renderNotifications(list) {
  const container = document.getElementById("notification-list");
  const badge = document.getElementById("notification-badge");

  if (!container) return;

  if (!list || !list.length) {
    container.innerHTML = `<p class="text-gray-400 italic py-2 text-center">No notifications</p>`;
    badge.classList.add("hidden");
    return;
  }

  // Đếm thông báo chưa đọc
  const unreadCount = list.filter(n => !n.read).length;
  if (unreadCount > 0) {
    badge.textContent = unreadCount > 9 ? "9+" : unreadCount;
    badge.classList.remove("hidden");
  } else {
    badge.classList.add("hidden");
  }

  // Render danh sách
  container.innerHTML = list
    .map(n => `
      <div class="notification-item ${n.read ? 'opacity-70' : 'bg-blue-50'} border-b px-3 py-2 hover:bg-blue-100 transition cursor-pointer"
           data-id="${n.id}">
        <p class="text-sm">${escapeHtml(n.message)}</p>
        <p class="text-xs text-gray-500">${new Date(n.createdAt).toLocaleString("vi-VN")}</p>
      </div>
    `)
    .join("");

  container.querySelectorAll(".notification-item").forEach(item => {
    item.addEventListener("click", () => markAsRead(item.dataset.id));
  });
}

/* ---------------------- 📩 MỞ / ĐÓNG PANEL ---------------------- */
const bellBtn = document.getElementById("notification-btn");
const panel = document.getElementById("notification-panel");

if (bellBtn && panel) {
  bellBtn.addEventListener("click", async e => {
    e.stopPropagation();
    panel.classList.toggle("hidden");
    if (!panel.classList.contains("hidden")) {
      await loadNotifications();
    }
  });

  document.addEventListener("click", e => {
    if (!panel.contains(e.target) && !bellBtn.contains(e.target)) {
      panel.classList.add("hidden");
    }
  });
}

/* ---------------------- ✅ MARK AS READ ---------------------- */
async function markAsRead(id) {
  try {
    const res = await fetch(`${API_BASE_URL}/api/notifications/${id}/read`, {
      method: "PUT",
      headers: { Authorization: `Bearer ${AUTH_TOKEN}` },
    });
    if (!res.ok) throw new Error();
    notifications = notifications.map(n =>
      n.id == id ? { ...n, read: true } : n
    );
    renderNotifications(notifications);
  } catch (err) {
    console.error("❌ markAsRead failed:", err);
  }
}

document.getElementById("mark-all-read")?.addEventListener("click", async () => {
  try {
    const res = await fetch(`${API_BASE_URL}/api/notifications/mark-all`, {
      method: "PUT",
      headers: { Authorization: `Bearer ${AUTH_TOKEN}` },
    });
    if (!res.ok) throw new Error();
    notifications = notifications.map(n => ({ ...n, read: true }));
    renderNotifications(notifications);
    showToast("✅ Đã đánh dấu tất cả là đã đọc.");
  } catch (err) {
    console.error("❌ markAllRead failed:", err);
  }
});

/* ---------------------- 🌐 WEBSOCKET REALTIME ---------------------- */
export function initNotificationSocket(userId) {
  if (!userId) return;

  const socketUrl = `${API_BASE_URL.replace(/^http/, "ws")}/ws/notifications?userId=${userId}`;
  socket = new WebSocket(socketUrl);

  socket.onopen = () => console.log("🔗 Connected to notification socket.");
  socket.onmessage = event => {
    try {
      const msg = JSON.parse(event.data);
      handleIncomingNotification(msg);
    } catch (err) {
      console.error("❌ Invalid socket message:", event.data);
    }
  };
  socket.onclose = () => {
    console.warn("🔌 Socket closed, reconnecting in 5s...");
    setTimeout(() => initNotificationSocket(userId), 5000);
  };
}

/* ---------------------- 🆕 XỬ LÝ THÔNG BÁO MỚI ---------------------- */
function handleIncomingNotification(msg) {
  if (!msg || !msg.message) return;

  // Thêm vào đầu danh sách
  notifications.unshift(msg);
  renderNotifications(notifications);
  showToast(`🔔 ${msg.message}`);

  // Hiển thị badge nháy
  const badge = document.getElementById("notification-badge");
  badge.classList.remove("hidden");
  badge.textContent = "•";
  badge.classList.add("animate-pulse");
  setTimeout(() => badge.classList.remove("animate-pulse"), 2000);
}

/* ---------------------- 🧹 CLEANUP ---------------------- */
export function closeSocket() {
  if (socket) socket.close();
}
