package com.hikvision.hbfa.sf.handler.dag.impl;

import com.hikvision.hbfa.sf.entity.enumeration.NodeType;
import com.hikvision.hbfa.sf.handler.call.Callable;
import com.hikvision.hbfa.sf.handler.dag.DAGNodeExecutor;
import com.hikvision.hbfa.sf.handler.dag.data.DAGAsyncSubtask;
import com.hikvision.hbfa.sf.util.ExpiringCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class DAGHandlerConfiguration {

    @Autowired
    ExpiringCache<String, DAGAsyncSubtask> asyncSubtaskCache;
    @Autowired
    ExecutorService taskExecutor;


    @Bean
    public String addCallableDAGNodeExecutors(
            ApplicationContext context,
            List<Callable<?>> callables,
            ExpiringCache<String, DAGAsyncSubtask> asyncSubtaskCache,
            ExecutorService taskExecutor) {
        var beanFactory = (SingletonBeanRegistry) context.getAutowireCapableBeanFactory();
        var mm = NodeType.values().stream()
                .filter(nt -> nt.callType() != null)
                .collect(Collectors.toMap(NodeType::callType, Function.identity()));
        for (Callable<?> callable : callables) {
            var type = mm.get(callable.type());
            var executor = new CallableDAGNodeExecutor(
                    type,
                    callable,
                    asyncSubtaskCache,
                    taskExecutor
            );
            var name = type.name() + "CallableDAGNodeExecutor";
            beanFactory.registerSingleton(name, executor);
        }
        return "";
    }

    @Bean
    DAGNodeHandlerManager dagNodeHandlerManager(
            String addCallableDAGNodeExecutors,
            List<DAGNodeExecutor> executors) {
        return new DAGNodeHandlerManager(executors);
    }

}
