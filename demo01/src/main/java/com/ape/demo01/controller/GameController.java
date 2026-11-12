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
 * 哄女友小游戏接口（角色扮演：用户哄生气的女友，原谅值 0~100）。
 *
 * <p>请求示例：GET /ai/game?prompt=女朋友问我闺蜜谁好看我说都好看，她生气了&chatId=default
 */
@RestController
@RequestMapping("/ai")
public class GameController {

    /** 由 CommonConfiguration 注入的游戏专用 ChatClient（系统提示词=女友人设）。 */
    @Autowired
    private ChatClient gameChatClient;

    /** 会话ID仓库（让游戏对话也能出现在左侧历史列表里）。 */
    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    /**
     * 游戏对话接口（流式返回）。
     *
     * @param prompt  用户输入（如哄人的话 / 女友生气的理由），不传则为空串
     * @param chatId  会话ID，不传默认 "default"
     * @return 流式文本
     */
    @RequestMapping(value = "/game", produces = "text/html;charset=utf-8")
    public Flux<String> chat(@RequestParam(required = false, defaultValue = "") String prompt,
                             @RequestParam(required = false, defaultValue = "default") String chatId) {
        // 记录会话ID到「game」业务分组，保证历史列表能看到游戏会话
        chatHistoryRepository.save("game", chatId);
        // 流式调用；记忆 Key 隔离为 game:chatId
        return gameChatClient.prompt()
                .user(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, ConversationKeys.of("game", chatId)))
                .stream()
                .content();
    }
}
