// ✅ static/js/chatbox.js
document.addEventListener("DOMContentLoaded", function () {
  const chatBtn = document.getElementById("chatbox-button");
  const chatBox = document.getElementById("chatbox-container");
  const closeBtn = document.getElementById("chatbox-close");
  const body = document.getElementById("chatbox-body");
  const input = document.getElementById("chatbox-input");
  const sendBtn = document.getElementById("chatbox-send");

  if (!chatBtn || !chatBox) return;

  // 🟢 Mở chatbox
  chatBtn.addEventListener("click", () => {
    chatBox.classList.remove("hidden");
    chatBtn.classList.add("hidden");
    input.focus();
  });

  // 🔴 Đóng chatbox
  closeBtn.addEventListener("click", () => {
    chatBox.classList.add("hidden");
    chatBtn.classList.remove("hidden");
  });

  // 💬 Hàm hiển thị tin nhắn
  function appendMessage(text, sender = "user") {
    const msg = document.createElement("div");
    msg.className =
      sender === "user"
        ? "text-right text-sm text-blue-700"
        : "text-left text-sm text-gray-800 bg-gray-100 p-2 rounded-lg my-1";
    msg.textContent = text;
    body.appendChild(msg);
    body.scrollTop = body.scrollHeight;
  }

  // 🔁 Luồng tạo project
  let projectFlow = { step: 0, name: "", description: "", priority: "MEDIUM" };

  // 🚀 Xử lý từng bước tạo project
  async function handleProjectFlow(message) {
    switch (projectFlow.step) {
      case 1:
        projectFlow.name = message;
        projectFlow.step = 2;
        appendMessage("💡 Hãy nhập mô tả cho dự án:", "bot");
        break;
      case 2:
        projectFlow.description = message;
        projectFlow.step = 3;
        appendMessage("🔥 Mức độ ưu tiên (LOW / MEDIUM / HIGH):", "bot");
        break;
      case 3:
        projectFlow.priority =
          ["LOW", "MEDIUM", "HIGH"].includes(message.toUpperCase())
            ? message.toUpperCase()
            : "MEDIUM";
        appendMessage("🚀 Đang tạo dự án...", "bot");

        const requestBody = {
          name: projectFlow.name,
          description: projectFlow.description,
          priority: projectFlow.priority,
          startDate: new Date().toISOString().split("T")[0],
          endDate: null,
        };

        try {
          const res = await fetch("/api/projects/create", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(requestBody),
          });
          const data = await res.json();
          if (data.success) {
            appendMessage(`✅ Dự án "${data.data.name}" đã được tạo thành công!`, "bot");
          } else {
            appendMessage(`⚠️ ${data.message}`, "bot");
          }
        } catch (err) {
          appendMessage("🚨 Lỗi khi tạo dự án, vui lòng thử lại.", "bot");
          console.error(err);
        }

        projectFlow = { step: 0, name: "", description: "", priority: "MEDIUM" }; // reset
        break;
    }
  }

  // 💬 Gửi tin nhắn
  sendBtn.addEventListener("click", async () => {
    const message = input.value.trim();
    if (!message) return;

    appendMessage(message, "user");
    input.value = "";

    if (projectFlow.step > 0) {
      await handleProjectFlow(message);
      return;
    }

    if (message.toLowerCase().includes("tạo project") || message.toLowerCase().includes("tạo dự án")) {
      projectFlow.step = 1;
      appendMessage("📝 Vui lòng nhập tên dự án:", "bot");
      return;
    }

    // 🎯 Nếu không thuộc flow → Gửi sang AI Backend
    try {
      const res = await fetch("/api/chat/send", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ message }),
      });
      const data = await res.json();
      appendMessage(data.reply || "🤖 Mình chưa hiểu ý bạn.", "bot");
    } catch (err) {
      appendMessage("⚠️ Lỗi khi kết nối AI!", "bot");
    }
  });
});
