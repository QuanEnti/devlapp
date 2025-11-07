// ✅ dates.js – Quản lý popup chọn ngày cho task
import { apiFetch, showToast } from "./utils.js";
import { currentTask } from "./modal-task.js";

let selectedStart = null;
let selectedDue = null;
let selectedRecurring = "Never";
let selectedReminder = "Never";

/* ---------------------- 📅 OPEN / CLOSE POPUP ---------------------- */
export function openDatePopup(event) {
  const popup = document.getElementById("date-popup");
  if (!popup) return;
  event?.stopPropagation();

  // Hiển thị popup gần phần due-date-display
  const rect = event?.target?.getBoundingClientRect?.() || { top: 150, left: 400 };
  popup.style.top = `${rect.top + window.scrollY + 40}px`;
  popup.style.left = `${rect.left + window.scrollX}px`;

  popup.classList.remove("hidden");

  // Điền giá trị sẵn có của task
  const startInput = document.getElementById("start-date-input");
  const dueInput = document.getElementById("due-date-input");
  const recurringSelect = document.getElementById("recurring-select");
  const reminderSelect = document.getElementById("reminder-select");

  selectedStart = currentTask?.startDate || "";
  selectedDue = currentTask?.dueDate || "";
  selectedRecurring = currentTask?.recurring || "Never";
  selectedReminder = currentTask?.reminder || "Never";

  startInput.value = selectedStart ? formatInputDate(selectedStart) : "";
  dueInput.value = selectedDue ? formatInputDate(selectedDue) : "";
  recurringSelect.value = selectedRecurring;
  reminderSelect.value = selectedReminder;

  document.getElementById("start-check").checked = !!selectedStart;
  document.getElementById("due-check").checked = !!selectedDue;

  startInput.disabled = !selectedStart;
  dueInput.disabled = !selectedDue;
}

/* ---------------------- 🧹 CLOSE ---------------------- */
document.getElementById("close-date-btn")?.addEventListener("click", () => {
  document.getElementById("date-popup")?.classList.add("hidden");
});

/* ---------------------- 💾 SAVE ---------------------- */
export async function saveDate() {
  if (!currentTask) return;
  const startChecked = document.getElementById("start-check").checked;
  const dueChecked = document.getElementById("due-check").checked;
  const startInput = document.getElementById("start-date-input").value || null;
  const dueInput = document.getElementById("due-date-input").value || null;
  const recurring = document.getElementById("recurring-select").value;
  const reminder = document.getElementById("reminder-select").value;

  const payload = {
    startDate: startChecked ? startInput : null,
    dueDate: dueChecked ? dueInput : null,
    recurring,
    reminder
  };

  try {
    const res = await apiFetch(`/api/tasks/${currentTask.taskId}/dates`, {
      method: "PUT",
      body: JSON.stringify(payload)
    });
    if (!res.ok) throw new Error("Failed to save date");
    showToast("✅ Đã lưu ngày thành công!");
    document.getElementById("date-popup").classList.add("hidden");
    updateDateStatus(payload);
  } catch (err) {
    console.error("❌ saveDate failed:", err);
    showToast("Không thể lưu ngày.", "error");
  }
}

document.getElementById("save-date-btn")?.addEventListener("click", saveDate);

/* ---------------------- 🗑️ REMOVE ---------------------- */
export async function removeDate() {
  if (!currentTask) return;
  try {
    const res = await apiFetch(`/api/tasks/${currentTask.taskId}/dates`, { method: "DELETE" });
    if (!res.ok) throw new Error();
    currentTask.startDate = null;
    currentTask.dueDate = null;
    updateDateStatus({});
    showToast("🗑️ Đã xóa ngày cho task.");
    document.getElementById("date-popup").classList.add("hidden");
  } catch (err) {
    console.error("❌ removeDate failed:", err);
    showToast("Không thể xóa ngày.", "error");
  }
}

document.getElementById("remove-date-btn")?.addEventListener("click", removeDate);

/* ---------------------- 🕓 UPDATE TRẠNG THÁI TRÊN UI ---------------------- */
export function updateDateStatus({ startDate, dueDate }) {
  const dueText = document.getElementById("due-date-text");
  const dueStatus = document.getElementById("due-date-status");

  if (!dueText || !dueStatus) return;

  if (dueDate) {
    const dateObj = new Date(dueDate);
    const now = new Date();
    const diffDays = Math.floor((dateObj - now) / (1000 * 60 * 60 * 24));

    dueText.textContent = dateObj.toLocaleString("vi-VN");
    dueStatus.textContent =
      diffDays < 0 ? "Overdue" : diffDays === 0 ? "Today" : `${diffDays} days left`;
    dueStatus.className = `ml-2 text-xs font-medium rounded px-2 py-0.5 ${
      diffDays < 0
        ? "bg-red-100 text-red-700"
        : diffDays === 0
        ? "bg-yellow-100 text-yellow-700"
        : "bg-blue-100 text-blue-700"
    }`;
  } else {
    dueText.textContent = "No due date";
    dueStatus.textContent = "None";
    dueStatus.className =
      "ml-2 text-xs font-medium rounded px-2 py-0.5 bg-gray-200 text-gray-600";
  }
}

/* ---------------------- ⚙️ HELPER ---------------------- */
function formatInputDate(dateStr) {
  const d = new Date(dateStr);
  const iso = d.toISOString();
  return iso.slice(0, 16); // yyyy-MM-ddTHH:mm
}

/* ---------------------- 🧠 CHECKBOX ENABLE/DISABLE ---------------------- */
document.getElementById("start-check")?.addEventListener("change", e => {
  document.getElementById("start-date-input").disabled = !e.target.checked;
});
document.getElementById("due-check")?.addEventListener("change", e => {
  document.getElementById("due-date-input").disabled = !e.target.checked;
});
