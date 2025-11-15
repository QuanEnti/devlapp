// ============================================================
// 🏷️ LABELS MODULE – Manage Project Labels for Tasks
// ============================================================
import { showToast, safeStop, escapeHtml as escapeHtmlUtil, getToken } from "./utils.js"; // ✅ thêm safeStop
import { updateCardLabels } from "./main.js";

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
let currentEditingLabelMeta = null;
let debounceLabelTimer;
let lastLabelKeyword = "";
let savedLabelsPopupPosition = null; // Lưu vị trí labels popup

// ================== POPUP POSITIONING CONFIG ==================
// Cấu hình dễ chỉnh sửa cho tất cả label popups
const LABEL_POPUP_CONFIG = {
  offset: 8, // Khoảng cách từ button/trigger
  minMargin: 16, // Khoảng cách tối thiểu từ edge viewport
};

// Helper function để tính toán vị trí thông minh cho popup
function calculateSmartPosition(popupElement, triggerRect, fallbackPosition = null) {
  if (!popupElement) return { top: 0, left: 0 };

  // Kích thước popup (từ CSS hoặc ước tính)
  // Nếu popup bị ẩn, dùng giá trị ước tính dựa trên class
  let popupWidth = popupElement.offsetWidth;
  let popupHeight = popupElement.offsetHeight;
  
  if (!popupWidth || !popupHeight) {
    // Ước tính dựa trên class CSS
    if (popupElement.classList.contains("labels-popup")) {
      popupWidth = 320; // Từ CSS: width: 320px
      popupHeight = 400; // Ước tính
    } else if (popupElement.classList.contains("label-editor")) {
      popupWidth = 360; // Từ CSS: width: 360px
      popupHeight = 450; // Ước tính
    } else {
      popupWidth = 320;
      popupHeight = 400;
    }
  }

  const viewportWidth = window.innerWidth;
  const viewportHeight = window.innerHeight;
  const scrollX = window.scrollX || 0;
  const scrollY = window.scrollY || 0;

  let top, left;

  if (!triggerRect || triggerRect.width === 0 || triggerRect.height === 0) {
    // Không có trigger rect → dùng fallback hoặc tọa độ chuột
    if (fallbackPosition) {
      left = fallbackPosition.left;
      top = fallbackPosition.top;
    } else {
      const mouseX = window.contextMenuX || viewportWidth / 2;
      const mouseY = window.contextMenuY || viewportHeight / 2;
      left = mouseX + scrollX + LABEL_POPUP_CONFIG.offset;
      top = mouseY + scrollY + LABEL_POPUP_CONFIG.offset;
    }
  } else {
    // Có trigger rect → tính toán dựa trên vị trí button
    const buttonBottom = triggerRect.bottom + scrollY;
    const buttonLeft = triggerRect.left + scrollX;
    const buttonRight = triggerRect.right + scrollX;
    const buttonTop = triggerRect.top + scrollY;

    // Ưu tiên hiển thị bên dưới và bên phải button
    left = buttonLeft;
    top = buttonBottom + LABEL_POPUP_CONFIG.offset;

    // Kiểm tra không gian xung quanh
    const spaceBelow = viewportHeight + scrollY - buttonBottom;
    const spaceAbove = buttonTop - scrollY;
    const spaceRight = viewportWidth + scrollX - buttonLeft;
    const spaceLeft = buttonLeft - scrollX;

    // Nếu không đủ chỗ bên dưới, hiển thị bên trên
    if (spaceBelow < popupHeight + LABEL_POPUP_CONFIG.minMargin && spaceAbove > spaceBelow) {
      top = buttonTop - popupHeight - LABEL_POPUP_CONFIG.offset;
    }

    // Nếu không đủ chỗ bên phải, hiển thị bên trái
    if (spaceRight < popupWidth + LABEL_POPUP_CONFIG.minMargin && spaceLeft > spaceRight) {
      left = buttonRight - popupWidth;
    }
  }

  // Đảm bảo không vượt quá viewport
  left = Math.max(
    scrollX + LABEL_POPUP_CONFIG.minMargin,
    Math.min(left, viewportWidth + scrollX - popupWidth - LABEL_POPUP_CONFIG.minMargin)
  );
  top = Math.max(
    scrollY + LABEL_POPUP_CONFIG.minMargin,
    Math.min(top, viewportHeight + scrollY - popupHeight - LABEL_POPUP_CONFIG.minMargin)
  );

  return { top, left };
}

function parseNumericId(value) {
  const parsed = Number(value);
  return Number.isNaN(parsed) ? null : parsed;
}

function getCurrentUserId() {
  const stored = localStorage.getItem("currentUserId");
  if (!stored) return null;
  const parsed = Number(stored);
  return Number.isNaN(parsed) ? null : parsed;
}

function normalizeRole(role) {
  return (role || window.CURRENT_ROLE || "ROLE_MEMBER")
    .toString()
    .toUpperCase();
}

function hasManagerLabelPrivileges(role) {
  const normalized = normalizeRole(role);
  return normalized === "ROLE_PM" || normalized === "ROLE_ADMIN";
}

function isProjectMember(role) {
  const normalized = normalizeRole(role);
  return (
    normalized === "ROLE_PM" ||
    normalized === "ROLE_ADMIN" ||
    normalized === "ROLE_MEMBER"
  );
}

function isCurrentUserFollower() {
  const currentId = getCurrentUserId();
  if (currentId == null) return false;
  const followers = Array.isArray(window.CURRENT_TASK_FOLLOWER_IDS)
    ? window.CURRENT_TASK_FOLLOWER_IDS
    : [];
  return followers.some((id) => Number(id) === Number(currentId));
}

function getLabelCreatorId(label = {}) {
  const candidates = [
    label.createdById,
    label.createdBy?.userId,
    label.createdBy,
    label.ownerId,
    label.owner?.userId,
  ];
  for (const candidate of candidates) {
    if (candidate === undefined || candidate === null || candidate === "")
      continue;
    const parsed = parseNumericId(candidate);
    if (parsed !== null) return parsed;
  }
  return null;
}

function canCreateLabels() {
  return isProjectMember();
}

function canAssignLabels() {
  if (hasManagerLabelPrivileges()) return true;
  return isCurrentUserFollower();
}

function canEditLabel(label = {}) {
  if (hasManagerLabelPrivileges()) return true;
  if (typeof label.isCreator === "boolean") return label.isCreator;
  const currentId = getCurrentUserId();
  if (currentId == null) return false;
  const creatorId = getLabelCreatorId(label);
  if (creatorId == null) return false;
  return Number(currentId) === Number(creatorId);
}

async function extractErrorMessage(res) {
  try {
    const data = await res.clone().json();
    if (typeof data === "string") return data;
    return data?.message || data?.error || "";
  } catch (err) {
    try {
      return await res.text();
    } catch (textErr) {
      return "";
    }
  }
}

function stopEvent(e) {
  if (!e) return;
  if (typeof e.preventDefault === "function") e.preventDefault();
  if (typeof e.stopPropagation === "function") e.stopPropagation();
}

function decodeLabelName(value) {
  if (!value) return "";
  try {
    return decodeURIComponent(value);
  } catch (err) {
    return value;
  }
}

function getLabelMetadata(labelId) {
  const row = document.querySelector(
    `.label-option[data-label-id="${labelId}"]`
  );
  if (!row) return null;
  const name = decodeLabelName(row.dataset.labelName || "");
  const color = row.dataset.labelColor || "#94a3b8";
  return {
    labelId,
    name,
    color,
    creatorId:
      row.dataset.creatorId != null ? Number(row.dataset.creatorId) : null,
  };
}

function addInlineLabelChip(meta) {
  const container = document.getElementById("labels-display-inline");
  if (!container || !meta) return;
  const existing = container.querySelector(`[data-label-id="${meta.labelId}"]`);
  if (existing) return;
  const chip = document.createElement("span");
  chip.dataset.labelId = String(meta.labelId);
  chip.className =
    "inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-semibold text-white";
  chip.style.backgroundColor = meta.color || "#94a3b8";
  chip.textContent = meta.name || "";
  container.appendChild(chip);
}

function removeInlineLabelChip(labelId) {
  const container = document.getElementById("labels-display-inline");
  if (!container) return;
  const chip = container.querySelector(`[data-label-id="${labelId}"]`);
  if (chip) chip.remove();
}

function setLabelRowAssigned(labelId, assigned) {
  const row = document.querySelector(
    `.label-option[data-label-id="${labelId}"]`
  );
  if (!row) return;
  row.classList.toggle("label-option--active", Boolean(assigned));
  const checkbox = row.querySelector('input[type="checkbox"]');
  if (checkbox) checkbox.checked = Boolean(assigned);
}

function updateLabelRowMetadata(label) {
  if (!label || label.labelId == null) return;
  const row = document.querySelector(
    `.label-option[data-label-id="${label.labelId}"]`
  );
  if (!row) return;

  const encodedName = encodeURIComponent(label.name || "Unnamed");
  row.dataset.labelName = encodedName;
  row.dataset.labelColor = label.color || "#94a3b8";
  row.dataset.creatorId =
    label.createdById != null ? String(label.createdById) : "";

  const nameEl = row.querySelector(".label-option__name");
  if (nameEl) nameEl.textContent = label.name || "";

  const swatch = row.querySelector(".label-option__swatch");
  if (swatch) swatch.style.background = label.color || DEFAULT_COLOR;
}

function updateInlineLabelChip(meta) {
  if (!meta || meta.labelId == null) return;
  const container = document.getElementById("labels-display-inline");
  if (!container) return;
  const chip = container.querySelector(`[data-label-id="${meta.labelId}"]`);
  if (!chip) return;
  chip.textContent = meta.name || "";
  chip.style.backgroundColor = meta.color || "#94a3b8";
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

  if (!canCreateLabels()) {
    createLabelBtn?.classList.add("hidden");
  }

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
    if (!canCreateLabels()) {
      showToast(
        " You do not have permission to create labels in this project.",
        "error"
      );
      return;
    }
    if (!createLabelPopup) return;

    const triggerRect = e.currentTarget.getBoundingClientRect();
    const labelsPopupRect = labelsPopup?.getBoundingClientRect();
    createLabelPopup.style.position = "fixed";

    // Lưu vị trí labels popup trước khi ẩn (để có thể quay lại sau)
    if (labelsPopupRect) {
      savedLabelsPopupPosition = {
        top: labelsPopupRect.top + window.scrollY,
        left: labelsPopupRect.left + window.scrollX,
      };
    }

    // Tính toán vị trí thông minh - ưu tiên dùng vị trí labels popup nếu có
    const fallbackPosition = savedLabelsPopupPosition || null;
    const position = calculateSmartPosition(createLabelPopup, triggerRect, fallbackPosition);
    createLabelPopup.style.top = `${position.top}px`;
    createLabelPopup.style.left = `${position.left}px`;

    // Điều chỉnh lại sau khi render
    requestAnimationFrame(() => {
      const actualRect = createLabelPopup.getBoundingClientRect();
      const actualWidth = actualRect.width;
      const actualHeight = actualRect.height;
      const currentTop = parseFloat(createLabelPopup.style.top) || position.top;
      const currentLeft = parseFloat(createLabelPopup.style.left) || position.left;

      const viewportWidth = window.innerWidth;
      const viewportHeight = window.innerHeight;
      const scrollX = window.scrollX || 0;
      const scrollY = window.scrollY || 0;

      let adjustedLeft = currentLeft;
      let adjustedTop = currentTop;

      if (currentLeft + actualWidth > viewportWidth + scrollX - LABEL_POPUP_CONFIG.minMargin) {
        adjustedLeft = viewportWidth + scrollX - actualWidth - LABEL_POPUP_CONFIG.minMargin;
      }
      if (currentTop + actualHeight > viewportHeight + scrollY - LABEL_POPUP_CONFIG.minMargin) {
        adjustedTop = viewportHeight + scrollY - actualHeight - LABEL_POPUP_CONFIG.minMargin;
      }

      adjustedLeft = Math.max(scrollX + LABEL_POPUP_CONFIG.minMargin, adjustedLeft);
      adjustedTop = Math.max(scrollY + LABEL_POPUP_CONFIG.minMargin, adjustedTop);

      if (adjustedLeft !== currentLeft || adjustedTop !== currentTop) {
        createLabelPopup.style.left = `${adjustedLeft}px`;
        createLabelPopup.style.top = `${adjustedTop}px`;
      }
    });

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

  document.addEventListener("mousedown", (event) => {
    const labelsPopupEl = document.getElementById("labels-popup");
    const createPopupEl = document.getElementById("create-label-popup");
    const editWrapperEl = document.getElementById("edit-label-popup-wrapper");

    const clickedInsidePopup =
      labelsPopupEl?.contains(event.target) ||
      createPopupEl?.contains(event.target) ||
      editWrapperEl?.contains(event.target);

    const triggeredFromControls =
      event.target.closest("#open-labels-btn") ||
      event.target.closest("#open-labels-btn-inline") ||
      event.target.closest("#card-context-menu") ||
      event.target.closest(".label-option") ||
      event.target.closest(".color-option") ||
      event.target.closest("#create-label-btn") ||
      event.target.closest("#labels-popup");

    if (!clickedInsidePopup && !triggeredFromControls) {
      labelsPopupEl?.classList.add("hidden");
      createPopupEl?.classList.add("hidden");
      editWrapperEl?.classList.add("hidden");
    }
  });
}

// ================= OPEN POPUP =================
export function openLabelsPopup(e) {
  stopEvent(e);
  safeStop(e);

  const labelsPopup = document.getElementById("labels-popup");
  if (!labelsPopup) return console.error(" labelsPopup not found in DOM");
  const createLabelPopup = document.getElementById("create-label-popup");
  const editLabelPopupWrapper = document.getElementById(
    "edit-label-popup-wrapper"
  );
  const createLabelBtn = document.getElementById("create-label-btn");
  if (createLabelBtn) {
    const canCreate = canCreateLabels();
    createLabelBtn.classList.toggle("hidden", !canCreate);
    createLabelBtn.disabled = !canCreate;
  }

  // Lấy trigger rect
  let triggerRect = e?.currentTarget?.getBoundingClientRect?.() ?? null;
  const isValidRect = triggerRect && triggerRect.width > 0 && triggerRect.height > 0;
  if (!isValidRect) triggerRect = null;

  // Tính toán vị trí thông minh
  labelsPopup.style.position = "fixed";
  const position = calculateSmartPosition(labelsPopup, triggerRect);
  labelsPopup.style.top = `${position.top}px`;
  labelsPopup.style.left = `${position.left}px`;
  labelsPopup.classList.remove("hidden");
  createLabelPopup?.classList.add("hidden");
  editLabelPopupWrapper?.classList.add("hidden");

  // Điều chỉnh lại sau khi render để lấy kích thước thực tế
  requestAnimationFrame(() => {
    const actualRect = labelsPopup.getBoundingClientRect();
    const actualWidth = actualRect.width;
    const actualHeight = actualRect.height;
    const currentTop = parseFloat(labelsPopup.style.top) || position.top;
    const currentLeft = parseFloat(labelsPopup.style.left) || position.left;

    // Kiểm tra lại và điều chỉnh nếu cần
    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;
    const scrollX = window.scrollX || 0;
    const scrollY = window.scrollY || 0;

    let adjustedLeft = currentLeft;
    let adjustedTop = currentTop;

    if (currentLeft + actualWidth > viewportWidth + scrollX - LABEL_POPUP_CONFIG.minMargin) {
      adjustedLeft = viewportWidth + scrollX - actualWidth - LABEL_POPUP_CONFIG.minMargin;
    }
    if (currentTop + actualHeight > viewportHeight + scrollY - LABEL_POPUP_CONFIG.minMargin) {
      adjustedTop = viewportHeight + scrollY - actualHeight - LABEL_POPUP_CONFIG.minMargin;
    }

    adjustedLeft = Math.max(scrollX + LABEL_POPUP_CONFIG.minMargin, adjustedLeft);
    adjustedTop = Math.max(scrollY + LABEL_POPUP_CONFIG.minMargin, adjustedTop);

    if (adjustedLeft !== currentLeft || adjustedTop !== currentTop) {
      labelsPopup.style.left = `${adjustedLeft}px`;
      labelsPopup.style.top = `${adjustedTop}px`;
    }
  });

  const searchInput = document.getElementById("search-label-input");
  if (searchInput) {
    lastLabelKeyword = "";
    searchInput.value = "";
    searchInput.focus();
  }

  console.log(`✅ Labels popup opened at top=${position.top}, left=${position.left}`);
  loadLabels();
}

// ================= LOAD LABELS =================
export async function loadLabels(keyword = lastLabelKeyword) {
  const projectId = window.PROJECT_ID;
  const taskId = window.CURRENT_TASK_ID;
  const listEl = document.getElementById("labels-list");
  if (!taskId || !listEl) return;

  lastLabelKeyword = keyword ?? "";

  const canAssign = canAssignLabels();
  const permissionHint = document.getElementById("labels-permission-hint");
  if (permissionHint) {
    if (canAssign) {
      permissionHint.textContent = "";
      permissionHint.classList.add("hidden");
    } else {
      permissionHint.textContent =
        "Only project managers or members assigned to this task can change labels.";
      permissionHint.classList.remove("hidden");
    }
  }
  try {
    listEl.dataset.readonly = canAssign ? "false" : "true";
  } catch (err) {
    // ignore dataset errors on older browsers
  }

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
  const assignable = canAssignLabels();
  const canEditThisLabel = canEditLabel(label);
  const labelClass = `label-option${
    assignable ? "" : " label-option--readonly"
  }`;
  const contentClass = `label-option__content${
    assignable ? "" : " label-option__content--readonly"
  }`;
  const checkboxAttrs = assignable ? "" : ' disabled aria-disabled="true"';
  const actionsHtml = canEditThisLabel
    ? `<span class="label-option__actions">
          <button
            type="button"
            class="label-option__edit"
            aria-label="Edit label ${displayName}"
            onclick="openEditLabel(${label.labelId}, event)"
          >
            ✎
          </button>
        </span>`
    : "";
  const rawName = label.name || "Unnamed";
  const encodedName = encodeURIComponent(rawName);
  return `
    <label class="${labelClass}${
    isAssigned ? " label-option--active" : ""
  }" style="--label-color:${color}" data-label-id="${
    label.labelId
  }" data-label-name="${encodedName}" data-label-color="${color}" data-creator-id="${
    label.createdById ?? ""
  }">
      <input type="checkbox" data-label-id="${label.labelId}" ${
    isAssigned ? "checked" : ""
  }${checkboxAttrs}
        onchange="handleLabelChange(${taskId},${label.labelId},this.checked)">
      <span class="${contentClass}" role="presentation">
        <span class="label-option__left">
          <span class="label-option__swatch" style="background:${color};"></span>
          <span class="label-option__name" title="${displayName}">${displayName}</span>
        </span>
        ${actionsHtml}
      </span>
    </label>`;
}

async function handleLabelChange(taskId, labelId, isChecked) {
  if (!canAssignLabels()) {
    showToast(
      " You do not have permission to change labels on this task.",
      "error"
    );
    const checkbox = document.querySelector(
      `.label-option input[data-label-id="${labelId}"]`
    );
    if (checkbox) checkbox.checked = !isChecked;
    return;
  }

  const success = isChecked
    ? await assignLabel(taskId, labelId)
    : await unassignLabel(taskId, labelId);

  if (!success) {
    const checkbox = document.querySelector(
      `.label-option input[data-label-id="${labelId}"]`
    );
    if (checkbox) checkbox.checked = !isChecked;
  }
  if (success) {
    setLabelRowAssigned(labelId, isChecked);
  }
}

// ================= ASSIGN / UNASSIGN =================
export async function assignLabel(taskId, labelId) {
  if (!canAssignLabels()) {
    showToast(
      " You do not have permission to assign labels on this task.",
      "error"
    );
    return false;
  }
  try {
    const res = await fetch(`/api/tasks/${taskId}/labels/${labelId}`, {
      method: "POST",
      headers: getAuthHeaders(),
    });
    if (!res.ok) {
      if (res.status === 403) {
        const message = await extractErrorMessage(res);
        showToast(
          message ||
            " You do not have permission to assign labels on this task.",
          "error"
        );
        return false;
      }
      throw new Error(`Failed to assign label (${res.status})`);
    }
    const meta = getLabelMetadata(labelId);
    addInlineLabelChip(meta);

    // Cập nhật card bên ngoài
    try {
      const res = await fetch(`/api/tasks/${taskId}`, {
        headers: getAuthHeaders(),
      });
      if (res.ok) {
        const updatedTask = await res.json();
        if (updatedTask && updatedTask.labels) {
          updateCardLabels(taskId, updatedTask.labels);
        }
      }
    } catch (err) {
      console.error("Error reloading task for card update:", err);
    }

    return true;
  } catch (err) {
    console.error(" assignLabel error:", err);
    showToast(" Failed to assign label", "error");
    return false;
  }
}

export async function unassignLabel(taskId, labelId) {
  if (!canAssignLabels()) {
    showToast(
      " You do not have permission to remove labels from this task.",
      "error"
    );
    return false;
  }
  try {
    const res = await fetch(`/api/tasks/${taskId}/labels/${labelId}`, {
      method: "DELETE",
      headers: getAuthHeaders(),
    });
    if (res.ok || res.status === 204) {
      removeInlineLabelChip(labelId);

      // Cập nhật card bên ngoài
      try {
        const reloadRes = await fetch(`/api/tasks/${taskId}`, {
          headers: getAuthHeaders(),
        });
        if (reloadRes.ok) {
          const updatedTask = await reloadRes.json();
          updateCardLabels(taskId, updatedTask.labels || []);
        }
      } catch (err) {
        console.error("Error reloading task for card update:", err);
      }

      return true;
    }
    if (res.status === 403) {
      const message = await extractErrorMessage(res);
      showToast(
        message ||
          " You do not have permission to remove labels from this task.",
        "error"
      );
      return false;
    }
    throw new Error(`Failed to unassign label: ${res.status}`);
  } catch (err) {
    console.error(" unassignLabel error:", err);
    showToast(" Failed to remove label", "error");
    return false;
  }
}

async function handleCreateLabel() {
  if (!canCreateLabels()) {
    showToast(
      " You do not have permission to create labels in this project.",
      "error"
    );
    return;
  }
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
    if (!canEditLabel(label)) {
      showToast(
        " Only project managers or label creators can edit this label.",
        "error"
      );
      return;
    }
    currentEditingLabelId = labelId;
    currentEditingLabelMeta = label;
    selectedEditColor = label.color || DEFAULT_COLOR;
    editNameInput.value = label.name || "";

    renderColorGrid(editGrid, {
      selected: selectedEditColor,
      mode: "edit",
    });
    updateEditLabelPreview();

    // Lưu vị trí labels popup trước khi ẩn (để có thể quay lại sau)
    const labelsPopupRect = labelsPopup?.getBoundingClientRect();
    if (labelsPopupRect) {
      savedLabelsPopupPosition = {
        top: labelsPopupRect.top + window.scrollY,
        left: labelsPopupRect.left + window.scrollX,
      };
    }

    // Đặt vị trí edit popup
    popupWrapper.style.position = "fixed";
    popupWrapper.style.inset = "0";
    editPopup.style.position = "fixed";

    // Tính toán vị trí thông minh - ưu tiên dùng vị trí labels popup nếu có
    const fallbackPosition = savedLabelsPopupPosition || null;
    const triggerRect = e?.currentTarget?.getBoundingClientRect?.() ?? null;
    const position = calculateSmartPosition(editPopup, triggerRect, fallbackPosition);
    
    editPopup.style.top = `${position.top}px`;
    editPopup.style.left = `${position.left}px`;
    editPopup.style.transform = "none";

    // Điều chỉnh lại sau khi render
    requestAnimationFrame(() => {
      const actualRect = editPopup.getBoundingClientRect();
      const actualWidth = actualRect.width;
      const actualHeight = actualRect.height;
      const currentTop = parseFloat(editPopup.style.top) || position.top;
      const currentLeft = parseFloat(editPopup.style.left) || position.left;

      const viewportWidth = window.innerWidth;
      const viewportHeight = window.innerHeight;
      const scrollX = window.scrollX || 0;
      const scrollY = window.scrollY || 0;

      let adjustedLeft = currentLeft;
      let adjustedTop = currentTop;

      if (currentLeft + actualWidth > viewportWidth + scrollX - LABEL_POPUP_CONFIG.minMargin) {
        adjustedLeft = viewportWidth + scrollX - actualWidth - LABEL_POPUP_CONFIG.minMargin;
      }
      if (currentTop + actualHeight > viewportHeight + scrollY - LABEL_POPUP_CONFIG.minMargin) {
        adjustedTop = viewportHeight + scrollY - actualHeight - LABEL_POPUP_CONFIG.minMargin;
      }

      adjustedLeft = Math.max(scrollX + LABEL_POPUP_CONFIG.minMargin, adjustedLeft);
      adjustedTop = Math.max(scrollY + LABEL_POPUP_CONFIG.minMargin, adjustedTop);

      if (adjustedLeft !== currentLeft || adjustedTop !== currentTop) {
        editPopup.style.left = `${adjustedLeft}px`;
        editPopup.style.top = `${adjustedTop}px`;
      }
    });

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
  currentEditingLabelMeta = null;
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
  if (!canEditLabel(currentEditingLabelMeta || {})) {
    showToast(
      " Only project managers or label creators can edit this label.",
      "error"
    );
    return;
  }

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
    const updated = await res.json();
    currentEditingLabelMeta = updated;
    updateLabelRowMetadata(updated);
    updateInlineLabelChip({
      labelId: updated.labelId,
      name: updated.name,
      color: updated.color,
    });
    closeEditLabelPopup(null);
  } catch (err) {
    console.error(" saveEditedLabel error:", err);
    showToast(" Failed to update label", "error");
  }
}

async function deleteEditedLabel() {
  if (!currentEditingLabelId)
    return showToast(" Please select a label to delete", "error");

  if (!canEditLabel(currentEditingLabelMeta || {})) {
    showToast(
      " Only project managers or label creators can delete this label.",
      "error"
    );
    return;
  }

  try {
    const res = await fetch(`/api/labels/${currentEditingLabelId}`, {
      method: "DELETE",
      headers: getAuthHeaders(),
    });

    if (!res.ok) throw new Error(`Delete failed (${res.status})`);
    removeInlineLabelChip(currentEditingLabelId);
    const row = document.querySelector(
      `.label-option[data-label-id="${currentEditingLabelId}"]`
    );
    row?.remove();
    closeEditLabelPopup(null);
    showToast(" Label deleted", "success");
  } catch (err) {
    console.error(" deleteEditedLabel error:", err);
    showToast("Failed to delete label", "error");
  }
}

function getAuthHeaders(additional = {}) {
  const token = getToken();
  const headers = { ...additional };
  if (token) {
    headers.Authorization = "Bearer " + token;
  }
  return headers;
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
