package com.wiseq.cn.service;

import com.wiseq.cn.business.BusinessTest;

/**
 * 版本        修改时间        作者        修改内容
 * V1.0        ------        wangyh       原始版本
 * 文件说明: TestService
 **/
public interface TestService {
    void save(BusinessTest businessTest);
    void update(BusinessTest businessTest);
    void updateRs(BusinessTest businessTest);
    void updateBs(BusinessTest businessTest);
    void updateFz(BusinessTest businessTest);
}
