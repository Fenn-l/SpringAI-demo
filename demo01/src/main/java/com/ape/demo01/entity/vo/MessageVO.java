package com.ape.demo01.entity.vo;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;

/**
 * 面向前端的会话消息 VO（课件 1.5.3）。
 *
 * <p>Spring AI 的 {@link Message} 是面向大模型的抽象（带 metadata、type 等），
 * 直接返回给前端既冗余又不安全，所以转成只含 role + content 的轻量对象。
 */
@NoArgsConstructor
@Data
public class MessageVO {

    /** 消息角色：user / assistant / system / tool。 */
    private String role;

    /** 消息正文。 */
    private String content;

    /** 从 Spring AI 的 Message 构造 VO。 */
    public MessageVO(Message message) {
        // 把枚举类型映射成前端可读的角色字符串
        this.role = switch (message.getMessageType()) {
            case USER -> "user";
            case ASSISTANT -> "assistant";
            case SYSTEM -> "system";
            case TOOL -> "tool";
            // 【健壮性修复】枚举未来可能新增取值，没有 default 会抛异常；
            // 兜底成 "user" 保证不崩溃
            default -> "user";
        };
        // 取消息文本正文
        this.content = message.getText();
    }
}
