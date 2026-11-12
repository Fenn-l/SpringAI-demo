package com.ape.demo01.controller;

import com.ape.demo01.repository.ChatHistoryRepository;
import com.ape.demo01.util.ConversationKeys;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 智能客服接口（「猿究院」职业教育客服，带 Function Calling 工具调用）。
 *
 * <p>由于本功能启用了工具调用（@Tool），且部分前端对 SSE 流式兼容性差，
 * 所以这里固定用<b>同步</b>返回（而非 Flux 流式）。
 *
 * <p>请求示例：GET /ai/service?prompt=我想学Java&chatId=default
 */
@RestController
@RequestMapping("/ai")
public class CustomerServiceController {

    /** 由 CommonConfiguration 注入的客服专用 ChatClient（系统提示词=客服人设 + 课程查询工具）。 */
    @Autowired
    private ChatClient serviceChatClient;

    /** 会话ID仓库。 */
    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    /**
     * 客服对话接口。
     *
     * @param prompt  用户问题（如「有什么编程课」），不传则为空串
     * @param chatId  会话ID，不传默认 "default"
     * @return 模型生成的回复文本
     */
    @RequestMapping(value = "/service", produces = "text/html;charset=utf-8")
    public String service(@RequestParam(required = false, defaultValue = "") String prompt,
                          @RequestParam(required = false, defaultValue = "default") String chatId) {
        // 1.记录会话ID到「service」业务分组
        chatHistoryRepository.save("service", chatId);
        // 2.发起同步调用：.user(prompt) 设置用户输入，.call() 等待完整结果
        return serviceChatClient.prompt()
                .user(prompt)
                // 记忆 Key 隔离为 service:chatId，避免与 chat/game/pdf 串号
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, ConversationKeys.of("service", chatId)))
                .call()
                .content();
    }
}
