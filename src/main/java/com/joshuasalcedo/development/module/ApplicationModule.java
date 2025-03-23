package com.joshuasalcedo.development.module;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods for tracking usage
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
public @interface ApplicationModule {
        /**
         * Name of the module (defaults to method name if not specified)
         */
        String name() default "";

        /**
         * Success message for tracking
         */
        String successMessage() default "Operation completed successfully";

        /**
         * Error message for tracking
         */
        String errorMessage() default "Operation failed";
}