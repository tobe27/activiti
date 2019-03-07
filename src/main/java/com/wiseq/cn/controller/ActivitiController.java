package com.wiseq.cn.controller;

import com.wiseq.cn.activiti.bpm.RollbackTaskCmd;
import com.wiseq.cn.commons.entity.Result;
import com.wiseq.cn.commons.utils.ResultUtils;
import com.wiseq.cn.entity.FlowTask;
import com.wiseq.cn.service.ActivitiService;
import com.wiseq.cn.utils.FlowUtil;
import io.swagger.annotations.Api;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.impl.interceptor.Command;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping(value = "/flow")
@Api(description = "工作流接口")
public class ActivitiController {

    @Autowired
    private ActivitiService activitiService;

    /**
     *  创建流程模型
     * @param name   模型名称
     * @param code   模型编码
     * @param description  模型描述
     * @return
     */
    @PostMapping(value = "/createModule")
    public Result create(@RequestParam(value = "name") String name,
                         @RequestParam(value = "code") String code,
                         @RequestParam(value = "description",required = false) String description) {
        return ResultUtils.success(activitiService.createModel(name, code, description));
    }

    /**
     *  查询模型
     * @return
     */
    @GetMapping(value = "/findModel")
    public Result findModel(){
        return ResultUtils.success(activitiService.findModel());
    }

    /**
     *  删除模型
     * @param modelId
     * @return
     */
    @DeleteMapping(value = "/deleteModel")
    public Result deleteModel(@RequestParam(value = "modelId") String modelId){
        activitiService.deleteModel(modelId);
        return ResultUtils.success();
    }

    /**
     * 发布模型为流程定义
     * @param modelId 模型ID
     * @return
     */
    @PostMapping(value = "/releaseModel")
    public Result releaseModel(@RequestParam(value = "modelId") String modelId){
        return ResultUtils.success(activitiService.releaseModel(modelId));
    }

    /**
     *  删除流程
     * @param deploymentId  流程定义ID
     * @param flag  是否级联删除
     * @return
     */
    @DeleteMapping("/deleteProcess")
    public Result deleteProcess(@RequestParam(value = "deploymentId") String deploymentId,
                                @RequestParam(value = "flag",defaultValue = "true") Boolean flag){
        activitiService.deleteProcess(deploymentId, flag);
        return ResultUtils.success();
    }

    /**
     *  查询所有流程
     * @return
     */
    @GetMapping(value = "/findProcess")
    public Result findProcess(){
        return ResultUtils.success(activitiService.findProcess());
    }

    /**
     * 启动流程
     * @param processKey 流程定义id
     * @return
     */
    @PostMapping(value = "/start")
    public Result start(@RequestParam(value = "processKey") String processKey, HttpServletRequest request){
        Map<String, Object> variables = FlowUtil.parseRequestParams(request);
        return ResultUtils.success(activitiService.startProcess(processKey, variables));
    }

    /**
     * 查询用户待办任务
     * @param assignee  任务办理人
     */
    @GetMapping(value = "/findTask")
    public Result findTask(@RequestParam(value = "assignee") String assignee){
        return ResultUtils.success(activitiService.findTaskByAssignee(assignee));
    }

    /**
     *  查询已办任务
     * @param assignee
     * @return
     */
    @GetMapping(value = "/historyTasks")
    public Result historyTasks(@RequestParam(value = "assignee") String assignee){
        return ResultUtils.success(activitiService.historyTasks(assignee));
    }

    /**
     *  选择给定用户是候选用户的任务
     * @param assignee  用户
     * @return
     */
    @GetMapping(value = "/findGroupTask")
    public Result findGroupTask(@RequestParam(value = "assignee") String assignee){
        return ResultUtils.success(activitiService.findGroupTask(assignee));
    }

    /**
     * 认领任务
     * @param taskId  任务ID
     * @param assignee  用户
     */
    @PostMapping(value = "/claimTask")
    public Result claimTask(@RequestParam(value = "taskId") String taskId,
                            @RequestParam(value = "assignee") String assignee){
        activitiService.claimTask(taskId, assignee);
        return ResultUtils.success();
    }

    /**
     *  释放任务
     * @param taskId 任务ID
     */
    @PostMapping(value = "/releaseTask")
    public Result releaseTask(@RequestParam(value = "taskId") String taskId){
        activitiService.releaseTask(taskId);
        return ResultUtils.success();
    }

    /**
     * 任务办理
     * @param taskId 任务ID
     * @return
     */
    @PostMapping(value = "/doTask")
    public Result doTask(@RequestParam(value = "taskId") String taskId, HttpServletRequest request){
        Map<String, Object> variables = FlowUtil.parseRequestParams(request);
        activitiService.completeTask(taskId, variables);
        return ResultUtils.success();
    }

    /**
     *  协办任务
     * @param taskId  任务ID
     * @param userId  协办人
     */
    @PostMapping(value = "/assistTask")
    public Result assistTask(@RequestParam(value = "taskId") String taskId,
                           @RequestParam(value = "userId") String userId, HttpServletRequest request){
        Map<String, Object> variables = FlowUtil.parseRequestParams(request);
        activitiService.assistTask(taskId, userId, variables);
        return ResultUtils.success();
    }

    /**
     *  删除任务
     * @param executionId   流程实例ID
     * @param deleteReason  删除原因
     */
    @DeleteMapping(value = "/deleteTask")
    public Result deleteTask(@RequestParam(value = "executionId") String executionId,
                             @RequestParam(value = "deleteReason") String deleteReason){
        activitiService.deleteTask(executionId, deleteReason);
        return ResultUtils.success();
    }

    /**
     *  转办任务
     * @param taskId  任务ID
     * @param assignee  转办人
     */
    @PostMapping(value = "/turnSendTask")
    public Result turnSendTask(@RequestParam(value = "taskId")String taskId,
                               @RequestParam(value = "assignee")String assignee){
        activitiService.forwardTask(taskId, assignee);
        return ResultUtils.success();
    }

    /**
     *  退回任务到上一级
     * @param taskId  任务ID
     * @return
     */
    @PostMapping(value = "/fallBackTask")
    public Result fallBackTask(@RequestParam(value = "taskId")String taskId,
                               @RequestParam(value = "activityId", required = false)String activityId,
                               @RequestParam(value = "userId", required = false)String userId){
        activitiService.fallBackTask(taskId, activityId, userId);
        return ResultUtils.success();
    }

    /**
     *  撤回已提交任务
     * @param hisTaskId 历史任务ID
     */
    @PostMapping(value = "/withdraw")
    public Result withdraw(@RequestParam(value = "hisTaskId")String hisTaskId){
        activitiService.withdraw(hisTaskId);
        return ResultUtils.success();
    }

    /**
     * 挂起流程(挂起后不可创建实例)
     * @param processKey 流程定义Key
     */
    @PostMapping(value = "/hangProcess")
    public Result hangProcess(@RequestParam(value = "processKey") String processKey){
        activitiService.hangProcess(processKey);
        return ResultUtils.success();
    }

    /**
     * 激活挂起的流程
     * @param processKey 流程定义Key
     */
    @PostMapping(value = "/activateProcess")
    public Result activateProcess(@RequestParam(value = "processKey") String processKey){
        activitiService.activateProcess(processKey);
        return ResultUtils.success();
    }

    /**
     * 挂起任务(流程实例,挂起后任务不可办理)
     * @param executionId 流程实例ID
     */
    @PostMapping(value = "/hangTask")
    public Result hangTask(@RequestParam(value = "executionId") String executionId){
        activitiService.hangTask(executionId);
        return ResultUtils.success();
    }

    /**
     * 激活挂起的任务(流程实例)
     * @param executionId 流程实例ID
     */
    @PostMapping(value = "/activateTask")
    public Result activateTask(@RequestParam(value = "executionId") String executionId){
        activitiService.activateTask(executionId);
        return ResultUtils.success();
    }

    /**
     * 流程跟踪
     * @param executionId 流程实例ID
     */
    @GetMapping("/flowTrace")
    public Result flowTrace(@RequestParam("executionId") String executionId) {
        return ResultUtils.success(activitiService.flowTrace(executionId));
    }

    /**
     * 流程图XML查看
     * @param processId 流程定义ID
     */
    @GetMapping("/flowXml")
    public Result flowXml(@RequestParam("processId") String processId){
        return ResultUtils.success(activitiService.flowXml(processId));
    }

    /**
     * 根据流程ID查询开始表单
     * @param processId 流程定义ID
     * @return
     */
    @GetMapping("/findStartForm")
    public Result findStartForm(@RequestParam("processId") String processId){
        return ResultUtils.success(activitiService.findStartForm(processId));
    }

    /**
     *  获取Task表单
     * @param taskId  任务ID
     * @return
     */
    @GetMapping("/findTaskForm")
    public Result findTaskForm(@RequestParam("taskId") String taskId){
        return ResultUtils.success(activitiService.findTaskForm(taskId));
    }

    /**
     *  查询流程所有节点
     * @param processId  流程定义ID
     * @return
     */
    @GetMapping("/findNode")
    public Result findNode(@RequestParam("processId") String processId){
        return ResultUtils.success(activitiService.findNode(processId));
    }

    /**
     *  查询流程当前运行的实例
     * @param processId  流程定义ID
     * @return
     */
    @GetMapping("/running")
    public Result running(@RequestParam("processId") String processId){
        return ResultUtils.success(activitiService.running(processId));
    }
}