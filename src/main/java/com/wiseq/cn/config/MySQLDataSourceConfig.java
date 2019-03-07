package com.wiseq.cn.config;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.beans.PropertyVetoException;

/**
 * 版本        修改时间        作者      修改内容
 * V1.0        ------        jpdong     原始版本
 * 文件说明: Mysql 数据源配置
 **/
@Configuration
@AutoConfigureAfter({CpConfigProperties.class})
public class MySQLDataSourceConfig {
    @Autowired
    private CpConfigProperties cpConfigProperties;

    @Bean(name = "mySQLdataSource")
    public ComboPooledDataSource createDataSource() throws PropertyVetoException {
        ComboPooledDataSource mySQLdataSource = new ComboPooledDataSource();
        mySQLdataSource.setDriverClass(cpConfigProperties.getDriverClass());
        mySQLdataSource.setJdbcUrl(cpConfigProperties.getUrl());
        mySQLdataSource.setUser(cpConfigProperties.getUser());
        mySQLdataSource.setPassword(cpConfigProperties.getPassword());
        mySQLdataSource.setMaxPoolSize(cpConfigProperties.getMaxPoolSize());
        mySQLdataSource.setMinPoolSize(cpConfigProperties.getMinPoolSize());
        mySQLdataSource.setAutoCommitOnClose(cpConfigProperties.isAutoCommitOnClose());
        mySQLdataSource.setCheckoutTimeout(cpConfigProperties.getCheckoutTimeout());
        mySQLdataSource.setAcquireIncrement(cpConfigProperties.getAcquireIncrement());
        mySQLdataSource.setTestConnectionOnCheckin(cpConfigProperties.isTestConnectionOnCheckin());
        mySQLdataSource.setTestConnectionOnCheckout(cpConfigProperties.isTestConnectionOnCheckout());
        mySQLdataSource.setInitialPoolSize(cpConfigProperties.getInitialPoolSize());
        mySQLdataSource.setMaxIdleTime(cpConfigProperties.getMaxIdleTime());
        mySQLdataSource.setIdleConnectionTestPeriod(cpConfigProperties.getIdleConnectionTestPeriod());
        return mySQLdataSource;
    }
}

