package com.ape.demo01.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * PDF 问答相关的 AI 配置：定义向量库 Bean 和 PDF 专用 ChatClient。
 *
 * <p>RAG（检索增强生成）链路：
 * 上传的 PDF → 切片 → 嵌入模型转向量 → 存进 VectorStore；
 * 用户提问时，QuestionAnswerAdvisor 先从 VectorStore 检索相关片段，再交给大模型作答。
 */
@Configuration
public class SpringAIConfiguration {

    /** 内存向量库：存文档向量，供检索使用（SimpleVectorStore 不依赖外部数据库，适合演示）。 */
    @Bean
    public VectorStore vectorStore(OpenAiEmbeddingModel embeddingModel) {
        // 用嵌入模型把文本转成向量；维度/模型由 application.yaml 的 embedding 配置决定
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    /** PDF 专用 ChatClient：在通用记忆之上叠加「问答检索」Advisor。 */
    @Bean
    public ChatClient pdfChatClient(
            OpenAiChatModel model,
            ChatMemory chatMemory,
            VectorStore vectorStore) {
        return ChatClient.builder(model)
                .defaultAdvisors(
                        SimpleLoggerAdvisor.builder().build(), // 日志
                        MessageChatMemoryAdvisor.builder(chatMemory).build(), // 会话记忆
                        QuestionAnswerAdvisor // 检索增强：先查向量库再回答
                                .builder(vectorStore)
                                .searchRequest(
                                        SearchRequest.builder()
                                                .similarityThreshold(0.5d) // 相似度阈值：低于此值不召回
                                                .topK(2) // 每次最多取 2 个最相关片段
                                                .build()
                                ).build()
                )
                .build();
    }
}
