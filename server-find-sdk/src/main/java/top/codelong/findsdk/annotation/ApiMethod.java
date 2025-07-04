package top.codelong.findsdk.annotation;

import top.codelong.findsdk.enums.HttpTypeEnum;

import java.lang.annotation.*;

/**
 * 自定义方法注解
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ApiMethod {

    int isHttp() default 0;

    HttpTypeEnum httpType() default HttpTypeEnum.GET;

    String url() default "";

    int isAuth() default 0;
}
