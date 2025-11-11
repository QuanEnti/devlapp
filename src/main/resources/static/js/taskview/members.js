import { renderAvatar, showToast } from "./utils.js";

let debounceTimer;

// 🔹 Mở popup (hỗ trợ click thường + chuột phải)
export function openMembersPopup(e) {
  const popup = document.getElementById("members-popup");
  if (!popup) {
    console.error("❌ #members-popup not found");
    return;
  }

  console.log("openMembersPopup called", e);

  // Tính toạ độ
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

  console.log("Popup opened at top=" + top + ", left=" + left);

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
  const searchInput = document.getElementById("search-member-input");

  // 🟦 Open / Close button
  openBtn?.addEventListener("click", openMembersPopup);
  closeBtn?.addEventListener("click", closeMembersPopup);

  // 🟧 Đóng popup khi click ra ngoài
  document.addEventListener("click", (e) => {
    const isInside = popup?.contains(e.target);
    const isOpenBtn = e.target.closest("#open-members-btn");
    const isContextMenu = e.target.closest("#card-context-menu");
    if (!isInside && !isOpenBtn && !isContextMenu) closeMembersPopup();
  });

  // 🟨 Gõ tìm kiếm (debounce)
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
  listContainer.innerHTML = `<p class="text-gray-400 text-sm italic">Loading...</p>`;

  try {
    const headers = {
      Authorization: "Bearer " + localStorage.getItem("token"),
    };

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
      : allPayload;

    const resTask = await fetch(`/api/tasks/${taskId}/members`, { headers });
    if (!resTask.ok) throw new Error("Failed to load task members");
    const taskMembers = await resTask.json();
    const assignedIds = (taskMembers || []).map((m) => m.userId);

    listContainer.innerHTML = allMembers
      .map((m) => renderMemberRow(m, assignedIds.includes(m.userId)))
      .join("");
  } catch (err) {
    console.error("❌ Error loading members:", err);
    listContainer.innerHTML = `<p class="text-red-500 text-sm">Error loading members</p>`;
  }
}

function renderMemberRow(member, isAssigned) {
  return `
    <div class="flex items-center justify-between p-1 hover:bg-gray-100 rounded-md">
      <div class="flex items-center gap-2">
        ${renderAvatar(member)}
        <p class="text-sm text-gray-700">${member.name}</p>
      </div>
      <button 
        class="${
          isAssigned
            ? "text-red-500 hover:text-red-700"
            : "text-gray-400 hover:text-blue-500"
        } text-lg"
        title="${isAssigned ? "Remove" : "Add"}"
        onclick="${
          isAssigned
            ? `unassignMember(${member.userId})`
            : `assignMember(${member.userId})`
        }">
        ${isAssigned ? "×" : "＋"}
      </button>
    </div>`;
}

// ================== ASSIGN / UNASSIGN ==================
export async function assignMember(userId) {
  const taskId = window.CURRENT_TASK_ID;
  if (!taskId) return;

  try {
    const res = await fetch(`/api/tasks/${taskId}/assign/${userId}`, {
      method: "PUT",
      headers: { Authorization: "Bearer " + localStorage.getItem("token") },
    });
    if (!res.ok) throw new Error("Assign failed");
    showToast("✅ Member added");
    await loadMembers();
  } catch (err) {
    console.error("❌ assignMember error:", err);
    showToast("❌ Failed to assign member", "error");
  }
}

export async function unassignMember(userId) {
  const taskId = window.CURRENT_TASK_ID;
  if (!taskId) return;

  try {
    const res = await fetch(`/api/tasks/${taskId}/unassign/${userId}`, {
      method: "PUT",
      headers: { Authorization: "Bearer " + localStorage.getItem("token") },
    });
    if (!res.ok) throw new Error("Unassign failed");
    showToast("✅ Member removed");
    await loadMembers();
  } catch (err) {
    console.error("❌ unassignMember error:", err);
    showToast("❌ Failed to unassign member", "error");
  }
}

Object.assign(window, {
  assignMember,
  unassignMember,
  openMemberPopup: openMembersPopup,
});
