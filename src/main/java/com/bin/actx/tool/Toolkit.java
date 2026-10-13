package com.bin.actx.tool;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

/**
 * 工具包注解
 *
 * @author Bin
 * @since 2026/4/17 09:59
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Toolkit {

    @AliasFor(annotation = Component.class)
    String value() default "";

    String name() default "";

    String description() default "";

    boolean autoDiscover() default true;

}
