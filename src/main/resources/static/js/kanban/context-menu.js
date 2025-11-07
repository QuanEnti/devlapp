// ✅ context-menu.js – Quản lý menu chuột phải (Right-click menu) trên thẻ task
import { showToast } from "./utils.js";
import { openTaskModal } from "./modal-task.js";
import { currentProjectId } from "./main.js";

let contextMenu, boardContainer, currentTaskId;

document.addEventListener("DOMContentLoaded", () => {
  contextMenu = document.getElementById("card-context-menu");
  boardContainer = document.getElementById("kanban-board");

  if (!contextMenu || !boardContainer) return;

  /* ---------------------- 🖱️ HIỆN MENU ---------------------- */
  boardContainer.addEventListener("contextmenu", e => {
    const card = e.target.closest("[data-open-task]");
    if (!card) {
      hideContextMenu();
      return;
    }

    e.preventDefault();
    e.stopPropagation();

    currentTaskId = card.dataset.openTask;
    positionContextMenu(e);
    contextMenu.classList.remove("hidden");
  });

  /* ---------------------- 🖱️ ẨN MENU KHI CLICK NGOÀI ---------------------- */
  document.addEventListener("click", hideContextMenu);
  document.addEventListener("scroll", hideContextMenu, true);
});

/* ---------------------- 📍 ĐỊNH VỊ MENU ---------------------- */
function positionContextMenu(e) {
  const menuW = contextMenu.offsetWidth || 200;
  const menuH = contextMenu.offsetHeight || 240;
  const { clientX: x, clientY: y } = e;

  const viewportW = window.innerWidth;
  const viewportH = window.innerHeight;

  let left = x;
  let top = y;

  // Giới hạn menu không vượt khung hình
  if (x + menuW > viewportW) left = viewportW - menuW - 10;
  if (y + menuH > viewportH) top = viewportH - menuH - 10;

  contextMenu.style.left = `${left}px`;
  contextMenu.style.top = `${top}px`;
}

/* ---------------------- 🧹 ẨN MENU ---------------------- */
function hideContextMenu() {
  if (contextMenu) contextMenu.classList.add("hidden");
}

/* ---------------------- ⚙️ CÁC HÀNH ĐỘNG MENU ---------------------- */
document.getElementById("menu-edit")?.addEventListener("click", () => {
  hideContextMenu();
  if (!currentTaskId) return;
  openTaskModal(currentTaskId);
});

document.getElementById("menu-duplicate")?.addEventListener("click", async () => {
  hideContextMenu();
  if (!currentTaskId) return;
  try {
    const res = await fetch(`/api/tasks/${currentTaskId}/duplicate`, { method: "POST" });
    if (!res.ok) throw new Error();
    showToast("📋 Đã nhân bản task.");
    window.dispatchEvent(new CustomEvent("refreshBoard", { detail: { projectId: currentProjectId } }));
  } catch (err) {
    console.error("❌ Duplicate task failed:", err);
    showToast("Không thể nhân bản task.", "error");
  }
});

document.getElementById("menu-delete")?.addEventListener("click", async () => {
  hideContextMenu();
  if (!currentTaskId || !confirm("Bạn có chắc muốn xoá task này?")) return;
  try {
    const res = await fetch(`/api/tasks/${currentTaskId}`, { method: "DELETE" });
    if (!res.ok) throw new Error();
    showToast("🗑️ Đã xoá task.");
    document.querySelector(`[data-open-task="${currentTaskId}"]`)?.remove();
  } catch (err) {
    console.error("❌ Delete task failed:", err);
    showToast("Không thể xoá task.", "error");
  }
});

document.getElementById("menu-move")?.addEventListener("click", () => {
  hideContextMenu();
  if (!currentTaskId) return;
  const movePopup = document.getElementById("move-popup");
  if (movePopup) movePopup.classList.remove("hidden");
  showToast("↔️ Chọn cột đích để di chuyển task.");
});

document.getElementById("menu-copy-link")?.addEventListener("click", () => {
  hideContextMenu();
  if (!currentTaskId) return;
  const link = `${window.location.origin}/task/${currentTaskId}`;
  navigator.clipboard.writeText(link);
  showToast("🔗 Đã sao chép liên kết task vào clipboard.");
});
