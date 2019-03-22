package com.github.rodbate.uid.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * User: jiangsongsong
 * Date: 2018/12/11
 * Time: 11:47
 */
@Slf4j
public final class ResourceBundleUtil {

    private ResourceBundleUtil() {
        throw new IllegalStateException("NO INSTANCE");
    }


    /**
     * get i18n message by bundle base name and locale
     *
     * @param baseName bundle base name
     * @param locale   locale
     * @param key      message key
     * @param args     message argument
     * @return i18n message
     */
    public static String getMessage(final String baseName, final Locale locale, final String key, final Object... args) {
        Objects.requireNonNull(baseName, "baseName require not null");
        Objects.requireNonNull(locale, "locale require not null");
        Objects.requireNonNull(key, "message key require not null");

        String message = "";
        try {
            message = ResourceBundle.getBundle(baseName, locale).getString(key);
        } catch (Throwable e1) {
            logWarnMessage(baseName, locale, key, e1.getMessage());
            try {
                message = ResourceBundle.getBundle(baseName, Locale.getDefault()).getString(key);
            } catch (Throwable e2) {
                logWarnMessage(baseName, Locale.getDefault(), key, e2.getMessage());
                try {
                    message = ResourceBundle.getBundle(baseName, Locale.SIMPLIFIED_CHINESE).getString(key);
                } catch (Throwable e3) {
                    logWarnMessage(baseName, Locale.SIMPLIFIED_CHINESE, key, e3.getMessage());
                }
            }
        }
        if (StringUtils.isNotBlank(message) && args != null && args.length > 0) {
            message = MessageFormat.format(message, args);
        }
        return message;
    }


    private static void logWarnMessage(final String baseName, final Locale locale, final String key, final String exception) {
        log.warn("cannot get i18n message of key={} from resource bundle(baseName={}, locale={}), exception: {}", key, baseName, locale, exception);
    }

}
