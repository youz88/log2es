package com.youz.log.exception;

import java.io.Serializable;

/** 业务异常 */
public class ServiceException extends RuntimeException implements Serializable {
    private static final long serialVersionUID = 1L;

    public ServiceException() {super("业务异常");this.errorMessage = "业务异常";}
    public ServiceException(String msg) {super(msg); this.errorMessage = msg;}

    public ServiceException(String code, String msg) {
        super(msg);
        this.errorCode = code;
        this.errorMessage = msg;
    }

    private String errorCode;

    private String errorMessage;

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }



    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
