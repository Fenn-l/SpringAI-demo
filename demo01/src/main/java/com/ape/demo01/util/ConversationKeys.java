package com.ape.demo01.util;

/**
 * 会话记忆键（Conversation Key）工具类。
 *
 * <p>背景：本项目四个功能（chat / service / game / pdf）在配置类里共用<b>同一个</b>
 * {@link org.springframework.ai.chat.memory.ChatMemory} Bean。如果它们都用同一个
 * chatId（比如默认值 {@code "default"}），各功能的对话历史就会串到一起。
 * 解决办法：把「业务类型 + chatId」拼成一个全局唯一的 Key，从物理上隔离各功能的记忆。
 *
 * <p>例如：{@code chat:default}、{@code pdf:abc123}、{@code game:default}。
 *
 * <p>同时提供 {@link #sanitize(String)} 用来清洗用户输入的 chatId，只允许字母、数字、
 * 下划线和连字符，避免把脏字符带进记忆 Key 或向量检索的过滤表达式（防止注入）。
 */
public final class ConversationKeys {

    /** 私有构造，工具类不允许实例化。 */
    private ConversationKeys() {
    }

    /**
     * 生成隔离后的记忆 Key。
     *
     * @param type   业务类型，如 chat / service / game / pdf
     * @param chatId 会话ID（会被 {@link #sanitize} 清洗）
     * @return 形如 {@code "chat:default"} 的唯一 Key
     */
    public static String of(String type, String chatId) {
        return type + ":" + sanitize(chatId);
    }

    /**
     * 清洗字符串：仅保留 {@code [A-Za-z0-9_-]}，其余字符全部替换为下划线。
     * 空值或清洗后为空时回退为 {@code "default"}，保证永不返回 null/空串。
     *
     * @param s 原始字符串（通常来自前端传入的 chatId）
     * @return 安全可用的字符串
     */
    public static String sanitize(String s) {
        if (s == null || s.isBlank()) {
            return "default";
        }
        // 用正则把非白名单字符替换为下划线，杜绝路径穿越字符(../)与单引号等
        String cleaned = s.replaceAll("[^A-Za-z0-9_-]", "_");
        return cleaned.isEmpty() ? "default" : cleaned;
    }
}
