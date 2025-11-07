// ✅ board.js – Quản lý cột và thẻ trong Kanban
import { escapeHtml, apiFetch, getColorForId, showToast } from "./utils.js";
import { PROJECT_ID } from "./main.js";

let columns = [];

/* ---------------------- 🧱 LOAD DATA ---------------------- */
export async function loadColumns(projectId) {
  try {
    const res = await apiFetch(`/api/columns/project/${projectId}`);
    columns = await res.json();
    return columns;
  } catch (err) {
    console.error("❌ loadColumns failed:", err);
    showToast("Không thể tải danh sách cột.", "error");
    return [];
  }
}

export async function loadTasks(columnId) {
  try {
    const res = await apiFetch(`/api/tasks/column/${columnId}`);
    const tasks = await res.json();
    return Array.isArray(tasks) ? tasks : [];
  } catch (err) {
    console.error(`❌ loadTasks failed for column ${columnId}:`, err);
    return [];
  }
}

/* ---------------------- 🧩 RENDER ---------------------- */
export async function renderDashboard(projectId) {
  const board = document.getElementById("kanban-board");
  if (!board) return;

  board.innerHTML = `<p class="text-white opacity-80 italic">Loading board...</p>`;
  const cols = await loadColumns(projectId);
  if (!cols.length) {
    board.innerHTML = `<p class="text-white italic opacity-70">No columns found.</p>`;
    return;
  }

  board.innerHTML = "";

  for (const col of cols) {
    const column = document.createElement("div");
    column.className = "bg-white/90 rounded-lg shadow-md p-3 min-w-[260px] flex flex-col";
    column.dataset.columnId = col.columnId;

    column.innerHTML = `
      <div class="flex justify-between items-center mb-2">
        <h3 class="font-semibold text-gray-800">${escapeHtml(col.name)}</h3>
        <button class="text-gray-400 hover:text-gray-700 text-sm">⋯</button>
      </div>
      <div class="space-y-2" id="task-list-${col.columnId}">
        <p class="text-gray-400 italic text-sm">Loading tasks...</p>
      </div>
      <button data-add-task="${col.columnId}"
              class="mt-2 w-full text-left text-sm text-blue-600 hover:bg-blue-50 px-2 py-1 rounded transition">
        + Add task
      </button>
    `;

    board.appendChild(column);
    const tasks = await loadTasks(col.columnId);
    renderTasks(col.columnId, tasks);
  }
}

/* ---------------------- 🗂️ TASKS ---------------------- */
function renderTasks(columnId, tasks) {
  const list = document.getElementById(`task-list-${columnId}`);
  if (!list) return;
  list.innerHTML = "";

  if (!tasks.length) {
    list.innerHTML = `<p class="text-gray-400 italic text-sm">No tasks yet.</p>`;
    return;
  }

  for (const t of tasks) {
    const card = renderCard(t);
    list.appendChild(card);
  }
}

export function renderCard(task) {
  const card = document.createElement("div");
  card.className = "bg-white border border-gray-200 rounded-md p-2 shadow-sm hover:shadow transition cursor-pointer";
  card.dataset.openTask = task.taskId;

  const labelHtml = (task.labels || [])
    .map(l => `<span class="text-[10px] px-2 py-[1px] rounded-full text-white font-medium"
                    style="background:${getColorForId(l.labelId)}">${escapeHtml(l.name)}</span>`)
    .join(" ");

  card.innerHTML = `
    <div class="flex justify-between items-start mb-1">
      <h4 class="text-sm font-semibold text-gray-800">${escapeHtml(task.title || "Untitled")}</h4>
      <button class="text-gray-400 hover:text-gray-600 text-sm">⋯</button>
    </div>
    ${labelHtml ? `<div class="mb-1 space-x-1">${labelHtml}</div>` : ""}
    ${task.dueDate ? `<p class="text-xs text-gray-500">🕒 ${escapeHtml(task.dueDate)}</p>` : ""}
  `;

  // Mở modal chi tiết
  card.addEventListener("click", () => openTaskModal(task.taskId));
  return card;
}

/* ---------------------- ⚙️ UTILS ---------------------- */
function openTaskModal(taskId) {
  const modal = document.getElementById("task-detail-modal");
  if (!modal) return;
  modal.classList.remove("hidden");
  console.log("🪟 Open modal for task:", taskId);
}
