package com.wiseq.cn.dao;

import com.wiseq.cn.entity.FlowProcess;
import com.wiseq.cn.entity.FlowTask;

import java.util.List;

/**
 * 版本        修改时间        作者        修改内容
 * V1.0        ------        wangyh       原始版本
 * 文件说明: MetaFieldDao
 **/
public interface ProcessDao {
    /**
     *  查询所有流程
     */
    List<FlowProcess> findProcess();

    /**
     * 查询代办任务
     * @param assignee
     * @return
     */
    List<FlowTask> findTaskByUser(String assignee);
}
