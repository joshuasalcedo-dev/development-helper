package com.joshuasalcedo.development.module;

import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Scanner to find all ApplicationModule annotations
 */
@Component
public class ModuleScanner {

    private final ApplicationContext applicationContext;


    private final ModuleRegistry registry;


    private final RequestMappingHandlerMapping requestMappingHandlerMapping;

    public ModuleScanner (ApplicationContext applicationContext, ModuleRegistry registry, RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this.applicationContext = applicationContext;
        this.registry = registry;
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
    }

    @EventListener
    public void onApplicationEvent (ContextRefreshedEvent event) {
        scanComponents();
        scanControllers();
    }

    private void scanComponents () {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(Service.class);

        for (Object bean : beans.values()) {
            Class<?> clazz = bean.getClass();

            // Check if the class itself is annotated
            if (clazz.isAnnotationPresent(ApplicationModule.class)) {
                processClassAnnotation(clazz);
            }

            // Check methods
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(ApplicationModule.class)) {
                    processMethodAnnotation(clazz, method);
                }
            }
        }
    }

    private void scanControllers () {
        // Get all mappings registered with Spring MVC
        Map<RequestMappingInfo, HandlerMethod> handlerMethods =
                requestMappingHandlerMapping.getHandlerMethods();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            HandlerMethod handlerMethod = entry.getValue();
            Class<?> controllerClass = handlerMethod.getBeanType();
            Method method = handlerMethod.getMethod();

            // Process controller class and method annotations
            if (controllerClass.isAnnotationPresent(ApplicationModule.class) ||
                    method.isAnnotationPresent(ApplicationModule.class)) {

                RequestMappingInfo mappingInfo = entry.getKey();
                String path = mappingInfo.getPatternsCondition().getPatterns().iterator().next();

                if (method.isAnnotationPresent(ApplicationModule.class)) {
                    processControllerMethodAnnotation(controllerClass, method, path);
                } else {
                    processControllerClassAnnotation(controllerClass, method, path);
                }
            }
        }
    }

    private void processClassAnnotation (Class<?> clazz) {
        ApplicationModule annotation = clazz.getAnnotation(ApplicationModule.class);
        String moduleName = annotation.name().isEmpty() ?
                clazz.getSimpleName() : annotation.name();

        // Register all methods of the class
        for (Method method : clazz.getDeclaredMethods()) {
            registerModuleMethod(clazz, method, annotation, moduleName + "." + method.getName());
        }
    }

    private void processMethodAnnotation (Class<?> clazz, Method method) {
        ApplicationModule annotation = method.getAnnotation(ApplicationModule.class);
        String moduleName = annotation.name().isEmpty() ?
                method.getName() : annotation.name();

        registerModuleMethod(clazz, method, annotation, moduleName);
    }

    private void processControllerClassAnnotation (Class<?> controllerClass, Method method, String path) {
        ApplicationModule annotation = controllerClass.getAnnotation(ApplicationModule.class);
        String moduleName = annotation.name().isEmpty() ?
                controllerClass.getSimpleName() : annotation.name();

        registerControllerModuleMethod(controllerClass, method, annotation,
                moduleName + "." + method.getName(), path);
    }

    private void processControllerMethodAnnotation (Class<?> controllerClass, Method method, String path) {
        ApplicationModule annotation = method.getAnnotation(ApplicationModule.class);
        String moduleName = annotation.name().isEmpty() ?
                method.getName() : annotation.name();

        registerControllerModuleMethod(controllerClass, method, annotation, moduleName, path);
    }

    private void registerModuleMethod (Class<?> clazz, Method method, ApplicationModule annotation, String moduleName) {
        List<String> parameterTypes = getParameterTypeNames(method);

        Module module = new Module(
                moduleName,
                clazz.getName(),
                method.getName(),
                parameterTypes,
                annotation.errorMessage(),
                annotation.successMessage()
        );

        registry.registerModule(moduleName, module);
    }

    private void registerControllerModuleMethod (Class<?> clazz, Method method,
                                                 ApplicationModule annotation,
                                                 String moduleName, String path) {
        List<String> parameterTypes = getParameterTypeNames(method);

        Module module = new Module(
                moduleName,
                clazz.getName(),
                method.getName(),
                parameterTypes,
                annotation.errorMessage(),
                annotation.successMessage()
        );

        // Add path information for controllers
        module.addEndpoint(path);

        registry.registerModule(moduleName, module);
    }

    private List<String> getParameterTypeNames (Method method) {
        return Stream.of(method.getParameterTypes())
                .map(Class::getSimpleName)
                .collect(Collectors.toList());
    }
}
