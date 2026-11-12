package com.ape.demo01.controller;

import com.ape.demo01.entity.vo.Result;
import com.ape.demo01.repository.ChatHistoryRepository;
import com.ape.demo01.repository.IFileService;
import com.ape.demo01.util.ConversationKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * PDF 知识库问答接口（RAG：上传 PDF → 向量化入库 → 基于 PDF 内容问答）。
 *
 * <p>涉及三个动作：
 * <ol>
 *   <li>POST /ai/pdf/upload/{chatId}  上传 PDF，并自动切片向量化</li>
 *   <li>GET  /ai/pdf/file/{chatId}    下载该会话对应的 PDF</li>
 *   <li>GET  /ai/pdf/chat?prompt=...&chatId=...  基于该 PDF 内容问答</li>
 * </ol>
 */
@Slf4j
@RestController
@RequestMapping("/ai/pdf")
public class PdfController {

    /** 会话ID仓库。 */
    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    /** 文件服务：负责落盘 + 向量化入库。 */
    @Autowired
    private IFileService fileService;

    /** 由 SpringAIConfiguration 注入的 PDF 专用 ChatClient（带 QuestionAnswerAdvisor 检索增强）。 */
    @Autowired
    private ChatClient pdfChatClient;

    /** 文件上传：仅接受 PDF，成功后落到磁盘并写入向量库。 */
    @RequestMapping("/upload/{chatId}")
    public Result uploadPdf(@PathVariable String chatId, @RequestParam("file") MultipartFile file) {
        try {
// 1.校验内容类型必须是 PDF（防止上传非 PDF 文件导致切片失败）
            if (!Objects.equals(file.getContentType(), "application/pdf")) {
                return Result.fail("只能上传PDF文件！");
            }
// 2.保存文件并向量化（IFileService 内部已做文件名清洗、路径穿越防护）
            boolean success = fileService.save(chatId, file.getResource());
            if (!success) {
                return Result.fail("保存文件失败！");
            }
            return Result.ok();
        } catch (Exception e) {
            log.error("Failed to upload PDF.", e);
            return Result.fail("上传文件失败！");
        }
    }

    /** 文件下载：按 chatId 取回之前上传的 PDF，并正确设置中文文件名。 */
    @GetMapping("/file/{chatId}")
    public ResponseEntity<Resource> download(@PathVariable("chatId") String chatId) throws IOException {
// 1.读取文件（IFileService 返回的是绝对路径 Resource）
        Resource resource = fileService.getFile(chatId);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
// 2.URL 编码文件名，避免中文/特殊字符在响应头里乱码
        String filename = URLEncoder.encode(Objects.requireNonNull(resource.getFilename()), StandardCharsets.UTF_8);
// 3.以附件形式返回，浏览器会触发下载框
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    /** 基于 PDF 内容问答（流式）。 */
    @RequestMapping(value = "/chat", produces = "text/html;charset=UTF-8")
    public Flux<String> chat(@RequestParam(required = false, defaultValue = "") String prompt,
                             @RequestParam(required = false, defaultValue = "default") String chatId) {
// 记录会话ID到「pdf」业务分组
        chatHistoryRepository.save("pdf", chatId);
// 先清洗 chatId，保证记忆 Key 与过滤表达式都不含危险字符
        String safeChatId = ConversationKeys.sanitize(chatId);
        return pdfChatClient
                .prompt(prompt)
                // 记忆 Key 隔离为 pdf:chatId
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, ConversationKeys.of("pdf", chatId)))
                // 向量检索过滤：只召回「属于当前会话」的 PDF 片段
                // 【安全修复】原来直接拼接 chatId，含单引号会破坏表达式；这里先 sanitize 只允许白名单字符
                .advisors(a -> a.param(QuestionAnswerAdvisor.FILTER_EXPRESSION,
                        "chat_id == '" + safeChatId + "'"))
                .stream()
                .content();
    }
}
