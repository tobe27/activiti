package com.wiseq.cn.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * 版本        修改时间        作者        修改内容
 * V1.0        ------        wangyh       原始版本
 * 文件说明: Process
 **/
@Setter
@Getter
public class FlowProcess {
    private String id;
    private String name;
    private String key;
    private String deploymentId;
    private int version;
    private int status;
}
