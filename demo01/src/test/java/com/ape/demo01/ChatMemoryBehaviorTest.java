package com.ape.demo01;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 会话记忆（ChatMemory + MessageChatMemoryAdvisor）行为验证。
 *
 * <p>用「桩模型」替代 Ollama，不依赖本地大模型，秒级跑完。
 * 覆盖三个关键点：
 * <ol>
 *   <li>同一个 conversationId 的多轮对话，历史会累积；</li>
 *   <li>第 N 轮的 prompt 会自动带上前面 N-1 轮的历史（记忆真正生效）；</li>
 *   <li>不同 conversationId 之间记忆互不干扰（这是 1.4.3 加会话ID 的意义）。</li>
 * </ol>
 */
class ChatMemoryBehaviorTest {

    /** 桩模型：模拟 Ollama 的多 chunk 流式输出，并记录每次收到的 prompt */
    static class StubChatModel implements ChatModel {

        final AtomicInteger callCount = new AtomicInteger();
        final List<List<String>> receivedPrompts = new java.util.ArrayList<>();

        @Override
        public ChatResponse call(Prompt prompt) {
            return build(prompt);
        }

        @Override
        public reactor.core.publisher.Flux<ChatResponse> stream(Prompt prompt) {
            return reactor.core.publisher.Flux.just(build(prompt));
        }

        private ChatResponse build(Prompt prompt) {
            int n = callCount.incrementAndGet();
            receivedPrompts.add(prompt.getInstructions().stream().map(Message::getText).toList());
            return new ChatResponse(List.of(new Generation(new AssistantMessage("回复" + n))));
        }
    }

    private ChatClient client(ChatModel model, ChatMemory memory) {
        return ChatClient.builder(model)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
                .build();
    }

    private String ask(ChatClient client, String prompt, String conversationId) {
        return client.prompt(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content()
                .blockLast();
    }

    @Test
    void 同一会话的多轮对话会累积历史() {
        StubChatModel model = new StubChatModel();
        ChatMemory memory = MessageWindowChatMemory.builder().build();
        ChatClient client = client(model, memory);
        String cid = "conv-1";

        ask(client, "我叫张三", cid);
        ask(client, "我叫什么", cid);

        List<Message> history = memory.get(cid);
        assertEquals(4, history.size(), "两轮对话应产生 4 条记忆");
        assertEquals("我叫张三", history.get(0).getText());
        assertEquals("回复1", history.get(1).getText());
        assertEquals("我叫什么", history.get(2).getText());
        assertEquals("回复2", history.get(3).getText());
    }

    @Test
    void 第二轮的prompt会自动带上第一轮的历史() {
        StubChatModel model = new StubChatModel();
        ChatMemory memory = MessageWindowChatMemory.builder().build();
        ChatClient client = client(model, memory);
        String cid = "conv-2";

        ask(client, "我叫张三", cid);
        ask(client, "我叫什么", cid);

        // 第二轮发给模型的 prompt = [第一轮user, 第一轮assistant, 第二轮user]
        List<String> secondPrompt = model.receivedPrompts.get(1);
        assertEquals(3, secondPrompt.size(), "第二轮 prompt 应包含 3 条消息");
        assertTrue(secondPrompt.contains("我叫张三"), "第二轮 prompt 应带上历史里的用户消息");
        assertTrue(secondPrompt.contains("回复1"), "第二轮 prompt 应带上历史里的助手消息");
        assertEquals(2, model.callCount.get(), "两轮对话只应调用模型 2 次");
    }

    @Test
    void 不同会话的记忆互相隔离() {
        StubChatModel model = new StubChatModel();
        ChatMemory memory = MessageWindowChatMemory.builder().build();
        ChatClient client = client(model, memory);

        ask(client, "A 会话的问题", "conv-A");
        ask(client, "B 会话的问题", "conv-B");

        assertEquals(2, memory.get("conv-A").size());
        assertEquals(2, memory.get("conv-B").size());
        assertEquals("A 会话的问题", memory.get("conv-A").get(0).getText());
        assertEquals("B 会话的问题", memory.get("conv-B").get(0).getText());
        // B 会话的 prompt 不应包含 A 会话的内容
        assertFalse(model.receivedPrompts.get(1).contains("A 会话的问题"), "不同会话之间记忆不应串台");
    }

    private static void assertFalse(boolean condition, String message) {
        org.junit.jupiter.api.Assertions.assertFalse(condition, message);
    }
}
