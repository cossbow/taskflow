package com.hikvision.hbfa.sf.handler.call;

import com.hikvision.hbfa.sf.entity.enumeration.CallType;
import com.hikvision.hbfa.sf.util.TypedBeanManager;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CallableManager extends TypedBeanManager<CallType, Callable<?>> {

    public CallableManager(List<Callable<?>> callables) {
        super("Callables", callables);
    }

}
