package com.bin.actx.tool;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 工具策略注解
 *
 * @author Bin
 * @since 2026/4/17 09:59
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ToolStrategy {

    String OPERATION_TYPE_READ = "read";

    String OPERATION_TYPE_WRITE = "write";

    String OPERATION_TYPE_IDEMPOTENT_WRITE = "idempotent_write";

    String operationType() default OPERATION_TYPE_READ;

}
