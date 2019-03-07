package com.wiseq.cn.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * 版本        修改时间        作者        修改内容
 * V1.0        ------        wangyh       原始版本
 * 文件说明: FlowForm
 **/
@Setter
@Getter
public class FlowForm {
    private Integer id;
    private String name;
    private String code;
    private String content;
}
