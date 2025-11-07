// ✅ labels.js – Quản lý label của task (hiển thị, tạo mới, gán, gỡ)
import { apiFetch, escapeHtml, getColorForId, showToast } from "./utils.js";
import { currentTask } from "./modal-task.js";

let allLabels = [];
let assignedLabels = [];

/* ---------------------- 🧱 LOAD LABELS ---------------------- */
export async function loadLabels(projectId) {
  try {
    const res = await apiFetch(`/api/projects/${projectId}/labels`);
    allLabels = await res.json();
    return allLabels;
  } catch (err) {
    console.error("❌ loadLabels failed:", err);
    showToast("Không thể tải danh sách nhãn.", "error");
    return [];
  }
}

/* ---------------------- 🏷️ POPUP MỞ / ĐÓNG ---------------------- */
export function openLabelsPopup(task) {
  const popup = document.getElementById("labels-popup");
  if (!popup) return;
  assignedLabels = [...(task.labels || [])];
  currentTask = task;
  renderLabelList(allLabels, assignedLabels);
  popup.classList.remove("hidden");
}

export function closeLabelsPopup() {
  const popup = document.getElementById("labels-popup");
  if (popup) popup.classList.add("hidden");
  currentTask = null;
}

document.getElementById("close-labels-btn")?.addEventListener("click", closeLabelsPopup);

/* ---------------------- 🔍 TÌM KIẾM ---------------------- */
const searchInput = document.getElementById("search-label-input");
if (searchInput) {
  searchInput.addEventListener("input", e => {
    const keyword = e.target.value.toLowerCase();
    const filtered = allLabels.filter(l => l.name.toLowerCase().includes(keyword));
    renderLabelList(filtered, assignedLabels);
  });
}

/* ---------------------- 🧩 HIỂN THỊ DANH SÁCH ---------------------- */
function renderLabelList(list, assigned) {
  const container = document.getElementById("labels-list");
  if (!container) return;

  if (!list.length) {
    container.innerHTML = `<p class="text-gray-400 italic text-sm">No labels found.</p>`;
    return;
  }

  container.innerHTML = "";
  list.forEach(label => {
    const isAssigned = assigned.some(a => a.labelId === label.labelId);
    const div = document.createElement("div");
    div.className = "flex items-center justify-between px-2 py-1 rounded hover:bg-gray-50";

    div.innerHTML = `
      <div class="flex items-center gap-2">
        <div class="w-4 h-4 rounded" style="background:${label.color || getColorForId(label.labelId)}"></div>
        <p class="text-sm text-gray-800">${escapeHtml(label.name)}</p>
      </div>
      <button data-id="${label.labelId}" 
              class="text-sm font-medium ${isAssigned ? 'text-red-500 hover:text-red-700' : 'text-blue-600 hover:text-blue-800'}">
        ${isAssigned ? 'Remove' : 'Add'}
      </button>
    `;
    container.appendChild(div);
  });

  container.querySelectorAll("button[data-id]").forEach(btn => {
    btn.addEventListener("click", async e => {
      const id = e.target.dataset.id;
      const isAssigned = assignedLabels.some(l => l.labelId == id);
      if (isAssigned) await unassignLabel(id);
      else await assignLabel(id);
    });
  });
}

/* ---------------------- ➕ GÁN NHÃN ---------------------- */
async function assignLabel(labelId) {
  if (!currentTask) return;
  try {
    const res = await apiFetch(`/api/tasks/${currentTask.taskId}/labels`, {
      method: "POST",
      body: JSON.stringify({ labelId })
    });
    if (!res.ok) throw new Error();
    const label = allLabels.find(l => l.labelId == labelId);
    assignedLabels.push(label);
    showToast(`🏷️ Đã thêm nhãn ${label.name}.`);
    renderLabelList(allLabels, assignedLabels);
  } catch (err) {
    console.error("❌ assignLabel failed:", err);
    showToast("Không thể gán nhãn.", "error");
  }
}

/* ---------------------- ➖ GỠ NHÃN ---------------------- */
async function unassignLabel(labelId) {
  if (!currentTask) return;
  try {
    const res = await apiFetch(`/api/tasks/${currentTask.taskId}/labels/${labelId}`, {
      method: "DELETE"
    });
    if (!res.ok) throw new Error();
    assignedLabels = assignedLabels.filter(l => l.labelId != labelId);
    showToast("🗑️ Đã gỡ nhãn khỏi task.");
    renderLabelList(allLabels, assignedLabels);
  } catch (err) {
    console.error("❌ unassignLabel failed:", err);
    showToast("Không thể gỡ nhãn.", "error");
  }
}

/* ---------------------- 🆕 TẠO NHÃN MỚI ---------------------- */
const createBtn = document.getElementById("create-label-btn");
if (createBtn) createBtn.addEventListener("click", openCreateLabelPopup);

function openCreateLabelPopup() {
  const popup = document.getElementById("create-label-popup");
  if (!popup) return;
  popup.classList.remove("hidden");
  setupColorGrid();
}

document.getElementById("close-create-label-btn")?.addEventListener("click", () => {
  document.getElementById("create-label-popup")?.classList.add("hidden");
});

async function createLabel() {
  const name = document.getElementById("new-label-name")?.value.trim();
  const color = document.getElementById("label-preview")?.style.backgroundColor || null;

  if (!name) {
    showToast("⚠️ Vui lòng nhập tên nhãn.", "warning");
    return;
  }

  try {
    const res = await apiFetch(`/api/labels`, {
      method: "POST",
      body: JSON.stringify({ name, color })
    });
    if (!res.ok) throw new Error();
    const newLabel = await res.json();
    allLabels.push(newLabel);
    showToast(`✅ Đã tạo nhãn "${newLabel.name}".`);
    document.getElementById("create-label-popup")?.classList.add("hidden");
    renderLabelList(allLabels, assignedLabels);
  } catch (err) {
    console.error("❌ createLabel failed:", err);
    showToast("Không thể tạo nhãn mới.", "error");
  }
}

document.getElementById("create-label-confirm")?.addEventListener("click", createLabel);

/* ---------------------- 🎨 GRID CHỌN MÀU ---------------------- */
function setupColorGrid() {
  const grid = document.getElementById("color-grid");
  const preview = document.getElementById("label-preview");
  if (!grid || !preview) return;

  const colors = ["#f59e0b","#10b981","#3b82f6","#8b5cf6","#ec4899","#ef4444","#14b8a6","#f97316","#6366f1","#84cc16"];
  grid.innerHTML = "";
  colors.forEach(c => {
    const box = document.createElement("div");
    box.className = "w-6 h-6 rounded cursor-pointer border border-gray-200 hover:scale-105 transition";
    box.style.background = c;
    box.addEventListener("click", () => {
      preview.style.background = c;
      preview.textContent = document.getElementById("new-label-name").value || "New Label";
    });
    grid.appendChild(box);
  });

  document.getElementById("remove-color-btn")?.addEventListener("click", () => {
    preview.style.background = "";
  });
}
