package com.inventory.infrastructure.logging;

import java.util.Arrays;

import org.jboss.logging.Logger;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@LogMethod
@Priority(Interceptor.Priority.APPLICATION)
public class MethodLoggerInterceptor {

    @AroundInvoke
    public Object logMethod(InvocationContext ctx) throws Exception {
        Logger log = Logger.getLogger(ctx.getTarget().getClass().getName());
        String method = ctx.getMethod().getName();
        Object[] params = ctx.getParameters();

        try {
            log.infof("Entering %s.%s params=%s", ctx.getTarget().getClass().getSimpleName(), method,
                    Arrays.toString(params));
            Object result = ctx.proceed();
            log.infof("Exiting %s.%s result=%s", ctx.getTarget().getClass().getSimpleName(), method, result);
            return result;
        } catch (Exception e) {
            log.errorf(e, "Exception in %s.%s", ctx.getTarget().getClass().getSimpleName(), method);
            throw e;
        }
    }
}
