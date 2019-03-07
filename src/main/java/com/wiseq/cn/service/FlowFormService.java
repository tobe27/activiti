package com.wiseq.cn.service;

import com.wiseq.cn.entity.FlowForm;

import java.util.List;

/**
 * 版本        修改时间        作者        修改内容
 * V1.0        ------        wangyh       原始版本
 * 文件说明: FormService
 **/
public interface FlowFormService {
    /**
     *  保存表单
     * @param flowForm
     */
    Boolean saveForm(FlowForm flowForm);

    /**
     *  查询表单
     * @param code
     */
    List<FlowForm> findForm(String code);

    /**
     * 删除表单
     * @param id
     */
    Boolean removeForm(Integer id);
}
