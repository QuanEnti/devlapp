import {
  escapeHtml,
  showToast,
  showToastNotification,
  formatTime,
  formatRelativeTime,
  renderAvatar,
  safeStop,
} from "./utils.js";

// ================== IMPORT MODULES ==================
import { initMemberEvents } from "./members.js";

import { openDatePopup, updateDateStatus } from "./dates.js";

import { initLabelEvents } from "./labels.js";

import { initAttachmentEvents, loadAttachments } from "./attachments.js";

document.getElementById("share-board-btn").addEventListener("click", () => {
  openSharePopup();
});

// ================== GLOBAL ==================
const params = new URLSearchParams(window.location.search);
const PROJECT_ID =
  window.PROJECT_ID ||
  new URLSearchParams(window.location.search).get("projectId") ||
  1;
window.PROJECT_ID = PROJECT_ID;

const modal = document.getElementById("task-detail-modal");
const closeModalBtn = document.getElementById("close-modal-btn");

// ================== BOARD ==================
async function loadColumns(projectId) {
  const res = await fetch(`/api/columns/project/${projectId}`, {
    headers: { Authorization: "Bearer " + localStorage.getItem("token") },
  });
  if (!res.ok) throw new Error("Không thể tải danh sách cột");
  return await res.json();
}

async function loadTasks(projectId) {
  const res = await fetch(`/api/tasks/project/${projectId}`, {
    headers: { Authorization: "Bearer " + localStorage.getItem("token") },
  });
  if (!res.ok) throw new Error("Không thể tải danh sách task");
  return await res.json();
}

function groupByColumn(tasks) {
  const groups = {};
  tasks.forEach((t) => {
    const col = t.columnName || "Backlog";
    if (!groups[col]) groups[col] = [];
    groups[col].push(t);
  });
  return groups;
}

async function renderDashboard(projectId) {
  try {
    const [columns, tasks] = await Promise.all([
      loadColumns(projectId),
      loadTasks(projectId),
    ]);
    const grouped = groupByColumn(tasks);
    const board = document.getElementById("kanban-board");
    board.innerHTML = "";

    columns.forEach((col) => {
      const items = grouped[col.name] || [];
      const htmlTasks = items.length
        ? items.map(renderCard).join("")
        : `<div class="text-sm text-slate-400 italic">Chưa có thẻ</div>`;

      board.innerHTML += `
  <div class="kanban-list w-[272px] bg-gray-50/50 rounded-lg border-0 shadow-sm
              flex flex-col overflow-hidden hover:shadow-md transition-shadow duration-200"> <!-- 272px ~ Trello list width -->

    <!-- Header: sticky với menu button -->
    <div class="sticky top-0 z-10 bg-gray-50/95 backdrop-blur-sm px-3 pt-3 pb-2.5 border-b border-gray-200/60">
      <div class="flex items-center justify-between group">
        <h3 class="font-semibold text-gray-700 text-sm truncate flex-1">${escapeHtml(col.name)}</h3>
        <button class="list-options-btn opacity-0 group-hover:opacity-100 text-gray-400 hover:text-gray-600 hover:bg-gray-200/60 rounded p-1 transition-all duration-150" aria-label="List options">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="currentColor" viewBox="0 0 20 20">
            <path d="M10 6a2 2 0 110-4 2 2 0 010 4zM10 12a2 2 0 110-4 2 2 0 010 4zM10 18a2 2 0 110-4 2 2 0 010 4z" />
          </svg>
        </button>
      </div>

      <!-- Menu -->
      <div class="list-options-menu hidden absolute right-2 mt-1 bg-white border border-gray-200
                  rounded-lg shadow-xl w-48 z-50 overflow-hidden">
        <p class="px-3 py-2 text-xs text-gray-500 border-b border-gray-100 bg-gray-50">List actions</p>
        <button class="block w-full text-left px-3 py-2 hover:bg-gray-50 text-sm transition-colors">Add card</button>
        <button class="block w-full text-left px-3 py-2 hover:bg-gray-50 text-sm transition-colors">Copy list</button>
        <button class="block w-full text-left px-3 py-2 hover:bg-gray-50 text-sm transition-colors">Move list</button>
        <button class="block w-full text-left px-3 py-2 hover:bg-gray-50 text-sm text-red-600 transition-colors">Archive list</button>
      </div>
    </div>

    <!-- Tasks: scrollable với spacing tối ưu -->
    <div id="col-${col.columnId}" class="flex-1 overflow-y-auto px-2.5 py-2.5 space-y-2 min-h-[50px] max-h-[calc(100vh-14rem)]">
      ${htmlTasks}
    </div>

    <!-- Add Card: button gọn gàng -->
    <div class="add-card-area px-2.5 py-2" data-col="${col.columnId}">
      <button
        class="w-full text-left text-sm text-gray-500 hover:text-gray-700 hover:bg-gray-100/80 font-medium px-2 py-1.5 rounded transition-all duration-150"
        data-add-card="${col.columnId}">
        <span class="inline-flex items-center gap-1.5">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          Add a card
        </span>
      </button>
    </div>
  </div>
`;
    });

    document.querySelectorAll("[data-add-card]").forEach((btn) => {
      btn.addEventListener("click", () =>
        showAddCardInput(btn.getAttribute("data-add-card"))
      );
    });
    enableDragDrop();
  } catch (e) {
    console.error("⚠️ Lỗi khi render board:", e);
  }
}

function renderCard(t) {
  const taskId = t.id || t.taskId;

  // 🔹 Render labels (colored tags) - Trello style
  const labelHtml =
    t.labels && Array.isArray(t.labels) && t.labels.length
      ? `<div class="flex flex-wrap gap-1 mb-2">
          ${t.labels
            .map(
              (l) => `
                <span class="inline-block h-2 w-12 rounded-sm"
                      style="background-color: ${l.color || "#94a3b8"}"
                      title="${escapeHtml(l.name)}">
                </span>
              `
            )
            .join("")}
        </div>`
      : "";

  // 🔹 Metadata icons (due date, comments, members) - Trello style
  const hasDeadline = t.deadline && t.deadline.trim() !== "";
  const hasAssignee = t.assigneeName && t.assigneeName !== "Unassigned";
  const commentCount = t.commentCount || 0;
  
  // Format due date
  let dueDateHtml = "";
  if (hasDeadline) {
    try {
      const deadlineDate = new Date(t.deadline);
      const now = new Date();
      const isOverdue = deadlineDate < now;
      const daysDiff = Math.ceil((deadlineDate - now) / (1000 * 60 * 60 * 24));
      
      let dateText = "";
      let dateClass = "text-gray-600";
      let bgClass = "bg-gray-100";
      
      if (isOverdue) {
        dateText = "Overdue";
        dateClass = "text-red-700";
        bgClass = "bg-red-100";
      } else if (daysDiff === 0) {
        dateText = "Today";
        dateClass = "text-orange-700";
        bgClass = "bg-orange-100";
      } else if (daysDiff === 1) {
        dateText = "Tomorrow";
        dateClass = "text-orange-600";
        bgClass = "bg-orange-50";
      } else if (daysDiff <= 7) {
        dateText = `${daysDiff}d`;
        dateClass = "text-gray-600";
        bgClass = "bg-gray-100";
      } else {
        dateText = deadlineDate.toLocaleDateString("en-US", { month: "short", day: "numeric" });
        dateClass = "text-gray-600";
        bgClass = "bg-gray-100";
      }
      
      dueDateHtml = `
        <div class="flex items-center gap-1 ${bgClass} ${dateClass} px-1.5 py-0.5 rounded text-[10px] font-medium">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
          <span>${dateText}</span>
        </div>
      `;
    } catch (e) {
      // Fallback
    }
  }
  
  const metadataHtml = `
    <div class="flex items-center gap-1.5 mt-2 flex-wrap">
      ${dueDateHtml}
      
      ${commentCount > 0 ? `
        <div class="flex items-center gap-1 text-gray-600">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
          </svg>
          <span class="text-[10px]">${commentCount}</span>
        </div>
      ` : ""}
      
      ${hasAssignee ? `
        <div class="flex items-center ml-auto">
          <img src="${t.assigneeAvatar || "https://i.pravatar.cc/24"}" 
               alt="${escapeHtml(t.assigneeName)}" 
               class="w-6 h-6 rounded-full border-2 border-white object-cover shadow-sm"
               title="${escapeHtml(t.assigneeName)}">
        </div>
      ` : ""}
    </div>
  `;

  return `
    <div data-open-task="${taskId}"
         class="kanban-card bg-white border-0 rounded-lg p-2.5 shadow-sm hover:shadow-md hover:bg-gray-50/50 transition-all duration-150 cursor-pointer">
      ${labelHtml}
      <p class="font-medium text-gray-800 text-sm leading-5 mb-0">${escapeHtml(t.title)}</p>
      ${(hasDeadline || commentCount > 0 || hasAssignee) ? metadataHtml : ""}
    </div>
  `;
}

// ================== LIST MENU (⋯) ==================
document.addEventListener("click", (e) => {
  // mở popup
  const btn = e.target.closest(".list-options-btn");
  if (btn) {
    e.stopPropagation();
    const menu = btn.parentElement.querySelector(".list-options-menu");

    // đóng popup khác
    document.querySelectorAll(".list-options-menu").forEach((m) => {
      if (m !== menu) m.classList.add("hidden");
    });

    // toggle popup hiện tại
    menu.classList.toggle("hidden");
    return;
  }

  // click ra ngoài => đóng hết
  if (!e.target.closest(".list-options-menu")) {
    document
      .querySelectorAll(".list-options-menu")
      .forEach((m) => m.classList.add("hidden"));
  }
});

// ================== QUICK ADD ==================
function showAddCardInput(columnId) {
  const area = document.querySelector(`.add-card-area[data-col='${columnId}']`);
  area.innerHTML = `
            <div class="space-y-2">
              <textarea id="new-card-title-${columnId}" rows="2"
                        class="w-full border border-slate-300 rounded-md px-2 py-1.5 text-sm focus:ring focus:ring-blue-300"
                        placeholder="Enter a title or paste a link"></textarea>
              <div class="flex items-center gap-2">
                <button class="bg-blue-600 hover:bg-blue-700 text-white text-sm px-3 py-1.5 rounded-md"
                        data-add-card-confirm="${columnId}">Add card</button>
                <button class="text-slate-500 text-sm" data-add-card-cancel="${columnId}">✕</button>
              </div>
            </div>
          `;

  document
    .querySelector(`[data-add-card-confirm="${columnId}"]`)
    .addEventListener("click", () => addCard(columnId));
  document
    .querySelector(`[data-add-card-cancel="${columnId}"]`)
    .addEventListener("click", () => cancelAddCard(columnId));
  document.getElementById(`new-card-title-${columnId}`).focus();
}

function cancelAddCard(columnId) {
  const area = document.querySelector(`.add-card-area[data-col='${columnId}']`);
  area.innerHTML = `
            <button class="w-full text-left text-sm text-gray-500 hover:text-gray-700 hover:bg-gray-100/80 font-medium px-2 py-1.5 rounded transition-all duration-150"
                    data-add-card="${columnId}">
              <span class="inline-flex items-center gap-1.5">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                </svg>
                Add a card
              </span>
            </button>
          `;
  area
    .querySelector("[data-add-card]")
    .addEventListener("click", () => showAddCardInput(columnId));
}

async function addCard(columnId) {
  const textarea = document.getElementById(`new-card-title-${columnId}`);
  const title = textarea.value.trim();
  if (!title) return;

  // ⚡ Hiển thị card tạm
  const colContainer = document.getElementById(`col-${columnId}`);
  const tempId = "temp-" + Date.now();
  colContainer.insertAdjacentHTML(
    "beforeend",
    `
            <div id="${tempId}" class="bg-white border border-slate-200 rounded-md p-3 shadow-sm opacity-60">
              <p class="font-medium text-slate-800 text-sm">${escapeHtml(
                title
              )}</p>
            </div>
          `
  );
  textarea.disabled = true;

  try {
    const res = await fetch("/api/tasks/quick", {
      method: "POST",
      headers: {
        Authorization: "Bearer " + localStorage.getItem("token"),
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ title, projectId: PROJECT_ID, columnId }),
    });

    if (!res.ok) throw new Error("Không thể tạo task");
    await renderDashboard(PROJECT_ID);
  } catch (err) {
    console.error("❌ Lỗi tạo task:", err);
    alert("Không thể tạo task!");
  } finally {
    document.getElementById(tempId)?.remove();
    textarea.disabled = false;
  }
}

document.addEventListener("click", (e) => {
  const openBtn = e.target.closest("[data-open-task]");
  if (openBtn) openModal(openBtn.getAttribute("data-open-task"));
});

async function openModal(taskId) {
  try {
    const res = await fetch(`/api/tasks/${taskId}`, {
      headers: {
        Authorization: "Bearer " + localStorage.getItem("token"),
      },
    });

    if (!res.ok) throw new Error("Không thể tải chi tiết task");
    const task = await res.json();
    document.querySelector("#task-detail-modal h2").textContent =
      task.title || "Untitled";
    renderDescription(task);
    updateDateStatus(task.deadline);
    window.CURRENT_TASK_ID = taskId;
    await loadAttachments(taskId);
    await loadActivityFeed(taskId);

    modal.classList.remove("hidden");
  } catch (err) {
    console.error("❌ Lỗi khi mở modal:", err);
  }
}

function closeModal() {
  modal.classList.add("hidden");
}
closeModalBtn.addEventListener("click", closeModal);
modal.addEventListener("click", (e) => {
  if (e.target === modal) closeModal();
});
document.addEventListener("keydown", (e) => {
  if (e.key === "Escape") {
    closeModal();
    closeMemberPopup();
  }
});

// ================== DESCRIPTION ==================
const descDisplay = document.getElementById("description-display");
const descContent = document.getElementById("description-content");
const descPlaceholder = document.getElementById("description-placeholder");
const descEditor = document.getElementById("description-editor");
const descTextarea = document.getElementById("description-textarea");
const editDescBtn = document.getElementById("edit-desc-btn");
const saveDescBtn = document.getElementById("save-desc-btn");
const cancelDescBtn = document.getElementById("cancel-desc-btn");

function showDescriptionEditor() {
  descDisplay.classList.add("hidden");
  descEditor.classList.remove("hidden");
  descTextarea.value = descContent.textContent.trim() || "";
  descTextarea.focus();
}
function hideDescriptionEditor() {
  descEditor.classList.add("hidden");
  descDisplay.classList.remove("hidden");
}
editDescBtn.addEventListener("click", showDescriptionEditor);
descDisplay.addEventListener("dblclick", showDescriptionEditor);
cancelDescBtn.addEventListener("click", hideDescriptionEditor);
saveDescBtn.addEventListener("click", saveDescription);

async function saveDescription() {
  const newDescription = descTextarea.value.trim();
  const taskId = window.CURRENT_TASK_ID;

  try {
    const res = await fetch(`/api/tasks/${taskId}/description`, {
      method: "PUT",
      headers: {
        Authorization: "Bearer " + localStorage.getItem("token"),
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ description_md: newDescription }),
    });

    // Nếu server trả lỗi 500 nhưng là lỗi lazy load → vẫn coi là thành công
    if (!res.ok) {
      const msg = await res.text();
      console.warn("⚠️ Server response:", res.status, msg);

      if (
        msg.includes("could not initialize proxy") ||
        msg.includes("no Session")
      ) {
        console.log(
          "✅ Saved successfully (proxy serialization error ignored)."
        );
        descContent.textContent = newDescription;
        descPlaceholder.style.display = newDescription ? "none" : "block";
        hideDescriptionEditor();
        return;
      }

      throw new Error(msg);
    }

    // ✅ Trường hợp response OK
    const updated = await res.json();
    const newDesc = updated.descriptionMd || "";
    descContent.textContent = newDesc;
    descPlaceholder.style.display = newDesc ? "none" : "block";
    hideDescriptionEditor();
  } catch (err) {
    console.error("❌ Save description error:", err);
    alert("Không thể lưu mô tả (vui lòng thử lại).");
  }
}

function renderDescription(task) {
  const desc = (task.descriptionMd || "").trim();
  descContent.textContent = desc;
  descPlaceholder.style.display = desc ? "none" : "block";
  editDescBtn.classList.toggle("hidden", !desc);
}
// ================== DRAG & DROP ==================
function enableDragDrop() {
  const taskCards = document.querySelectorAll("[data-open-task]");
  const columns = document.querySelectorAll("[id^='col-']");

  taskCards.forEach((card) => {
    card.setAttribute("draggable", "true");

    card.addEventListener("dragstart", (e) => {
      e.dataTransfer.setData("taskId", card.getAttribute("data-open-task"));
      card.classList.add("opacity-50");
    });

    card.addEventListener("dragend", (e) => {
      card.classList.remove("opacity-50");
    });
  });

  columns.forEach((col) => {
    col.addEventListener("dragover", (e) => e.preventDefault());
    col.addEventListener("drop", (e) => handleDrop(e, col));

    // 🟦 Gắn drop zone cuối cùng
    const dropZone = col.querySelector(".drop-zone");
    if (dropZone) {
      dropZone.addEventListener("dragover", (e) => e.preventDefault());
      dropZone.addEventListener("drop", (e) => handleDrop(e, col));
    }
  });
  let isMoving = false;
  async function handleDrop(e, col) {
    e.preventDefault();
    if (isMoving) return; // 🔒 ngăn gọi trùng
    isMoving = true;
    const taskId = e.dataTransfer.getData("taskId");
    const targetColId = parseInt(col.id.replace("col-", ""));
    const draggedCard = document.querySelector(`[data-open-task='${taskId}']`);

    col.appendChild(draggedCard);

    const cards = Array.from(col.querySelectorAll("[data-open-task]"));
    const newIndex = cards.indexOf(draggedCard);

    console.log("🧩 Sending:", {
      taskId,
      targetColumnId: targetColId,
      newOrderIndex: newIndex,
    });

    try {
      const res = await fetch(`/api/tasks/${taskId}/move`, {
        method: "PUT",
        headers: {
          Authorization: "Bearer " + localStorage.getItem("token"),
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          targetColumnId: Number(targetColId),
          newOrderIndex: Number(newIndex),
        }),
      });

      if (!res.ok) throw new Error(`Move failed: ${res.status}`);
      const updated = await res.json();
      await renderDashboard(PROJECT_ID);
    } catch (err) {
      console.error("⚠️ Move failed:", err);
    }
  }
}

// ========== ⚙️ LOAD ACTIVITY + COMMENTS ==========
async function loadActivityFeed(taskId) {
  const container = document.getElementById("activity-section");
  if (!container) return;

  container.innerHTML = `
          <h3 class="font-semibold text-gray-800 flex items-center gap-2 mb-2">
            💬 Comments & Activity
          </h3>
          <div class="mb-4">
            <textarea id="comment-input"
              class="w-full border border-gray-300 rounded-md p-2 text-sm h-20 focus:ring-2 focus:ring-blue-400"
              placeholder="Write a comment..."></textarea>
            <button id="send-comment"
              class="mt-2 bg-blue-600 hover:bg-blue-700 text-white px-3 py-1.5 rounded-md text-sm">
              Post
            </button>
          </div>
          <div id="activity-feed" class="space-y-4 text-sm text-gray-700">
            <p class="text-gray-400 italic">Loading...</p>
          </div>
        `;

  // Gắn event cho nút gửi comment
  document
    .getElementById("send-comment")
    .addEventListener("click", async () => {
      const content = document.getElementById("comment-input").value.trim();
      if (!content) return alert("Please enter a comment");
      await postComment(taskId, content);
      await loadActivityFeed(taskId);
    });

  try {
    const res = await fetch(`/api/tasks/${taskId}/activity`, {
      headers: {
        Authorization: "Bearer " + localStorage.getItem("token"),
      },
    });

    const data = await res.json();
    renderCommentAndActivity(taskId, data.comments, data.activityLogs);
  } catch (err) {
    console.error(err);
    container.querySelector(
      "#activity-feed"
    ).innerHTML = `<p class="text-red-500">❌ Failed to load comments or activity</p>`;
  }
}

// ========== 💬 RENDER COMMENTS + ACTIVITY ==========
function renderCommentAndActivity(taskId, comments, activities) {
  const feed = document.getElementById("activity-feed");
  if (!feed) return;

  feed.innerHTML = `
          <div id="comments-section" class="space-y-3">
            ${
              comments && comments.length
                ? renderComments(taskId, comments)
                : `<p class="text-gray-400 italic">No comments yet.</p>`
            }
          </div>
          <hr class="my-4 border-gray-300">
          <h4 class="font-semibold text-gray-700 mb-2">Activity</h4>
          <div id="activity-section-inner" class="space-y-2">
            ${
              activities && activities.length
                ? renderActivities(activities)
                : `<p class="text-gray-400 italic">No activity yet.</p>`
            }
          </div>
        `;
}

// ========== 💬 COMMENTS ==========
function renderComments(taskId, comments) {
  const currentUserId = Number(localStorage.getItem("currentUserId"));
  const currentUserEmail = localStorage.getItem("currentUserEmail");
  const currentUserName = localStorage.getItem("currentUserName") || "Unknown";

  return comments
    .map(
      (c) => `
        <div class="border border-gray-200 rounded-md p-3 bg-white hover:bg-gray-50 transition">
          <div class="flex items-center gap-2 mb-1">
            <img src="${
              c.userAvatar || "https://i.pravatar.cc/30"
            }" class="w-6 h-6 rounded-full">
            <b class="text-sm">${c.userName}</b>
            <span class="text-xs text-gray-500">${formatTime(
              c.createdAt
            )}</span>
          </div>

          <p class="text-sm text-gray-800 ml-8 whitespace-pre-line">
            ${highlightMentions(escapeHtml(c.content), c.mentionsJson)}
          </p>

          <div class="ml-8 mt-1 flex gap-3 text-xs text-blue-500">
            ${
              Number(c.userId) === currentUserId ||
              (c.userEmail && c.userEmail === currentUserEmail)
                ? `<button onclick="editComment(${taskId}, ${c.commentId})">Edit</button>
                  <button onclick="deleteComment(${taskId}, ${c.commentId})">Delete</button>`
                : `<button onclick="toggleReplyBox(${c.commentId})">Reply</button>`
            }
          </div>

          <div id="reply-box-${c.commentId}" class="hidden ml-8 mt-2 space-y-1">
            <textarea id="reply-input-${
              c.commentId
            }" class="w-full border border-gray-300 rounded-md p-1 text-xs h-12 focus:ring-2 focus:ring-blue-400" placeholder="Write a reply..."></textarea>
            <button onclick="postReply(${taskId}, ${
        c.commentId
      })" class="bg-blue-600 hover:bg-blue-700 text-white px-2 py-1 rounded-md text-xs">Reply</button>
          </div>

          ${renderReplies(taskId, c.replies || [])}
        </div>
      `
    )
    .join("");
}

// ========== 💬 RENDER REPLIES ==========
function renderReplies(taskId, replies) {
  return replies
    .map(
      (r) => `
        <div class="ml-10 mt-2 border-l border-gray-300 pl-3">
          <div class="flex items-center gap-2 mb-1">
            <img src="${
              r.userAvatar || "https://i.pravatar.cc/25"
            }" class="w-5 h-5 rounded-full">
            <b class="text-sm">${r.userName}</b>
            <span class="text-xs text-gray-500">${formatTime(
              r.createdAt
            )}</span>
          </div>
          <p class="text-sm text-gray-700 ml-6">
            ${highlightMentions(escapeHtml(r.content), r.mentionsJson)}
          </p>
        </div>
      `
    )
    .join("");
}
// 🔹 Escape regex ký tự đặc biệt
function escapeRegex(str) {
  return str.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
// 🎨 Highlight @mentions, @board/@card, hoặc email
function highlightMentions(text, mentionsJson) {
  if (!text) return "";

  try {
    const mentions = mentionsJson ? JSON.parse(mentionsJson) : [];

    if (Array.isArray(mentions) && mentions.length > 0) {
      mentions.forEach((m) => {
        const email = m.email || "";
        const name = m.name || "";
        const safeEmail = escapeRegex(email);
        const safeName = escapeRegex(name.trim()).replace(/\s+/g, "\\s+");
        const isSpecial = email === "@card" || email === "@board";

        // 🟣 Tag đặc biệt: @card / @board
        if (isSpecial) {
          const regex = new RegExp(
            `@?${escapeRegex(email.replace("@", ""))}(?=[\\s,.!?]|$)`,
            "gu"
          );
          text = text.replace(
            regex,
            `<span class="mention-chip" data-type="special">@${email.replace(
              "@",
              ""
            )}</span>`
          );
          return;
        }

        // 🟢 Email thật hoặc mention @Tên
        const regexEmail = new RegExp(`${safeEmail}(?=[\\s,.!?]|$)`, "gu");
        const regexName = new RegExp(`@${safeName}(?=[\\s,.!?]|$)`, "gu");

        text = text
          .replace(
            regexEmail,
            `<span class="mention-chip" onclick="openMentionProfile('${email}')">${email}</span>`
          )
          .replace(
            regexName,
            `<span class="mention-chip" onclick="openMentionProfile('${email}')">@${name}</span>`
          );
      });

      return text;
    }
  } catch (err) {
    console.warn("⚠️ Mentions parse failed:", err);
  }

  // 🧩 Fallback: tự động highlight email và tag đặc biệt trong text thô
  return (
    text
      // highlight email
      .replace(
        /\b([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,})\b/g,
        `<span class="mention-chip" onclick="openMentionProfile('$1')">$1</span>`
      )
      // highlight @card hoặc @board nếu người dùng gõ tay
      .replace(
        /@(?:card|board)\b/g,
        (match) =>
          `<span class="mention-chip" data-type="special">${match}</span>`
      )
  );
}

async function openMentionProfile(email) {
  try {
    const res = await fetch(
      `/api/users/by-email/${encodeURIComponent(email)}`,
      {
        headers: { Authorization: "Bearer " + localStorage.getItem("token") },
      }
    );
    if (!res.ok) throw new Error("Không tìm thấy người dùng");
    const data = await res.json();

    const popup = document.createElement("div");
    popup.className =
      "fixed inset-0 bg-black/40 flex items-center justify-center z-[9999]";
    popup.innerHTML = `
          <div class="bg-white rounded-lg p-5 shadow-lg w-80 relative animate-fadeIn">
            <button class="absolute top-2 right-3 text-gray-400 hover:text-gray-600 text-lg" onclick="this.closest('.fixed').remove()">×</button>
            <div class="flex flex-col items-center text-center">
              <img src="${
                data.avatarUrl || "https://i.pravatar.cc/100?u=" + data.email
              }" class="w-20 h-20 rounded-full object-cover mb-3">
              <h3 class="text-lg font-semibold text-gray-800">${data.name}</h3>
              <p class="text-sm text-gray-500 mb-2">${data.email}</p>
              <p class="text-xs text-gray-400 italic">${
                data.provider ? `(${data.provider})` : ""
              }</p>
              <p class="mt-3 text-sm text-gray-600">${
                data.bio || "No bio available."
              }</p>
            </div>
          </div>
        `;
    document.body.appendChild(popup);
  } catch (err) {
    console.error("⚠️ openMentionProfile failed:", err);
    alert("Không thể tải thông tin người dùng này!");
  }
}

// ========== 💬 GỢI Ý @MENTION ==========
async function loadMentionSuggestions(keyword) {
  try {
    const res = await fetch(
      `/api/pm/members/project/${PROJECT_ID}/mentions?keyword=${encodeURIComponent(
        keyword
      )}`
    );
    const data = await res.json();

    const allOptions = data.members || [];

    const suggestionBox = document.getElementById("mention-suggestions");
    if (!allOptions.length || !suggestionBox)
      return suggestionBox?.classList.add("hidden");

    suggestionBox.innerHTML = allOptions
      .map(
        (m) => `
          <div class="px-3 py-2 hover:bg-blue-50 cursor-pointer flex items-center gap-2"
              onclick="selectMention('${m.name}', '${m.email}', '${
          m.avatarUrl || ""
        }')">
            <img src="${
              m.avatarUrl || "https://i.pravatar.cc/30?u=" + m.email
            }" class="w-6 h-6 rounded-full">
            <div>
              <b class="text-sm text-gray-800">${m.name}</b>
              <p class="text-xs text-gray-500">${m.email}</p>
            </div>
          </div>
        `
      )
      .join("");

    const commentInput = document.getElementById("comment-input");
    const rect = commentInput.getBoundingClientRect();
    suggestionBox.style.position = "absolute";
    suggestionBox.style.top = rect.bottom + window.scrollY + "px";
    suggestionBox.style.left = rect.left + window.scrollX + "px";
    suggestionBox.style.width = rect.width + "px";
    suggestionBox.classList.remove("hidden");
  } catch (err) {
    console.error("⚠️ loadMentionSuggestions failed:", err);
  }
}

// ✅ GLOBAL LISTENER – chỉ 1 lần duy nhất, auto hoạt động mọi modal
document.addEventListener("input", async (e) => {
  if (e.target?.id === "comment-input") {
    const cursorPos = e.target.selectionStart;
    const text = e.target.value.slice(0, cursorPos);
    const match = text.match(/@([\wÀ-ỹ\s]*)$/u);
    if (match) {
      const keyword = match[1].trim();
      console.log("🔍 Mention trigger:", keyword);
      await loadMentionSuggestions(keyword);
    } else {
      document.getElementById("mention-suggestions")?.classList.add("hidden");
    }
  }
});

window.selectMention = function (name, email, avatarUrl) {
  const commentInput = document.getElementById("comment-input");
  if (!commentInput) return;

  const cursorPos = commentInput.selectionStart;
  const before = commentInput.value
    .slice(0, cursorPos)
    .replace(/@[\wÀ-ỹ\s]*$/u, "");
  const after = commentInput.value.slice(cursorPos);

  // Chèn tag thực tế
  commentInput.value = before + `${email} ` + after;

  document.getElementById("mention-suggestions")?.classList.add("hidden");

  const mentions = JSON.parse(localStorage.getItem("currentMentions") || "[]");
  if (!mentions.some((m) => m.email === email)) {
    mentions.push({ name, email, avatarUrl });
    localStorage.setItem("currentMentions", JSON.stringify(mentions));
  }
};

// ========== 📜 ACTIVITY (Trello-style) ==========
function renderActivities(activities) {
  return activities
    .filter((a) => !a.action.startsWith("COMMENT_")) // loại bỏ log comment nội bộ
    .map((a) => {
      let msg = "";
      let data = {};
      try {
        data = a.dataJson ? JSON.parse(a.dataJson) : {};
      } catch {
        data = {};
      }

      switch (a.action) {
        case "CREATE_TASK":
          msg = `created card <b>${escapeHtml(
            data.title || "Untitled"
          )}</b> in <i>${escapeHtml(data.column || "")}</i>`;
          break;

        case "EDIT_TASK":
          msg = `edited card title to <b>${escapeHtml(
            data.title || "Untitled"
          )}</b>`;
          break;

        case "MOVE_COLUMN":
          msg = `moved this card from <i>${escapeHtml(
            data.from || "Unknown"
          )}</i> to <i>${escapeHtml(data.to || "Unknown")}</i>`;
          break;

        case "ATTACH_LINK":
          msg = `attached link <a href="${escapeHtml(
            data.link || data.url || "#"
          )}" target="_blank">${escapeHtml(
            data.name || data.link || "link"
          )}</a>`;
          break;

        case "ATTACH_FILE":
          msg = `uploaded file <b>${escapeHtml(data.fileName || "a file")}</b>`;
          break;

        case "DELETE_ATTACHMENT":
          msg = `deleted attachment <b>${escapeHtml(
            data.fileName || data.name || "unknown"
          )}</b>`;
          break;

        case "ASSIGN_TASK":
          msg = `assigned to user ID <code>${escapeHtml(
            data.assigneeId || "?"
          )}</code>`;
          break;

        case "UPDATE_DATES":
          msg = `updated dates: start <i>${escapeHtml(
            data.start || "N/A"
          )}</i> → deadline <i>${escapeHtml(data.deadline || "N/A")}</i>`;
          break;

        case "CLOSE_TASK":
          msg = `closed this card`;
          break;

        case "REOPEN_TASK":
          msg = `reopened this card`;
          break;

        default:
          msg = a.action.replaceAll("_", " ").toLowerCase();
      }

      return `
              <div class="text-xs text-gray-700 border-l-2 border-blue-400 pl-2 py-1 hover:bg-gray-50 rounded transition">
                <b class="text-blue-700">${escapeHtml(a.actorName)}</b> ${msg}
                <span class="text-gray-400 ml-1">${formatTime(
                  a.createdAt
                )}</span>
              </div>
            `;
    })
    .join("");
}

// ========== 🔁 REPLY TO COMMENT ==========
async function postReply(taskId, parentId) {
  const input = document.getElementById(`reply-input-${parentId}`);
  const content = input.value.trim();
  if (!content) return alert("Please enter a reply");

  try {
    const res = await fetch(`/api/tasks/${taskId}/comments/${parentId}/reply`, {
      method: "POST",
      headers: {
        Authorization: "Bearer " + localStorage.getItem("token"),
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ content }),
    });

    if (!res.ok) throw new Error("Reply failed");
    await loadActivityFeed(taskId);
  } catch (err) {
    console.error(err);
    alert("❌ Failed to send reply");
  }
}

async function postComment(taskId, content) {
  // ✅ fallback: lấy từ biến toàn cục
  taskId = taskId || window.CURRENT_TASK_ID;
  if (!taskId || taskId === "undefined") {
    console.error("❌ taskId is undefined when posting comment");
    alert("Không xác định được thẻ hiện tại (taskId undefined)");
    return;
  }

  try {
    const res = await fetch(`/api/tasks/${taskId}/comments`, {
      method: "POST",
      headers: {
        Authorization: "Bearer " + localStorage.getItem("token"),
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ content }),
    });

    if (!res.ok) throw new Error("Comment failed");
  } catch (err) {
    console.error(err);
    alert("❌ Failed to post comment");
  }
}

// ========== 🗑️ DELETE COMMENT ==========
async function deleteComment(taskId, commentId) {
  if (!confirm("🗑️ Delete this comment?")) return;
  try {
    const res = await fetch(`/api/tasks/${taskId}/comments/${commentId}`, {
      method: "DELETE",
      headers: { Authorization: "Bearer " + localStorage.getItem("token") },
    });

    if (!res.ok) throw new Error("Delete failed");
    await loadActivityFeed(taskId);
  } catch (err) {
    console.error(err);
    alert("❌ Failed to delete comment");
  }
}

// ========== 🖊️ EDIT COMMENT ==========
function editComment(taskId, commentId) {
  const commentEl = document
    .querySelector(`button[onclick="editComment(${taskId}, ${commentId})"]`)
    .closest("div.border");
  const oldText = commentEl.querySelector("p").innerText;
  commentEl.innerHTML = `
          <textarea id="edit-input-${commentId}" class="w-full border border-gray-300 rounded-md p-2 text-sm h-16">${oldText}</textarea>
          <div class="flex justify-end mt-1 gap-2">
            <button class="bg-gray-300 px-3 py-1 rounded text-xs" onclick="loadActivityFeed(${taskId})">Cancel</button>
            <button class="bg-blue-600 text-white px-3 py-1 rounded text-xs" onclick="saveEdit(${taskId}, ${commentId})">Save</button>
          </div>
        `;
}

function toggleReplyBox(id) {
  const box = document.getElementById(`reply-box-${id}`);
  if (!box) return;
  box.classList.toggle("hidden");
}

// ================= SHARE BOARD POPUP =================

const sharePopup = document.getElementById("share-board-popup");
const closeSharePopup = document.getElementById("close-share-popup");
const inviteEmail = document.getElementById("invite-email");
const inviteRole = document.getElementById("invite-role");
const inviteBtn = document.getElementById("invite-btn");
const membersList = document.getElementById("members-list");

// ✅ Các phần tử mới cho link hint & popup xác nhận xóa
const hintText = document.getElementById("share-link-hint");
const copyLinkBtn = document.getElementById("copy-link");
const deleteLinkBtn = document.getElementById("delete-link");
const deleteConfirmPopup = document.getElementById("delete-link-confirm");
const confirmDeleteBtn = document.getElementById("confirm-delete-link");

async function syncShareUI(projectId) {
  try {
    const res = await fetch(`/api/pm/invite/project/${projectId}/share/link`, {
      headers: { Authorization: "Bearer " + localStorage.getItem("token") },
    });
    if (!res.ok) throw new Error("Không thể tải trạng thái chia sẻ");
    const data = await res.json();

    const hint = document.getElementById("share-link-hint");
    const copyLinkBtn = document.getElementById("copy-link");
    const deleteLinkBtn = document.getElementById("delete-link");

    if (data.allowLinkJoin && data.inviteLink) {
      // ✅ Khi link đang bật
      hint.textContent = ""; // Không in thêm dòng mô tả
      copyLinkBtn.textContent = "Copy link";
      deleteLinkBtn.textContent = "Delete link";
      deleteLinkBtn.classList.remove("text-gray-400", "cursor-not-allowed");
      deleteLinkBtn.classList.add("text-red-600", "hover:underline");
      copyLinkBtn.disabled = false;
      deleteLinkBtn.disabled = false;
    } else {
      // 🔒 Khi link bị xóa → hiển thị “Create link”
      hint.textContent = "🔒 Link sharing is disabled.";
      hint.className = "text-xs text-gray-500 mt-1 ml-5 italic";
      copyLinkBtn.textContent = "Create link";
      deleteLinkBtn.classList.remove("text-red-600", "hover:underline");
      deleteLinkBtn.classList.add("text-gray-400", "cursor-not-allowed");
      deleteLinkBtn.disabled = true;
    }
  } catch (err) {
    console.error("❌ syncShareUI error:", err);
    const hint = document.getElementById("share-link-hint");
    hint.textContent = "⚠️ Cannot load share status.";
    hint.className = "text-xs text-red-500 mt-1 ml-5 italic";
  }
}

function openSharePopup() {
  sharePopup.classList.remove("hidden");
  loadBoardMembers(PROJECT_ID);
  syncShareUI(PROJECT_ID); // ✅ gọi ngay khi mở
}

function closeShareBoard() {
  sharePopup.classList.add("hidden");
}

closeSharePopup.addEventListener("click", closeShareBoard);

copyLinkBtn.addEventListener("click", async (e) => {
  e.preventDefault();
  try {
    const res = await fetch(
      `/api/pm/invite/project/${PROJECT_ID}/share/enable`,
      {
        method: "POST",
        headers: { Authorization: "Bearer " + localStorage.getItem("token") },
      }
    );
    if (!res.ok) throw new Error();
    const data = await res.json();
    const fullLink = `${window.location.origin}/join/${data.inviteLink}`;
    await navigator.clipboard.writeText(fullLink);
    alert(`✅ Link copied:\n${fullLink}`);
    await syncShareUI(PROJECT_ID);
  } catch {
    alert("❌ Không thể bật chia sẻ qua link");
  }
});

deleteLinkBtn.addEventListener("click", (e) => {
  e.preventDefault();
  const rect = deleteLinkBtn.getBoundingClientRect();
  deleteConfirmPopup.style.top = `${rect.bottom + window.scrollY + 8}px`;
  deleteConfirmPopup.style.left = `${rect.left + window.scrollX - 80}px`;
  deleteConfirmPopup.classList.remove("hidden");
});
confirmDeleteBtn.addEventListener("click", async () => {
  try {
    const res = await fetch(
      `/api/pm/invite/project/${PROJECT_ID}/share/disable`,
      {
        method: "DELETE",
        headers: { Authorization: "Bearer " + localStorage.getItem("token") },
      }
    );
    if (!res.ok) throw new Error();
    showToast("🔒 Link sharing disabled.");
    deleteConfirmPopup.classList.add("hidden");

    // ✅ Sau khi xóa link → đồng bộ lại UI (hiển thị "Create link")
    await syncShareUI(PROJECT_ID);
  } catch {
    showToast("❌ Failed to disable link", "error");
  }
});

document.addEventListener("click", (e) => {
  if (
    !deleteConfirmPopup.contains(e.target) &&
    !e.target.closest("#delete-link")
  ) {
    deleteConfirmPopup.classList.add("hidden");
  }
});

async function loadBoardMembers(projectId) {
  const membersList = document.getElementById("members-list");
  membersList.innerHTML = `<p class="text-gray-400 text-sm italic">Loading...</p>`;

  try {
    const res = await fetch(`/api/pm/invite/project/${projectId}`, {
      headers: { Authorization: "Bearer " + localStorage.getItem("token") },
    });

    if (!res.ok) throw new Error(`Cannot load members: ${res.status}`);

    const data = await res.json();
    // ✅ Backend trả về mảng members (List<MemberDTO>)
    const members = data.members || [];

    if (!Array.isArray(members) || members.length === 0) {
      membersList.innerHTML = `<p class="text-gray-500 text-sm italic">No members found.</p>`;
      return;
    }

    membersList.innerHTML = members
      .map(
        (m) => `
            <div class="flex justify-between items-center p-2 hover:bg-gray-50 rounded-md">
              <div class="flex items-center gap-2">
                ${renderAvatar(m)}
                <div>
                  <p class="text-sm font-medium text-gray-800">${
                    m.name || "Unnamed"
                  }</p>
                  <p class="text-xs text-gray-500">${m.email || ""}</p>
                </div>
              </div>

              <select onchange="updateMemberRole(${projectId}, ${
          m.userId
        }, this.value)"
                      class="text-xs border border-gray-300 rounded-md px-2 py-1 bg-white focus:ring-2 focus:ring-blue-400">
                ${renderRoleOptions(m.roleInProject)}
              </select>
            </div>
          `
      )
      .join("");
  } catch (err) {
    console.error("❌ loadBoardMembers error:", err);
    membersList.innerHTML = `<p class="text-red-500 text-sm">Failed to load members</p>`;
  }
}

function renderRoleOptions(currentRole) {
  const roles = ["PM", "MEMBER"]; // 🔹 chỉ còn 2 vai trò chính
  const role = (currentRole || "").toUpperCase();

  return roles
    .map(
      (r) =>
        `<option value="${r}" ${r === role ? "selected" : ""}>
          ${r === "PM" ? "Project Manager" : "Member"}
        </option>`
    )
    .join("");
}

async function updateMemberRole(projectId, userId, newRole) {
  try {
    const selectEl = event?.target;
    if (selectEl) selectEl.disabled = true; // ⏳ disable khi đang cập nhật

    const res = await fetch(
      `/api/pm/invite/project/${projectId}/member/${userId}/role?role=${newRole}`,
      {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: "Bearer " + localStorage.getItem("token"),
        },
      }
    );

    if (!res.ok) throw new Error("Failed to update role");
    const data = await res.json();

    // ✅ Reload lại danh sách thật sau khi DB cập nhật
    await loadBoardMembers(projectId);

    // ✅ Thông báo nhẹ (hoặc toast)
    console.log("✅ Vai trò đã cập nhật:", data);
  } catch (err) {
    console.error("❌ Update role failed:", err);
    alert("❌ Không thể cập nhật vai trò!");
  } finally {
    if (event?.target) event.target.disabled = false;
  }
}

inviteBtn.addEventListener("click", async () => {
  const email = inviteEmail.value.trim();
  const role = inviteRole.value || "Member";
  if (!email) return alert("❌ Vui lòng nhập email!");

  try {
    const resInvite = await fetch(
      `/api/pm/invite?projectId=${PROJECT_ID}&email=${encodeURIComponent(
        email
      )}&role=${role}`,
      {
        method: "POST",
        headers: {
          Authorization: "Bearer " + localStorage.getItem("token"),
        },
      }
    );
    if (!resInvite.ok) throw new Error("Invite failed");
    const data = await resInvite.json();
    alert(data.message || "✅ Đã mời thành viên thành công!");
    inviteEmail.value = "";
    await loadBoardMembers(PROJECT_ID);
  } catch (err) {
    console.error("❌ Error inviting:", err);
    alert("❌ Không thể mời thành viên!");
  }
});
// ================= INIT DASHBOARD OR JOIN PROJECT =================
document.addEventListener("DOMContentLoaded", async () => {
  try {
    // 1️⃣ Luôn đảm bảo user hiện tại đã đăng nhập
    await ensureCurrentUser();
    await fetchProjectRole(PROJECT_ID);

    initMemberEvents();
    initLabelEvents();
    initAttachmentEvents();
    // ✅ Khởi tạo sự kiện cho popup ngày hạn
    const dueDateDisplay = document.getElementById("due-date-display");
    if (dueDateDisplay) {
      dueDateDisplay.addEventListener("click", openDatePopup);
    }

    // 2️⃣ Kiểm tra URL: /join/<inviteLink>
    const path = window.location.pathname;
    if (path.startsWith("/join/")) {
      await handleJoinByLink(path);
      return; // ⛔ Không load dashboard khi đang join
    }

    // 3️⃣ Nếu không phải join link → hiển thị Kanban board
    await renderDashboard(PROJECT_ID);
  } catch (err) {
    console.error("🚨 Init failed:", err);
    alert(
      "❌ Cannot initialize dashboard: " + (err.message || "Unknown error")
    );
  }
});

/**
 * 📩 Hàm xử lý khi người dùng truy cập link mời
 * Ví dụ: /join/AbC123XYZ
 */
async function handleJoinByLink(path) {
  const inviteLink = path.split("/join/")[1];
  if (!inviteLink) {
    alert("⚠️ Invalid invite link!");
    return;
  }

  try {
    const res = await fetch(`/api/pm/invite/join/${inviteLink}`, {
      method: "POST",
      headers: {
        Authorization: "Bearer " + localStorage.getItem("token"),
        "Content-Type": "application/json",
      },
    });

    if (!res.ok) throw new Error(await res.text());
    const data = await res.json();

    alert(`✅ ${data.message}\n➡️ Dự án: ${data.projectName}`);

    // ✅ Chuyển hướng về dashboard dự án vừa tham gia
    if (data.projectId) {
      window.location.href = `/dashboard.html?projectId=${data.projectId}`;
    }
  } catch (err) {
    console.error("❌ Join project failed:", err);
    alert("❌ Không thể tham gia dự án qua link mời!\n" + (err.message || ""));
  }
}

// ===============================
// 🔹 AUTOCOMPLETE INVITE USER
// ===============================
const inviteInput = document.getElementById("invite-email");
const suggestionBox = document.getElementById("invite-suggestions");

let debounceInvite;
inviteInput.addEventListener("input", (e) => {
  clearTimeout(debounceInvite);
  const keyword = e.target.value.trim();
  if (!keyword) {
    suggestionBox.classList.add("hidden");
    return;
  }
  debounceInvite = setTimeout(() => loadInviteSuggestions(keyword), 250);
});
async function loadInviteSuggestions(keyword) {
  try {
    const token = localStorage.getItem("token");

    // 🔹 Chuẩn hóa header (JWT hoặc fallback)
    const headers = token
      ? { Authorization: "Bearer " + token, "Content-Type": "application/json" }
      : { "Content-Type": "application/json" };

    // 🔹 Nếu không có token, vẫn cho phép cookie xác thực (OAuth2 fallback)
    const useCredentials = !token;

    const res = await fetch(
      `/api/pm/invite/search-users?keyword=${encodeURIComponent(keyword)}`,
      {
        method: "GET",
        headers,
        ...(useCredentials ? { credentials: "include" } : {}), // chỉ thêm khi cần
      }
    );

    if (!res.ok) throw new Error(`Request failed: ${res.status}`);

    const users = await res.json();

    if (!Array.isArray(users) || users.length === 0) {
      suggestionBox.innerHTML = `<p class="p-2 text-sm text-gray-400 italic">No results found</p>`;
      suggestionBox.classList.remove("hidden");
      return;
    }

    // ✅ Render danh sách user gợi ý
    suggestionBox.innerHTML = users
      .map(
        (u) => `
        <div class="flex items-center gap-2 px-3 py-2 hover:bg-blue-50 cursor-pointer"
            onclick="selectInvite('${u.email}')">
          <img src="${u.avatarUrl || "https://i.pravatar.cc/40?u=" + u.email}" 
              class="w-6 h-6 rounded-full">
          <div>
            <p class="text-sm font-medium text-gray-700">${
              u.name || "(No name)"
            }</p>
            <p class="text-xs text-gray-500">${u.email}</p>
          </div>
        </div>
      `
      )
      .join("");
    suggestionBox.classList.remove("hidden");
  } catch (err) {
    console.error("❌ loadInviteSuggestions error:", err);
    suggestionBox.innerHTML = `
        <p class="p-2 text-sm text-red-500 italic">
          ⚠️ Cannot load suggestions.
        </p>`;
    suggestionBox.classList.remove("hidden");
  }
}

window.selectInvite = function (email) {
  inviteInput.value = email;
  suggestionBox.classList.add("hidden");
};

async function saveEdit(taskId, commentId) {
  const newText = document
    .getElementById(`edit-input-${commentId}`)
    .value.trim();
  if (!newText) return alert("Content cannot be empty");

  try {
    const res = await fetch(`/api/tasks/${taskId}/comments/${commentId}`, {
      method: "PUT",
      headers: {
        Authorization: "Bearer " + localStorage.getItem("token"),
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ content: newText }),
    });

    if (!res.ok) throw new Error("Update failed");
    await loadActivityFeed(taskId);
  } catch (err) {
    console.error(err);
    alert("❌ Failed to update comment");
  }
}

// ================== CARD CONTEXT MENU (Right Click) - FINAL FIX ==================
const contextMenu = document.getElementById("card-context-menu");
const kanbanBoardContainer = document.getElementById("kanban-board");
const deleteBtn = document.getElementById("delete-card-btn");

// ✅ Role hiện tại (render từ backend, ví dụ: PM / MEMBER / ADMIN)
window.CURRENT_ROLE = window.CURRENT_ROLE || "ROLE_MEMBER";

/**
 * 🧩 Hiển thị menu khi chuột phải lên thẻ
 */
kanbanBoardContainer.addEventListener("contextmenu", (e) => {
  const card = e.target.closest("[data-open-task]");
  if (!card) {
    contextMenu.classList.add("hidden");
    return;
  }

  e.preventDefault();
  e.stopPropagation();
  safeStop(e);

  // ✅ Lưu thông tin toàn cục
  const taskId = card.getAttribute("data-open-task");
  window.CURRENT_TASK_ID = taskId;
  window.contextMenuX = e.clientX;
  window.contextMenuY = e.clientY;

  contextMenu.setAttribute("data-task-id", taskId);

  if (deleteBtn) {
    if (window.CURRENT_ROLE === "ROLE_PM") deleteBtn.classList.remove("hidden");
    else deleteBtn.classList.add("hidden");
  }

  // --- Định vị thông minh ---
  contextMenu.classList.remove("hidden");
  const menuW = contextMenu.offsetWidth || 200;
  const menuH = contextMenu.offsetHeight || 250;
  contextMenu.classList.add("hidden");

  const screenW = window.innerWidth;
  const screenH = window.innerHeight;
  let top = e.clientY,
    left = e.clientX;
  if (left + menuW > screenW) left = e.clientX - menuW;
  if (top + menuH > screenH) top = e.clientY - menuH;

  contextMenu.style.top = `${top}px`;
  contextMenu.style.left = `${left}px`;
  contextMenu.classList.remove("hidden");
});

/**
 * 🧹 Ẩn menu khi click ra ngoài
 */
document.addEventListener("click", (e) => {
  if (!contextMenu.contains(e.target) && e.button !== 2)
    contextMenu.classList.add("hidden");
});

/**
 * 🧩 Xử lý hành động trong menu
 */
contextMenu.addEventListener("click", async (e) => {
  const button = e.target.closest("button[data-action]");
  if (!button) return;

  const action = button.getAttribute("data-action");
  const taskId = contextMenu.getAttribute("data-task-id");
  const cardElement = document.querySelector(`[data-open-task="${taskId}"]`);
  if (!taskId || !cardElement) return;

  // ✅ Lưu lại taskId toàn cục
  window.CURRENT_TASK_ID = taskId;

  // ✅ Tạo event giả để các popup dùng vị trí context menu
  const fakeEvent = {
    currentTarget: cardElement,
    target: cardElement,
    clientX: window.contextMenuX,
    clientY: window.contextMenuY,
    stopPropagation: () => {},
    preventDefault: () => {},
  };

  try {
    switch (action) {
      case "open":
        openModal(taskId);
        break;

      case "labels":
        openLabelsPopup(fakeEvent);
        break;

      case "members":
        openMemberPopup(fakeEvent);
        break;

      case "dates":
        openDatePopup(fakeEvent);
        break;
      case "mark-complete":
        try {
          await markTaskComplete(taskId);
          alert("✅ Task marked as completed!");
          await renderDashboard(PROJECT_ID);
        } catch (err) {
          console.error("❌ Mark complete failed:", err);
          alert("❌ Failed to mark task as complete");
        }
        break;

      case "copy-link":
        const link = `${window.location.origin}${window.location.pathname}?taskId=${taskId}`;
        await navigator.clipboard.writeText(link);
        alert("✅ Link copied to clipboard!");
        break;

      case "archive":
        if (!confirm("🗃️ Archive this task?")) return;
        await archiveTask(taskId);
        alert("✅ Task archived successfully!");
        await renderDashboard(PROJECT_ID);
        break;

      case "delete":
        if (window.CURRENT_ROLE !== "ROLE_PM") {
          alert("❌ Only Project Managers can delete tasks!");
          return;
        }

        if (!confirm("⚠️ Permanently delete this task? This cannot be undone!"))
          return;
        await deleteTask(taskId);
        alert("🗑️ Task deleted permanently!");
        await renderDashboard(PROJECT_ID);
        break;

      default:
        console.warn(`⚠️ Unhandled context menu action: ${action}`);
    }
  } catch (err) {
    console.error("❌ Context menu action error:", err);
    alert("❌ Operation failed: " + err.message);
  } finally {
    contextMenu.classList.add("hidden");
  }
});

// ================== API HELPERS ==================
async function archiveTask(taskId) {
  const res = await fetch(`/api/tasks/${taskId}/archive`, {
    method: "PUT",
    headers: {
      Authorization: "Bearer " + localStorage.getItem("token"),
    },
  });
  if (!res.ok) throw new Error("Archive failed");
}
async function markTaskComplete(taskId) {
  const res = await fetch(`/api/tasks/${taskId}/complete`, {
    method: "PUT",
    headers: {
      Authorization: "Bearer " + localStorage.getItem("token"),
    },
  });

  if (!res.ok) {
    if (res.status === 403) {
      const msg = await res.text();
      throw new Error(msg || "Bạn không có quyền đánh dấu hoàn thành task này");
    }
    throw new Error("Request failed: " + res.status);
  }

  const updated = await res.json();

  // 💡 Cập nhật giao diện trực tiếp (mờ card + đổi badge)
  const card = document.querySelector(`[data-open-task="${taskId}"]`);
  if (card) {
    card.style.opacity = "0.6";
    const badge = card.querySelector(".due-date-badge");
    if (badge) {
      badge.textContent = "Completed";
      badge.className =
        "due-date-badge bg-gray-200 text-gray-600 text-xs px-2 py-0.5 rounded-md";
    }
  }

  return updated;
}

async function deleteTask(taskId) {
  const res = await fetch(`/api/tasks/${taskId}`, {
    method: "DELETE",
    headers: {
      Authorization: "Bearer " + localStorage.getItem("token"),
    },
  });
  if (!res.ok) throw new Error("Delete failed");
}

async function ensureCurrentUser() {
  try {
    const res = await fetch("/api/auth/me", {
      headers: { Authorization: "Bearer " + localStorage.getItem("token") },
    });
    if (!res.ok) throw new Error("Failed to fetch /api/auth/me");
    const result = await res.json();
    const user = result.user || result;
    localStorage.setItem("currentUserId", user.userId);
    localStorage.setItem("currentUserName", user.name);
    localStorage.setItem("currentUserEmail", user.email);
    localStorage.setItem("currentUserAvatar", user.avatarUrl || "");
    return user; // ✅ THÊM DÒNG NÀY
  } catch (err) {
    console.error("❌ Cannot fetch current user:", err);
    return null; // ✅ fail-safe
  }
}

async function fetchProjectRole(projectId) {
  try {
    const res = await fetch(`/api/projects/${projectId}/role`, {
      headers: { Authorization: "Bearer " + localStorage.getItem("token") },
    });

    if (!res.ok) throw new Error("Failed to fetch project role");
    const data = await res.json();

    const role = (data.data?.role || data.role || "Member").toUpperCase();
    window.CURRENT_ROLE = "ROLE_" + role;

    console.log("🎭 Project Role Loaded:", window.CURRENT_ROLE);
  } catch (err) {
    console.error("❌ Cannot fetch project role:", err);
    window.CURRENT_ROLE = "ROLE_MEMBER"; // fallback mặc định
  }
}

Object.assign(window, {
  editComment,
  deleteComment,
  postReply,
  toggleReplyBox,
  saveEdit,
  loadActivityFeed,
});
Object.assign(window, {
  updateMemberRole,
});
