// ✅ comments.js – Quản lý bình luận và nhật ký hoạt động
import { apiFetch, escapeHtml, showToast, formatTime } from "./utils.js";
import { currentTask } from "./modal-task.js";

let comments = [];
let activityFeed = [];

/* ---------------------- 💬 LOAD COMMENTS ---------------------- */
export async function loadComments(taskId) {
  try {
    const res = await apiFetch(`/api/tasks/${taskId}/comments`);
    comments = await res.json();
    renderComments(comments);
  } catch (err) {
    console.error("❌ loadComments failed:", err);
    showToast("Không thể tải bình luận.", "error");
  }
}

/* ---------------------- 🧾 LOAD ACTIVITY FEED ---------------------- */
export async function loadActivityFeed(taskId) {
  try {
    const res = await apiFetch(`/api/tasks/${taskId}/activity`);
    activityFeed = await res.json();
    renderActivityFeed(activityFeed);
  } catch (err) {
    console.error("❌ loadActivityFeed failed:", err);
    showToast("Không thể tải lịch sử hoạt động.", "error");
  }
}

/* ---------------------- 🖋️ RENDER COMMENTS ---------------------- */
export function renderComments(list) {
  const container = document.getElementById("comments-list");
  if (!container) return;

  if (!list || !list.length) {
    container.innerHTML = `<p class="text-gray-400 italic">No comments yet.</p>`;
    return;
  }

  container.innerHTML = "";
  list.forEach(c => {
    const div = document.createElement("div");
    div.className = "border-b pb-2";
    div.innerHTML = `
      <div class="flex justify-between items-center">
        <b class="text-gray-800">${escapeHtml(c.authorName || "Unknown")}</b>
        <span class="text-xs text-gray-400">${escapeHtml(formatTime(c.createdAt || ""))}</span>
      </div>
      <p class="text-sm text-gray-700 mt-1">${escapeHtml(c.content || "")}</p>
      <button data-reply="${c.commentId}"
              class="text-xs text-blue-600 hover:underline mt-1">Reply</button>
      <div id="replies-${c.commentId}" class="ml-5 mt-1 space-y-1">
        ${renderRepliesHtml(c.replies || [])}
      </div>
    `;
    container.appendChild(div);
  });

  // Gắn listener Reply
  container.querySelectorAll("[data-reply]").forEach(btn => {
    btn.addEventListener("click", e => openReplyBox(e.target.dataset.reply));
  });
}

function renderRepliesHtml(replies) {
  if (!replies?.length) return "";
  return replies.map(r => `
    <div class="border-l-2 border-gray-200 pl-2">
      <p class="text-sm"><b>${escapeHtml(r.authorName)}</b>: ${escapeHtml(r.content)}</p>
      <p class="text-xs text-gray-400 ml-1">${escapeHtml(formatTime(r.createdAt || ""))}</p>
    </div>
  `).join("");
}

/* ---------------------- 📜 RENDER ACTIVITY FEED ---------------------- */
export function renderActivityFeed(list) {
  const container = document.getElementById("activity-feed");
  if (!container) return;

  if (!list || !list.length) {
    container.innerHTML = `<p class="text-gray-400 italic">No activity yet.</p>`;
    return;
  }

  container.innerHTML = "";
  list.forEach(a => {
    const div = document.createElement("div");
    div.className = "border-b pb-1";
    div.innerHTML = `
      <p><b>${escapeHtml(a.actorName || "Someone")}</b> ${escapeHtml(a.action || "")}</p>
      <p class="text-xs italic text-gray-500">${escapeHtml(formatTime(a.timestamp || ""))}</p>
    `;
    container.appendChild(div);
  });
}

/* ---------------------- ✏️ POST COMMENT ---------------------- */
const input = document.getElementById("comment-input");
const postBtn = document.getElementById("post-comment-btn");

if (input && postBtn) {
  input.addEventListener("input", () => {
    postBtn.classList.toggle("hidden", !input.value.trim());
  });

  postBtn.addEventListener("click", postComment);
}

export async function postComment() {
  if (!currentTask) return;
  const content = input.value.trim();
  if (!content) return;

  try {
    const res = await apiFetch(`/api/tasks/${currentTask.taskId}/comments`, {
      method: "POST",
      body: JSON.stringify({ content })
    });
    if (!res.ok) throw new Error();
    const newComment = await res.json();

    comments.push(newComment);
    renderComments(comments);
    input.value = "";
    postBtn.classList.add("hidden");
    showToast("💬 Đã đăng bình luận!");
  } catch (err) {
    console.error("❌ postComment failed:", err);
    showToast("Không thể đăng bình luận.", "error");
  }
}

/* ---------------------- 💬 POST REPLY ---------------------- */
async function postReply(parentId, content) {
  if (!currentTask || !content.trim()) return;
  try {
    const res = await apiFetch(`/api/comments/${parentId}/replies`, {
      method: "POST",
      body: JSON.stringify({ content })
    });
    if (!res.ok) throw new Error();
    const newReply = await res.json();

    // Gắn vào UI
    const parent = comments.find(c => c.commentId == parentId);
    if (parent) {
      parent.replies = parent.replies || [];
      parent.replies.push(newReply);
      renderComments(comments);
    }
    showToast("↩️ Đã trả lời bình luận.");
  } catch (err) {
    console.error("❌ postReply failed:", err);
    showToast("Không thể trả lời bình luận.", "error");
  }
}

/* ---------------------- 💭 MỞ HỘP REPLY ---------------------- */
function openReplyBox(parentId) {
  const container = document.getElementById(`replies-${parentId}`);
  if (!container) return;

  // Nếu đã có box reply rồi thì không thêm nữa
  if (container.querySelector(".reply-box")) return;

  const box = document.createElement("div");
  box.className = "reply-box mt-1";
  box.innerHTML = `
    <textarea class="w-full border border-gray-300 rounded-md p-1 text-xs resize-none"
              rows="2" placeholder="Write a reply..."></textarea>
    <div class="flex justify-end gap-1 mt-1">
      <button class="text-xs text-gray-500 hover:underline cancel">Cancel</button>
      <button class="text-xs text-blue-600 hover:underline send">Send</button>
    </div>
  `;
  container.appendChild(box);

  const textarea = box.querySelector("textarea");
  const cancel = box.querySelector(".cancel");
  const send = box.querySelector(".send");

  cancel.addEventListener("click", () => box.remove());
  send.addEventListener("click", async () => {
    const content = textarea.value.trim();
    if (!content) return;
    await postReply(parentId, content);
    box.remove();
  });
}
