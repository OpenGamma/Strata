/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.function.Parameter;
import com.opengamma.sesame.proxy.ProxyInvocationHandler;
import com.thoughtworks.paranamer.AdaptiveParanamer;
import com.thoughtworks.paranamer.AnnotationParanamer;
import com.thoughtworks.paranamer.BytecodeReadingParanamer;
import com.thoughtworks.paranamer.CachingParanamer;
import com.thoughtworks.paranamer.Paranamer;
import com.thoughtworks.paranamer.PositionalParanamer;

public final class EngineFunctionUtils {

  /** Cache of the results of {@link #getSupertypes} */
  private static ConcurrentMap<Class<?>, Set<Class<?>>> s_supertypes = Maps.newConcurrentMap();
  /** Cache of the results of {@link #getInterfaces} */
  private static ConcurrentMap<Class<?>, Set<Class<?>>> s_interfaces = Maps.newConcurrentMap();

  private static Paranamer s_paranamer =
      new CachingParanamer(
          new AdaptiveParanamer(
              new BytecodeReadingParanamer(), new AnnotationParanamer(), new PositionalParanamer()));

  private EngineFunctionUtils() {
  }

  /**
   * Returns a constructor for the engine to build instances of type.
   * If there is only one constructor it's returned. If there are multiple constructors and one is annotated with
   * {@link Inject} it's returned. Otherwise an {@link IllegalArgumentException} is thrown.
   * @param type The type
   * @param <T> Tye type
   * @return The constructor the engine should use for building instances, not null
   * @throws IllegalArgumentException If there isn't a valid constructor
   * TODO return null or throw specific exception?
   */
  @SuppressWarnings("unchecked")
  public static <T> Constructor<T> getConstructor(Class<T> type) {
    Constructor<?>[] constructors = type.getDeclaredConstructors();
    if (constructors.length == 0) {
      throw new IllegalArgumentException("No constructors found for " + type.getName());
    }
    if (constructors.length == 1) {
      Constructor<T> constructor = (Constructor<T>) constructors[0];
      if (!Modifier.isPublic(constructor.getModifiers())) {
        throw new IllegalArgumentException(type.getName() + " has no public constructors");
      }
      return constructor;
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
    if (!Modifier.isPublic(injectableConstructor.getModifiers())) {
      throw new IllegalArgumentException(type.getName() + " has no public constructors");
    }
    return (Constructor<T>) injectableConstructor;
  }

  public static List<Parameter> getParameters(Method method) {
    return getParameters(method,
                         method.getDeclaringClass(),
                         // TODO [SSM-108] use generic types for parameters, will allow for nicer error messages
                         method.getParameterTypes(),
                         method.getParameterAnnotations());
  }

  // TODO won't work for non-static inner classes. throw exception. how do I know?
  public static List<Parameter> getParameters(Constructor<?> constructor) {
    return getParameters(constructor,
                         constructor.getDeclaringClass(),
                         // TODO [SSM-108] use generic types for parameters, will allow for nicer error messages
                         constructor.getParameterTypes(),
                         constructor.getParameterAnnotations());
  }

  private static List<Parameter> getParameters(AccessibleObject ctorOrMethod,
                                               Class<?> declaringClass,
                                               Class<?>[] parameterTypes,
                                               Annotation[][] allAnnotations) {
    String[] paramNames = s_paranamer.lookupParameterNames(ctorOrMethod);
    List<Parameter> parameters = Lists.newArrayList();
    for (int i = 0; i < parameterTypes.length; i++) {
      Map<Class<?>, Annotation> annotationMap = Maps.newHashMap();
      Class<?> type = parameterTypes[i];
      Annotation[] annotations = allAnnotations[i];
      for (Annotation annotation : annotations) {
        annotationMap.put(annotation.annotationType(), annotation);
      }
      parameters.add(new Parameter(declaringClass, paramNames[i], type, i, annotationMap));
    }
    return parameters;
  }

  /**
   * Creates function metadata for a named method on a class.
   * This is for testing and isn't intended to be robust. e.g. If there are multiple methods with the same name
   * the first one will be used.
   * @param functionType The interface declaring the function method
   * @param methodName The name of the method
   * @return Metadata for the function
   */
  public static FunctionMetadata createMetadata(Class<?> functionType, String methodName) {
    return new FunctionMetadata(getMethod(functionType, methodName));
  }

  /**
   * Returns a named method on a class.
   * This only works if there is exactly one method with a matching name. If there are zero or multiple methods
   * with a matching name an exception is thrown.
   * @param type The type declaring the method
   * @param methodName The name of the method
   * @return The method
   * @throws IllegalArgumentException If there isn't exactly one method in the class with a matching name
   */
  public static Method getMethod(Class<?> type, String methodName) {
    Method[] methods = type.getMethods();
    Method found = null;
    for (Method method : methods) {
      if (methodName.equals(method.getName())) {
        if (found == null) {
          found = method;
        } else {
          throw new IllegalArgumentException("Multiple methods found named " + methodName);
        }
      }
    }
    if (found != null) {
      return found;
    }
    throw new IllegalArgumentException("No method found named " + methodName);
  }

  public static Set<Class<?>> getSupertypes(Class<?> type) {
    Set<Class<?>> existingSupertypes = s_supertypes.get(type);
    if (existingSupertypes != null) {
      return existingSupertypes;
    }
    Set<Class<?>> supertypes = Sets.newLinkedHashSet();
    Set<Class<?>> interfaces = Sets.newLinkedHashSet();
    getSupertypes(type, supertypes, interfaces);
    supertypes.addAll(interfaces);
    Set<Class<?>> cachedSupertypes = Collections.unmodifiableSet(supertypes);
    s_supertypes.put(type, cachedSupertypes);
    return cachedSupertypes;
  }

  public static Set<Class<?>> getInterfaces(Class<?> type) {
    Set<Class<?>> existingInterfaces = s_interfaces.get(type);
    if (existingInterfaces != null) {
      return existingInterfaces;
    }
    Set<Class<?>> interfaces = Sets.newLinkedHashSet();
    getSupertypes(type, Sets.<Class<?>>newLinkedHashSet(), interfaces);
    Set<Class<?>> cachedInterfaces = Collections.unmodifiableSet(interfaces);
    s_interfaces.put(type, cachedInterfaces);
    return cachedInterfaces;
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

  public static boolean hasMethodAnnotation(Class<?> type, Class<? extends Annotation> annotation) {
    for (Method method : type.getMethods()) {
      if (method.getAnnotation(annotation) != null) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the real object behind a proxy.
   * If object isn't a proxy it is returned. If it's a proxy the underlying object is returned. If there are multiple
   * proxies this method recurses until it finds the real object.
   * All proxies must have an invocation handler of type {@link ProxyInvocationHandler}.
   * @param object An object, possibly a proxy
   * @return The real object behind the proxy
   */
  public static Object getProxiedObject(Object object) {
    // if object isn't a proxy then we've reached the end of the chain of proxies
    if (!Proxy.isProxyClass(object.getClass())) {
      return object;
    }
    ProxyInvocationHandler invocationHandler = (ProxyInvocationHandler) Proxy.getInvocationHandler(object);
    return getProxiedObject(invocationHandler.getReceiver());
  }
}
