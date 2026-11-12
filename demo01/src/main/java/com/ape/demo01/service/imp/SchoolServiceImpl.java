package com.ape.demo01.service.imp;

import com.ape.demo01.entity.po.School;
import com.ape.demo01.mapper.SchoolMapper;
import com.ape.demo01.service.ISchoolService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class SchoolServiceImpl extends ServiceImpl<SchoolMapper, School> implements ISchoolService {
}