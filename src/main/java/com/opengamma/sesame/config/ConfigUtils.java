/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 *
 */
/* package */ final class ConfigUtils {

  private ConfigUtils() {
  }

  /* package */
  static Constructor<?> getConstructor(Class<?> type) {
    // TODO check there's exactly 1 or that it's correctly annotated. create @Inject annotation
    // TODO what about classes with implicit no-args constructors?
    Constructor<?>[] constructors = type.getDeclaredConstructors();
    if (constructors.length == 0) {
      throw new IllegalArgumentException("No constructors found for " + type.getName());
    }
    for (Constructor<?> constructor : constructors) {
      // TODO check for exactly 1 @Inject annotation
    }
    return constructors[0];
  }

  /* package */
  static List<ConstructorParameter> getParameters(Constructor<?> constructor) {
    Class<?>[] parameterTypes = constructor.getParameterTypes();
    Annotation[][] allAnnotations = constructor.getParameterAnnotations();
    List<ConstructorParameter> parameters = Lists.newArrayList();
    for (int i = 0; i < parameterTypes.length; i++) {
      Map<Class<?>, Annotation> annotationMap = Maps.newHashMap();
      Class<?> type = parameterTypes[i];
      Annotation[] annotations = allAnnotations[i];
      for (Annotation annotation : annotations) {
        annotationMap.put(annotation.annotationType(), annotation);
      }
      parameters.add(new ConstructorParameter(type, annotationMap));
    }
    return parameters;
  }

  /* package */ static FunctionMetadata getFunctionMetadata(Class<?> functionInterface) {
    if (!functionInterface.isInterface()) {
      throw new IllegalArgumentException(functionInterface.getName() + " isn't an interface");
    }
    EngineFunction engineFunctionAnnotation = functionInterface.getAnnotation(EngineFunction.class);
    String valueName;
    if (engineFunctionAnnotation == null) {
      valueName = null;
    } else {
      valueName = engineFunctionAnnotation.value();
    }
    DefaultImplementation defaultImplementationAnnotation = functionInterface.getAnnotation(DefaultImplementation.class);
    if (defaultImplementationAnnotation == null) {
      throw new IllegalArgumentException(functionInterface.getName() + " isn't annotated with @DefaultImplementation");
    }
    Class defaultImpl = defaultImplementationAnnotation.value();
    return new FunctionMetadata(valueName, defaultImpl);
  }
}
