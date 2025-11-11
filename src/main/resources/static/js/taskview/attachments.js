// ================== 📎 ATTACHMENTS MODULE (SECURE) ==================
import { escapeHtml, showToast } from "./utils.js";

// ========== DOM SELECTORS ==========
const uploadBtn = document.getElementById("upload-attachment-btn");
const fileInput = document.getElementById("attachment-file");
const attachmentsList = document.getElementById("attachments-list");
const previewModal = document.getElementById("attachment-preview-modal");
const previewContent = document.getElementById("preview-content");
const previewFileName = document.getElementById("preview-file-name");
const closePreviewBtn = document.getElementById("close-preview-btn");
const downloadFileBtn = document.getElementById("download-file-btn");
const attachPopup = document.getElementById("attach-popup");
const openAttachBtn = document.getElementById("open-attach-popup");
const closeAttachBtn = document.getElementById("close-attach-popup");
const cancelAttachBtn = document.getElementById("cancel-attach-btn");
const insertAttachBtn = document.getElementById("insert-attach-btn");
const chooseFileBtn = document.getElementById("choose-file-btn");
const popupFileInput = document.getElementById("popup-attachment-file");
const linkInput = document.getElementById("link-input");
const displayTextInput = document.getElementById("display-text");

// ================== INIT ==================
export function initAttachmentEvents() {
  if (!uploadBtn || !fileInput) return;

  // 🟦 Upload nhanh
  uploadBtn.addEventListener("click", () => fileInput.click());
  fileInput.addEventListener("change", handleFileUpload);

  // 🟩 Popup "Add Attachment"
  openAttachBtn?.addEventListener("click", async () => {
    attachPopup.classList.remove("hidden");
    await loadRecentLinks();
  });
  closeAttachBtn?.addEventListener("click", closeAttachPopup);
  cancelAttachBtn?.addEventListener("click", closeAttachPopup);
  chooseFileBtn?.addEventListener("click", () => popupFileInput.click());
  insertAttachBtn?.addEventListener("click", handlePopupInsert);

  // 🟧 Preview modal
  closePreviewBtn?.addEventListener("click", closePreviewModal);
  previewModal?.addEventListener("click", (e) => {
    if (e.target === previewModal) closePreviewModal();
  });
}

function closeAttachPopup() {
  attachPopup.classList.add("hidden");
}

// ================== UPLOAD ==================
async function handleFileUpload() {
  const file = fileInput.files[0];
  if (!file || !window.CURRENT_TASK_ID) return;

  const formData = new FormData();
  formData.append("file", file);

  try {
    const res = await fetch(
      `/api/tasks/${window.CURRENT_TASK_ID}/attachments`,
      {
        method: "POST",
        headers: { Authorization: "Bearer " + localStorage.getItem("token") },
        body: formData,
      }
    );
    if (!res.ok) throw new Error("Upload failed");
    await loadAttachments(window.CURRENT_TASK_ID);
    showToast("✅ Uploaded successfully!");
  } catch (err) {
    console.error("❌ Upload error:", err);
    showToast("❌ Failed to upload attachment", "error");
  } finally {
    fileInput.value = "";
  }
}

// ================== LOAD & RENDER ==================
export async function loadAttachments(taskId) {
  try {
    const res = await fetch(`/api/tasks/${taskId}/attachments`, {
      headers: { Authorization: "Bearer " + localStorage.getItem("token") },
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
  if (!items?.length) {
    attachmentsList.innerHTML = `<p class="text-gray-400 italic">No attachments yet.</p>`;
    return;
  }

  attachmentsList.innerHTML = items
    .map((a) => {
      const fileUrl =
        a.fileUrl?.startsWith("/") && !a.fileUrl.startsWith("/http")
          ? `${window.location.origin}${a.fileUrl}`
          : a.fileUrl;

      const isImage = a.mimeType?.startsWith("image/");
      const isLink =
        a.mimeType === "link/url" || /^https?:\/\//.test(a.fileUrl);
      const preview = isImage
        ? `<img src="${fileUrl}" class="w-20 h-20 object-cover rounded-md border cursor-pointer">`
        : `<div class="w-20 h-20 flex items-center justify-center bg-gray-100 border rounded-md text-gray-500 cursor-pointer">📄</div>`;

      const dataAttr = encodeURIComponent(
        JSON.stringify({ ...a, fileUrl, isLink })
      );

      return `
        <div class="attachment-row flex items-center gap-3 border border-gray-200 rounded-md p-2 hover:bg-gray-50 transition cursor-pointer"
             data-attachment='${dataAttr}'>
          ${preview}
          <div class="flex-1 truncate">
            <p class="file-name font-medium text-blue-600 hover:underline">${
              isLink ? "🌐 " : ""
            }${a.fileName}</p>
            <p class="text-xs text-gray-500">${
              a.uploadedBy?.name || "Unknown"
            } • ${new Date(a.uploadedAt).toLocaleString()}</p>
          </div>
          <button class="delete-attach text-red-500 hover:text-red-700 text-sm" data-id="${
            a.attachmentId
          }">🗑️</button>
        </div>`;
    })
    .join("");

  // 🟦 Events
  document.querySelectorAll(".attachment-row").forEach((row) => {
    const data = JSON.parse(decodeURIComponent(row.dataset.attachment));
    row.addEventListener("click", (e) => {
      if (e.target.closest(".delete-attach")) return;
      data.isLink
        ? window.open(data.fileUrl, "_blank")
        : openPreviewModal(data);
    });
  });

  document.querySelectorAll(".delete-attach").forEach((btn) => {
    btn.addEventListener("click", (e) => {
      e.stopPropagation();
      deleteAttachment(e.target.dataset.id);
    });
  });
}

// ================== DELETE ==================
async function deleteAttachment(id) {
  if (!confirm("🗑️ Delete this attachment?")) return;
  try {
    const res = await fetch(
      `/api/tasks/${window.CURRENT_TASK_ID}/attachments/${id}`,
      {
        method: "DELETE",
        headers: { Authorization: "Bearer " + localStorage.getItem("token") },
      }
    );
    if (!res.ok) throw new Error("Delete failed");
    await loadAttachments(window.CURRENT_TASK_ID);
    showToast("🗑️ Deleted attachment");
  } catch (err) {
    console.error("❌ Delete error:", err);
    showToast("❌ Failed to delete attachment", "error");
  }
}

// ================== ATTACHMENT PREVIEW MODAL ==================
async function openPreviewModal(attachment) {
  const isLink =
    attachment.isLink === true ||
    attachment.isLink === "true" ||
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
      const encodedName = encodeURIComponent(decodeURIComponent(parts[1]));
      url = parts[0] + "/download/" + encodedName;
    }
  }

  try {
    const headers = {
      Authorization: "Bearer " + localStorage.getItem("token"),
    };

    if (mime.startsWith("image/")) {
      const blob = await fetch(url, { headers }).then((r) => r.blob());
      const blobUrl = URL.createObjectURL(blob);
      previewContent.innerHTML = `
        <div class="relative flex flex-col items-center justify-center">
          <img src="${blobUrl}" class="max-h-[80vh] object-contain rounded-md shadow" />
          ${buildFooter(attachment)}
        </div>`;
    } else if (mime === "application/pdf") {
      const blob = await fetch(url, { headers }).then((r) => r.blob());
      const blobUrl = URL.createObjectURL(blob);
      previewContent.innerHTML = `
        <div class="relative">
          <iframe src="${blobUrl}" class="w-full h-[80vh]" frameborder="0"></iframe>
          ${buildFooter(attachment)}
        </div>`;
    } else if (
      mime.startsWith("text/") ||
      url.endsWith(".txt") ||
      url.endsWith(".log")
    ) {
      const text = await fetch(url, { headers }).then((r) => r.text());
      previewContent.innerHTML = `
        <div class="relative">
          <pre class="bg-gray-50 text-gray-800 p-4 rounded-md max-h-[70vh] overflow-auto whitespace-pre-wrap font-mono text-sm leading-relaxed">
${escapeHtml(text)}
          </pre>
          ${buildFooter(attachment)}
        </div>`;
    } else if (
      mime.includes("officedocument") ||
      url.endsWith(".docx") ||
      url.endsWith(".pptx") ||
      url.endsWith(".xlsx")
    ) {
      const gview = `https://docs.google.com/gview?embedded=true&url=${encodeURIComponent(
        url
      )}`;
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

function attachFooterEvents(attachment = {}) {
  const fileUrl = attachment.fileUrl || "#";
  const fileName = attachment.fileName || "Untitled";

  document
    .getElementById("open-tab-btn")
    ?.addEventListener("click", () => window.open(fileUrl, "_blank"));
  document
    .getElementById("download-btn")
    ?.addEventListener("click", async () => {
      try {
        const token = localStorage.getItem("token");
        const res = await fetch(fileUrl, {
          headers: { Authorization: "Bearer " + token },
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

  document.getElementById("delete-btn")?.addEventListener("click", async () => {
    if (window.CURRENT_ROLE !== "ROLE_PM") {
      alert("❌ Only Project Managers can delete attachments!");
      return;
    }
    if (!confirm(`🗑️ Delete ${fileName}?`)) return;
    try {
      await deleteAttachment(attachment.attachmentId);
      previewModal.classList.add("hidden");
    } catch (err) {
      alert("❌ Failed to delete attachment");
      console.error(err);
    }
  });
}

function closePreviewModal() {
  previewModal.classList.add("hidden");
  previewModal.style.opacity = "0";
  previewModal.style.pointerEvents = "none";
  previewContent.innerHTML = "";
}

// ================== RECENT LINKS POPUP ==================
async function loadRecentLinks() {
  const container = document.getElementById("recent-links");
  container.innerHTML = `<p class="text-gray-400 italic text-sm">Loading recent links...</p>`;

  try {
    const res = await fetch(`/api/attachments/recent-links`, {
      headers: { Authorization: "Bearer " + localStorage.getItem("token") },
    });
    if (!res.ok) throw new Error("Failed to fetch recent links");
    const links = await res.json();

    if (!Array.isArray(links) || links.length === 0) {
      container.innerHTML = `<p class="text-gray-400 italic text-sm">No recent links</p>`;
      return;
    }

    container.innerHTML = "";
    links.slice(0, 5).forEach((link) => {
      const p = document.createElement("p");
      p.textContent = `📄 ${link.fileName || link.fileUrl}`;
      p.className = "hover:underline cursor-pointer";
      p.addEventListener("click", () => {
        linkInput.value = link.fileUrl;
        displayTextInput.value = link.fileName || "";
      });
      container.appendChild(p);
    });
  } catch (err) {
    console.error("❌ loadRecentLinks error:", err);
    container.innerHTML = `<p class="text-red-500 text-sm">Failed to load recent links</p>`;
  }
}

// ================== INSERT FILE / LINK ==================
async function handlePopupInsert() {
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
      const formData = new FormData();
      formData.append("file", file);
      const res = await fetch(
        `/api/tasks/${window.CURRENT_TASK_ID}/attachments`,
        {
          method: "POST",
          headers: { Authorization: "Bearer " + localStorage.getItem("token") },
          body: formData,
        }
      );
      if (!res.ok) throw new Error("Upload failed");
    } else {
      const res = await fetch(
        `/api/tasks/${window.CURRENT_TASK_ID}/attachments/link`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: "Bearer " + localStorage.getItem("token"),
          },
          body: JSON.stringify({
            fileUrl: link,
            fileName: displayText || link,
          }),
        }
      );
      if (!res.ok) throw new Error("Attach link failed");
    }

    showToast("✅ Uploaded successfully!");
    attachPopup.classList.add("hidden");
    await loadAttachments(window.CURRENT_TASK_ID);
  } catch (err) {
    console.error("❌ Upload error:", err);
    showToast("❌ Failed to upload or attach link.", "error");
  } finally {
    popupFileInput.value = "";
    linkInput.value = "";
    displayTextInput.value = "";
    insertAttachBtn.disabled = false;
  }
}
