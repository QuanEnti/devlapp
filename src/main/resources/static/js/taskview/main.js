import {
  escapeHtml,
  showToast,
  showToastNotification,
  formatTime,
  formatRelativeTime,
  renderAvatar,
  getInitials,
  getColorForId,
  safeStop,
  getToken,
} from "./utils.js";

// ================== IMPORT MODULES ==================
import {
  initMemberEvents,
  openMembersPopup,
  closeMembersPopup,
} from "./members.js";

import { openDatePopup, updateDateStatus } from "./dates.js";

import { initLabelEvents, openLabelsPopup } from "./labels.js";
import { openChecklistPopup, loadChecklistItems } from "./checklist.js";

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
  const token = getToken();
  const headers = {};
  if (token) {
    headers.Authorization = "Bearer " + token;
  }
  const res = await fetch(`/api/columns/project/${projectId}`, {
    headers,
    credentials: "include",
  });
  if (!res.ok) throw new Error("Không thể tải danh sách cột");
  return await res.json();
}

async function loadTasks(projectId) {
  const token = getToken();
  const headers = {};
  if (token) {
    headers.Authorization = "Bearer " + token;
  }
  const res = await fetch(`/api/tasks/project/${projectId}`, {
    headers,
    credentials: "include",
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
        : `<div class="text-sm text-slate-400 italic">No cards yet</div>`;

      board.innerHTML += `
  <div class="kanban-list w-[300px] rounded-lg border-0 shadow-sm
              flex flex-col overflow-hidden hover:shadow-md transition-shadow duration-200"> <!-- 300px column width -->

    <!-- Header: sticky với menu button -->
    <div class="sticky top-0 z-10 bg-[#f4f5f7] backdrop-blur-sm px-3 pt-3 pb-2.5 border-b border-gray-200/60 rounded-t-lg">
      <div class="flex items-center justify-between group">
        <h3 class="font-semibold text-gray-700 text-sm truncate flex-1">${escapeHtml(
          col.name
        )}</h3>
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
    <div id="col-${
      col.columnId
    }" class="flex-1 overflow-y-auto min-h-[50px] max-h-[calc(100vh-14rem)]">
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

const TASK_CHECKBOX_BASE_CLASS =
  "task-checkbox-btn flex-shrink-0 w-4 h-4 rounded-full border-2 flex items-center justify-center transition-all duration-200";
const TASK_CHECKBOX_COMPLETED_CLASS =
  "bg-green-600 border-green-600 hover:bg-green-700 opacity-100 pointer-events-auto";
const TASK_CHECKBOX_INCOMPLETE_CLASS =
  "bg-transparent border-transparent opacity-0 pointer-events-none group-hover:opacity-100 group-hover:pointer-events-auto group-hover:border-gray-300 group-hover:bg-white";

function getColumnElementById(columnId) {
  if (!columnId && columnId !== 0) return null;
  return document.getElementById(`col-${columnId}`);
}

function renderCard(t) {
  const taskId = t.id || t.taskId;

  // Map từ assigneeName/assigneeAvatar/assigneeId sang assignees array nếu chưa có
  if (!t.assignees || !Array.isArray(t.assignees) || t.assignees.length === 0) {
    if (t.assigneeName && t.assigneeName !== "Unassigned") {
      t.assignees = [
        {
          name: t.assigneeName,
          assigneeName: t.assigneeName,
          avatarUrl: t.assigneeAvatar || "",
          userId: t.assigneeId,
          id: t.assigneeId,
          color:
            t.assigneeColor ||
            getColorForId(String(t.assigneeId || t.assigneeName)),
        },
      ];
    } else {
      t.assignees = [];
    }
  }

  // Đảm bảo assignees là array hợp lệ và có dữ liệu đầy đủ
  // Loại bỏ duplicate dựa trên userId
  if (Array.isArray(t.assignees)) {
    const seen = new Set();
    t.assignees = t.assignees
      .map((a) => ({
        name: a.name || a.assigneeName || "",
        assigneeName: a.assigneeName || a.name || "",
        avatarUrl: a.avatarUrl || a.assigneeAvatar || "",
        userId: a.userId || a.id || a.assigneeId,
        id: a.id || a.userId || a.assigneeId,
        color:
          a.color ||
          a.assigneeColor ||
          getColorForId(
            String(a.userId || a.id || a.assigneeId || a.name || a.assigneeName)
          ),
      }))
      .filter((a) => {
        if (!a.userId) return false; // Bỏ qua nếu không có userId
        if (seen.has(a.userId)) return false; // Bỏ qua nếu đã có
        seen.add(a.userId);
        return true;
      });
  }

  // 🔹 Render labels (colored tags với tên) - như ảnh 2
  const labelHtml =
    t.labels && Array.isArray(t.labels) && t.labels.length
      ? `<div class="flex flex-wrap gap-1 mb-2">
          ${t.labels
            .map(
              (l) => `
                <span class="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-semibold text-white"
                      style="background-color: ${l.color || "#94a3b8"}">
                  ${escapeHtml(l.name || "")}
                </span>
              `
            )
            .join("")}
        </div>`
      : "";

  // 🔹 Metadata icons (due date, comments, attachments, subtasks) - như ảnh 2
  const hasDeadline = t.deadline && t.deadline.trim() !== "";
  const hasAssignee =
    (t.assignees && Array.isArray(t.assignees) && t.assignees.length > 0) ||
    (t.assigneeName && t.assigneeName !== "Unassigned");
  const commentCount = t.commentCount || 0;
  const attachmentCount = t.attachmentCount || 0;
  const subtaskCount = t.subtaskCount || 0;
  const subtaskCompleted = t.subtaskCompleted || 0;

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
        dateText = deadlineDate.toLocaleDateString("en-US", {
          month: "short",
          day: "numeric",
        });
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

  // 🔹 Metadata icons row - như ảnh 2
  const metadataHtml = `
    <div class="flex items-center gap-2 mt-2 flex-wrap">
      ${dueDateHtml}
      
      ${
        commentCount > 0
          ? `
        <div class="flex items-center gap-1 text-gray-600" data-comment-count="${taskId}">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
          </svg>
          <span class="text-[10px]">${commentCount}</span>
        </div>
      `
          : ""
      }
      
      ${
        attachmentCount > 0
          ? `
        <div class="flex items-center gap-1 text-gray-600" data-attachment-count="${taskId}">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13" />
          </svg>
          <span class="text-[10px]">${attachmentCount}</span>
        </div>
      `
          : ""
      }
      
      ${
        subtaskCount > 0
          ? `
        <div class="flex items-center gap-1 text-gray-600">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span class="text-[10px]">${subtaskCompleted}/${subtaskCount}</span>
        </div>
      `
          : ""
      }
      
      ${
        hasAssignee
          ? (() => {
              const hasAssigneesArray =
                t.assignees &&
                Array.isArray(t.assignees) &&
                t.assignees.length > 0;

              // Loại bỏ duplicate và lấy tối đa 2 avatars
              const uniqueAssignees = hasAssigneesArray
                ? t.assignees.filter(
                    (a, index, self) =>
                      a.userId &&
                      index === self.findIndex((b) => b.userId === a.userId)
                  )
                : [];

              const maxAvatars = 2;
              const visibleAssignees = uniqueAssignees.slice(0, maxAvatars);
              const remainingCount = uniqueAssignees.length - maxAvatars;

              return `
        <div class="flex items-center ml-auto gap-1">
          ${
            visibleAssignees.length > 0
              ? visibleAssignees
                  .map((assignee) => {
                    const assigneeData = {
                      name: assignee.name || assignee.assigneeName || "",
                      avatarUrl: assignee.avatarUrl || "",
                      userId: assignee.userId || assignee.id,
                      color:
                        assignee.color ||
                        getColorForId(
                          String(
                            assignee.userId || assignee.id || assignee.name
                          )
                        ),
                    };

                    // Sử dụng renderAvatar với size "sm" cho card bên ngoài
                    return `<div class="relative member-avatar-chip">${renderAvatar(
                      assigneeData,
                      "sm"
                    )}</div>`;
                  })
                  .join("") +
                (remainingCount > 0
                  ? `<div class="relative member-avatar-chip member-avatar-more" title="${remainingCount} more members">
                      <div class="w-6 h-6 rounded-full flex items-center justify-center text-white text-[9px] font-semibold border-2 border-white shadow-sm bg-gray-500" style="box-shadow: 0 1px 3px rgba(9, 30, 66, 0.15);">
                        +${remainingCount}
                      </div>
                    </div>`
                  : "")
              : t.assigneeName && t.assigneeName !== "Unassigned"
              ? `
          <div class="relative member-avatar-chip">${renderAvatar(
            {
              name: t.assigneeName || "",
              avatarUrl: t.assigneeAvatar || "",
              userId: t.assigneeId,
              color:
                t.assigneeColor ||
                getColorForId(String(t.assigneeId || t.assigneeName)),
            },
            "sm"
          )}</div>
        `
              : ""
          }
        </div>
      `;
            })()
          : ""
      }
    </div>
  `;

  // 🔹 Checkbox tròn - như ảnh mẫu
  const isCompleted =
    t.status === "DONE" || t.completed === true || t.completed === "true";
  const checkboxClasses = `${TASK_CHECKBOX_BASE_CLASS} ${
    isCompleted ? TASK_CHECKBOX_COMPLETED_CLASS : TASK_CHECKBOX_INCOMPLETE_CLASS
  }`;

  const checkboxHtml = `
    <div class="flex items-center gap-2 mb-1">
      <button 
        class="${checkboxClasses}"
        data-task-id="${taskId}"
        data-completed="${isCompleted}"
        onclick="event.stopPropagation(); handleTaskCheckboxClick(this, '${taskId}');"
        title="${isCompleted ? "Đánh dấu chưa hoàn tất" : "Đánh dấu hoàn tất"}"
      >
        ${
          isCompleted
            ? `<svg xmlns="http://www.w3.org/2000/svg" class="h-[0.65rem] w-[0.65rem] text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="3">
              <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
            </svg>`
            : ""
        }
      </button>
      <p class="task-title-text font-medium text-sm leading-5 mb-0 flex-1 ${
        isCompleted ? "text-gray-500" : "text-gray-800"
      }">${escapeHtml(t.title)}</p>
    </div>
  `;

  return `
    <div data-open-task="${taskId}" data-column-id="${t.columnId || ""}"
         class="kanban-card group bg-white border-0 rounded-lg p-2.5 shadow-sm hover:shadow-md hover:bg-gray-50/50 transition-all duration-150 cursor-pointer">
      ${labelHtml}
      ${checkboxHtml}
      ${
        hasDeadline ||
        commentCount > 0 ||
        attachmentCount > 0 ||
        subtaskCount > 0 ||
        hasAssignee
          ? metadataHtml
          : ""
      }
    </div>
  `;
}

function handleCardDragStart(e) {
  const card = e.currentTarget;
  e.dataTransfer.effectAllowed = "move";
  const taskId = card.getAttribute("data-open-task");
  if (taskId) {
    e.dataTransfer.setData("taskId", taskId);
  }
  card.classList.add("opacity-50");

  const columnEl = card.closest("[id^='col-']");
  const columnId = columnEl ? columnEl.id.replace("col-", "") : "";
  if (columnId) {
    card.dataset.columnId = columnId;
    card.dataset.originalColumnId = columnId;
    card.setAttribute("data-column-id", columnId);
  }
  const nextSibling = card.nextElementSibling;
  card.dataset.originalNextTaskId = nextSibling
    ? nextSibling.getAttribute("data-open-task") || ""
    : "";
}

function handleCardDragEnd(e) {
  const card = e.currentTarget;
  // ✅ Chỉ remove opacity nếu không đang trong quá trình move (isMoving)
  // Nếu đang move, onDrop sẽ xử lý việc này
  if (!window.isMovingCard) {
    card.classList.remove("opacity-50");
  }
  // ✅ Không xóa dataset ngay, để onDrop có thể sử dụng
  // onDrop sẽ cleanup trong finally block
}

function attachCardDragHandlers(card) {
  if (!card) return;
  card.setAttribute("draggable", "true");
  card.addEventListener("dragstart", handleCardDragStart);
  card.addEventListener("dragend", handleCardDragEnd);
}

function insertTaskCardIntoColumn(columnId, task) {
  const columnEl = document.getElementById(`col-${columnId}`);
  if (!columnEl) return null;

  // ✅ Xóa empty state "Chưa có thẻ" hoặc "No cards yet" nếu có
  const emptyState = columnEl.querySelector(".text-slate-400, .text-gray-400");
  if (
    emptyState &&
    (emptyState.textContent.includes("Chưa có thẻ") ||
      emptyState.textContent.includes("No cards") ||
      emptyState.textContent.includes("No card"))
  ) {
    emptyState.remove();
  }

  const tempWrapper = document.createElement("div");
  tempWrapper.innerHTML = renderCard({
    ...task,
    columnId,
  }).trim();
  const newCard = tempWrapper.firstElementChild;
  if (!newCard) return null;

  // ✅ Chèn card vào cuối column (trước add-card-area)
  columnEl.appendChild(newCard);
  newCard.dataset.columnId = columnId;
  newCard.setAttribute("data-column-id", columnId);
  attachCardDragHandlers(newCard);
  return newCard;
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
    const token = getToken();
    const headers = {
      "Content-Type": "application/json",
    };
    if (token) {
      headers.Authorization = "Bearer " + token;
    }
    const res = await fetch("/api/tasks/quick", {
      method: "POST",
      headers,
      credentials: "include",
      body: JSON.stringify({ title, projectId: PROJECT_ID, columnId }),
    });

    if (!res.ok) throw new Error("Không thể tạo task");
    const createdTask = await res.json().catch(() => null);
    if (createdTask) {
      insertTaskCardIntoColumn(columnId, createdTask);
      // ✅ Đóng form add card sau khi thêm thành công (sẽ reset textarea)
      cancelAddCard(columnId);
    } else {
      await renderDashboard(PROJECT_ID);
    }
  } catch (err) {
    console.error("❌ Lỗi tạo task:", err);
    showToast("Không thể tạo task!", "error");
    // ✅ Nếu lỗi, giữ form mở và enable lại textarea
    if (textarea) {
      textarea.disabled = false;
    }
  } finally {
    document.getElementById(tempId)?.remove();
  }
}

document.addEventListener("click", (e) => {
  const openBtn = e.target.closest("[data-open-task]");
  if (openBtn) openModal(openBtn.getAttribute("data-open-task"));
});

async function openModal(taskId) {
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

    if (!res.ok) throw new Error("Không thể tải chi tiết task");
    const task = await res.json();

    // Update title
    const titleEl = document.getElementById("task-title-display");
    if (titleEl) titleEl.textContent = task.title || "Untitled";

    // Update column name
    const columnNameEl = document.getElementById("column-name-display");
    if (columnNameEl && task.columnName) {
      columnNameEl.textContent = task.columnName;
    }

    // Render members
    renderMembersInModal(task);

    // Render labels
    renderLabelsInModal(task);

    renderDescription(task);
    updateDateStatus(task.deadline);
    // Show/hide due date section based on whether dates are set
    const dueDateSection = document.getElementById("due-date-section");
    if (dueDateSection) {
      if (task.deadline) {
        dueDateSection.classList.remove("hidden");
      } else {
        dueDateSection.classList.add("hidden");
      }
    }
    window.CURRENT_TASK_ID = taskId;
    await loadAttachments(taskId);
    await loadActivityFeed(taskId);
    await loadChecklistItems();

    modal.classList.remove("hidden");

    // Ensure description UI is updated after modal is shown
    // Use setTimeout to ensure DOM is fully rendered
    setTimeout(() => {
      const descContentEl = document.getElementById("description-content");
      const descPlaceholderEl = document.getElementById(
        "description-placeholder"
      );
      // Check if content exists and is visible (not placeholder)
      const contentVisible =
        descContentEl &&
        window.getComputedStyle(descContentEl).display !== "none";
      const hasText =
        descContentEl && descContentEl.textContent.trim().length > 0;
      const hasContent = contentVisible && hasText;
      updateDescriptionUI(hasContent);
    }, 100);
  } catch (err) {
    console.error("❌ Lỗi khi mở modal:", err);
  }
}

function renderMembersInModal(task) {
  const membersContainer = document.getElementById("members-avatars-inline");
  if (!membersContainer) return;

  membersContainer.innerHTML = "";
  const followerIds = [];

  if (
    task.assignees &&
    Array.isArray(task.assignees) &&
    task.assignees.length > 0
  ) {
    task.assignees.forEach((assignee) => {
      const name = assignee.name || assignee.assigneeName || "?";
      const nameParts = name.split(" ");
      const initials =
        nameParts.length > 1
          ? (
              nameParts[0].charAt(0) + nameParts[nameParts.length - 1].charAt(0)
            ).toUpperCase()
          : name.charAt(0).toUpperCase();
      const color = assignee.color || "#94a3b8";

      const memberEl = document.createElement("div");
      memberEl.className =
        "w-6 h-6 rounded-full flex items-center justify-center text-white text-[10px] font-semibold shadow-sm cursor-pointer";
      memberEl.style.backgroundColor = color;
      memberEl.title = name;
      memberEl.textContent = initials;
      if (assignee.userId !== undefined && assignee.userId !== null) {
        memberEl.dataset.memberId = String(assignee.userId);
        const parsed = Number(assignee.userId);
        if (!Number.isNaN(parsed)) followerIds.push(parsed);
      }
      membersContainer.appendChild(memberEl);
      if (assignee.userId !== undefined && assignee.userId !== null) {
        const parsed = Number(assignee.userId);
        if (!Number.isNaN(parsed)) followerIds.push(parsed);
      }
    });
  } else if (task.assigneeName && task.assigneeName !== "Unassigned") {
    const name = task.assigneeName;
    const nameParts = name.split(" ");
    const initials =
      nameParts.length > 1
        ? (
            nameParts[0].charAt(0) + nameParts[nameParts.length - 1].charAt(0)
          ).toUpperCase()
        : name.charAt(0).toUpperCase();

    const memberEl = document.createElement("div");
    memberEl.className =
      "w-6 h-6 rounded-full flex items-center justify-center text-white text-[10px] font-semibold shadow-sm bg-teal-500 cursor-pointer";
    memberEl.title = name;
    memberEl.textContent = initials;
    if (task.assigneeId != null) {
      memberEl.dataset.memberId = String(task.assigneeId);
      const parsed = Number(task.assigneeId);
      if (!Number.isNaN(parsed)) followerIds.push(parsed);
    }
    membersContainer.appendChild(memberEl);
  }

  window.CURRENT_TASK_FOLLOWER_IDS = followerIds;
}

function renderLabelsInModal(task) {
  const labelsContainer = document.getElementById("labels-display-inline");
  if (!labelsContainer) return;

  labelsContainer.innerHTML = "";

  if (task.labels && Array.isArray(task.labels) && task.labels.length > 0) {
    task.labels.forEach((label) => {
      const labelEl = document.createElement("span");
      labelEl.className =
        "inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-semibold text-white";
      labelEl.style.backgroundColor = label.color || "#94a3b8";
      labelEl.textContent = label.name || "";
      if (label.labelId != null) {
        labelEl.dataset.labelId = String(label.labelId);
      }
      labelsContainer.appendChild(labelEl);
    });
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
    closeMembersPopup();
  }
});

// Event listeners cho buttons trong modal
document.addEventListener("click", (e) => {
  // Members button
  const membersBtn = e.target.closest("#open-members-btn");
  if (membersBtn) {
    const fakeEvent = {
      currentTarget: membersBtn,
      target: membersBtn,
      clientX: e.clientX,
      clientY: e.clientY,
      stopPropagation: () => {},
      preventDefault: () => {},
    };
    openMembersPopup(fakeEvent);
  }

  // Labels button
  const labelsBtn = e.target.closest("#open-labels-btn");
  if (labelsBtn) {
    const fakeEvent = {
      currentTarget: labelsBtn,
      target: labelsBtn,
      clientX: e.clientX,
      clientY: e.clientY,
      stopPropagation: () => {},
      preventDefault: () => {},
    };
    openLabelsPopup(fakeEvent);
  }

  // Dates button
  const datesBtn = e.target.closest("#open-dates-btn");
  if (datesBtn) {
    const fakeEvent = {
      currentTarget: datesBtn,
      target: datesBtn,
      clientX: e.clientX,
      clientY: e.clientY,
      stopPropagation: () => {},
      preventDefault: () => {},
    };
    openDatePopup(fakeEvent);
  }

  // Checklist button
  const checklistBtn = e.target.closest("#open-checklist-btn");
  if (checklistBtn) {
    const fakeEvent = {
      currentTarget: checklistBtn,
      target: checklistBtn,
      clientX: e.clientX,
      clientY: e.clientY,
      stopPropagation: () => {},
      preventDefault: () => {},
    };
    openChecklistPopup(fakeEvent);
  }

  // Inline buttons (for backward compatibility)
  const membersBtnInline = e.target.closest("#open-members-btn-inline");
  if (membersBtnInline) {
    const fakeEvent = {
      currentTarget: membersBtnInline,
      target: membersBtnInline,
      clientX: e.clientX,
      clientY: e.clientY,
      stopPropagation: () => {},
      preventDefault: () => {},
    };
    openMembersPopup(fakeEvent);
  }

  const labelsBtnInline = e.target.closest("#open-labels-btn-inline");
  if (labelsBtnInline) {
    const fakeEvent = {
      currentTarget: labelsBtnInline,
      target: labelsBtnInline,
      clientX: e.clientX,
      clientY: e.clientY,
      stopPropagation: () => {},
      preventDefault: () => {},
    };
    openLabelsPopup(fakeEvent);
  }

  const attachmentBtn = e.target.closest("#open-attachment-btn-inline");
  if (attachmentBtn) {
    // Scroll to attachments section
    const attachmentsSection = document.getElementById("attachments-section");
    if (attachmentsSection) {
      attachmentsSection.scrollIntoView({ behavior: "smooth", block: "start" });
    }
  }
});

// ================== DESCRIPTION ==================
const descDisplay = document.getElementById("description-display");
const descContent = document.getElementById("description-content");
const descPlaceholder = document.getElementById("description-placeholder");
const descTextDisplay = document.getElementById("description-text-display");
const descTextContent = document.getElementById("description-text-content");
const descEditor = document.getElementById("description-editor");
const descTextarea = document.getElementById("description-textarea");
const saveDescBtn = document.getElementById("save-desc-btn");
const cancelDescBtn = document.getElementById("cancel-desc-btn");

let originalDescription = "";
let currentTaskDescription = ""; // Store description from backend

function showDescriptionEditor() {
  // Get current description - check text display first, then content, then stored value
  let currentDesc = "";
  if (descTextContent && descTextContent.textContent) {
    currentDesc = descTextContent.textContent.trim();
  } else if (descContent && descContent.textContent) {
    currentDesc = descContent.textContent.trim();
  } else if (currentTaskDescription) {
    currentDesc = currentTaskDescription.trim();
  }
  originalDescription = currentDesc;

  // Hide both display modes
  descDisplay.classList.add("hidden");
  if (descTextDisplay) descTextDisplay.classList.add("hidden");

  // Show editor
  descEditor.classList.remove("hidden");
  descTextarea.value = originalDescription;

  // Remove border if description already exists, keep border if it's new
  if (currentDesc) {
    // Has description - remove border
    descTextarea.classList.remove(
      "border",
      "border-gray-300",
      "focus:ring-2",
      "focus:ring-blue-400",
      "focus:border-blue-400"
    );
    descTextarea.classList.add("border-0");
  } else {
    // No description - keep border
    descTextarea.classList.remove("border-0");
    descTextarea.classList.add(
      "border",
      "border-gray-300",
      "focus:ring-2",
      "focus:ring-blue-400",
      "focus:border-blue-400"
    );
  }

  // Auto-resize textarea
  descTextarea.style.height = "auto";
  descTextarea.style.height = Math.max(60, descTextarea.scrollHeight) + "px";

  descTextarea.focus();

  // Auto-resize on input
  descTextarea.addEventListener("input", autoResizeTextarea);

  // Setup click outside handler
  setupClickOutsideHandler();
}

function autoResizeTextarea() {
  descTextarea.style.height = "auto";
  descTextarea.style.height = Math.max(60, descTextarea.scrollHeight) + "px";
}

function hideDescriptionEditor() {
  descEditor.classList.add("hidden");
  descTextarea.removeEventListener("input", autoResizeTextarea);

  // Cleanup click outside handler
  if (clickOutsideHandler) {
    document.removeEventListener("click", clickOutsideHandler);
    clickOutsideHandler = null;
  }

  // Show appropriate display based on content
  updateDescriptionDisplay();
}

// Click on description area to edit (when empty/placeholder)
descDisplay.addEventListener("click", (e) => {
  // Don't trigger if clicking on a link or button inside
  if (e.target.tagName === "A" || e.target.tagName === "BUTTON") return;
  // Don't trigger if editor is already open
  if (!descEditor.classList.contains("hidden")) return;
  // Only allow click-to-edit when there's no content (placeholder visible)
  if (descPlaceholder && descPlaceholder.style.display !== "none") {
    showDescriptionEditor();
  }
});

// Click on description text display to edit (when has content)
if (descTextDisplay) {
  descTextDisplay.addEventListener("click", (e) => {
    // Don't trigger if clicking on a link or button inside
    if (e.target.tagName === "A" || e.target.tagName === "BUTTON") return;
    // Don't trigger if editor is already open
    if (!descEditor.classList.contains("hidden")) return;
    // Allow click-to-edit when has content
    showDescriptionEditor();
  });
}

// Edit button click handler
const editDescBtn = document.getElementById("edit-description-btn");
if (editDescBtn) {
  editDescBtn.addEventListener("click", (e) => {
    e.stopPropagation();
    showDescriptionEditor();
  });
}

// Click outside to cancel (only when editor is open)
let clickOutsideHandler = null;
function setupClickOutsideHandler() {
  if (clickOutsideHandler) {
    document.removeEventListener("click", clickOutsideHandler);
  }
  clickOutsideHandler = (e) => {
    if (
      descEditor &&
      !descEditor.contains(e.target) &&
      !descDisplay.contains(e.target) &&
      !(descTextDisplay && descTextDisplay.contains(e.target)) &&
      !descEditor.classList.contains("hidden")
    ) {
      // Only cancel if clicking outside display, text display, and editor
      if (e.target !== saveDescBtn && e.target !== cancelDescBtn) {
        cancelDescription();
        document.removeEventListener("click", clickOutsideHandler);
        clickOutsideHandler = null;
      }
    }
  };
  // Use setTimeout to avoid immediate trigger
  setTimeout(() => {
    document.addEventListener("click", clickOutsideHandler);
  }, 100);
}

function cancelDescription() {
  descTextarea.value = originalDescription;
  hideDescriptionEditor();

  // Cleanup click outside handler
  if (clickOutsideHandler) {
    document.removeEventListener("click", clickOutsideHandler);
    clickOutsideHandler = null;
  }
}

cancelDescBtn.addEventListener("click", cancelDescription);
saveDescBtn.addEventListener("click", saveDescription);

async function saveDescription() {
  const newDescription = descTextarea.value.trim();
  const taskId = window.CURRENT_TASK_ID;

  try {
    const token = getToken();
    const headers = {
      "Content-Type": "application/json",
    };
    if (token) {
      headers.Authorization = "Bearer " + token;
    }
    const res = await fetch(`/api/tasks/${taskId}/description`, {
      method: "PUT",
      headers,
      credentials: "include",
      body: JSON.stringify({ description_md: newDescription }),
    });

    if (!res.ok) {
      const msg = await res.text();
      console.warn("Server response:", res.status, msg);

      if (
        msg.includes("could not initialize proxy") ||
        msg.includes("no Session")
      ) {
        // Store updated description
        currentTaskDescription = newDescription;
        // Update both content elements
        if (descContent) descContent.textContent = newDescription;
        if (descTextContent) descTextContent.textContent = newDescription;
        updateDescriptionUI(!!newDescription);
        hideDescriptionEditor();
        return;
      }

      showToast("Không thể lưu mô tả (vui lòng thử lại).", "error");
      return;
    }

    // ✅ Trường hợp response OK
    const updated = await res.json();
    const newDesc = updated.descriptionMd || "";
    // Store updated description
    currentTaskDescription = newDesc;
    // Update both content elements
    if (descContent) descContent.textContent = newDesc;
    if (descTextContent) descTextContent.textContent = newDesc;
    updateDescriptionUI(!!newDesc);
    hideDescriptionEditor();
  } catch (err) {
    console.error("❌ Save description error:", err);
    showToast("Không thể lưu mô tả (vui lòng thử lại).", "error");
  }
}

function updateDescriptionDisplay() {
  const hasContent = descTextContent
    ? descTextContent.textContent.trim().length > 0
    : descContent
    ? descContent.textContent.trim().length > 0
    : false;

  if (hasContent) {
    // Show text display (no border), hide bordered display
    if (descTextDisplay) descTextDisplay.classList.remove("hidden");
    if (descDisplay) descDisplay.classList.add("hidden");
  } else {
    // Show bordered display (for placeholder), hide text display
    if (descTextDisplay) descTextDisplay.classList.add("hidden");
    if (descDisplay) descDisplay.classList.remove("hidden");
  }
}

function updateDescriptionUI(hasContent) {
  // Find elements fresh each time to ensure they exist
  const editBtn = document.getElementById("edit-description-btn");

  if (editBtn) {
    if (hasContent) {
      editBtn.classList.remove("hidden");
    } else {
      editBtn.classList.add("hidden");
    }
  }

  // Update display based on content
  updateDescriptionDisplay();
}

function renderDescription(task) {
  const desc = (task.descriptionMd || task.description || "").trim();
  // Store description from backend
  currentTaskDescription = desc;

  if (descContent) {
    descContent.textContent = desc;
  }
  if (descTextContent) {
    descTextContent.textContent = desc;
  }
  if (descPlaceholder) {
    descPlaceholder.style.display = desc ? "none" : "block";
  }
  if (descContent) {
    descContent.style.display = desc ? "block" : "none";
  }
  // Update UI based on content
  updateDescriptionUI(!!desc);
}
// ================== DRAG & DROP ==================
// ================== DRAG & DROP (fixed) ==================
function enableDragDrop() {
  const cards = document.querySelectorAll("[data-open-task]");
  const columns = document.querySelectorAll("[id^='col-']");

  // 🔹 set draggable + pack task id
  cards.forEach((card) => attachCardDragHandlers(card));

  // 🔹 hỗ trợ tính vị trí chèn theo chuột
  const getDragAfterElement = (container, y) => {
    const els = [
      ...container.querySelectorAll("[data-open-task]:not(.opacity-50)"),
    ];
    let closest = { offset: Number.NEGATIVE_INFINITY, element: null };
    for (const el of els) {
      const box = el.getBoundingClientRect();
      const offset = y - (box.top + box.height / 2);
      if (offset < 0 && offset > closest.offset) {
        closest = { offset, element: el };
      }
    }
    return closest.element; // null = chèn vào cuối
  };

  let isMoving = false;

  columns.forEach((col) => {
    const onDragOver = (e) => {
      e.preventDefault();
      e.stopPropagation();
      const afterEl = getDragAfterElement(col, e.clientY);
      const taskId = e.dataTransfer.getData("taskId");
      const dragged = document.querySelector(`[data-open-task='${taskId}']`);
      if (!dragged) return;

      if (afterEl == null) {
        col.appendChild(dragged);
      } else {
        col.insertBefore(dragged, afterEl);
      }
    };

    const onDrop = async (e) => {
      e.preventDefault();
      e.stopPropagation();
      if (isMoving) return;
      isMoving = true;
      window.isMovingCard = true; // ✅ Flag để dragend không reset

      const taskId = e.dataTransfer.getData("taskId");
      const colId = parseInt(col.id.replace("col-", ""), 10);
      const dragged = document.querySelector(`[data-open-task='${taskId}']`);
      if (!dragged || Number.isNaN(colId)) {
        isMoving = false;
        return;
      }

      const originalColumnId =
        dragged.dataset.originalColumnId || dragged.dataset.columnId || "";
      const originalColumnEl = getColumnElementById(originalColumnId);
      const originalNextTaskId = dragged.dataset.originalNextTaskId;

      let ordered = [...col.querySelectorAll("[data-open-task]")];
      let newIndex = ordered.indexOf(dragged);
      if (newIndex === -1) {
        ordered.push(dragged);
        newIndex = ordered.length - 1;
      }

      try {
        const token = getToken();
        const headers = {
          "Content-Type": "application/json",
        };
        if (token) {
          headers.Authorization = "Bearer " + token;
        }
        const res = await fetch(`/api/tasks/${taskId}/move`, {
          method: "PUT",
          headers,
          credentials: "include",
          body: JSON.stringify({
            targetColumnId: colId,
            newOrderIndex: newIndex,
          }),
        });

        if (!res.ok) throw new Error(`Move failed: ${res.status}`);
        const updatedTask = await res.json();

        // ✅ Đảm bảo card đã ở đúng vị trí trong column mới (đã được di chuyển trong onDragOver)
        // Nếu card chưa ở trong column mới, di chuyển nó
        if (!col.contains(dragged)) {
          const afterEl = getDragAfterElement(col, e.clientY);
          if (afterEl == null) {
            col.appendChild(dragged);
          } else {
            col.insertBefore(dragged, afterEl);
          }
        }

        // ✅ Không reload toàn bộ board, chỉ cập nhật metadata
        dragged.dataset.columnId = String(colId);
        dragged.setAttribute("data-column-id", String(colId));

        // ✅ Đảm bảo card hiển thị bình thường (remove opacity)
        dragged.classList.remove("opacity-50");

        // ✅ Re-attach drag handlers để card vẫn có thể drag tiếp
        attachCardDragHandlers(dragged);

        // ✅ Xóa empty state message trong column mới nếu có
        const emptyState = col.querySelector(".text-slate-400, .text-gray-400");
        if (
          emptyState &&
          (emptyState.textContent.includes("No cards") ||
            emptyState.textContent.includes("No card") ||
            emptyState.textContent.includes("Chưa có thẻ"))
        ) {
          emptyState.remove();
        }

        // ✅ Thêm empty state vào column cũ nếu trống
        if (
          originalColumnEl &&
          originalColumnEl !== col &&
          originalColumnEl.querySelectorAll("[data-open-task]").length === 0
        ) {
          const emptyMsg = document.createElement("div");
          emptyMsg.className = "text-sm text-slate-400 italic";
          emptyMsg.textContent = "No cards yet";
          originalColumnEl.appendChild(emptyMsg);
        }

        // ✅ Cập nhật column name trong modal nếu đang mở
        if (
          window.CURRENT_TASK_ID &&
          String(window.CURRENT_TASK_ID) === String(taskId)
        ) {
          const columnNameEl = document.getElementById("column-name-display");
          if (columnNameEl && updatedTask.columnName) {
            columnNameEl.textContent = updatedTask.columnName;
          }
          showActivitySectionIfHidden();
          await refreshActivityFeedOnly(taskId);
        }

        // ✅ Hiển thị toast thông báo thành công (optional)
        // showToast("Card moved successfully", "success");
      } catch (err) {
        console.error("⚠️ Move failed:", err);
        if (originalColumnEl) {
          const referenceNode =
            originalNextTaskId &&
            originalColumnEl.querySelector(
              `[data-open-task='${originalNextTaskId}']`
            );
          if (referenceNode) {
            originalColumnEl.insertBefore(dragged, referenceNode);
          } else {
            originalColumnEl.appendChild(dragged);
          }
        }
        showToast("❌ Không thể di chuyển thẻ. Vui lòng thử lại.", "error");
      } finally {
        delete dragged.dataset.originalColumnId;
        delete dragged.dataset.originalNextTaskId;
        isMoving = false;
        window.isMovingCard = false; // ✅ Reset flag
        // ✅ Đảm bảo card không còn opacity sau khi move xong
        dragged.classList.remove("opacity-50");
      }
    };

    // gắn vào cả cột và drop-zone (nếu có)
    col.addEventListener("dragover", onDragOver);
    col.addEventListener("drop", onDrop);

    const dropZone = col.querySelector(".drop-zone");
    if (dropZone) {
      dropZone.addEventListener("dragover", onDragOver);
      dropZone.addEventListener("drop", onDrop);
    }
  });
}

// ========== ⚙️ HELPER: SHOW ACTIVITY SECTION ==========
export function showActivitySectionIfHidden() {
  const feedEl = document.getElementById("activity-feed");
  const toggleBtn = document.getElementById("toggle-activity-btn");

  if (feedEl && feedEl.classList.contains("hidden")) {
    // Show activity feed
    feedEl.classList.remove("hidden");

    // Show activity header and HR if they exist
    const activityHr = feedEl?.previousElementSibling?.previousElementSibling;
    const activityHeader = feedEl?.previousElementSibling;

    if (
      activityHeader &&
      activityHeader.tagName === "H4" &&
      activityHeader.textContent.includes("Activity")
    ) {
      activityHeader.classList.remove("hidden");
    }
    if (activityHr && activityHr.tagName === "HR") {
      activityHr.classList.remove("hidden");
    }

    // Update toggle button text
    if (toggleBtn) {
      toggleBtn.textContent = "Hide details";
    }
  }
}

// ========== ⚙️ LOAD ACTIVITY + COMMENTS ==========
export async function loadActivityFeed(taskId) {
  const container = document.getElementById("activity-section");
  if (!container) return;
  container.innerHTML = `
    <div class="p-4 space-y-3">
      <div class="flex items-center justify-between mb-2">
        <h3 class="font-semibold text-gray-800 flex items-center gap-2 leading-none">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            class="h-5 w-5 text-gray-700"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            stroke-width="1.6"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"
            />
          </svg>
          Comments and activity
        </h3>
        <button
          id="toggle-activity-btn"
          class="text-sm text-gray-600 hover:text-gray-800 bg-gray-100 hover:bg-gray-200 px-3 py-1 rounded-md"
        >
          Hide details
        </button>
      </div>

      <div id="comment-composer">
        <div
          id="comment-editor"
          class="rounded-md border border-gray-300 focus-within:border-blue-500 focus-within:ring-2 focus-within:ring-blue-100 overflow-hidden bg-white"
        >
          <div
            class="flex items-center justify-between px-3 py-2 border-b border-gray-200 bg-gray-50"
          >
            <div class="flex items-center gap-2 text-gray-600">
              <button
                class="px-1.5 py-0.5 hover:bg-gray-200 rounded text-sm"
              >
                Aa ▾
              </button>
              <div class="h-5 w-px bg-gray-200"></div>
              <button
                class="font-bold px-1.5 py-0.5 hover:bg-gray-200 rounded"
              >
                B
              </button>
              <button
                class="italic px-1.5 py-0.5 hover:bg-gray-200 rounded"
              >
                I
              </button>
              <button
                class="px-1.5 py-0.5 hover:bg-gray-200 rounded"
              >
                ...
              </button>
              <div class="h-5 w-px bg-gray-200"></div>
              <button
                class="px-1.5 py-0.5 hover:bg-gray-200 rounded flex items-center gap-1"
                title="Bullet list"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  class="h-4 w-4"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M4 6h16M4 10h16M4 14h16M4 18h16"
                  />
                </svg>
                ▾
              </button>
              <button
                class="px-1.5 py-0.5 hover:bg-gray-200 rounded flex items-center gap-1"
                title="Add"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  class="h-4 w-4"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M12 4v16m8-8H4"
                  />
                </svg>
                ▾
              </button>
            </div>
            <div class="flex items-center gap-3">
              <button
                title="Attach link"
                class="px-1.5 py-0.5 hover:bg-gray-200 rounded"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  class="h-4 w-4"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.102 1.101"
                  />
                </svg>
              </button>
              <button
                title="Help"
                class="px-1.5 py-0.5 hover:bg-gray-200 rounded"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  class="h-4 w-4"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.79 4 4 0 1.152-.468 2.19-1.228 2.99a1.5 1.5 0 01-2.12.04l-1.06-1.06a1.5 1.5 0 01.04-2.12 2.99 2.99 0 002.99-1.228c-.548 1.165-2.03 2-3.772 2-2.21 0-4-1.79-4-4 0-1.152.468-2.19 1.228-2.99a1.5 1.5 0 012.12-.04l1.06 1.06a1.5 1.5 0 01-.04 2.12 2.99 2.99 0 00-2.99 1.228z"
                  />
                </svg>
              </button>
            </div>
          </div>

          <div class="relative">
            <textarea
              id="comment-input"
              class="w-full px-3 py-2 text-sm min-h-[64px] resize-none outline-none bg-white placeholder:text-gray-500"
              placeholder="Write a comment..."
              spellcheck="false"
            ></textarea>
          </div>
        </div>

        <div
          id="comment-actions"
          class="flex items-center gap-3 pt-3"
        >
          <button
            id="send-comment"
            class="px-3 py-1.5 rounded-md text-sm font-medium transition-colors"
            disabled
          >
            Save
          </button>
        </div>
      </div>

      <!-- Comment list -->
      <div
        id="comments-list"
        class="space-y-3 text-sm text-gray-800"
      ></div>

      <!-- Activity feed -->
      <div
        id="activity-feed"
        class="space-y-4 text-sm text-gray-700"
      >
        <p class="text-gray-400 italic">Loading...</p>
      </div>
    </div>
  `;

  // ✅ Gọi hàm init theo phong cách Trello
  initTrelloCommentComposer(taskId);

  // 🔀 Toggle show/hide activity (chỉ ẩn activity log, giữ lại comments)
  const toggleBtn = container.querySelector("#toggle-activity-btn");
  const feedEl = container.querySelector("#activity-feed");
  const commentsListEl = container.querySelector("#comments-list");

  // Đảm bảo comments-list luôn hiển thị (không bao giờ bị ẩn)
  if (commentsListEl) {
    commentsListEl.classList.remove("hidden");
  }

  if (toggleBtn && feedEl) {
    // Set text mặc định: nếu activity feed đang hiện thì "Hide details", nếu đang ẩn thì "Show details"
    const isCurrentlyHidden = feedEl.classList.contains("hidden");
    toggleBtn.textContent = isCurrentlyHidden ? "Show details" : "Hide details";

    toggleBtn.addEventListener("click", () => {
      const isCurrentlyHidden = feedEl.classList.contains("hidden");

      // Toggle: nếu đang ẩn thì hiện, nếu đang hiện thì ẩn
      feedEl.classList.toggle("hidden", !isCurrentlyHidden);

      // Ẩn/hiện phần header "Activity" và HR separator nếu có (trong over.html)
      const activityHr = feedEl?.previousElementSibling?.previousElementSibling;
      const activityHeader = feedEl?.previousElementSibling;

      if (
        activityHeader &&
        activityHeader.tagName === "H4" &&
        activityHeader.textContent.includes("Activity")
      ) {
        activityHeader.classList.toggle("hidden", !isCurrentlyHidden);
      }
      if (activityHr && activityHr.tagName === "HR") {
        activityHr.classList.toggle("hidden", !isCurrentlyHidden);
      }

      // Đảm bảo comments-list luôn hiển thị
      if (commentsListEl) {
        commentsListEl.classList.remove("hidden");
      }

      // Cập nhật text nút dựa trên trạng thái SAU khi toggle:
      // - Nếu activity feed đang ẩn (sau toggle) → hiển thị "Show details"
      // - Nếu activity feed đang hiện (sau toggle) → hiển thị "Hide details"
      const isNowHidden = feedEl.classList.contains("hidden");
      toggleBtn.textContent = isNowHidden ? "Show details" : "Hide details";
    });
  }

  try {
    const token = getToken();
    const headers = {};
    if (token) {
      headers.Authorization = "Bearer " + token;
    }
    const res = await fetch(`/api/tasks/${taskId}/activity`, {
      headers,
      credentials: "include",
    });

    const data = await res.json();
    renderCommentAndActivity(taskId, data.comments, data.activityLogs);
  } catch (err) {
    console.error(err);
    const feedEl = container.querySelector("#activity-feed");
    if (feedEl) {
      feedEl.innerHTML = `<p class="text-red-500">❌ Failed to load comments or activity</p>`;
    }
  }
}

// ========== ⚙️ REFRESH ACTIVITY FEED ONLY (không rebuild toàn bộ) ==========
export async function refreshActivityFeedOnly(taskId) {
  const feedEl = document.getElementById("activity-feed");
  if (!feedEl) {
    // Nếu chưa có activity feed, load toàn bộ
    await loadActivityFeed(taskId);
    return;
  }

  try {
    const token = getToken();
    const headers = {};
    if (token) {
      headers.Authorization = "Bearer " + token;
    }
    const res = await fetch(`/api/tasks/${taskId}/activity`, {
      headers,
      credentials: "include",
    });

    if (!res.ok) throw new Error("Failed to fetch activity");

    const data = await res.json();

    // Chỉ update phần activity feed, giữ nguyên comments
    if (feedEl && data.activityLogs) {
      const renderedActivities = data.activityLogs
        .filter((a) => !a.action.startsWith("COMMENT_"))
        .map((a) => renderSingleActivity(a));

      if (renderedActivities.length > 0) {
        feedEl.innerHTML = renderedActivities.join("");
      } else {
        feedEl.innerHTML = "";
      }
    }
  } catch (err) {
    console.error("Failed to refresh activity feed:", err);
    // Không hiển thị error để tránh làm phiền user
  }
}
/**
 * Khởi tạo trình soạn thảo comment (phong cách Trello).
 * Editor luôn hiển thị, nút Save luôn thấy nhưng disable khi không có nội dung.
 */
function initTrelloCommentComposer(taskId) {
  const ta = document.getElementById("comment-input");
  const actions = document.getElementById("comment-actions");
  const btn = document.getElementById("send-comment");
  const editor = document.getElementById("comment-editor");

  if (!ta || !actions || !btn) {
    console.error("Missing comment composer elements in HTML");
    return;
  }

  // --- Cleanup cũ (nếu có) ---
  if (window.__comment_onInput)
    ta.removeEventListener("input", window.__comment_onInput);
  if (window.__comment_onFocus)
    ta.removeEventListener("focus", window.__comment_onFocus);
  if (window.__comment_onBlur)
    ta.removeEventListener("blur", window.__comment_onBlur);
  if (window.__comment_onKeyDown)
    ta.removeEventListener("keydown", window.__comment_onKeyDown);
  if (window.__comment_onPost)
    btn.removeEventListener("click", window.__comment_onPost);
  if (window.__commentDocClick) {
    document.removeEventListener("click", window.__commentDocClick);
    window.__commentDocClick = null;
  }

  actions.classList.remove("hidden");
  actions.classList.add("flex");
  const setSaveState = (enabled) => {
    btn.disabled = !enabled;
    btn.classList.remove(
      "bg-blue-600",
      "text-white",
      "hover:bg-blue-700",
      "cursor-pointer",
      "bg-gray-200",
      "text-gray-500",
      "cursor-not-allowed"
    );
    if (enabled) {
      btn.classList.add(
        "bg-blue-600",
        "text-white",
        "hover:bg-blue-700",
        "cursor-pointer"
      );
    } else {
      btn.classList.add("bg-gray-200", "text-gray-500", "cursor-not-allowed");
    }
  };
  setSaveState(false);

  window.__comment_onFocus = () => {
    editor?.classList.add("ring-2", "ring-blue-200", "border-blue-500");
  };
  ta.addEventListener("focus", window.__comment_onFocus);

  window.__comment_onInput = () => {
    const hasText = ta.value.trim().length !== 0;
    setSaveState(hasText);
    if (hasText) {
      editor?.classList.add("border-gray-300");
    }
  };
  ta.addEventListener("input", window.__comment_onInput);

  window.__comment_onBlur = () => {
    editor?.classList.remove("ring-2", "ring-blue-200", "border-blue-500");
    if (ta.value.trim().length === 0) {
      setSaveState(false);
      editor?.classList.add("border-gray-300");
    }
  };
  ta.addEventListener("blur", window.__comment_onBlur);

  // Ctrl + Enter
  window.__comment_onKeyDown = (e) => {
    if ((e.ctrlKey || e.metaKey) && e.key === "Enter" && !btn.disabled)
      btn.click();
  };
  ta.addEventListener("keydown", window.__comment_onKeyDown);

  // Click Save
  window.__comment_onPost = async () => {
    const raw = ta.value.trim();
    if (!raw) return;
    const content = upgradeLinksToCards(raw);
    setSaveState(false);
    btn.textContent = "Saving...";
    try {
      await postComment(taskId, content);
    } finally {
      btn.textContent = "Save";
      setSaveState(false);
    }
  };
  btn.addEventListener("click", window.__comment_onPost);
}
const LINK_CARD_RX = /\[LINK_CARD:([^:]+):([^:]+):([^\]]+)\]/g;
const SINGLE_URL_RX = /^\s*https?:\/\/\S+\s*$/i;

function decodeLinkCardToUrl(txt) {
  return txt.replace(LINK_CARD_RX, (_, encUrl) => {
    try {
      return decodeURIComponent(encUrl);
    } catch {
      return _;
    }
  });
}

function shouldHideTextarea(txt) {
  // Ẩn khi nội dung chỉ là 1 URL hoặc 1 LINK_CARD (giống Trello)
  return SINGLE_URL_RX.test(txt) || /^\s*\[LINK_CARD:[\s\S]+\]\s*$/.test(txt);
}

// ========== 💬 RENDER COMMENTS + ACTIVITY (SEPARATED) ==========
function renderCommentAndActivity(taskId, comments, activities) {
  const commentsList = document.getElementById("comments-list");
  const activityFeed = document.getElementById("activity-feed");

  // ✅ Render comments vào comments-list (luôn hiển thị, không bị ẩn)
  if (commentsList) {
    if (comments && comments.length > 0) {
      // Sắp xếp comments theo thời gian (mới nhất ở trên)
      const sortedComments = [...comments].sort((a, b) => {
        const timeA = new Date(a.createdAt).getTime();
        const timeB = new Date(b.createdAt).getTime();
        return timeB - timeA;
      });

      const renderedComments = sortedComments
        .map((c) => renderSingleComment(taskId, c))
        .filter((html) => html && html.trim() !== "");

      if (renderedComments.length > 0) {
        commentsList.innerHTML = renderedComments.join("");
        ensureCommentInteractionHandlers(commentsList);
        // Đảm bảo comments-list luôn hiển thị
        commentsList.classList.remove("hidden");
      } else {
        commentsList.innerHTML = "";
        // Vẫn hiển thị ngay cả khi không có comments
        commentsList.classList.remove("hidden");
      }
    } else {
      commentsList.innerHTML = "";
      // Vẫn hiển thị ngay cả khi không có comments
      commentsList.classList.remove("hidden");
    }
  }

  // ✅ Render activities vào activity-feed (có thể ẩn/hiện bằng nút Hide details)
  if (activityFeed) {
    if (activities && activities.length > 0) {
      const filteredActivities = activities.filter(
        (a) => !a.action.startsWith("COMMENT_") // loại bỏ log comment nội bộ
      );

      if (filteredActivities.length > 0) {
        // Sắp xếp activities theo thời gian (mới nhất ở trên)
        const sortedActivities = [...filteredActivities].sort((a, b) => {
          const timeA = new Date(a.createdAt).getTime();
          const timeB = new Date(b.createdAt).getTime();
          return timeB - timeA;
        });

        const renderedActivities = sortedActivities
          .map((a) => renderSingleActivity(a))
          .filter((html) => html && html.trim() !== "");

        if (renderedActivities.length > 0) {
          activityFeed.innerHTML = renderedActivities.join("");
        } else {
          activityFeed.innerHTML = "";
        }
      } else {
        activityFeed.innerHTML = "";
      }
    } else {
      activityFeed.innerHTML = "";
    }
  }
}

// ========== 💬 RENDER SINGLE COMMENT ==========
function renderSingleComment(taskId, c) {
  const currentUserId = Number(localStorage.getItem("currentUserId"));
  const currentUserEmail = localStorage.getItem("currentUserEmail");
  const currentUserName = localStorage.getItem("currentUserName") || "Unknown";

  // ✅ SỬA LỖI: Gọi highlightMentions để xử lý @tags
  const safeContent = highlightMentions(c.content, c.mentionsJson);
  const isOwner =
    Number(c.userId) === currentUserId ||
    (c.userEmail && c.userEmail === currentUserEmail);
  const rawAttr = encodeURIComponent(c.content || "");

  return `
    <div class="mb-5 flex items-start gap-3 comment-item" data-comment-id="${
      c.commentId
    }" data-task-id="${taskId}" data-comment-owner="${isOwner ? "1" : "0"}">
      <img src="${
        c.userAvatar || "https://i.pravatar.cc/30"
      }" class="w-7 h-7 rounded-full flex-shrink-0">
      <div class="flex-1">
        <div class="flex items-baseline gap-2 text-xs">
          <span class="font-semibold text-gray-900">${c.userName}</span>
          <a href="#" class="font-medium text-blue-600 hover:underline">${formatTime(
            c.createdAt
          )}</a>
        </div>
        <div class="mt-2 border border-gray-200 rounded-lg bg-white px-3 py-2 shadow-sm comment-bubble ${
          isOwner ? "comment-bubble-editable cursor-text" : ""
        }" data-comment-raw="${rawAttr}" ${
    isOwner ? 'data-comment-editable="true"' : ""
  }>
          <div class="comment-content text-xs leading-relaxed text-gray-800 break-words">
            ${safeContent}
          </div>
        </div>
        <div class="mt-1 flex gap-3 text-[11px] font-medium text-blue-600 comment-action-bar">
          ${
            isOwner
              ? `
                <button onclick="editComment(${taskId}, ${c.commentId})" class="hover:underline">Edit</button>
                <button onclick="deleteComment(${taskId}, ${c.commentId})" class="hover:underline">Delete</button>
              `
              : `<button onclick="toggleReplyBox(${c.commentId})" class="hover:underline">Reply</button>`
          }
        </div>
        <div id="reply-box-${c.commentId}" class="hidden mt-3 space-y-2">
          <textarea id="reply-input-${c.commentId}"
            class="w-full border border-gray-300 rounded-md p-2 text-xs h-16 focus:ring-2 focus:ring-blue-400"
            placeholder="Write a reply..."></textarea>
          <div class="flex gap-2">
            <button onclick="postReply(${taskId}, ${c.commentId})"
              class="bg-blue-600 hover:bg-blue-700 text-white px-3 py-1 rounded-md text-xs font-medium">Reply</button>
            <button onclick="toggleReplyBox(${c.commentId})"
              class="bg-gray-200 hover:bg-gray-300 text-gray-700 px-3 py-1 rounded-md text-xs font-medium">Cancel</button>
          </div>
        </div>
        ${renderReplies(taskId, c.replies || [])}
      </div>
    </div>
  `;
}

// ========== 💬 COMMENTS (BACKWARD COMPATIBILITY) ==========
// ==========================
// 🧩 COMMENT RENDER (TRELLO STYLE)
// ==========================
function renderComments(taskId, comments) {
  return comments.map((c) => renderSingleComment(taskId, c)).join("");
}

// ========== 💬 RENDER REPLIES ==========
function renderReplies(taskId, replies, depth = 0) {
  const currentUserId = Number(localStorage.getItem("currentUserId"));
  const currentUserEmail = localStorage.getItem("currentUserEmail");

  // Giới hạn độ sâu tối đa để tránh quá sâu
  const maxDepth = 5;
  if (depth > maxDepth) {
    console.warn("⚠️ Reply depth exceeded maximum, stopping recursion");
    return "";
  }

  // Tính margin-left dựa trên depth (mỗi cấp thêm 2rem = 32px)
  const marginLeft = 8 + depth * 8; // ml-8 cho cấp 0, ml-16 cho cấp 1, ml-24 cho cấp 2, etc.

  return replies
    .map((r) => {
      // Lấy replyId - có thể là replyId, id, hoặc commentId (nếu reply được lưu như comment)
      const replyId = r.replyId || r.id || r.commentId;
      if (!replyId) {
        console.warn("⚠️ Reply missing ID:", r);
        return ""; // Skip reply without ID
      }

      const isOwner =
        Number(r.userId) === currentUserId ||
        (r.userEmail && r.userEmail === currentUserEmail);
      const rawAttr = encodeURIComponent(r.content || "");

      return `
        <div class="flex items-start gap-3 mt-4 reply-item" style="margin-left: ${
          marginLeft * 0.25
        }rem;" data-reply-id="${replyId}" data-task-id="${taskId}" data-reply-owner="${
        isOwner ? "1" : "0"
      }" data-reply-depth="${depth}">
          <img src="${
            r.userAvatar || "https://i.pravatar.cc/28"
          }" class="w-7 h-7 rounded-full flex-shrink-0">
          <div class="flex-1 min-w-0">
            <div class="flex items-baseline gap-2 text-[11px]">
              <span class="font-semibold text-gray-900">${r.userName}</span>
              <a href="#" class="font-medium text-blue-600 hover:underline">${formatTime(
                r.createdAt
              )}</a>
            </div>
            <div class="mt-2 border border-gray-200 rounded-lg bg-white px-3 py-2 shadow-sm reply-bubble ${
              isOwner ? "reply-bubble-editable cursor-text" : ""
            }" data-reply-raw="${rawAttr}" ${
        isOwner ? 'data-reply-editable="true"' : ""
      }>
              <div class="comment-content text-xs text-gray-800 break-words">
                ${highlightMentions(r.content, r.mentionsJson)}
              </div>
            </div>
            <div class="mt-1 flex gap-3 text-[11px] font-medium text-blue-600 reply-action-bar">
              ${
                isOwner
                  ? `
                    <button onclick="editReply(${taskId}, ${replyId})" class="hover:underline">Edit</button>
                    <button onclick="deleteReply(${taskId}, ${replyId})" class="hover:underline">Delete</button>
                  `
                  : `<button onclick="toggleReplyBox(${replyId})" class="hover:underline">Reply</button>`
              }
            </div>
            <div id="reply-box-${replyId}" class="hidden mt-3 space-y-2">
              <textarea id="reply-input-${replyId}"
                class="w-full border border-gray-300 rounded-md p-2 text-xs h-16 focus:ring-2 focus:ring-blue-400"
                placeholder="Write a reply..."></textarea>
              <div class="flex gap-2">
                <button onclick="postReply(${taskId}, ${replyId})"
                  class="bg-blue-600 hover:bg-blue-700 text-white px-3 py-1 rounded-md text-xs font-medium">Reply</button>
                <button onclick="toggleReplyBox(${replyId})"
                  class="bg-gray-200 hover:bg-gray-300 text-gray-700 px-3 py-1 rounded-md text-xs font-medium">Cancel</button>
              </div>
            </div>
            ${
              r.replies && r.replies.length > 0
                ? renderReplies(taskId, r.replies, depth + 1)
                : ""
            }
          </div>
        </div>
      `;
    })
    .join("");
}

function ensureCommentInteractionHandlers(feed) {
  if (!feed) return;
  if (!feed.__commentBubbleListenerAttached) {
    feed.addEventListener("click", (e) => {
      // Xử lý click vào comment bubble để edit
      const commentBubble = e.target.closest(
        ".comment-bubble[data-comment-editable='true']"
      );
      if (commentBubble) {
        if (e.target.closest("button") || e.target.closest(".mention-chip")) {
          return;
        }
        // Prevent selecting text from immediately triggering edit
        const selection = window.getSelection();
        if (selection && selection.toString().length > 0) return;

        const container = commentBubble.closest("[data-comment-id]");
        if (!container) return;
        const commentId = container.getAttribute("data-comment-id");
        const taskId =
          container.getAttribute("data-task-id") || window.CURRENT_TASK_ID;
        editComment(taskId, commentId);
        return;
      }

      // Xử lý click vào reply bubble để edit (giống như comment)
      const replyBubble = e.target.closest(
        ".reply-bubble[data-reply-editable='true']"
      );
      if (replyBubble) {
        // Bỏ qua nếu click vào button (để onclick tự xử lý) hoặc mention chip
        if (e.target.closest("button") || e.target.closest(".mention-chip")) {
          return; // Let button's onclick handle it
        }
        // Bỏ qua nếu click vào action bar
        if (e.target.closest(".reply-action-bar")) {
          return; // Let button's onclick handle it
        }
        // Prevent selecting text from immediately triggering edit
        const selection = window.getSelection();
        if (selection && selection.toString().length > 0) return;

        const container = replyBubble.closest("[data-reply-id]");
        if (!container) return;
        const replyId = container.getAttribute("data-reply-id");
        if (!replyId || replyId === "undefined" || replyId === "null") {
          console.error("❌ Invalid replyId from container:", replyId);
          return;
        }
        const taskId =
          container.getAttribute("data-task-id") || window.CURRENT_TASK_ID;
        editReply(taskId, replyId);
      }
    });
    feed.__commentBubbleListenerAttached = true;
  }
}
// 🔹 Escape regex ký tự đặc biệt
function escapeRegex(str) {
  return str.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
// 🎨 Highlight @mentions, @board/@card, hoặc email
// Process links in comment content
function processCommentLinks(text) {
  if (!text) return "";

  // Check if text already contains HTML (already processed)
  if (/<[^>]+>/.test(text)) {
    return text;
  }

  let result = text;
  const linkCardPlaceholders = [];
  let placeholderIndex = 0;

  // Step 1: Extract LINK_CARD format before escaping
  // Replace LINK_CARD with placeholder to preserve it
  result = result.replace(
    /\[LINK_CARD:([^:]+):([^:]+):([^\]]+)\]/g,
    (match, url, iconHtml, title) => {
      try {
        const decodedIcon = decodeURIComponent(iconHtml);
        const decodedUrl = decodeURIComponent(url);
        const decodedTitle = decodeURIComponent(title);

        // Store the HTML for this placeholder
        const placeholder = `__LINK_CARD_${placeholderIndex}__`;
        const cardId = `card-${placeholderIndex}-${Math.random()
          .toString(36)
          .substr(2, 9)}`;

        // Add onerror handler to convert card to blue link if icon fails
        const iconWithErrorHandler = decodedIcon.replace(
          /<img([^>]*?)>/,
          (imgTag, attrs) => {
            const cleanAttrs = attrs.replace(/\s*onerror="[^"]*"/g, "");
            return `<img${cleanAttrs} onerror="handleCardIconError('${cardId}')" />`;
          }
        );

        linkCardPlaceholders[placeholderIndex] = `<a href="${escapeHtml(
          decodedUrl
        )}" target="_blank" id="${cardId}" class="link-card inline-flex items-center gap-2 border border-gray-200 rounded-md px-3 py-1.5 bg-white hover:bg-gray-50 transition cursor-pointer no-underline my-1">
          <span class="flex-shrink-0">${iconWithErrorHandler}</span>
          <span class="text-blue-600 font-normal text-sm">${escapeHtml(
            decodedTitle
          )}</span>
        </a>`;
        placeholderIndex++;
        return placeholder;
      } catch (e) {
        console.warn("Failed to decode LINK_CARD:", e);
        return match;
      }
    }
  );

  // Step 2: Escape the rest of the text
  result = escapeHtml(result);

  // Step 3: Restore LINK_CARD placeholders
  linkCardPlaceholders.forEach((html, index) => {
    result = result.replace(`__LINK_CARD_${index}__`, html);
  });

  // Step 4: Process regular URLs (like image 2) - convert to blue links
  // Only match URLs that are not already in HTML tags
  result = result.replace(
    /(^|[\s>])(https?:\/\/[^\s<>"']+)(?=[\s<.,!?]|$)/g,
    (match, before, url) => {
      // Skip if already inside HTML tag
      if (match.includes("<a") || match.includes("</a>")) return match;
      return (
        before +
        `<a href="${escapeHtml(
          url
        )}" target="_blank" class="text-blue-600 hover:underline">${escapeHtml(
          url
        )}</a>`
      );
    }
  );

  return result;
}

// 🎨 Highlight @mentions (ĐÃ SỬA ĐỂ HIỂN THỊ EMAIL NHƯ ẢNH 3)
function highlightMentions(text, mentionsJson) {
  if (!text) return "";

  // Bước 1: Vẫn xử lý các link card (Trello, Youtube...)
  text = processCommentLinks(text);

  try {
    const mentions = mentionsJson ? JSON.parse(mentionsJson) : [];

    if (Array.isArray(mentions) && mentions.length > 0) {
      mentions.forEach((m) => {
        const email = m.email || "";
        const name = m.name || "";
        const safeEmail = escapeRegex(email);
        const safeName = escapeRegex(name.trim()).replace(/\s+/g, "\\s+");
        const isSpecial = email === "@card" || email === "@board";

        // 🟣 Tag đặc biệt: @card / @board (Giữ nguyên)
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

        // 🟢 Email thật hoặc mention @Tên → hiển thị chip highlight
        const regexEmail = new RegExp(`${safeEmail}(?=[\\s,.!?]|$)`, "gu");
        const regexName = new RegExp(`@${safeName}(?=[\\s,.!?]|$)`, "gu");

        const displayText = name ? `@${name}` : email;
        const replacementHtml = `<span class="mention-chip mention-chip-user" data-email="${escapeHtml(
          email
        )}">${escapeHtml(displayText)}</span>`;

        // Thay thế cả @Name và email bằng link email mới
        text = text.replace(regexName, replacementHtml);
        text = text.replace(regexEmail, replacementHtml);
      });

      return text;
    }
  } catch (err) {
    console.warn("⚠️ Mentions parse failed:", err);
  }

  // 🧩 Fallback: highlight email dạng chip
  return (
    text
      .replace(
        /\b([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,})\b/g,
        `<span class="mention-chip mention-chip-user" data-email="$1">$1</span>`
      )
      // highlight @card hoặc @board (Giữ nguyên)
      .replace(
        /@(?:card|board)\b/g,
        (match) =>
          `<span class="mention-chip" data-type="special">${match}</span>`
      )
  );
}

async function openMentionProfile(email) {
  try {
    const token = getToken();
    const headers = {};
    if (token) {
      headers.Authorization = "Bearer " + token;
    }
    const res = await fetch(
      `/api/users/by-email/${encodeURIComponent(email)}`,
      {
        headers,
        credentials: "include",
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
    showToast("Không thể tải thông tin người dùng này!", "error");
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

document.addEventListener("input", async (e) => {
  if (e.target?.id === "comment-input") {
    let inputValue = e.target.value;
    const cursorPos = e.target.selectionStart;

    const hasLinkCard = /\[LINK_CARD:/.test(inputValue);
    if (hasLinkCard) {
      const beforeReplace = inputValue;
      inputValue = inputValue.replace(
        /\[LINK_CARD:([^:]+):([^:]+):([^\]]+)\]/g,
        (match, url) => {
          try {
            return decodeURIComponent(url);
          } catch (e) {
            return match;
          }
        }
      );
      // Update input if changed, but preserve cursor position
      if (inputValue !== beforeReplace) {
        const lengthDiff = inputValue.length - beforeReplace.length;
        e.target.value = inputValue;
        // Adjust cursor position based on length difference
        const newCursorPos = Math.max(
          0,
          Math.min(cursorPos + lengthDiff, inputValue.length)
        );
        e.target.setSelectionRange(newCursorPos, newCursorPos);
      }
    }

    const text = inputValue.slice(0, cursorPos);
    const match = text.match(/@([\wÀ-ỹ\s]*)$/u);
    if (match) {
      const keyword = match[1].trim();

      await loadMentionSuggestions(keyword);
    } else {
      document.getElementById("mention-suggestions")?.classList.add("hidden");
    }
  }
});

document.addEventListener("paste", (e) => {
  if (e.target?.id === "comment-input") {
  }
});

function updateCommentPreview(text) {
  const preview = document.getElementById("comment-preview");
  const editToggle = document.getElementById("comment-edit-toggle");
  if (preview) preview.classList.add("hidden");
  if (editToggle) editToggle.classList.add("hidden");
}

// Process links for preview - only show blue links, no cards
function processCommentLinksForPreview(text) {
  if (!text) return "";

  // Escape HTML first
  let result = escapeHtml(text);

  // Convert LINK_CARD format back to URL (show as blue link only)
  // This handles cases where LINK_CARD format is already in the text
  result = result.replace(
    /\[LINK_CARD:([^:]+):([^:]+):([^\]]+)\]/g,
    (match, url, iconHtml, title) => {
      try {
        const decodedUrl = decodeURIComponent(url);
        // Return the URL as plain text, will be converted to link below
        return decodedUrl;
      } catch (e) {
        // If decode fails, try to extract URL from the match
        return match;
      }
    }
  );

  // Process all URLs - convert to blue links
  // Match URLs that are not already inside HTML tags
  result = result.replace(
    /(^|[\s>])(https?:\/\/[^\s<>"']+)(?=[\s<.,!?]|$)/g,
    (match, before, url) => {
      // Skip if already inside HTML tag or already a link
      if (
        match.includes("<a") ||
        match.includes("</a>") ||
        match.includes("href=")
      ) {
        return match;
      }
      return (
        before +
        `<a href="${escapeHtml(
          url
        )}" target="_blank" class="text-blue-600 hover:underline">${escapeHtml(
          url
        )}</a>`
      );
    }
  );

  return result;
}

// Handle card icon error - convert card to blue link if favicon fails to load
window.handleCardIconError = function (cardId) {
  const card = document.getElementById(cardId);
  if (!card) return;

  const url = card.href;
  const titleSpan = card.querySelector("span:last-child");
  const title = titleSpan ? titleSpan.textContent : url;

  // Convert card to simple blue link
  card.outerHTML = `<a href="${escapeHtml(
    url
  )}" target="_blank" class="text-blue-600 hover:underline">${escapeHtml(
    title
  )}</a>`;
};

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

// ========== 📜 RENDER SINGLE ACTIVITY (TRELLO STYLE) ==========
function renderSingleActivity(a) {
  let msg = "";
  let data = {};
  try {
    data = a.dataJson ? JSON.parse(a.dataJson) : {};
  } catch {
    data = {};
  }

  // ✅ Format message giống Trello
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
      // ✅ Format giống Trello: không in nghiêng (italic) cho tên cột
      msg = `moved this card from <b>${escapeHtml(
        data.from || "Unknown"
      )}</b> to <b>${escapeHtml(data.to || "Unknown")}</b>`;
      break;

    case "ATTACH_LINK":
      msg = `attached ${renderActivityLinkCard(
        data.link || data.url || "#",
        data.name || data.link || "link"
      )} to this card`;
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
    case "ADD_MEMBER":
      // ✅ Format giống Trello: "added **username** to this card"
      const memberName =
        data.memberName || data.assigneeName || data.username || "a member";
      msg = `added <b>${escapeHtml(memberName)}</b> to this card`;
      break;

    case "REMOVE_MEMBER":
      // ✅ Format giống Trello: "removed **username** from this card"
      const removedName =
        data.memberName || data.assigneeName || data.username || "a member";
      msg = `removed <b>${escapeHtml(removedName)}</b> from this card`;
      break;

    case "SET_DUE_DATE":
    case "UPDATE_DATES":
      // ✅ Format giống Trello: "set this card to be due Nov 2 at 11:34 AM"
      // Kiểm tra nếu có deadline và là string hợp lệ (không phải "null" hoặc null)
      if (
        data.deadline &&
        data.deadline !== "null" &&
        data.deadline !== null &&
        data.deadline !== "N/A"
      ) {
        try {
          const deadlineDate = new Date(data.deadline);
          if (!isNaN(deadlineDate.getTime())) {
            // Format date: "Nov 2"
            const dateStr = deadlineDate.toLocaleDateString("en-US", {
              month: "short",
              day: "numeric",
            });
            // Format time: "11:34 AM"
            const timeStr = deadlineDate.toLocaleTimeString("en-US", {
              hour: "numeric",
              minute: "2-digit",
              hour12: true,
            });
            // Date phrase highlighted like in comments (blue + underline) and bold
            const duePhrase = `<a href="#" class="text-blue-600 hover:underline"><b>${dateStr} at ${timeStr}</b></a>`;
            msg = `set this card to be due ${duePhrase}`;
          } else {
            // Invalid date, skip this activity
            return "";
          }
        } catch (e) {
          // Parse error, skip this activity
          return "";
        }
      } else {
        // ✅ Skip nếu deadline là null hoặc không hợp lệ (không hiển thị "updated dates: start null → deadline null")
        return "";
      }
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

  // ✅ Skip nếu message rỗng (ví dụ: UPDATE_DATES với cả hai null)
  if (!msg) return "";

  // ✅ Sử dụng avatar giống comment (img tag)
  const actorAvatar =
    a.actorAvatar || a.userAvatar || "https://i.pravatar.cc/30";
  const actorName = a.actorName || a.userName || "Unknown";

  // ✅ Format giống Trello: không có border, layout giống comment nhưng đơn giản hơn
  return `
    <div class="mb-3 flex items-start gap-3">
      <!-- Avatar giống comment -->
      <div class="flex-shrink-0">
        <img src="${actorAvatar}" class="w-7 h-7 rounded-full" alt="${escapeHtml(
    actorName
  )}">
      </div>
      
      <!-- Content -->
      <div class="flex-1 min-w-0">
        <div class="text-xs leading-relaxed" style="color: #172b4d; font-weight: 400; text-decoration: none;">
          <span style="color:#172b4d; text-decoration:none;"><b>${escapeHtml(
            actorName
          )}</b></span> <span style="text-decoration:none; color:#172b4d;">${msg}</span>
        </div>
        <span class="text-[11px] mt-0.5 inline-block" style="color: #000; text-decoration: none; font-weight: 400;">${formatTime(
          a.createdAt
        )}</span>
      </div>
    </div>
  `;
}

function renderActivityLinkCard(url, displayText) {
  if (!url) {
    return `<span style="color: #172b4d;">${escapeHtml(
      displayText || "link"
    )}</span>`;
  }

  let iconData = null;
  try {
    iconData = getWebsiteIconForComment(url);
  } catch {
    iconData = null;
  }

  const safeUrl = escapeHtml(url);
  const safeText = escapeHtml(displayText || url);

  if (iconData) {
    const cardId = `activity-card-${Math.random().toString(36).slice(2, 10)}`;
    const iconHtml = iconData.icon.replace(/<img([^>]*?)>/, (imgTag, attrs) => {
      const cleanAttrs = attrs.replace(/\s*onerror="[^"]*"/g, "");
      return `<img${cleanAttrs} onerror="handleCardIconError('${cardId}')" />`;
    });

    return `<a href="${safeUrl}" target="_blank" id="${cardId}"
              class="inline-flex items-center gap-2 border border-gray-200 rounded-md px-2.5 py-1.5 bg-[#f7f8fa] hover:bg-[#ebecf0] transition cursor-pointer no-underline"
              style="text-decoration: none; color: #172b4d;">
              <span class="flex-shrink-0">${iconHtml}</span>
              <span class="text-[#172b4d] font-medium text-sm" style="text-decoration: none;">${safeText}</span>
            </a>`;
  }

  return `<a href="${safeUrl}" target="_blank" style="color: #172b4d; text-decoration: none; font-weight: 500;">${safeText}</a>`;
}

// ========== 📜 ACTIVITY (BACKWARD COMPATIBILITY) ==========
function renderActivities(activities) {
  return activities
    .filter((a) => !a.action.startsWith("COMMENT_")) // loại bỏ log comment nội bộ
    .map((a) => renderSingleActivity(a))
    .join("");
}

// ========== 🔁 REPLY TO COMMENT OR REPLY ==========
async function postReply(taskId, parentId) {
  const input = document.getElementById(`reply-input-${parentId}`);
  const content = input.value.trim();
  if (!content) {
    showToast("Please enter a reply", "error");
    return;
  }

  try {
    const res = await fetch(`/api/tasks/${taskId}/comments/${parentId}/reply`, {
      method: "POST",
      headers: (() => {
        const token = getToken();
        const h = { "Content-Type": "application/json" };
        if (token) h.Authorization = "Bearer " + token;
        return h;
      })(),
      credentials: "include",
      body: JSON.stringify({ content }),
    });

    if (!res.ok) throw new Error("Reply failed");

    const newReply = await res.json();

    // Kiểm tra xem parentId là commentId hay replyId
    const commentContainer = document.querySelector(
      `[data-comment-id="${parentId}"]`
    );
    const replyContainer = document.querySelector(
      `[data-reply-id="${parentId}"]`
    );

    const replyHtml = renderReplies(taskId, [newReply]);

    if (replyContainer) {
      // Reply cho reply - thêm sau reply container
      const replyBox = replyContainer.querySelector(`#reply-box-${parentId}`);
      if (replyBox) {
        replyBox.insertAdjacentHTML("afterend", replyHtml);
      } else {
        // Thêm sau reply action bar
        const actionBar = replyContainer.querySelector(".reply-action-bar");
        if (actionBar) {
          actionBar.insertAdjacentHTML("afterend", replyHtml);
        } else {
          replyContainer.insertAdjacentHTML("afterend", replyHtml);
        }
      }
    } else if (commentContainer) {
      // Reply cho comment - thêm sau comment container
      const replyBox = commentContainer.querySelector(`#reply-box-${parentId}`);
      if (replyBox) {
        replyBox.insertAdjacentHTML("afterend", replyHtml);
      } else {
        const actionBar = commentContainer.querySelector(".comment-action-bar");
        if (actionBar) {
          actionBar.insertAdjacentHTML("afterend", replyHtml);
        } else {
          const bubble = commentContainer.querySelector(".comment-bubble");
          if (bubble) {
            bubble
              .closest(".flex-1")
              .insertAdjacentHTML("beforeend", replyHtml);
          }
        }
      }
    }

    // Đảm bảo event handlers được attach
    const commentsList = document.getElementById("comments-list");
    if (commentsList) {
      ensureCommentInteractionHandlers(commentsList);
    }

    // Clear input và đóng reply box
    if (input) input.value = "";
    toggleReplyBox(parentId);

    // Refresh activity feed to show new reply
    showActivitySectionIfHidden();
    await refreshActivityFeedOnly(taskId);
  } catch (err) {
    console.error(err);
    showToast("Failed to send reply", "error");
  }
}
// Chỉ tạo CARD cho allowlist; còn lại trả về null để giữ link xanh
function getWebsiteIconForComment(url) {
  if (!url) return null;

  // Helper: rút base domain (mail.google.com -> google.com)
  const toBaseDomain = (host) => {
    const parts = host
      .toLowerCase()
      .replace(/^www\./, "")
      .split(".");
    if (parts.length <= 2) return parts.join(".");
    // đơn giản: lấy 2 phần cuối (không xử lý co.uk phức tạp vì không cần ở đây)
    return parts.slice(-2).join(".");
  };

  try {
    const u = new URL(url);
    const host = u.hostname.toLowerCase();

    // ❌ loại nội bộ / IP / file đính kèm -> KHÔNG tạo card
    const isIp = /^\d{1,3}(\.\d{1,3}){3}$/.test(host);
    const isLocal =
      host === "localhost" ||
      host.endsWith(".local") ||
      host.endsWith(".test") ||
      host.endsWith(".internal") ||
      isIp;
    const isFileLike =
      /\.(sql|zip|rar|7z|pdf|docx?|xlsx?|pptx?|png|jpe?g|gif|webp|mp4|mp3)$/i.test(
        u.pathname
      );
    if (isLocal || isFileLike) return null;

    // ✅ CHỈ những domain dưới đây mới tạo card
    const ALLOW_CARD_DOMAINS = new Set([
      "youtube.com",
      "youtu.be",
      "facebook.com",
      "twitter.com",
      "x.com",
      "tiktok.com",
      "instagram.com",
      "linkedin.com",
    ]);

    const base = toBaseDomain(host);
    if (!ALLOW_CARD_DOMAINS.has(base)) return null; // ⬅️ giữ link xanh

    // Tạo favicon + tiêu đề ngắn
    const favicon = `https://www.google.com/s2/favicons?domain=${encodeURIComponent(
      base
    )}&sz=64`;

    const titleMap = {
      "youtube.com": "YouTube",
      "youtu.be": "YouTube",
      "facebook.com": "Facebook",
      "twitter.com": "Twitter",
      "x.com": "X",
      "tiktok.com": "TikTok",
      "instagram.com": "Instagram",
      "linkedin.com": "LinkedIn",
    };
    const title = titleMap[base] || base;

    return {
      icon: `<img src="${favicon}" alt="${base}" class="h-4 w-4 flex-shrink-0" />`,
      title,
    };
  } catch {
    return null; // lỗi parse URL -> giữ link xanh
  }
}

const URL_RX = /(https?:\/\/[^\s<>"']+)/g;

function upgradeLinksToCards(text) {
  if (!text) return "";
  return text.replace(URL_RX, (raw) => {
    let u;
    try {
      u = new URL(raw);
    } catch {
      return raw;
    }

    // Get favicon/icon for URL (always returns a value, uses Google Favicon API)
    const iconData = getWebsiteIconForComment(u.href);
    if (!iconData) return raw; // Should not happen, but fallback to raw URL

    const title = iconData.title || u.hostname;
    const icon = iconData.icon;

    // đóng gói theo format bạn đã dùng
    const placeholder = `[LINK_CARD:${encodeURIComponent(
      u.href
    )}:${encodeURIComponent(icon)}:${encodeURIComponent(title)}]`;
    return placeholder;
  });
}

async function postComment(taskId, content) {
  // ✅ fallback: lấy từ biến toàn cục
  taskId = taskId || window.CURRENT_TASK_ID;
  if (!taskId || taskId === "undefined") {
    console.error("❌ taskId is undefined when posting comment");
    showToast("Không xác định được thẻ hiện tại (taskId undefined)", "error");
    return;
  }

  try {
    const res = await fetch(`/api/tasks/${taskId}/comments`, {
      method: "POST",
      headers: (() => {
        const token = getToken();
        const h = { "Content-Type": "application/json" };
        if (token) h.Authorization = "Bearer " + token;
        return h;
      })(),
      credentials: "include",
      body: JSON.stringify({ content }),
    });

    if (!res.ok) throw new Error("Comment failed");

    const newComment = await res.json();
    const commentsList = document.getElementById("comments-list");
    if (commentsList) {
      const commentHtml = renderSingleComment(taskId, newComment);
      commentsList.insertAdjacentHTML("afterbegin", commentHtml);
      ensureCommentInteractionHandlers(commentsList);
    }

    const commentInput = document.getElementById("comment-input");
    if (commentInput) commentInput.value = "";

    // Refresh activity feed to show new comment
    showActivitySectionIfHidden();
    await refreshActivityFeedOnly(taskId);

    // Update comment count on card
    const commentsListEl = document.getElementById("comments-list");
    const commentCount = commentsListEl
      ? commentsListEl.querySelectorAll("[data-comment-id]").length
      : 0;
    updateCardCommentCount(taskId, commentCount);
  } catch (err) {
    console.error(err);
    showToast("Failed to post comment", "error");
  }
}

async function deleteComment(taskId, commentId) {
  try {
    const res = await fetch(`/api/tasks/${taskId}/comments/${commentId}`, {
      method: "DELETE",
      headers: (() => {
        const token = getToken();
        const h = {};
        if (token) h.Authorization = "Bearer " + token;
        return h;
      })(),
      credentials: "include",
    });

    if (!res.ok) throw new Error("Delete failed");

    const commentContainer = document.querySelector(
      `[data-comment-id="${commentId}"]`
    );
    if (commentContainer) {
      commentContainer.remove();
    }

    // Refresh activity feed to update activity log
    showActivitySectionIfHidden();
    await refreshActivityFeedOnly(taskId);

    // Update comment count on card
    const commentsListEl = document.getElementById("comments-list");
    const commentCount = commentsListEl
      ? commentsListEl.querySelectorAll("[data-comment-id]").length
      : 0;
    updateCardCommentCount(taskId, commentCount);
  } catch (err) {
    console.error(err);
    showToast("Failed to delete comment", "error");
  }
}

// ========== 🖊️ EDIT COMMENT ==========
function editComment(taskId, commentId) {
  taskId = taskId || window.CURRENT_TASK_ID;
  if (!commentId) return;

  const container =
    document.querySelector(
      `[data-comment-id='${commentId}'][data-task-id='${taskId}']`
    ) || document.querySelector(`[data-comment-id='${commentId}']`);
  if (!container) return;

  const bubble = container.querySelector(".comment-bubble");
  if (!bubble || bubble.dataset.editing === "true") return;

  const encodedRaw = bubble.getAttribute("data-comment-raw") || "";
  let rawContent = "";
  try {
    rawContent = decodeURIComponent(encodedRaw);
  } catch (err) {
    console.warn("Failed to decode comment raw content", err);
    rawContent = encodedRaw;
  }

  bubble.dataset.editing = "true";
  bubble.classList.add("comment-bubble-editing");

  const actionBar = container.querySelector(".comment-action-bar");
  if (actionBar) actionBar.classList.add("hidden");

  const editorHtml = `
    <textarea id="edit-input-${commentId}" class="comment-simple-textarea w-full border border-gray-300 rounded-md p-2 text-sm h-24 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 shadow-none" spellcheck="false">${escapeHtml(
    decodeLinkCardToUrl(rawContent)
  )}</textarea>
    <div class="flex items-center gap-2 mt-2">
      <button class="comment-save-btn bg-[#0C66E4] hover:bg-[#0b58c4] text-white text-sm font-medium px-3 py-1.5 rounded-md" data-comment-save="${commentId}">Save</button>
      <button class="comment-cancel-btn bg-[#DFE1E6] hover:bg-[#c7cbd6] text-sm text-[#172B4D] font-medium px-3 py-1.5 rounded-md" data-comment-cancel="${commentId}">Discard changes</button>
    </div>
  `;

  bubble.innerHTML = editorHtml;

  const textarea = bubble.querySelector(`#edit-input-${commentId}`);
  const saveBtn = bubble.querySelector(`[data-comment-save='${commentId}']`);
  const cancelBtn = bubble.querySelector(
    `[data-comment-cancel='${commentId}']`
  );

  if (saveBtn) {
    saveBtn.addEventListener("click", () => saveEdit(taskId, commentId));
  }
  if (cancelBtn) {
    cancelBtn.addEventListener("click", () => loadActivityFeed(taskId));
  }

  textarea?.focus();
  if (textarea) {
    textarea.setSelectionRange(textarea.value.length, textarea.value.length);
  }
}

function toggleReplyBox(id) {
  const box = document.getElementById(`reply-box-${id}`);
  if (!box) return;
  const isHidden = box.classList.contains("hidden");
  box.classList.toggle("hidden");

  // Clear input khi đóng reply box
  if (!isHidden) {
    const input = document.getElementById(`reply-input-${id}`);
    if (input) input.value = "";
  }
}

// ========== 🗑️ DELETE REPLY ==========
async function deleteReply(taskId, replyId) {
  try {
    // Reply là comment con, nên dùng API comment
    const res = await fetch(`/api/tasks/${taskId}/comments/${replyId}`, {
      method: "DELETE",
      headers: (() => {
        const token = getToken();
        const h = {};
        if (token) h.Authorization = "Bearer " + token;
        return h;
      })(),
      credentials: "include",
    });

    if (!res.ok) throw new Error("Delete failed");

    // Xóa reply khỏi DOM
    const replyContainer = document.querySelector(
      `[data-reply-id="${replyId}"]`
    );
    if (replyContainer) {
      replyContainer.remove();
    }

    // Refresh activity feed to update activity log
    showActivitySectionIfHidden();
    await refreshActivityFeedOnly(taskId);
  } catch (err) {
    console.error(err);
    showToast("Failed to delete reply", "error");
  }
}

// ========== 🖊️ EDIT REPLY ==========
function editReply(taskId, replyId) {
  taskId = taskId || window.CURRENT_TASK_ID;

  // Validate replyId
  if (!replyId || replyId === "undefined" || replyId === "null") {
    console.error("❌ editReply: Invalid replyId:", replyId);
    showToast("❌ Failed to edit reply: Invalid reply ID", "error");
    return;
  }

  const container =
    document.querySelector(
      `[data-reply-id='${replyId}'][data-task-id='${taskId}']`
    ) || document.querySelector(`[data-reply-id='${replyId}']`);
  if (!container) {
    console.error(
      " editReply: Container not found for replyId:",
      replyId,
      "taskId:",
      taskId
    );
    return;
  }

  const bubble = container.querySelector(".reply-bubble");
  if (!bubble || bubble.dataset.editing === "true") return;

  const encodedRaw = bubble.getAttribute("data-reply-raw") || "";
  let rawContent = "";
  try {
    rawContent = decodeURIComponent(encodedRaw);
  } catch (err) {
    console.warn("Failed to decode reply raw content", err);
    rawContent = encodedRaw;
  }

  bubble.dataset.editing = "true";
  bubble.classList.add("comment-bubble-editing");

  const actionBar = container.querySelector(".reply-action-bar");
  if (actionBar) actionBar.classList.add("hidden");

  const editorHtml = `
    <textarea id="edit-reply-input-${replyId}" class="comment-simple-textarea w-full border border-gray-300 rounded-md p-2 text-sm h-24 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 shadow-none" spellcheck="false">${escapeHtml(
    decodeLinkCardToUrl(rawContent)
  )}</textarea>
    <div class="flex items-center gap-2 mt-2">
      <button class="comment-save-btn bg-[#0C66E4] hover:bg-[#0b58c4] text-white text-sm font-medium px-3 py-1.5 rounded-md" data-reply-save="${replyId}">Save</button>
      <button class="comment-cancel-btn bg-[#DFE1E6] hover:bg-[#c7cbd6] text-sm text-[#172B4D] font-medium px-3 py-1.5 rounded-md" data-reply-cancel="${replyId}">Discard changes</button>
    </div>
  `;

  bubble.innerHTML = editorHtml;

  const textarea = bubble.querySelector(`#edit-reply-input-${replyId}`);
  const saveBtn = bubble.querySelector(`[data-reply-save='${replyId}']`);
  const cancelBtn = bubble.querySelector(`[data-reply-cancel='${replyId}']`);

  if (saveBtn) {
    saveBtn.addEventListener("click", (e) => {
      // Lấy replyId từ button's data attribute để đảm bảo có giá trị
      const btnReplyId = e.currentTarget.getAttribute("data-reply-save");
      if (!btnReplyId) {
        console.error("❌ Reply ID not found in button data attribute");
        showToast("❌ Failed to update reply: Reply ID not found", "error");
        return;
      }
      saveReplyEdit(taskId, btnReplyId);
    });
  }
  if (cancelBtn) {
    cancelBtn.addEventListener("click", () => loadActivityFeed(taskId));
  }

  textarea?.focus();
  if (textarea) {
    textarea.setSelectionRange(textarea.value.length, textarea.value.length);
  }
}

// ========== 💾 SAVE REPLY EDIT ==========
async function saveReplyEdit(taskId, replyId) {
  // Nếu replyId không được truyền, thử lấy từ textarea ID
  if (!replyId) {
    const textarea = document.querySelector(
      'textarea[id^="edit-reply-input-"]'
    );
    if (textarea) {
      const idMatch = textarea.id.match(/edit-reply-input-(\d+)/);
      if (idMatch) replyId = idMatch[1];
    }
  }

  if (!replyId) {
    console.error("❌ Reply ID not found");
    showToast("❌ Failed to update reply: Reply ID not found", "error");
    return;
  }

  const textarea = document.querySelector(`#edit-reply-input-${replyId}`);
  if (!textarea) {
    console.error("❌ Textarea not found for reply ID:", replyId);
    showToast("❌ Failed to update reply: Textarea not found", "error");
    return;
  }

  const content = textarea.value.trim();
  if (!content) {
    showToast("⚠️ Reply cannot be empty", "warning");
    return;
  }

  taskId = taskId || window.CURRENT_TASK_ID;
  if (!taskId) {
    console.error("❌ Task ID not found");
    showToast("❌ Failed to update reply: Task ID not found", "error");
    return;
  }

  try {
    // Reply là comment con, nên dùng API comment
    const res = await fetch(`/api/tasks/${taskId}/comments/${replyId}`, {
      method: "PUT",
      headers: (() => {
        const token = getToken();
        const h = { "Content-Type": "application/json" };
        if (token) h.Authorization = "Bearer " + token;
        return h;
      })(),
      credentials: "include",
      body: JSON.stringify({ content }),
    });

    if (!res.ok) throw new Error("Update failed");

    const updatedReply = await res.json();
    // Update reply trong DOM
    const container = document.querySelector(`[data-reply-id="${replyId}"]`);
    if (container) {
      const bubble = container.querySelector(".reply-bubble");
      if (bubble) {
        const rawAttr = encodeURIComponent(updatedReply.content || "");
        const safeContent = highlightMentions(
          updatedReply.content,
          updatedReply.mentionsJson
        );
        bubble.setAttribute("data-reply-raw", rawAttr);
        bubble.dataset.editing = "false";
        bubble.classList.remove("comment-bubble-editing");
        bubble.innerHTML = `<div class="comment-content text-xs text-gray-800 break-words">${safeContent}</div>`;

        // Hiện lại action bar
        const actionBar = container.querySelector(".reply-action-bar");
        if (actionBar) actionBar.classList.remove("hidden");
      }
    }

    // Refresh activity feed to update activity log
    showActivitySectionIfHidden();
    await refreshActivityFeedOnly(taskId);
  } catch (err) {
    console.error(
      "❌ saveReplyEdit error:",
      err,
      "replyId:",
      replyId,
      "taskId:",
      taskId
    );
    showToast("❌ Failed to update reply", "error");
  }
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
      headers: (() => {
        const token = getToken();
        const h = {};
        if (token) h.Authorization = "Bearer " + token;
        return h;
      })(),
      credentials: "include",
    });
    if (!res.ok) throw new Error("Cannot load share status");
    const data = await res.json();

    if (!hintText || !copyLinkBtn || !deleteLinkBtn) {
      console.warn("Share UI elements not found");
      return;
    }

    if (data.allowLinkJoin && data.inviteLink) {
      // ✅ When link is enabled
      hintText.textContent = ""; // Don't show additional description
      copyLinkBtn.textContent = "Copy link";
      deleteLinkBtn.textContent = "Delete link";
      deleteLinkBtn.classList.remove("text-gray-400", "cursor-not-allowed");
      deleteLinkBtn.classList.add("text-red-600", "hover:underline");
      copyLinkBtn.disabled = false;
      deleteLinkBtn.disabled = false;
    } else {
      // 🔒 When link is deleted → show "Create link"
      hintText.textContent = "🔒 Link sharing is disabled.";
      hintText.className = "text-xs text-gray-500 mt-1 ml-5 italic";
      copyLinkBtn.textContent = "Create link";
      deleteLinkBtn.classList.remove("text-red-600", "hover:underline");
      deleteLinkBtn.classList.add("text-gray-400", "cursor-not-allowed");
      deleteLinkBtn.disabled = true;
    }
  } catch (err) {
    console.error("❌ syncShareUI error:", err);
    if (hintText) {
      hintText.textContent = "⚠️ Cannot load share status.";
      hintText.className = "text-xs text-red-500 mt-1 ml-5 italic";
    }
  }
}

async function openSharePopup() {
  sharePopup.classList.remove("hidden");
  await loadBoardMembers(PROJECT_ID);
  syncShareUI(PROJECT_ID); // ✅ Call immediately when opening

  // ✅ Check PM permission and hide Join requests tab if not PM
  await checkAndHideJoinRequestsTab(PROJECT_ID);

  // Default to show Board members tab
  switchTab("members");
}

// ========== KIỂM TRA QUYỀN PM VÀ ẨN TAB JOIN REQUESTS ==========
async function checkAndHideJoinRequestsTab(projectId) {
  const tabRequests = document.getElementById("tab-requests");
  if (!tabRequests) return;

  try {
    // Thử load join requests để kiểm tra quyền
    const token = getToken();
    const headers = {};
    if (token) {
      headers.Authorization = "Bearer " + token;
    }
    const res = await fetch(
      `/api/pm/invite/project/${projectId}/join-requests`,
      {
        headers,
        credentials: "include",
      }
    );

    // Nếu không phải PM (403) hoặc lỗi → ẩn tab
    if (!res.ok) {
      if (res.status === 403) {
        tabRequests.classList.add("hidden");
        return;
      }
    }

    // Nếu là PM → hiển thị tab và load count
    tabRequests.classList.remove("hidden");
    if (res.ok) {
      const requests = await res.json();
      updateJoinRequestsBadge(Array.isArray(requests) ? requests.length : 0);
    }
  } catch (err) {
    // Lỗi → ẩn tab
    console.error("❌ checkAndHideJoinRequestsTab error:", err);
    tabRequests.classList.add("hidden");
  }
}

// ========== LOAD JOIN REQUESTS COUNT (for badge) ==========
async function loadJoinRequestsCount(projectId) {
  try {
    const token = getToken();
    const headers = {};
    if (token) {
      headers.Authorization = "Bearer " + token;
    }
    const res = await fetch(
      `/api/pm/invite/project/${projectId}/join-requests`,
      {
        headers,
        credentials: "include",
      }
    );

    if (res.ok) {
      const requests = await res.json();
      updateJoinRequestsBadge(Array.isArray(requests) ? requests.length : 0);
    }
  } catch (err) {
    console.error("❌ loadJoinRequestsCount error:", err);
  }
}

function closeShareBoard() {
  sharePopup.classList.add("hidden");
}

closeSharePopup.addEventListener("click", closeShareBoard);

// ✅ Đóng modal khi click ra ngoài (vào overlay background)
if (sharePopup) {
  sharePopup.addEventListener("click", (e) => {
    // Nếu click vào chính overlay (không phải vào nội dung modal bên trong) → đóng
    if (e.target === sharePopup) {
      closeShareBoard();
    }
  });

  // Ngăn event bubbling từ nội dung modal để không đóng khi click vào bên trong
  const modalContent = sharePopup.querySelector(".bg-white");
  if (modalContent) {
    modalContent.addEventListener("click", (e) => {
      e.stopPropagation();
    });
  }
}

copyLinkBtn.addEventListener("click", async (e) => {
  e.preventDefault();
  try {
    // Kiểm tra xem đã có link chưa
    const token = getToken();
    const headers = {};
    if (token) {
      headers.Authorization = "Bearer " + token;
    }
    const checkRes = await fetch(
      `/api/pm/invite/project/${PROJECT_ID}/share/link`,
      {
        headers,
        credentials: "include",
      }
    );

    if (!checkRes.ok) throw new Error("Cannot check link status");

    const checkData = await checkRes.json();

    let fullLink = "";

    if (!checkData.allowLinkJoin || !checkData.inviteLink) {
      const enableToken = getToken();
      const enableHeaders = {};
      if (enableToken) {
        enableHeaders.Authorization = "Bearer " + enableToken;
      }
      const enableRes = await fetch(
        `/api/pm/invite/project/${PROJECT_ID}/share/enable`,
        {
          method: "POST",
          headers: enableHeaders,
          credentials: "include",
        }
      );
      if (!enableRes.ok) throw new Error("Cannot enable share link");
      const enableData = await enableRes.json();
      fullLink = `${window.location.origin}/join/${enableData.inviteLink}`;
      await syncShareUI(PROJECT_ID);
      showToast("Link created! Invited users will join directly.", "success");
    } else {
      const copyToken = getToken();
      const copyHeaders = {};
      if (copyToken) {
        copyHeaders.Authorization = "Bearer " + copyToken;
      }
      const copyRes = await fetch(
        `/api/pm/invite/project/${PROJECT_ID}/share/copy`,
        {
          method: "POST",
          headers: copyHeaders,
          credentials: "include",
        }
      );

      if (!copyRes.ok) {
        const error = await copyRes.json();
        throw new Error(error.error || "Cannot copy link");
      }

      const copyData = await copyRes.json();
      fullLink = `${window.location.origin}/join/${copyData.inviteLink}`;

      // ✅ Show message based on requiresApproval
      if (copyData.requiresApproval) {
        showToast("Link copied! Invited users will need PM approval.", "info");
      } else {
        showToast("Link copied! Invited users will join directly.", "success");
      }
    }

    await navigator.clipboard.writeText(fullLink);
    copyLinkBtn.textContent = "Copy link";
  } catch (err) {
    console.error("❌ Copy link error:", err);
    showToast(err.message || "Cannot copy link", "error");
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
    const token = getToken();
    const headers = {};
    if (token) {
      headers.Authorization = "Bearer " + token;
    }
    const res = await fetch(
      `/api/pm/invite/project/${PROJECT_ID}/share/disable`,
      {
        method: "DELETE",
        headers,
        credentials: "include",
      }
    );
    if (!res.ok) throw new Error();
    showToast("🔒 Link sharing disabled.");
    deleteConfirmPopup.classList.add("hidden");

    // ✅ After deleting link → sync UI again (show "Create link")
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
      headers: (() => {
        const token = getToken();
        const h = {};
        if (token) h.Authorization = "Bearer " + token;
        return h;
      })(),
      credentials: "include",
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

    const token = getToken();
    const headers = {
      "Content-Type": "application/json",
    };
    if (token) {
      headers.Authorization = "Bearer " + token;
    }
    const res = await fetch(
      `/api/pm/invite/project/${projectId}/member/${userId}/role?role=${newRole}`,
      {
        method: "PUT",
        headers,
        credentials: "include",
      }
    );

    if (!res.ok) throw new Error("Failed to update role");
    const data = await res.json();

    // ✅ Reload lại danh sách thật sau khi DB cập nhật
    await loadBoardMembers(projectId);
  } catch (err) {
    console.error("❌ Update role failed:", err);
    showToast("❌ Không thể cập nhật vai trò!", "error");
  } finally {
    if (event?.target) event.target.disabled = false;
  }
}

// ========== UPDATE JOIN REQUESTS BADGE ==========
function updateJoinRequestsBadge(count) {
  const requestsTab = document.getElementById("tab-requests");
  if (!requestsTab) return;

  // Tìm hoặc tạo badge
  let badge = requestsTab.querySelector(".join-requests-badge");
  if (count > 0) {
    if (!badge) {
      badge = document.createElement("span");
      badge.className =
        "join-requests-badge ml-2 bg-blue-600 text-white text-xs font-semibold px-2 py-0.5 rounded-full";
      requestsTab.appendChild(badge);
    }
    badge.textContent = count;
    badge.classList.remove("hidden");
  } else {
    if (badge) {
      badge.classList.add("hidden");
    }
  }
}

// ========== TAB SWITCHING ==========
function switchTab(tabName) {
  const membersTab = document.getElementById("tab-members");
  const requestsTab = document.getElementById("tab-requests");
  const membersList = document.getElementById("members-list");
  const requestsList = document.getElementById("join-requests-list");

  // ✅ Nếu tab requests bị ẩn (không phải PM) → chỉ cho phép tab members
  if (
    tabName === "requests" &&
    requestsTab &&
    requestsTab.classList.contains("hidden")
  ) {
    return; // Không cho switch sang tab requests
  }

  if (tabName === "members") {
    membersTab.classList.add(
      "text-blue-600",
      "border-b-2",
      "border-blue-600",
      "font-semibold"
    );
    membersTab.classList.remove("text-gray-500", "font-medium");
    // ✅ Chỉ update requestsTab nếu nó không bị ẩn
    if (requestsTab && !requestsTab.classList.contains("hidden")) {
      requestsTab.classList.remove(
        "text-blue-600",
        "border-b-2",
        "border-blue-600",
        "font-semibold"
      );
      requestsTab.classList.add("text-gray-500", "font-medium");
    }
    membersList.classList.remove("hidden");
    if (requestsList) requestsList.classList.add("hidden");
  } else if (tabName === "requests") {
    requestsTab.classList.add(
      "text-blue-600",
      "border-b-2",
      "border-blue-600",
      "font-semibold"
    );
    requestsTab.classList.remove("text-gray-500", "font-medium");
    membersTab.classList.remove(
      "text-blue-600",
      "border-b-2",
      "border-blue-600",
      "font-semibold"
    );
    membersTab.classList.add("text-gray-500", "font-medium");
    membersList.classList.add("hidden");
    requestsList.classList.remove("hidden");
    loadJoinRequests(PROJECT_ID);
  }
}

// Event listeners for tabs
const tabMembers = document.getElementById("tab-members");
const tabRequests = document.getElementById("tab-requests");
if (tabMembers) {
  tabMembers.addEventListener("click", () => switchTab("members"));
}
if (tabRequests) {
  tabRequests.addEventListener("click", () => switchTab("requests"));
}

// ========== LOAD JOIN REQUESTS ==========
async function loadJoinRequests(projectId) {
  const requestsList = document.getElementById("join-requests-list");
  const tabRequests = document.getElementById("tab-requests");

  if (!requestsList) return;

  // ✅ Ẩn tab nếu không phải PM
  if (tabRequests && tabRequests.classList.contains("hidden")) {
    return;
  }

  requestsList.innerHTML = `<p class="text-gray-400 text-sm italic">Loading...</p>`;

  try {
    const token = getToken();
    const headers = {};
    if (token) {
      headers.Authorization = "Bearer " + token;
    }
    const res = await fetch(
      `/api/pm/invite/project/${projectId}/join-requests`,
      {
        headers,
        credentials: "include",
      }
    );

    // ✅ Nếu không phải PM (403) → ẩn tab và return
    if (res.status === 403) {
      if (tabRequests) tabRequests.classList.add("hidden");
      requestsList.innerHTML = "";
      return;
    }

    if (!res.ok) throw new Error(`Cannot load join requests: ${res.status}`);

    const requests = await res.json();

    // Cập nhật badge số lượng requests
    updateJoinRequestsBadge(Array.isArray(requests) ? requests.length : 0);

    if (!Array.isArray(requests) || requests.length === 0) {
      requestsList.innerHTML = `<p class="text-gray-500 text-sm italic text-center py-4">No pending join requests</p>`;
      return;
    }

    requestsList.innerHTML = requests
      .map(
        (r) => `
          <div class="flex justify-between items-center p-3 hover:bg-gray-50 rounded-md border border-gray-200">
            <div class="flex items-center gap-3 flex-1">
              <div class="w-10 h-10 rounded-full bg-gray-200 flex items-center justify-center text-gray-600 font-semibold">
                ${(r.userName || r.userEmail || "U")[0].toUpperCase()}
              </div>
              <div class="flex-1">
                <p class="text-sm font-medium text-gray-800">${escapeHtml(
                  r.userName || "Unnamed"
                )}</p>
                <p class="text-xs text-gray-500">${escapeHtml(
                  r.userEmail || ""
                )}</p>
                <p class="text-xs text-gray-400 mt-1">${formatTime(
                  r.createdAt
                )}</p>
              </div>
            </div>
            <div class="flex gap-2">
              <button
                onclick="approveJoinRequest(${r.requestId}, ${projectId})"
                class="px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white text-xs font-semibold rounded-md transition-colors"
              >
                Approve
              </button>
              <button
                onclick="rejectJoinRequest(${r.requestId}, ${projectId})"
                class="px-3 py-1.5 bg-gray-200 hover:bg-gray-300 text-gray-700 text-xs font-semibold rounded-md transition-colors"
              >
                Reject
              </button>
            </div>
          </div>
        `
      )
      .join("");
  } catch (err) {
    console.error("❌ loadJoinRequests error:", err);
    requestsList.innerHTML = `<p class="text-red-500 text-sm">Failed to load join requests</p>`;
  }
}

// ========== APPROVE JOIN REQUEST ==========
async function approveJoinRequest(requestId, projectId) {
  try {
    const token = getToken();
    const headers = {};
    if (token) {
      headers.Authorization = "Bearer " + token;
    }
    const res = await fetch(
      `/api/pm/invite/join-requests/${requestId}/approve`,
      {
        method: "POST",
        headers,
        credentials: "include",
      }
    );

    if (!res.ok) {
      const error = await res.json();
      throw new Error(error.error || "Failed to approve request");
    }

    await loadJoinRequests(projectId);
    await loadBoardMembers(projectId); // Reload members list
    // Cập nhật badge sau khi approve
    const badgeToken = getToken();
    const badgeHeaders = {};
    if (badgeToken) {
      badgeHeaders.Authorization = "Bearer " + badgeToken;
    }
    const requests = await fetch(
      `/api/pm/invite/project/${projectId}/join-requests`,
      {
        headers: badgeHeaders,
        credentials: "include",
      }
    ).then((r) => (r.ok ? r.json() : []));
    updateJoinRequestsBadge(Array.isArray(requests) ? requests.length : 0);
  } catch (err) {
    console.error("❌ approveJoinRequest error:", err);
    showToast(err.message || "Failed to approve request", "error");
  }
}

// ========== REJECT JOIN REQUEST ==========
async function rejectJoinRequest(requestId, projectId) {
  try {
    const token = getToken();
    const headers = {};
    if (token) {
      headers.Authorization = "Bearer " + token;
    }
    const res = await fetch(
      `/api/pm/invite/join-requests/${requestId}/reject`,
      {
        method: "POST",
        headers,
        credentials: "include",
      }
    );

    if (!res.ok) {
      const error = await res.json();
      throw new Error(error.error || "Failed to reject request");
    }

    await loadJoinRequests(projectId);
    // Badge sẽ được cập nhật trong loadJoinRequests
  } catch (err) {
    console.error("❌ rejectJoinRequest error:", err);
    showToast(err.message || "Failed to reject request", "error");
  }
}

// Make functions available globally
window.approveJoinRequest = approveJoinRequest;
window.rejectJoinRequest = rejectJoinRequest;

inviteBtn.addEventListener("click", async () => {
  const email = inviteEmail.value.trim();
  const role = inviteRole.value || "Member";
  if (!email) {
    showToast("Please enter an email", "error");
    return;
  }

  try {
    const token = getToken();
    const headers = {};
    if (token) {
      headers.Authorization = "Bearer " + token;
    }
    const resInvite = await fetch(
      `/api/pm/invite?projectId=${PROJECT_ID}&email=${encodeURIComponent(
        email
      )}&role=${role}`,
      {
        method: "POST",
        headers,
        credentials: "include",
      }
    );
    if (!resInvite.ok) {
      const error = await resInvite.json();
      throw new Error(error.error || "Invite failed");
    }
    const data = await resInvite.json();
    inviteEmail.value = "";
    await loadBoardMembers(PROJECT_ID);
  } catch (err) {
    console.error("❌ Error inviting:", err);
    showToast(err.message || "Failed to invite member", "error");
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
    showToast(
      " Cannot initialize dashboard: " + (err.message || "Unknown error"),
      "error"
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
    showToast("Invalid invite link!", "warning");
    return;
  }

  try {
    const res = await fetch(`/api/pm/invite/join/${inviteLink}`, {
      method: "POST",
      headers: (() => {
        const token = getToken();
        const h = { "Content-Type": "application/json" };
        if (token) h.Authorization = "Bearer " + token;
        return h;
      })(),
      credentials: "include",
    });

    if (!res.ok) {
      const errorData = await res
        .json()
        .catch(async () => ({ error: await res.text() }));
      throw new Error(errorData.error || "Không thể tham gia dự án");
    }

    const data = await res.json();
    if (data.message === "join_request_sent") {
      showToast(
        ` Yêu cầu tham gia đã được gửi!\n Dự án: ${data.projectName}\n⏳ Vui lòng đợi PM duyệt yêu cầu của bạn.`,
        "info"
      );
      window.location.href = `/view/pm/dashboard`;
    } else if (data.message === "joined_success") {
      showToast(
        ` Tham gia dự án thành công!\n Dự án: ${data.projectName}`,
        "success"
      );
      if (data.projectId) {
        window.location.href = `/dashboard.html?projectId=${data.projectId}`;
      }
    } else {
      showToast(` ${data.message}\n Dự án: ${data.projectName}`, "info");
      if (data.projectId) {
        window.location.href = `/dashboard.html?projectId=${data.projectId}`;
      }
    }
  } catch (err) {
    console.error("❌ Join project failed:", err);
    showToast(
      " Không thể tham gia dự án qua link mời!\n" + (err.message || ""),
      "error"
    );
  }
}

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
    const token = getToken();

    const headers = token
      ? { Authorization: "Bearer " + token, "Content-Type": "application/json" }
      : { "Content-Type": "application/json" };

    const useCredentials = !token;

    const res = await fetch(
      `/api/pm/invite/search-users?keyword=${encodeURIComponent(keyword)}`,
      {
        method: "GET",
        headers,
        ...(useCredentials ? { credentials: "include" } : {}),
      }
    );

    if (!res.ok) throw new Error(`Request failed: ${res.status}`);

    const users = await res.json();

    if (!Array.isArray(users) || users.length === 0) {
      suggestionBox.innerHTML = `<p class="p-2 text-sm text-gray-400 italic">No results found</p>`;
      suggestionBox.classList.remove("hidden");
      return;
    }

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
    console.error(" loadInviteSuggestions error:", err);
    suggestionBox.innerHTML = `
        <p class="p-2 text-sm text-red-500 italic">
           Cannot load suggestions.
        </p>`;
    suggestionBox.classList.remove("hidden");
  }
}

window.selectInvite = function (email) {
  inviteInput.value = email;
  suggestionBox.classList.add("hidden");
};

async function saveEdit(taskId, commentId) {
  const textarea = document.getElementById(`edit-input-${commentId}`);
  if (!textarea) return;

  const newText = textarea.value.trim();
  if (!newText) {
    showToast("Content cannot be empty", "warning");
    return;
  }

  try {
    const res = await fetch(`/api/tasks/${taskId}/comments/${commentId}`, {
      method: "PUT",
      headers: (() => {
        const token = getToken();
        const h = { "Content-Type": "application/json" };
        if (token) h.Authorization = "Bearer " + token;
        return h;
      })(),
      credentials: "include",
      body: JSON.stringify({ content: newText }),
    });

    if (!res.ok) throw new Error("Update failed");

    const updatedComment = await res.json();
    // Update comment trong DOM
    const container = document.querySelector(
      `[data-comment-id="${commentId}"]`
    );
    if (container) {
      const bubble = container.querySelector(".comment-bubble");
      if (bubble) {
        const rawAttr = encodeURIComponent(updatedComment.content || "");
        const safeContent = highlightMentions(
          updatedComment.content,
          updatedComment.mentionsJson
        );
        bubble.setAttribute("data-comment-raw", rawAttr);
        bubble.dataset.editing = "false";
        bubble.classList.remove("comment-bubble-editing");
        bubble.innerHTML = `<div class="comment-content text-xs leading-relaxed text-gray-800 break-words">${safeContent}</div>`;

        // Hiện lại action bar
        const actionBar = container.querySelector(".comment-action-bar");
        if (actionBar) actionBar.classList.remove("hidden");
      }
    }

    // Refresh activity feed to update activity log
    showActivitySectionIfHidden();
    await refreshActivityFeedOnly(taskId);
  } catch (err) {
    console.error(err);
    showToast("❌ Failed to update comment", "error");
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
        openMembersPopup(fakeEvent);
        break;

      case "dates":
        openDatePopup(fakeEvent);
        break;
      case "mark-complete":
        try {
          await markTaskComplete(taskId);
          const checkboxBtn = cardElement.querySelector(".task-checkbox-btn");
          if (checkboxBtn) {
            updateTaskCheckboxUI(checkboxBtn, true);
          }
        } catch (err) {
          console.error(" Mark complete failed:", err);
          const errorMsg = err.message || "Không thể đánh dấu hoàn tất task";
          showToast(" " + errorMsg, "error");
        }
        break;

      case "archive":
        const archiveConfirmed = await showConfirmModal("Archive this task?", {
          icon: "🗃️",
        });
        if (!archiveConfirmed) return;
        await archiveTask(taskId);
        cardElement.remove();
        break;

      case "delete":
        if (window.CURRENT_ROLE !== "ROLE_PM") {
          showToast(" Only Project Managers can delete tasks!", "error");
          return;
        }

        const deleteConfirmed = await showConfirmModal(
          "Permanently delete this task? This cannot be undone!",
          {
            icon: "⚠️",
            okText: "Delete",
            okButtonClass: "bg-red-600 hover:bg-red-700 text-white",
          }
        );
        if (!deleteConfirmed) return;
        await deleteTask(taskId);
        cardElement.remove();
        break;

      default:
        console.warn(` Unhandled context menu action: ${action}`);
    }
  } catch (err) {
    console.error("❌ Context menu action error:", err);
    showToast(" Operation failed: " + err.message, "error");
  } finally {
    contextMenu.classList.add("hidden");
  }
});

// ================== API HELPERS ==================
async function archiveTask(taskId) {
  const res = await fetch(`/api/tasks/${taskId}/archive`, {
    method: "PUT",
    headers: (() => {
      const token = getToken();
      const h = {};
      if (token) h.Authorization = "Bearer " + token;
      return h;
    })(),
    credentials: "include",
  });
  if (!res.ok) throw new Error("Archive failed");
}

// ✅ Hàm xử lý click vào checkbox
window.handleTaskCheckboxClick = async function (buttonEl, taskId) {
  try {
    const wasCompleted = buttonEl.dataset.completed === "true";
    buttonEl.disabled = true;

    if (wasCompleted) {
      await markTaskIncomplete(taskId);
    } else {
      await markTaskComplete(taskId);
    }

    updateTaskCheckboxUI(buttonEl, !wasCompleted);
  } catch (err) {
    console.error("❌ Mark complete/incomplete failed:", err);
    // Hiển thị message lỗi từ server hoặc message mặc định
    const errorMsg = err.message || "Không thể cập nhật trạng thái task";
    showToast(errorMsg, "error");
    // Không cập nhật UI nếu có lỗi
  } finally {
    buttonEl.disabled = false;
  }
};

function updateTaskCheckboxUI(buttonEl, isCompleted) {
  buttonEl.dataset.completed = isCompleted ? "true" : "false";
  buttonEl.title = isCompleted ? "Đánh dấu chưa hoàn tất" : "Đánh dấu hoàn tất";

  if (isCompleted) {
    buttonEl.className = `${TASK_CHECKBOX_BASE_CLASS} ${TASK_CHECKBOX_COMPLETED_CLASS}`;
  } else {
    buttonEl.className = `${TASK_CHECKBOX_BASE_CLASS} ${TASK_CHECKBOX_INCOMPLETE_CLASS}`;
  }

  buttonEl.innerHTML = isCompleted
    ? `<svg xmlns="http://www.w3.org/2000/svg" class="h-[0.65rem] w-[0.65rem] text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="3">
        <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
      </svg>`
    : "";

  const titleEl = buttonEl.parentElement.querySelector(".task-title-text");
  if (titleEl) {
    titleEl.classList.toggle("text-gray-500", isCompleted);
    titleEl.classList.toggle("text-gray-800", !isCompleted);
  }

  const card = buttonEl.closest(".kanban-card");
  if (card) {
    card.dataset.taskStatus = isCompleted ? "DONE" : "OPEN";
  }
}

async function markTaskComplete(taskId) {
  const res = await fetch(`/api/tasks/${taskId}/complete`, {
    method: "PUT",
    headers: (() => {
      const token = getToken();
      const h = {};
      if (token) h.Authorization = "Bearer " + token;
      return h;
    })(),
    credentials: "include",
  });

  if (!res.ok) {
    try {
      const errorData = await res.json();
      if (res.status === 403) {
        throw new Error(
          errorData.message || "Bạn không có quyền đánh dấu hoàn thành task này"
        );
      } else if (res.status === 404) {
        throw new Error(errorData.message || "Không tìm thấy task");
      } else {
        throw new Error(errorData.message || "Lỗi hệ thống");
      }
    } catch (parseError) {
      // Nếu không parse được JSON, dùng message mặc định
      if (res.status === 403) {
        throw new Error("Bạn không có quyền đánh dấu hoàn thành task này");
      }
      throw new Error("Request failed: " + res.status);
    }
  }

  const updated = await res.json();
  return updated;
}

// ================== UPDATE CARD UI FUNCTIONS ==================
// Cập nhật labels trong card bên ngoài
export function updateCardLabels(taskId, labels) {
  const card = document.querySelector(`[data-open-task="${taskId}"]`);
  if (!card) return;

  // Tìm container labels hoặc tạo mới
  let labelContainer = card.querySelector(".flex.flex-wrap.gap-1.mb-2");

  if (labels && Array.isArray(labels) && labels.length > 0) {
    const labelHtml = `<div class="flex flex-wrap gap-1 mb-2">
      ${labels
        .map(
          (l) => `
            <span class="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-semibold text-white"
                  style="background-color: ${l.color || "#94a3b8"}">
              ${escapeHtml(l.name || "")}
            </span>
          `
        )
        .join("")}
    </div>`;

    if (labelContainer) {
      labelContainer.outerHTML = labelHtml;
    } else {
      // Chèn vào đầu card (trước checkbox)
      const checkboxDiv = card.querySelector(".flex.items-center.gap-2.mb-1");
      if (checkboxDiv) {
        checkboxDiv.insertAdjacentHTML("beforebegin", labelHtml);
      } else {
        card.insertAdjacentHTML("afterbegin", labelHtml);
      }
    }
  } else {
    // Xóa labels nếu không có
    if (labelContainer) {
      labelContainer.remove();
    }
  }
}

// Cập nhật members trong card bên ngoài
export function updateCardMembers(taskId, assignees) {
  const card = document.querySelector(`[data-open-task="${taskId}"]`);
  if (!card) return;

  let metadataDiv = card.querySelector(
    ".flex.items-center.gap-2.mt-2.flex-wrap"
  );

  // Tạo metadata div nếu chưa có
  if (!metadataDiv) {
    metadataDiv = document.createElement("div");
    metadataDiv.className = "flex items-center gap-2 mt-2 flex-wrap";
    // Chèn sau checkbox div
    const checkboxDiv = card.querySelector(".flex.items-center.gap-2.mb-1");
    if (checkboxDiv) {
      checkboxDiv.insertAdjacentElement("afterend", metadataDiv);
    } else {
      card.appendChild(metadataDiv);
    }
  }

  // Tìm hoặc tạo container members
  let membersContainer = metadataDiv.querySelector(
    ".flex.items-center.ml-auto.gap-1"
  );

  if (assignees && Array.isArray(assignees) && assignees.length > 0) {
    // Loại bỏ duplicate dựa trên userId
    const seen = new Set();
    const uniqueAssignees = assignees.filter((a) => {
      const userId = a.userId || a.id;
      if (!userId || seen.has(userId)) return false;
      seen.add(userId);
      return true;
    });

    const maxAvatars = 2;
    const visibleAssignees = uniqueAssignees.slice(0, maxAvatars);
    const remainingCount = uniqueAssignees.length - maxAvatars;

    const membersHtml = `
      <div class="flex items-center ml-auto gap-1">
        ${visibleAssignees
          .map((assignee) => {
            const assigneeData = {
              name: assignee.name || assignee.assigneeName || "",
              avatarUrl: assignee.avatarUrl || "",
              userId: assignee.userId || assignee.id,
              color:
                assignee.color ||
                getColorForId(
                  String(assignee.userId || assignee.id || assignee.name)
                ),
            };
            // Sử dụng renderAvatar với size "sm" cho card bên ngoài
            return `<div class="relative member-avatar-chip">${renderAvatar(
              assigneeData,
              "sm"
            )}</div>`;
          })
          .join("")}
        ${
          remainingCount > 0
            ? `<div class="relative member-avatar-chip member-avatar-more" title="${remainingCount} more members">
                <div class="w-6 h-6 rounded-full flex items-center justify-center text-white text-[9px] font-semibold border-2 border-white shadow-sm bg-gray-500" style="box-shadow: 0 1px 3px rgba(9, 30, 66, 0.15);">
                  +${remainingCount}
                </div>
              </div>`
            : ""
        }
      </div>
    `;

    if (membersContainer) {
      membersContainer.outerHTML = membersHtml;
    } else {
      metadataDiv.insertAdjacentHTML("beforeend", membersHtml);
    }
  } else {
    // Xóa members nếu không có
    if (membersContainer) {
      membersContainer.remove();
    }
  }
}

// Cập nhật date trong card bên ngoài
export function updateCardCommentCount(taskId, commentCount) {
  const card = document.querySelector(`[data-open-task="${taskId}"]`);
  if (!card) return;

  const metadataRow = card.querySelector(".flex.items-center.gap-2.mt-2");
  if (!metadataRow) return;

  // Tìm hoặc tạo comment count element
  let commentEl = metadataRow.querySelector("[data-comment-count]");

  if (commentCount > 0) {
    const commentHtml = `
      <div class="flex items-center gap-1 text-gray-600" data-comment-count="${taskId}">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
        </svg>
        <span class="text-[10px]">${commentCount}</span>
      </div>
    `;

    if (commentEl) {
      commentEl.outerHTML = commentHtml;
    } else {
      // Tìm vị trí chèn (sau due date, trước attachments)
      const dueDateEl = metadataRow.querySelector("[data-due-date]");
      const attachmentEl = metadataRow.querySelector("[data-attachment-count]");
      const insertBefore = attachmentEl || metadataRow.lastElementChild;

      if (insertBefore) {
        insertBefore.insertAdjacentHTML("beforebegin", commentHtml);
      } else {
        metadataRow.insertAdjacentHTML("beforeend", commentHtml);
      }
    }
  } else {
    // Xóa nếu comment count = 0
    if (commentEl) {
      commentEl.remove();
    }
  }
}

export function updateCardAttachmentCount(taskId, attachmentCount) {
  const card = document.querySelector(`[data-open-task="${taskId}"]`);
  if (!card) return;

  const metadataRow = card.querySelector(".flex.items-center.gap-2.mt-2");
  if (!metadataRow) return;

  // Tìm hoặc tạo attachment count element
  let attachmentEl = metadataRow.querySelector("[data-attachment-count]");

  if (attachmentCount > 0) {
    const attachmentHtml = `
      <div class="flex items-center gap-1 text-gray-600" data-attachment-count="${taskId}">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13" />
        </svg>
        <span class="text-[10px]">${attachmentCount}</span>
      </div>
    `;

    if (attachmentEl) {
      attachmentEl.outerHTML = attachmentHtml;
    } else {
      // Tìm vị trí chèn (sau comments, trước subtasks)
      const commentEl = metadataRow.querySelector("[data-comment-count]");
      const subtaskEl = metadataRow.querySelector("[data-subtask-count]");
      const insertBefore = subtaskEl || metadataRow.lastElementChild;

      if (insertBefore) {
        insertBefore.insertAdjacentHTML("beforebegin", attachmentHtml);
      } else {
        metadataRow.insertAdjacentHTML("beforeend", attachmentHtml);
      }
    }
  } else {
    // Xóa nếu attachment count = 0
    if (attachmentEl) {
      attachmentEl.remove();
    }
  }
}

export function updateCardDate(taskId, deadline) {
  const card = document.querySelector(`[data-open-task="${taskId}"]`);
  if (!card) {
    console.warn(`Card not found for taskId: ${taskId}`);
    return;
  }

  const metadataDiv = card.querySelector(
    ".flex.items-center.gap-2.mt-2.flex-wrap"
  );
  if (!metadataDiv) {
    console.warn(`Metadata div not found for taskId: ${taskId}`);
    return;
  }

  // Tìm date container hiện tại - tìm phần tử có SVG calendar (path d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z")
  let dateContainer = null;
  const allContainers = metadataDiv.querySelectorAll(
    ".flex.items-center.gap-1"
  );
  for (const container of allContainers) {
    const svg = container.querySelector("svg");
    if (svg) {
      const path = svg.querySelector('path[d*="M8 7V3m8 4V3"]');
      if (path) {
        dateContainer = container;
        break;
      }
    }
  }

  if (deadline && deadline.trim() !== "") {
    try {
      const deadlineDate = new Date(deadline);
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
        dateText = deadlineDate.toLocaleDateString("en-US", {
          month: "short",
          day: "numeric",
        });
        dateClass = "text-gray-600";
        bgClass = "bg-gray-100";
      }

      const dateHtml = `
        <div class="flex items-center gap-1 ${bgClass} ${dateClass} px-1.5 py-0.5 rounded text-[10px] font-medium">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
          <span>${dateText}</span>
        </div>
      `;

      if (dateContainer) {
        // Thay thế date container hiện tại
        dateContainer.outerHTML = dateHtml;
      } else {
        // Chèn vào đầu metadata div
        metadataDiv.insertAdjacentHTML("afterbegin", dateHtml);
      }
    } catch (e) {
      console.error("Error updating card date:", e);
    }
  } else {
    // Xóa date nếu không có
    if (dateContainer) {
      dateContainer.remove();
    }
  }
}

// Hàm reload card từ server để đồng bộ
async function reloadCardFromServer(taskId) {
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
    if (!res.ok) return null;
    const task = await res.json();
    return task;
  } catch (err) {
    console.error("Error reloading card:", err);
    return null;
  }
}

async function markTaskIncomplete(taskId) {
  const res = await fetch(`/api/tasks/${taskId}/incomplete`, {
    method: "PUT",
    headers: (() => {
      const token = getToken();
      const h = {};
      if (token) h.Authorization = "Bearer " + token;
      return h;
    })(),
    credentials: "include",
  });

  if (!res.ok) {
    try {
      const errorData = await res.json();
      if (res.status === 403) {
        throw new Error(
          errorData.message ||
            "Bạn không có quyền chuyển task về trạng thái chưa hoàn tất"
        );
      } else if (res.status === 404) {
        throw new Error(errorData.message || "Không tìm thấy task");
      } else {
        throw new Error(
          errorData.message || errorData.detail || "Lỗi hệ thống"
        );
      }
    } catch (parseError) {
      // Nếu không parse được JSON, dùng message mặc định
      if (res.status === 403) {
        throw new Error(
          "Bạn không có quyền chuyển task về trạng thái chưa hoàn tất"
        );
      }
      throw new Error("Request failed: " + res.status);
    }
  }

  const response = await res.json();
  // Response format: {status: "success", task: result, message: "..."}
  if (response.status === "success" && response.task) {
    return response.task;
  }
  return response;
}

function showConfirmModal(message, options = {}) {
  const {
    icon = "",
    okText = "OK",
    cancelText = "Hủy",
    okButtonClass = "bg-blue-600 hover:bg-blue-700 text-white",
    cancelButtonClass = "bg-gray-100 hover:bg-gray-200 text-gray-700",
  } = options;

  return new Promise((resolve) => {
    const existing = document.getElementById("confirm-modal-overlay");
    if (existing) existing.remove();

    const overlay = document.createElement("div");
    overlay.id = "confirm-modal-overlay";
    overlay.className =
      "fixed inset-0 bg-black/40 backdrop-blur-sm z-[9999] flex items-center justify-center px-4";

    const modal = document.createElement("div");
    modal.className =
      "bg-white rounded-2xl shadow-2xl w-full max-w-sm p-6 space-y-4 text-center";

    if (icon) {
      const iconEl = document.createElement("div");
      iconEl.textContent = icon;
      iconEl.className = "text-3xl";
      modal.appendChild(iconEl);
    }

    const messageEl = document.createElement("p");
    messageEl.textContent = message;
    messageEl.className = "text-gray-700 text-base leading-relaxed";
    modal.appendChild(messageEl);

    const buttons = document.createElement("div");
    buttons.className = "flex gap-3 pt-2";

    const cancelBtn = document.createElement("button");
    cancelBtn.type = "button";
    cancelBtn.textContent = cancelText;
    cancelBtn.className = `flex-1 py-2 rounded-xl font-medium transition ${cancelButtonClass}`;
    buttons.appendChild(cancelBtn);

    const okBtn = document.createElement("button");
    okBtn.type = "button";
    okBtn.textContent = okText;
    okBtn.className = `flex-1 py-2 rounded-xl font-semibold transition ${okButtonClass}`;
    buttons.appendChild(okBtn);

    modal.appendChild(buttons);
    overlay.appendChild(modal);
    document.body.appendChild(overlay);

    const cleanup = () => {
      overlay.remove();
      document.removeEventListener("keydown", onKeyDown);
    };

    const handleResolve = (value) => {
      cleanup();
      resolve(value);
    };

    const onKeyDown = (e) => {
      if (e.key === "Escape") handleResolve(false);
      if (e.key === "Enter") handleResolve(true);
    };

    document.addEventListener("keydown", onKeyDown);

    overlay.addEventListener("click", (e) => {
      if (e.target === overlay) handleResolve(false);
    });

    cancelBtn.addEventListener("click", () => handleResolve(false));
    okBtn.addEventListener("click", () => handleResolve(true));
  });
}

async function deleteTask(taskId) {
  const res = await fetch(`/api/tasks/${taskId}`, {
    method: "DELETE",
    headers: (() => {
      const token = getToken();
      const h = {};
      if (token) h.Authorization = "Bearer " + token;
      return h;
    })(),
    credentials: "include",
  });
  if (!res.ok) throw new Error("Delete failed");
}

async function ensureCurrentUser() {
  try {
    const res = await fetch("/api/auth/me", {
      headers: (() => {
        const token = getToken();
        const h = {};
        if (token) h.Authorization = "Bearer " + token;
        return h;
      })(),
      credentials: "include",
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
    const token = getToken();
    const headers = {};
    if (token) {
      headers.Authorization = "Bearer " + token;
    }
    const res = await fetch(`/api/projects/${projectId}/role`, {
      headers,
      credentials: "include",
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
  refreshActivityFeedOnly,
  showActivitySectionIfHidden,
  editReply,
  deleteReply,
  saveReplyEdit,
});
Object.assign(window, {
  updateMemberRole,
});
