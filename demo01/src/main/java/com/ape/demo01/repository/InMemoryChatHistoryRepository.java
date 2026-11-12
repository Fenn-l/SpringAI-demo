package com.ape.demo01.repository;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存版会话ID仓库（课件 1.5.1）
 *
 * <p>课件用 HashMap 保存，我这里做了三点加固：
 * <ol>
 *   <li>用 ConcurrentHashMap + 同步集合，避免并发请求下数据结构被破坏；</li>
 *   <li>用 LinkedHashSet 去重：同一次会话的每条消息都会调用一次 save()，
 *       如果像课件那样直接 add，前端左侧栏会出现大量重复的会话条目；</li>
 *   <li>保留插入顺序，最近创建的会话排在前面，方便前端展示。</li>
 * </ol>
 *
 * <p>将来要持久化，只需把这里的实现换成基于 Redis / MySQL 的版本，接口不用动。
 */
@Component
public class InMemoryChatHistoryRepository implements ChatHistoryRepository {

    // key：业务类型；value：该类型下所有会话ID（去重且保持插入顺序）
    private final Map<String, Set<String>> chatHistory = new ConcurrentHashMap<>();

    @Override
    public void save(String type, String chatId) {
        if (type == null || chatId == null || chatId.isBlank()) {
            return;
        }
        Set<String> chatIds = chatHistory.computeIfAbsent(type,
                k -> Collections.synchronizedSet(new LinkedHashSet<>()));
        chatIds.add(chatId);
    }

    @Override
    public List<String> getChatIds(String type) {
        Set<String> chatIds = chatHistory.get(type);
        if (chatIds == null) {
            return List.of();
        }
        // 复制一份快照再返回，避免调用方遍历时和写入线程冲突
        synchronized (chatIds) {
            return List.copyOf(chatIds);
        }
    }
}
