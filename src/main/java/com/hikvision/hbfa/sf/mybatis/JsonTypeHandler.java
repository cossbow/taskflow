package com.hikvision.hbfa.sf.mybatis;


import com.fasterxml.jackson.databind.JavaType;
import com.hikvision.hbfa.sf.util.JsonUtil;
import com.hikvision.hbfa.sf.util.ValueUtil;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class JsonTypeHandler<T> extends BaseTypeHandler<T> {
    private final JavaType type;

    public JsonTypeHandler(Class<T> targetType) {
        this.type = JsonUtil.typeFactory().constructType(targetType);
    }

    @SuppressWarnings("unchecked")
    public static <T> void registerAlias(TypeHandlerRegistry typeHandlerRegistry, Class<?>... classes) {
        for (Class<?> type : classes) {
            typeHandlerRegistry.register((Class<T>) type, JdbcType.OTHER,
                    new JsonTypeHandler<>((Class<T>) type));
        }
    }


    private T parseJsonResult(byte[] data) throws SQLException {
        if (ValueUtil.isEmpty(data)) {
            return null;
        }
        try {
            return JsonUtil.fromJson(data, type);
        } catch (IllegalArgumentException e) {
            throw new SQLException("错误的JSON数据: " + e.getMessage(), e.getCause());
        }
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
        try {
            String value = JsonUtil.toJson(parameter);

            String type = ps.getParameterMetaData().getParameterTypeName(i);

            PGobject jsonb = new PGobject();
            jsonb.setType(type);
            jsonb.setValue(value);

            ps.setObject(i, jsonb);
        } catch (IllegalStateException e) {
            throw new SQLException("JSON序列化错误: " + e.getMessage(), e.getCause());
        }
    }

    @Override
    public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
        if (rs.wasNull()) return null;
        return parseJsonResult(rs.getBytes(columnName));
    }

    @Override
    public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        if (rs.wasNull()) return null;
        return parseJsonResult(rs.getBytes(columnIndex));
    }

    @Override
    public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        if (cs.wasNull()) return null;
        return parseJsonResult(cs.getBytes(columnIndex));
    }

}
