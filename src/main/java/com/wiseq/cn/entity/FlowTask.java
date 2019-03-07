package com.wiseq.cn.entity;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

/**
 * 版本        修改时间        作者        修改内容
 * V1.0        ------        wangyh       原始版本
 * 文件说明: FlowTask
 **/
@Setter
@Getter
public class FlowTask {
    private String id;
    private String name;
    private String formKey;
    private String executionId;
    private Timestamp createTime;
    private int status;
}
