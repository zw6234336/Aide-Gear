package com.aidegear.common.exception;

/**
 * Aide-Gear 框架异常基类。
 *
 * @author wayne
 * @since 1.0.0
 */
public class AideGearException extends RuntimeException {

    public AideGearException(String message) {
        super(message);
    }

    public AideGearException(String message, Throwable cause) {
        super(message, cause);
    }
}
