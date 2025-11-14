// ================== IMPORT UTILS ==================
import { showToast, safeStop } from "./utils.js";
import { updateCardDate } from "./main.js";

// ================== STATE ==================
const datePopup = document.getElementById("date-popup");
const closeDateBtn = document.getElementById("close-date-btn");
const saveDateBtn = document.getElementById("save-date-btn");
const removeDateBtn = document.getElementById("remove-date-btn");
const startCheck = document.getElementById("start-check");
const dueCheck = document.getElementById("due-check");
const startDateInput = document.getElementById("start-date-input");
const startTimeInput = document.getElementById("start-time-input");
const dueDateInput = document.getElementById("due-date-input");
const dueTimeInput = document.getElementById("due-time-input");
const clearStartBtn = document.getElementById("clear-start-btn");
const clearDueBtn = document.getElementById("clear-due-btn");
let activeDateTarget = "due";

const DATE_FORMAT = /^(0?[1-9]|1[0-2])\/(0?[1-9]|[12][0-9]|3[01])\/\d{4}$/;
const TIME_FORMAT = /^(\d{1,2})(?::([0-5]?\d))?\s?(AM|PM)?$/i;

function sanitizeDate(value) {
  if (!value) return "";
  const trimmed = value.trim();
  if (!DATE_FORMAT.test(trimmed)) return "";
  const [month, day, year] = trimmed.split("/").map((p) => Number(p));
  const iso = `${year}-${pad(month)}-${pad(day)}`;
  const test = new Date(iso);
  return Number.isNaN(test.getTime()) ? "" : iso;
}

function sanitizeTime(value) {
  if (!value) return "";
  const trimmed = value.trim().toUpperCase();
  const match = trimmed.match(TIME_FORMAT);
  if (!match) return "";
  let hour = Number(match[1]);
  let minute = Number(match[2] || "0");
  let period = match[3];

  if (Number.isNaN(hour) || Number.isNaN(minute)) return "";
  if (minute < 0 || minute > 59) return "";

  if (!period) {
    // interpret 0-23 inputs similar to Trello
    period = hour >= 12 ? "PM" : "AM";
  }

  // Normalize hour into 12-hour range before converting
  if (hour >= 24) hour = hour % 24;
  if (period) {
    hour = hour % 12;
    if (period === "PM") hour += 12;
  }
  if (hour > 23) return "";

  if (!period) {
    // already 24h formatted, nothing to adjust
  } else if (period === "AM" && hour === 12) {
    hour = 0;
  }

  return `${pad(hour)}:${pad(minute)}`;
}

function displayDate(iso) {
  if (!iso) return "";
  const [year, month, day] = iso.split("-");
  return `${Number(month)}/${Number(day)}/${year}`;
}

function displayTime(time24) {
  if (!time24) return "";
  const [hourStr, minuteStr = "00"] = time24.split(":");
  let hour = Number(hourStr);
  const minute = minuteStr.padStart(2, "0");
  const period = hour >= 12 ? "PM" : "AM";
  hour = hour % 12;
  if (hour === 0) hour = 12;
  return `${hour}:${minute} ${period}`;
}

function setDateValue(input, iso) {
  if (!input) return;
  input.dataset.iso = iso || "";
  input.value = displayDate(iso);
}

function setTimeValue(input, val24) {
  if (!input) return;
  input.dataset.time = val24 || "";
  input.value = displayTime(val24);
}

function readDateValue(input) {
  if (!input) return "";
  return input.dataset.iso || sanitizeDate(input.value);
}

function readTimeValue(input) {
  if (!input) return "";
  return input.dataset.time || sanitizeTime(input.value);
}

// ================== STATUS BADGE ==================
export function updateDateStatus(dueDateStr) {
  const textEl = document.getElementById("due-date-text");
  const statusEl = document.getElementById("due-date-status");

  if (!dueDateStr) {
    textEl.textContent = "No due date";
    statusEl.textContent = "None";
    statusEl.className =
      "ml-2 text-xs font-medium rounded px-2 py-0.5 bg-gray-200 text-gray-600";
    return;
  }

  const due = new Date(dueDateStr);
  const now = new Date();
  const diffHours = (due - now) / (1000 * 60 * 60);
  const formatted = due.toLocaleString("en-US", {
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
  });

  textEl.textContent = formatted;

  if (diffHours < 0) {
    statusEl.textContent = "Overdue";
    statusEl.className =
      "ml-2 text-xs font-bold rounded px-2 py-0.5 bg-red-100 text-red-600";
    // Ensure font-bold is applied
    statusEl.style.fontWeight = "700";
  } else if (diffHours <= 24) {
    statusEl.textContent = "Due soon";
    statusEl.className =
      "ml-2 text-xs font-medium rounded px-2 py-0.5 bg-yellow-100 text-yellow-700";
  } else {
    statusEl.textContent = "On track";
    statusEl.className =
      "ml-2 text-xs font-medium rounded px-2 py-0.5 bg-green-100 text-green-700";
  }
}

// Helper function để cập nhật UI date section
function updateDateSectionUI(deadline) {
  updateDateStatus(deadline || null);
  const dueDateSection = document.getElementById("due-date-section");
  if (dueDateSection) {
    if (deadline) {
      dueDateSection.classList.remove("hidden");
    } else {
      dueDateSection.classList.add("hidden");
    }
  }
}

// Helper function để parse ISO datetime thành date và time
function parseDateTime(isoString) {
  if (!isoString) return { date: "", time: "" };
  try {
    const date = new Date(isoString);
    if (isNaN(date.getTime())) return { date: "", time: "" };

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    const dateIso = `${year}-${month}-${day}`;

    const hours = String(date.getHours()).padStart(2, "0");
    const minutes = String(date.getMinutes()).padStart(2, "0");
    const time24 = `${hours}:${minutes}`;

    return { date: dateIso, time: time24 };
  } catch {
    return { date: "", time: "" };
  }
}

// Load task dates vào popup
async function loadTaskDatesIntoPopup() {
  const taskId = window.CURRENT_TASK_ID;
  if (!taskId) return;

  try {
    const res = await fetch(`/api/tasks/${taskId}`, {
      headers: {
        Authorization: "Bearer " + localStorage.getItem("token"),
      },
    });

    if (!res.ok) return;
    const task = await res.json();

    // Load start date
    if (task.startDate) {
      const { date, time } = parseDateTime(task.startDate);
      startCheck.checked = true;
      toggleStartFields(true);
      setDateValue(startDateInput, date);
      setTimeValue(startTimeInput, time);
    } else {
      startCheck.checked = false;
      toggleStartFields(false);
      setDateValue(startDateInput, "");
      setTimeValue(startTimeInput, "");
    }

    // Load due date
    if (task.deadline) {
      const { date, time } = parseDateTime(task.deadline);
      dueCheck.checked = true;
      toggleDueFields(true);
      setDateValue(dueDateInput, date);
      setTimeValue(dueTimeInput, time);
    } else {
      dueCheck.checked = false;
      toggleDueFields(false);
      setDateValue(dueDateInput, "");
      setTimeValue(dueTimeInput, "");
    }

    // Render calendar với dates mới
    renderCalendar();
  } catch (err) {
    console.error(" Error loading task dates:", err);
  }
}

// ================== OPEN POPUP ==================
export function openDatePopup(e) {
  console.log("🔍 openDatePopup called");
  safeStop(e);

  let rect = null;
  if (e?.currentTarget?.getBoundingClientRect) {
    rect = e.currentTarget.getBoundingClientRect();
  }

  const isValidRect =
    rect && rect.top > 0 && rect.left > 0 && rect.width > 0 && rect.height > 0;
  let top, left;

  if (!isValidRect) {
    // Gọi từ context menu → dùng tọa độ chuột
    top = (window.contextMenuY || 100) + 10;
    left = (window.contextMenuX || 100) + 10;
  } else {
    // Gọi từ nút modal → dùng vị trí button
    top = rect.bottom + window.scrollY + 6;
    left = rect.left + window.scrollX;
  }

  datePopup.style.top = `${top}px`;
  datePopup.style.left = `${left}px`;
  datePopup.classList.remove("hidden");

  // Load task dates vào popup khi mở
  loadTaskDatesIntoPopup();

  console.log(" Date popup displayed at:", { top, left });
}

// ================== CLOSE POPUP ==================
export function closeDatePopup() {
  datePopup.classList.add("hidden");
  console.log("Date popup closed");
}

// ================== EVENT BINDINGS ==================
if (closeDateBtn) closeDateBtn.addEventListener("click", closeDatePopup);

document.addEventListener("mousedown", (e) => {
  const inside = datePopup.contains(e.target);
  const fromContextMenu = e.target.closest("#card-context-menu");
  if (!inside && !fromContextMenu && e.button !== 2) closeDatePopup();
});

function toggleStartFields(enabled) {
  [startDateInput, startTimeInput].forEach((input) => {
    if (!input) return;
    input.disabled = !enabled;
    input.classList.toggle("disabled", !enabled);
    if (!enabled) {
      setDateValue(startDateInput, "");
      setTimeValue(startTimeInput, "");
    }
  });
  if (enabled) activeDateTarget = "start";
}

function toggleDueFields(enabled) {
  [dueDateInput, dueTimeInput].forEach((input) => {
    if (!input) return;
    input.disabled = !enabled;
    input.classList.toggle("disabled", !enabled);
    if (!enabled) {
      setDateValue(dueDateInput, "");
      setTimeValue(dueTimeInput, "");
    }
  });
  if (enabled) activeDateTarget = "due";
}

startCheck?.addEventListener("change", () => {
  const enabled = startCheck.checked;
  toggleStartFields(enabled);
  if (!enabled) {
    renderCalendar();
  } else {
    activeDateTarget = "start";
  }
});

dueCheck?.addEventListener("change", () => {
  const enabled = dueCheck.checked;
  toggleDueFields(enabled);
  if (!enabled) {
    renderCalendar();
  } else {
    activeDateTarget = "due";
  }
});

clearStartBtn?.addEventListener("click", (e) => {
  e.preventDefault();
  startCheck.checked = false;
  toggleStartFields(false);
  renderCalendar();
});

clearDueBtn?.addEventListener("click", (e) => {
  e.preventDefault();
  dueCheck.checked = false;
  toggleDueFields(false);
  renderCalendar();
});

removeDateBtn?.addEventListener("click", async () => {
  const taskId = window.CURRENT_TASK_ID;
  if (!taskId) return;

  try {
    // Sử dụng API mới để remove deadline
    const res = await fetch(`/api/tasks/${taskId}/deadline/remove`, {
      method: "PUT",
      headers: {
        Authorization: "Bearer " + localStorage.getItem("token"),
        "Content-Type": "application/json",
      },
    });

    if (!res.ok) {
      const errorData = await res.json().catch(() => ({}));
      throw new Error(errorData.message || "Failed to remove deadline");
    }

    const response = await res.json();

    // Reload task data từ server để đảm bảo đồng bộ với database
    try {
      const taskRes = await fetch(`/api/tasks/${taskId}`, {
        headers: {
          Authorization: "Bearer " + localStorage.getItem("token"),
        },
      });
      if (taskRes.ok) {
        const task = await taskRes.json();
        // Cập nhật UI với dữ liệu mới nhất từ server (deadline sẽ là null)
        updateDateSectionUI(task.deadline);
        // Cập nhật card bên ngoài
        updateCardDate(taskId, task.deadline);
      } else {
        // Nếu reload thất bại, set deadline thành null
        updateDateSectionUI(null);
        updateCardDate(taskId, null);
      }
    } catch (reloadErr) {
      console.warn(" Could not reload task data:", reloadErr);
      updateDateSectionUI(null);
      updateCardDate(taskId, null);
    }
  } catch (err) {
    console.error(" Error removing deadline:", err);
    showToast(err.message || "Failed to remove deadline", "error");
  } finally {
    closeDatePopup();
  }
});

function composeDateTime(dateVal, timeVal) {
  if (!dateVal) return null;
  const time = timeVal || "00:00";
  return `${dateVal}T${time}`;
}

saveDateBtn?.addEventListener("click", async () => {
  const taskId = window.CURRENT_TASK_ID;
  if (!taskId) return;

  const startDateIso = startCheck.checked ? readDateValue(startDateInput) : "";
  const startTimeVal = startCheck.checked ? readTimeValue(startTimeInput) : "";
  const dueDateIso = dueCheck.checked ? readDateValue(dueDateInput) : "";
  const dueTimeVal = dueCheck.checked ? readTimeValue(dueTimeInput) : "";

  const start = startDateIso
    ? composeDateTime(startDateIso, startTimeVal || "00:00")
    : null;
  const due = dueDateIso
    ? composeDateTime(dueDateIso, dueTimeVal || "00:00")
    : null;

  try {
    const res = await fetch(`/api/tasks/${taskId}/dates`, {
      method: "PUT",
      headers: {
        Authorization: "Bearer " + localStorage.getItem("token"),
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        startDate: start,
        deadline: due,
      }),
    });

    if (!res.ok) {
      const errorData = await res.json().catch(() => ({}));
      throw new Error(errorData.message || "Failed to save date");
    }

    const updated = await res.json();

    // Reload task data từ server để đảm bảo đồng bộ với database
    try {
      const taskRes = await fetch(`/api/tasks/${taskId}`, {
        headers: {
          Authorization: "Bearer " + localStorage.getItem("token"),
        },
      });
      if (taskRes.ok) {
        const task = await taskRes.json();
        // Cập nhật UI modal với dữ liệu mới nhất từ server
        updateDateSectionUI(task.deadline);
        // Cập nhật card bên ngoài column
        updateCardDate(taskId, task.deadline);
      } else {
        // Nếu reload thất bại, dùng dữ liệu từ response ban đầu
        updateDateSectionUI(updated.deadline);
        updateCardDate(taskId, updated.deadline);
      }
    } catch (reloadErr) {
      console.warn(" Could not reload task data:", reloadErr);
      // Fallback: dùng dữ liệu từ response ban đầu
      updateDateSectionUI(updated.deadline);
      updateCardDate(taskId, updated.deadline);
    }

    closeDatePopup();
  } catch (err) {
    console.error(" saveDate error:", err);
    showToast(err.message || "Failed to save date", "error");
  }
});
// Mini calendar rendering
const calendarEl = document.getElementById("calendar-days");
const monthLabel = document.getElementById("calendar-month");
let current = new Date();

function pad(num) {
  return String(num).padStart(2, "0");
}

function formatDateKey(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(
    date.getDate()
  )}`;
}

function renderCalendar() {
  if (!calendarEl || !monthLabel) return;
  const year = current.getFullYear();
  const month = current.getMonth();
  const firstDay = new Date(year, month, 1).getDay();
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const startKey = startDateInput?.dataset?.iso || "";
  const dueKey = dueDateInput?.dataset?.iso || "";

  monthLabel.textContent = current.toLocaleString("en-US", {
    month: "long",
    year: "numeric",
  });
  calendarEl.innerHTML = "";

  for (let i = 0; i < firstDay; i++) {
    const spacer = document.createElement("div");
    spacer.className = "calendar-day placeholder";
    calendarEl.appendChild(spacer);
  }

  for (let day = 1; day <= daysInMonth; day++) {
    const date = new Date(year, month, day);
    const dayKey = formatDateKey(date);
    const dayBtn = document.createElement("button");
    dayBtn.type = "button";
    dayBtn.className = "calendar-day";
    if (dayKey === startKey) dayBtn.classList.add("selected-start");
    if (dayKey === dueKey) dayBtn.classList.add("selected-due");
    dayBtn.dataset.date = dayKey;
    dayBtn.textContent = day;
    calendarEl.appendChild(dayBtn);
  }

  // chọn ngày -> tự điền vào due date
  calendarEl.querySelectorAll("button[data-date]").forEach((d) => {
    d.addEventListener("click", () => {
      const localDate = d.getAttribute("data-date");
      let target = activeDateTarget;
      if (target === "start" && !startCheck.checked && dueCheck.checked) {
        target = "due";
      }
      if (target === "due" && !dueCheck.checked && startCheck.checked) {
        target = "start";
      }

      if (target === "start") {
        if (!startCheck.checked) {
          startCheck.checked = true;
          toggleStartFields(true);
        }
        setDateValue(startDateInput, localDate);
        if (!startTimeInput.dataset.time) {
          setTimeValue(startTimeInput, "12:00");
        }
        activeDateTarget = "start";
      } else {
        if (!dueCheck.checked) {
          dueCheck.checked = true;
          toggleDueFields(true);
        }
        setDateValue(dueDateInput, localDate);
        if (!dueTimeInput.dataset.time) {
          setTimeValue(dueTimeInput, "12:00");
        }
        activeDateTarget = "due";
      }
      renderCalendar();
    });
  });
}

const prevMonthBtn = document.getElementById("prev-month");
const nextMonthBtn = document.getElementById("next-month");

prevMonthBtn?.addEventListener("click", () => {
  current.setMonth(current.getMonth() - 1);
  renderCalendar();
});

nextMonthBtn?.addEventListener("click", () => {
  current.setMonth(current.getMonth() + 1);
  renderCalendar();
});

toggleStartFields(Boolean(startCheck?.checked));
toggleDueFields(Boolean(dueCheck?.checked));

[startDateInput, dueDateInput].forEach((input) => {
  input?.addEventListener("input", () => {
    input.value = input.value.replace(/[^0-9/]/g, "");
  });

  input?.addEventListener("blur", () => {
    if (!input.value.trim()) {
      setDateValue(input, "");
      return;
    }
    const iso = sanitizeDate(input.value);
    if (iso) {
      setDateValue(input, iso);
      renderCalendar();
    } else {
      input.classList.add("input-error");
      setTimeout(() => input.classList.remove("input-error"), 1500);
      input.value = "";
      input.dataset.iso = "";
    }
  });

  input?.addEventListener("focus", () => {
    activeDateTarget = input === startDateInput ? "start" : "due";
  });
});

[startTimeInput, dueTimeInput].forEach((input) => {
  input?.addEventListener("input", () => {
    input.value = input.value.replace(/[^0-9:amp ]/gi, "");
  });

  input?.addEventListener("blur", () => {
    if (!input.value.trim()) {
      setTimeValue(input, "");
      return;
    }
    const val = sanitizeTime(input.value);
    if (val) {
      setTimeValue(input, val);
    } else {
      input.classList.add("input-error");
      setTimeout(() => input.classList.remove("input-error"), 1500);
      input.value = "";
      input.dataset.time = "";
    }
  });

  input?.addEventListener("focus", () => {
    activeDateTarget = input === startTimeInput ? "start" : "due";
  });
});

if (calendarEl && monthLabel) {
  renderCalendar();
}
