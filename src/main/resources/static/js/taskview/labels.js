// ============================================================
// 🏷️ LABELS MODULE – Manage Project Labels for Tasks
// ============================================================
import { showToast, safeStop } from "./utils.js"; // ✅ thêm safeStop

let selectedColor = null;
let selectedEditColor = null; // ✅ thêm biến cho edit preview
let debounceLabelTimer;

// ================= INIT EVENTS =================
export function initLabelEvents() {
  const labelsPopup = document.getElementById("labels-popup");
  const openLabelsBtn = document.getElementById("open-labels-btn");
  const closeLabelsBtn = document.getElementById("close-labels-btn");
  const searchLabelInput = document.getElementById("search-label-input");

  const createLabelPopup = document.getElementById("create-label-popup");
  const createLabelBtn = document.getElementById("create-label-btn");
  const closeCreateLabelBtn = document.getElementById("close-create-label-btn");
  const createLabelConfirm = document.getElementById("create-label-confirm");
  const colorGrid = document.getElementById("color-grid");
  const newLabelName = document.getElementById("new-label-name");

  // 🟢 Mở popup labels
  openLabelsBtn?.addEventListener("click", openLabelsPopup);

  // ❌ Đóng popup labels
  closeLabelsBtn?.addEventListener("click", () =>
    labelsPopup?.classList.add("hidden")
  );

  // 🔍 Tìm kiếm nhãn (debounce)
  searchLabelInput?.addEventListener("input", (e) => {
    clearTimeout(debounceLabelTimer);
    debounceLabelTimer = setTimeout(() => {
      loadLabels(e.target.value.trim());
    }, 350);
  });

  // ➕ Mở popup tạo nhãn
  createLabelBtn?.addEventListener("click", (e) => {
    const rect = e.currentTarget.getBoundingClientRect();
    createLabelPopup.style.position = "absolute";
    createLabelPopup.style.top = `${rect.bottom + window.scrollY + 8}px`;
    createLabelPopup.style.left = `${rect.left + window.scrollX}px`;
    createLabelPopup.classList.remove("hidden");
    labelsPopup?.classList.add("hidden");
    renderColorGrid(colorGrid);
    updateLabelPreview();
  });

  // ❌ Đóng popup tạo nhãn
  closeCreateLabelBtn?.addEventListener("click", () =>
    createLabelPopup.classList.add("hidden")
  );

  newLabelName?.addEventListener("input", updateLabelPreview);
  createLabelConfirm?.addEventListener("click", handleCreateLabel);

  // 🪄 Expose global cho inline HTML và context menu
  Object.assign(window, {
    openLabelsPopup,
    handleLabelChange,
    assignLabel,
    unassignLabel,
    selectColor,
  });
}

// ================= OPEN POPUP =================
export function openLabelsPopup(e) {
  safeStop(e);

  const labelsPopup = document.getElementById("labels-popup");
  if (!labelsPopup) return console.error("❌ labelsPopup not found in DOM");

  let rect = e?.currentTarget?.getBoundingClientRect?.() ?? null;
  const isValidRect = rect && rect.width > 0 && rect.height > 0;

  const top = isValidRect
    ? rect.bottom + window.scrollY + 6
    : (window.contextMenuY || e?.clientY || 100) + 8;
  const left = isValidRect
    ? rect.left + window.scrollX
    : (window.contextMenuX || e?.clientX || 100) + 8;

  labelsPopup.style.position = "fixed";
  labelsPopup.style.top = `${top}px`;
  labelsPopup.style.left = `${left}px`;
  labelsPopup.classList.remove("hidden");

  console.log(`📍 Labels popup opened at top=${top}, left=${left}`);
  loadLabels();
}

// ================= LOAD LABELS =================
export async function loadLabels(keyword = "") {
  const projectId = window.PROJECT_ID;
  const taskId = window.CURRENT_TASK_ID;
  const listEl = document.getElementById("labels-list");
  if (!taskId) return;

  listEl.innerHTML = `<p class="text-gray-400 text-sm italic">Loading...</p>`;

  try {
    const headers = { Authorization: "Bearer " + localStorage.getItem("token") };

    const [resAll, resTask] = await Promise.all([
      fetch(`/api/labels?projectId=${projectId}&keyword=${encodeURIComponent(keyword)}`, { headers }),
      fetch(`/api/tasks/${taskId}/labels`, { headers }),
    ]);

    if (!resAll.ok || !resTask.ok)
      throw new Error("Failed to load labels or task labels");

    const [allLabels, taskLabels] = await Promise.all([
      resAll.json(),
      resTask.json(),
    ]);

    const assignedIds = new Set(taskLabels.map((l) => l.labelId));
    listEl.innerHTML = allLabels.length
      ? allLabels.map((l) => renderLabelRow(l, assignedIds.has(l.labelId))).join("")
      : `<p class="text-gray-400 text-sm italic">No labels found</p>`;
  } catch (err) {
    console.error("❌ loadLabels error:", err);
    listEl.innerHTML = `<p class="text-red-500 text-sm">Failed to load labels</p>`;
  }
}

// ================= RENDER ROW =================
function renderLabelRow(label, isAssigned) {
  const taskId = window.CURRENT_TASK_ID;
  return `
    <div class="flex items-center justify-between p-1 hover:bg-gray-50 rounded-md transition">
      <div class="flex items-center gap-2">
        <input type="checkbox" ${isAssigned ? "checked" : ""}
          onchange="handleLabelChange(${taskId},${label.labelId},this.checked)">
        <div class="w-5 h-5 rounded-md border" style="background:${label.color || "#ccc"}"></div>
        <p class="text-sm text-gray-700">${label.name}</p>
      </div>
    </div>`;
}

function handleLabelChange(taskId, labelId, isChecked) {
  if (isChecked) assignLabel(taskId, labelId);
  else unassignLabel(taskId, labelId);
}

// ================= ASSIGN / UNASSIGN =================
export async function assignLabel(taskId, labelId) {
  try {
    const res = await fetch(`/api/tasks/${taskId}/labels/${labelId}`, {
      method: "POST",
      headers: { Authorization: "Bearer " + localStorage.getItem("token") },
    });
    if (!res.ok) throw new Error(`Failed to assign label (${res.status})`);
    showToast("✅ Label assigned", "success");
    await loadLabels();
  } catch (err) {
    console.error("❌ assignLabel error:", err);
    showToast("❌ Failed to assign label", "error");
  }
}

export async function unassignLabel(taskId, labelId) {
  try {
    const res = await fetch(`/api/tasks/${taskId}/labels/${labelId}`, {
      method: "DELETE",
      headers: {
        Authorization: "Bearer " + localStorage.getItem("token"),
        "Content-Type": "application/json",
      },
    });
    if (res.ok || res.status === 204) {
      showToast("🗑️ Label removed", "success");
      await loadLabels();
      return;
    }
    throw new Error(`Failed to unassign label: ${res.status}`);
  } catch (err) {
    console.error("❌ unassignLabel error:", err);
    showToast("❌ Failed to remove label", "error");
  }
}

// ================= CREATE LABEL =================
async function handleCreateLabel() {
  const name = document.getElementById("new-label-name").value.trim();
  if (!name) return showToast("⚠️ Please enter label name", "error");

  const body = new URLSearchParams({
    projectId: window.PROJECT_ID,
    name,
    color: selectedColor || "#9CA3AF",
  });

  try {
    const res = await fetch(`/api/labels?${body.toString()}`, {
      method: "POST",
      headers: { Authorization: "Bearer " + localStorage.getItem("token") },
    });
    if (!res.ok) throw new Error("Create failed");
    showToast("✅ Label created", "success");
    document.getElementById("create-label-popup").classList.add("hidden");
    await loadLabels();
  } catch (err) {
    console.error("❌ createLabel error:", err);
    showToast("❌ Failed to create label", "error");
  }
}

// ================= COLOR GRID + PREVIEW =================
function renderColorGrid(gridEl) {
  const colors = [
    "#16a34a", "#ef4444", "#f59e0b", "#eab308",
    "#06b6d4", "#3b82f6", "#8b5cf6", "#ec4899",
    "#f97316", "#10b981", "#4ade80", "#a855f7",
    "#6366f1", "#14b8a6", "#84cc16", "#94a3b8",
  ];
  gridEl.innerHTML = colors.map(
    (c) => `
      <div class="w-6 h-6 rounded cursor-pointer border border-gray-200 hover:scale-110 transition"
           style="background:${c}"
           onclick="selectColor('${c}')"></div>`
  ).join("");
}

function selectColor(c) {
  selectedColor = c;
  updateLabelPreview();
}

function updateLabelPreview() {
  const preview = document.getElementById("label-preview");
  const name = document.getElementById("new-label-name").value.trim() || "New Label";
  preview.textContent = name;
  preview.style.background = selectedColor || "#9CA3AF";
  preview.style.color = "#fff";
  preview.style.borderColor = selectedColor || "#9CA3AF";
}

// ================= EDIT PREVIEW =================
function updateEditLabelPreview() {
  const preview = document.getElementById("edit-label-preview");
  const name = document.getElementById("edit-label-name").value.trim() || "New";
  preview.textContent = name;
  preview.style.background = selectedEditColor || "#9CA3AF";
  preview.style.color = "#fff";
}
document.getElementById("edit-label-name")?.addEventListener("input", updateEditLabelPreview);

// ✅ Expose global cho context menu hoặc inline script gọi được
Object.assign(window, {
  openLabelsPopup,
  loadLabels,
  assignLabel,
  unassignLabel,
});
