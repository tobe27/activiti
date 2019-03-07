package com.wiseq.cn.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import javax.annotation.Resource;
import javax.sql.DataSource;

/**
 * 版本        修改时间        作者      修改内容
 * V1.0        ------        jpdong     原始版本
 * 文件说明: SqlSessionFactory
 **/
@Configuration
@AutoConfigureAfter({MySQLDataSourceConfig.class})
public class SessionFactoryConfiguration {

    @Resource(name="mySQLdataSource")
    private DataSource mySQLdataSource;

    @Bean(name = "sqlSessionFactory")
    public SqlSessionFactoryBean CreateSqlSessionFactoryBean (ApplicationContext applicationContext) throws Exception {
        ResourcePatternResolver resourcePatternResolver;
        resourcePatternResolver = new PathMatchingResourcePatternResolver();


        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(mySQLdataSource);
        //sessionFactory.setPlugins(new Interceptor[]{new PageInterceptor()});
        sessionFactory.setConfigLocation(new DefaultResourceLoader().getResource("classpath:mybatis-config.xml"));
        sessionFactory.setMapperLocations(resourcePatternResolver.getResources("classpath*:mapper/**/*.xml"));
        sessionFactory.setTypeAliasesPackage("com.wiseq.cn.entity");
        return sessionFactory;
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

}



