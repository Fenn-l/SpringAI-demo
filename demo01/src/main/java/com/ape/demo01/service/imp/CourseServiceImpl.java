package com.ape.demo01.service.imp;

import com.ape.demo01.entity.po.Course;
import com.ape.demo01.mapper.CourseMapper;
import com.ape.demo01.service.ICourseService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements ICourseService {
}
