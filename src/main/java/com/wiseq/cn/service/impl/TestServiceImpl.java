package com.wiseq.cn.service.impl;

import com.wiseq.cn.business.BusinessTest;
import com.wiseq.cn.dao.BusinessTestDao;
import com.wiseq.cn.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 版本        修改时间        作者        修改内容
 * V1.0        ------        wangyh       原始版本
 * 文件说明: TestServiceImpl
 **/
@Service
public class TestServiceImpl implements TestService {

    @Autowired
    BusinessTestDao businessTestDao;

    @Override
    public void save(BusinessTest businessTest) {
        businessTestDao.insert(businessTest);
    }

    @Override
    public void update(BusinessTest businessTest) {
        businessTest.setStatus("1");
        businessTestDao.update(businessTest);
    }

    @Override
    public void updateRs(BusinessTest businessTest) {
        businessTest.setStatus("2");
        businessTestDao.update(businessTest);
    }

    @Override
    public void updateBs(BusinessTest businessTest) {
        businessTest.setStatus("3");
        businessTestDao.update(businessTest);
    }

    @Override
    public void updateFz(BusinessTest businessTest) {
        businessTest.setStatus("4");
        businessTestDao.update(businessTest);
    }
}
