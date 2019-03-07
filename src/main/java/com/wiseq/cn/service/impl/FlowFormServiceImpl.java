package com.wiseq.cn.service.impl;

import com.wiseq.cn.dao.FlowFormDao;
import com.wiseq.cn.entity.FlowForm;
import com.wiseq.cn.service.FlowFormService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 版本        修改时间        作者        修改内容
 * V1.0        ------        wangyh       原始版本
 * 文件说明: FormServiceImpl
 **/
@Service
public class FlowFormServiceImpl implements FlowFormService {

    @Autowired
    FlowFormDao flowFormDao;

    /**
     *  保存表单
     * @param flowForm
     */
    @Override
    public Boolean saveForm(FlowForm flowForm) {
        int num = 0;
        if(flowForm.getId() == null){
            num = flowFormDao.insert(flowForm);
        }else{
            num = flowFormDao.update(flowForm);
        }
        return num>0?true:false;
    }

    /**
     *  查询表单
     * @param code
     */
    @Override
    public List<FlowForm> findForm(String code) {
        return flowFormDao.findForm(code);
    }

    /**
     * 删除表单
     * @param id
     */
    @Override
    public Boolean removeForm(Integer id) {
        return flowFormDao.delete(id)>0?true:false;
    }
}
