document.addEventListener("DOMContentLoaded", function() {
    const logoutBtn = document.getElementById("logoutBtn");
    if (logoutBtn) {
        logoutBtn.addEventListener("click", (e) => {
            e.preventDefault();
            fetch("/api/auth/logout", { method: "POST", credentials: "include" })
                .then(() => {
                    document.cookie = "AUTH_TOKEN=; Max-Age=0; path=/;";
                    document.cookie = "REFRESH_TOKEN=; Max-Age=0; path=/;";
                    localStorage.removeItem("AUTH_TOKEN");
                    localStorage.removeItem("REFRESH_TOKEN");
                    window.location.href = "/view/signin";
                })
                .catch((err) => console.error("Logout error:", err));
        });
    }
});
