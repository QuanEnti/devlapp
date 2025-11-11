// 🟦 SHARE BUTTON EVENT
    document.getElementById("share-board-btn").addEventListener("click", () => {
      openSharePopup(); // mở popup có sẵn
    });
    // ✅ Giúp xử lý mọi loại event an toàn (Event hoặc object thường)
    function safeStop(e) {
      if (e && typeof e.stopPropagation === "function") {
        e.stopPropagation();
      }
    }


      // ================== GLOBAL ==================
      const params = new URLSearchParams(window.location.search);
      const PROJECT_ID =
        window.PROJECT_ID ||
        new URLSearchParams(window.location.search).get("projectId") ||
        1;
      window.PROJECT_ID = PROJECT_ID; // ✅ đảm bảo toàn cục

      const modal = document.getElementById('task-detail-modal');
      const closeModalBtn = document.getElementById('close-modal-btn');

      // ================== BOARD ==================
      async function loadColumns(projectId) {
        const res = await fetch(`/api/columns/project/${projectId}`, {
          headers: { "Authorization": "Bearer " + localStorage.getItem("token") }
        });
        if (!res.ok) throw new Error("Không thể tải danh sách cột");
        return await res.json();
      }

    async function loadTasks(projectId) {
      const res = await fetch(`/api/tasks/project/${projectId}`, {
        headers: { "Authorization": "Bearer " + localStorage.getItem("token") }
      });
      if (!res.ok) throw new Error("Không thể tải danh sách task");
      return await res.json();
    }

      function groupByColumn(tasks) {
        const groups = {};
        tasks.forEach(t => {
          const col = t.columnName || "Backlog";
          if (!groups[col]) groups[col] = [];
          groups[col].push(t);
        });
        return groups;
      }

      async function renderDashboard(projectId) {
        try {
          const [columns, tasks] = await Promise.all([loadColumns(projectId), loadTasks(projectId)]);
          const grouped = groupByColumn(tasks);
          const board = document.getElementById("kanban-board");
          board.innerHTML = "";

        columns.forEach(col => {
      const items = grouped[col.name] || [];
      const htmlTasks = items.length
        ? items.map(renderCard).join("")
        : `<div class="text-sm text-slate-400 italic">Chưa có thẻ</div>`;

      board.innerHTML += `
        <div class="kanban-list relative bg-white/95 backdrop-blur-md rounded-xl shadow-md p-4 min-w-[320px] flex flex-col justify-between">
          <div>
            <div class="flex justify-between items-center mb-3">
              <h3 class="font-semibold text-slate-800">${col.name}</h3>

              <!-- 🔹 Nút 3 chấm -->
              <button class="list-options-btn text-gray-500 hover:text-gray-700 text-xl">⋯</button>

              <!-- 🔹 Popup menu -->
              <div class="list-options-menu hidden absolute top-10 right-2 bg-white border border-gray-300 rounded-md shadow-lg w-52 z-50">
                <p class="px-3 py-2 text-xs text-gray-500 border-b">List actions</p>
                <button class="block w-full text-left px-3 py-2 hover:bg-gray-100 text-sm">Add card</button>
                <button class="block w-full text-left px-3 py-2 hover:bg-gray-100 text-sm">Copy list</button>
                <button class="block w-full text-left px-3 py-2 hover:bg-gray-100 text-sm">Move list</button>
                <button class="block w-full text-left px-3 py-2 hover:bg-gray-100 text-sm text-red-600">Archive list</button>
              </div>
            </div>

            <div class="space-y-3 min-h-[60px] pb-10" id="col-${col.columnId}">
              ${htmlTasks}
              <div class="drop-zone h-8"></div>
            </div>
          </div>

          <div class="mt-4">
            <div class="add-card-area" data-col="${col.columnId}">
              <button class="w-full text-left text-slate-500 hover:text-blue-600 text-sm font-medium"
                      data-add-card="${col.columnId}">
                ＋ Add a card
              </button>
            </div>
          </div>
        </div>
      `;
    });

          document.querySelectorAll("[data-add-card]").forEach(btn => {
            btn.addEventListener("click", () => showAddCardInput(btn.getAttribute("data-add-card")));
          }); 
          enableDragDrop();
        } catch (e) {
          console.error("⚠️ Lỗi khi render board:", e);
        }
      }

      function renderCard(t) {
      const taskId = t.id || t.taskId;

      // 🔹 Render danh sách label mini
      const labelHtml = (t.labels && Array.isArray(t.labels) && t.labels.length)
        ? t.labels.map(l => `
            <span class="inline-block text-[10px] px-2 py-[1px] rounded-md text-white mr-1"
                  style="background:${l.color || '#94a3b8'}">
              ${escapeHtml(l.name)}
            </span>
          `).join('')
        : '';

      return `
        <div data-open-task="${taskId}"
            class="bg-white border border-slate-200 rounded-md p-3 shadow-sm hover:shadow-md transition cursor-pointer">
          
          <div class="flex flex-wrap gap-1 mb-1">
            ${labelHtml}
          </div>

          <p class="font-medium text-slate-800 text-sm">${escapeHtml(t.title)}</p>
        </div>
      `;
    }

    // ================== LIST MENU (⋯) ==================
    document.addEventListener("click", e => {
      // mở popup
      const btn = e.target.closest(".list-options-btn");
      if (btn) {
        e.stopPropagation();
        const menu = btn.parentElement.querySelector(".list-options-menu");

        // đóng popup khác
        document.querySelectorAll(".list-options-menu").forEach(m => {
          if (m !== menu) m.classList.add("hidden");
        });

        // toggle popup hiện tại
        menu.classList.toggle("hidden");
        return;
      }

      // click ra ngoài => đóng hết
      if (!e.target.closest(".list-options-menu")) {
        document.querySelectorAll(".list-options-menu").forEach(m => m.classList.add("hidden"));
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

        document.querySelector(`[data-add-card-confirm="${columnId}"]`)
          .addEventListener("click", () => addCard(columnId));
        document.querySelector(`[data-add-card-cancel="${columnId}"]`)
          .addEventListener("click", () => cancelAddCard(columnId));
        document.getElementById(`new-card-title-${columnId}`).focus();
      }

      function cancelAddCard(columnId) {
        const area = document.querySelector(`.add-card-area[data-col='${columnId}']`);
        area.innerHTML = `
          <button class="w-full text-left text-slate-500 hover:text-blue-600 text-sm font-medium"
                  data-add-card="${columnId}">
            ＋ Add a card
          </button>
        `;
        area.querySelector("[data-add-card]").addEventListener("click", () => showAddCardInput(columnId));
      }

      async function addCard(columnId) {
        const textarea = document.getElementById(`new-card-title-${columnId}`);
        const title = textarea.value.trim();
        if (!title) return;

        // ⚡ Hiển thị card tạm
        const colContainer = document.getElementById(`col-${columnId}`);
        const tempId = "temp-" + Date.now();
        colContainer.insertAdjacentHTML("beforeend", `
          <div id="${tempId}" class="bg-white border border-slate-200 rounded-md p-3 shadow-sm opacity-60">
            <p class="font-medium text-slate-800 text-sm">${escapeHtml(title)}</p>
          </div>
        `);
        textarea.disabled = true;

        try {
          const res = await fetch("/api/tasks/quick", {
            method: "POST",
            headers: {
              "Authorization": "Bearer " + localStorage.getItem("token"),
              "Content-Type": "application/json"
            },
            body: JSON.stringify({ title, projectId: PROJECT_ID, columnId })
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

      // ================== MODAL ==================
      function escapeHtml(str) {
        return (str || "").replace(/[&<>"']/g, s => (
          { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[s]
        ));
      }

      document.addEventListener("click", (e) => {
        const openBtn = e.target.closest("[data-open-task]");
        if (openBtn) openModal(openBtn.getAttribute("data-open-task"));
      });

      async function openModal(taskId) {
        try {
          const res = await fetch(`/api/tasks/${taskId}`, {
            headers: {
              "Authorization": "Bearer " + localStorage.getItem("token")
            }
          });

          if (!res.ok) throw new Error("Không thể tải chi tiết task");
          const task = await res.json();
          document.querySelector("#task-detail-modal h2").textContent = task.title || "Untitled";
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

      function closeModal() { modal.classList.add("hidden"); }
      closeModalBtn.addEventListener("click", closeModal);
      modal.addEventListener("click", (e) => { if (e.target === modal) closeModal(); });
      document.addEventListener("keydown", (e) => { if (e.key === "Escape") { closeModal(); closeMemberPopup(); }});

    // ================== DESCRIPTION ==================
      const descDisplay = document.getElementById('description-display');
      const descContent = document.getElementById('description-content');
      const descPlaceholder = document.getElementById('description-placeholder');
      const descEditor = document.getElementById('description-editor');
      const descTextarea = document.getElementById('description-textarea');
      const editDescBtn = document.getElementById('edit-desc-btn');
      const saveDescBtn = document.getElementById('save-desc-btn');
      const cancelDescBtn = document.getElementById('cancel-desc-btn');

      function showDescriptionEditor() {
        descDisplay.classList.add('hidden');
        descEditor.classList.remove('hidden');
        descTextarea.value = descContent.textContent.trim() || "";
        descTextarea.focus();
      }
      function hideDescriptionEditor() {
        descEditor.classList.add('hidden');
        descDisplay.classList.remove('hidden');
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
          "Authorization": "Bearer " + localStorage.getItem("token"),
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ description_md: newDescription })
      });

      // Nếu server trả lỗi 500 nhưng là lỗi lazy load → vẫn coi là thành công
      if (!res.ok) {
        const msg = await res.text();
        console.warn("⚠️ Server response:", res.status, msg);

        if (msg.includes("could not initialize proxy") || msg.includes("no Session")) {
          console.log("✅ Saved successfully (proxy serialization error ignored).");
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



      // ================== MEMBERS ==================
      const membersPopup = document.getElementById('members-popup');
      const openMembersBtn = document.getElementById('open-members-btn');
      const closeMembersBtn = document.getElementById('close-members-btn');
      const searchInput = document.getElementById('search-member-input');

      openMembersBtn.addEventListener("click", (e) => openMemberPopup(e));
      closeMembersBtn.addEventListener("click", closeMemberPopup);
    document.addEventListener("click", e => {
        const isInsidePopup = membersPopup.contains(e.target);
        const isOpenBtn = e.target.closest("#open-members-btn");
        const isContextMenu = e.target.closest("#card-context-menu");

        if (!isInsidePopup && !isOpenBtn && !isContextMenu) {
          closeMemberPopup();
        }
      });



      function closeMemberPopup() { membersPopup.classList.add("hidden"); }

      // ================== MEMBER HANDLING ==================
    async function loadMembers(keyword = "") {
      const taskId = window.CURRENT_TASK_ID;
      if (!taskId) return;

      const listContainer = document.getElementById("members-section");
      listContainer.innerHTML = `<p class="text-gray-400 text-sm italic">Loading...</p>`;

      try {
        // 🔹 Thêm Authorization header cho tất cả API
        const headers = { "Authorization": "Bearer " + localStorage.getItem("token") };

        // Lấy danh sách toàn bộ thành viên trong project
        const resAll = await fetch(
          `/api/pm/members?projectId=${PROJECT_ID}&keyword=${encodeURIComponent(keyword)}`,
          { headers }
        );
        if (!resAll.ok) throw new Error("Failed to load project members");
        const allPayload = await resAll.json();
        const allMembers = Array.isArray(allPayload.content) ? allPayload.content : allPayload;

        // Lấy danh sách thành viên đã được gán cho task
        const resTask = await fetch(`/api/tasks/${taskId}/members`, { headers });
        if (!resTask.ok) throw new Error("Failed to load task members");
        const taskMembers = await resTask.json();
        const assignedIds = (taskMembers || []).map(m => m.userId);

        // Render danh sách ra popup
        listContainer.innerHTML = allMembers.map(m => `
          <div class="flex items-center justify-between p-1 hover:bg-gray-100 rounded-md">
            <div class="flex items-center gap-2">
              ${renderAvatar(m)}
              <p class="text-sm text-gray-700">${m.name}</p>
            </div>
            <button
              onclick="${assignedIds.includes(m.userId)
                ? `unassignMember(${m.userId})`
                : `assignMember(${m.userId})`}"
              class="${assignedIds.includes(m.userId)
                ? 'text-red-500 hover:text-red-700'
                : 'text-gray-400 hover:text-blue-500'} text-lg"
              title="${assignedIds.includes(m.userId) ? 'Remove' : 'Add'}">
              ${assignedIds.includes(m.userId) ? '×' : '＋'}
            </button>
          </div>
        `).join('');
      } catch (err) {
        console.error("❌ Error loading members:", err);
        listContainer.innerHTML = `<p class="text-red-500 text-sm">Error loading members</p>`;
      }
    }


      async function assignMember(userId) {
      const taskId = window.CURRENT_TASK_ID;
      if (!taskId) return;

      try {
        const res = await fetch(`/api/tasks/${taskId}/assign/${userId}`, {
          method: "PUT",
          headers: { "Authorization": "Bearer " + localStorage.getItem("token") }
        });

        if (!res.ok) throw new Error("Failed to assign member");
        await loadMembers();
      } catch (err) {
        console.error("❌ assignMember error:", err);
        alert("❌ Failed to assign member");
      }
    }

    async function unassignMember(userId) {
      const taskId = window.CURRENT_TASK_ID;
      if (!taskId) return;

      try {
        const res = await fetch(`/api/tasks/${taskId}/unassign/${userId}`, {
          method: "PUT",
          headers: { "Authorization": "Bearer " + localStorage.getItem("token") }
        });

        if (!res.ok) throw new Error("Failed to unassign member");
        await loadMembers();
      } catch (err) {
        console.error("❌ unassignMember error:", err);
        alert("❌ Failed to unassign member");
      }
    }


      function renderAvatar(m) {
        if (m.avatarUrl?.trim())
          return `<img src="${m.avatarUrl}" alt="${m.name}" class="w-8 h-8 rounded-full border border-gray-300 object-cover">`;
        const initials = getInitials(m.name);
        const color = getColorForId(m.userId);
        return `<div class="w-8 h-8 flex items-center justify-center rounded-full text-xs font-semibold text-white"
                    style="background-color:${color}">${initials}</div>`;
      }

      function getInitials(name) {
        if (!name) return "?";
        const w = name.trim().split(" ");
        return w.length > 1 ? (w[0][0] + w[w.length - 1][0]).toUpperCase() : w[0][0].toUpperCase();
      }

      function getColorForId(id) {
        const colors = ["#f59e0b","#10b981","#3b82f6","#8b5cf6","#ec4899","#ef4444","#14b8a6","#f97316","#6366f1","#84cc16"];
        const i = typeof id === "number"
          ? id % colors.length
          : id.toString().split("").reduce((a,c)=>a+c.charCodeAt(0),0)%colors.length;
        return colors[i];
      }

      let debounceTimer;
      searchInput.addEventListener('input', e => {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => loadMembers(e.target.value.trim()), 300);
      });




      function updateDateStatus(dueDateStr) {
      const textEl = document.getElementById("due-date-text");
      const statusEl = document.getElementById("due-date-status");

      if (!dueDateStr) {
        textEl.textContent = "No due date";
        statusEl.textContent = "None";
        statusEl.className = "ml-2 text-xs font-medium rounded px-2 py-0.5 bg-gray-200 text-gray-600";
        return;
      }

      const due = new Date(dueDateStr);
      const now = new Date();
      const diffMs = due - now;
      const diffHours = diffMs / (1000 * 60 * 60);

      // Format hiển thị: Oct 23, 5:51 PM
      const options = { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' };
      const formatted = due.toLocaleString('en-US', options);
      textEl.textContent = formatted;

      // Badge màu
      if (diffHours < 0) {
        statusEl.textContent = "Overdue";
        statusEl.className = "ml-2 text-xs font-medium rounded px-2 py-0.5 bg-red-100 text-red-700";
      } else if (diffHours <= 24) {
        statusEl.textContent = "Due soon";
        statusEl.className = "ml-2 text-xs font-medium rounded px-2 py-0.5 bg-yellow-100 text-yellow-700";
      } else {
        statusEl.textContent = "On track";
        statusEl.className = "ml-2 text-xs font-medium rounded px-2 py-0.5 bg-green-100 text-green-700";
      }
    }

    // ================= LABELS POPUP =================
    const labelsPopup = document.getElementById("labels-popup");
    const openLabelsBtn = document.getElementById("open-labels-btn");
    const closeLabelsBtn = document.getElementById("close-labels-btn");
    const searchLabelInput = document.getElementById("search-label-input");
    const labelsList = document.getElementById("labels-list");

    openLabelsBtn.addEventListener("click", e => openLabelsPopup(e));
    closeLabelsBtn.addEventListener("click", () => labelsPopup.classList.add("hidden"));


    async function loadLabels(keyword = "") {
      const projectId = PROJECT_ID;
      const taskId = window.CURRENT_TASK_ID;
      labelsList.innerHTML = `<p class="text-gray-400 text-sm italic">Loading...</p>`;

      try {
        // 🔹 Gửi token cho cả 2 request
        const headers = { "Authorization": "Bearer " + localStorage.getItem("token") };

        const resAll = await fetch(
          `/api/labels?projectId=${projectId}&keyword=${encodeURIComponent(keyword)}`,
          { headers }
        );
        const allLabels = await resAll.json();

        const resTask = await fetch(`/api/tasks/${taskId}/labels`, { headers });
        if (!resTask.ok) throw new Error("Failed to fetch task labels");

        const taskLabels = await resTask.json();
        const labelsArray = Array.isArray(taskLabels) ? taskLabels : [];
        const assignedIds = labelsArray.map(l => l.labelId);


        labelsList.innerHTML = allLabels.map(l => `
          <div class="flex items-center justify-between p-1 hover:bg-gray-50 rounded-md">
            <div class="flex items-center gap-2">
              <input type="checkbox" ${assignedIds.includes(l.labelId) ? 'checked' : ''}
                    onchange="${assignedIds.includes(l.labelId)
                      ? `unassignLabel(${taskId},${l.labelId})`
                      : `assignLabel(${taskId},${l.labelId})`}">
              <div class="w-16 h-5 rounded-md" style="background:${l.color || '#ccc'}"></div>
              <p class="text-sm text-gray-700">${l.name}</p>
            </div>
            <button class="text-gray-400 hover:text-blue-600 text-sm" onclick="editLabel(${l.labelId})">✎</button>
          </div>
        `).join('');
      } catch (err) {
        console.error("❌ loadLabels error:", err);
        labelsList.innerHTML = `<p class="text-red-500 text-sm">Failed to load labels</p>`;
      }
    }


    async function assignLabel(taskId, labelId) {
      await fetch(`/api/tasks/${taskId}/labels/${labelId}`, {
        method: "POST",
        headers: { "Authorization": "Bearer " + localStorage.getItem("token") }
      });
      await loadLabels(searchLabelInput.value.trim());
    }


    async function unassignLabel(taskId, labelId) {
      await fetch(`/api/tasks/${taskId}/labels/${labelId}`, {
        method: "DELETE",
        headers: { "Authorization": "Bearer " + localStorage.getItem("token") }
      });
      await loadLabels(searchLabelInput.value.trim());
    }


    let debounceLabelTimer;
    searchLabelInput.addEventListener('input', e => {
      clearTimeout(debounceLabelTimer);
      debounceLabelTimer = setTimeout(() => loadLabels(e.target.value.trim()), 300);
    });

    // ================= CREATE LABEL POPUP =================
    const createLabelPopup = document.getElementById("create-label-popup");
    const createLabelBtn = document.getElementById("create-label-btn");
    const closeCreateLabelBtn = document.getElementById("close-create-label-btn");
    const createLabelConfirm = document.getElementById("create-label-confirm");
    const colorGrid = document.getElementById("color-grid");
    const newLabelName = document.getElementById("new-label-name");

    createLabelBtn.addEventListener("click", (e) => {
      const rect = e.currentTarget.getBoundingClientRect();
      createLabelPopup.style.position = "absolute";
      createLabelPopup.style.top = `${rect.bottom + window.scrollY + 8}px`;
      createLabelPopup.style.left = `${rect.left + window.scrollX}px`;
      createLabelPopup.classList.remove("hidden");
      labelsPopup.classList.add("hidden");
      renderColorGrid();
      updateLabelPreview(); 
    });


    closeCreateLabelBtn.addEventListener("click", () => createLabelPopup.classList.add("hidden"));

    let selectedColor = null;
    newLabelName.addEventListener("input", updateLabelPreview);

    function renderColorGrid() {
      const colors = ["#16a34a","#ef4444","#f59e0b","#eab308","#06b6d4","#3b82f6","#8b5cf6","#ec4899",
                      "#f97316","#10b981","#4ade80","#a855f7","#6366f1","#14b8a6","#84cc16","#94a3b8"];
      colorGrid.innerHTML = colors.map(c => `
        <div class="w-6 h-6 rounded color-option cursor-pointer border border-gray-200"
            style="background:${c}"
            onclick="selectColor('${c}')"></div>
      `).join('');
    }

    function selectColor(c) {
      selectedColor = c;
      updateLabelPreview();
    }
    function updateLabelPreview() {
      const preview = document.getElementById("label-preview");
      const name = newLabelName.value.trim() || "New Label";
      preview.textContent = name;
      preview.style.background = selectedColor || "#d1d5db"; // xám nhẹ nếu chưa chọn màu
      preview.style.color = "#fff";
      preview.style.borderColor = selectedColor ? selectedColor : "#ccc";
    }

    createLabelConfirm.addEventListener("click", async () => {
      const name = newLabelName.value.trim();
      if (!name) return alert("Please enter label name");
      const body = new URLSearchParams({ projectId: PROJECT_ID, name, color: selectedColor || "#ccc" });
      try {
      const res = await fetch(`/api/labels?${body.toString()}`, {
        method: "POST",
        headers: { "Authorization": "Bearer " + localStorage.getItem("token") }
      });

        if (!res.ok) throw new Error();
        alert("✅ Label created!");
        createLabelPopup.classList.add("hidden");
        await loadLabels();
      } catch {
        alert("❌ Failed to create label");
      }
    });

    // ================= DATE POPUP =================
    const datePopup = document.getElementById('date-popup');
    const closeDateBtn = document.getElementById('close-date-btn');
    const saveDateBtn = document.getElementById('save-date-btn');
    const removeDateBtn = document.getElementById('remove-date-btn');
    const startCheck = document.getElementById('start-check');
    const dueCheck = document.getElementById('due-check');
    const startInput = document.getElementById('start-date-input');
    const dueInput = document.getElementById('due-date-input');


    closeDateBtn.addEventListener("click", () => datePopup.classList.add("hidden"));
    document.addEventListener("mousedown", (e) => {
      const insideDatePopup = datePopup.contains(e.target);
      const fromContextMenu = e.target.closest("#card-context-menu");

      if (!insideDatePopup && !fromContextMenu && e.button !== 2) {
        closeDatePopup();
      }
    });


    // enable/disable inputs when checkbox toggled
    startCheck.addEventListener("change", () => startInput.disabled = !startCheck.checked);
    dueCheck.addEventListener("change", () => dueInput.disabled = !dueCheck.checked);

    // remove date
    removeDateBtn.addEventListener("click", async () => {
      const taskId = window.CURRENT_TASK_ID;
      if (!taskId) return;
      try {
        await fetch(`/api/tasks/${taskId}/dates`, {
          method: "PUT",
          headers: {
            "Authorization": "Bearer " + localStorage.getItem("token"),
            "Content-Type": "application/json"
          },
          body: JSON.stringify({ startDate: null, deadline: null })
        });

        updateDateStatus(null);
        alert("Removed due date");
      } catch (err) {
        alert("Failed to remove");
      } finally {
        datePopup.classList.add("hidden");
      }
    });

    // save date
    // save date
    saveDateBtn.addEventListener("click", async () => {
      const taskId = window.CURRENT_TASK_ID;
      const start = startCheck.checked ? startInput.value : null;
      const due = dueCheck.checked ? dueInput.value : null;
      const recurring = document.getElementById("recurring-select").value;
      const reminder = document.getElementById("reminder-select").value;

      try {
        const res = await fetch(`/api/tasks/${taskId}/dates`, {
          method: "PUT",
          headers: {
      "Authorization": "Bearer " + localStorage.getItem("token"),
      "Content-Type": "application/json"
    },
          body: JSON.stringify({
            startDate: start,       // ✅ đúng key
            deadline: due,          // ✅ đúng key
            recurring: recurring,
            reminder: reminder
          })
        });

        if (!res.ok) throw new Error();
        const updated = await res.json();
        updateDateStatus(updated.deadline);
        datePopup.classList.add("hidden");
      } catch (err) {
        console.error("❌ Lỗi lưu date:", err);
        alert("Failed to save date");
      }
    });

      // ================== DRAG & DROP ==================
    function enableDragDrop() {
      const taskCards = document.querySelectorAll("[data-open-task]");
      const columns = document.querySelectorAll("[id^='col-']");

      taskCards.forEach(card => {
        card.setAttribute("draggable", "true");

        card.addEventListener("dragstart", e => {
          e.dataTransfer.setData("taskId", card.getAttribute("data-open-task"));
          card.classList.add("opacity-50");
        });

        card.addEventListener("dragend", e => {
          card.classList.remove("opacity-50");
        });
      });

    columns.forEach(col => {
      col.addEventListener("dragover", e => e.preventDefault());
      col.addEventListener("drop", e => handleDrop(e, col));

      // 🟦 Gắn drop zone cuối cùng
      const dropZone = col.querySelector(".drop-zone");
      if (dropZone) {
        dropZone.addEventListener("dragover", e => e.preventDefault());
        dropZone.addEventListener("drop", e => handleDrop(e, col));
      }
    });
    let isMoving = false;
    async function handleDrop(e, col) {
      e.preventDefault();
      if (isMoving) return;  // 🔒 ngăn gọi trùng
      isMoving = true;
      const taskId = e.dataTransfer.getData("taskId");
      const targetColId = parseInt(col.id.replace("col-", ""));
      const draggedCard = document.querySelector(`[data-open-task='${taskId}']`);

      col.appendChild(draggedCard);

      const cards = Array.from(col.querySelectorAll("[data-open-task]"));
      const newIndex = cards.indexOf(draggedCard);

      console.log("🧩 Sending:", { taskId, targetColumnId: targetColId, newOrderIndex: newIndex });

      try {
        const res = await fetch(`/api/tasks/${taskId}/move`, {
          method: "PUT",
          headers: {
            "Authorization": "Bearer " + localStorage.getItem("token"),
            "Content-Type": "application/json"
          },
          body: JSON.stringify({
            targetColumnId: Number(targetColId),
            newOrderIndex: Number(newIndex)
          })
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
      document.getElementById("send-comment").addEventListener("click", async () => {
        const content = document.getElementById("comment-input").value.trim();
        if (!content) return alert("Please enter a comment");
        await postComment(taskId, content);
        await loadActivityFeed(taskId);
      });

      try {
        const res = await fetch(`/api/tasks/${taskId}/activity`, {
          headers: {
            "Authorization": "Bearer " + localStorage.getItem("token")
          }
        });

        const data = await res.json();
        renderCommentAndActivity(taskId, data.comments, data.activityLogs);
      } catch (err) {
        console.error(err);
        container.querySelector("#activity-feed").innerHTML =
          `<p class="text-red-500">❌ Failed to load comments or activity</p>`;
      }
    }

    // ========== 💬 RENDER COMMENTS + ACTIVITY ==========
    function renderCommentAndActivity(taskId, comments, activities) {
      const feed = document.getElementById("activity-feed");
      if (!feed) return;

      feed.innerHTML = `
        <div id="comments-section" class="space-y-3">
          ${comments && comments.length ? renderComments(taskId, comments) : `<p class="text-gray-400 italic">No comments yet.</p>`}
        </div>
        <hr class="my-4 border-gray-300">
        <h4 class="font-semibold text-gray-700 mb-2">Activity</h4>
        <div id="activity-section-inner" class="space-y-2">
          ${activities && activities.length ? renderActivities(activities) : `<p class="text-gray-400 italic">No activity yet.</p>`}
        </div>
      `;
    }

  // ========== 💬 COMMENTS ==========
  function renderComments(taskId, comments) {
    const currentUserId = Number(localStorage.getItem("currentUserId"));
    const currentUserEmail = localStorage.getItem("currentUserEmail");
    const currentUserName = localStorage.getItem("currentUserName") || "Unknown";

    return comments.map(c => `
      <div class="border border-gray-200 rounded-md p-3 bg-white hover:bg-gray-50 transition">
        <div class="flex items-center gap-2 mb-1">
          <img src="${c.userAvatar || "https://i.pravatar.cc/30"}" class="w-6 h-6 rounded-full">
          <b class="text-sm">${c.userName}</b>
          <span class="text-xs text-gray-500">${formatTime(c.createdAt)}</span>
        </div>

        <p class="text-sm text-gray-800 ml-8 whitespace-pre-line">
          ${highlightMentions(escapeHtml(c.content), c.mentionsJson)}
        </p>

        <div class="ml-8 mt-1 flex gap-3 text-xs text-blue-500">
          ${
            Number(c.userId) === currentUserId || (c.userEmail && c.userEmail === currentUserEmail)
              ? `<button onclick="editComment(${taskId}, ${c.commentId})">Edit</button>
                <button onclick="deleteComment(${taskId}, ${c.commentId})">Delete</button>`
              : `<button onclick="toggleReplyBox(${c.commentId})">Reply</button>`
          }
        </div>

        <div id="reply-box-${c.commentId}" class="hidden ml-8 mt-2 space-y-1">
          <textarea id="reply-input-${c.commentId}" class="w-full border border-gray-300 rounded-md p-1 text-xs h-12 focus:ring-2 focus:ring-blue-400" placeholder="Write a reply..."></textarea>
          <button onclick="postReply(${taskId}, ${c.commentId})" class="bg-blue-600 hover:bg-blue-700 text-white px-2 py-1 rounded-md text-xs">Reply</button>
        </div>

        ${renderReplies(taskId, c.replies || [])}
      </div>
    `).join('');
  }

  // ========== 💬 RENDER REPLIES ==========
  function renderReplies(taskId, replies) {
    return replies.map(r => `
      <div class="ml-10 mt-2 border-l border-gray-300 pl-3">
        <div class="flex items-center gap-2 mb-1">
          <img src="${r.userAvatar || "https://i.pravatar.cc/25"}" class="w-5 h-5 rounded-full">
          <b class="text-sm">${r.userName}</b>
          <span class="text-xs text-gray-500">${formatTime(r.createdAt)}</span>
        </div>
        <p class="text-sm text-gray-700 ml-6">
          ${highlightMentions(escapeHtml(r.content), r.mentionsJson)}
        </p>
      </div>
    `).join('');
  }
  // 🔹 Escape regex ký tự đặc biệt
  function escapeRegex(str) {
    return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  }
  // 🎨 Highlight @mentions, @board/@card, hoặc email
  function highlightMentions(text, mentionsJson) {
    if (!text) return "";

    try {
      const mentions = mentionsJson ? JSON.parse(mentionsJson) : [];

      if (Array.isArray(mentions) && mentions.length > 0) {
        mentions.forEach(m => {
          const email = m.email || "";
          const name = m.name || "";
          const safeEmail = escapeRegex(email);
          const safeName = escapeRegex(name.trim()).replace(/\s+/g, "\\s+");
          const isSpecial = email === "@card" || email === "@board";

          // 🟣 Tag đặc biệt: @card / @board
          if (isSpecial) {
            const regex = new RegExp(`@?${escapeRegex(email.replace("@", ""))}(?=[\\s,.!?]|$)`, "gu");
            text = text.replace(
              regex,
              `<span class="mention-chip" data-type="special">@${email.replace("@", "")}</span>`
            );
            return;
          }

          // 🟢 Email thật hoặc mention @Tên
          const regexEmail = new RegExp(`${safeEmail}(?=[\\s,.!?]|$)`, "gu");
          const regexName = new RegExp(`@${safeName}(?=[\\s,.!?]|$)`, "gu");

          text = text
            .replace(regexEmail,
              `<span class="mention-chip" onclick="openMentionProfile('${email}')">${email}</span>`)
            .replace(regexName,
              `<span class="mention-chip" onclick="openMentionProfile('${email}')">@${name}</span>`);
        });

        return text;
      }
    } catch (err) {
      console.warn("⚠️ Mentions parse failed:", err);
    }

    // 🧩 Fallback: tự động highlight email và tag đặc biệt trong text thô
    return text
      // highlight email
      .replace(
        /\b([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,})\b/g,
        `<span class="mention-chip" onclick="openMentionProfile('$1')">$1</span>`
      )
      // highlight @card hoặc @board nếu người dùng gõ tay
      .replace(
        /@(?:card|board)\b/g,
        match => `<span class="mention-chip" data-type="special">${match}</span>`
      );
  }


  async function openMentionProfile(email) {
    try {
      const res = await fetch(`/api/users/by-email/${encodeURIComponent(email)}`, {
        headers: { "Authorization": "Bearer " + localStorage.getItem("token") }
      });
      if (!res.ok) throw new Error("Không tìm thấy người dùng");
      const data = await res.json();

      const popup = document.createElement("div");
      popup.className = "fixed inset-0 bg-black/40 flex items-center justify-center z-[9999]";
      popup.innerHTML = `
        <div class="bg-white rounded-lg p-5 shadow-lg w-80 relative animate-fadeIn">
          <button class="absolute top-2 right-3 text-gray-400 hover:text-gray-600 text-lg" onclick="this.closest('.fixed').remove()">×</button>
          <div class="flex flex-col items-center text-center">
            <img src="${data.avatarUrl || 'https://i.pravatar.cc/100?u=' + data.email}" class="w-20 h-20 rounded-full object-cover mb-3">
            <h3 class="text-lg font-semibold text-gray-800">${data.name}</h3>
            <p class="text-sm text-gray-500 mb-2">${data.email}</p>
            <p class="text-xs text-gray-400 italic">${data.provider ? `(${data.provider})` : ''}</p>
            <p class="mt-3 text-sm text-gray-600">${data.bio || 'No bio available.'}</p>
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
      const res = await fetch(`/api/pm/members/project/${PROJECT_ID}/mentions?keyword=${encodeURIComponent(keyword)}`);
      const data = await res.json();
  
      const allOptions = data.members || [];

      const suggestionBox = document.getElementById("mention-suggestions");
      if (!allOptions.length || !suggestionBox) return suggestionBox?.classList.add("hidden");

      suggestionBox.innerHTML = allOptions.map(m => `
        <div class="px-3 py-2 hover:bg-blue-50 cursor-pointer flex items-center gap-2"
            onclick="selectMention('${m.name}', '${m.email}', '${m.avatarUrl || ""}')">
          <img src="${m.avatarUrl || 'https://i.pravatar.cc/30?u=' + m.email}" class="w-6 h-6 rounded-full">
          <div>
            <b class="text-sm text-gray-800">${m.name}</b>
            <p class="text-xs text-gray-500">${m.email}</p>
          </div>
        </div>
      `).join("");

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
    const before = commentInput.value.slice(0, cursorPos).replace(/@[\wÀ-ỹ\s]*$/u, "");
    const after  = commentInput.value.slice(cursorPos);

    // Chèn tag thực tế
    commentInput.value = before + `${email} ` + after;

    document.getElementById("mention-suggestions")?.classList.add("hidden");

    const mentions = JSON.parse(localStorage.getItem("currentMentions") || "[]");
    if (!mentions.some(m => m.email === email)) {
      mentions.push({ name, email, avatarUrl });
      localStorage.setItem("currentMentions", JSON.stringify(mentions));
    }
  };



    // ========== 📜 ACTIVITY (Trello-style) ==========
    function renderActivities(activities) {
      return activities
        .filter(a => !a.action.startsWith("COMMENT_")) // loại bỏ log comment nội bộ
        .map(a => {
          let msg = "";
          let data = {};
          try {
            data = a.dataJson ? JSON.parse(a.dataJson) : {};
          } catch {
            data = {};
          }

          switch (a.action) {
            case "CREATE_TASK":
              msg = `created card <b>${escapeHtml(data.title || "Untitled")}</b> in <i>${escapeHtml(data.column || "")}</i>`;
              break;

            case "EDIT_TASK":
              msg = `edited card title to <b>${escapeHtml(data.title || "Untitled")}</b>`;
              break;

            case "MOVE_COLUMN":
              msg = `moved this card from <i>${escapeHtml(data.from || "Unknown")}</i> to <i>${escapeHtml(data.to || "Unknown")}</i>`;
              break;

            case "ATTACH_LINK":
              msg = `attached link <a href="${escapeHtml(data.link || data.url || "#")}" target="_blank">${escapeHtml(data.name || data.link || "link")}</a>`;
              break;

            case "ATTACH_FILE":
              msg = `uploaded file <b>${escapeHtml(data.fileName || "a file")}</b>`;
              break;

            case "DELETE_ATTACHMENT":
              msg = `deleted attachment <b>${escapeHtml(data.fileName || data.name || "unknown")}</b>`;
              break;

            case "ASSIGN_TASK":
              msg = `assigned to user ID <code>${escapeHtml(data.assigneeId || "?")}</code>`;
              break;

            case "UPDATE_DATES":
              msg = `updated dates: start <i>${escapeHtml(data.start || "N/A")}</i> → deadline <i>${escapeHtml(data.deadline || "N/A")}</i>`;
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
              <span class="text-gray-400 ml-1">${formatTime(a.createdAt)}</span>
            </div>
          `;
        })
        .join('');
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
            "Authorization": "Bearer " + localStorage.getItem("token"),
            "Content-Type": "application/json"
          },
          body: JSON.stringify({ content })
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
          "Authorization": "Bearer " + localStorage.getItem("token"),
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ content })
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
          headers: { "Authorization": "Bearer " + localStorage.getItem("token") }
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
      const commentEl = document.querySelector(`button[onclick="editComment(${taskId}, ${commentId})"]`).closest("div.border");
      const oldText = commentEl.querySelector("p").innerText;
      commentEl.innerHTML = `
        <textarea id="edit-input-${commentId}" class="w-full border border-gray-300 rounded-md p-2 text-sm h-16">${oldText}</textarea>
        <div class="flex justify-end mt-1 gap-2">
          <button class="bg-gray-300 px-3 py-1 rounded text-xs" onclick="loadActivityFeed(${taskId})">Cancel</button>
          <button class="bg-blue-600 text-white px-3 py-1 rounded text-xs" onclick="saveEdit(${taskId}, ${commentId})">Save</button>
        </div>
      `;
    }


    // ========== ⏰ UTILS ==========
    function formatTime(dateStr) {
      const d = new Date(dateStr);
      return d.toLocaleString("vi-VN", { hour: "2-digit", minute: "2-digit", day: "2-digit", month: "2-digit" });
    }

    function escapeHtml(str) {
      return str.replace(/[&<>"']/g, m => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
      }[m]));
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
      headers: { "Authorization": "Bearer " + localStorage.getItem("token") }
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
        const res = await fetch(`/api/pm/invite/project/${PROJECT_ID}/share/enable`, {
          method: "POST",
          headers: { "Authorization": "Bearer " + localStorage.getItem("token") }
        });
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
    const res = await fetch(`/api/pm/invite/project/${PROJECT_ID}/share/disable`, {
      method: "DELETE",
      headers: { "Authorization": "Bearer " + localStorage.getItem("token") }
    });
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
  if (!deleteConfirmPopup.contains(e.target) && !e.target.closest("#delete-link")) {
    deleteConfirmPopup.classList.add("hidden");
  }
});

    async function loadBoardMembers(projectId) {
      const membersList = document.getElementById("members-list");
      membersList.innerHTML = `<p class="text-gray-400 text-sm italic">Loading...</p>`;

      try {
        const res = await fetch(`/api/pm/invite/project/${projectId}`, {
          headers: { "Authorization": "Bearer " + localStorage.getItem("token") }
        });

        if (!res.ok) throw new Error(`Cannot load members: ${res.status}`);

        const data = await res.json();
        // ✅ Backend trả về mảng members (List<MemberDTO>)
        const members = data.members || [];

        if (!Array.isArray(members) || members.length === 0) {
          membersList.innerHTML = `<p class="text-gray-500 text-sm italic">No members found.</p>`;
          return;
        }

        membersList.innerHTML = members.map(m => `
          <div class="flex justify-between items-center p-2 hover:bg-gray-50 rounded-md">
            <div class="flex items-center gap-2">
              ${renderAvatar(m)}
              <div>
                <p class="text-sm font-medium text-gray-800">${m.name || "Unnamed"}</p>
                <p class="text-xs text-gray-500">${m.email || ""}</p>
              </div>
            </div>

            <select onchange="updateMemberRole(${projectId}, ${m.userId}, this.value)"
                    class="text-xs border border-gray-300 rounded-md px-2 py-1 bg-white focus:ring-2 focus:ring-blue-400">
              ${renderRoleOptions(m.roleInProject)}
            </select>
          </div>
        `).join('');
      } catch (err) {
        console.error("❌ loadBoardMembers error:", err);
        membersList.innerHTML = `<p class="text-red-500 text-sm">Failed to load members</p>`;
      }
    }

    // ✅ Avatar fallback logic
    function renderAvatar(m) {
      const url = m.avatarUrl && m.avatarUrl.trim() !== ""
        ? m.avatarUrl
        : `https://i.pravatar.cc/100?u=${m.userId}`;

      return `<img src="${url}" alt="${m.name}"
                  onerror="this.src='https://i.pravatar.cc/100?u=${m.userId}'"
                  class="w-8 h-8 rounded-full border object-cover">`;
    }

  function renderRoleOptions(currentRole) {
    const roles = ["PM", "MEMBER"]; // 🔹 chỉ còn 2 vai trò chính
    const role = (currentRole || "").toUpperCase();

    return roles.map(r =>
      `<option value="${r}" ${r === role ? "selected" : ""}>
        ${r === "PM" ? "Project Manager" : "Member"}
      </option>`
    ).join("");
  }




    async function updateMemberRole(projectId, userId, newRole) {
      try {
        const selectEl = event?.target;
        if (selectEl) selectEl.disabled = true; // ⏳ disable khi đang cập nhật

        const res = await fetch(`/api/pm/invite/project/${projectId}/member/${userId}/role?role=${newRole}`, {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
            "Authorization": "Bearer " + localStorage.getItem("token")
          }
        });

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
        const resInvite = await fetch(`/api/pm/invite?projectId=${PROJECT_ID}&email=${encodeURIComponent(email)}&role=${role}`, {
          method: "POST",
          headers: {
            "Authorization": "Bearer " + localStorage.getItem("token")
          }
        });
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
        alert("❌ Cannot initialize dashboard: " + (err.message || "Unknown error"));
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
            "Authorization": "Bearer " + localStorage.getItem("token"),
            "Content-Type": "application/json"
          }
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
inviteInput.addEventListener("input", e => {
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
      ? { "Authorization": "Bearer " + token, "Content-Type": "application/json" }
      : { "Content-Type": "application/json" };

    // 🔹 Nếu không có token, vẫn cho phép cookie xác thực (OAuth2 fallback)
    const useCredentials = !token;

    const res = await fetch(`/api/pm/invite/search-users?keyword=${encodeURIComponent(keyword)}`, {
      method: "GET",
      headers,
      ...(useCredentials ? { credentials: "include" } : {}) // chỉ thêm khi cần
    });

    if (!res.ok) throw new Error(`Request failed: ${res.status}`);

    const users = await res.json();

    if (!Array.isArray(users) || users.length === 0) {
      suggestionBox.innerHTML = `<p class="p-2 text-sm text-gray-400 italic">No results found</p>`;
      suggestionBox.classList.remove("hidden");
      return;
    }

    // ✅ Render danh sách user gợi ý
    suggestionBox.innerHTML = users.map(u => `
      <div class="flex items-center gap-2 px-3 py-2 hover:bg-blue-50 cursor-pointer"
           onclick="selectInvite('${u.email}')">
        <img src="${u.avatarUrl || 'https://i.pravatar.cc/40?u=' + u.email}" 
             class="w-6 h-6 rounded-full">
        <div>
          <p class="text-sm font-medium text-gray-700">${u.name || "(No name)"}</p>
          <p class="text-xs text-gray-500">${u.email}</p>
        </div>
      </div>
    `).join("");
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



window.selectInvite = function(email) {
  inviteInput.value = email;
  suggestionBox.classList.add("hidden");
};


  // ================= EDIT LABEL POPUP =================
  let CURRENT_EDIT_LABEL_ID = null;
  let selectedEditColor = "#94a3b8";

  function closeEditLabelPopup() {
    document.getElementById("edit-label-popup-wrapper").classList.add("hidden");
  }

  async function editLabel(labelId) {
  CURRENT_EDIT_LABEL_ID = labelId;
  try {
    const res = await fetch(`/api/labels/${labelId}`, {
      headers: { "Authorization": "Bearer " + localStorage.getItem("token") }
    });
    if (!res.ok) throw new Error("Failed to fetch label");
    const label = await res.json();

    document.getElementById("edit-label-name").value = label.name;
    selectedEditColor = label.color || "#94a3b8";
    updateEditLabelPreview();
    renderEditColorGrid();

    document.getElementById("labels-popup").classList.add("hidden");
    document.getElementById("edit-label-popup-wrapper").classList.remove("hidden");
  } catch (err) {
    console.error("❌ editLabel error:", err);
    alert("Failed to open edit popup");
  }
}



  function renderEditColorGrid() {
    const grid = document.getElementById("edit-color-grid");
    const colors = ["#16a34a","#ef4444","#f59e0b","#eab308","#06b6d4","#3b82f6","#8b5cf6","#ec4899",
                    "#f97316","#10b981","#4ade80","#a855f7","#6366f1","#14b8a6","#84cc16","#94a3b8"];

    grid.innerHTML = colors.map(c => `
      <div class="w-6 h-6 rounded-md cursor-pointer border border-gray-200 hover:scale-105 transition-all"
          style="background:${c}"
          onclick="selectEditColor('${c}')">
      </div>
    `).join('');
  }

  function selectEditColor(c) {
    selectedEditColor = c;
    updateEditLabelPreview();
  }

  function removeEditColor() {
    selectedEditColor = "#94a3b8";
    updateEditLabelPreview();
  }

  function updateEditLabelPreview() {
    const preview = document.getElementById("edit-label-preview");
    const name = document.getElementById("edit-label-name").value.trim() || "New";
    preview.textContent = name;
    preview.style.background = selectedEditColor;
    preview.style.color = "#fff";
  }

  document.getElementById("edit-label-name")?.addEventListener("input", updateEditLabelPreview);

  async function saveEdit(taskId, commentId) {
    const newText = document.getElementById(`edit-input-${commentId}`).value.trim();
    if (!newText) return alert("Content cannot be empty");

    try {
      const res = await fetch(`/api/tasks/${taskId}/comments/${commentId}`, {
        method: "PUT",
        headers: {
          "Authorization": "Bearer " + localStorage.getItem("token"),
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ content: newText })
      });


      if (!res.ok) throw new Error("Update failed");
      await loadActivityFeed(taskId);
    } catch (err) {
      console.error(err);
      alert("❌ Failed to update comment");
    }
  }


  async function deleteEditedLabel() {
    const choice = confirm("🗑️ Delete this label from ALL tasks (not just this one)?");
    const token = localStorage.getItem("token");
    try {
      let res;
      if (choice) {
        // ✅ Xóa hoàn toàn label khỏi hệ thống
        res = await fetch(`/api/labels/${CURRENT_EDIT_LABEL_ID}`, {
          method: "DELETE",
          headers: { "Authorization": "Bearer " + token },
        });
      } else {
        // ✅ Chỉ gỡ label khỏi task hiện tại
        res = await fetch(`/api/tasks/${window.CURRENT_TASK_ID}/labels/${CURRENT_EDIT_LABEL_ID}`, {
          method: "DELETE",
          headers: { "Authorization": "Bearer " + token },
        });
      }

      if (!res.ok) throw new Error("Delete failed");
      alert("✅ Label deleted successfully!");
      closeEditLabelPopup();
      await loadLabels();
    } catch (err) {
      console.error(err);
      alert("❌ Failed to delete label");
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
    if (window.CURRENT_ROLE === "ROLE_PM")
      deleteBtn.classList.remove("hidden");
    else
      deleteBtn.classList.add("hidden");
  }


  // --- Định vị thông minh ---
  contextMenu.classList.remove("hidden");
  const menuW = contextMenu.offsetWidth || 200;
  const menuH = contextMenu.offsetHeight || 250;
  contextMenu.classList.add("hidden");

  const screenW = window.innerWidth;
  const screenH = window.innerHeight;
  let top = e.clientY, left = e.clientX;
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
    preventDefault: () => {}
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

        if (!confirm("⚠️ Permanently delete this task? This cannot be undone!")) return;
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
      "Authorization": "Bearer " + localStorage.getItem("token"),
    }
  });
  if (!res.ok) throw new Error("Archive failed");
}
  async function markTaskComplete(taskId) {
  const res = await fetch(`/api/tasks/${taskId}/complete`, {
    method: "PUT",
    headers: {
      "Authorization": "Bearer " + localStorage.getItem("token")
    }
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
      badge.className = "due-date-badge bg-gray-200 text-gray-600 text-xs px-2 py-0.5 rounded-md";
    }
  }

  return updated;
}

async function deleteTask(taskId) {
  const res = await fetch(`/api/tasks/${taskId}`, {
    method: "DELETE",
    headers: {
      "Authorization": "Bearer " + localStorage.getItem("token"),
    }
  });
  if (!res.ok) throw new Error("Delete failed");
}

// ==================== CẬP NHẬT CÁC HÀM POPUP ĐỂ HỖ TRỢ CONTEXT MENU ====================

/**
 * ✅ FIXED: openMemberPopup - hỗ trợ cả click thường và context menu
 */
function openMemberPopup(e) {
  console.log("🔍 openMemberPopup called");
  console.log("Event:", e);
  console.log("contextMenuX:", window.contextMenuX);
  console.log("contextMenuY:", window.contextMenuY);
  
  safeStop(e);
  
  let rect = null;
  if (e && e.currentTarget && typeof e.currentTarget.getBoundingClientRect === "function") {
    rect = e.currentTarget.getBoundingClientRect();
    console.log("Rect:", rect);
  } else {
    console.log("⚠️ No valid rect");
  }

  // ⚡ Fix: kiểm tra rect có hợp lệ không
  const isValidRect = rect && rect.top > 0 && rect.left > 0 && rect.width > 0 && rect.height > 0;
  console.log("isValidRect:", isValidRect);

  if (!isValidRect) {
    // Gọi từ context menu → dùng tọa độ chuột
    const top = (window.contextMenuY || 100) + 10;
    const left = (window.contextMenuX || 100) + 10;
    console.log(`Setting position from context menu: top=${top}, left=${left}`);
    
    membersPopup.style.top = `${top}px`;
    membersPopup.style.left = `${left}px`;
  } else {
    // Gọi từ modal button → dùng vị trí button
    const top = rect.bottom + window.scrollY + 6;
    const left = rect.left + window.scrollX;
    console.log(`Setting position from button: top=${top}, left=${left}`);
    
    membersPopup.style.top = `${top}px`;
    membersPopup.style.left = `${left}px`;
  }

  console.log("Final popup position:", {
    top: membersPopup.style.top,
    left: membersPopup.style.left
  });

  membersPopup.classList.remove("hidden");
  console.log("✅ Members popup displayed");
  loadMembers();
}
/**
 * ✅ FIXED: openDatePopup - hỗ trợ cả click thường và context menu
 */
function openDatePopup(e) {
  console.log("🔍 openDatePopup called");
  console.log("Event:", e);
  console.log("contextMenuX:", window.contextMenuX);
  console.log("contextMenuY:", window.contextMenuY);
  
  safeStop(e);

  let rect = null;
  if (e && e.currentTarget && typeof e.currentTarget.getBoundingClientRect === "function") {
    rect = e.currentTarget.getBoundingClientRect();
    console.log("Rect:", rect);
  } else {
    console.log("⚠️ No valid rect");
  }

  // ⚡ Fix: kiểm tra rect có hợp lệ không
  const isValidRect = rect && rect.top > 0 && rect.left > 0 && rect.width > 0 && rect.height > 0;
  console.log("isValidRect:", isValidRect);

  if (!isValidRect) {
    // Gọi từ context menu → dùng tọa độ chuột
    const top = (window.contextMenuY || 100) + 10;
    const left = (window.contextMenuX || 100) + 10;
    console.log(`Setting position from context menu: top=${top}, left=${left}`);
    
    datePopup.style.top = `${top}px`;
    datePopup.style.left = `${left}px`;
  } else {
    // Gọi từ modal button → dùng vị trí button
    const top = rect.bottom + window.scrollY + 6;
    const left = rect.left + window.scrollX;
    console.log(`Setting position from button: top=${top}, left=${left}`);
    
    datePopup.style.top = `${top}px`;
    datePopup.style.left = `${left}px`;
  }

  console.log("Final popup position:", {
    top: datePopup.style.top,
    left: datePopup.style.left
  });

  datePopup.classList.remove("hidden");
  console.log("✅ Date popup displayed");
}

/**
 * ✅ FIXED: openLabelsPopup - hỗ trợ cả click thường và context menu
 */
function openLabelsPopup(e) {
  safeStop(e);
  
  let rect = null;
  if (e && e.currentTarget && typeof e.currentTarget.getBoundingClientRect === "function") {
    rect = e.currentTarget.getBoundingClientRect();
  }

  // Nếu gọi từ context menu hoặc rect không hợp lệ
  if (!rect || (rect.width === 0 && rect.height === 0)) {
    labelsPopup.style.top = `${(window.contextMenuY || e.clientY) + 10}px`;
    labelsPopup.style.left = `${(window.contextMenuX || e.clientX) + 10}px`;
  } else {
    labelsPopup.style.top = `${rect.bottom + window.scrollY + 6}px`;
    labelsPopup.style.left = `${rect.left + window.scrollX}px`;
  }

  labelsPopup.classList.remove("hidden");
  loadLabels();
}

console.log("✅ Context Menu Fixed - Members, Labels & Dates hoạt động từ chuột phải!");
  // ================== ATTACHMENTS (SECURE) ==================
  const uploadBtn = document.getElementById("upload-attachment-btn");
  const fileInput = document.getElementById("attachment-file");
  const attachmentsList = document.getElementById("attachments-list");

  // 🟦 Mở file picker
  uploadBtn.addEventListener("click", () => fileInput.click());

  fileInput.addEventListener("change", async () => {
    const file = fileInput.files[0];
    if (!file || !window.CURRENT_TASK_ID) return;

    const formData = new FormData();
    formData.append("file", file);

    try {
      const res = await fetch(`/api/tasks/${window.CURRENT_TASK_ID}/attachments`, {
        method: "POST",
        headers: {
          "Authorization": "Bearer " + localStorage.getItem("token")
        },
        body: formData,
      });
      if (!res.ok) throw new Error("Upload failed");
      await loadAttachments(window.CURRENT_TASK_ID);
    } catch (err) {
      console.error("❌ Upload error:", err);
      alert("❌ Failed to upload attachment");
    } finally {
      fileInput.value = "";
    }
  });

  async function loadAttachments(taskId) {
    try {
      const res = await fetch(`/api/tasks/${taskId}/attachments`, {
          headers: {
            "Authorization": "Bearer " + localStorage.getItem("token")
          }
        });

      if (!res.ok) throw new Error("Load attachments failed");
      const attachments = await res.json();
      renderAttachments(attachments);
    } catch (err) {
      console.error("❌ loadAttachments error:", err);
      attachmentsList.innerHTML = `<p class="text-red-500 text-sm">Failed to load attachments</p>`;
    }
  }

  function renderAttachments(items) {
    if (!items || !items.length) {
      attachmentsList.innerHTML = `<p class="text-gray-400 italic">No attachments yet.</p>`;
      return;
    }

    attachmentsList.innerHTML = items.map(a => {
      // ✅ Mọi file đều là API secure: /api/tasks/{id}/attachments/download/{file}
      const fileUrl = a.fileUrl?.startsWith("/") && !a.fileUrl.startsWith("/http")
      ? `${window.location.origin}${a.fileUrl}`
      : a.fileUrl;

      const isImage = a.mimeType?.startsWith("image/");
      const isLink = a.mimeType === "link/url" || /^https?:\/\//.test(a.fileUrl);

      const preview = isImage
        ? `<img src="${fileUrl}" alt="${a.fileName}" class="w-20 h-20 object-cover rounded-md border cursor-pointer">`
        : `<div class="w-20 h-20 flex items-center justify-center bg-gray-100 border rounded-md text-gray-500 cursor-pointer">📄</div>`;

      const uploader = a.uploadedBy?.name || "Unknown";
      const uploadedAt = new Date(a.uploadedAt).toLocaleString();

      const dataAttr = encodeURIComponent(JSON.stringify({
        ...a,
        fileUrl,
        isLink
      }));

      return `
        <div class="attachment-row flex items-center gap-3 border border-gray-200 rounded-md p-2 hover:bg-gray-50 transition cursor-pointer"
            data-attachment='${dataAttr}'>
          ${preview}
          <div class="flex-1 truncate">
            <p class="file-name font-medium text-blue-600 hover:underline">${isLink ? "🌐 " : ""}${a.fileName}</p>
            <p class="text-xs text-gray-500">${uploader} • ${uploadedAt}</p>
          </div>
          <button class="delete-attach text-red-500 hover:text-red-700 text-sm" data-id="${a.attachmentId}">🗑️</button>
        </div>
      `;
    }).join('');

    // 🟦 Click preview
    document.querySelectorAll(".attachment-row").forEach(row => {
      row.addEventListener("click", e => {
        if (e.target.closest(".delete-attach")) return;
        const data = JSON.parse(decodeURIComponent(row.dataset.attachment));

        if (data.isLink) {
          window.open(data.fileUrl, "_blank");
        } else {
          openPreviewModal(data);
        }
      });
    });

    // 🗑️ Xóa file
    document.querySelectorAll(".delete-attach").forEach(btn => {
      btn.addEventListener("click", e => {
        e.stopPropagation();
        deleteAttachment(e.target.dataset.id);
      });
    });
  }

  async function deleteAttachment(id) {
    if (!confirm("🗑️ Delete this attachment?")) return;

    try {
      const res = await fetch(`/api/tasks/${window.CURRENT_TASK_ID}/attachments/${id}`, {
        method: "DELETE",
        headers: { "Authorization": "Bearer " + localStorage.getItem("token") }
      });
      if (!res.ok) throw new Error("Delete failed");
      await loadAttachments(window.CURRENT_TASK_ID);
    } catch (err) {
      console.error("❌ Delete error:", err);
      alert("❌ Failed to delete attachment");
    }
  }


  // ================== ATTACHMENT PREVIEW MODAL ==================
  const previewModal = document.getElementById("attachment-preview-modal");
  const previewContent = document.getElementById("preview-content");
  const previewFileName = document.getElementById("preview-file-name");
  const closePreviewBtn = document.getElementById("close-preview-btn");
  const downloadFileBtn = document.getElementById("download-file-btn");

  async function openPreviewModal(attachment) {
    const isLink = attachment.isLink === true || attachment.isLink === "true" ||
                  (!attachment.mimeType && /^https?:\/\//.test(attachment.fileUrl));

    if (isLink) {
      window.open(attachment.fileUrl, "_blank");
      return;
    }

    previewModal.classList.remove("hidden");
    previewModal.style.opacity = "1";
    previewModal.style.pointerEvents = "auto";

    previewFileName.textContent = attachment.fileName || "Untitled";
    downloadFileBtn.href = attachment.fileUrl;
    previewContent.innerHTML = `<p class="text-gray-500 italic">Loading preview...</p>`;

    const mime = (attachment.mimeType || "").toLowerCase();
    let url = attachment.fileUrl;
    if (url && !url.startsWith("http")) {
    const parts = url.split("/download/");
    if (parts.length === 2) {
      // ✅ decode trước, rồi encode lại chính xác 1 lần
      const encodedName = encodeURIComponent(decodeURIComponent(parts[1]));
      url = parts[0] + "/download/" + encodedName;
    }
  }



    try {
      const headers = { "Authorization": "Bearer " + localStorage.getItem("token") };

      if (mime.startsWith("image/")) {
        const blob = await fetch(url, { headers }).then(r => r.blob());
        const blobUrl = URL.createObjectURL(blob);
        previewContent.innerHTML = `
          <div class="relative flex flex-col items-center justify-center">
            <img src="${blobUrl}" class="max-h-[80vh] object-contain rounded-md shadow" />
            ${buildFooter(attachment)}
          </div>`;
      } else if (mime === "application/pdf") {
        const blob = await fetch(url, { headers }).then(r => r.blob());
        const blobUrl = URL.createObjectURL(blob);
        previewContent.innerHTML = `
          <div class="relative">
            <iframe src="${blobUrl}" class="w-full h-[80vh]" frameborder="0"></iframe>
            ${buildFooter(attachment)}
          </div>`;
      } else if (mime.startsWith("text/") || url.endsWith(".txt") || url.endsWith(".log")) {
        const text = await fetch(url, { headers }).then(r => r.text());
        previewContent.innerHTML = `
          <div class="relative">
            <pre class="bg-gray-50 text-gray-800 p-4 rounded-md max-h-[70vh] overflow-auto whitespace-pre-wrap font-mono text-sm leading-relaxed">
      ${escapeHtml(text)}
            </pre>
            ${buildFooter(attachment)}
          </div>`;
      }
      else if (
        mime.includes("officedocument") ||
        url.endsWith(".docx") || url.endsWith(".pptx") || url.endsWith(".xlsx")
      ) {
        const gview = `https://docs.google.com/gview?embedded=true&url=${encodeURIComponent(url)}`;
        previewContent.innerHTML = `
          <div class="relative">
            <iframe src="${gview}" class="w-full h-[80vh]" frameborder="0"></iframe>
            ${buildFooter(attachment)}
          </div>`;
      } else {
        showNoPreview(attachment);
      }

      attachFooterEvents(attachment);
    } catch (err) {
      console.error("❌ Preview failed:", err);
      showNoPreview(attachment);
    }
  }




  /**
   * Hiển thị fallback "No preview available"
   */
  function showNoPreview(attachment = {}) {
    previewContent.innerHTML = `
      <div class="relative w-full h-full flex flex-col items-center justify-center text-center bg-gray-50">
        <p class="text-gray-600 mb-3">There is no preview available for this attachment.</p>
        <button id="download-direct-btn"
                class="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-md text-sm mb-10">
          Download
        </button>
        ${buildFooter(attachment)}
      </div>`;

    document.getElementById("download-direct-btn").onclick = () =>
      window.open(attachment.fileUrl, "_blank");

    attachFooterEvents(attachment);
  }

  /**
   * Footer kiểu Trello
   */
  function buildFooter(attachment = {}) {
    const fileName = attachment.fileName || "Untitled";
    const uploadedAt = attachment.uploadedAt
      ? new Date(attachment.uploadedAt).toLocaleString("en-US", {
          month: "short",
          day: "numeric",
          year: "numeric",
          hour: "numeric",
          minute: "2-digit",
        })
      : "Unknown date";
    const fileSizeKB = attachment.size
      ? `${(attachment.size / 1024).toFixed(1)} KB`
      : "—";

    return `
      <div class="absolute bottom-0 left-0 right-0 bg-black/70 text-gray-200 px-4 py-3 text-sm flex flex-col items-center">
        <p class="font-semibold text-white mb-1">${fileName}</p>
        <p class="text-xs text-gray-300 mb-2">Added ${uploadedAt} • ${fileSizeKB}</p>
        <div class="flex gap-4">
          <button class="hover:text-blue-400 flex items-center gap-1" id="open-tab-btn">🔗 Open in new tab</button>
          <button class="hover:text-blue-400 flex items-center gap-1" id="download-btn">⬇️ Download</button>
          <button class="hover:text-red-400 flex items-center gap-1" id="delete-btn">🗑️ Delete</button>
        </div>
      </div>`;
  }

  /**
   * Gắn sự kiện cho footer
   */
  function attachFooterEvents(attachment = {}) {
    const fileUrl = attachment.fileUrl || "#";
    const fileName = attachment.fileName || "Untitled";

    const openTabBtn = document.getElementById("open-tab-btn");
    const downloadBtn = document.getElementById("download-btn");
    const deleteBtn = document.getElementById("delete-btn");

    openTabBtn?.addEventListener("click", () => window.open(fileUrl, "_blank"));
    downloadBtn?.addEventListener("click", async () => {
      try {
        const token = localStorage.getItem("token");
        const res = await fetch(fileUrl, {
          headers: { "Authorization": "Bearer " + token }
        });

        if (!res.ok) throw new Error("Download failed");

        const blob = await res.blob();
        const blobUrl = window.URL.createObjectURL(blob);

        const a = document.createElement("a");
        a.href = blobUrl;
        a.download = fileName;
        document.body.appendChild(a);
        a.click();

        setTimeout(() => {
          document.body.removeChild(a);
          window.URL.revokeObjectURL(blobUrl);
        }, 500);
      } catch (err) {
        console.error("❌ Download error:", err);
        alert("Không thể tải file này!");
      }
    });



    deleteBtn?.addEventListener("click", async () => {
      if (window.CURRENT_ROLE !== "ROLE_PM") {
        alert("❌ Only Project Managers can delete attachments!");
        return;
      }

      const confirmDel = confirm(`🗑️ Delete ${fileName}?`);
      if (!confirmDel) return;

      try {
        await deleteAttachment(attachment.attachmentId);
        previewModal.classList.add("hidden");
      } catch (err) {
        alert("❌ Failed to delete attachment");
        console.error(err);
      }
    });

  }

  closePreviewBtn.addEventListener("click", () => {
    previewModal.classList.add("hidden");
    previewModal.style.opacity = "0";
    previewModal.style.pointerEvents = "none"; // ✅ tránh che click
    previewContent.innerHTML = "";
  });


  previewModal.addEventListener("click", (e) => {
    if (e.target === previewModal) previewModal.classList.add("hidden");
  });

  // ================== ATTACH POPUP ==================
  const attachPopup = document.getElementById("attach-popup");
  const openAttachBtn = document.getElementById("open-attach-popup");
  const closeAttachBtn = document.getElementById("close-attach-popup");
  const cancelAttachBtn = document.getElementById("cancel-attach-btn");
  const insertAttachBtn = document.getElementById("insert-attach-btn");
  const chooseFileBtn = document.getElementById("choose-file-btn");
  const popupFileInput = document.getElementById("popup-attachment-file");
  const linkInput = document.getElementById("link-input");
  const displayTextInput = document.getElementById("display-text");

  openAttachBtn.addEventListener("click", async () => {
    attachPopup.classList.remove("hidden");
    await loadRecentLinks(); // 🔹 gọi API khi popup mở
  });

  async function loadRecentLinks() {
    const container = document.getElementById("recent-links");
    container.innerHTML = `<p class="text-gray-400 italic text-sm">Loading recent links...</p>`;

    try {
      // ✅ endpoint mới (không còn phụ thuộc taskId)
      const res = await fetch(`/api/attachments/recent-links`, {
        headers: {
          "Authorization": "Bearer " + localStorage.getItem("token"),
        }
      });

      if (!res.ok) throw new Error("Failed to fetch recent links");
      const links = await res.json();

      if (!Array.isArray(links) || links.length === 0) {
        container.innerHTML = `<p class="text-gray-400 italic text-sm">No recent links</p>`;
        return;
      }
  // ================== DRAG & DROP ==================
      // ✅ render
      container.innerHTML = "";
      links.slice(0, 5).forEach(link => {
        const p = document.createElement("p");
        p.textContent = `📄 ${link.fileName || link.fileUrl}`;
        p.className = "hover:underline cursor-pointer";
        p.addEventListener("click", () => {
          document.getElementById("link-input").value = link.fileUrl;
          document.getElementById("display-text").value = link.fileName || "";
        });
        container.appendChild(p);
      });
    } catch (err) {
      console.error("❌ loadRecentLinks error:", err);
      container.innerHTML = `<p class="text-red-500 text-sm">Failed to load recent links</p>`;
    }
  }


  closeAttachBtn.addEventListener("click", () => {
    attachPopup.classList.add("hidden");
  });

  cancelAttachBtn.addEventListener("click", () => {
    attachPopup.classList.add("hidden");
  });

  // 🟦 Chọn file
  chooseFileBtn.addEventListener("click", () => popupFileInput.click());

  // 🧩 Upload hoặc insert link
  insertAttachBtn.addEventListener("click", async () => {
    const file = popupFileInput.files[0];
    const link = linkInput.value.trim();
    const displayText = displayTextInput.value.trim();

    if (!file && !link) {
      alert("⚠️ Please select a file or enter a link!");
      return;
    }

    insertAttachBtn.disabled = true;

    try {
      if (file) {
        // 🟩 Upload file
        const formData = new FormData();
        formData.append("file", file);

        const res = await fetch(`/api/tasks/${window.CURRENT_TASK_ID}/attachments`, {
          method: "POST",
          headers: {
            "Authorization": "Bearer " + localStorage.getItem("token")
          },
          body: formData,
        });


        if (!res.ok) throw new Error("Upload failed");
      } else {
        // 🟦 Upload link
        const res = await fetch(`/api/tasks/${window.CURRENT_TASK_ID}/attachments/link`, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Authorization": "Bearer " + localStorage.getItem("token") // 🔥 thêm dòng này
          },
          body: JSON.stringify({
            fileUrl: link,
            fileName: displayText || link,
          }),
        });

        if (!res.ok) throw new Error("Attach link failed");
      }

      alert("✅ Uploaded successfully!");
      attachPopup.classList.add("hidden");

      // 🔄 Refresh list
      await loadAttachments(window.CURRENT_TASK_ID);
    } catch (err) {
      console.error("❌ Upload error:", err);
      alert("❌ Failed to upload or attach link.");
    } finally {
      // 🧹 Reset input sau mỗi lần upload
      popupFileInput.value = "";
      linkInput.value = "";
      displayTextInput.value = "";
      insertAttachBtn.disabled = false;
    }
  });


  document.getElementById("edit-label-popup-wrapper").addEventListener("click", e => {
    if (e.target.id === "edit-label-popup-wrapper") {
      closeEditLabelPopup();
    }
  });
  async function ensureCurrentUser() {
    try {
      const res = await fetch("/api/auth/me", {
        headers: { "Authorization": "Bearer " + localStorage.getItem("token") }
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

  /**
 * 🎯 Lấy vai trò thực tế của user trong project hiện tại
 */
async function fetchProjectRole(projectId) {
  try {
    const res = await fetch(`/api/projects/${projectId}/role`, {
      headers: { "Authorization": "Bearer " + localStorage.getItem("token") }
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

  function closeDatePopup() {
  const popup = document.getElementById("date-popup");
  if (popup) popup.classList.add("hidden");
  console.log("✅ Date popup closed");
}


// ================== 🔔 NOTIFICATIONS PANEL ==================
const notifBtn = document.getElementById("open-notif-btn");
const notifPanel = document.getElementById("notif-panel");
const notifList = document.getElementById("notif-list");
const notifEmpty = document.getElementById("notif-empty");
const toggleUnread = document.getElementById("toggle-unread");
const notifDot = document.getElementById("notif-dot");
const markAllBtn = document.getElementById("mark-all-read");
const closeNotifPanel = document.getElementById("close-notif-panel");
const notifCountBadge = document.getElementById("notif-count-badge");

function updateUnreadBadge(count) {
  if (!notifCountBadge) return;
  const c = Number(count) || 0;
  if (c <= 0) {
    notifCountBadge.classList.add("hidden");
    notifCountBadge.textContent = "";
    notifDot?.classList.add("hidden");
  } else {
    notifCountBadge.classList.remove("hidden");
    notifCountBadge.textContent = (c > 99) ? "99+" : String(c);
    notifDot?.classList.remove("hidden");
  }
}

// Tăng 1 khi nhận realtime (nếu panel đang đóng)
function incrementUnreadBadge() {
  if (!notifCountBadge) return;
  const isOpen = !document.getElementById("notif-panel").classList.contains("hidden");
  if (isOpen) return; // panel mở thì sẽ reload list và tính lại

  const cur = parseInt(notifCountBadge.textContent) || 0;
  updateUnreadBadge(cur + 1);
}
// ================== ⏰ Helper ==================
function formatRelativeTime(isoString) {
  const now = new Date();
  const date = new Date(isoString);
  const diff = Math.floor((now - date) / 60000);
  if (diff < 1) return "just now";
  if (diff < 60) return `${diff} min${diff > 1 ? "s" : ""} ago`;
  const hrs = Math.floor(diff / 60);
  if (hrs < 24) return `${hrs} hour${hrs > 1 ? "s" : ""} ago`;
  const days = Math.floor(hrs / 24);
  return `${days} day${days > 1 ? "s" : ""} ago`;
}

// ================== 📥 Load Notifications (Tre llo-style) ==================
async function loadNotifications(onlyUnread = false) {
  const token = localStorage.getItem("token");
  notifList.innerHTML = `<p class="text-gray-400 text-sm italic text-center py-4">Loading...</p>`;
  notifEmpty.classList.add("hidden");

  try {
    const res = await fetch(`/api/notifications`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (!res.ok) throw new Error(`Failed to fetch notifications (${res.status})`);

    const data = await res.json();
    const list = Array.isArray(data)
      ? data
      : data?.content || data?.notifications || data?.data || [];

    const totalUnread = list.filter(n => n.status && n.status.toLowerCase() !== "read").length;
    updateUnreadBadge(totalUnread);

    const filtered = onlyUnread
      ? list.filter((n) => n.status && n.status.toLowerCase() !== "read")
      : list;

    if (filtered.length === 0) {
      notifEmpty.classList.remove("hidden");
      notifList.innerHTML = "";
      markAllBtn?.classList.add("hidden");
      notifDot.classList.add("hidden");
      return;
    }

    const hasUnread = list.some(
      (n) => n.status && n.status.toLowerCase() !== "read"
    );
    notifDot.classList.toggle("hidden", !hasUnread);
    markAllBtn?.classList.remove("hidden");

    notifList.innerHTML = filtered
      .map((n) => {
        const id = n.notificationId || n.id;
        const link = n.link || "#";
        const msg = n.message || "";
        const createdAt = n.createdAt || new Date().toISOString();
        const isRead = n.status && n.status.toLowerCase() === "read";
        const senderName = n.senderName || "Hệ thống";
        const senderAvatar =
          n.senderAvatar && n.senderAvatar.trim() !== ""
            ? n.senderAvatar
            : "https://cdn-icons-png.flaticon.com/512/149/149071.png";

        return `
          <div onclick="handleNotificationClick(${id}, '${link}')"
               class="bg-white px-4 py-3 hover:bg-gray-50 transition cursor-pointer relative border-b border-gray-100 ${
                 isRead ? "opacity-60" : ""
               }">
            <div class="flex items-start gap-3">
              <img src="${senderAvatar}" 
                   class="w-9 h-9 rounded-full border object-cover" 
                   alt="avatar">
              <div class="flex-1 text-sm">
                <p class="text-gray-800 font-medium">${senderName}</p>
                <p class="text-gray-700 leading-snug">${msg}</p>
                <p class="text-xs text-gray-400 mt-1">${formatRelativeTime(createdAt)}</p>
              </div>
              ${
                !isRead
                  ? `<span class="absolute right-3 top-3 w-2.5 h-2.5 bg-blue-500 rounded-full"></span>`
                  : ""
              }
            </div>
          </div>`;
      })
      .join("");
  } catch (err) {
    console.error("❌ loadNotifications:", err);
    notifList.innerHTML = `<p class="text-red-500 text-sm text-center py-4">Failed to load notifications</p>`;
  }
}

// ================== ✅ Mark Read ==================
async function markNotificationRead(id) {
  if (!id) return;
  const token = localStorage.getItem("token");
  try {
    const res = await fetch(`/api/notifications/${id}/read`, {
      method: "PUT",
      headers: { Authorization: `Bearer ${token}` },
    });
    if (!res.ok) console.warn("⚠️ Failed to mark read:", res.status);
  } catch (err) {
    console.error("❌ markNotificationRead:", err);
  }
}

// ================== ✅ Mark All as Read ==================
async function markAllNotificationsRead() {
  const token = localStorage.getItem("token");
  try {
    const res = await fetch(`/api/notifications/read-all`, {
      method: "PUT",
      headers: { Authorization: `Bearer ${token}` },
    });
    if (!res.ok) throw new Error("Failed to mark all as read");
    await loadNotifications(toggleUnread.checked);
    updateUnreadBadge(0);
  } catch (err) {
    console.error("❌ markAllNotificationsRead:", err);
  }
}
if (markAllBtn) markAllBtn.addEventListener("click", markAllNotificationsRead);

// ================== 🔗 Handle Click Notification ==================
async function handleNotificationClick(id, link) {
  console.log("🟣 CLICK:", id, link);
  await markNotificationRead(id);
  // Giảm 1 local (UI) – server sẽ sync lại khi reload panel
  const cur = parseInt(document.getElementById("notif-count-badge")?.textContent) || 0;
  if (cur > 0) updateUnreadBadge(cur - 1);

  if (!link || link === "#" || link === "null") {
    console.log("ℹ️ No link to redirect.");
    return;
  }

  // 🧩 Trường hợp task notification
  if (link.includes("/tasks/")) {
    const taskId = link.split("taskId=")[1] || link.split("/tasks/")[1];
    if (taskId) {
      await openTaskDetailFromApi(taskId);
      return;
    }
  }

  // 🧭 Nếu là link dự án thì mở nội bộ (cùng tab)
  if (link.startsWith("/view/pm/project/")) {
    window.location.href = link; // 👉 Chuyển nội bộ thay vì mở tab mới
    return;
  }

  // 🌐 Mặc định mở tab mới
  window.open(link, "_blank");
}


async function openTaskDetailFromApi(taskId) {
  try {
    const res = await fetch(`/api/tasks/${taskId}`, {
      headers: { "Authorization": "Bearer " + localStorage.getItem("token") }
    });
    if (!res.ok) throw new Error("Failed to load task");
    const task = await res.json();
    openTaskModal(task);
  } catch (err) {
    console.error("❌ openTaskDetailFromApi:", err);
    alert("Không thể tải thông tin công việc.");
  }
}


// ================== 🪟 Render Task Detail into Modal ==================
function openTaskModal(task) {
  const modal = document.getElementById("task-detail-modal");
  if (!modal) {
    console.error("❌ Modal task-detail-modal not found in DOM");
    return;
  }

  const titleEl = modal.querySelector("h2.text-2xl");
  if (titleEl) titleEl.textContent = task.title || "(Untitled Task)";

  const descContent = modal.querySelector("#description-content");
  const descPlaceholder = modal.querySelector("#description-placeholder");
  if (descContent && descPlaceholder) {
    if (task.descriptionMd && task.descriptionMd.trim() !== "") {
      descContent.textContent = task.descriptionMd;
      descPlaceholder.classList.add("hidden");
    } else {
      descContent.textContent = "";
      descPlaceholder.classList.remove("hidden");
    }
  }

  const dueText = modal.querySelector("#due-date-text");
  const dueStatus = modal.querySelector("#due-date-status");
  if (dueText && dueStatus) {
    if (task.deadline) {
      const date = new Date(task.deadline);
      dueText.textContent = date.toLocaleString();
      dueStatus.textContent = "Due";
      dueStatus.className =
        "ml-2 text-xs font-medium rounded px-2 py-0.5 bg-yellow-100 text-yellow-700";
    } else {
      dueText.textContent = "No due date";
      dueStatus.textContent = "None";
      dueStatus.className =
        "ml-2 text-xs font-medium rounded px-2 py-0.5 bg-gray-200 text-gray-600";
    }
  }

  const attachList = modal.querySelector("#attachments-list");
  if (attachList) {
    if (task.attachments && task.attachments.length > 0) {
      attachList.innerHTML = task.attachments
        .map(
          (a) => `
        <div class="flex items-center justify-between border rounded px-3 py-2">
          <a href="${a.fileUrl}" target="_blank" class="text-blue-600 hover:underline truncate">${a.fileName}</a>
          <span class="text-xs text-gray-400">${new Date(
            a.uploadedAt
          ).toLocaleDateString()}</span>
        </div>`
        )
        .join("");
    } else {
      attachList.innerHTML = `<p class="text-gray-400 italic">No attachments yet.</p>`;
    }
  }

  if (typeof loadActivityFeed === "function") {
    loadActivityFeed(task.taskId);
  }

  modal.classList.remove("hidden");
}

// ================== 📡 WebSocket Realtime Notifications ==================
let stompClient = null;
let wsConnected = false;
let wsRetry = 0;
const WS_MAX_RETRY = 8;          // tối đa 8 lần (≈ ~30s backoff)
const WS_BASE_DELAY = 1000;      // 1s
const WS_MAX_DELAY  = 30000;     // 30s
let wsSubscription = null;

function backoffDelay(attempt) {
  // exponential backoff + jitter
  const expo = Math.min(WS_MAX_DELAY, WS_BASE_DELAY * Math.pow(2, attempt));
  return Math.floor(expo * (0.7 + Math.random() * 0.6)); // 70%–130% jitter
}

async function connectWebSocketNotifications() {
  let token = localStorage.getItem("token");

  if (!token) {
    try {
      const res = await fetch("/api/auth/me", { credentials: "include" });
      if (res.ok) {
        const data = await res.json();
        console.log("✅ Fallback via cookie:", data.email || data.user?.email);
      } else {
        console.warn("⚠️ Không thể xác thực cookie, WS sẽ không kết nối");
        return;
      }
    } catch (err) {
      console.error("❌ Lỗi khi xác thực cookie:", err);
      return;
    }
  }

  // 3️⃣ Tiếp tục kết nối (có hoặc không có token)
  if (stompClient && wsConnected) return;

  const socket = new SockJS("/ws");
  stompClient = Stomp.over(socket);
  stompClient.heartbeat.outgoing = 15000;
  stompClient.heartbeat.incoming = 15000;

  const headers = token ? { Authorization: `Bearer ${token}` } : {};
  stompClient.connect(
    headers,
    (frame) => {
      console.log("✅ WS connected:", frame);
      wsConnected = true;
      wsRetry = 0;

      // hủy sub cũ
      if (wsSubscription && typeof wsSubscription.unsubscribe === "function") {
        try { wsSubscription.unsubscribe(); } catch {}
      }

      wsSubscription = stompClient.subscribe("/user/queue/notifications", (message) => {
        try {
          const notif = JSON.parse(message.body);
          console.log("🔔 Realtime:", notif);
          incrementUnreadBadge();

          if (!document.getElementById("notif-panel").classList.contains("hidden")) {
            loadNotifications(document.getElementById("toggle-unread").checked);
          } else {
            showToastNotification(notif.title, notif.message, notif.senderAvatar, notif.senderName);
          }
        } catch (e) {
          console.error("❌ WS message parse:", e);
        }
      });
    },
    (error) => {
      console.warn("⚠️ WS connect ERROR:", error);
      scheduleWsReconnect();
    }
  );

  socket.onclose = () => {
    console.warn("⚠️ WS closed");
    wsConnected = false;
    scheduleWsReconnect();
  };
}


function scheduleWsReconnect() {
  if (wsConnected) return; // đã kết nối lại
  if (wsRetry >= WS_MAX_RETRY) {
    console.error("🚫 WS max retries reached — stop reconnecting");
    return;
  }
  const delay = backoffDelay(wsRetry);
  console.log(`⏳ WS reconnect in ${Math.round(delay/1000)}s (attempt ${wsRetry+1}/${WS_MAX_RETRY})`);
  setTimeout(() => {
    wsRetry++;
    connectWebSocketNotifications();
  }, delay);
}


function ensureToastStack() {
  let stack = document.getElementById("toast-stack");
  if (!stack) {
    stack = document.createElement("div");
    stack.id = "toast-stack";
    document.body.appendChild(stack);
  }
  return stack;
}

function showToastNotification(title, msg, avatarUrl, sender = "Hệ thống") {
  const stack = ensureToastStack();
  const toast = document.createElement("div");
  toast.className = "toast-card";

  const safeAvatar = (avatarUrl && avatarUrl.trim() !== "")
    ? avatarUrl
    : "https://cdn-icons-png.flaticon.com/512/149/149071.png";

  toast.innerHTML = `
    <img src="${safeAvatar}" class="w-10 h-10 rounded-full object-cover border" alt="avatar">
    <div class="flex-1">
      <div class="toast-title">${sender}</div>
      <div class="toast-msg">${msg}</div>
      <div class="toast-meta">${formatRelativeTime(new Date().toISOString())}</div>
    </div>
  `;

  stack.appendChild(toast);

  // Auto close sau 5s, có animation slide-out
  setTimeout(() => {
    toast.classList.add("toast-exit");
    setTimeout(() => toast.remove(), 200);
  }, 5000);
}



// ================== 🎯 Event bindings ==================
toggleUnread.addEventListener("change", (e) =>
  loadNotifications(e.target.checked)
);

notifBtn.addEventListener("click", (e) => {
  e.stopPropagation();
  notifPanel.classList.toggle("hidden");
  if (!notifPanel.classList.contains("hidden"))
    loadNotifications(toggleUnread.checked);
});

closeNotifPanel?.addEventListener("click", () =>
  notifPanel.classList.add("hidden")
);

document.addEventListener("click", (e) => {
  if (!notifPanel.contains(e.target) && !notifBtn.contains(e.target)) {
    notifPanel.classList.add("hidden");
  }
});

// 🔁 Auto refresh mỗi 30s
setInterval(() => {
  if (!notifPanel.classList.contains("hidden"))
    loadNotifications(toggleUnread.checked);
}, 30000);


// ====== ⋯ NOTIFICATION SETTINGS MENU ======
const notifMenuBtn = document.getElementById("notif-options-btn");
const notifMenu = document.getElementById("notif-options-menu");
const toggleEmailBtn = document.getElementById("toggle-email-btn");
const emailToggleText = document.getElementById("email-toggle-text");

let emailEnabled = true; 

notifMenuBtn?.addEventListener("click", (e) => {
  e.stopPropagation();
  notifMenu.classList.toggle("hidden");
});

// Đóng khi click ra ngoài
document.addEventListener("click", (e) => {
  if (!notifMenu.contains(e.target) && !notifMenuBtn.contains(e.target)) {
    notifMenu.classList.add("hidden");
  }
});


  // Gọi API bật/tắt Gmail notification
toggleEmailBtn?.addEventListener("click", async (e) => {
  e.preventDefault();     // 🛑 Ngăn hành vi mặc định
  e.stopPropagation();    // 🛑 Không lan sang <a> bên dưới

  emailEnabled = !emailEnabled;

  try {
    const token = localStorage.getItem("token");

    const res = await fetch("/api/settings/notifications/email-toggle", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${token}`
      },
      body: JSON.stringify({ enabled: emailEnabled })
    });

    if (res.ok) {
      emailToggleText.textContent = emailEnabled
        ? "Tắt thông báo qua Gmail"
        : "Bật lại thông báo qua Gmail";
      showToast(emailEnabled
        ? "✅ Đã bật lại thông báo Gmail"
        : "🚫 Đã tắt thông báo Gmail");
    } else {
      showToast("⚠️ Không thể cập nhật cài đặt.");
    }
  } catch (err) {
    console.error("❌ toggleEmailBtn error:", err);
    showToast("⚠️ Lỗi kết nối server.");
  }

  notifMenu.classList.add("hidden");
});
function showToast(message, type = "info") {
  const toast = document.createElement("div");
  toast.textContent = message;
  toast.className = `fixed bottom-4 right-4 px-4 py-2 rounded-lg text-white z-[9999]
    ${type === "error" ? "bg-red-500" : "bg-green-600"} shadow-lg animate-fadeIn`;
  document.body.appendChild(toast);
  setTimeout(() => {
    toast.classList.add("opacity-0", "transition-opacity", "duration-300");
    setTimeout(() => toast.remove(), 300);
  }, 2500);
}

openSettingsLink.addEventListener("click", async (e) => {
  e.preventDefault(); 
  const user = await ensureCurrentUser(); 
  if (!user) {
    alert("Bạn chưa đăng nhập! Vui lòng đăng nhập trước khi truy cập trang cài đặt.");
    window.location.href = "/view/signin";
    return;
  }
  const token = localStorage.getItem("token");
  if (!token) {
    alert(" Bạn chưa đăng nhập! Vui lòng đăng nhập trước khi truy cập trang cài đặt.");
    window.location.href = "/view/signin";
    return;
  }
  const res = await fetch("/api/settings/notifications/me", {
    headers: { "Authorization": `Bearer ${token}` },
  });

  if (!res.ok) {
    const errorMsg = await res.text(); 
    alert(` Không thể mở trang cài đặt! ${errorMsg || "Token không hợp lệ hoặc hết hạn."}`);
    return;
  }
  window.location.href = "/settings/notifications";
});
window.addEventListener("load", connectWebSocketNotifications);