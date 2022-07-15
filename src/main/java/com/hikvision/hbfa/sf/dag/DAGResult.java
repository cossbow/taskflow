package com.hikvision.hbfa.sf.dag;

import java.util.Map;


public class DAGResult<D> {
    private final boolean success;
    private final D data;
    private final String error;

    public DAGResult(boolean success, D data, String error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    //

    public boolean isSuccess() {
        return success;
    }

    public D getData() {
        return data;
    }

    public String getError() {
        return error;
    }

    //

    public Map<String, Object> toMap() {
        if (success) {
            if (null == data) return Map.of("success", true);
            return Map.of("success", true, "data", data);
        }

        if (null == error) return Map.of("success", false);
        return Map.of("success", false, "error", error);
    }

    //

    public static <D> DAGResult<D> success(D output) {
        return new DAGResult<>(true, output, null);
    }

    public static <D> DAGResult<D> error(String error) {
        return new DAGResult<>(false, null, error);
    }

}
