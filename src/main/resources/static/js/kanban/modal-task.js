// ✅ modal-task.js – Quản lý chi tiết Task (mô tả, đính kèm, comment…)
import { apiFetch, escapeHtml, showToast } from "./utils.js";

let currentTask = null;

/* ---------------------- 🪟 OPEN & CLOSE MODAL ---------------------- */
export async function openTaskModal(taskId) {
  const modal = document.getElementById("task-detail-modal");
  if (!modal) return;

  try {
    const res = await apiFetch(`/api/tasks/${taskId}`);
    if (!res.ok) throw new Error("Task not found");
    currentTask = await res.json();

    renderTaskDetail(currentTask);
    modal.classList.remove("hidden");
    document.body.classList.add("overflow-hidden");
  } catch (err) {
    console.error("❌ openTaskModal failed:", err);
    showToast("Không thể tải chi tiết công việc.", "error");
  }
}

export function closeTaskModal() {
  const modal = document.getElementById("task-detail-modal");
  if (modal) modal.classList.add("hidden");
  document.body.classList.remove("overflow-hidden");
  currentTask = null;
}

document.getElementById("close-modal-btn")?.addEventListener("click", closeTaskModal);

/* ---------------------- 🧾 RENDER MAIN CONTENT ---------------------- */
function renderTaskDetail(task) {
  document.querySelector("#task-detail-modal h2").textContent = task.title || "Untitled Task";

  // ✅ Description
  renderDescription(task.description);

  // ✅ Due date
  const dueText = document.getElementById("due-date-text");
  const dueStatus = document.getElementById("due-date-status");
  if (task.dueDate) {
    dueText.textContent = new Date(task.dueDate).toLocaleString("vi-VN");
    dueStatus.textContent = "Due";
    dueStatus.className = "ml-2 text-xs font-medium rounded px-2 py-0.5 bg-blue-100 text-blue-700";
  } else {
    dueText.textContent = "No due date";
    dueStatus.textContent = "None";
    dueStatus.className = "ml-2 text-xs font-medium rounded px-2 py-0.5 bg-gray-200 text-gray-600";
  }

  // ✅ Attachments
  renderAttachments(task.attachments || []);

  // ✅ Comments & Activity
  renderComments(task.comments || []);
  renderActivity(task.activityLogs || []);
}

/* ---------------------- 🧾 DESCRIPTION ---------------------- */
function renderDescription(text) {
  const contentEl = document.getElementById("description-content");
  const placeholder = document.getElementById("description-placeholder");
  const display = document.getElementById("description-display");
  const editor = document.getElementById("description-editor");
  const textarea = document.getElementById("description-textarea");

  if (!contentEl || !textarea) return;

  if (text && text.trim()) {
    placeholder.classList.add("hidden");
    contentEl.innerHTML = escapeHtml(text).replace(/\n/g, "<br>");
  } else {
    placeholder.classList.remove("hidden");
    contentEl.innerHTML = "";
  }

  document.getElementById("edit-desc-btn")?.addEventListener("click", () => {
    display.classList.add("hidden");
    editor.classList.remove("hidden");
    textarea.value = text || "";
  });

  document.getElementById("cancel-desc-btn")?.addEventListener("click", () => {
    editor.classList.add("hidden");
    display.classList.remove("hidden");
  });

  document.getElementById("save-desc-btn")?.addEventListener("click", async () => {
    await saveDescription(textarea.value.trim());
    editor.classList.add("hidden");
    display.classList.remove("hidden");
  });
}

async function saveDescription(newText) {
  if (!currentTask) return;
  try {
    const res = await apiFetch(`/api/tasks/${currentTask.taskId}/description`, {
      method: "PUT",
      body: JSON.stringify({ description: newText })
    });
    if (!res.ok) throw new Error("Failed to update");
    currentTask.description = newText;
    renderDescription(newText);
    showToast("✅ Description updated!");
  } catch (err) {
    console.error("❌ saveDescription failed:", err);
    showToast("Không thể lưu mô tả.", "error");
  }
}

/* ---------------------- 📎 ATTACHMENTS ---------------------- */
function renderAttachments(list) {
  const container = document.getElementById("attachments-list");
  if (!container) return;

  if (!list.length) {
    container.innerHTML = `<p class="text-gray-400 italic">No attachments yet.</p>`;
    return;
  }

  container.innerHTML = "";
  list.forEach(file => {
    const div = document.createElement("div");
    div.className = "flex items-center justify-between bg-gray-50 border border-gray-200 rounded-md px-2 py-1";
    div.innerHTML = `
      <span class="truncate text-sm">${escapeHtml(file.displayName || file.fileName)}</span>
      <div class="flex items-center gap-2">
        <button class="text-blue-600 hover:text-blue-800 text-sm" data-preview="${file.id}">Preview</button>
        <button class="text-red-500 hover:text-red-700 text-sm" data-delete="${file.id}">Delete</button>
      </div>
    `;
    container.appendChild(div);
  });

  container.querySelectorAll("[data-preview]").forEach(btn =>
    btn.addEventListener("click", e => openAttachmentPreview(e.target.dataset.preview))
  );

  container.querySelectorAll("[data-delete]").forEach(btn =>
    btn.addEventListener("click", e => deleteAttachment(e.target.dataset.delete))
  );
}

async function deleteAttachment(fileId) {
  if (!confirm("Delete this attachment?")) return;
  try {
    const res = await apiFetch(`/api/attachments/${fileId}`, { method: "DELETE" });
    if (res.ok) {
      currentTask.attachments = currentTask.attachments.filter(f => f.id !== fileId);
      renderAttachments(currentTask.attachments);
      showToast("🗑️ Deleted attachment.");
    } else {
      throw new Error();
    }
  } catch (err) {
    console.error("❌ deleteAttachment failed:", err);
    showToast("Không thể xóa tệp đính kèm.", "error");
  }
}

/* ---------------------- 💬 COMMENTS ---------------------- */
function renderComments(list) {
  const container = document.getElementById("comments-list");
  if (!container) return;

  if (!list.length) {
    container.innerHTML = `<p class="text-gray-400 italic">No comments yet.</p>`;
    return;
  }

  container.innerHTML = "";
  list.forEach(c => {
    const div = document.createElement("div");
    div.className = "border-b pb-2";
    div.innerHTML = `
      <div class="flex justify-between items-center">
        <b>${escapeHtml(c.authorName || "Unknown")}</b>
        <span class="text-xs text-gray-400">${escapeHtml(c.createdAt || "")}</span>
      </div>
      <p class="text-sm text-gray-700 mt-1">${escapeHtml(c.content || "")}</p>
    `;
    container.appendChild(div);
  });
}

/* ---------------------- 📜 ACTIVITY FEED ---------------------- */
function renderActivity(list) {
  const feed = document.getElementById("activity-feed");
  if (!feed) return;

  if (!list.length) {
    feed.innerHTML = `<p class="text-gray-400 italic">No activity yet.</p>`;
    return;
  }

  feed.innerHTML = "";
  list.forEach(a => {
    const div = document.createElement("div");
    div.className = "border-b pb-1";
    div.innerHTML = `
      <p><b>${escapeHtml(a.actorName || "Someone")}</b> ${escapeHtml(a.action || "")}</p>
      <p class="text-xs italic text-gray-500">${escapeHtml(a.timestamp || "")}</p>
    `;
    feed.appendChild(div);
  });
}
