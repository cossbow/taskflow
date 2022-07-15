package com.hikvision.hbfa.sf.mybatis;

import com.hikvision.hbfa.sf.util.Typed;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandlerRegistry;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class TypedTypeHandler<T extends Typed> extends BaseTypeHandler<T> {

    private final Class<T> type;

    public TypedTypeHandler(Class<T> type) {
        this.type = Objects.requireNonNull(type);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Typed> void registerAlias(
            TypeHandlerRegistry typeHandlerRegistry,
            List<Class<? extends Typed>> classes) {
        for (Class<?> type : classes) {
            typeHandlerRegistry.register((Class<T>) type, JdbcType.VARCHAR,
                    new TypedTypeHandler<>((Class<T>) type));
        }
    }

    //

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, T p, JdbcType jdbcType) throws SQLException {
        ps.setString(i, p.name());
    }

    private T parse(String name) {
        return null == name ? null : Typed.valueOf(type, name);
    }

    @Override
    public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

}
