// ============================================================
// ✅ CHECKLIST MODULE – Manage Checklists for Tasks
// ============================================================

const checklistPopup = document.getElementById("checklist-popup");
const openChecklistBtn = document.getElementById("open-checklist-btn");
const closeChecklistBtn = document.getElementById("close-checklist-btn");
const addChecklistBtn = document.getElementById("add-checklist-btn");
const checklistTitleInput = document.getElementById("checklist-title-input");
const addChecklistItemBtn = document.getElementById("add-checklist-item-btn");
const checklistItemsList = document.getElementById("checklist-items-list");
const checklistProgress = document.getElementById("checklist-progress");
const checklistProgressText = document.getElementById(
  "checklist-progress-text"
);
const checklistProgressBar = document.getElementById("checklist-progress-bar");
const checklistSection = document.getElementById("checklist-section");

// State để track việc hide/show checked items
let hideCheckedItems = false;
let allItems = []; // Lưu tất cả items để filter

// ================== ROLE HELPERS ==================
function getCurrentUserId() {
  const raw = localStorage.getItem("currentUserId");
  if (!raw) return null;
  const parsed = Number(raw);
  return Number.isNaN(parsed) ? null : parsed;
}

function hasManagerChecklistPrivileges(role) {
  const normalized = (role || window.CURRENT_ROLE || "").toUpperCase();
  return normalized === "ROLE_PM" || normalized === "ROLE_ADMIN";
}

function canDeleteChecklistItem(item = {}) {
  if (hasManagerChecklistPrivileges()) return true;
  const currentUserId = getCurrentUserId();
  const creatorId =
    item?.createdById ??
    (item?.createdBy && typeof item.createdBy === "object"
      ? item.createdBy.userId
      : null);
  if (currentUserId == null || creatorId == null) return false;
  return Number(currentUserId) === Number(creatorId);
}

// Mở popup checklist
function openChecklistPopup(e) {
  if (e) {
    e.stopPropagation();
    e.preventDefault();
  }

  if (!checklistPopup) return;

  // Lấy vị trí của button để đặt popup
  const button = openChecklistBtn;
  if (button) {
    const rect = button.getBoundingClientRect();
    checklistPopup.style.top = `${rect.bottom + 8}px`;
    checklistPopup.style.left = `${rect.left}px`;
  }

  checklistPopup.classList.remove("hidden");
  checklistTitleInput?.focus();
}

// Đóng popup checklist
function closeChecklistPopup(e) {
  if (e) {
    e.stopPropagation();
    e.preventDefault();
  }
  if (checklistPopup) {
    checklistPopup.classList.add("hidden");
  }
  // Reset form
  if (checklistTitleInput) checklistTitleInput.value = "";
}

// Load checklist items
async function loadChecklistItems() {
  const taskId = window.CURRENT_TASK_ID;
  if (!taskId || !checklistItemsList) {
    // Ẩn section nếu không có taskId hoặc checklistItemsList
    if (checklistSection) {
      checklistSection.classList.add("hidden");
    }
    return;
  }

  try {
    const res = await fetch(`/api/checklists/task/${taskId}`, {
      headers: {
        Authorization: "Bearer " + localStorage.getItem("token"),
      },
    });

    if (!res.ok) {
      console.error("❌ Failed to load checklist items");
      // Ẩn section nếu load thất bại (giống due date khi null)
      if (checklistSection) {
        checklistSection.classList.add("hidden");
      }
      return;
    }

    const items = await res.json();
    // Xử lý null/undefined - chuyển thành array rỗng
    const safeItems = Array.isArray(items) ? items : [];

    // Lưu tất cả items để filter
    allItems = safeItems;

    // Ẩn section nếu không có items (giống due date khi deadline = null)
    if (!safeItems || safeItems.length === 0) {
      if (checklistSection) {
        checklistSection.classList.add("hidden");
      }
      return;
    }

    // Hiện section và render items nếu có
    renderChecklistItems(safeItems);
    updateProgress(safeItems);
    updateHideCheckedButton(safeItems);
  } catch (err) {
    console.error("❌ Error loading checklist items:", err);
    // Ẩn section nếu có lỗi (giống due date khi null)
    if (checklistSection) {
      checklistSection.classList.add("hidden");
    }
  }
}

// Render checklist items
function renderChecklistItems(items) {
  if (!checklistItemsList) return;

  // Hiện section khi có items để render
  if (checklistSection) {
    checklistSection.classList.remove("hidden");
  }

  // Xử lý null/undefined/empty - không render gì
  if (!items || !Array.isArray(items) || items.length === 0) {
    checklistItemsList.innerHTML = "";
    return;
  }

  // Filter items: ẩn checked items nếu hideCheckedItems = true
  const filteredItems = hideCheckedItems
    ? items.filter((item) => !item.isDone)
    : items;

  // Nếu không còn items sau khi filter, hiển thị empty state
  if (filteredItems.length === 0 && items.length > 0) {
    checklistItemsList.innerHTML = `
      <p class="text-gray-500 text-center font-normal text-sm py-2">
        Everything in this checklist is complete!
      </p>
    `;
    return;
  }

  const allowToggle = hasManagerChecklistPrivileges();

  checklistItemsList.innerHTML = filteredItems
    .map((item) => {
      const itemCanDelete = canDeleteChecklistItem(item);
      const checkboxDisabledAttr = allowToggle
        ? ""
        : 'disabled data-disabled="true"';
      const checkboxCursorClass = allowToggle
        ? "cursor-pointer"
        : "cursor-not-allowed opacity-60";
      const convertButtonClasses = itemCanDelete
        ? "w-full text-left px-3 py-2 text-sm text-gray-700 hover:bg-gray-100"
        : "w-full text-left px-3 py-2 text-sm text-gray-700 hover:bg-gray-100 rounded-b-md";
      const deleteMenuHtml = itemCanDelete
        ? `<button
          class="w-full text-left px-3 py-2 text-sm text-red-600 hover:bg-gray-100 rounded-b-md"
          data-checklist-id="${item.checklistId}"
          onclick="deleteChecklistItem(${item.checklistId})"
        >
          Delete
        </button>`
        : "";

      return `
    <div class="flex items-center gap-2 group hover:bg-gray-50 rounded px-1 py-0.5 transition-colors relative" data-checklist-id="${
      item.checklistId
    }">
      <input
        type="checkbox"
        ${item.isDone ? "checked" : ""}
        class="h-3.5 w-3.5 text-blue-600 rounded border-gray-300 focus:ring-blue-500 ${checkboxCursorClass} flex-shrink-0 mt-0.5"
        data-checklist-id="${item.checklistId}"
        ${checkboxDisabledAttr}
      />
      <span class="flex-1 text-sm font-normal text-gray-800 ${
        item.isDone ? "line-through text-gray-400" : ""
      }">${escapeHtml(item.item)}</span>
      <div class="hidden group-hover:flex items-center gap-1">
        <button
          class="flex items-center justify-center w-6 h-6 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded transition-colors"
          data-checklist-id="${item.checklistId}"
          title="Due date"
          onclick="openChecklistItemDueDate(${item.checklistId})"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </button>
        <button
          class="flex items-center justify-center w-6 h-6 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded transition-colors"
          data-checklist-id="${item.checklistId}"
          title="Assign"
          onclick="openChecklistItemAssign(${item.checklistId})"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
          </svg>
        </button>
        <div class="relative">
          <button
            class="flex items-center justify-center w-6 h-6 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded transition-colors"
            data-checklist-id="${item.checklistId}"
            title="More options"
            onclick="toggleChecklistItemMenu(${item.checklistId}, event)"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 5v.01M12 12v.01M12 19v.01M12 6a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2z" />
            </svg>
          </button>
        </div>
      </div>
      <!-- Menu được đặt ngoài group-hover container để không bị ẩn khi di chuyển chuột -->
      <div
        id="checklist-item-menu-${item.checklistId}"
        class="hidden fixed bg-white border border-gray-200 rounded-md shadow-lg z-[60] min-w-[160px]"
        onmousedown="event.stopPropagation()"
        onclick="event.stopPropagation()"
      >
        <div class="px-3 py-2 border-b border-gray-200 flex items-center justify-between">
          <h4 class="text-xs font-semibold text-gray-700">Item actions</h4>
          <button
            class="flex items-center justify-center w-5 h-5 text-gray-400 hover:text-blue-600 rounded-full border border-transparent hover:border-blue-600 hover:bg-blue-50 transition-colors"
            onclick="closeChecklistItemMenu(${item.checklistId})"
            title="Close"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        <button
          class="${convertButtonClasses}"
          data-checklist-id="${item.checklistId}"
          onclick="convertChecklistItemToCard(${item.checklistId})"
        >
          Convert to card
        </button>
        ${deleteMenuHtml}
      </div>
    </div>
  `;
    })
    .join("");

  // Attach event listeners for checkboxes
  if (allowToggle) {
    checklistItemsList
      .querySelectorAll('input[type="checkbox"]')
      .forEach((checkbox) => {
        checkbox.addEventListener("change", (e) => {
          const checklistId = e.target.dataset.checklistId;
          const isDone = e.target.checked;
          toggleChecklistItem(checklistId, isDone);
        });
      });
  }
}

// Update progress bar
function updateProgress(items) {
  if (!items || items.length === 0) {
    if (checklistProgress) checklistProgress.classList.add("hidden");
    // Xóa completion message nếu có
    const completionMsg = document.getElementById(
      "checklist-completion-message"
    );
    if (completionMsg) completionMsg.remove();
    return;
  }

  const doneCount = items.filter((item) => item.isDone).length;
  const totalCount = items.length;
  const percentage =
    totalCount > 0 ? Math.round((doneCount / totalCount) * 100) : 0;

  if (checklistProgress) {
    checklistProgress.classList.remove("hidden");
    if (checklistProgressText)
      checklistProgressText.textContent = `${percentage}%`;
    if (checklistProgressBar) {
      checklistProgressBar.style.width = `${percentage}%`;
      // Đổi màu xanh lá khi 100%
      if (percentage === 100) {
        checklistProgressBar.classList.remove("bg-blue-600");
        checklistProgressBar.classList.add("bg-green-600");
      } else {
        checklistProgressBar.classList.remove("bg-green-600");
        checklistProgressBar.classList.add("bg-blue-600");
      }
    }
  }

  const completionMsg = document.getElementById("checklist-completion-message");
  if (percentage === 100) {
    if (!completionMsg && checklistItemsList) {
      const msg = document.createElement("p");
      msg.id = "checklist-completion-message";
      msg.className = "text-gray-600 text-sm font-normal text-center py-2";
      checklistItemsList.insertAdjacentElement("afterend", msg);
    }
  } else {
    if (completionMsg) completionMsg.remove();
  }
}

function toggleHideCheckedItems() {
  hideCheckedItems = !hideCheckedItems;
  renderChecklistItems(allItems);
  updateHideCheckedButton(allItems);
}

function updateHideCheckedButton(items) {
  const hideBtn = document.getElementById("hide-checked-items-btn");
  if (!hideBtn) return;

  const checkedCount = items.filter((item) => item.isDone).length;

  if (checkedCount === 0) {
    hideBtn.classList.add("hidden");
  } else {
    hideBtn.classList.remove("hidden");
    // Cập nhật text dựa trên state
    if (hideCheckedItems) {
      hideBtn.textContent = `Show checked items (${checkedCount})`;
    } else {
      hideBtn.textContent = "Hide checked items";
    }
  }
}

// Toggle checklist item
async function toggleChecklistItem(checklistId, isDone) {
  try {
    const res = await fetch(`/api/checklists/${checklistId}/toggle`, {
      method: "PUT",
      headers: {
        Authorization: "Bearer " + localStorage.getItem("token"),
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ isDone }),
    });

    if (!res.ok) {
      if (res.status === 403) {
        const message = await res.text();
        const text =
          (message && message.trim()) ||
          "You do not have permission to update this checklist item.";
        showToast(text, "error");
      } else {
        console.error(" Failed to toggle checklist item");
        showToast("Failed to toggle checklist item", "error");
      }
      return;
    }

    // Reload checklist items to update UI
    await loadChecklistItems();
  } catch (err) {
    console.error(" Error toggling checklist item:", err);
    showToast("Error toggling checklist item", "error");
  }
}

// Delete checklist item
async function deleteChecklistItem(checklistId) {
  // Close menu if open
  const menu = document.getElementById(`checklist-item-menu-${checklistId}`);
  if (menu) menu.classList.add("hidden");

  try {
    const res = await fetch(`/api/checklists/${checklistId}`, {
      method: "DELETE",
      headers: {
        Authorization: "Bearer " + localStorage.getItem("token"),
      },
    });

    if (!res.ok) {
      if (res.status === 403) {
        const message = await res.text();
        const text =
          (message && message.trim()) ||
          "You do not have permission to delete this checklist item.";
        showToast(text, "error");
      } else {
        console.error(" Failed to delete checklist item");
        showToast("Failed to delete checklist item", "error");
      }
      return;
    }

    // Reload checklist items
    await loadChecklistItems();
  } catch (err) {
    console.error(" Error deleting checklist item:", err);
    showToast("Error deleting checklist item", "error");
  }
}

// Convert checklist item to card
async function convertChecklistItemToCard(checklistId) {
  // Close menu
  const menu = document.getElementById(`checklist-item-menu-${checklistId}`);
  if (menu) menu.classList.add("hidden");

  const taskId = window.CURRENT_TASK_ID;
  if (!taskId) {
    showToast("Task ID not found", "error");
    return;
  }

  try {
    // 1. Lấy thông tin checklist item
    const checklistRes = await fetch(`/api/checklists/task/${taskId}`, {
      headers: {
        Authorization: "Bearer " + localStorage.getItem("token"),
      },
    });

    if (!checklistRes.ok) {
      throw new Error("Failed to load checklist items");
    }

    const items = await checklistRes.json();
    const item = items.find((i) => i.checklistId === checklistId);
    if (!item) {
      throw new Error("Checklist item not found");
    }

    // 2. Lấy thông tin task để có columnId
    const taskRes = await fetch(`/api/tasks/${taskId}`, {
      headers: {
        Authorization: "Bearer " + localStorage.getItem("token"),
      },
    });

    if (!taskRes.ok) {
      throw new Error("Failed to load task");
    }

    const task = await taskRes.json();
    const columnId = task.columnId || task.column?.columnId;
    const projectId =
      window.PROJECT_ID ||
      new URLSearchParams(window.location.search).get("projectId");

    if (!columnId) {
      throw new Error("Column ID not found");
    }

    if (!projectId) {
      throw new Error("Project ID not found");
    }

    // 3. Tạo task mới từ checklist item
    const createRes = await fetch("/api/tasks/quick", {
      method: "POST",
      headers: {
        Authorization: "Bearer " + localStorage.getItem("token"),
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        title: item.item,
        projectId: projectId,
        columnId: columnId,
      }),
    });

    if (!createRes.ok) {
      const errorText = await createRes.text();
      throw new Error(`Failed to create card: ${errorText}`);
    }

    const newTask = await createRes.json();
    console.log(" Card created:", newTask);

    // 4. Xóa checklist item sau khi convert thành công
    const deleteRes = await fetch(`/api/checklists/${checklistId}`, {
      method: "DELETE",
      headers: {
        Authorization: "Bearer " + localStorage.getItem("token"),
      },
    });

    if (!deleteRes.ok) {
      console.warn(" Failed to delete checklist item after conversion");
    }

    // 5. Xóa checklist item
    await loadChecklistItems();

    // 6. Thêm card mới vào kanban board mà không reload trang
    try {
      // Tìm column container - thử nhiều selector
      const columnContainer =
        document.querySelector(`#col-${columnId}`) ||
        document.querySelector(`[data-col="${columnId}"]`) ||
        document.querySelector(`div[id*="col-${columnId}"]`);

      if (columnContainer) {
        // Tìm phần tasks trong column
        const tasksContainer =
          columnContainer.querySelector(".kanban-column-tasks") ||
          columnContainer.querySelector('div[class*="overflow"]') ||
          columnContainer;

        // Tạo card HTML giống như trong addCard function
        const cardHtml = `
          <div id="task-${
            newTask.taskId || newTask.id
          }" class="bg-white border border-slate-200 rounded-md p-3 shadow-sm hover:shadow-md transition-shadow cursor-pointer mb-2" data-open-task="${
          newTask.taskId || newTask.id
        }">
            <p class="font-medium text-slate-800 text-sm">${escapeHtml(
              item.item
            )}</p>
          </div>
        `;

        const addCardArea = tasksContainer.querySelector(".add-card-area");
        if (addCardArea) {
          addCardArea.insertAdjacentHTML("beforebegin", cardHtml);
        } else {
          tasksContainer.insertAdjacentHTML("beforeend", cardHtml);
        }

        console.log(" Card added to board:", newTask.taskId || newTask.id);
      } else {
        console.warn(
          " Column container not found, card was created but not displayed"
        );
      }
    } catch (err) {
      console.warn(" Could not add card to board, but card was created:", err);
    }

    console.log(" Checklist item đã được chuyển thành card mới trong column!");
  } catch (err) {
    console.error(" Error converting checklist item to card:", err);
  }
}

function escapeHtml(text) {
  const div = document.createElement("div");
  div.textContent = text;
  return div.innerHTML;
}

async function addChecklistItem() {
  const taskId = window.CURRENT_TASK_ID;
  if (!taskId) {
    showToast("Task ID not found", "error");
    return;
  }

  const title = checklistTitleInput?.value.trim() || "Add an item";
  if (!title || title === "Add an item") {
    showToast("Please enter a valid item title", "error");
    return;
  }

  try {
    const res = await fetch(`/api/checklists/task/${taskId}`, {
      method: "POST",
      headers: {
        Authorization: "Bearer " + localStorage.getItem("token"),
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ item: title }),
    });

    // Kiểm tra status code - có thể là 200, 201, hoặc các status khác
    if (!res.ok) {
      let errorText = "";
      try {
        errorText = await res.text();
      } catch (e) {
        errorText = "Unknown error";
      }
      console.error(" Server error:", res.status, errorText);
      throw new Error(`Server returned ${res.status}: ${errorText}`);
    }

    // Response OK - thử parse JSON
    let newItem = null;
    try {
      newItem = await res.json();
      console.log(" Checklist item added:", newItem);
    } catch (jsonErr) {
      // Nếu không phải JSON hoặc parse lỗi, nhưng status OK thì vẫn coi là thành công
      // (có thể response là empty hoặc không phải JSON format)
      console.log(
        " Checklist item added (status:",
        res.status + ", response may not be JSON)"
      );
    }

    // Đóng popup nếu thành công
    closeChecklistPopup();

    // Reload checklist items
    await loadChecklistItems();
  } catch (err) {
    console.error(" Failed to add checklist item:", err);
    if (err.message && !err.message.includes("but request succeeded")) {
      showToast("Failed to add checklist item: " + err.message, "error");
    }
  }
}

// Event listeners
if (openChecklistBtn) {
  openChecklistBtn.addEventListener("click", openChecklistPopup);
}

if (addChecklistItemBtn) {
  addChecklistItemBtn.addEventListener("click", openChecklistPopup);
}

if (closeChecklistBtn) {
  closeChecklistBtn.addEventListener("click", closeChecklistPopup);
}

if (addChecklistBtn) {
  addChecklistBtn.addEventListener("click", addChecklistItem);
}

// Hide checked items button
const hideCheckedItemsBtn = document.getElementById("hide-checked-items-btn");
if (hideCheckedItemsBtn) {
  hideCheckedItemsBtn.addEventListener("click", toggleHideCheckedItems);
}

document.addEventListener("click", (e) => {
  if (
    checklistPopup &&
    !checklistPopup.contains(e.target) &&
    !openChecklistBtn?.contains(e.target) &&
    !checklistPopup.classList.contains("hidden")
  ) {
    closeChecklistPopup();
  }
});

if (checklistTitleInput) {
  checklistTitleInput.addEventListener("keydown", (e) => {
    if (e.key === "Enter") {
      e.preventDefault();
      addChecklistItem();
    }
  });
}

function toggleChecklistItemSettings(checklistId, event) {
  if (event) {
    event.stopPropagation();
  }

  const actions = document.getElementById(
    `checklist-item-actions-${checklistId}`
  );
  if (!actions) return;

  // Close all other action panels
  document.querySelectorAll('[id^="checklist-item-actions-"]').forEach((a) => {
    if (a.id !== `checklist-item-actions-${checklistId}`) {
      a.classList.add("hidden");
    }
  });

  actions.classList.toggle("hidden");
}

// Toggle checklist item menu
function toggleChecklistItemMenu(checklistId, event) {
  if (event) {
    event.stopPropagation();
  }

  const menu = document.getElementById(`checklist-item-menu-${checklistId}`);
  if (!menu) return;

  // Close all other menus
  document.querySelectorAll('[id^="checklist-item-menu-"]').forEach((m) => {
    if (m.id !== `checklist-item-menu-${checklistId}`) {
      m.classList.add("hidden");
    }
  });

  const isHidden = menu.classList.contains("hidden");
  if (isHidden) {
    menu.classList.remove("hidden");
    // Đặt vị trí menu dựa trên vị trí của button
    const button = event?.target?.closest(
      'button[onclick*="toggleChecklistItemMenu"]'
    );
    if (button) {
      const rect = button.getBoundingClientRect();
      menu.style.top = `${rect.bottom + 8}px`;
      menu.style.left = `${rect.right - menu.offsetWidth}px`;
    }
  } else {
    menu.classList.add("hidden");
  }
}

// Close checklist item menu
function closeChecklistItemMenu(checklistId) {
  const menu = document.getElementById(`checklist-item-menu-${checklistId}`);
  if (menu) menu.classList.add("hidden");
}

// Open due date for checklist item (placeholder - chưa xử lý)
function openChecklistItemDueDate(checklistId) {
  console.log("Open due date for checklist item:", checklistId);
  // TODO: Implement due date functionality
}

// Open assign for checklist item (placeholder - chưa xử lý)
function openChecklistItemAssign(checklistId) {
  console.log("Open assign for checklist item:", checklistId);
  // TODO: Implement assign functionality
}

// Close menus when clicking outside (chỉ đóng khi click ra ngoài, không đóng khi scroll hoặc di chuyển chuột)
document.addEventListener("mousedown", (e) => {
  // Kiểm tra xem click có phải vào menu hoặc nút toggle không
  const clickedMenu = e.target.closest('[id^="checklist-item-menu-"]');
  const clickedToggle = e.target.closest(
    'button[onclick*="toggleChecklistItemMenu"]'
  );

  // Chỉ đóng menu nếu click ra ngoài menu và không phải nút toggle
  if (!clickedMenu && !clickedToggle) {
    document
      .querySelectorAll('[id^="checklist-item-menu-"]')
      .forEach((menu) => {
        menu.classList.add("hidden");
      });
  }
});

// Make functions available globally for onclick handlers
window.deleteChecklistItem = deleteChecklistItem;
window.toggleChecklistItemSettings = toggleChecklistItemSettings;
window.toggleChecklistItemMenu = toggleChecklistItemMenu;
window.closeChecklistItemMenu = closeChecklistItemMenu;
window.openChecklistItemDueDate = openChecklistItemDueDate;
window.openChecklistItemAssign = openChecklistItemAssign;
window.convertChecklistItemToCard = convertChecklistItemToCard;

export {
  openChecklistPopup,
  closeChecklistPopup,
  addChecklistItem,
  loadChecklistItems,
};
