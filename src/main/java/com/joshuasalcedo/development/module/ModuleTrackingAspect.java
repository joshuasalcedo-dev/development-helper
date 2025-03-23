package com.joshuasalcedo.development.module;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Aspect to intercept method calls and track usage
 */
@Aspect
@Component
public class ModuleTrackingAspect {
    private static final Logger logger = LoggerFactory.getLogger(ModuleTrackingAspect.class);

    private final ModuleRegistry registry;

    public ModuleTrackingAspect (ModuleRegistry registry) {
        this.registry = registry;
    }

    // Define a pointcut that matches all methods annotated with @ApplicationModule
    @Pointcut("@annotation(com.joshuasalcedo.development.module.ApplicationModule)")
    public void applicationModuleMethod () {
    }

    // Also match classes annotated with @ApplicationModule
    @Pointcut("within(@com.joshuasalcedo.development.module.ApplicationModule *)")
    public void applicationModuleClass () {
    }

    // Define the around advice that will execute for all matches
    @Around("applicationModuleMethod() || (applicationModuleClass() && execution(* *(..)) && !execution(* *.get*()) && !execution(* *.set*()))")
    public Object trackModuleUsage (ProceedingJoinPoint joinPoint) throws Throwable {
        // Get method being called
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // Get class name
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = method.getName();

        // Get module name from annotation if available
        ApplicationModule annotation = method.getAnnotation(ApplicationModule.class);
        if (annotation == null) {
            annotation = joinPoint.getTarget().getClass().getAnnotation(ApplicationModule.class);
        }

        String moduleName = (annotation != null && !annotation.name().isEmpty())
                ? annotation.name()
                : methodName;

        // Get caller
        String caller = getCaller();

        logger.debug("Module call detected: {} by {}", moduleName, caller);

        long startTime = System.currentTimeMillis();
        try {
            // Execute the method
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            // Record the method call
            registry.trackMethodCall(className, methodName, caller);

            logger.info("Module '{}' executed successfully by {} in {}ms",
                    moduleName, caller, executionTime);

            return result;
        }
        catch (Throwable e) {
            long executionTime = System.currentTimeMillis() - startTime;

            // Still record the method call even if it fails
            registry.trackMethodCall(className, methodName, caller);

            logger.error("Module '{}' execution failed by {} after {}ms: {}",
                    moduleName, caller, executionTime, e.getMessage());

            throw e;
        }
    }

    /**
     * Get information about the calling class
     */
    private String getCaller () {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // Skip first few elements as they will be this aspect and Spring proxies
        for (int i = 3; i < stackTrace.length; i++) {
            StackTraceElement element = stackTrace[i];
            String className = element.getClassName();
            // Skip if it's the current class or related to Spring AOP
            if (!className.startsWith("com.joshuasalcedo.development.module") &&
                    !className.contains("$$EnhancerBySpringCGLIB$$") &&
                    !className.startsWith("org.springframework")) {
                return className + "." + element.getMethodName();
            }
        }
        return "Unknown";
    }
}