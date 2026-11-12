package com.ape.demo01.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("course_reservation")
public class CourseReservation implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     * 预约课程
     */
    private String course;
    /**
     * 学⽣姓名
     */
    private String studentName;
    /**
     * 联系⽅式
     */
    private String contactInfo;
    /**
     * 预约校区
     */
    private String school;
    /**
    * 备注
    */
    private String remark;
}
