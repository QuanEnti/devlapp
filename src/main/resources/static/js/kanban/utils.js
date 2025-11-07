// ✅ utils.js – Hàm tiện ích chung cho toàn hệ thống

/* ---------------------- 🧱 ESCAPE HTML ---------------------- */
/**
 * Ngăn chặn XSS bằng cách thay thế các ký tự đặc biệt.
 * Dùng trước khi chèn dữ liệu server vào innerHTML.
 */
export function escapeHtml(unsafe = "") {
  return unsafe
    .replaceAll(/&/g, "&amp;")
    .replaceAll(/</g, "&lt;")
    .replaceAll(/>/g, "&gt;")
    .replaceAll(/"/g, "&quot;")
    .replaceAll(/'/g, "&#039;");
}

/* ---------------------- 🧭 SAFE STOP ---------------------- */
/**
 * Dừng nổi bọt / mặc định event một cách an toàn.
 */
export function safeStop(e) {
  if (e && typeof e.stopPropagation === "function") e.stopPropagation();
  if (e && typeof e.preventDefault === "function") e.preventDefault();
}

/* ---------------------- ⏰ FORMAT TIME ---------------------- */
/**
 * Định dạng thời gian theo ngữ cảnh người Việt.
 * @example formatTime("2025-11-07T10:30:00") → "10:30 07/11/2025"
 */
export function formatTime(dateStr) {
  if (!dateStr) return "";
  try {
    const d = new Date(dateStr);
    return `${d.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })} ${d.toLocaleDateString("vi-VN")}`;
  } catch {
    return dateStr;
  }
}

/* ---------------------- 👤 GET INITIALS ---------------------- */
/**
 * Trích ký tự đầu của tên người dùng (dùng khi chưa có avatar).
 * @example getInitials("Nguyễn Tiến Quân") → "NQ"
 */
export function getInitials(name = "") {
  return name
    .split(" ")
    .filter(Boolean)
    .slice(-2)
    .map(w => w.charAt(0).toUpperCase())
    .join("");
}

/* ---------------------- 🎨 RANDOM COLOR ---------------------- */
export function randomColor() {
  const colors = [
    "#F87171", "#FB923C", "#FACC15", "#4ADE80",
    "#60A5FA", "#A78BFA", "#F472B6", "#34D399", "#FBBF24"
  ];
  return colors[Math.floor(Math.random() * colors.length)];
}

/* ---------------------- 🔔 TOAST NOTIFICATIONS ---------------------- */
let toastStack = null;

export function showToast(msg, type = "info", duration = 3000) {
  if (!toastStack) {
    toastStack = document.getElementById("toast-stack");
    if (!toastStack) {
      toastStack = document.createElement("div");
      toastStack.id = "toast-stack";
      toastStack.className = "fixed bottom-4 right-4 z-[9999] flex flex-col gap-2";
      document.body.appendChild(toastStack);
    }
  }

  const toast = document.createElement("div");
  toast.className = `
    px-4 py-2 rounded-md shadow-md text-sm font-medium text-white animate-fadeIn
    ${type === "error" ? "bg-red-600" :
      type === "success" ? "bg-green-600" :
      type === "warn" ? "bg-yellow-500 text-gray-900" :
      "bg-gray-800"}
  `;
  toast.textContent = msg;

  toastStack.appendChild(toast);
  setTimeout(() => {
    toast.classList.add("animate-fadeOut");
    setTimeout(() => toast.remove(), 300);
  }, duration);
}

/* ---------------------- 🧾 API FETCH WRAPPER ---------------------- */
/**
 * Tự động thêm Authorization header và parse JSON nếu cần.
 */
export async function apiFetch(url, options = {}) {
  const token = window.AUTH_TOKEN || options.token || "";
  const headers = {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers || {}),
  };
  const res = await fetch(url, { ...options, headers });
  return res;
}

/* ---------------------- ⌛ DELAY ---------------------- */
export const delay = ms => new Promise(r => setTimeout(r, ms));

/* ---------------------- 🧩 SIMPLE ANIMATIONS ---------------------- */
export function fadeIn(el, display = "block") {
  if (!el) return;
  el.style.opacity = 0;
  el.style.display = display;
  el.style.transition = "opacity 0.2s ease";
  requestAnimationFrame(() => (el.style.opacity = 1));
}

export function fadeOut(el) {
  if (!el) return;
  el.style.opacity = 1;
  el.style.transition = "opacity 0.2s ease";
  requestAnimationFrame(() => (el.style.opacity = 0));
  setTimeout(() => (el.style.display = "none"), 200);
}

/* ---------------------- 🔐 TOKEN HELPERS ---------------------- */
export function saveToken(token) {
  localStorage.setItem("AUTH_TOKEN", token);
  window.AUTH_TOKEN = token;
}

export function getToken() {
  return window.AUTH_TOKEN || localStorage.getItem("AUTH_TOKEN") || "";
}

/* ---------------------- 🧰 EXPORT DEFAULT OBJECT ---------------------- */
export default {
  escapeHtml,
  safeStop,
  formatTime,
  getInitials,
  randomColor,
  showToast,
  apiFetch,
  fadeIn,
  fadeOut,
  delay,
  saveToken,
  getToken,
};
