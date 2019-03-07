package com.wiseq.cn.dao;

import com.wiseq.cn.entity.FlowForm;
import com.wiseq.cn.entity.FlowTask;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 版本        修改时间        作者        修改内容
 * V1.0        ------        wangyh       原始版本
 * 文件说明: MetaFieldDao
 **/
public interface FlowFormDao {
    /**
     * 保存表单
     * @param flowForm  表单实体
     * @return
     */
    Integer insert(FlowForm flowForm);

    /**
     * 修改表单
     * @param flowForm  表单实体
     * @return
     */
    Integer update(FlowForm flowForm);

    /**
     * 查询表单
     * @param code 表单编码
     * @return
     */
    List<FlowForm> findForm(@Param(value="code") String code);

    /**
     *  删除表单
     * @param id 表单ID
     * @return
     */
    Integer delete(Integer id);
}
