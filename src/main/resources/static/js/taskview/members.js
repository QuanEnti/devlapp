import {
  renderAvatar,
  showToast,
  getColorForId,
  getInitials,
  escapeHtml,
  getToken,
} from "./utils.js";
import {
  updateCardMembers,
  refreshActivityFeedOnly,
  showActivitySectionIfHidden,
} from "./main.js";

let debounceTimer;

// ================== POPUP POSITIONING CONFIG ==================
const MEMBERS_POPUP_CONFIG = {
  offset: 8, // Khoảng cách từ button/trigger
  minMargin: 16, // Khoảng cách tối thiểu từ edge viewport
};

// Helper function để tính toán vị trí thông minh cho members popup
function calculateMembersPopupPosition(popupElement, triggerRect) {
  if (!popupElement) return { top: 0, left: 0 };

  // Kích thước popup (từ CSS hoặc ước tính)
  let popupWidth = popupElement.offsetWidth;
  let popupHeight = popupElement.offsetHeight;

  if (!popupWidth || !popupHeight) {
    popupWidth = 340; // Từ CSS: w-[340px]
    popupHeight = 400; // Ước tính
  }

  const viewportWidth = window.innerWidth;
  const viewportHeight = window.innerHeight;
  const scrollX = window.scrollX || 0;
  const scrollY = window.scrollY || 0;

  let top, left;

  if (!triggerRect || triggerRect.width === 0 || triggerRect.height === 0) {
    // Gọi từ context menu (chuột phải) hoặc overview
    const mouseX = window.contextMenuX || viewportWidth / 2;
    const mouseY = window.contextMenuY || viewportHeight / 2;

    // Tính toán vị trí ưu tiên bottom-right
    left = mouseX + scrollX + MEMBERS_POPUP_CONFIG.offset;
    top = mouseY + scrollY + MEMBERS_POPUP_CONFIG.offset;

    // Kiểm tra và điều chỉnh nếu vượt quá viewport
    if (
      left + popupWidth >
      viewportWidth + scrollX - MEMBERS_POPUP_CONFIG.minMargin
    ) {
      left = mouseX + scrollX - popupWidth - MEMBERS_POPUP_CONFIG.offset;
    }
    if (
      top + popupHeight >
      viewportHeight + scrollY - MEMBERS_POPUP_CONFIG.minMargin
    ) {
      top = mouseY + scrollY - popupHeight - MEMBERS_POPUP_CONFIG.offset;
    }

    // Đảm bảo không vượt quá biên trái/trên
    left = Math.max(scrollX + MEMBERS_POPUP_CONFIG.minMargin, left);
    top = Math.max(scrollY + MEMBERS_POPUP_CONFIG.minMargin, top);
  } else {
    // Gọi từ nút "Members" trong modal
    const buttonBottom = triggerRect.bottom + scrollY;
    const buttonLeft = triggerRect.left + scrollX;
    const buttonRight = triggerRect.right + scrollX;
    const buttonTop = triggerRect.top + scrollY;

    // Ưu tiên hiển thị bên dưới và bên phải button
    left = buttonLeft;
    top = buttonBottom + MEMBERS_POPUP_CONFIG.offset;

    // Kiểm tra không gian xung quanh
    const spaceBelow = viewportHeight + scrollY - buttonBottom;
    const spaceAbove = buttonTop - scrollY;
    const spaceRight = viewportWidth + scrollX - buttonLeft;
    const spaceLeft = buttonLeft - scrollX;

    // Nếu không đủ chỗ bên dưới, hiển thị bên trên
    if (
      spaceBelow < popupHeight + MEMBERS_POPUP_CONFIG.minMargin &&
      spaceAbove > spaceBelow
    ) {
      top = buttonTop - popupHeight - MEMBERS_POPUP_CONFIG.offset;
    }

    // Nếu không đủ chỗ bên phải, hiển thị bên trái
    if (
      spaceRight < popupWidth + MEMBERS_POPUP_CONFIG.minMargin &&
      spaceLeft > spaceRight
    ) {
      left = buttonRight - popupWidth;
    }

    // Đảm bảo không vượt quá viewport
    left = Math.max(
      scrollX + MEMBERS_POPUP_CONFIG.minMargin,
      Math.min(
        left,
        viewportWidth + scrollX - popupWidth - MEMBERS_POPUP_CONFIG.minMargin
      )
    );
    top = Math.max(
      scrollY + MEMBERS_POPUP_CONFIG.minMargin,
      Math.min(
        top,
        viewportHeight + scrollY - popupHeight - MEMBERS_POPUP_CONFIG.minMargin
      )
    );
  }

  return { top, left };
}

// 🔹 Mở popup (hỗ trợ click thường + chuột phải)
export function openMembersPopup(e) {
  const popup = document.getElementById("members-popup");
  if (!popup) {
    console.error(" #members-popup not found");
    return;
  }

  e?.stopPropagation();

  // Lấy trigger rect
  let triggerRect = null;
  if (
    e &&
    e.currentTarget &&
    typeof e.currentTarget.getBoundingClientRect === "function"
  ) {
    triggerRect = e.currentTarget.getBoundingClientRect();
    const hasValidRect =
      triggerRect &&
      triggerRect.top >= 0 &&
      triggerRect.left >= 0 &&
      triggerRect.width > 0 &&
      triggerRect.height > 0;
    if (!hasValidRect) triggerRect = null;
  }

  // Tính toán vị trí thông minh
  popup.style.position = "fixed";
  const position = calculateMembersPopupPosition(popup, triggerRect);
  popup.style.top = `${position.top}px`;
  popup.style.left = `${position.left}px`;
  popup.classList.remove("hidden");

  // Điều chỉnh lại sau khi render để lấy kích thước thực tế
  requestAnimationFrame(() => {
    const actualRect = popup.getBoundingClientRect();
    const actualWidth = actualRect.width;
    const actualHeight = actualRect.height;
    const currentTop = parseFloat(popup.style.top) || position.top;
    const currentLeft = parseFloat(popup.style.left) || position.left;

    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;
    const scrollX = window.scrollX || 0;
    const scrollY = window.scrollY || 0;

    let adjustedLeft = currentLeft;
    let adjustedTop = currentTop;

    if (
      currentLeft + actualWidth >
      viewportWidth + scrollX - MEMBERS_POPUP_CONFIG.minMargin
    ) {
      adjustedLeft =
        viewportWidth + scrollX - actualWidth - MEMBERS_POPUP_CONFIG.minMargin;
    }
    if (
      currentTop + actualHeight >
      viewportHeight + scrollY - MEMBERS_POPUP_CONFIG.minMargin
    ) {
      adjustedTop =
        viewportHeight +
        scrollY -
        actualHeight -
        MEMBERS_POPUP_CONFIG.minMargin;
    }

    adjustedLeft = Math.max(
      scrollX + MEMBERS_POPUP_CONFIG.minMargin,
      adjustedLeft
    );
    adjustedTop = Math.max(
      scrollY + MEMBERS_POPUP_CONFIG.minMargin,
      adjustedTop
    );

    if (adjustedLeft !== currentLeft || adjustedTop !== currentTop) {
      popup.style.left = `${adjustedLeft}px`;
      popup.style.top = `${adjustedTop}px`;
    }
  });

  document.getElementById("search-member-input")?.focus();
  loadMembers();
}

// 🔹 Đóng popup
export function closeMembersPopup() {
  document.getElementById("members-popup")?.classList.add("hidden");
}

// 🔹 Gắn sự kiện popup + input tìm kiếm
export function initMemberEvents() {
  const openBtn = document.getElementById("open-members-btn");
  const closeBtn = document.getElementById("close-members-btn");
  const popup = document.getElementById("members-popup");
  const searchInput = document.getElementById("search-member-input"); // 🟦 Open / Close button

  openBtn?.addEventListener("click", openMembersPopup);
  closeBtn?.addEventListener("click", closeMembersPopup); // 🟧 Đóng popup khi click ra ngoài

  document.addEventListener("click", (e) => {
    const isInside = popup?.contains(e.target); // Kiểm tra xem có click vào nút mở (hoặc nút bên trong nút mở) không
    const isOpenBtn = e.target.closest("#open-members-btn");
    const isContextMenu = e.target.closest("#card-context-menu");
    if (!isInside && !isOpenBtn && !isContextMenu) {
      closeMembersPopup();
    }
  }); // 🟨 Gõ tìm kiếm (debounce)

  searchInput?.addEventListener("input", (e) => {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => loadMembers(e.target.value.trim()), 300);
  });
}

// ================== MEMBER HANDLING ==================
export async function loadMembers(keyword = "") {
  const taskId = window.CURRENT_TASK_ID;
  if (!taskId) return;

  const listContainer = document.getElementById("members-section");
  listContainer.innerHTML = `<p class="members-empty muted">Loading…</p>`;

  try {
    const token = getToken();
    const headers = {};
    if (token) {
      headers.Authorization = "Bearer " + token;
    } // 1. Lấy TẤT CẢ thành viên dự án (có lọc theo keyword)

    const resAll = await fetch(
      `/api/pm/members?projectId=${
        window.PROJECT_ID
      }&keyword=${encodeURIComponent(keyword)}`,
      { headers, credentials: "include" }
    );
    if (!resAll.ok) throw new Error("Failed to load project members");
    const allPayload = await resAll.json();
    const allMembers = Array.isArray(allPayload.content)
      ? allPayload.content
      : allPayload; // 2. Lấy thành viên ĐÃ ĐƯỢC GÁN vào task này

    const resTask = await fetch(`/api/tasks/${taskId}/members`, {
      headers,
      credentials: "include",
    });
    if (!resTask.ok) throw new Error("Failed to load task members");
    const taskMembers = await resTask.json();
    const assignedIds = new Set((taskMembers || []).map((m) => m.userId)); // 3. Phân loại thành 2 nhóm

    window.CURRENT_TASK_FOLLOWER_IDS = Array.from(assignedIds).reduce(
      (acc, id) => {
        const parsed = Number(id);
        if (!Number.isNaN(parsed)) acc.push(parsed);
        return acc;
      },
      []
    );

    const assigned = [];
    const notAssigned = [];

    allMembers.forEach((member) => {
      if (assignedIds.has(member.userId)) {
        assigned.push(member);
      } else {
        notAssigned.push(member);
      }
    });

    const cardMembers = assigned.map((m) => renderMemberRow(m, true)).join("");
    const boardMembers = notAssigned
      .map((m) => renderMemberRow(m, false))
      .join("");

    listContainer.innerHTML = `
      ${
        cardMembers
          ? `<div class="members-group" data-group="card"><h4 class="members-group__title">Card members</h4><div class="members-group__list" data-list="card">${cardMembers}</div></div>`
          : ""
      }
      ${
        boardMembers
          ? `<div class="members-group" data-group="board"><h4 class="members-group__title secondary">Board members</h4><div class="members-group__list" data-list="board">${boardMembers}</div></div>`
          : ""
      }
      ${
        !cardMembers && !boardMembers
          ? `<p class="members-empty">No members found.</p>`
          : ""
      }
    `;

    // 5. Gắn sự kiện click sau khi render
    addMemberClickListeners(listContainer);
  } catch (err) {
    console.error(" Error loading members:", err);
    listContainer.innerHTML = `<p class="members-error">Error loading members</p>`;
  }
}

/**
 * 🎨 Render một hàng thành viên
 * Loại bỏ 'onclick' và dùng data-attributes để thay thế
 */
function renderMemberRow(member, isAssigned) {
  const action = isAssigned ? "unassign" : "assign";
  const title = isAssigned ? "Remove from card" : "Add to card";
  const rowAttrs = isAssigned
    ? 'data-action="assigned"'
    : 'data-action="assign"';
  const userId = member.userId ?? member.id ?? "";
  const displayName = member.name || member.fullName || "Unnamed";
  const safeName = escapeHtml(displayName);

  return `
    <div class="member-row ${
      isAssigned ? "is-assigned" : ""
    }" ${rowAttrs} data-user-id="${userId}">
      <div class="member-row__avatar">${renderAvatar(member)}</div>
      <div class="member-row__info">
        <span class="member-row__name" title="${safeName}">${safeName}</span>
      </div>
      ${
        isAssigned
          ? `<button
        class="member-row__btn is-remove"
        title="${title}"
        data-action="${action}"
        data-user-id="${userId}"
      >
        <span aria-hidden="true">✕</span>
      </button>`
          : ""
      }
    </div>`;
}

/**
 * 🎧 Gắn sự kiện click cho các nút assign/unassign
 */
function addMemberClickListeners(container) {
  container.querySelectorAll(".member-row").forEach((row) => {
    if (row.dataset.listenerBound === "true") return;
    row.dataset.listenerBound = "true";
    row.addEventListener("click", (e) => {
      const button = e.target.closest("button[data-action]");
      if (button) {
        e.stopPropagation();
        const userId = button.dataset.userId || row.dataset.userId;
        unassignMember(userId, button);
        return;
      }

      if (row.dataset.action === "assign") {
        const userId = row.dataset.userId;
        assignMember(userId, row);
      }
    });
  });
}

function ensureMembersGroupList(type) {
  const container = document.getElementById("members-section");
  if (!container) return null;

  let group = container.querySelector(`.members-group[data-group="${type}"]`);
  if (!group) {
    const groupEl = document.createElement("div");
    groupEl.className = "members-group";
    groupEl.dataset.group = type;
    const titleClass =
      type === "card"
        ? "members-group__title"
        : "members-group__title secondary";
    const titleText = type === "card" ? "Card members" : "Board members";
    groupEl.innerHTML = `<h4 class="${titleClass}">${titleText}</h4><div class="members-group__list" data-list="${type}"></div>`;
    if (type === "card") {
      container.insertBefore(groupEl, container.firstChild || null);
    } else {
      container.appendChild(groupEl);
    }
    group = groupEl;
  }

  return group.querySelector(".members-group__list");
}

function extractMemberMeta(row) {
  if (!row) return null;
  const userIdRaw = row.dataset.userId;
  const userId = userIdRaw ? Number(userIdRaw) : null;
  const name = row.querySelector(".member-row__name")?.textContent.trim() || "";
  let avatarUrl = "";
  const img = row.querySelector(".member-row__avatar img");
  if (img) avatarUrl = img.getAttribute("src") || "";
  const color = getColorForId(String(userId || name));
  return { userId, name, avatarUrl, color };
}

function addMemberAvatarChip(meta) {
  if (!meta || meta.userId == null) return;
  const container = document.getElementById("members-avatars-inline");
  if (!container) return;
  const existing = container.querySelector(`[data-member-id="${meta.userId}"]`);
  if (existing) return;

  const chip = document.createElement("div");
  chip.dataset.memberId = String(meta.userId);
  chip.className =
    "w-6 h-6 rounded-full flex items-center justify-center text-white text-[10px] font-semibold shadow-sm cursor-pointer";
  chip.style.backgroundColor = meta.color || getColorForId(String(meta.userId));
  chip.title = meta.name || "";
  chip.textContent = getInitials(meta.name || "");
  container.appendChild(chip);
}

function removeMemberAvatarChip(userId) {
  const container = document.getElementById("members-avatars-inline");
  if (!container) return;
  const chip = container.querySelector(`[data-member-id="${userId}"]`);
  if (chip) chip.remove();
}

function updateFollowerIdsArray(userId, shouldAdd) {
  if (userId == null) return;
  const numericId = Number(userId);
  if (Number.isNaN(numericId)) return;
  if (!Array.isArray(window.CURRENT_TASK_FOLLOWER_IDS)) {
    window.CURRENT_TASK_FOLLOWER_IDS = [];
  }
  if (shouldAdd) {
    if (!window.CURRENT_TASK_FOLLOWER_IDS.includes(numericId)) {
      window.CURRENT_TASK_FOLLOWER_IDS.push(numericId);
    }
  } else {
    window.CURRENT_TASK_FOLLOWER_IDS = window.CURRENT_TASK_FOLLOWER_IDS.filter(
      (id) => Number(id) !== numericId
    );
  }
}

function insertMemberRow(targetList, memberObj, isAssigned) {
  if (!targetList) return null;
  const temp = document.createElement("div");
  temp.innerHTML = renderMemberRow(memberObj, isAssigned).trim();
  const newRow = temp.firstElementChild;
  targetList.appendChild(newRow);
  addMemberClickListeners(targetList);
  return newRow;
}

function moveMemberRow(row, targetType, meta) {
  if (!row || !meta) return null;
  const sourceList = row.parentElement;
  const targetList = ensureMembersGroupList(targetType);
  if (!targetList) return null;

  const memberObj = {
    userId: meta.userId,
    name: meta.name,
    avatarUrl: meta.avatarUrl,
  };

  const newRow = insertMemberRow(targetList, memberObj, targetType === "card");
  row.remove();

  if (sourceList && sourceList.children.length === 0) {
    const group = sourceList.closest(".members-group");
    group?.remove();
  }

  const container = document.getElementById("members-section");
  const emptyMsg = container?.querySelector(".members-empty");
  if (emptyMsg && container.querySelector(".members-group")) {
    emptyMsg.remove();
  }

  return newRow;
}

// ================== ASSIGN / UNASSIGN ==================
export async function assignMember(userId, rowElement) {
  const taskId = window.CURRENT_TASK_ID;
  if (!taskId) return;

  const row = rowElement;
  row?.classList.add("is-loading");

  try {
    const token = getToken();
    const headers = {};
    if (token) {
      headers.Authorization = "Bearer " + token;
    }
    const res = await fetch(`/api/tasks/${taskId}/assign/${userId}`, {
      method: "PUT",
      headers,
      credentials: "include",
    });

    if (res.status === 403) {
      const message = await res.text();
      const text =
        (message && message.trim()) ||
        "You do not have permission to assign members.";
      showToast(text, "error");
      return;
    }

    if (!res.ok) {
      throw new Error(`Assign failed (${res.status})`);
    }

    const meta = extractMemberMeta(row);
    if (!meta) {
      const keyword =
        document.getElementById("search-member-input")?.value || "";
      await loadMembers(keyword);
      return;
    }

    moveMemberRow(row, "card", meta);
    addMemberAvatarChip(meta);
    updateFollowerIdsArray(meta.userId, true);

    // Cập nhật card bên ngoài
    try {
      const token = getToken();
      const headers = {};
      if (token) {
        headers.Authorization = "Bearer " + token;
      }
      const res = await fetch(`/api/tasks/${taskId}`, {
        headers,
        credentials: "include",
      });
      if (res.ok) {
        const updatedTask = await res.json();
        if (updatedTask && updatedTask.assignees) {
          updateCardMembers(taskId, updatedTask.assignees);
        }
      }
    } catch (err) {
      console.error("Error reloading task for card update:", err);
    }

    // Refresh activity feed to show new activity
    showActivitySectionIfHidden();
    await refreshActivityFeedOnly(taskId);
  } catch (err) {
    console.error("assignMember error:", err);
    showToast("Failed to assign member", "error");
  } finally {
    row?.classList.remove("is-loading");
  }
}

export async function unassignMember(userId, button) {
  const taskId = window.CURRENT_TASK_ID;
  if (!taskId) return;

  button.disabled = true;
  const row = button.closest(".member-row");
  const meta = extractMemberMeta(row);

  try {
    const token = getToken();
    const headers = {};
    if (token) {
      headers.Authorization = "Bearer " + token;
    }
    const res = await fetch(`/api/tasks/${taskId}/unassign/${userId}`, {
      method: "PUT",
      headers,
      credentials: "include",
    });

    if (res.status === 403) {
      const message = await res.text();
      const text =
        (message && message.trim()) ||
        "You do not have permission to remove members.";
      showToast(text, "error");
      return;
    }

    if (!res.ok) {
      throw new Error(`Unassign failed (${res.status})`);
    }

    if (!meta) {
      const keyword =
        document.getElementById("search-member-input")?.value || "";
      await loadMembers(keyword);
      return;
    }

    moveMemberRow(row, "board", meta);
    removeMemberAvatarChip(meta.userId);
    updateFollowerIdsArray(meta.userId, false);

    // Cập nhật card bên ngoài
    try {
      const token = getToken();
      const headers = {};
      if (token) {
        headers.Authorization = "Bearer " + token;
      }
      const res = await fetch(`/api/tasks/${taskId}`, {
        headers,
        credentials: "include",
      });
      if (res.ok) {
        const updatedTask = await res.json();
        updateCardMembers(taskId, updatedTask.assignees || []);
      }
    } catch (err) {
      console.error("Error reloading task for card update:", err);
    }

    // Refresh activity feed to show new activity
    showActivitySectionIfHidden();
    await refreshActivityFeedOnly(taskId);
  } catch (err) {
    console.error("unassignMember error:", err);
    showToast("Failed to unassign member", "error");
  } finally {
    button.disabled = false;
  }
}
