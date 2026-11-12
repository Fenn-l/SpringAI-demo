package com.ape.demo01.repository;

import java.util.List;

/**
 * 会话ID管理（课件 1.5.1）
 *
 * 所有会话ID按「业务类型」分组保存，方便前端左侧栏按功能区分
 * （chat / service / pdf ...）。将来可以扩展 userId、time 等维度。
 */
public interface ChatHistoryRepository {

    /**
     * 保存会话记录
     *
     * @param type   业务类型，如：chat、service、pdf
     * @param chatId 会话ID
     */
    void save(String type, String chatId);

    /**
     * 获取会话ID列表
     *
     * @param type 业务类型，如：chat、service、pdf
     * @return 会话ID列表
     */
    List<String> getChatIds(String type);
}
