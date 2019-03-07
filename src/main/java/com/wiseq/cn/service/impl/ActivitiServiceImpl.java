package com.wiseq.cn.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.wiseq.cn.activiti.bpm.RollbackTaskCmd;
import com.wiseq.cn.activiti.bpm.WithdrawTaskCmd;
import com.wiseq.cn.activiti.bpm.generator.CustomProcessDiagramGenerator;
import com.wiseq.cn.commons.utils.QuHelper;
import com.wiseq.cn.dao.FlowFormDao;
import com.wiseq.cn.dao.FlowProcessDao;
import com.wiseq.cn.entity.FlowForm;
import com.wiseq.cn.entity.FlowProcess;
import com.wiseq.cn.entity.FlowTask;
import com.wiseq.cn.service.ActivitiService;
import com.wiseq.cn.utils.FlowUtil;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.Model;
import org.activiti.engine.task.Task;
import org.activiti.image.ProcessDiagramGenerator;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ActivitiServiceImpl implements ActivitiService {

	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private RuntimeService runtimeService;
	@Autowired
	private TaskService taskService;
	@Autowired
	private FormService formService;
	@Autowired
	private ProcessEngine processEngine;
	@Autowired
	private HistoryService historyService;

	@Autowired
	private FlowProcessDao processDao;
	@Autowired
	private FlowFormDao flowFormDao;

	/**
	 *  创建流程模型
	 * @param name   模型名称
	 * @param code   模型编码
	 * @param description  模型描述
	 * @return
	 */
	@Override
	public String createModel(String name, String code, String description) {
		try {
			ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
			RepositoryService repositoryService = processEngine.getRepositoryService();
			ObjectMapper objectMapper = new ObjectMapper();
			ObjectNode editorNode = objectMapper.createObjectNode();
			editorNode.put("id", "canvas");
			editorNode.put("resourceId", "canvas");
			ObjectNode stencilSetNode = objectMapper.createObjectNode();
			stencilSetNode.put("namespace", "http://b3mn.org/stencilset/bpmn2.0#");
			editorNode.put("stencilset", stencilSetNode);
			Model modelData = repositoryService.newModel();

			ObjectNode modelObjectNode = objectMapper.createObjectNode();
			modelObjectNode.put(ModelDataJsonConstants.MODEL_NAME, name);
			modelObjectNode.put(ModelDataJsonConstants.MODEL_REVISION, 1);
			modelObjectNode.put(ModelDataJsonConstants.MODEL_DESCRIPTION, description);
			modelData.setMetaInfo(modelObjectNode.toString());
			modelData.setName(name);
			modelData.setKey(code);
			//保存模型
			repositoryService.saveModel(modelData);
			repositoryService.addModelEditorSource(modelData.getId(), editorNode.toString().getBytes("utf-8"));

			return modelData.getId();
		} catch (Exception e) {
			System.out.println("创建模型失败：");
		}
		return null;
	}

	/**
	 * 获取所有模型
	 * @return
	 */
	@Override
	public Object findModel() {
		RepositoryService repositoryService = processEngine.getRepositoryService();
		return repositoryService.createModelQuery().list();
	}

	/**
	 * 删除模型
	 * @param modelId 模型ID
	 * @return
	 */
	@Override
	public void deleteModel(String modelId) {
		RepositoryService repositoryService = processEngine.getRepositoryService();
		repositoryService.deleteModel(modelId);
	}

	/**
	 * 发布模型为流程定义
	 * @param modelId 模型ID
	 * @return
	 */
	@Override
	public String releaseModel(String modelId) {
		try {
			//获取模型
			RepositoryService repositoryService = processEngine.getRepositoryService();
			Model modelData = repositoryService.getModel(modelId);
			if(modelData != null){
				byte[] bytes = repositoryService.getModelEditorSource(modelData.getId());
				if (bytes == null) {
					return "模型数据为空，请先设计流程并成功保存，再进行发布";
				}
				JsonNode modelNode = new ObjectMapper().readTree(bytes);
				BpmnModel model = new BpmnJsonConverter().convertToBpmnModel(modelNode);
				if(model.getProcesses().size()==0){
					return "数据模型不符要求，请至少设计一条主线流程";
				}

				DeploymentBuilder db = repositoryService.createDeployment().name(modelData.getName());
				List<JsonNode> forms = modelNode.findValues("formkeydefinition");
				for (JsonNode node : forms) {
					String formName = node.textValue();
					if (!QuHelper.isNull(formName)) {
						// 查询表单信息
						List<FlowForm> formLi = flowFormDao.findForm(formName);
						if(formLi.size() > 0){
							ByteArrayInputStream bi = new ByteArrayInputStream(formLi.get(0).getContent().getBytes());
							db.addInputStream(formName, bi);
						}
					}
				}

				//发布流程
				byte[] bpmnBytes = new BpmnXMLConverter().convertToXML(model);
				String processName = modelData.getName() + ".bpmn20.xml";
				Deployment deployment = db.addString(processName, new String(bpmnBytes, "UTF-8"))
						.deploy();
				System.err.println("部署ID：" + deployment.getId());
				modelData.setDeploymentId(deployment.getId());
				repositoryService.saveModel(modelData);
				if (deployment == null || deployment.getId() == null) {
					return "流程发布失败";
				}
			}else{
				return "数据模型不存在";
			}
		}catch (Exception e){
			e.printStackTrace();
		}
		return "流程发布成功";
	}

	/**
	 * 根据部署id删除流程定义
	 */
	@Override
	public void deleteProcess(String deploymentId, boolean flag) {
		repositoryService.deleteDeployment(deploymentId, flag);
	}

	/**
	 *  查询所有流程
	 * @return
	 */
	@Override
	public List<FlowProcess> findProcess() {
		return processDao.findProcess();
	}

	/**
	 * 启动流程
	 * @param processKey 流程定义id
	 */
	@Override
	public String startProcess(String processKey, Map<String, Object> variables) {
		// 添加ID,用作业务数据ID
		String businessKey = FlowUtil.getId();
		variables.put("id", businessKey);
		return runtimeService.startProcessInstanceByKey(processKey, businessKey, variables).getId();
	}

	/**
	 * 查询指定办理人的待办任务（根据任务的创建时间升序排列）
	 * @param assignee  任务办理人
	 */
	@Override
	public List<FlowTask> findTaskByAssignee(String assignee) {
		List<Task> list = taskService.createTaskQuery().taskAssignee(assignee).active().list();
		List<FlowTask> newLi = Lists.newLinkedList();
		list.stream().forEach(task -> {
			FlowTask flowTask = new FlowTask();
			flowTask.setId(task.getId());
			flowTask.setName(task.getName());
			flowTask.setFormKey(task.getFormKey());
			flowTask.setExecutionId(task.getExecutionId());
			flowTask.setCreateTime(new Timestamp(task.getCreateTime().getTime()));
			newLi.add(flowTask);
		});
		return newLi;
	}

	/**
	 *  查询已办任务
	 * @param assignee
	 * @return
	 */
	public List<LinkedHashMap<String,String>> historyTasks(String assignee){
		return processDao.historyTasks(assignee);
	}

	/**
	 *  选择给定用户是候选用户的任务
	 * @param assignee  用户
	 * @return
	 */
	public List<FlowTask> findGroupTask(String assignee){
		String userId = processDao.findByUserName(assignee);
		userId = userId==null?assignee:userId;

		// 查询两次(查询组用户和多用户情况)
		List<Task> list = processEngine.getTaskService().createTaskQuery().taskCandidateUser(userId).list();
		if(list.size() <= 0) list = processEngine.getTaskService().createTaskQuery().taskCandidateUser(assignee).list();

		List<FlowTask> newList = Lists.newArrayList();
		list.stream().forEach(task ->{
			FlowTask newTask = new FlowTask();
			newTask.setId(task.getId());
			newTask.setName(task.getName());
			newTask.setFormKey(task.getFormKey());
			newTask.setExecutionId(task.getExecutionId());
			newTask.setCreateTime(new Timestamp(task.getCreateTime().getTime()));
			newList.add(newTask);
		});
		return newList;
	}

	/**
	 * 认领任务
	 * @param taskId  任务ID
	 * @param assignee  用户
	 */
	@Override
	public void claimTask(String taskId, String assignee) {
		taskService.claim(taskId, assignee);
	}

	/**
	 *  释放任务
	 * @param taskId 任务ID
	 */
	public void releaseTask(String taskId){
		taskService.setAssignee(taskId, null);
	}

	/**
	 * 完成任务
	 * @param taskId 任务ID
	 * @param variables 参数变量
	 */
	@Override
	public void completeTask(String taskId, Map<String, Object> variables) {
		System.err.println(variables);
		taskService.complete(taskId, variables);
	}

	/**
	 *  协办任务(跳过)
	 * @param taskId  任务ID
	 * @param userId  协办人
	 * @param variables 流程参数
	 */
	public void assistTask(String taskId, String userId, Map<String, Object> variables){
		// 委托
		taskService.delegateTask(taskId, userId);
		// 转变
		taskService.resolveTask(taskId, variables);
		// 办理
		taskService.complete(taskId, variables);
	}

	/**
	 *  删除任务
	 * @param executionId   流程实例ID
	 * @param deleteReason  删除原因
	 */
	public void deleteTask(String executionId, String deleteReason){
		runtimeService.deleteProcessInstance(executionId, deleteReason);
	}

	/**
	 *  转办任务
	 * @param taskId  任务ID
	 * @param assignee  转办人
	 */
	public void forwardTask(String taskId, String assignee){
		processEngine.getTaskService().setAssignee(taskId, assignee);
	}

	/**
	 *  任务回退
	 * @param taskId  任务ID
	 * @param activityId  回退目标节点ID
	 * @param userId  任务执行人
	 */
	public void fallBackTask(String taskId, String activityId, String userId){
		Command<Object> cmd = new RollbackTaskCmd(taskId, activityId, userId);
		processEngine.getManagementService().executeCommand(cmd);
	}

	/**
	 *  撤回已提交任务
	 * @param hisTaskId 历史任务ID
	 */
	public void withdraw(String hisTaskId){
		Command<Integer> cmd = new WithdrawTaskCmd(hisTaskId);
		processEngine.getManagementService().executeCommand(cmd);
	}

	/**
	 * 挂起流程
	 * @param processKey 流程定义Key
	 */
	@Override
	public void hangProcess(String processKey) {
		repositoryService.suspendProcessDefinitionByKey(processKey);
	}

	/**
	 * 激活挂起的流程
	 * @param processKey 流程定义Key
	 */
	@Override
	public void activateProcess(String processKey) {
		repositoryService.activateProcessDefinitionByKey(processKey);
	}

	/**
	 * 挂起任务(流程实例)
	 * @param executionId 流程实例ID
	 */
	@Override
	public void hangTask(String executionId) {
		runtimeService.suspendProcessInstanceById(executionId);
	}

	/**
	 * 激活挂起的任务(流程实例)
	 * @param executionId 流程实例ID
	 */
	@Override
	public void activateTask(String executionId) {
		runtimeService.activateProcessInstanceById(executionId);
	}

	/**
	 *  流程跟踪
	 * @param executionId 流程实例ID
	 * @return
	 */
	public String flowTrace(String executionId) {
		//获取历史流程实例
		HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(executionId).singleResult();
		//获取流程图
		BpmnModel bpmnModel = repositoryService.getBpmnModel(processInstance.getProcessDefinitionId());
		ProcessEngineConfiguration processEngineConfiguration = processEngine.getProcessEngineConfiguration();
		Context.setProcessEngineConfiguration((ProcessEngineConfigurationImpl) processEngineConfiguration);
		ProcessDefinitionEntity definitionEntity = (ProcessDefinitionEntity)repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId());
		List<HistoricActivityInstance> highLightedActivitList = historyService.createHistoricActivityInstanceQuery().processInstanceId(executionId).list();
		//高亮环节id集合
		List<String> highLightedActivitis = new ArrayList<>();
		//高亮线路id集合
		List<String> highLightedFlows = getHighLightedFlows(definitionEntity,highLightedActivitList);
		for(HistoricActivityInstance tempActivity : highLightedActivitList){
			String activityId = tempActivity.getActivityId();
			highLightedActivitis.add(activityId);
		}

		CustomProcessDiagramGenerator generator = new CustomProcessDiagramGenerator();
		Color[] colors = {new Color(0,128,0),new Color(255, 0, 0)};
		InputStream is = generator.generateDiagram(bpmnModel, "png", highLightedActivitis, highLightedFlows,"宋体","宋体","宋体",null,1.0D, colors);

		// 对图片数据进行Base64编码处理
		byte[] data = null;
		// 读取图片字节数组
		try {
			ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
			byte[] buff = new byte[100];
			int rc;
			while ((rc = is.read(buff, 0, 100)) > 0) {
				swapStream.write(buff, 0, rc);
			}
			data = swapStream.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return "data:image/png;base64," + new String(Base64.encodeBase64(data));
	}


	/**
	 * 获取需要高亮的线
	 * @param processDefinitionEntity
	 * @param historicActivityInstances
	 * @return
	 */
	private List<String> getHighLightedFlows( ProcessDefinitionEntity processDefinitionEntity, List<HistoricActivityInstance> historicActivityInstances) {
		List<String> highFlows = new ArrayList<>();
		// 用以保存高亮的线flowId
		for (int i = 0; i < historicActivityInstances.size() - 1; i++) {
			// 对历史流程节点进行遍历
			ActivityImpl activityImpl = processDefinitionEntity .findActivity(historicActivityInstances.get(i) .getActivityId());
			// 得到节点定义的详细信息
			List<ActivityImpl> sameStartTimeNodes = new ArrayList<>();
			// 用以保存后需开始时间相同的节点
			ActivityImpl sameActivityImpl1 = processDefinitionEntity .findActivity(historicActivityInstances.get(i + 1) .getActivityId());
			// 将后面第一个节点放在时间相同节点的集合里
			sameStartTimeNodes.add(sameActivityImpl1);
			for (int j = i + 1; j < historicActivityInstances.size() - 1; j++) {
				HistoricActivityInstance activityImpl1 = historicActivityInstances .get(j);
				// 后续第一个节点
				HistoricActivityInstance activityImpl2 = historicActivityInstances .get(j + 1);
				// 后续第二个节点
				if (activityImpl1.getStartTime().equals( activityImpl2.getStartTime())) {
					// 如果第一个节点和第二个节点开始时间相同保存
					ActivityImpl sameActivityImpl2 = processDefinitionEntity .findActivity(activityImpl2.getActivityId());
					sameStartTimeNodes.add(sameActivityImpl2);
				} else {
					// 有不相同跳出循环 break;
					}
			}
			List<PvmTransition> pvmTransitions = activityImpl .getOutgoingTransitions();
			// 取出节点的所有出去的线
			for (PvmTransition pvmTransition : pvmTransitions) {
				// 对所有的线进行遍历
				ActivityImpl pvmActivityImpl = (ActivityImpl) pvmTransition .getDestination();
				// 如果取出的线的目标节点存在时间相同的节点里，保存该线的id，进行高亮显示
				if (sameStartTimeNodes.contains(pvmActivityImpl)) {
					highFlows.add(pvmTransition.getId());
				}
			}
		}
		return highFlows;
	}


	/**
	 * 流程图XML查看
	 * @param processId 流程定义ID
	 */
	public String flowXml(String processId){
		ProcessDefinitionEntity process = (ProcessDefinitionEntity) repositoryService.getProcessDefinition(processId);
		InputStream is = repositoryService.getResourceAsStream(process.getDeploymentId(), process.getResourceName());
		return new BufferedReader(new InputStreamReader(is)).lines()
				.parallel()
				.collect(Collectors.joining(System.lineSeparator()));
	}

	/**
	 * 根据流程ID查询开始表单
	 * @param processId 流程定义ID
	 * @return
	 */
	public String findStartForm(String processId){
		String formKey = formService.getStartFormKey(processId);
		if(QuHelper.isNull(formKey)){
			//如果启动表单为空直接启动流程
		}
		return formKey;
	}

	/**
	 *  获取Task表单
	 * @param taskId  任务ID
	 * @return
	 */
	public Object findTaskForm(String taskId){
		return formService.getRenderedTaskForm(taskId);
	}

	/**
	 *  查询流程所有节点
	 * @param processId  流程定义ID
	 * @return
	 */
	public List<Map<String,String>> findNode(String processId){
		BpmnModel model = repositoryService.getBpmnModel(processId);
		List<Map<String,String>> list = Lists.newArrayList();
		if(model != null){
			Collection<FlowElement> flowElements = model.getMainProcess().getFlowElements();
			for(FlowElement e : flowElements) {
				// 过滤节点名称为空和连线
				if(!QuHelper.isNull(e.getName()) && !"SequenceFlow".equals(e.getClass().getSimpleName())){
					Map<String,String> map = new HashMap<>();
					map.put("id",e.getId());
					map.put("name",e.getName());
					map.put("documentation",e.getDocumentation());
					list.add(map);
				}
			}
		}
		return list;
	}

	/**
	 *  查询流程当前运行的实例
	 * @param processId  流程定义ID
	 * @return
	 */
	public List<LinkedHashMap<String,String>> running(String processId){
		return processDao.findRunning(processId);
	}

	/**
	 *  查询历史任务历史节点
	 * @param hisTaskId   历史任务ID
	 * @return
	 */
	public String findHistoryNodeId(String hisTaskId){
		return processDao.findHistoryNodeId(hisTaskId);
	}

}