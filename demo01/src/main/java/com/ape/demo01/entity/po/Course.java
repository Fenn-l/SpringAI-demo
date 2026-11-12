package com.ape.demo01.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("course")
public class Course implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     * 学科名称
     */
    private String name;
    /**
     * 学历背景要求：0-⽆，1-初中，2-⾼中、3-⼤专、4-本科以上
     */
    private Integer edu;
    /**
     * 类型: 编程、⾮编程
     */
    private String type;
    private Long price;

    private Integer duration;
}
