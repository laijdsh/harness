/**
 * 首页健康状态刷新脚本。
 *
 * 页面通过 fetch 调用后端真实接口 /api/health，
 * 把应用状态和数据库状态显示在页面上；不是写死的静态结果。
 */
(function () {
    "use strict";

    const systemStatus = document.getElementById("system-status");
    const databaseStatus = document.getElementById("database-status");
    const systemMessage = document.getElementById("system-message");
    const lastChecked = document.getElementById("last-checked");
    const refreshButton = document.getElementById("refresh-button");

    /**
     * 更新状态标签的文字和颜色。
     *
     * @param {HTMLElement} element 状态标签
     * @param {string} text 显示文字
     * @param {string} state up / down / loading
     */
    function renderStatus(element, text, state) {
        element.textContent = text;
        element.classList.remove("up", "down", "loading");
        element.classList.add(state);
    }

    /**
     * 把后端返回的 ISO 时间转换成方便阅读的本地时间。
     *
     * @param {string} value 后端返回的时间字符串
     * @returns {string} 格式化后的时间
     */
    function formatTime(value) {
        if (!value) {
            return "--";
        }

        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return value;
        }

        return date.toLocaleString("zh-CN", { hour12: false });
    }

    /**
     * 请求健康检查接口并更新页面。
     */
    async function refreshHealth() {
        refreshButton.disabled = true;
        renderStatus(systemStatus, "检查中", "loading");
        renderStatus(databaseStatus, "检查中", "loading");

        try {
            const response = await fetch("/api/health", {
                method: "GET",
                headers: { "Accept": "application/json" },
                cache: "no-store"
            });

            // 健康检查在数据库异常时可能返回 503，但响应体仍然是 JSON。
            const data = await response.json();
            const applicationUp = data.status === "UP";
            const databaseUp = data.database === "UP";

            renderStatus(systemStatus, applicationUp ? "正常运行" : "异常", applicationUp ? "up" : "down");
            renderStatus(databaseStatus, databaseUp ? "已连接" : "不可用", databaseUp ? "up" : "down");

            systemMessage.textContent = applicationUp
                ? "应用与数据库连接正常。"
                : "应用能够响应请求，但数据库连接异常。";
            lastChecked.textContent = formatTime(data.timestamp);
        } catch (error) {
            renderStatus(systemStatus, "无法连接", "down");
            renderStatus(databaseStatus, "未知", "down");
            systemMessage.textContent = "无法访问 /api/health，请检查服务是否启动。";
            lastChecked.textContent = "--";
            console.error("健康检查请求失败", error);
        } finally {
            refreshButton.disabled = false;
        }
    }

    refreshButton.addEventListener("click", refreshHealth);

    // 首次加载立即检查一次，之后每 30 秒自动刷新。
    refreshHealth();
    window.setInterval(refreshHealth, 30000);
})();
