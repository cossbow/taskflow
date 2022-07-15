package com.hikvision.hbfa.sf.controller;

import com.hikvision.hbfa.sf.ex.NotFoundException;
import com.hikvision.hbfa.sf.handler.dag.CallTimeoutException;
import io.burt.jmespath.function.FunctionCallException;
import io.burt.jmespath.function.FunctionConfigurationException;
import io.burt.jmespath.parser.ParseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ValidationException;
import java.net.ConnectException;

import static org.springframework.http.HttpStatus.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public Object notExists(NotFoundException e) {
        log.debug("not found", e);
        return ResponseEntity.status(NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Object illegalArgument(IllegalArgumentException e) {
        log.debug("illegal argument", e);
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public Object illegalState(IllegalStateException e) {
        log.debug("illegal state", e);
        return ResponseEntity.status(CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler({FunctionCallException.class})
    public Object jsonDataError(FunctionCallException e) {
        log.debug("parse error", e);
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(ParseException.class)
    public Object JMESPathError(ParseException e) {
        log.warn("parse error", e);
        return ResponseEntity.status(CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(FunctionConfigurationException.class)
    public Object JMESError(FunctionConfigurationException e) {
        log.error("parse error", e);
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(e.getMessage());
    }


    @ExceptionHandler(ValidationException.class)
    public Object validation(ValidationException e) {
        log.debug("validation error", e);
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(CallTimeoutException.class)
    public Object callTimeout(CallTimeoutException e) {
        log.warn("call timeout", e);
        return ResponseEntity.status(REQUEST_TIMEOUT).body(e.getMessage());
    }

    @ExceptionHandler(ConnectException.class)
    public Object connect(ConnectException e) {
        log.error("client connect error", e);
        return ResponseEntity.status(REQUEST_TIMEOUT).body(e.getMessage());
    }

}
