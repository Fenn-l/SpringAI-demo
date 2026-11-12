package com.ape.demo01;

import com.ape.demo01.util.VectorDistanceUtils;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

@SpringBootTest
class Demo01ApplicationTests {

    // ⾃动注⼊向量模型,springAI提供给的
    @Autowired
    private OpenAiEmbeddingModel embeddingModel;
    @Test
    public void testEmbedding() {
        // 1.测试数据
        // 1.1.⽤来查询的⽂本，国际冲突
        String query = "global conflicts";
        // 1.2.⽤来做⽐较的⽂本
        String[] texts = new String[]{
                "哈⻢斯称加沙下阶段停⽕谈判仍在进⾏ 以⽅尚未做出承诺",
                "⼟⽿其、芬兰、瑞典与北约代表将继续就瑞典“⼊约”问题进⾏谈判",
                "⽇本航空基地⽔井中检测出有机氟化物超标",
                "国家游泳中⼼（⽔⽴⽅）：恢复游泳、嬉⽔乐园等⽔上项⽬运营",
                "我国⾸次在空间站开展舱外辐射⽣物学暴露实验",
        };
        // 2.向量化
        // 2.1.先将查询⽂本向量化
        float[] queryVector = embeddingModel.embed(query);
        //可以试着输出打印queryVector ，就是⼀串向量数字
        // 2.2.再将⽐较⽂本向量化，放到⼀个数组
        List<float[]> textVectors = embeddingModel.embed(Arrays.asList(texts));
        // 3.⽐较欧⽒距离
        // 3.1.把查询⽂本⾃⼰与⾃⼰⽐较，肯定是相似度最⾼的
        System.out.println(VectorDistanceUtils.euclideanDistance(queryVector,
                queryVector));
        // 3.2.把查询⽂本与其它⽂本⽐较
        for (float[] textVector : textVectors) {
            System.out.println(VectorDistanceUtils.euclideanDistance(queryVector,
                    textVector));
        }
        System.out.println("------------------");
        // 4.⽐较余弦距离
        // 4.1.把查询⽂本⾃⼰与⾃⼰⽐较，肯定是相似度最⾼的
        System.out.println(VectorDistanceUtils.cosineDistance(queryVector, queryVector));
        // 4.2.把查询⽂本与其它⽂本⽐较
        for (float[] textVector : textVectors) {
            System.out.println(VectorDistanceUtils.cosineDistance(queryVector, textVector));
        }
    }
}
