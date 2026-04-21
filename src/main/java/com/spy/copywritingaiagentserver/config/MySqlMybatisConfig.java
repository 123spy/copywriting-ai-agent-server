package com.spy.copywritingaiagentserver.config;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties;
import com.baomidou.mybatisplus.autoconfigure.SpringBootVFS;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Mysql数据源
 */
@Configuration
public class MySqlMybatisConfig {

    @Bean(name = "mysqlSqlSessionFactory")
    @Primary
    public SqlSessionFactory mysqlSqlSessionFactory(
            @Qualifier("mysqlDataSource") DataSource dataSource,
            MybatisPlusProperties mybatisPlusProperties,
            ObjectProvider<Interceptor[]> interceptorsProvider,
            ApplicationContext applicationContext
    ) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setVfs(SpringBootVFS.class);
        factoryBean.setApplicationContext(applicationContext);
        factoryBean.setTransactionFactory(new SpringManagedTransactionFactory());

        MybatisPlusProperties.CoreConfiguration coreConfiguration = mybatisPlusProperties.getConfiguration();
        if (coreConfiguration != null || mybatisPlusProperties.getConfigLocation() == null) {
            MybatisConfiguration configuration = new MybatisConfiguration();
            if (coreConfiguration != null) {
                coreConfiguration.applyTo(configuration);
            }
            factoryBean.setConfiguration(configuration);
        }

        if (mybatisPlusProperties.getConfigurationProperties() != null) {
            factoryBean.setConfigurationProperties(mybatisPlusProperties.getConfigurationProperties());
        }

        GlobalConfig globalConfig = mybatisPlusProperties.getGlobalConfig();
        if (globalConfig != null) {
            factoryBean.setGlobalConfig(globalConfig);
        }

        Resource[] mapperLocations = mybatisPlusProperties.resolveMapperLocations();
        if (mapperLocations != null && mapperLocations.length > 0) {
            factoryBean.setMapperLocations(mapperLocations);
        }

        Interceptor[] interceptors = interceptorsProvider.getIfAvailable();
        if (interceptors != null && interceptors.length > 0) {
            factoryBean.setPlugins(interceptors);
        }

        return factoryBean.getObject();
    }

    @Bean(name = "mysqlSqlSessionTemplate")
    @Primary
    public SqlSessionTemplate mysqlSqlSessionTemplate(
            @Qualifier("mysqlSqlSessionFactory") SqlSessionFactory sqlSessionFactory,
            MybatisPlusProperties mybatisPlusProperties
    ) {
        ExecutorType executorType = mybatisPlusProperties.getExecutorType();
        if (executorType != null) {
            return new SqlSessionTemplate(sqlSessionFactory, executorType);
        }
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean(name = {"mysqlTransactionManager", "transactionManager"})
    @Primary
    public PlatformTransactionManager mysqlTransactionManager(
            @Qualifier("mysqlDataSource") DataSource dataSource
    ) {
        return new DataSourceTransactionManager(dataSource);
    }
}
