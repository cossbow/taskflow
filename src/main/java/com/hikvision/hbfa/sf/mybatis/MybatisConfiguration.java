package com.hikvision.hbfa.sf.mybatis;


import com.hikvision.hbfa.sf.dao.NodeDao;
import com.hikvision.hbfa.sf.entity.Node;
import com.hikvision.hbfa.sf.entity.enumeration.CallType;
import com.hikvision.hbfa.sf.entity.enumeration.NodeType;
import com.hikvision.hbfa.sf.entity.enumeration.ScriptType;
import com.hikvision.hbfa.sf.entity.enumeration.TaskSourceType;
import com.hikvision.hbfa.sf.entity.json.ObjectMap;
import com.hikvision.hbfa.sf.entity.json.Script;
import com.hikvision.hbfa.sf.entity.json.SubmitConfig;
import org.apache.ibatis.type.EnumTypeHandler;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@MapperScan(basePackageClasses = {NodeDao.class})
@Configuration
public class MybatisConfiguration implements ConfigurationCustomizer {

    @Override
    public void customize(org.apache.ibatis.session.Configuration conf) {
        conf.setMapUnderscoreToCamelCase(true);
        conf.getTypeAliasRegistry().registerAliases(Node.class.getPackageName());
        conf.setDefaultEnumTypeHandler(EnumTypeHandler.class);

        var handlerRegistry = conf.getTypeHandlerRegistry();
        JsonTypeHandler.registerAlias(handlerRegistry,
                ObjectMap.class, SubmitConfig.class, Script.class);
        TypedTypeHandler.registerAlias(handlerRegistry,
                List.of(CallType.class, NodeType.class, ScriptType.class, TaskSourceType.class));
    }

}
