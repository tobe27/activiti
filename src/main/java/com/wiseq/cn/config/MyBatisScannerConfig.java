package com.wiseq.cn.config;

import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;

import javax.annotation.Resource;
import javax.sql.DataSource;

/**
 * 版本        修改时间        作者      修改内容
 * V1.0        ------        jpdong     原始版本
 * 文件说明: mybatis scanner dao file
 **/
@Configuration
@AutoConfigureAfter({SessionFactoryConfiguration.class})
@EnableTransactionManagement
public class MyBatisScannerConfig implements TransactionManagementConfigurer
{
    @Resource(name="mySQLdataSource")
    private DataSource mySQLdataSource;
    @Bean
    public static MapperScannerConfigurer MapperScannerConfigurer() {
        MapperScannerConfigurer mapperScannerConfigurer = new MapperScannerConfigurer();
        mapperScannerConfigurer.setSqlSessionFactoryBeanName("sqlSessionFactory");
        mapperScannerConfigurer.setBasePackage("com.wiseq.cn.dao");
        return mapperScannerConfigurer;
    }

    @Bean(name = "transactionManager")
    @Override
    /**
     * 关于事务管理，需要返回PlatformTransactionManager的实现
     */
    public PlatformTransactionManager annotationDrivenTransactionManager() {
        return new DataSourceTransactionManager(mySQLdataSource);
        //return null;
    }
}

