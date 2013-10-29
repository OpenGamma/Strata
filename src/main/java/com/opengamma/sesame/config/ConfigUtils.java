/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.opengamma.sesame.function.Inject;
import com.opengamma.sesame.function.Parameter;

public final class ConfigUtils {

  private static ConcurrentMap<Class<?>, Set<Class<?>>> s_supertypes = Maps.newConcurrentMap();

  private ConfigUtils() {
  }

  /**
   * Returns a constructor for the engine to build instances of type.
   * If there is only one constructor it's returned. If there are multiple constructors and one is annotated with
   * {@link Inject} it's returned. Otherwise an {@link IllegalArgumentException} is thrown.
   * @param type The type
   * @param <T> Tye type
   * @return The constructor the engine should use for building instances
   * @throws IllegalArgumentException If there isn't a valid constructor
   */
  @SuppressWarnings("unchecked")
  public static <T> Constructor<T> getConstructor(Class<T> type) {
    Constructor<?>[] constructors = type.getDeclaredConstructors();
    if (constructors.length == 0) {
      throw new IllegalArgumentException("No constructors found for " + type.getName());
    }
    if (constructors.length == 1) {
      return (Constructor<T>) constructors[0];
    }
    Constructor<?> injectableConstructor = null;
    for (Constructor<?> constructor : constructors) {
      Inject annotation = constructor.getAnnotation(Inject.class);
      if (annotation != null) {
        if (injectableConstructor == null) {
          injectableConstructor = constructor;
        } else {
          throw new IllegalArgumentException("Only one constructor should be annotated with @Inject in " + type.getName());
        }
      }
    }
    if (injectableConstructor == null) {
      throw new IllegalArgumentException(type.getName() + " has multiple constructors but none have an @Inject annotation");
    }
    return (Constructor<T>) injectableConstructor;
  }

  public static List<Parameter> getParameters(Method method) {
    return getParameters(method.getParameterTypes(), method.getParameterAnnotations());
  }

  public static List<Parameter> getParameters(Constructor<?> constructor) {
    return getParameters(constructor.getParameterTypes(), constructor.getParameterAnnotations());
  }

  private static List<Parameter> getParameters(Class<?>[] parameterTypes, Annotation[][] allAnnotations) {
    List<Parameter> parameters = Lists.newArrayList();
    for (int i = 0; i < parameterTypes.length; i++) {
      Map<Class<?>, Annotation> annotationMap = Maps.newHashMap();
      Class<?> type = parameterTypes[i];
      Annotation[] annotations = allAnnotations[i];
      for (Annotation annotation : annotations) {
        annotationMap.put(annotation.annotationType(), annotation);
      }
      parameters.add(new Parameter(name, type, ordinal, annotationMap));
    }
    return parameters;
  }

  /**
   * Returns a parameter annotated with an annotation type.
   * Throws {@link IllegalArgumentException} if there are multiple parameters with the annotation.
   * @param annotationType The annotation type
   * @param method The method
   * @return The method parameter with the specified annotation, null if there isn't one
   */
  public static Parameter getAnnotatedParameter(Class<? extends Annotation> annotationType, Method method) {
    Parameter annotated = null;
    for (Parameter parameter : getParameters(method)) {
      if (parameter.getAnnotations().containsKey(annotationType)) {
        if (annotated == null) {
          annotated = parameter;
        } else {
          throw new IllegalArgumentException("Exactly one parameter in " + method + " must be annotated with " +
                                                 annotationType.getName());
        }
      }
    }
    return annotated;
  }

  public static Set<Class<?>> getSupertypes(Class<?> type) {
    Set<Class<?>> supertypes = Sets.newLinkedHashSet();
    Set<Class<?>> interfaces = Sets.newLinkedHashSet();
    getSupertypes(type, supertypes, interfaces);
    supertypes.addAll(interfaces);
    s_supertypes.put(type, Collections.unmodifiableSet(supertypes));
    return supertypes;
  }

  private static void getSupertypes(Class<?> type, Set<Class<?>> supertypeAccumulator, Set<Class<?>> interfaceAccumulator) {
    supertypeAccumulator.add(type);
    getInterfaces(type.getInterfaces(), interfaceAccumulator);
    Class<?> superclass = type.getSuperclass();
    if (superclass != null) {
      getSupertypes(superclass, supertypeAccumulator, interfaceAccumulator);
    }
  }

  private static void getInterfaces(Class<?>[] interfaces, Set<Class<?>> accumulator) {
    accumulator.addAll(Arrays.asList(interfaces));
    for (Class<?> iFace : interfaces) {
      getInterfaces(iFace.getInterfaces(), accumulator);
    }
  }
}
