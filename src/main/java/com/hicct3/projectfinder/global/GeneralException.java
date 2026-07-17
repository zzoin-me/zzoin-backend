package com.hicct3.projectfinder.global;

import lombok.Getter;

@Getter
public class GeneralException extends RuntimeException{
    private final ErrorCode errorCode;

    public GeneralException(String msg)
    {
        super(msg);
        this.errorCode = ErrorCode.COMMON_BAD_REQUEST;
    }

    public GeneralException(ErrorCode errorCode)
    {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public GeneralException(ErrorCode errorCode, String message)
    {
        super(message);
        this.errorCode = errorCode;
    }
}