package com.example;

import org.postgresql.Driver;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Collection;

//@SpringBootApplication
public class BeansEvaluationApplication {

    public static void main(String[] args) {
        var start = System.nanoTime();
        one();
        var stop = System.nanoTime();
        var elapsed = (stop - start) / 1_000_000_000.0;
        System.out.println("Elapsed time: " + elapsed + "s");
    }

    private static void zero() {
        var factory = new DefaultListableBeanFactory();

        var driverDefinition = new RootBeanDefinition(Driver.class);
        factory.registerBeanDefinition("driver", driverDefinition);

        var dataSourceBeanDefinition = new RootBeanDefinition(SimpleDriverDataSource.class);
        var constructorArgumentValues = dataSourceBeanDefinition.getConstructorArgumentValues();
        constructorArgumentValues.addGenericArgumentValue(new RuntimeBeanReference("driver"));
        constructorArgumentValues.addGenericArgumentValue("jdbc:postgresql://localhost:5432/mydatabase");
        constructorArgumentValues.addGenericArgumentValue("myuser");
        constructorArgumentValues.addGenericArgumentValue("secret");

        var jdbcClientBeanDefinition = new RootBeanDefinition(JdbcClient.class);
        jdbcClientBeanDefinition.setFactoryMethodName("create");
        jdbcClientBeanDefinition.getConstructorArgumentValues().addGenericArgumentValue(dataSourceBeanDefinition);
        factory.registerBeanDefinition("jdbcClient", jdbcClientBeanDefinition);

        var customerServiceBeanDefinition = new RootBeanDefinition(CustomerService.class);
        customerServiceBeanDefinition.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference("jdbcClient"));
        factory.registerBeanDefinition("customerService", customerServiceBeanDefinition);

        var gac = new GenericApplicationContext(factory);
        gac.refresh();
        exercise(gac);

    }

    private static void one() {
        var xml = new ClassPathXmlApplicationContext("beans-1.xml");
        exercise(xml);
    }

    static void exercise(ApplicationContext applicationContext) {
        var cs = applicationContext.getBean(CustomerService.class);
        for (Customer customer : cs.getCustomers()) {
            System.out.println(customer.toString());
        }
    }

}


/**
 * InitializingBeam == @PostConstruct
 * DisposableBean == @PreDestroy
 */
@Service
class CustomerService implements InitializingBean {

    private final JdbcClient jdbcClient;

    CustomerService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    Collection<Customer> getCustomers() {
        return this.jdbcClient
                .sql("SELECT * FROM customers")
                .query((rs, rowNum) -> new Customer(rs.getInt("id"), rs.getString("name")))
                .list();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("afterPropertiesSet");
        Assert.notNull(this.jdbcClient, "jdbcClient must not be null");
    }
}


record Customer(Integer id, String name) {

}