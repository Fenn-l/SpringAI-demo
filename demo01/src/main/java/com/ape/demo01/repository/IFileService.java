package com.ape.demo01.repository;

import org.springframework.core.io.Resource;

public interface IFileService {
    /**
     * 保存⽂件,还要记录chatId与⽂件的映射关系
     * @param chatId 会话id
     * @param resource ⽂件
     * @return 上传成功，返回true； 否则返回false
     */
    boolean save(String chatId, Resource resource);
    /**
     * 根据chatId获取⽂件
     * @param chatId 会话id
     * @return 找到的⽂件
     */
    Resource getFile(String chatId);
}
