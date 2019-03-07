package com.wiseq.cn.activiti.bpm.generator;

import java.awt.Color;
import java.io.InputStream;
import java.util.List;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.image.ProcessDiagramGenerator;

public interface CustomProcessDiagramGeneratorI extends ProcessDiagramGenerator {
    InputStream generateDiagram(BpmnModel bpmnModel, String imageType, List<String> highLightedActivities,
            List<String> highLightedFlows, String activityFontName, String labelFontName, String annotationFontName,
            ClassLoader customClassLoader, double scaleFactor, Color[] colors);
}