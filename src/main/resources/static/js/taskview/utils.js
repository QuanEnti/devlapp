// ================== 🧰 UTILS ==================

// Escape HTML
export function escapeHtml(str) {
  if (!str) return "";
  return str.replace(
    /[&<>"']/g,
    (c) =>
      ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;",
      }[c])
  );
}

// Toast
export function showToast(message, type = "info") {
  const toast = document.createElement("div");
  toast.textContent = message;
  toast.className = `fixed bottom-4 right-4 px-4 py-2 rounded-lg text-white z-[9999]
      ${
        type === "error" ? "bg-red-500" : "bg-green-600"
      } shadow-lg animate-fadeIn`;
  document.body.appendChild(toast);
  setTimeout(() => {
    toast.classList.add("opacity-0", "transition-opacity", "duration-300");
    setTimeout(() => toast.remove(), 300);
  }, 2500);
}

// Relative time
export function formatRelativeTime(isoString) {
  const now = new Date();
  const date = new Date(isoString);
  const diff = Math.floor((now - date) / 60000);
  if (diff < 1) return "just now";
  if (diff < 60) return `${diff} min${diff > 1 ? "s" : ""} ago`;
  const hrs = Math.floor(diff / 60);
  if (hrs < 24) return `${hrs} hour${hrs > 1 ? "s" : ""} ago`;
  const days = Math.floor(hrs / 24);
  return `${days} day${days > 1 ? "s" : ""} ago`;
}

// Absolute time (ví dụ cho logs hoặc due date)
export function formatTime(isoString) {
  const d = new Date(isoString);
  return d.toLocaleString("en-US", {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

// Safe stop interval
export function safeStop(id) {
  if (id) clearInterval(id);
}

// =============== 🎨 COLOR & AVATAR HELPERS ===============

// Sinh màu ngẫu nhiên ổn định theo ID (dùng cho labels, avatars)
export function getColorForId(id) {
  if (!id) return "#10b981"; // emerald-500 fallback
  const colors = [
    "#ef4444", // red-500
    "#f97316", // orange-500
    "#eab308", // yellow-500
    "#22c55e", // green-500
    "#10b981", // emerald-500 (thêm màu xanh lá đậm)
    "#3b82f6", // blue-500
    "#a855f7", // purple-500
    "#ec4899", // pink-500
    "#14b8a6", // teal-500
    "#06b6d4", // cyan-500
  ];
  let hash = 0;
  for (let i = 0; i < id.length; i++)
    hash = id.charCodeAt(i) + ((hash << 5) - hash);
  return colors[Math.abs(hash) % colors.length];
}

// Lấy ký tự đầu cho avatar placeholder
export function getInitials(name = "U") {
  return name
    .split(" ")
    .map((w) => w[0])
    .join("")
    .substring(0, 2)
    .toUpperCase();
}

// Render avatar (img hoặc fallback chữ)
export function renderAvatar(user) {
  if (user?.avatarUrl) {
    return `<img src="${user.avatarUrl}" alt="${escapeHtml(user.name)}"
              class="w-8 h-8 rounded-full border object-cover">`;
  }
  const initials = getInitials(user?.name);
  const bg = getColorForId(user?.userId || user?.id || initials);
  return `<div class="w-8 h-8 flex items-center justify-center rounded-full text-white font-medium"
                style="background-color:${bg}">${initials}</div>`;
}

// =============== 🔔 REUSABLE TOAST STACK (Realtime) ===============
export function ensureToastStack() {
  let stack = document.getElementById("toast-stack");
  if (!stack) {
    stack = document.createElement("div");
    stack.id = "toast-stack";
    document.body.appendChild(stack);
  }
  return stack;
}

export function showToastNotification(
  title,
  msg,
  avatarUrl,
  sender = "System"
) {
  const stack = ensureToastStack();
  const toast = document.createElement("div");
  toast.className = "toast-card";

  const safeAvatar =
    avatarUrl && avatarUrl.trim() !== ""
      ? avatarUrl
      : "https://cdn-icons-png.flaticon.com/512/149/149071.png";

  toast.innerHTML = `
      <img src="${safeAvatar}" class="w-10 h-10 rounded-full object-cover border" alt="avatar">
      <div class="flex-1">
        <div class="toast-title">${escapeHtml(sender)}</div>
        <div class="toast-msg">${escapeHtml(msg)}</div>
        <div class="toast-meta">${formatRelativeTime(
          new Date().toISOString()
        )}</div>
      </div>
    `;

  stack.appendChild(toast);
  setTimeout(() => {
    toast.classList.add("toast-exit");
    setTimeout(() => toast.remove(), 200);
  }, 5000);
}
