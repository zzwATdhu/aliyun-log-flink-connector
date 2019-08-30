package com.aliyun.openservices.log.flink.util;

import com.aliyun.openservices.log.exception.LogException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

class RetryUtil {
    private static final Logger LOG = LoggerFactory.getLogger(LogClientProxy.class);

    private static final long INITIAL_BACKOFF = 500;
    private static final long MAX_BACKOFF = 5000;
    private static final int MAX_ATTEMPTS = 32;

    private static void waitForMs(long sleepMs) {
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean isRecoverableException(LogException lex) {
        return lex.GetHttpCode() != 400 && lex.GetHttpCode() != 405 && lex.GetHttpCode() != 415;
    }

    static <T> T retryCall(Callable<T> callable, String errorMsg) throws LogException {
        int counter = 0;
        long backoff = INITIAL_BACKOFF;
        while (counter <= MAX_ATTEMPTS) {
            try {
                // TODO Handle ignorable exception in call()
                return callable.call();
            } catch (LogException e1) {
                if (isRecoverableException(e1) && counter < MAX_ATTEMPTS) {
                    LOG.error("{}: {}, retry {}/{}", errorMsg, e1.GetErrorMessage(), counter, MAX_ATTEMPTS);
                } else {
                    throw e1;
                }
            } catch (Exception e2) {
                if (counter >= MAX_ATTEMPTS) {
                    throw new LogException("Unknown", errorMsg, e2, "");
                }
                LOG.error("{}, retry {}/{}", errorMsg, counter, MAX_ATTEMPTS, e2);
            }
            waitForMs(backoff);
            backoff = Math.min(backoff * 2, MAX_BACKOFF);
            counter++;
        }
        throw new RuntimeException("Not possible!");
    }
}
