// ✅ attachments.js – Quản lý upload, preview, delete, load attachments
import { apiFetch, escapeHtml, showToast } from "./utils.js";
import { currentTask } from "./modal-task.js";

let attachments = [];

/* ---------------------- 📦 LOAD ATTACHMENTS ---------------------- */
export async function loadAttachments(taskId) {
  try {
    const res = await apiFetch(`/api/tasks/${taskId}/attachments`);
    attachments = await res.json();
    renderAttachments(attachments);
  } catch (err) {
    console.error("❌ loadAttachments failed:", err);
    showToast("Không thể tải danh sách tệp.", "error");
  }
}

/* ---------------------- 🧾 RENDER ---------------------- */
function renderAttachments(list) {
  const container = document.getElementById("attachments-list");
  if (!container) return;

  if (!list || !list.length) {
    container.innerHTML = `<p class="text-gray-400 italic">No attachments yet.</p>`;
    return;
  }

  container.innerHTML = "";
  list.forEach(file => {
    const div = document.createElement("div");
    div.className = "flex items-center justify-between bg-gray-50 border border-gray-200 rounded-md px-2 py-1";

    div.innerHTML = `
      <div class="truncate flex items-center gap-2">
        <span class="text-blue-600">📎</span>
        <a href="#" data-preview="${file.id}" 
           class="text-sm text-blue-700 hover:underline truncate">${escapeHtml(file.displayName || file.fileName)}</a>
      </div>
      <button class="text-red-500 hover:text-red-700 text-sm" data-delete="${file.id}">Delete</button>
    `;
    container.appendChild(div);
  });

  container.querySelectorAll("[data-preview]").forEach(link =>
    link.addEventListener("click", e => {
      e.preventDefault();
      openPreview(e.target.dataset.preview);
    })
  );

  container.querySelectorAll("[data-delete]").forEach(btn =>
    btn.addEventListener("click", e => deleteAttachment(e.target.dataset.delete))
  );
}

/* ---------------------- ⬆️ UPLOAD ---------------------- */
document.getElementById("upload-attachment-btn")?.addEventListener("click", () => {
  document.getElementById("attachment-file")?.click();
});

document.getElementById("attachment-file")?.addEventListener("change", async e => {
  const file = e.target.files[0];
  if (!file || !currentTask) return;

  const formData = new FormData();
  formData.append("file", file);

  try {
    const res = await apiFetch(`/api/tasks/${currentTask.taskId}/attachments`, {
      method: "POST",
      body: formData
    });
    if (!res.ok) throw new Error("Upload failed");

    const uploaded = await res.json();
    attachments.push(uploaded);
    renderAttachments(attachments);
    showToast(`✅ Đã tải lên "${file.name}"`);
  } catch (err) {
    console.error("❌ uploadAttachment failed:", err);
    showToast("Không thể tải lên tệp.", "error");
  }
});

/* ---------------------- 🔗 ATTACH LINK (POPUP) ---------------------- */
document.getElementById("open-attach-popup")?.addEventListener("click", () => {
  document.getElementById("attach-popup")?.classList.remove("hidden");
});

document.getElementById("close-attach-popup")?.addEventListener("click", () => {
  document.getElementById("attach-popup")?.classList.add("hidden");
});

document.getElementById("cancel-attach-btn")?.addEventListener("click", () => {
  document.getElementById("attach-popup")?.classList.add("hidden");
});

document.getElementById("insert-attach-btn")?.addEventListener("click", async () => {
  const link = document.getElementById("link-input")?.value.trim();
  const display = document.getElementById("display-text")?.value.trim();
  if (!link || !currentTask) {
    showToast("⚠️ Vui lòng nhập link hợp lệ.", "warning");
    return;
  }

  try {
    const res = await apiFetch(`/api/tasks/${currentTask.taskId}/attachments/link`, {
      method: "POST",
      body: JSON.stringify({ link, displayName: display || link })
    });
    if (!res.ok) throw new Error();

    const newLink = await res.json();
    attachments.push(newLink);
    renderAttachments(attachments);
    showToast("🔗 Đã thêm liên kết!");
    document.getElementById("attach-popup")?.classList.add("hidden");
  } catch (err) {
    console.error("❌ insertLink failed:", err);
    showToast("Không thể thêm liên kết.", "error");
  }
});

/* ---------------------- 👁️ PREVIEW ---------------------- */
async function openPreview(fileId) {
  const modal = document.getElementById("attachment-preview-modal");
  const preview = document.getElementById("preview-content");
  const nameEl = document.getElementById("preview-file-name");
  const downloadBtn = document.getElementById("download-file-btn");

  if (!modal || !preview) return;
  modal.classList.remove("hidden");
  preview.innerHTML = `<p class="text-gray-400 italic">Loading preview...</p>`;

  try {
    const res = await apiFetch(`/api/attachments/${fileId}`);
    if (!res.ok) throw new Error();
    const blob = await res.blob();

    const type = blob.type;
    const url = URL.createObjectURL(blob);
    nameEl.textContent = attachments.find(f => f.id == fileId)?.displayName || "Attachment";

    if (type.startsWith("image/")) {
      preview.innerHTML = `<img src="${url}" alt="preview" class="max-h-[70vh] mx-auto rounded shadow">`;
    } else if (type === "application/pdf") {
      preview.innerHTML = `<iframe src="${url}" class="w-full h-[70vh]" frameborder="0"></iframe>`;
    } else if (type.startsWith("text/")) {
      const text = await blob.text();
      preview.innerHTML = `<pre class="text-sm whitespace-pre-wrap p-3 bg-gray-50 border rounded">${escapeHtml(text)}</pre>`;
    } else {
      preview.innerHTML = `<p class="text-gray-500 italic">Không thể xem trước loại tệp này. Hãy tải xuống để xem.</p>`;
    }

    // Nút tải an toàn
    downloadBtn.href = url;
    downloadBtn.download = nameEl.textContent;
  } catch (err) {
    console.error("❌ openPreview failed:", err);
    showToast("Không thể tải xem trước tệp.", "error");
  }
}

document.getElementById("close-preview-btn")?.addEventListener("click", () => {
  document.getElementById("attachment-preview-modal")?.classList.add("hidden");
});

/* ---------------------- 🗑️ DELETE ---------------------- */
async function deleteAttachment(fileId) {
  if (!confirm("Bạn có chắc muốn xoá tệp này?")) return;
  try {
    const res = await apiFetch(`/api/attachments/${fileId}`, { method: "DELETE" });
    if (!res.ok) throw new Error();
    attachments = attachments.filter(f => f.id != fileId);
    renderAttachments(attachments);
    showToast("🗑️ Đã xoá tệp.");
  } catch (err) {
    console.error("❌ deleteAttachment failed:", err);
    showToast("Không thể xoá tệp.", "error");
  }
}
