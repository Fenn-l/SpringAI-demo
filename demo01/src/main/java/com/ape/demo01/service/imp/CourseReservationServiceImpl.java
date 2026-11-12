package com.ape.demo01.service.imp;

import com.ape.demo01.entity.po.CourseReservation;
import com.ape.demo01.mapper.CourseReservationMapper;
import com.ape.demo01.service.ICourseReservationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class CourseReservationServiceImpl extends ServiceImpl<CourseReservationMapper, CourseReservation> implements ICourseReservationService {
}
