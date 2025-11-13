// ============================================================
// 🏷️ LABELS MODULE – Manage Project Labels for Tasks
// ============================================================
import { showToast, safeStop, escapeHtml as escapeHtmlUtil } from "./utils.js"; // ✅ thêm safeStop

const DEFAULT_COLOR = "#94A3B8";
const COLOR_PALETTE = [
  "#4BCE97",
  "#1F845A",
  "#F5CD47",
  "#FAA53D",
  "#F87462",
  "#F38BFF",
  "#579DFF",
  "#6CC3FF",
  "#94F3E4",
  "#B3F5FF",
  "#FECDCA",
  "#FFAB00",
  "#F15BB5",
  "#AB63E7",
  "#6E5DC6",
  "#0C66E4",
  "#1D4ED8",
  "#0EA5E9",
  "#22D3EE",
  "#A855F7",
  "#6366F1",
  "#10B981",
  "#059669",
  "#84CC16",
  "#F97316",
  "#EF4444",
  "#EC4899",
  "#FFB4A2",
  "#94A3B8",
  "#64748B",
];

let selectedColor = null;
let selectedEditColor = null; // ✅ thêm biến cho edit preview
let currentEditingLabelId = null;
let debounceLabelTimer;
let lastLabelKeyword = "";
let savedLabelsPopupPosition = null; // Lưu vị trí labels popup

function stopEvent(e) {
  if (!e) return;
  if (typeof e.preventDefault === "function") e.preventDefault();
  if (typeof e.stopPropagation === "function") e.stopPropagation();
}

// ================= INIT EVENTS =================
export function initLabelEvents() {
  const labelsPopup = document.getElementById("labels-popup");
  const openLabelsBtn = document.getElementById("open-labels-btn");
  const closeLabelsBtn = document.getElementById("close-labels-btn");
  const searchLabelInput = document.getElementById("search-label-input");

  const createLabelPopup = document.getElementById("create-label-popup");
  const createLabelBtn = document.getElementById("create-label-btn");
  const closeCreateLabelBtn = document.getElementById("close-create-label-btn");
  const createLabelBack = document.getElementById("create-label-back");
  const createLabelConfirm = document.getElementById("create-label-confirm");
  const colorGrid = document.getElementById("color-grid");
  const newLabelName = document.getElementById("new-label-name");
  const removeColorBtn = document.getElementById("remove-color-btn");

  const editLabelBack = document.getElementById("edit-label-back");
  const editLabelName = document.getElementById("edit-label-name");

  // 🟢 Mở popup labels
  openLabelsBtn?.addEventListener("click", openLabelsPopup);

  // ❌ Đóng popup labels
  closeLabelsBtn?.addEventListener("click", (e) => {
    stopEvent(e);
    safeStop(e);
    labelsPopup?.classList.add("hidden");
    lastLabelKeyword = "";
  });

  // 🔍 Tìm kiếm nhãn (debounce)
  searchLabelInput?.addEventListener("input", (e) => {
    clearTimeout(debounceLabelTimer);
    debounceLabelTimer = setTimeout(() => {
      loadLabels(e.target.value.trim());
    }, 350);
  });

  // ➕ Mở popup tạo nhãn
  createLabelBtn?.addEventListener("click", (e) => {
    stopEvent(e);
    safeStop(e);
    if (!createLabelPopup) return;

    const rect = e.currentTarget.getBoundingClientRect();
    const labelsPopupRect = labelsPopup?.getBoundingClientRect();
    createLabelPopup.style.position = "fixed";

    // Lưu vị trí labels popup trước khi ẩn
    if (labelsPopupRect) {
      savedLabelsPopupPosition = {
        top: labelsPopupRect.top + window.scrollY,
        left: labelsPopupRect.left + window.scrollX,
      };
    }

    // Đặt popup lên trên labels popup
    if (savedLabelsPopupPosition) {
      createLabelPopup.style.top = `${savedLabelsPopupPosition.top}px`;
      createLabelPopup.style.left = `${savedLabelsPopupPosition.left}px`;
    } else {
      // Fallback nếu không có labels popup
      createLabelPopup.style.top = `${
        rect.top + window.scrollY - createLabelPopup.offsetHeight - 8
      }px`;
      createLabelPopup.style.left = `${Math.min(
        rect.left + window.scrollX,
        window.innerWidth - createLabelPopup.offsetWidth - 24
      )}px`;
    }

    selectedColor = COLOR_PALETTE[0];
    newLabelName.value = "";
    updateLabelPreview();
    renderColorGrid(colorGrid, { selected: selectedColor, mode: "create" });

    createLabelPopup.classList.remove("hidden");
    labelsPopup?.classList.add("hidden");
    newLabelName?.focus();
  });

  // ❌ Đóng popup tạo nhãn
  const handleCloseCreate = (e) => {
    stopEvent(e);
    safeStop(e);
    closeCreateLabelPopup();
  };

  closeCreateLabelBtn?.addEventListener("click", handleCloseCreate);
  createLabelBack?.addEventListener("click", handleCloseCreate);

  removeColorBtn?.addEventListener("click", (e) => {
    stopEvent(e);
    safeStop(e);
    selectedColor = DEFAULT_COLOR;
    renderColorGrid(colorGrid, { selected: selectedColor, mode: "create" });
    updateLabelPreview();
  });

  newLabelName?.addEventListener("input", updateLabelPreview);
  createLabelConfirm?.addEventListener("click", handleCreateLabel);

  editLabelName?.addEventListener("input", updateEditLabelPreview);
  editLabelBack?.addEventListener("click", (e) => closeEditLabelPopup(e, true));
}

// ================= OPEN POPUP =================
export function openLabelsPopup(e) {
  stopEvent(e);
  safeStop(e);

  const labelsPopup = document.getElementById("labels-popup");
  if (!labelsPopup) return console.error("❌ labelsPopup not found in DOM");
  const createLabelPopup = document.getElementById("create-label-popup");
  const editLabelPopupWrapper = document.getElementById(
    "edit-label-popup-wrapper"
  );

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
  createLabelPopup?.classList.add("hidden");
  editLabelPopupWrapper?.classList.add("hidden");

  const searchInput = document.getElementById("search-label-input");
  if (searchInput) {
    lastLabelKeyword = "";
    searchInput.value = "";
    searchInput.focus();
  }

  console.log(`📍 Labels popup opened at top=${top}, left=${left}`);
  loadLabels();
}

// ================= LOAD LABELS =================
export async function loadLabels(keyword = lastLabelKeyword) {
  const projectId = window.PROJECT_ID;
  const taskId = window.CURRENT_TASK_ID;
  const listEl = document.getElementById("labels-list");
  if (!taskId || !listEl) return;

  lastLabelKeyword = keyword ?? "";

  listEl.innerHTML = `<p class="text-gray-400 text-sm italic">Loading...</p>`;

  try {
    const headers = getAuthHeaders();

    const [resAll, resTask] = await Promise.all([
      fetch(
        `/api/labels?projectId=${projectId}&keyword=${encodeURIComponent(
          lastLabelKeyword
        )}`,
        { headers }
      ),
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
      ? allLabels
          .map((l) => renderLabelRow(l, assignedIds.has(l.labelId)))
          .join("")
      : `<p class="text-gray-400 text-sm italic">No labels found</p>`;
  } catch (err) {
    console.error(" loadLabels error:", err);
    listEl.innerHTML = `<p class="text-red-500 text-sm">Failed to load labels</p>`;
  }
}

// ================= RENDER ROW =================
function renderLabelRow(label, isAssigned) {
  const taskId = window.CURRENT_TASK_ID;
  const color = label.color || DEFAULT_COLOR;
  const displayName = escapeHtmlUtil(label.name || "Unnamed");
  return `
    <label class="label-option" style="--label-color:${color}">
      <input type="checkbox" ${isAssigned ? "checked" : ""}
        onchange="handleLabelChange(${taskId},${label.labelId},this.checked)">
      <span class="label-option__content" role="presentation">
        <span class="label-option__left">
          <span class="label-option__swatch" style="background:${color};"></span>
          <span class="label-option__name" title="${displayName}">${displayName}</span>
        </span>
        <span class="label-option__actions">
          <button
            type="button"
            class="label-option__edit"
            aria-label="Edit label ${displayName}"
            onclick="openEditLabel(${label.labelId}, event)"
          >
            ✎
          </button>
        </span>
      </span>
    </label>`;
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
      headers: getAuthHeaders(),
    });
    if (!res.ok) throw new Error(`Failed to assign label (${res.status})`);
    await loadLabels();
  } catch (err) {
    console.error(" assignLabel error:", err);
    showToast(" Failed to assign label", "error");
  }
}

export async function unassignLabel(taskId, labelId) {
  try {
    const res = await fetch(`/api/tasks/${taskId}/labels/${labelId}`, {
      method: "DELETE",
      headers: getAuthHeaders(),
    });
    if (res.ok || res.status === 204) {
      await loadLabels();
      return;
    }
    throw new Error(`Failed to unassign label: ${res.status}`);
  } catch (err) {
    console.error(" unassignLabel error:", err);
    showToast(" Failed to remove label", "error");
  }
}

async function handleCreateLabel() {
  const name = document.getElementById("new-label-name").value.trim();
  if (!name) return showToast("⚠️ Please enter label name", "error");

  const body = new URLSearchParams({
    projectId: window.PROJECT_ID,
    name,
    color: selectedColor || DEFAULT_COLOR,
  });

  try {
    const res = await fetch(`/api/labels?${body.toString()}`, {
      method: "POST",
      headers: getAuthHeaders(),
    });
    if (!res.ok) throw new Error("Create failed");
    closeCreateLabelPopup();
    await loadLabels();
  } catch (err) {
    console.error(" createLabel error:", err);
    showToast(" Failed to create label", "error");
  }
}

// ================= COLOR GRID + PREVIEW =================
function closeCreateLabelPopup() {
  const createLabelPopup = document.getElementById("create-label-popup");
  const labelsPopup = document.getElementById("labels-popup");
  createLabelPopup?.classList.add("hidden");
  labelsPopup?.classList.remove("hidden");
}

function renderColorGrid(gridEl, { selected = null, mode = "create" } = {}) {
  if (!gridEl) return;

  gridEl.innerHTML = COLOR_PALETTE.map(
    (color) => `
      <button
        type="button"
        class="color-option ${selected === color ? "selected" : ""}"
        style="background:${color}"
        data-color="${color}"
        data-mode="${mode}"
        aria-label="Select color ${color}"
      ></button>`
  ).join("");

  gridEl.querySelectorAll(".color-option").forEach((btn) => {
    btn.addEventListener("click", () => {
      const chosen = btn.dataset.color;
      if (mode === "edit") {
        selectedEditColor = chosen;
        renderColorGrid(gridEl, { selected: selectedEditColor, mode });
        updateEditLabelPreview();
      } else {
        selectedColor = chosen;
        renderColorGrid(gridEl, { selected: selectedColor, mode });
        updateLabelPreview();
      }
    });
  });
}

function updateLabelPreview() {
  const preview = document.getElementById("label-preview");
  if (!preview) return;
  const name =
    document.getElementById("new-label-name")?.value.trim() || "New Label";
  const color = selectedColor || DEFAULT_COLOR;
  preview.textContent = name;
  applyPreviewStyles(preview, color);
}

function updateEditLabelPreview() {
  const preview = document.getElementById("edit-label-preview");
  if (!preview) return;
  const name =
    document.getElementById("edit-label-name")?.value.trim() || "New";
  const color = selectedEditColor || DEFAULT_COLOR;
  preview.textContent = name;
  applyPreviewStyles(preview, color);
}

function applyPreviewStyles(el, color) {
  const textColor = getReadableTextColor(color);
  el.style.background = color;
  el.style.color = textColor;
  el.style.borderColor = color;
}

function getReadableTextColor(hexColor = DEFAULT_COLOR) {
  const { r, g, b } = hexToRgb(hexColor);
  // Luminosity formula
  const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
  return luminance > 0.6 ? "#172B4D" : "#FFFFFF";
}

function hexToRgb(hex) {
  const sanitized = hex?.replace("#", "") ?? "000000";
  const bigint = parseInt(sanitized, 16);
  if (Number.isNaN(bigint)) return { r: 0, g: 0, b: 0 };
  return {
    r: (bigint >> 16) & 255,
    g: (bigint >> 8) & 255,
    b: bigint & 255,
  };
}

async function openEditLabel(labelId, e) {
  stopEvent(e);
  safeStop(e);

  const popupWrapper = document.getElementById("edit-label-popup-wrapper");
  const editPopup = document.getElementById("edit-label-popup");
  const labelsPopup = document.getElementById("labels-popup");
  const editGrid = document.getElementById("edit-color-grid");
  const editNameInput = document.getElementById("edit-label-name");

  if (!popupWrapper || !editPopup || !editGrid || !editNameInput) return;

  try {
    const res = await fetch(`/api/labels/${labelId}`, {
      headers: getAuthHeaders(),
    });
    if (!res.ok) throw new Error(`Failed to fetch label ${labelId}`);

    const label = await res.json();
    currentEditingLabelId = labelId;
    selectedEditColor = label.color || DEFAULT_COLOR;
    editNameInput.value = label.name || "";

    renderColorGrid(editGrid, {
      selected: selectedEditColor,
      mode: "edit",
    });
    updateEditLabelPreview();

    // Lưu vị trí labels popup trước khi ẩn
    const labelsPopupRect = labelsPopup?.getBoundingClientRect();
    if (labelsPopupRect) {
      savedLabelsPopupPosition = {
        top: labelsPopupRect.top + window.scrollY,
        left: labelsPopupRect.left + window.scrollX,
      };
    }

    // Đặt vị trí edit popup lên trên labels popup
    popupWrapper.style.position = "fixed";
    popupWrapper.style.inset = "0";
    editPopup.style.position = "fixed";
    if (savedLabelsPopupPosition) {
      editPopup.style.top = `${savedLabelsPopupPosition.top}px`;
      editPopup.style.left = `${savedLabelsPopupPosition.left}px`;
      editPopup.style.transform = "none";
    } else {
      // Fallback: center màn hình
      editPopup.style.top = "50%";
      editPopup.style.left = "50%";
      editPopup.style.transform = "translate(-50%, -50%)";
    }

    labelsPopup?.classList.add("hidden");
    popupWrapper.classList.remove("hidden");
    editNameInput.focus();
  } catch (err) {
    console.error(" openEditLabel error:", err);
    showToast(" Failed to open label", "error");
  }
}

function closeEditLabelPopup(e, reopenList = true) {
  stopEvent(e);
  safeStop(e);
  const popupWrapper = document.getElementById("edit-label-popup-wrapper");
  const labelsPopup = document.getElementById("labels-popup");
  const editGrid = document.getElementById("edit-color-grid");

  popupWrapper?.classList.add("hidden");
  if (reopenList) labelsPopup?.classList.remove("hidden");

  currentEditingLabelId = null;
  selectedEditColor = DEFAULT_COLOR;
  renderColorGrid(editGrid, { selected: selectedEditColor, mode: "edit" });
  updateEditLabelPreview();
}

function removeEditColor() {
  const editGrid = document.getElementById("edit-color-grid");
  selectedEditColor = DEFAULT_COLOR;
  renderColorGrid(editGrid, { selected: selectedEditColor, mode: "edit" });
  updateEditLabelPreview();
}

async function saveEditedLabel() {
  if (!currentEditingLabelId)
    return showToast(" Please select a label to edit", "error");

  const name = document.getElementById("edit-label-name")?.value.trim();
  if (!name) return showToast(" Please enter label name", "error");

  const payload = {
    name,
    color: selectedEditColor || DEFAULT_COLOR,
  };

  try {
    const res = await fetch(`/api/labels/${currentEditingLabelId}`, {
      method: "PUT",
      headers: getAuthHeaders({ "Content-Type": "application/json" }),
      body: JSON.stringify(payload),
    });

    if (!res.ok) throw new Error(`Update failed (${res.status})`);
    closeEditLabelPopup(null);
    await loadLabels();
  } catch (err) {
    console.error(" saveEditedLabel error:", err);
    showToast(" Failed to update label", "error");
  }
}

async function deleteEditedLabel() {
  if (!currentEditingLabelId)
    return showToast(" Please select a label to delete", "error");

  const confirmed = window.confirm(
    "Are you sure you want to delete this label?"
  );
  if (!confirmed) return;

  try {
    const res = await fetch(`/api/labels/${currentEditingLabelId}`, {
      method: "DELETE",
      headers: getAuthHeaders(),
    });

    if (!res.ok) throw new Error(`Delete failed (${res.status})`);
    closeEditLabelPopup(null);
    await loadLabels();
  } catch (err) {
    console.error(" deleteEditedLabel error:", err);
    showToast("Failed to delete label", "error");
  }
}

function getAuthHeaders(additional = {}) {
  return {
    Authorization: "Bearer " + localStorage.getItem("token"),
    ...additional,
  };
}

// ✅ Expose global cho context menu hoặc inline script gọi được
Object.assign(window, {
  openLabelsPopup,
  loadLabels,
  handleLabelChange,
  assignLabel,
  unassignLabel,
  openEditLabel,
  closeEditLabelPopup,
  removeEditColor,
  saveEditedLabel,
  deleteEditedLabel,
});
