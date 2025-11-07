// ✅ members.js – Quản lý popup Members (thêm / xóa / tìm kiếm thành viên)
import { apiFetch, escapeHtml, showToast, getInitials } from "./utils.js";
import { currentTask } from "./modal-task.js";

let allMembers = [];
let assignedMembers = [];

/* ---------------------- 🧱 LOAD MEMBERS ---------------------- */
export async function loadMembers(projectId) {
  try {
    const res = await apiFetch(`/api/projects/${projectId}/members`);
    allMembers = await res.json();
    return allMembers;
  } catch (err) {
    console.error("❌ loadMembers failed:", err);
    showToast("Không thể tải danh sách thành viên.", "error");
    return [];
  }
}

/* ---------------------- 👥 RENDER POPUP ---------------------- */
export function openMembersPopup(task) {
  const popup = document.getElementById("members-popup");
  if (!popup) return;
  assignedMembers = [...(task.members || [])];
  currentTask = task;

  renderMemberList(allMembers, assignedMembers);
  popup.classList.remove("hidden");

  // Vị trí popup (giữa màn hình)
  const rect = popup.getBoundingClientRect();
  popup.style.top = `calc(50% - ${rect.height / 2}px)`;
  popup.style.left = `calc(50% - ${rect.width / 2}px)`;
}

export function closeMembersPopup() {
  const popup = document.getElementById("members-popup");
  if (popup) popup.classList.add("hidden");
  currentTask = null;
}

document.getElementById("close-members-btn")?.addEventListener("click", closeMembersPopup);

/* ---------------------- 🔍 SEARCH ---------------------- */
const searchInput = document.getElementById("search-member-input");
if (searchInput) {
  searchInput.addEventListener("input", e => {
    const keyword = e.target.value.toLowerCase();
    const filtered = allMembers.filter(m => 
      m.name.toLowerCase().includes(keyword) || m.email.toLowerCase().includes(keyword)
    );
    renderMemberList(filtered, assignedMembers);
  });
}

/* ---------------------- 🧩 RENDER MEMBER LIST ---------------------- */
function renderMemberList(list, assigned) {
  const section = document.getElementById("members-section");
  if (!section) return;

  if (!list.length) {
    section.innerHTML = `<p class="text-gray-400 italic text-sm">No members found.</p>`;
    return;
  }

  section.innerHTML = "";
  list.forEach(m => {
    const isAssigned = assigned.some(a => a.userId === m.userId);
    const div = document.createElement("div");
    div.className = "flex items-center justify-between px-2 py-1 rounded hover:bg-gray-50";

    div.innerHTML = `
      <div class="flex items-center gap-2">
        <div class="w-8 h-8 rounded-full bg-gray-300 flex items-center justify-center text-xs font-semibold">
          ${escapeHtml(getInitials(m.name))}
        </div>
        <div>
          <p class="text-sm font-medium text-gray-800">${escapeHtml(m.name)}</p>
          <p class="text-xs text-gray-500">${escapeHtml(m.email)}</p>
        </div>
      </div>
      <button data-id="${m.userId}" 
              class="text-sm font-medium ${isAssigned ? 'text-red-500 hover:text-red-700' : 'text-blue-600 hover:text-blue-800'}">
        ${isAssigned ? 'Remove' : 'Add'}
      </button>
    `;
    section.appendChild(div);
  });

  section.querySelectorAll("button[data-id]").forEach(btn => {
    btn.addEventListener("click", async e => {
      const id = e.target.dataset.id;
      const isAssigned = assignedMembers.some(a => a.userId == id);
      if (isAssigned) await unassignMember(id);
      else await assignMember(id);
    });
  });
}

/* ---------------------- ➕ ASSIGN MEMBER ---------------------- */
async function assignMember(userId) {
  if (!currentTask) return;
  try {
    const res = await apiFetch(`/api/tasks/${currentTask.taskId}/members`, {
      method: "POST",
      body: JSON.stringify({ userId })
    });
    if (!res.ok) throw new Error();
    const newMember = allMembers.find(m => m.userId == userId);
    assignedMembers.push(newMember);
    showToast(`✅ Đã thêm ${newMember.name} vào task.`);
    renderMemberList(allMembers, assignedMembers);
  } catch (err) {
    console.error("❌ assignMember failed:", err);
    showToast("Không thể thêm thành viên.", "error");
  }
}

/* ---------------------- ➖ UNASSIGN MEMBER ---------------------- */
async function unassignMember(userId) {
  if (!currentTask) return;
  try {
    const res = await apiFetch(`/api/tasks/${currentTask.taskId}/members/${userId}`, {
      method: "DELETE"
    });
    if (!res.ok) throw new Error();
    assignedMembers = assignedMembers.filter(m => m.userId != userId);
    showToast("🗑️ Đã xóa thành viên khỏi task.");
    renderMemberList(allMembers, assignedMembers);
  } catch (err) {
    console.error("❌ unassignMember failed:", err);
    showToast("Không thể xóa thành viên.", "error");
  }
}
