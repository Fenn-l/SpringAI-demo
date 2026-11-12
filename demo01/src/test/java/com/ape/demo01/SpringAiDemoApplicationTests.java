package com.ape.demo01;

import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import org.springframework.ai.document.Document;

import java.util.List;

@SpringBootTest
public class SpringAiDemoApplicationTests {
    // ⾃动注⼊向量模型
    @Autowired
    private VectorStore vectorStore;
    @Test
    public void testVectorStore(){
        Resource resource = new FileSystemResource("java⼿册.pdf");
        // 1.创建PDF的读取器
        PagePdfDocumentReader reader = new PagePdfDocumentReader(
                resource, // ⽂件源
                PdfDocumentReaderConfig.builder()
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.defaults())
                        .withPagesPerDocument(1) // 每1⻚PDF作为⼀个Document
                        .build()
        );
        // 2.读取PDF⽂档，拆分为Document
        List<Document> documents = reader.read();
        // 3.写⼊向量库
        vectorStore.add(documents);
        // 4.搜索
        SearchRequest request = SearchRequest.builder()
                .query("数据库索引规范有哪些")// ⽤⼾查询⽂本，会转为向量做相似度检索
                .topK(5) // topK：最多返回相似度最⾼的5条⽂档⽚段
                .similarityThreshold(0.6)// 相似度阈值：只返回相似度分数 ≥0.6 的结果
                .filterExpression("file_name == 'java⼿册.pdf'")// 元数据过滤，只从⽂ 件名为java⼿册.pdf的⽂档中检索
                .build();
        List<Document> docs = vectorStore.similaritySearch(request);
        if (docs == null) {
            System.out.println("没有搜索到任何内容");
            return;
        }
        for (Document doc : docs) {
            System.out.println(doc.getId()); // 向量⽂档id（向量库⾥⾯这条⽚段的唯⼀标识）
            System.out.println(doc.getScore()); // 相似度分数，值越接近1代表和query越相似
            System.out.println(doc.getText()); // ⽂档⽚段原⽂，检索出来的⽂本内容
        }
    }
}
