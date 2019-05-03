package com.youz.log.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.github.miemiedev.mybatis.paginator.OffsetLimitInterceptor;
import com.github.miemiedev.mybatis.paginator.dialect.MySQLDialect;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.youz.log.repository")
public class DataSourceConfig {

    private Interceptor mybatisPlugin() {
        OffsetLimitInterceptor mybatisInterceptor = new OffsetLimitInterceptor();
//        mybatisInterceptor.setAsyncTotalCount(true);
        mybatisInterceptor.setDialectClass(MySQLDialect.class.getName());
        return mybatisInterceptor;
    }

    @Bean
    @ConfigurationProperties(prefix = "database")
    public javax.sql.DataSource dataSource() {
        return DataSourceBuilder.create().type(DruidDataSource.class).build();
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(ApplicationContext applicationContext) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource());
//        sessionFactory.setMapperLocations(applicationContext.getResources("classpath:mapper/*.xml"));
        sessionFactory.setPlugins(new Interceptor[]{mybatisPlugin()});
        return sessionFactory.getObject();
    }

//    @Bean(name = "sqlSessionTemplate", destroyMethod = "clearCache")
//    public SqlSessionTemplate sqlSessionTemplate() throws Exception {
//        return new SqlSessionTemplate(sqlSessionFactory());
//    }
}
