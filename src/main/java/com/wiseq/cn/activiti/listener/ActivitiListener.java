package com.wiseq.cn.activiti.listener;

import com.wiseq.cn.commons.utils.QuHelper;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.impl.el.FixedValue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 版本        修改时间        作者        修改内容
 * V1.0        ------        wangyh       原始版本
 * 文件说明: ActivitiListener
 **/
public class ActivitiListener implements ExecutionListener, TaskListener {

    public FixedValue method;
    public Map<String, Object> variables;

    private DelegateExecution delegateE;
    private DelegateTask delegateT;

    /**
     *  流程监听器
     * @param execution   委托执行
     */
    @Override
    public void notify(DelegateExecution execution) {
        delegateE = execution;
        variables = execution.getVariables();
        execute();
    }

    /**
     * 任务监听器
     * @param delegateTask   委托任务
     */
    @Override
    public void notify(DelegateTask delegateTask) {
        delegateT = delegateTask;
        variables = delegateTask.getVariables();
        execute();
    }

    /**
     *  执行
     */
    private void execute() {
        try {
            String methodName = method.getExpressionText();
            if(!QuHelper.isNull(methodName)){
                Method method = this.getClass().getMethod(methodName);
                method.invoke(this);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void setExeVar(String key, Object value){
        delegateE.setVariable(key, value);
    }

    public void setTaskVar(String key, Object value){
        delegateT.setVariable(key, value);
    }

    /**
     *  表单数据转到Bean
     * @param clas
     * @return
     */
    public Object parseFormData(Class clas){
        try {
            Object obj = clas.newInstance();
            Field[] fields = clas.getDeclaredFields();
            for (int i=0; i<fields.length; i++){
                Field field = fields[i];
                field.setAccessible(true);
                field.set(obj, variables.get(field.getName()));
            }
            return obj;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     *  根据key获取数据
     * @param key
     * @return
     */
    public Object getDataByName(String key){
        return variables.get(key);
    }
}
