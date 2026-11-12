package com.ape.demo01.config;

import com.ape.demo01.constants.SystemConstants;
import com.ape.demo01.constants.SystemConstants2;
import com.ape.demo01.tools.CourseTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.ape.demo01.constants.SystemConstants2.SERVICE_SYSTEM_PROMPT;

/**
 * 通用 AI 配置：定义「会话记忆」和多个专用 ChatClient Bean。
 *
 * <p>本项目有 4 个功能，各自需要不同的系统提示词 / 模型 / 工具，所以这里装配了
 * 4 个 ChatClient：chatClient（本地 Ollama）、gameChatClient、serviceChatClient（均用 OpenAI 兼容接口）、
 * pdfChatClient 在另一个配置类里。它们共用同一个 ChatMemory，靠「type:chatId」Key 隔离。
 */
@Configuration
public class CommonConfiguration {

    /**
     * 会话记忆容器（课件 1.4.1）。
     *
     * <p>1.0.0 GA 把老版 InMemoryChatMemory 拆成两层：
     * <ul>
     *   <li>InMemoryChatMemoryRepository —— 真正存 List&lt;Message&gt;</li>
     *   <li>MessageWindowChatMemory      —— 在 Repository 之上加「滑动窗口」，只保留最近 N 条</li>
     * </ul>
     * 这里设窗口为 20 条，超出的旧消息会被丢弃，防止上下文无限膨胀。
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(20) // 每个会话最多保留最近 20 条消息
                .build();        // 不指定 Repository 时默认用内存实现
    }

    /** 基础聊天 ChatClient：使用本地 Ollama（deepseek-r1:8b），带「小猿」人设与记忆。 */
    @Bean
    public ChatClient chatClient(OllamaChatModel model, ChatMemory chatMemory) {
        return ChatClient.builder(model) // 以 Ollama 模型为底层构建 ChatClient 工厂
                .defaultSystem("你是一个热心、可爱的智能助手，你的名字叫小猿，" +
                        "请以小猿的身份和语气回答问题。") // 默认系统提示词（人设）
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(), // 把每次请求/响应打日志，便于调试
                        // 记忆 Advisor：自动把历史对话拼进下一次请求
                        // 1.0.0 GA 里 MessageChatMemoryAdvisor 构造器私有，必须走 builder
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build(); // 构建出 ChatClient 实例
    }

    /** 游戏 ChatClient：用 OpenAI 兼容接口（阿里云 DashScope），系统提示词是「女友」人设。 */
    @Bean
    public ChatClient gameChatClient(OpenAiChatModel model, ChatMemory chatMemory) {
        return ChatClient
                .builder(model)
                .defaultSystem(SystemConstants.GAME_SYSTEM_PROMPT) // 角色扮演提示词（见 SystemConstants）
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

    /** 客服 ChatClient：用 OpenAI 兼容接口，并挂载课程查询 / 预约工具（Function Calling）。 */
    @Bean
    public ChatClient serviceChatClient(
            OpenAiChatModel model,
            ChatMemory chatMemory,
            CourseTools courseTools) {
        return ChatClient.builder(model)
                .defaultSystem(SystemConstants2.SERVICE_SYSTEM_PROMPT) // 客服人设 + 防注入规则
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .defaultTools(courseTools) // 注册工具，模型可自主决定调用查询课程/校区/生成预约单
                .build();
    }
}
