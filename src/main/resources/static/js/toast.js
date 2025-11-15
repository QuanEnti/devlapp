// Toast Notification Utility
// Simple toast notification to replace alert()

function showToast(message, type = "info") {
  // Remove emoji from message for cleaner display
  const cleanMessage = message.replace(/[✅❌⚠️🚫🔒]/g, "").trim();

  const toast = document.createElement("div");
  toast.textContent = cleanMessage || message;

  // Determine background color based on type
  let bgClass = "bg-blue-600";
  if (
    type === "error" ||
    message.includes("❌") ||
    message.includes("Failed") ||
    message.includes("Error")
  ) {
    bgClass = "bg-red-600";
  } else if (type === "warning" || message.includes("⚠️")) {
    bgClass = "bg-amber-500";
  } else if (
    type === "success" ||
    message.includes("✅") ||
    message.includes("successfully")
  ) {
    bgClass = "bg-emerald-600";
  } else if (type === "info") {
    bgClass = "bg-blue-600";
  }

  toast.className = `fixed bottom-4 right-4 px-4 py-3 rounded-lg text-white text-sm font-medium shadow-xl z-[9999]
      pointer-events-none transition-all duration-200 ease-out translate-y-4 opacity-0 ${bgClass}`;

  document.body.appendChild(toast);

  // Animate in
  requestAnimationFrame(() => {
    toast.classList.remove("translate-y-4", "opacity-0");
  });

  // Auto remove after 3 seconds
  setTimeout(() => {
    toast.classList.add("opacity-0", "translate-y-4");
    setTimeout(() => toast.remove(), 250);
  }, 3000);
}
