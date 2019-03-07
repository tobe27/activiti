package com.wiseq.cn.dao;

import com.wiseq.cn.entity.FlowProcess;
import com.wiseq.cn.entity.FlowTask;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 版本        修改时间        作者        修改内容
 * V1.0        ------        wangyh       原始版本
 * 文件说明: MetaFieldDao
 **/
public interface FlowProcessDao {
    /**
     *  查询所有流程
     */
    List<FlowProcess> findProcess();

    /**
     * 查询代办任务
     * @param assignee
     * @return
     */
    List<FlowTask> findTaskByUser(@Param(value="assignee") String assignee);

    /**
     * 根据用户名查询用户ID
     * @param userName
     * @return
     */
    String findByUserName(@Param(value="userName") String userName);

    /**
     *  查询流程当前运行的实例
     * @param processId  流程定义ID
     * @return
     */
    List<LinkedHashMap<String,String>> findRunning(@Param(value="processId") String processId);

    /**
     *  查询已办任务
     * @param assignee
     * @return
     */
    List<LinkedHashMap<String,String>> historyTasks(@Param(value="assignee") String assignee);

    /**
     *  查询历史任务历史节点
     * @param taskId
     * @return
     */
    String findHistoryNodeId(@Param(value="taskId") String taskId);

    /**
     *  根据历史节点ID查找历史任务ID
     * @param id
     * @return
     */
    String findTaskIdFromActinstById(@Param(value="id") String id);

    /**
     *  查询历史活动任务
     * @param taskId
     * @return
     */
    List<LinkedHashMap<String,Object>> findHisActiveTask(@Param(value="taskId") String taskId);

    /**
     *  删除历史节点
     * @param id
     * @return
     */
    Integer deleteHistoryActivitie(@Param(value="id") String id);

    /**
     *  修改历史任务
     * @param endTime   结束时间
     * @param duration  持续时间
     * @param id  ID
     * @return
     */
    Integer updateHisTask(@Param(value="endTime") Date endTime,
                          @Param(value="duration") long duration,
                          @Param(value="id") String id);
}
