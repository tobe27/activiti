package com.wiseq.cn.service;

import com.wiseq.cn.entity.FlowProcess;
import com.wiseq.cn.entity.FlowTask;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Comment;
import org.activiti.engine.task.Task;
import org.apache.ibatis.annotations.Param;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface ActivitiService {

    /**
     *  创建流程模型
     * @param name   模型名称
     * @param code   模型编码
     * @param description  模型描述
     * @return
     */
    String createModel(String name, String code, String description);

    /**
     * 获取所有模型
     * @return
     */
    Object findModel();

    /**
     * 删除模型
     * @param modelId 模型ID
     * @return
     */
    void deleteModel(String modelId);

    /**
     * 发布模型为流程定义
     * @param modelId 模型ID
     * @return
     */
    String releaseModel(String modelId);

    /**
     * 删除流程定义
     *
     * @param deploymentId  流程发布ID
     * @param flag  为true时表示级联删除，会删除掉和当前部署的规则相关的信息，包括历史信息；否则为普通删除，如果当前部署的规则还存在正在制作的流程，会抛异常。
     */
    void deleteProcess(String deploymentId, boolean flag);

    /**
     *  查询所有流程
     * @return
     */
    List<FlowProcess> findProcess();

    /**
     * 启动流程
     *
     * @param processKey 流程定义id
     */
    String startProcess(String processKey, Map<String, Object> variables);

    /**
     * 查询指定办理人的待办任务
     *
     * @param assignee  用户
     * @return
     */
    List<FlowTask> findTaskByAssignee(String assignee);

    /**
     *  选择给定用户是候选用户的任务
     * @param assignee  用户ID
     * @return
     */
    List<FlowTask> findGroupTask(String assignee);

    /**
     * 认领任务
     * @param taskId  任务ID
     * @param assignee  用户
     */
    void claimTask(String taskId, String assignee);

    /**
     *  释放任务
     * @param taskId 任务ID
     */
    void releaseTask(String taskId);

    /**
     * 完成任务
     *
     * @param taskId 任务ID
     * @param variables  任务参数
     */
    void completeTask(String taskId, Map<String, Object> variables);

    /**
     *  协办任务
     * @param taskId  任务ID
     * @param userId  协办人
     * @param variables 流程参数
     */
    void assistTask(String taskId, String userId, Map<String, Object> variables);

    /**
     *  删除任务
     * @param executionId   流程实例ID
     * @param deleteReason  删除原因
     */
    void deleteTask(String executionId, String deleteReason);

    /**
     *  转办任务
     * @param taskId  任务ID
     * @param assignee  转办人
     */
    void forwardTask(String taskId, String assignee);

    /**
     *  退回任务到上一级
     * @param taskId  任务ID
     * @return
     */
    void fallBackTask(String taskId, String activityId, String userId);

    /**
     *  撤回已提交任务
     * @param hisTaskId 历史任务ID
     */
    void withdraw(String hisTaskId);

    /**
     * 挂起流程(挂起后不可创建实例)
     * @param processKey 流程定义Key
     */
    void hangProcess(String processKey);

    /**
     * 激活挂起的流程
     * @param processKey 流程定义Key
     */
    void activateProcess(String processKey);

    /**
     * 挂起任务(流程实例,挂起后任务不可办理)
     * @param executionId 流程实例ID
     */
    void hangTask(String executionId);

    /**
     * 激活挂起的任务(流程实例)
     * @param executionId 流程实例ID
     */
    void activateTask(String executionId);

    /**
     *  流程跟踪
     * @param executionId 流程实例ID
     * @return
     */
    String flowTrace(String executionId);

    /**
     * 流程图XML查看
     * @param processId 流程定义ID
     */
    String flowXml(String processId);

    /**
     * 根据流程ID查询开始表单
     * @param processId 流程定义ID
     * @return
     */
    String findStartForm(String processId);

    /**
     *  获取Task表单
     * @param taskId  任务ID
     * @return
     */
    Object findTaskForm(String taskId);

    /**
     *  查询流程所有节点
     * @param processId  流程定义ID
     * @return
     */
    List<Map<String,String>> findNode(String processId);

    /**
     *  查询流程当前运行的实例
     * @param processId  流程定义ID
     * @return
     */
    List<LinkedHashMap<String,String>> running(String processId);

    /**
     *  查询已办任务
     * @param assignee
     * @return
     */
    List<LinkedHashMap<String,String>> historyTasks(String assignee);

    /**
     *  查询历史任务历史节点
     * @param hisTaskId 历史任务ID
     * @return
     */
    String findHistoryNodeId(String hisTaskId);
}