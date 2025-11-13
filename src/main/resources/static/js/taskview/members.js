import { renderAvatar, showToast } from "./utils.js";

let debounceTimer;

// 🔹 Mở popup (hỗ trợ click thường + chuột phải)
export function openMembersPopup(e) {
  const popup = document.getElementById("members-popup");
  if (!popup) {
    console.error("❌ #members-popup not found");
    return;
  } // Ngăn chặn việc đóng popup ngay lập tức nếu click vào nút mở

  e?.stopPropagation(); // Tính toạ độ

  let top = 0;
  let left = 0;

  const rect =
    e &&
    e.currentTarget &&
    typeof e.currentTarget.getBoundingClientRect === "function"
      ? e.currentTarget.getBoundingClientRect()
      : null;

  const hasValidRect =
    rect &&
    rect.top >= 0 &&
    rect.left >= 0 &&
    rect.width > 0 &&
    rect.height > 0;

  if (hasValidRect) {
    // 👉 Gọi từ nút "Members" trong modal
    top = rect.bottom + window.scrollY + 6;
    left = rect.left + window.scrollX;
  } else {
    // 👉 Gọi từ context menu (chuột phải)
    top = (window.contextMenuY || e?.clientY || 100) + window.scrollY + 8;
    left = (window.contextMenuX || e?.clientX || 100) + window.scrollX + 8;
  }

  popup.style.position = "absolute";
  popup.style.top = `${top}px`;
  popup.style.left = `${left}px`;
  popup.classList.remove("hidden");

  document.getElementById("search-member-input").focus();
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
    const headers = {
      Authorization: "Bearer " + localStorage.getItem("token"),
    }; // 1. Lấy TẤT CẢ thành viên dự án (có lọc theo keyword)

    const resAll = await fetch(
      `/api/pm/members?projectId=${
        window.PROJECT_ID
      }&keyword=${encodeURIComponent(keyword)}`,
      { headers }
    );
    if (!resAll.ok) throw new Error("Failed to load project members");
    const allPayload = await resAll.json();
    const allMembers = Array.isArray(allPayload.content)
      ? allPayload.content
      : allPayload; // 2. Lấy thành viên ĐÃ ĐƯỢC GÁN vào task này

    const resTask = await fetch(`/api/tasks/${taskId}/members`, { headers });
    if (!resTask.ok) throw new Error("Failed to load task members");
    const taskMembers = await resTask.json();
    const assignedIds = new Set((taskMembers || []).map((m) => m.userId)); // 3. Phân loại thành 2 nhóm

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
          ? `<div class="members-group"><h4 class="members-group__title">Card members</h4><div class="members-group__list">${cardMembers}</div></div>`
          : ""
      }
      ${
        boardMembers
          ? `<div class="members-group"><h4 class="members-group__title secondary">Board members</h4><div class="members-group__list">${boardMembers}</div></div>`
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
    console.error("❌ Error loading members:", err);
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

  return `
    <div class="member-row ${
      isAssigned ? "is-assigned" : ""
    }" ${rowAttrs} data-user-id="${member.userId}">
      <div class="member-row__avatar">${renderAvatar(member)}</div>
      <div class="member-row__info">
        <span class="member-row__name" title="${member.name || ""}">${
    member.name || "Unnamed"
  }</span>
      </div>
      ${
        isAssigned
          ? `<button
        class="member-row__btn is-remove"
        title="${title}"
        data-action="${action}"
        data-user-id="${member.userId}"
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

// ================== ASSIGN / UNASSIGN ==================
export async function assignMember(userId, button) {
  const taskId = window.CURRENT_TASK_ID;
  if (!taskId) return;

  const isButton = button?.tagName === "BUTTON";
  if (isButton) {
    button.disabled = true;
  } else {
    button?.classList.add("is-loading");
  }

  try {
    const res = await fetch(`/api/tasks/${taskId}/assign/${userId}`, {
      method: "PUT",
      headers: { Authorization: "Bearer " + localStorage.getItem("token") },
    });
    if (!res.ok) throw new Error("Assign failed");
    showToast("✅ Member added");
    await loadMembers(document.getElementById("search-member-input").value); // Tải lại danh sách
  } catch (err) {
    console.error("❌ assignMember error:", err);
    showToast("❌ Failed to assign member", "error");
  } finally {
    if (isButton) {
      button.disabled = false;
    } else {
      button?.classList.remove("is-loading");
    }
  }
}

export async function unassignMember(userId, button) {
  const taskId = window.CURRENT_TASK_ID;
  if (!taskId) return;

  button.disabled = true;

  try {
    const res = await fetch(`/api/tasks/${taskId}/unassign/${userId}`, {
      method: "PUT",
      headers: { Authorization: "Bearer " + localStorage.getItem("token") },
    });
    if (!res.ok) throw new Error("Unassign failed");
    showToast("✅ Member removed");
    await loadMembers(document.getElementById("search-member-input").value); // Tải lại danh sách
  } catch (err) {
    console.error("❌ unassignMember error:", err);
    showToast("❌ Failed to unassign member", "error");
  } finally {
    button.disabled = false;
  }
}
