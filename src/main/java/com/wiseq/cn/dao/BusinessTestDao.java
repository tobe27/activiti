package com.wiseq.cn.dao;

import com.wiseq.cn.business.BusinessTest;
import com.wiseq.cn.entity.FlowForm;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 版本        修改时间        作者        修改内容
 * V1.0        ------        wangyh       原始版本
 * 文件说明: MetaFieldDao
 **/
public interface BusinessTestDao {
    /**
     * 保存表单
     * @param businessTest  表单实体
     * @return
     */
    Integer insert(BusinessTest businessTest);

    /**
     * 修改表单
     * @param businessTest  表单实体
     * @return
     */
    Integer update(BusinessTest businessTest);

    /**
     * 查询表单
     * @param id 表单编码
     * @return
     */
    List<FlowForm> find(@Param(value = "id") String id);
}
