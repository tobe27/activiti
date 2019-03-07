package com.wiseq.cn.activiti.bpm;

import com.wiseq.cn.commons.utils.SpringUtil;
import com.wiseq.cn.dao.FlowProcessDao;
import org.activiti.engine.impl.HistoricActivityInstanceQueryImpl;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.cmd.GetDeploymentProcessDefinitionCmd;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.*;
import org.activiti.engine.impl.pvm.process.ActivityImpl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 版本        修改时间        作者        修改内容
 * V1.0        ------        wangyh       原始版本
 * 文件说明: WithdrawTaskCmd
 **/
public class WithdrawTaskCmd implements Command<Integer> {

    private String historyTaskId;
    private FlowProcessDao processDao = SpringUtil.getBean(FlowProcessDao.class);

    /**
     * 这个historyTaskId是已经完成的一个任务的id.
     */
    public WithdrawTaskCmd(String historyTaskId) {
        this.historyTaskId = historyTaskId;
    }

    /**
     *  执行撤回(0-撤回成功   2-撤回失败)
     * @param commandContext
     * @return
     */
    @Override
    public Integer execute(CommandContext commandContext) {
        // 获得历史任务
        HistoricTaskInstanceEntity historicTaskInstanceEntity = Context
                .getCommandContext().getHistoricTaskInstanceEntityManager()
                .findHistoricTaskInstanceById(historyTaskId);
        // 获得历史节点
        HistoricActivityInstanceEntity historicActivityEntity = getHistoricActivityInstanceEntity(historyTaskId);
        Graph graph = new ActivitiHistoryGraphBuilder(historicTaskInstanceEntity.getProcessInstanceId()).build();
        Node node = graph.findById(historicActivityEntity.getId());
        // 检查节点是否可撤回
        if (!checkCouldWithdraw(node)) {
            return 2;
        }
        // 删除所有活动中的task
        System.err.println("ProcessInstanceId: " + historicTaskInstanceEntity.getProcessInstanceId());
        deleteActiveTasks(historicTaskInstanceEntity.getProcessInstanceId());
        // 获得期望撤销的节点后面的所有节点历史
        List<String> historyNodeIds = new ArrayList<>();
        collectNodes(node, historyNodeIds);
        deleteHistoryActivities(historyNodeIds);
        // 恢复期望撤销的任务和历史
        processHistoryTask(historicTaskInstanceEntity, historicActivityEntity);
        return 0;
    }

    /**
     *  获得历史节点
     * @param historyTaskId 执行撤回的历史任务ID
     * @return
     */
    public HistoricActivityInstanceEntity getHistoricActivityInstanceEntity(String historyTaskId) {
        String historicActivityInstanceId = processDao.findHistoryNodeId(historyTaskId);
        HistoricActivityInstanceQueryImpl historicActivityInstanceQueryImpl = new HistoricActivityInstanceQueryImpl();
        historicActivityInstanceQueryImpl.activityInstanceId(historicActivityInstanceId);
        HistoricActivityInstanceEntity historicActivityInstanceEntity = (HistoricActivityInstanceEntity) Context
                .getCommandContext()
                .getHistoricActivityInstanceEntityManager()
                .findHistoricActivityInstancesByQueryCriteria(historicActivityInstanceQueryImpl, new Page(0, 1))
                .get(0);
        return historicActivityInstanceEntity;
    }

    /**
     *  检查任务节点是否可撤回
     * @param node 执行撤回的节点
     * @return
     */
    public boolean checkCouldWithdraw(Node node) {
        // 遍历该节点所有的出口
        for (Edge edge : node.getOutgoingEdges()) {
            Node dest = edge.getDest();
            String type = dest.getType();
            // 节点类型为用户任务
            if ("userTask".equals(type)) {
                // 判断出口是否结束
                if (!dest.isActive()) {
                    return checkCouldWithdraw(dest);
                }
            } else if (type.endsWith("Gateway")) {
                // 节点类型为网关
                return checkCouldWithdraw(dest);
            } else {
                // 其他情况均不可撤回
                return false;
            }
        }
        return true;
    }

    /**
     * 删除未完成任务.
     */
    public void deleteActiveTasks(String processInstanceId) {
        Context.getCommandContext().getTaskEntityManager()
                .deleteTasksByProcessInstanceId(processInstanceId, "任务撤回", true);
    }

    /**
     * 遍历节点.
     */
    public void collectNodes(Node node, List<String> historyNodeIds) {
        for (Edge edge : node.getOutgoingEdges()) {
            Node dest = edge.getDest();
            historyNodeIds.add(dest.getId());
            this.collectNodes(dest, historyNodeIds);
        }
    }

    /**
     * 删除历史节点.
     */
    public void deleteHistoryActivities(List<String> historyNodeIds) {
        for (String id : historyNodeIds) {
            String taskId = processDao.findTaskIdFromActinstById(id);
            if (taskId != null) {
                Context.getCommandContext()
                        .getHistoricTaskInstanceEntityManager()
                        .deleteHistoricTaskInstanceById(taskId);
            }
            processDao.deleteHistoryActivitie(id);
        }
    }

    public void processHistoryTask(HistoricTaskInstanceEntity hisTaskEnty,HistoricActivityInstanceEntity hisActEnty) {

        hisActEnty.setEndTime(null);
        hisActEnty.setDeleteReason(null);
        hisActEnty.setDurationInMillis(null);

        hisTaskEnty.setEndTime(null);
        hisTaskEnty.setDeleteReason(null);
        hisTaskEnty.setDurationInMillis(null);

        TaskEntity task = TaskEntity.create(new Date());
        task.setId(hisTaskEnty.getId());
        task.setPriority(hisTaskEnty.getPriority());
        task.setTenantId(hisTaskEnty.getTenantId());
        task.setNameWithoutCascade(hisTaskEnty.getName());
        task.setExecutionId(hisTaskEnty.getExecutionId());
        task.setAssigneeWithoutCascade(hisTaskEnty.getAssignee());
        task.setProcessInstanceId(hisTaskEnty.getProcessInstanceId());
        task.setDescriptionWithoutCascade(hisTaskEnty.getDescription());
        task.setProcessDefinitionId(hisTaskEnty.getProcessDefinitionId());
        task.setParentTaskIdWithoutCascade(hisTaskEnty.getParentTaskId());

        task.setTaskDefinitionKey(hisTaskEnty.getTaskDefinitionKey());

        Context.getCommandContext().getTaskEntityManager().insert(task);

        ExecutionEntity executionEntity = Context.getCommandContext()
                .getExecutionEntityManager()
                .findExecutionById(task.getExecutionId());
        executionEntity.setActivity(getActivity(hisActEnty));
    }

    public ActivityImpl getActivity(HistoricActivityInstanceEntity historicActivityInstanceEntity) {
        ProcessDefinitionEntity processDefinitionEntity = new GetDeploymentProcessDefinitionCmd(
                historicActivityInstanceEntity.getProcessDefinitionId())
                .execute(Context.getCommandContext());

        return processDefinitionEntity.findActivity(historicActivityInstanceEntity.getActivityId());
    }

    /**
     *  判断节点是否为跳过节点
     * @param historyActivityId
     * @return
     */
    public boolean isSkipActivity(String historyActivityId) {
        String historyTaskId = processDao.findTaskIdFromActinstById(historyActivityId);
        HistoricTaskInstanceEntity historicTaskInstanceEntity = Context
                .getCommandContext().getHistoricTaskInstanceEntityManager()
                .findHistoricTaskInstanceById(historyTaskId);
        String deleteReason = historicTaskInstanceEntity.getDeleteReason();
        return "跳过".equals(deleteReason);
    }
}
