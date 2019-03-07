package com.wiseq.cn.business;

import com.wiseq.cn.activiti.listener.ActivitiListener;
import com.wiseq.cn.commons.utils.SpringUtil;
import com.wiseq.cn.dao.BusinessTestDao;
import com.wiseq.cn.service.TestService;

import java.util.ArrayList;
import java.util.List;

/**
 * 版本        修改时间        作者        修改内容
 * V1.0        ------        wangyh       原始版本
 * 文件说明: TestStart
 **/
public class TestStart extends ActivitiListener {

    TestService testService = SpringUtil.getBean(TestService.class);

    public void start(){
        List<String> assigneeList= new ArrayList<>(); //分配任务的人员
        assigneeList.add("admin");
        assigneeList.add("lida");

        setExeVar("userlist", assigneeList);
    }

    public void save(){
        testService.save((BusinessTest) parseFormData(BusinessTest.class));
    }

    public void update(){
        testService.update((BusinessTest) parseFormData(BusinessTest.class));
    }

    public void updateRs(){
        testService.updateRs((BusinessTest) parseFormData(BusinessTest.class));
    }

    public void updateBs(){
        testService.updateBs((BusinessTest) parseFormData(BusinessTest.class));
    }

    public void updateFz(){
        testService.updateFz((BusinessTest) parseFormData(BusinessTest.class));
    }
}
