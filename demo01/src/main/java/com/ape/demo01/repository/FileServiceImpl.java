package com.ape.demo01.repository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件服务实现（PDF 上传 / 下载 / 向量化入库）。
 *
 * <p>本类承担三件事：
 * <ol>
 *   <li>把上传的 PDF 落到磁盘（落到固定的 {@link #baseDir} 目录下，杜绝路径穿越）；</li>
 *   <li>把 PDF 按页切片、用嵌入模型转成向量、写进内存向量库（RAG 检索的基础）；</li>
 *   <li>在应用关闭时把「chatId→文件名」映射和向量库快照持久化，下次启动再加载回来。</li>
 * </ol>
 */
@Slf4j
@Service
public class FileServiceImpl implements IFileService {

    /** 注入的向量库 Bean（在 SpringAIConfiguration 里定义，SimpleVectorStore 内存实现）。 */
    @Autowired
    private VectorStore vectorStore;

    /** 基础目录：所有上传文件和持久化文件都放在这里，避免使用「当前工作目录」带来的不确定性。 */
    private final Path baseDir = Path.of("ai-data");

    /** 上传文件存放子目录（baseDir 下的 uploads）。 */
    private final Path uploadDir = Path.of("ai-data", "uploads");

    /** chatId → 文件绝对路径 的映射；用 ConcurrentHashMap 保证并发读写安全。 */
    private final Map<String, String> chatFiles = new ConcurrentHashMap<>();

    /** 持久化映射关系的文件名（注意：init 与 persistent 必须使用<b>完全一致</b>的文件名，旧代码因隐藏字符对不上导致加载失效）。 */
    private static final String PROPS_FILE = "chat-pdf.properties";

    /** 持久化向量库快照的文件名。 */
    private static final String VECTOR_FILE = "chat-pdf.json";

    @Override
    public boolean save(String chatId, Resource resource) {
        try {
// 1.确保目录存在（首次启动时创建 ai-data/uploads）
            Files.createDirectories(uploadDir);

// 2.清洗文件名：只保留原始文件名的基本名，去掉任何路径成分，防止 ../ 之类的路径穿越
            String originalName = resource.getFilename();
            String baseName = (originalName == null) ? "file.pdf" : new File(originalName).getName();
            if (baseName.isBlank()) {
                baseName = "file.pdf";
            }
// 3.用 UUID 前缀生成唯一落盘文件名，避免不同会话上传同名文件互相覆盖
            String storedName = UUID.randomUUID() + "_" + baseName;
            Path target = uploadDir.resolve(storedName);
            Files.copy(resource.getInputStream(), target);

// 4.记录 chatId → 文件绝对路径（存绝对路径，后续 getFile 不再依赖工作目录）
            chatFiles.put(chatId, target.toAbsolutePath().toString());

// 5.把 PDF 切片并写入向量库（RAG 检索的数据源）
            writeToVectorStore(resource, chatId);
            return true;
        } catch (IOException e) {
            log.error("Failed to save PDF resource.", e);
            return false;
        }
    }

    @Override
    public Resource getFile(String chatId) {
// 用绝对路径构建 Resource，避免「相对工作目录」解析失败
        String path = chatFiles.get(chatId);
        if (path == null) {
            return new FileSystemResource("");
        }
        return new FileSystemResource(path);
    }

    /** 应用启动时：恢复上次持久化的 chatId→文件映射 和 向量库快照。 */
    @PostConstruct
    private void init() {
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            log.warn("无法创建基础目录 {}", baseDir, e);
        }
// 1.恢复 chatId → 文件映射
        FileSystemResource propsResource = new FileSystemResource(baseDir.resolve(PROPS_FILE).toFile());
        if (propsResource.exists()) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(propsResource.getInputStream(), StandardCharsets.UTF_8))) {
                Properties props = new Properties();
                props.load(reader);
                props.forEach((k, v) -> chatFiles.put(String.valueOf(k), String.valueOf(v)));
            } catch (IOException e) {
                log.warn("加载 {} 失败", PROPS_FILE, e);
            }
        }
// 2.恢复向量库快照（SimpleVectorStore 支持把 JSON 快照整体 load 回来）
        FileSystemResource vectorResource = new FileSystemResource(baseDir.resolve(VECTOR_FILE).toFile());
        if (vectorResource.exists() && vectorStore instanceof SimpleVectorStore simpleVectorStore) {
            simpleVectorStore.load(vectorResource);
            log.info("已恢复向量库快照 {}", VECTOR_FILE);
        }
    }

    /** 应用关闭时：把映射和向量库快照落盘，方便下次冷启动直接复用。 */
    @PreDestroy
    private void persistent() {
        try {
            Files.createDirectories(baseDir);
// 1.持久化 chatId → 文件映射
            try (FileWriter writer = new FileWriter(baseDir.resolve(PROPS_FILE).toFile(), StandardCharsets.UTF_8)) {
                Properties props = new Properties();
                props.putAll(chatFiles);
                props.store(writer, LocalDateTime.now().toString());
            }
// 2.持久化向量库快照（仅当确实是 SimpleVectorStore 实现时）
            if (vectorStore instanceof SimpleVectorStore simpleVectorStore) {
                simpleVectorStore.save(baseDir.resolve(VECTOR_FILE).toFile());
            }
        } catch (IOException e) {
// 关闭阶段的 IO 异常不应该导致 JVM 异常退出，只记录日志
            log.error("持久化 PDF 数据失败", e);
        }
    }

    /** 把一份 PDF 资源切片、加 chat_id 元数据、写入向量库。 */
    private void writeToVectorStore(Resource resource, String chatId) {
// 1.创建 PDF 按页读取器（每页作为一个 Document）
        PagePdfDocumentReader reader = new PagePdfDocumentReader(
                resource,
                PdfDocumentReaderConfig.builder()
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.defaults())
                        .withPagesPerDocument(1) // 每 1 页 PDF 作为一个 Document
                        .build()
        );
// 2.读取并拆分成 List<Document>
        List<Document> documents = reader.read();
// 3.给每个 Document 打上 chat_id 元数据，供检索时按会话隔离（见 PdfController 的 FILTER_EXPRESSION）
        documents.forEach(document -> document.getMetadata().put("chat_id", chatId));
// 4.写入向量库（内部会调用嵌入模型把文本转成向量）
        vectorStore.add(documents);
    }
}
