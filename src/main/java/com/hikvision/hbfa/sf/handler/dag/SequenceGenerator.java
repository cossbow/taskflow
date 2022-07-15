package com.hikvision.hbfa.sf.handler.dag;

import java.util.function.Supplier;

/**
 * ID生成器
 */
@FunctionalInterface
public interface SequenceGenerator<ID> extends Supplier<ID> {

    /**
     * 获取生成的下一个唯一的ID
     */
    ID next();

    default ID get() {
        return next();
    }
}
