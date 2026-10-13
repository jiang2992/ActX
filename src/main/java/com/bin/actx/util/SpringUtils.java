/**
 * Copyright &copy; 2012-2016 <feigns href="https://github.com/thinkgem/jeesite">JeeSite</feigns> All rights reserved.
 */
package com.bin.actx.util;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Spring 工具类
 *
 * @author Bin
 * @since 2020-06-28 18:09
 */
@Slf4j
@Component
public class SpringUtils implements ApplicationContextAware, DisposableBean {

    @Getter
    private static ApplicationContext applicationContext = null;

    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name) {
        return (T) applicationContext.getBean(name);
    }

    public static <T> T getBean(Class<T> requiredType) {
        return applicationContext.getBean(requiredType);
    }

    public static void autowireBean(Object obj) {
        applicationContext.getAutowireCapableBeanFactory().autowireBean(obj);
    }

    public static void registerBean(String name, Object obj) {
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) ((ConfigurableApplicationContext) applicationContext).getBeanFactory();
        beanFactory.registerSingleton(name, obj);
        autowireBean(obj);
    }

    private static void clearHolder() {
        applicationContext = null;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        SpringUtils.applicationContext = applicationContext;
    }

    @Override
    public void destroy() {
        SpringUtils.clearHolder();
    }

}