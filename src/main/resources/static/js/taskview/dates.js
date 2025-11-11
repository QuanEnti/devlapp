// ================== IMPORT UTILS ==================
import { showToast, safeStop } from "./utils.js";

// ================== STATE ==================
const datePopup = document.getElementById("date-popup");
const closeDateBtn = document.getElementById("close-date-btn");
const saveDateBtn = document.getElementById("save-date-btn");
const removeDateBtn = document.getElementById("remove-date-btn");
const startCheck = document.getElementById("start-check");
const dueCheck = document.getElementById("due-check");
const startInput = document.getElementById("start-date-input");
const dueInput = document.getElementById("due-date-input");

// ================== STATUS BADGE ==================
export function updateDateStatus(dueDateStr) {
  const textEl = document.getElementById("due-date-text");
  const statusEl = document.getElementById("due-date-status");

  if (!dueDateStr) {
    textEl.textContent = "No due date";
    statusEl.textContent = "None";
    statusEl.className =
      "ml-2 text-xs font-medium rounded px-2 py-0.5 bg-gray-200 text-gray-600";
    return;
  }

  const due = new Date(dueDateStr);
  const now = new Date();
  const diffHours = (due - now) / (1000 * 60 * 60);
  const formatted = due.toLocaleString("en-US", {
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
  });

  textEl.textContent = formatted;

  if (diffHours < 0) {
    statusEl.textContent = "Overdue";
    statusEl.className =
      "ml-2 text-xs font-medium rounded px-2 py-0.5 bg-red-100 text-red-700";
  } else if (diffHours <= 24) {
    statusEl.textContent = "Due soon";
    statusEl.className =
      "ml-2 text-xs font-medium rounded px-2 py-0.5 bg-yellow-100 text-yellow-700";
  } else {
    statusEl.textContent = "On track";
    statusEl.className =
      "ml-2 text-xs font-medium rounded px-2 py-0.5 bg-green-100 text-green-700";
  }
}

// ================== OPEN POPUP ==================
export function openDatePopup(e) {
  console.log("🔍 openDatePopup called");
  safeStop(e);

  let rect = null;
  if (e?.currentTarget?.getBoundingClientRect) {
    rect = e.currentTarget.getBoundingClientRect();
  }

  const isValidRect =
    rect && rect.top > 0 && rect.left > 0 && rect.width > 0 && rect.height > 0;
  let top, left;

  if (!isValidRect) {
    // Gọi từ context menu → dùng tọa độ chuột
    top = (window.contextMenuY || 100) + 10;
    left = (window.contextMenuX || 100) + 10;
  } else {
    // Gọi từ nút modal → dùng vị trí button
    top = rect.bottom + window.scrollY + 6;
    left = rect.left + window.scrollX;
  }

  datePopup.style.top = `${top}px`;
  datePopup.style.left = `${left}px`;
  datePopup.classList.remove("hidden");
  console.log("✅ Date popup displayed at:", { top, left });
}

// ================== CLOSE POPUP ==================
export function closeDatePopup() {
  datePopup.classList.add("hidden");
  console.log("✅ Date popup closed");
}

// ================== EVENT BINDINGS ==================
if (closeDateBtn) closeDateBtn.addEventListener("click", closeDatePopup);

document.addEventListener("mousedown", (e) => {
  const inside = datePopup.contains(e.target);
  const fromContextMenu = e.target.closest("#card-context-menu");
  if (!inside && !fromContextMenu && e.button !== 2) closeDatePopup();
});

startCheck?.addEventListener(
  "change",
  () => (startInput.disabled = !startCheck.checked)
);
dueCheck?.addEventListener(
  "change",
  () => (dueInput.disabled = !dueCheck.checked)
);

removeDateBtn?.addEventListener("click", async () => {
  const taskId = window.CURRENT_TASK_ID;
  if (!taskId) return;

  try {
    await fetch(`/api/tasks/${taskId}/dates`, {
      method: "PUT",
      headers: {
        Authorization: "Bearer " + localStorage.getItem("token"),
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ startDate: null, deadline: null }),
    });
    updateDateStatus(null);
    showToast("🗑️ Removed due date");
  } catch {
    showToast("❌ Failed to remove", "error");
  } finally {
    closeDatePopup();
  }
});

saveDateBtn?.addEventListener("click", async () => {
  const taskId = window.CURRENT_TASK_ID;
  const start = startCheck.checked ? startInput.value : null;
  const due = dueCheck.checked ? dueInput.value : null;
  const recurring = document.getElementById("recurring-select")?.value;
  const reminder = document.getElementById("reminder-select")?.value;

  try {
    const res = await fetch(`/api/tasks/${taskId}/dates`, {
      method: "PUT",
      headers: {
        Authorization: "Bearer " + localStorage.getItem("token"),
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        startDate: start,
        deadline: due,
        recurring,
        reminder,
      }),
    });

    if (!res.ok) throw new Error();
    const updated = await res.json();
    updateDateStatus(updated.deadline);
    closeDatePopup();
  } catch (err) {
    console.error("❌ saveDate error:", err);
    showToast("⚠️ Failed to save date", "error");
  }
});
