package com.wiseq.cn.activiti.bpm;

import java.util.*;

import com.wiseq.cn.commons.utils.SpringUtil;
import com.wiseq.cn.dao.FlowProcessDao;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.form.StartFormData;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.impl.HistoricActivityInstanceQueryImpl;
import org.activiti.engine.impl.HistoricTaskInstanceQueryImpl;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.cmd.GetDeploymentProcessDefinitionCmd;
import org.activiti.engine.impl.cmd.GetStartFormCmd;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.activiti.engine.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.task.TaskDefinition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 退回任务.
 */
public class RollbackTaskCmd implements Command<Object> {

    /** task id. */
    private String taskId;

    /** activity id. */
    private String activityId;

    /** user id. */
    private String userId;

    /** use last assignee. */
    private boolean useLastAssignee = false;

    /** 需要处理的多实例节点. */
    private Set<String> multiInstanceExecutionIds = new HashSet<String>();

    private FlowProcessDao processDao = SpringUtil.getBean(FlowProcessDao.class);

    /**
     * 指定taskId和跳转到的activityId，自动使用最后的assignee.
     */
    public RollbackTaskCmd(String taskId, String activityId) {
        this.taskId = taskId;
        this.activityId = activityId;
        this.useLastAssignee = true;
    }

    /**
     * 指定taskId和跳转到的activityId, userId.
     */
    public RollbackTaskCmd(String taskId, String activityId, String userId) {
        this.taskId = taskId;
        this.activityId = activityId;
        this.userId = userId;
    }

    /**
     * 退回流程.
     * 
     * @return 0-退回成功 1-流程结束 2-下一结点已经通过,不能退回
     */
    public Integer execute(CommandContext commandContext) {
        // 获得任务
        TaskEntity taskEntity = findTask(commandContext);
        // 找到想要回退到的节点
        ActivityImpl targetActivity = findTargetActivity(commandContext, taskEntity);

        String type = (String) targetActivity.getProperty("type");
        if ("userTask".equals(type)) {
            rollbackUserTask(commandContext);
        } else if ("startEvent".equals(type)) {
            rollbackStartEvent(commandContext);
        } else {
            throw new IllegalStateException("cannot rollback " + type);
        }
        return 0;
    }

    /**
     * 回退到userTask.
     */
    public Integer rollbackUserTask(CommandContext commandContext) {
        // 获得任务
        TaskEntity taskEntity = findTask(commandContext);
        // 找到想要回退到的节点
        ActivityImpl targetActivity = this.findTargetActivity(commandContext, taskEntity);
        // 找到想要回退对应的节点历史
        HistoricActivityInstanceEntity historicActivityInstanceEntity = findTargetHistoricActivity(commandContext, taskEntity, targetActivity);
        // 找到想要回退对应的任务历史
        HistoricTaskInstanceEntity historicTaskInstanceEntity = findTargetHistoricTask(commandContext, taskEntity, targetActivity);

        Graph graph = new ActivitiHistoryGraphBuilder(historicTaskInstanceEntity.getProcessInstanceId()).build();
        Node node = graph.findById(historicActivityInstanceEntity.getId());
        // 检查是否可以回退
        if (!checkCouldRollback(node)) {
            System.err.println("任务不可回退");
            return 2;
        }
        // 检查是否是同一分支
        if (isSameBranch(historicTaskInstanceEntity)) {
            // 如果退回的目标节点的executionId与当前task的executionId一样，说明是同一个分支
            // 只删除当前分支的task
            TaskEntity targetTaskEntity = Context.getCommandContext().getTaskEntityManager().findTaskById(taskId);
            deleteActiveTask(targetTaskEntity);
        } else {
            // 否则认为是从分支跳回主干
            // 删除所有活动中的task
            deleteActiveTasks(historicTaskInstanceEntity.getProcessInstanceId());
            // 获得期望退回的节点后面的所有节点历史
            List<String> historyNodeIds = new ArrayList<String>();
            collectNodes(node, historyNodeIds);
            deleteHistoryActivities(historyNodeIds);
        }
        // 处理多实例
        processMultiInstance();
        // 恢复期望退回的任务和历史
        processHistoryTask(commandContext, taskEntity, historicTaskInstanceEntity, historicActivityInstanceEntity);
        return 0;
    }

    /**
     * 回退到startEvent.
     */
    public Integer rollbackStartEvent(CommandContext commandContext) {
        // 获得任务
        TaskEntity taskEntity = findTask(commandContext);
        // 找到想要回退到的节点
        ActivityImpl targetActivity = findTargetActivity(commandContext, taskEntity);
        // 判断是否是同一个分支
        if (taskEntity.getExecutionId().equals(taskEntity.getProcessInstanceId())) {
            // 如果退回的目标节点的executionId与当前task的executionId一样，说明是同一个分支
            // 只删除当前分支的task
            TaskEntity targetTaskEntity = Context.getCommandContext().getTaskEntityManager().findTaskById(taskId);
            deleteActiveTask(targetTaskEntity);
        } else {
            // 否则认为是从分支跳回主干
            // 删除所有活动中的task
            deleteActiveTasks(taskEntity.getProcessInstanceId());
        }
        // 把流程指向任务对应的节点
        ExecutionEntity executionEntity = Context.getCommandContext()
                .getExecutionEntityManager()
                .findExecutionById(taskEntity.getExecutionId());
        executionEntity.setActivity(targetActivity);
        // 创建HistoricActivityInstance
        Context.getCommandContext().getHistoryManager().recordActivityStart(executionEntity);
        // 处理多实例
        processMultiInstance();
        return 0;
    }

    /**
     * 获得当前任务.
     */
    public TaskEntity findTask(CommandContext commandContext) {
        TaskEntity taskEntity = commandContext.getTaskEntityManager().findTaskById(taskId);
        return taskEntity;
    }

    /**
     * 查找回退的目的节点.
     */
    public ActivityImpl findTargetActivity(CommandContext commandContext, TaskEntity taskEntity) {
        if (activityId == null) {
            String historyTaskId = findNearestUserTask(commandContext);
            HistoricTaskInstanceEntity historicTaskInstanceEntity = commandContext
                    .getHistoricTaskInstanceEntityManager()
                    .findHistoricTaskInstanceById(historyTaskId);
            activityId = historicTaskInstanceEntity.getTaskDefinitionKey();
        }
        System.err.println("activityId:" + activityId);
        String processDefinitionId = taskEntity.getProcessDefinitionId();
        ProcessDefinitionEntity processDefinitionEntity = new GetDeploymentProcessDefinitionCmd(
                processDefinitionId).execute(commandContext);

        return processDefinitionEntity.findActivity(activityId);
    }

    /**
     * 找到想要回退对应的节点历史.
     */
    public HistoricActivityInstanceEntity findTargetHistoricActivity(
            CommandContext commandContext, TaskEntity taskEntity, ActivityImpl activityImpl) {

        HistoricActivityInstanceQueryImpl hisActQueryImpl = new HistoricActivityInstanceQueryImpl();
        hisActQueryImpl.activityId(activityImpl.getId());
        hisActQueryImpl.processInstanceId(taskEntity.getProcessInstanceId());
        hisActQueryImpl.orderByHistoricActivityInstanceEndTime().desc();
        HistoricActivityInstanceEntity historicActivityInstanceEntity = (HistoricActivityInstanceEntity) commandContext
                .getHistoricActivityInstanceEntityManager()
                .findHistoricActivityInstancesByQueryCriteria(hisActQueryImpl, new Page(0, 1))
                .get(0);
        return historicActivityInstanceEntity;
    }

    /**
     * 找到想要回退对应的任务历史.
     */
    public HistoricTaskInstanceEntity findTargetHistoricTask(
            CommandContext commandContext, TaskEntity taskEntity, ActivityImpl activityImpl) {

        HistoricTaskInstanceQueryImpl historicTaskInstanceQueryImpl = new HistoricTaskInstanceQueryImpl();
        historicTaskInstanceQueryImpl.taskDefinitionKey(activityImpl.getId());
        historicTaskInstanceQueryImpl.processInstanceId(taskEntity.getProcessInstanceId());
        historicTaskInstanceQueryImpl.setFirstResult(0);
        historicTaskInstanceQueryImpl.setMaxResults(1);
        historicTaskInstanceQueryImpl.orderByTaskCreateTime().desc();

        HistoricTaskInstanceEntity historicTaskInstanceEntity = (HistoricTaskInstanceEntity) commandContext
                .getHistoricTaskInstanceEntityManager()
                .findHistoricTaskInstancesByQueryCriteria(historicTaskInstanceQueryImpl)
                .get(0);
        return historicTaskInstanceEntity;
    }

    /**
     * 判断想要回退的目标节点和当前节点是否在一个分支上.
     */
    public boolean isSameBranch(HistoricTaskInstanceEntity historicTaskInstanceEntity) {
        TaskEntity taskEntity = Context.getCommandContext().getTaskEntityManager().findTaskById(taskId);
        return taskEntity.getExecutionId().equals(historicTaskInstanceEntity.getExecutionId());
    }

    /**
     * 查找离当前节点最近的上一个userTask.
     */
    public String findNearestUserTask(CommandContext commandContext) {
        // 获得任务
        TaskEntity taskEntity = findTask(commandContext);
        if (taskEntity != null) {
            Graph graph = new ActivitiHistoryGraphBuilder(taskEntity.getProcessInstanceId()).build();
            Node node = graph.findById(processDao.findHistoryNodeId(taskId));
            String previousHistoricActivityInstanceId = findIncomingNode(graph, node);
            if (previousHistoricActivityInstanceId != null) {
                return processDao.findTaskIdFromActinstById(previousHistoricActivityInstanceId);
            }
        }
        return null;
    }

    /**
     * 查找进入的连线.
     */
    public String findIncomingNode(Graph graph, Node node) {
        for (Edge edge : graph.getEdges()) {
            Node src = edge.getSrc();
            Node dest = edge.getDest();
            String srcType = src.getType();

            if (!dest.getId().equals(node.getId())) continue;
            // 判断是否为用户任务
            if ("userTask".equals(srcType)) {
                return src.getId();
            } else if (srcType.endsWith("Gateway")) {
                // 如果为网关则继续递归
                return this.findIncomingNode(graph, src);
            }
        }
        return null;
    }

    /**
     * 判断是否可回退.
     */
    public boolean checkCouldRollback(Node node) {
        for (Edge edge : node.getOutgoingEdges()) {
            Node dest = edge.getDest();
            String type = dest.getType();
            // 判断是否为用户任务
            if ("userTask".equals(type)) {
                // 判断是否为活动节点
                if (!dest.isActive()) {
                    return true;
                }
            } else if (type.endsWith("Gateway")) {
                // 如果为网关则继续递归
                return checkCouldRollback(dest);
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * 删除活动状态任务.
     */
    public void deleteActiveTasks(String processInstanceId) {
        List<TaskEntity> taskEntities = Context.getCommandContext()
                .getTaskEntityManager().findTasksByProcessInstanceId(processInstanceId);
        for (TaskEntity taskEntity : taskEntities) {
            deleteActiveTask(taskEntity);
        }
    }

    /**
     * 遍历节点.
     */
    public void collectNodes(Node node, List<String> historyNodeIds) {
        for (Edge edge : node.getOutgoingEdges()) {
            Node dest = edge.getDest();
            historyNodeIds.add(dest.getId());
            collectNodes(dest, historyNodeIds);
        }
    }

    /**
     * 删除历史节点
     * @param historyNodeIds 历史节点id
     */
    public void deleteHistoryActivities(List<String> historyNodeIds) {
        historyNodeIds.stream().forEach(id -> processDao.deleteHistoryActivitie(id));
    }

    /**
     * 根据任务历史，创建待办任务.
     */
    public void processHistoryTask(CommandContext commandContext,TaskEntity taskEntity,
            HistoricTaskInstanceEntity historicTaskInstanceEntity, HistoricActivityInstanceEntity historicActivityInstanceEntity) {
        // 判断受让人是否为空
        if (userId == null) {
            if (useLastAssignee) {
                userId = historicTaskInstanceEntity.getAssignee();
            } else {
                String processDefinitionId = taskEntity.getProcessDefinitionId();
                ProcessDefinitionEntity processDefinitionEntity = new GetDeploymentProcessDefinitionCmd(processDefinitionId).execute(commandContext);
                TaskDefinition taskDefinition = processDefinitionEntity.getTaskDefinitions().get(historicTaskInstanceEntity.getTaskDefinitionKey());

                if (taskDefinition == null) {
                    throw new IllegalStateException("任务定义未找到");
                }
                // 判断任务受让人是否为空
                if (taskDefinition.getAssigneeExpression() != null) {
                    userId = (String) taskDefinition.getAssigneeExpression().getValue(taskEntity);
                }
            }
        }

        // 创建新任务
        TaskEntity task = TaskEntity.create(new Date());
        task.setProcessDefinitionId(historicTaskInstanceEntity.getProcessDefinitionId());
        task.setAssigneeWithoutCascade(userId);
        task.setParentTaskIdWithoutCascade(historicTaskInstanceEntity.getParentTaskId());
        task.setNameWithoutCascade(historicTaskInstanceEntity.getName());
        task.setTaskDefinitionKey(historicTaskInstanceEntity.getTaskDefinitionKey());
        task.setExecutionId(historicTaskInstanceEntity.getExecutionId());
        task.setPriority(historicTaskInstanceEntity.getPriority());
        task.setProcessInstanceId(historicTaskInstanceEntity.getProcessInstanceId());
        task.setExecutionId(historicTaskInstanceEntity.getExecutionId());
        task.setDescriptionWithoutCascade(historicTaskInstanceEntity.getDescription());
        task.setTenantId(historicTaskInstanceEntity.getTenantId());

        Context.getCommandContext().getTaskEntityManager().insert(task);

        // 把流程指向任务对应的节点
        ExecutionEntity executionEntity = Context.getCommandContext()
                .getExecutionEntityManager()
                .findExecutionById(historicTaskInstanceEntity.getExecutionId());
        executionEntity.setActivity(getActivity(historicActivityInstanceEntity));

        // 创建HistoricActivityInstance
        Context.getCommandContext().getHistoryManager().recordActivityStart(executionEntity);

        // 创建HistoricTaskInstance
        Context.getCommandContext().getHistoryManager().recordTaskCreated(task, executionEntity);
        Context.getCommandContext().getHistoryManager().recordTaskId(task);
        // 更新ACT_HI_ACTIVITY里的assignee字段
        Context.getCommandContext().getHistoryManager().recordTaskAssignment(task);
    }

    /**
     * 获得历史节点对应的节点信息.
     */
    public ActivityImpl getActivity(HistoricActivityInstanceEntity historicActivityInstanceEntity) {
        ProcessDefinitionEntity processDefinitionEntity = new GetDeploymentProcessDefinitionCmd(
                historicActivityInstanceEntity.getProcessDefinitionId())
                .execute(Context.getCommandContext());
        return processDefinitionEntity.findActivity(historicActivityInstanceEntity.getActivityId());
    }

    /**
     * 删除未完成任务.
     */
    public void deleteActiveTask(TaskEntity taskEntity) {
        ProcessDefinitionEntity processDefinitionEntity = new GetDeploymentProcessDefinitionCmd(
                taskEntity.getProcessDefinitionId()).execute(Context
                .getCommandContext());

        ActivityImpl activityImpl = processDefinitionEntity.findActivity(taskEntity.getTaskDefinitionKey());
        // 判断是否为会签任务
        if (isMultiInstance(activityImpl)) {
            multiInstanceExecutionIds.add(taskEntity.getExecution().getParent().getId());
        }
        Context.getCommandContext().getTaskEntityManager().deleteTask(taskEntity, "回退", false);

        List<LinkedHashMap<String, Object>> list = processDao.findHisActiveTask(taskId);
        Date now = new Date();
        for (Map<String, Object> map : list) {
            Date startTime = (Date) map.get("START_TIME_");
            long duration = now.getTime() - startTime.getTime();
            processDao.updateHisTask(now, duration, map.get("ID_").toString());
        }
    }

    /**
     * 判断是否会签.
     */
    public boolean isMultiInstance(PvmActivity pvmActivity) {
        return pvmActivity.getProperty("multiInstance") != null;
    }

    /**
     * 处理多实例.
     */
    public void processMultiInstance() {
        for (String executionId : multiInstanceExecutionIds) {
            ExecutionEntity parent = Context.getCommandContext().getExecutionEntityManager().findExecutionById(executionId);
            List<ExecutionEntity> children = Context.getCommandContext()
                    .getExecutionEntityManager()
                    .findChildExecutionsByParentExecutionId(parent.getId());

            for (ExecutionEntity executionEntity : children) {
                executionEntity.remove();
            }
            parent.remove();
        }
    }
}
