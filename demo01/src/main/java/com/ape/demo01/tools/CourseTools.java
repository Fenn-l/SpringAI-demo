package com.ape.demo01.tools;

import com.ape.demo01.entity.po.Course;
import com.ape.demo01.entity.po.CourseReservation;
import com.ape.demo01.entity.po.School;
import com.ape.demo01.entity.query.CourseQuery;
import com.ape.demo01.service.ICourseService;
import com.ape.demo01.service.ICourseReservationService;
import com.ape.demo01.service.ISchoolService;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 课程业务工具类（给大模型用的 Function Calling 工具）。
 *
 * <p>被打上 {@link Tool} 注解的方法，会被注册到 serviceChatClient，
 * 大模型在回答客服问题时可自主决定调用它们来查询课程、校区或生成预约单。
 */
@Component
public class CourseTools {

    /** 课程服务（MyBatis-Plus 提供）。 */
    @Autowired
    private ICourseService courseService;

    /** 校区服务。 */
    @Autowired
    private ISchoolService schoolService;

    /** 课程预约服务。 */
    @Autowired
    private ICourseReservationService courseReservationService;

    /** 按条件查询课程（类型 / 学历 / 排序）。 */
    @Tool(description = "根据条件查询课程")
    public List<Course> queryCourse(@ToolParam(required = false, description =
            "课程查询条件") CourseQuery query) {
        // 用 MyBatis-Plus 的链式查询构造器
        QueryChainWrapper<Course> wrapper = courseService.query();
        // 仅在参数非空时才追加对应条件，避免无效过滤
        wrapper
                .eq(query.getType() != null, "type", query.getType()) // 课程类型（编程/设计…）
                .le(query.getEdu() != null, "edu", query.getEdu());    // 学历要求上限
        // 按前端/模型给出的排序字段逐个追加排序
        if (query.getSorts() != null) {
            for (CourseQuery.Sort sort : query.getSorts()) {
                wrapper.orderBy(true, sort.getAsc(), sort.getField());
            }
        }
        return wrapper.list();
    }

    /** 查询所有校区（供客服向用户展示可选校区）。 */
    @Tool(description = "查询所有校区")
    public List<School> queryAllSchools() {
        return schoolService.list();
    }

    /** 生成课程预约单并入库，返回生成的预约单号。 */
    @Tool(description = "生成课程预约单,并返回生成的预约单号")
    public String generateCourseReservation(
            @ToolParam(required = false, description = "预约课程") String courseName,
            @ToolParam(required = false, description = "预约学生") String studentName,
            @ToolParam(required = false, description = "联系电话") String contactInfo,
            @ToolParam(required = false, description = "预约学校") String school,
            @ToolParam(required = false, description = "预约备注") String remark) {
        // 组装预约实体
        CourseReservation courseReservation = new CourseReservation();
        courseReservation.setCourse(courseName);
        courseReservation.setStudentName(studentName);
        courseReservation.setContactInfo(contactInfo);
        courseReservation.setSchool(school);
        courseReservation.setRemark(remark);
        // 落库（MyBatis-Plus 自增主键回填到 id）
        courseReservationService.save(courseReservation);
        // 返回预约单号（字符串形式，方便大模型在对话里回显）
        return String.valueOf(courseReservation.getId());
    }
}
