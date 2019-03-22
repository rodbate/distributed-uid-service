package com.github.rodbate.uid.exceptions;

import com.github.rodbate.uid.common.web.ReturnCode;
import lombok.Getter;
import lombok.Setter;

import java.text.MessageFormat;

/**
 * User: jiangsongsong
 * Date: 2018/12/11
 * Time: 11:10
 */
@Getter
@Setter
public class IdGeneratorServiceException extends RuntimeException {

    private final ReturnCode returnCode;
    private final Object[] args;

    public IdGeneratorServiceException(ReturnCode returnCode, Object... args) {
        super();
        this.returnCode = returnCode;
        this.args = args;
    }

    public IdGeneratorServiceException(ReturnCode returnCode, Throwable ex, Object... args) {
        super(ex);
        this.returnCode = returnCode;
        this.args = args;
    }

    private String formatMessage(String message, Object[] args) {
        if (args != null && args.length > 0) {
            message = MessageFormat.format(message, args);
        }
        return message;
    }

    @Override
    public String getMessage() {
        return formatMessage(returnCode.getDefaultMsg(), args);
    }
}
