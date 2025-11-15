// ================== 📎 ATTACHMENTS MODULE (SECURE) ==================
import { escapeHtml, showToast, getToken } from "./utils.js";
import {
  refreshActivityFeedOnly,
  showActivitySectionIfHidden,
  updateCardAttachmentCount,
} from "./main.js";

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

// ================== HELPERS ==================
function getCurrentUserId() {
  const raw = localStorage.getItem("currentUserId");
  if (!raw) return null;
  const parsed = Number(raw);
  return Number.isNaN(parsed) ? null : parsed;
}

function hasManagerAttachmentPrivileges(role) {
  const normalized = (role || window.CURRENT_ROLE || "").toUpperCase();
  return normalized === "ROLE_PM" || normalized === "ROLE_ADMIN";
}

function canCurrentUserDelete(attachment = {}) {
  if (hasManagerAttachmentPrivileges()) return true;
  const currentUserId = getCurrentUserId();
  const uploaderId = attachment?.uploadedBy?.userId;
  if (currentUserId == null || uploaderId == null) return false;
  return Number(currentUserId) === Number(uploaderId);
}

async function downloadAttachmentFile(fileUrl, fileName) {
  try {
    const token = getToken();
    const res = await fetch(fileUrl, {
      headers: { Authorization: "Bearer " + token },
      credentials: "include",
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
    console.error(" downloadAttachmentFile error:", err);
    showToast(" Failed to download file", "error");
  }
}

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

// ================== UPLOADING NOTIFICATION ==================
let uploadingNotification = null;

function showUploadingNotification() {
  // Remove existing notification if any
  if (uploadingNotification) {
    uploadingNotification.remove();
  }

  uploadingNotification = document.createElement("div");
  uploadingNotification.id = "uploading-notification";
  uploadingNotification.className =
    "fixed bottom-4 left-4 bg-white border border-gray-300 rounded-lg shadow-lg px-4 py-3 flex items-center gap-3 z-[9999] min-w-[200px]";
  uploadingNotification.innerHTML = `
    <div class="animate-spin">
      <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
      </svg>
    </div>
    <span class="text-gray-700 text-sm font-medium">Uploading file...</span>
    <button id="close-uploading-notification" class="ml-auto text-gray-400 hover:text-gray-600">
      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
      </svg>
    </button>
  `;

  document.body.appendChild(uploadingNotification);

  // Close button handler
  const closeBtn = uploadingNotification.querySelector(
    "#close-uploading-notification"
  );
  if (closeBtn) {
    closeBtn.addEventListener("click", () => {
      hideUploadingNotification();
    });
  }
}

function hideUploadingNotification() {
  if (uploadingNotification) {
    uploadingNotification.remove();
    uploadingNotification = null;
  }
}

// ================== UPLOAD ==================
async function handleFileUpload() {
  const file = fileInput.files[0];
  if (!file || !window.CURRENT_TASK_ID) return;

  // Show uploading notification
  showUploadingNotification();

  const formData = new FormData();
  formData.append("file", file);

  try {
    const token = getToken();
    const headers = {};
    if (token) {
      headers.Authorization = "Bearer " + token;
    }
    const res = await fetch(
      `/api/tasks/${window.CURRENT_TASK_ID}/attachments`,
      {
        method: "POST",
        headers,
        body: formData,
        credentials: "include",
      }
    );
    if (!res.ok) {
      let errorMsg = "Upload failed";
      try {
        const text = await res.text();
        const json = JSON.parse(text);
        errorMsg = json.message || json.error || text;
      } catch (e) {}
      throw new Error(errorMsg);
    }

    // Hide uploading notification
    hideUploadingNotification();

    await loadAttachments(window.CURRENT_TASK_ID);

    // Refresh activity feed to show new activity
    showActivitySectionIfHidden();
    await refreshActivityFeedOnly(window.CURRENT_TASK_ID);

    // Update attachment count on card
    try {
      const token = getToken();
      const headers = {};
      if (token) {
        headers.Authorization = "Bearer " + token;
      }
      const res = await fetch(
        `/api/tasks/${window.CURRENT_TASK_ID}/attachments`,
        {
          headers,
          credentials: "include",
        }
      );
      if (res.ok) {
        const attachments = await res.json();
        updateCardAttachmentCount(
          window.CURRENT_TASK_ID,
          attachments ? attachments.length : 0
        );
      }
    } catch (err) {
      console.error("Failed to update attachment count:", err);
    }
  } catch (err) {
    // Hide uploading notification
    hideUploadingNotification();

    showToast(err.message, "error");
  } finally {
    fileInput.value = "";
  }
}

// ================== LOAD & RENDER ==================
export async function loadAttachments(taskId) {
  try {
    const token = getToken();
    const headers = {};
    if (token) {
      headers.Authorization = "Bearer " + token;
    }
    const res = await fetch(`/api/tasks/${taskId}/attachments`, {
      headers,
      credentials: "include",
    });
    if (!res.ok) throw new Error("Load attachments failed");
    const attachments = await res.json();
    renderAttachments(attachments);
  } catch (err) {
    attachmentsList.innerHTML = `<p class="text-red-500 text-sm">Failed to load attachments</p>`;
  }
}

// Function to get website logo/icon based on URL
function getWebsiteIcon(url) {
  if (!url) return null;

  try {
    const urlObj = new URL(url);
    const hostname = urlObj.hostname.toLowerCase();

    // Remove www. prefix
    const domain = hostname.replace(/^www\./, "");

    // Map of known domains to their icons/logos
    const iconMap = {
      "youtube.com": {
        type: "svg",
        content: `<svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 flex-shrink-0" viewBox="0 0 24 24" fill="#FF0000">
          <path d="M23.498 6.186a3.016 3.016 0 0 0-2.122-2.136C19.505 3.545 12 3.545 12 3.545s-7.505 0-9.377.505A3.017 3.017 0 0 0 .502 6.186C0 8.07 0 12 0 12s0 3.93.502 5.814a3.016 3.016 0 0 0 2.122 2.136c1.871.505 9.376.505 9.376.505s7.505 0 9.377-.505a3.015 3.015 0 0 0 2.122-2.136C24 15.93 24 12 24 12s0-3.93-.502-5.814zM9.545 15.568V8.432L15.818 12l-6.273 3.568z"/>
        </svg>`,
      },
      "facebook.com": {
        type: "svg",
        content: `<svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 flex-shrink-0" viewBox="0 0 24 24" fill="#1877F2">
          <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/>
        </svg>`,
      },
      "gmail.com": {
        type: "svg",
        content: `<svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 flex-shrink-0" viewBox="0 0 24 24" fill="#EA4335">
          <path d="M24 5.457v13.909c0 .904-.732 1.636-1.636 1.636h-3.819V11.73L12 16.64l-6.545-4.91v9.273H1.636A1.636 1.636 0 0 1 0 19.366V5.457c0-2.023 2.309-3.277 4.091-1.964L12 9.09l7.909-5.596C21.691 2.18 24 3.434 24 5.457z"/>
        </svg>`,
      },
      "mail.google.com": {
        type: "svg",
        content: `<svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 flex-shrink-0" viewBox="0 0 24 24" fill="#EA4335">
          <path d="M24 5.457v13.909c0 .904-.732 1.636-1.636 1.636h-3.819V11.73L12 16.64l-6.545-4.91v9.273H1.636A1.636 1.636 0 0 1 0 19.366V5.457c0-2.023 2.309-3.277 4.091-1.964L12 9.09l7.909-5.596C21.691 2.18 24 3.434 24 5.457z"/>
        </svg>`,
      },
      "chatgpt.com": {
        type: "svg",
        content: `<svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 flex-shrink-0" viewBox="0 0 24 24" fill="#10A37F">
          <path d="M21.308 11.308c0 5.675-4.633 10.308-10.308 10.308-1.74 0-3.38-.433-4.81-1.198l-4.19 1.198 1.198-4.19C1.433 16.688 1 15.048 1 13.308 1 7.633 5.633 3 11.308 3c5.675 0 10.308 4.633 10.308 8.308zm-10.308-6.154c-3.398 0-6.154 2.756-6.154 6.154s2.756 6.154 6.154 6.154c.89 0 1.733-.189 2.5-.525l1.837.525-.525-1.837c.336-.767.525-1.61.525-2.5 0-3.398-2.756-6.154-6.154-6.154z"/>
        </svg>`,
      },
      "openai.com": {
        type: "svg",
        content: `<svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 flex-shrink-0" viewBox="0 0 24 24" fill="#10A37F">
          <path d="M21.308 11.308c0 5.675-4.633 10.308-10.308 10.308-1.74 0-3.38-.433-4.81-1.198l-4.19 1.198 1.198-4.19C1.433 16.688 1 15.048 1 13.308 1 7.633 5.633 3 11.308 3c5.675 0 10.308 4.633 10.308 8.308zm-10.308-6.154c-3.398 0-6.154 2.756-6.154 6.154s2.756 6.154 6.154 6.154c.89 0 1.733-.189 2.5-.525l1.837.525-.525-1.837c.336-.767.525-1.61.525-2.5 0-3.398-2.756-6.154-6.154-6.154z"/>
        </svg>`,
      },
    };

    // Check for exact domain match
    if (iconMap[domain]) {
      return iconMap[domain];
    }

    // Check for partial domain match (e.g., subdomains)
    for (const [key, value] of Object.entries(iconMap)) {
      if (domain.includes(key) || key.includes(domain.split(".")[0])) {
        return value;
      }
    }

    return null;
  } catch (e) {
    return null;
  }
}

function renderAttachments(items) {
  // Separate links and files
  const links = items.filter(
    (a) => a.mimeType === "link/url" || /^https?:\/\//.test(a.fileUrl || "")
  );
  const files = items.filter(
    (a) => a.mimeType !== "link/url" && !/^https?:\/\//.test(a.fileUrl || "")
  );

  let html = "";

  // Render Links section
  if (links.length > 0) {
    html += `<div class="mb-4">
      <h4 class="flex items-center gap-2 text-xs font-semibold text-gray-900 mb-2">
        <span class="inline-flex h-6 w-6 items-center justify-center rounded-full border border-gray-200 bg-white text-gray-500">🔗</span>
        <span>Links</span>
      </h4>
      <div class="space-y-2">`;

    links.forEach((a) => {
      const canDelete = canCurrentUserDelete(a);
      const fileUrl =
        a.fileUrl?.startsWith("/") && !a.fileUrl.startsWith("/http")
          ? `${window.location.origin}${a.fileUrl}`
          : a.fileUrl;

      // Get website icon
      const websiteIcon = getWebsiteIcon(fileUrl);
      const hasLogo = !!websiteIcon;

      const dataAttr = encodeURIComponent(
        JSON.stringify({
          ...a,
          fileUrl,
          isLink: true,
          hasLogo,
          websiteIcon: websiteIcon ? websiteIcon.content : null,
        })
      );

      let iconHtml = "";

      if (websiteIcon) {
        iconHtml = websiteIcon.content;
      } else {
        // Default chain link icon (like image 2)
        iconHtml = `<svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-gray-800 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
        </svg>`;
      }

      const commentButtonClasses = canDelete
        ? "link-comment-btn w-full text-left px-3 py-2 text-sm text-gray-700 hover:bg-gray-100 rounded-t-md"
        : "link-comment-btn w-full text-left px-3 py-2 text-sm text-gray-700 hover:bg-gray-100 rounded-md";
      const removeButtonHtml = canDelete
        ? `<button class="link-remove-btn w-full text-left px-3 py-2 text-sm text-red-600 hover:bg-gray-100 rounded-b-md" data-id="${a.attachmentId}" data-can-delete="true">Remove</button>`
        : "";

      html += `
        <div class="link-row flex items-center gap-2 border border-gray-200 rounded-md p-2 bg-white hover:bg-gray-50 transition cursor-pointer"
             data-attachment='${dataAttr}'>
          ${iconHtml}
          <a href="${fileUrl}" target="_blank" class="text-blue-600 hover:underline flex-1 truncate font-normal" onclick="event.stopPropagation()">${
        a.fileName || fileUrl
      }</a>
          <div class="relative flex-shrink-0">
            <button class="link-options-btn text-gray-500 hover:text-gray-700 text-sm px-1 py-0.5 rounded hover:bg-gray-100" data-id="${
              a.attachmentId
            }" data-url="${encodeURIComponent(
        fileUrl
      )}" onclick="event.stopPropagation()">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 5v.01M12 12v.01M12 19v.01M12 6a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2z" />
              </svg>
            </button>
            <div class="link-context-menu hidden absolute right-0 top-6 bg-white border border-gray-300 rounded-md shadow-lg z-50 min-w-[120px]">
              <button class="${commentButtonClasses}" data-id="${
        a.attachmentId
      }" data-url="${encodeURIComponent(fileUrl)}">Comment</button>
              ${removeButtonHtml}
            </div>
          </div>
        </div>`;
    });

    html += `</div></div>`;
  }

  // Render Files section
  if (files.length > 0) {
    html += `<div class="mt-5">
      <h4 class="flex items-center gap-2 text-xs font-semibold text-gray-900 mb-2">
        <span class="inline-flex h-6 w-6 items-center justify-center rounded-full border border-gray-200 bg-white text-gray-500">📁</span>
        <span>Files</span>
      </h4>
      <div class="space-y-2">`;
    files.forEach((a) => {
      const canDelete = canCurrentUserDelete(a);
      const fileUrl =
        a.fileUrl?.startsWith("/") && !a.fileUrl.startsWith("/http")
          ? `${window.location.origin}${a.fileUrl}`
          : a.fileUrl;

      const isImage = a.mimeType?.startsWith("image/");
      const preview = isImage
        ? `<img src="${fileUrl}" class="w-20 h-20 object-cover rounded-md border cursor-pointer">`
        : `<div class="w-20 h-20 flex items-center justify-center bg-gray-100 border rounded-md text-gray-500 cursor-pointer">📄</div>`;

      const dataAttr = encodeURIComponent(
        JSON.stringify({ ...a, fileUrl, isLink: false })
      );

      html += `
        <div class="attachment-row flex items-center gap-3 border border-gray-200 rounded-md p-2 hover:bg-gray-50 transition cursor-pointer"
             data-attachment='${dataAttr}'>
          ${preview}
          <div class="flex-1 truncate">
            <p class="file-name font-semibold text-gray-600">${a.fileName}</p>
            <p class="text-xs text-gray-500">${
              a.uploadedBy?.name || "Unknown"
            } • ${new Date(a.uploadedAt).toLocaleString()}</p>
          </div>
          <div class="flex items-center gap-1 file-actions">
            <button class="file-open-btn text-gray-500 hover:text-gray-700 text-sm px-2 py-1 rounded hover:bg-gray-100" title="Preview">
              ↗️
            </button>
            <div class="relative">
              <button class="file-options-btn text-gray-500 hover:text-gray-700 text-sm px-1 py-0.5 rounded hover:bg-gray-100" title="More actions">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 5v.01M12 12v.01M12 19v.01M12 6a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2z" />
                </svg>
              </button>
              <div class="file-context-menu hidden absolute right-0 top-6 bg-white border border-gray-300 rounded-md shadow-lg z-50 min-w-[140px]">
                <button class="file-preview-btn w-full text-left px-3 py-2 text-sm text-gray-700 hover:bg-gray-100">Preview</button>
                <button class="file-download-btn w-full text-left px-3 py-2 text-sm text-gray-700 hover:bg-gray-100">Download</button>
                ${
                  canDelete
                    ? `<button class="file-delete-btn w-full text-left px-3 py-2 text-sm text-red-600 hover:bg-gray-100" data-id="${a.attachmentId}" data-can-delete="true">Delete</button>`
                    : ""
                }
              </div>
            </div>
          </div>
        </div>`;
    });
    html += `</div></div>`;
  }

  attachmentsList.innerHTML = html;

  // 🟦 Events
  document.querySelectorAll(".attachment-row, .link-row").forEach((row) => {
    const data = JSON.parse(decodeURIComponent(row.dataset.attachment));
    row.addEventListener("click", (e) => {
      if (
        e.target.closest(".link-options-btn") ||
        e.target.closest(".link-context-menu") ||
        e.target.closest(".file-options-btn") ||
        e.target.closest(".file-context-menu") ||
        e.target.closest(".file-open-btn")
      )
        return;
      if (e.target.tagName === "A") return; // Don't trigger if clicking the link itself
      data.isLink
        ? window.open(data.fileUrl, "_blank")
        : openPreviewModal(data);
    });
  });

  // Link options menu (3 dots)
  document.querySelectorAll(".link-options-btn").forEach((btn) => {
    btn.addEventListener("click", (e) => {
      e.stopPropagation();
      // Close other menus
      document.querySelectorAll(".link-context-menu").forEach((menu) => {
        if (menu !== btn.nextElementSibling) {
          menu.classList.add("hidden");
        }
      });
      // Toggle current menu
      const menu = btn.nextElementSibling;
      if (menu) {
        menu.classList.toggle("hidden");
      }
    });
  });

  // Comment button - scroll to comment input and insert link
  document.querySelectorAll(".link-comment-btn").forEach((btn) => {
    btn.addEventListener("click", (e) => {
      e.stopPropagation();
      const menu = btn.closest(".link-context-menu");
      if (menu) menu.classList.add("hidden");

      const fileUrl = decodeURIComponent(btn.dataset.url || "");
      const commentInput = document.getElementById("comment-input");
      const linkRow = btn.closest(".link-row");
      const linkData = linkRow
        ? JSON.parse(decodeURIComponent(linkRow.dataset.attachment || "{}"))
        : {};
      const hasLogo = linkData.hasLogo || false;
      const websiteIcon = linkData.websiteIcon || null;
      const linkText = linkRow?.querySelector("a")?.textContent || fileUrl;

      if (commentInput) {
        // Scroll to comment section
        commentInput.scrollIntoView({ behavior: "smooth", block: "center" });
        commentInput.focus();

        const currentValue = commentInput.value;
        const cursorPos = commentInput.selectionStart || currentValue.length;
        const before = currentValue.slice(0, cursorPos);
        const after = currentValue.slice(cursorPos);

        let insertText = "";

        if (hasLogo && websiteIcon) {
          // Insert as formatted link card (like image 3) - use special format
          // Format: [LINK_CARD:url:icon:title]
          const encodedUrl = encodeURIComponent(fileUrl);
          const encodedIcon = encodeURIComponent(websiteIcon);
          const encodedTitle = encodeURIComponent(linkText);
          insertText = `[LINK_CARD:${encodedUrl}:${encodedIcon}:${encodedTitle}]`;
        } else {
          // Insert as simple blue URL (like image 2)
          insertText = fileUrl;
        }

        commentInput.value =
          before +
          (before && !before.endsWith(" ") && !before.endsWith("\n")
            ? " "
            : "") +
          insertText +
          (after && !after.startsWith(" ") && !after.startsWith("\n")
            ? " "
            : "") +
          after;

        const newCursorPos =
          cursorPos +
          insertText.length +
          (before && !before.endsWith(" ") && !before.endsWith("\n") ? 1 : 0) +
          (after && !after.startsWith(" ") && !after.startsWith("\n") ? 1 : 0);
        commentInput.setSelectionRange(newCursorPos, newCursorPos);

        // Trigger input event to update UI
        commentInput.dispatchEvent(new Event("input", { bubbles: true }));

        // Show Post button
        const postBtn = document.getElementById("post-comment-btn");
        if (postBtn) postBtn.classList.remove("hidden");
      }
    });
  });

  // Remove button
  document.querySelectorAll(".link-remove-btn").forEach((btn) => {
    btn.addEventListener("click", (e) => {
      e.stopPropagation();
      const menu = btn.closest(".link-context-menu");
      if (menu) menu.classList.add("hidden");
      deleteAttachment(btn.dataset.id);
    });
  });

  // File action buttons
  document.querySelectorAll(".file-open-btn").forEach((btn) => {
    btn.addEventListener("click", (e) => {
      e.stopPropagation();
      const row = btn.closest(".attachment-row");
      if (!row) return;
      const data = JSON.parse(
        decodeURIComponent(row.dataset.attachment || "{}")
      );
      openPreviewModal(data);
    });
  });

  document.querySelectorAll(".file-options-btn").forEach((btn) => {
    btn.addEventListener("click", (e) => {
      e.stopPropagation();
      document.querySelectorAll(".file-context-menu").forEach((menu) => {
        if (menu !== btn.nextElementSibling) menu.classList.add("hidden");
      });
      const menu = btn.nextElementSibling;
      if (menu) menu.classList.toggle("hidden");
    });
  });

  document.querySelectorAll(".file-preview-btn").forEach((btn) => {
    btn.addEventListener("click", (e) => {
      e.stopPropagation();
      const row = btn.closest(".attachment-row");
      const menu = btn.closest(".file-context-menu");
      if (menu) menu.classList.add("hidden");
      if (!row) return;
      const data = JSON.parse(
        decodeURIComponent(row.dataset.attachment || "{}")
      );
      openPreviewModal(data);
    });
  });

  document.querySelectorAll(".file-download-btn").forEach((btn) => {
    btn.addEventListener("click", async (e) => {
      e.stopPropagation();
      const row = btn.closest(".attachment-row");
      const menu = btn.closest(".file-context-menu");
      if (menu) menu.classList.add("hidden");
      if (!row) return;
      const data = JSON.parse(
        decodeURIComponent(row.dataset.attachment || "{}")
      );
      if (!data?.fileUrl) return;
      await downloadAttachmentFile(data.fileUrl, data.fileName || "download");
    });
  });

  document.querySelectorAll(".file-delete-btn").forEach((btn) => {
    btn.addEventListener("click", async (e) => {
      e.stopPropagation();
      const menu = btn.closest(".file-context-menu");
      if (menu) menu.classList.add("hidden");
      const success = await deleteAttachment(btn.dataset.id);
      if (success) previewModal.classList.add("hidden");
    });
  });

  // Close menu when clicking outside
  document.addEventListener("click", (e) => {
    if (
      !e.target.closest(".link-options-btn") &&
      !e.target.closest(".link-context-menu") &&
      !e.target.closest(".file-options-btn") &&
      !e.target.closest(".file-context-menu")
    ) {
      document.querySelectorAll(".link-context-menu").forEach((menu) => {
        menu.classList.add("hidden");
      });
      document.querySelectorAll(".file-context-menu").forEach((menu) => {
        menu.classList.add("hidden");
      });
    }
  });
}

// ================== DELETE ==================
async function deleteAttachment(id) {
  try {
    const token = getToken();
    const headers = {};
    if (token) {
      headers.Authorization = "Bearer " + token;
    }
    const res = await fetch(
      `/api/tasks/${window.CURRENT_TASK_ID}/attachments/${id}`,
      {
        method: "DELETE",
        headers,
        credentials: "include",
      }
    );
    if (!res.ok) throw new Error("Delete failed");
    await loadAttachments(window.CURRENT_TASK_ID);

    // Refresh activity feed to show new activity
    showActivitySectionIfHidden();
    await refreshActivityFeedOnly(window.CURRENT_TASK_ID);

    // Update attachment count on card
    try {
      const token = getToken();
      const headers = {};
      if (token) {
        headers.Authorization = "Bearer " + token;
      }
      const res = await fetch(
        `/api/tasks/${window.CURRENT_TASK_ID}/attachments`,
        {
          headers,
          credentials: "include",
        }
      );
      if (res.ok) {
        const attachments = await res.json();
        updateCardAttachmentCount(
          window.CURRENT_TASK_ID,
          attachments ? attachments.length : 0
        );
      }
    } catch (err) {
      console.error("Failed to update attachment count:", err);
    }

    return true;
  } catch (err) {
    console.error(" deleteAttachment error:", err);
    showToast("Failed to delete attachment", "error");
    return false;
  }
}

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
    const token = getToken();
    const headers = {};
    if (token) {
      headers.Authorization = "Bearer " + token;
    }

    if (mime.startsWith("image/")) {
      const blob = await fetch(url, { headers, credentials: "include" }).then(
        (r) => r.blob()
      );
      const blobUrl = URL.createObjectURL(blob);
      previewContent.innerHTML = `
        <div class="relative flex flex-col items-center justify-center">
          <img src="${blobUrl}" class="max-h-[80vh] object-contain rounded-md shadow" />
          ${buildFooter(attachment)}
        </div>`;
    } else if (mime === "application/pdf") {
      const blob = await fetch(url, { headers, credentials: "include" }).then(
        (r) => r.blob()
      );
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
      const text = await fetch(url, { headers, credentials: "include" }).then(
        (r) => r.text()
      );
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
  const allowDelete = canCurrentUserDelete(attachment);
  const deleteButtonHtml = allowDelete
    ? `<button class="hover:text-red-400 flex items-center gap-1" id="delete-btn">🗑️ Delete</button>`
    : "";

  return `
    <div class="absolute bottom-0 left-0 right-0 bg-black/70 text-gray-200 px-4 py-3 text-sm flex flex-col items-center">
      <p class="font-semibold text-white mb-1">${fileName}</p>
      <p class="text-xs text-gray-300 mb-2">Added ${uploadedAt} • ${fileSizeKB}</p>
      <div class="flex gap-4">
        <button class="hover:text-blue-400 flex items-center gap-1" id="open-tab-btn">🔗 Open in new tab</button>
        <button class="hover:text-blue-400 flex items-center gap-1" id="download-btn">⬇️ Download</button>
        ${deleteButtonHtml}
      </div>
    </div>`;
}

function attachFooterEvents(attachment = {}) {
  const fileUrl = attachment.fileUrl || "#";
  const fileName = attachment.fileName || "Untitled";
  const canDelete = canCurrentUserDelete(attachment);

  document
    .getElementById("open-tab-btn")
    ?.addEventListener("click", () => window.open(fileUrl, "_blank"));
  document
    .getElementById("download-btn")
    ?.addEventListener("click", async () => {
      await downloadAttachmentFile(fileUrl, fileName);
    });

  const deleteBtn = document.getElementById("delete-btn");
  if (deleteBtn) {
    if (!canDelete) {
      deleteBtn.classList.add("hidden");
    } else {
      deleteBtn.addEventListener("click", async () => {
        const success = await deleteAttachment(attachment.attachmentId);
        if (!success) return;
        previewModal.classList.add("hidden");
      });
    }
  }
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
    const token = getToken();
    const headers = {};
    if (token) {
      headers.Authorization = "Bearer " + token;
    }
    const res = await fetch(`/api/attachments/recent-links`, {
      headers,
      credentials: "include",
    });
    if (!res.ok) throw new Error("Failed to fetch recent links");
    const items = await res.json();

    if (!Array.isArray(items) || items.length === 0) {
      container.innerHTML = `<p class="text-gray-400 italic text-sm">No recent links</p>`;
      return;
    }

    // Filter only links (not folders or files)
    const links = items.filter(
      (item) =>
        item.mimeType === "link/url" || /^https?:\/\//.test(item.fileUrl || "")
    );

    if (links.length === 0) {
      container.innerHTML = `<p class="text-gray-400 italic text-sm">No recent links</p>`;
      return;
    }

    container.innerHTML = "";
    links.slice(0, 5).forEach((link) => {
      const fileUrl =
        link.fileUrl?.startsWith("/") && !link.fileUrl.startsWith("/http")
          ? `${window.location.origin}${link.fileUrl}`
          : link.fileUrl;

      // Get website icon
      const websiteIcon = getWebsiteIcon(fileUrl);
      const displayName = link.fileName || fileUrl || "Untitled Link";

      const itemDiv = document.createElement("div");
      itemDiv.className =
        "flex items-center gap-2 border border-gray-200 rounded-md p-2 bg-white hover:bg-gray-50 transition cursor-pointer";
      itemDiv.addEventListener("click", () => {
        linkInput.value = fileUrl;
        displayTextInput.value = link.fileName || "";
      });

      // Icon - simple icon without wrapper
      let iconHtml = "";
      if (websiteIcon) {
        iconHtml = websiteIcon.content;
      } else {
        iconHtml = `<svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-gray-500 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
        </svg>`;
      }

      // Text content - blue link style
      itemDiv.innerHTML = `
        ${iconHtml}
        <p class="text-sm font-normal text-blue-600 hover:underline truncate flex-1 min-w-0">
          ${escapeHtml(displayName)}
        </p>
      `;
      container.appendChild(itemDiv);
    });
  } catch (err) {
    container.innerHTML = `<p class="text-red-500 text-sm">Failed to load recent links</p>`;
  }
}

// ================== INSERT FILE / LINK ==================
async function handlePopupInsert() {
  const file = popupFileInput.files[0];
  const link = linkInput.value.trim();
  const displayText = displayTextInput.value.trim();

  if (!file && !link) {
    showToast(" Please select a file or enter a link!", "error");
    return;
  }

  insertAttachBtn.disabled = true;

  // Show uploading notification if uploading file
  if (file) {
    showUploadingNotification();
  }

  try {
    if (file) {
      const formData = new FormData();
      formData.append("file", file);
      const token = getToken();
      const headers = {};
      if (token) {
        headers.Authorization = "Bearer " + token;
      }
      const res = await fetch(
        `/api/tasks/${window.CURRENT_TASK_ID}/attachments`,
        {
          method: "POST",
          headers,
          body: formData,
          credentials: "include",
        }
      );
      if (!res.ok) {
        let errorMsg = "Upload failed";

        try {
          const text = await res.text();
          const json = JSON.parse(text);
          errorMsg = json.message || json.error || text;
        } catch (e) {
          // fallback
        }

        throw new Error(errorMsg);
      }

      // Hide uploading notification
      hideUploadingNotification();
    } else {
      const token = getToken();
      const headers = {
        "Content-Type": "application/json",
      };
      if (token) {
        headers.Authorization = "Bearer " + token;
      }
      const res = await fetch(
        `/api/tasks/${window.CURRENT_TASK_ID}/attachments/link`,
        {
          method: "POST",
          headers,
          body: JSON.stringify({
            fileUrl: link,
            fileName: displayText || link,
          }),
          credentials: "include",
        }
      );
      if (!res.ok) throw new Error("Attach link failed");
    }

    showToast(" Uploaded successfully!");
    attachPopup.classList.add("hidden");
    await loadAttachments(window.CURRENT_TASK_ID);

    // Refresh activity feed to show new activity
    showActivitySectionIfHidden();
    await refreshActivityFeedOnly(window.CURRENT_TASK_ID);

    // Update attachment count on card
    try {
      const token = getToken();
      const headers = {};
      if (token) {
        headers.Authorization = "Bearer " + token;
      }
      const res = await fetch(
        `/api/tasks/${window.CURRENT_TASK_ID}/attachments`,
        {
          headers,
          credentials: "include",
        }
      );
      if (res.ok) {
        const attachments = await res.json();
        updateCardAttachmentCount(
          window.CURRENT_TASK_ID,
          attachments ? attachments.length : 0
        );
      }
    } catch (err) {
      console.error("Failed to update attachment count:", err);
    }
  } catch (err) {
    // Hide uploading notification if shown
    hideUploadingNotification();

    showToast(err.message, "error");
  } finally {
    popupFileInput.value = "";
    linkInput.value = "";
    displayTextInput.value = "";
    insertAttachBtn.disabled = false;
  }
}
