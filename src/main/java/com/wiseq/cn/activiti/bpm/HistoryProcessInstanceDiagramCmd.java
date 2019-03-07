package com.wiseq.cn.activiti.bpm;

import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;

import java.io.InputStream;

public class HistoryProcessInstanceDiagramCmd implements Command<InputStream> {
    protected String historyProcessInstanceId;

    public HistoryProcessInstanceDiagramCmd(String historyProcessInstanceId) {
        this.historyProcessInstanceId = historyProcessInstanceId;
    }

    @Override
    public InputStream execute(CommandContext commandContext) {
        try {
            FlowchartGenerator customProcessDiagramGenerator = new FlowchartGenerator();

            return customProcessDiagramGenerator.generateDiagram(historyProcessInstanceId);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
