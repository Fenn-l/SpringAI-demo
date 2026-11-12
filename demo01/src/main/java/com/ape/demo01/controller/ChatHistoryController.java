package com.ape.demo01.controller;

import com.ape.demo01.entity.vo.MessageVO;
import com.ape.demo01.repository.ChatHistoryRepository;
import com.ape.demo01.util.ConversationKeys;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会话历史查询接口（对应前端左侧「聊天记录」列表）。
 *
 * <p>两个端点：
 * <ol>
 *   <li>GET /ai/history/{type}            —— 查某类业务下所有会话ID（如 chat / service / pdf / game）</li>
 *   <li>GET /ai/history/{type}/{chatId}   —— 查某个会话里的全部消息</li>
 * </ol>
 */
@RestController
@RequestMapping("/ai/history")
public class ChatHistoryController {

    /** 保存了各业务类型下的 chatId 列表。 */
    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    /** 保存了所有聊天内容（由 ChatMemory Advisor 写入）。 */
    @Autowired
    private ChatMemory chatMemory;

    /** 查询某业务类型下的全部会话ID。 */
    @GetMapping("/{type}")
    public List<String> getChatIds(@PathVariable("type") String type) {
        return chatHistoryRepository.getChatIds(type);
    }

    /**
     * 查询某会话的历史消息。
     *
     * @param type   业务类型（决定记忆 Key 的命名空间前缀）
     * @param chatId 会话ID
     * @return 该会话下的消息列表（转成前端友好的 MessageVO）
     */
    @GetMapping("/{type}/{chatId}")
    public List<MessageVO> getChatHistory(@PathVariable("type") String type,
                                          @PathVariable("chatId") String chatId) {
        // 【与课件差异】1.0.0 GA 的 ChatMemory#get 只接收 conversationId；
        // 这里必须用「type:chatId」取回与各 Controller 写入时完全一致的记忆 Key
        List<Message> messages = chatMemory.get(ConversationKeys.of(type, chatId));
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        // 把 Spring AI 的 Message 转成只含 role+content 的 VO，避免向前端暴露内部字段
        return messages.stream().map(MessageVO::new).toList();
    }
}
