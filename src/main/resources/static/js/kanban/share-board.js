// ✅ share-board.js – Quản lý popup chia sẻ bảng dự án
import { API_BASE_URL, AUTH_TOKEN, currentProjectId } from "./main.js";
import { escapeHtml, showToast } from "./utils.js";

let members = [];

/* ---------------------- ⚙️ KHỞI TẠO ---------------------- */
document.addEventListener("DOMContentLoaded", () => {
  const openBtn = document.getElementById("share-board-btn");
  const popup = document.getElementById("share-board-popup");
  const closeBtn = document.getElementById("close-share-popup");

  if (!openBtn || !popup) return;

  openBtn.addEventListener("click", async () => {
    popup.classList.remove("hidden");
    await loadMembers();
  });

  closeBtn?.addEventListener("click", () => popup.classList.add("hidden"));
  document.addEventListener("click", e => {
    if (!popup.contains(e.target) && !openBtn.contains(e.target)) {
      popup.classList.add("hidden");
    }
  });

  initInviteHandler();
  initLinkActions();
});

/* ---------------------- 👥 LOAD MEMBERS ---------------------- */
export async function loadMembers() {
  const container = document.getElementById("members-list");
  if (!container) return;

  try {
    const res = await fetch(`${API_BASE_URL}/api/projects/${currentProjectId}/members`, {
      headers: { Authorization: `Bearer ${AUTH_TOKEN}` },
    });
    if (!res.ok) throw new Error();
    members = await res.json();
    renderMembers(members);
  } catch (err) {
    console.error("❌ loadMembers failed:", err);
    container.innerHTML = `<p class="text-gray-400 italic">Không thể tải danh sách thành viên.</p>`;
  }
}

/* ---------------------- 🧾 RENDER MEMBERS ---------------------- */
function renderMembers(list) {
  const container = document.getElementById("members-list");
  if (!container) return;

  if (!list || !list.length) {
    container.innerHTML = `<p class="text-gray-400 italic text-sm text-center py-3">No members yet.</p>`;
    return;
  }

  container.innerHTML = list
    .map(m => `
      <div class="flex items-center justify-between p-2 border rounded-md hover:bg-gray-50 transition">
        <div class="flex items-center gap-3">
          <img src="${escapeHtml(m.avatarUrl || 'https://i.pravatar.cc/40?u=' + m.email)}"
               alt="${escapeHtml(m.name)}"
               class="w-8 h-8 rounded-full object-cover">
          <div>
            <p class="font-medium text-sm text-gray-800">${escapeHtml(m.name)}</p>
            <p class="text-xs text-gray-500">${escapeHtml(m.email)}</p>
          </div>
        </div>
        <select data-user="${m.userId}"
                class="role-select border border-gray-300 rounded-md text-sm px-2 py-1 focus:ring-1 focus:ring-blue-500">
          <option value="MEMBER" ${m.role === "MEMBER" ? "selected" : ""}>Member</option>
          <option value="ADMIN" ${m.role === "ADMIN" ? "selected" : ""}>Admin</option>
          <option value="VIEWER" ${m.role === "VIEWER" ? "selected" : ""}>Viewer</option>
        </select>
      </div>
    `)
    .join("");

  // 🎯 Khi đổi role
  container.querySelectorAll(".role-select").forEach(sel => {
    sel.addEventListener("change", e => updateMemberRole(e.target.dataset.user, e.target.value));
  });
}

/* ---------------------- 🔁 CẬP NHẬT ROLE ---------------------- */
async function updateMemberRole(userId, newRole) {
  try {
    const res = await fetch(`${API_BASE_URL}/api/projects/${currentProjectId}/members/${userId}/role`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${AUTH_TOKEN}`,
      },
      body: JSON.stringify({ role: newRole }),
    });
    if (!res.ok) throw new Error();
    showToast("✅ Cập nhật quyền thành công!");
  } catch (err) {
    console.error("❌ updateMemberRole failed:", err);
    showToast("Không thể cập nhật quyền.", "error");
  }
}

/* ---------------------- ✉️ MỜI THÀNH VIÊN ---------------------- */
function initInviteHandler() {
  const inviteBtn = document.getElementById("invite-btn");
  const emailInput = document.getElementById("invite-email");
  const roleSelect = document.getElementById("invite-role");

  if (!inviteBtn) return;

  inviteBtn.addEventListener("click", async () => {
    const email = emailInput.value.trim();
    const role = roleSelect.value;
    if (!email) return showToast("Vui lòng nhập email.", "error");

    try {
      const res = await fetch(`${API_BASE_URL}/api/projects/${currentProjectId}/invite`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${AUTH_TOKEN}`,
        },
        body: JSON.stringify({ email, role }),
      });
      if (!res.ok) throw new Error();
      showToast("📩 Đã gửi lời mời thành viên!");
      emailInput.value = "";
      await loadMembers();
    } catch (err) {
      console.error("❌ inviteMember failed:", err);
      showToast("Không thể gửi lời mời.", "error");
    }
  });
}

/* ---------------------- 🔗 LINK CHIA SẺ ---------------------- */
function initLinkActions() {
  const copyLink = document.getElementById("copy-link");
  const deleteLink = document.getElementById("delete-link");
  const permissionSelect = document.getElementById("link-permission");

  copyLink?.addEventListener("click", async e => {
    e.preventDefault();
    try {
      const res = await fetch(`${API_BASE_URL}/api/projects/${currentProjectId}/share-link`, {
        headers: { Authorization: `Bearer ${AUTH_TOKEN}` },
      });
      if (!res.ok) throw new Error();
      const data = await res.json();
      const link = data.link || `${window.location.origin}/join/${currentProjectId}`;
      await navigator.clipboard.writeText(link);
      showToast("🔗 Đã sao chép liên kết mời!");
    } catch (err) {
      console.error("❌ copyLink failed:", err);
      showToast("Không thể sao chép link.", "error");
    }
  });

  deleteLink?.addEventListener("click", async e => {
    e.preventDefault();
    if (!confirm("Bạn có chắc muốn xoá link chia sẻ?")) return;
    try {
      const res = await fetch(`${API_BASE_URL}/api/projects/${currentProjectId}/share-link`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${AUTH_TOKEN}` },
      });
      if (!res.ok) throw new Error();
      showToast("🗑️ Đã xoá link chia sẻ.");
    } catch (err) {
      console.error("❌ deleteLink failed:", err);
      showToast("Không thể xoá link.", "error");
    }
  });

  permissionSelect?.addEventListener("change", async e => {
    const perm = e.target.value;
    try {
      const res = await fetch(`${API_BASE_URL}/api/projects/${currentProjectId}/share-link/permission`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${AUTH_TOKEN}`,
        },
        body: JSON.stringify({ permission: perm }),
      });
      if (!res.ok) throw new Error();
      showToast(`🔐 Cập nhật quyền liên kết: ${perm}`);
    } catch (err) {
      console.error("❌ updateLinkPermission failed:", err);
      showToast("Không thể cập nhật quyền liên kết.", "error");
    }
  });
}
