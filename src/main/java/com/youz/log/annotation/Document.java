package com.youz.log.annotation;

import java.lang.annotation.*;

/**
 * Created by youz on 2017/11/16.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Document {

    /**
     * ES索引(如果需要动态填加时间这需按这种格式：索引前缀-${YYYY.MM.dd})
     * @return
     */
    String index();

    /**
     * ES类型
     * @return
     */
    String type();

}
