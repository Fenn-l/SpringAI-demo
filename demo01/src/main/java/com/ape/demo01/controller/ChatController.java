package com.ape.demo01.controller;

import com.ape.demo01.repository.ChatHistoryRepository;
import com.ape.demo01.util.ConversationKeys;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 基础 AI 聊天接口（对应前端「智能助手」功能）。
 *
 * <p>请求示例：
 * <pre>
 *   GET /ai/chat?prompt=讲个笑话&chatId=default        （流式返回）
 *   GET /ai/chat/sync?prompt=你好&chatId=default       （同步一次性返回）
 * </pre>
 */
@RestController
@RequestMapping("/ai")
public class ChatController {

    /** 由 CommonConfiguration 注入的 ChatClient（已带系统提示词 + 记忆 Advisor）。 */
    @Autowired
    private ChatClient chatClient;

    /** 会话ID仓库：把 chatId 按业务类型归档，供前端左侧历史列表展示。 */
    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    /**
     * 流式聊天（逐字返回，前端体验更顺滑）。
     *
     * @param prompt  用户输入的问题，不传则默认「讲个笑话」
     * @param chatId  会话ID，用于区分不同对话、串接历史，不传则默认 "default"
     * @return 流式文本（Flux<String>），Spring MVC 会按 SSE/流的方式逐步写回响应
     */
    @RequestMapping(value = "/chat", produces = "text/html;charset=UTF-8")
    public Flux<String> chat(@RequestParam(defaultValue = "讲个笑话") String prompt,
                             @RequestParam(defaultValue = "default") String chatId) {
        // 1.先把会话ID记到「聊天」业务分组下，方便历史列表按功能分类
        chatHistoryRepository.save("chat", chatId);

        // 2.发起大模型调用；prompt() 即本次的用户输入
        return chatClient
                .prompt(prompt)
                // 3.把「业务类型:chatId」作为记忆 Key 传进记忆 Advisor
                //    （用 ConversationKeys.of 隔离各功能记忆，避免和 game/service/pdf 串号）
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, ConversationKeys.of("chat", chatId)))
                .stream()       // 切换为流式调用：响应随生成进度逐段返回
                .content();     // 只取文本内容（忽略元数据），得到 Flux<String>
    }

    /**
     * 同步聊天（对照用）：等待模型把整段回答生成完才一次性返回。
     * 用来验证「会话记忆」在流式/同步两种调用方式下行为是否一致。
     */
    @RequestMapping(value = "/chat/sync", produces = "text/html;charset=UTF-8")
    public String chatSync(@RequestParam(defaultValue = "讲个笑话") String prompt,
                           @RequestParam(defaultValue = "default") String chatId) {
        // 同样先记录会话ID
        chatHistoryRepository.save("chat", chatId);
        // 调用方式换成 .call()（同步）而非 .stream()（流式）；其余完全一致
        return chatClient
                .prompt(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, ConversationKeys.of("chat", chatId)))
                .call()         // 同步请求：阻塞直到模型输出完整结果
                .content();     // 取出完整文本字符串
    }
}
