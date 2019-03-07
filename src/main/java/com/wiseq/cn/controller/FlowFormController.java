package com.wiseq.cn.controller;

import com.wiseq.cn.commons.entity.Result;
import com.wiseq.cn.commons.enums.ResultEnum;
import com.wiseq.cn.commons.utils.ResultUtils;
import com.wiseq.cn.entity.FlowForm;
import com.wiseq.cn.service.FlowFormService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 版本        修改时间        作者        修改内容
 * V1.0        ------        wangyh       原始版本
 * 文件说明: FlowFormController
 **/
@RestController
@RequestMapping(value = "/flow")
@Api(description = "流程表单接口")
public class FlowFormController {

    @Autowired
    FlowFormService flowFormService;

    /**
     *  保存表单
     * @param flowForm
     */
    @PostMapping(value = "/saveForm")
    public Result save(FlowForm flowForm){
        if (flowFormService.saveForm(flowForm)) {
            return ResultUtils.success();
        }
        return ResultUtils.error(ResultEnum.FAILURE);
    }

    /**
     *  查询表单
     * @param code
     */
    @GetMapping(value = "/findForm")
    public Result find(@RequestParam(value = "code", required = false) String code){
        return ResultUtils.success(flowFormService.findForm(code));
    }

    /**
     * 删除表单
     * @param id
     */
    @DeleteMapping(value = "/removeForm")
    public Result remove(@RequestParam(value = "id") Integer id){
        if (flowFormService.removeForm(id)) {
            return ResultUtils.success();
        }
        return ResultUtils.error(ResultEnum.FAILURE);
    }
}
