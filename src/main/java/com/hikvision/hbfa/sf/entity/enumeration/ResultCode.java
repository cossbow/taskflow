package com.hikvision.hbfa.sf.entity.enumeration;

public enum ResultCode {
    SUCCESS(0),
    INNER_ERROR(1),
    BAD_REQUEST(2),
    BAD_STATUS(3),
    NETWORK(4),
    TASK_ERROR(5),
    BAD_CONFIG(6),
    BAD_RESPONSE(8),
    ;
    final int value;

    ResultCode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }


}
