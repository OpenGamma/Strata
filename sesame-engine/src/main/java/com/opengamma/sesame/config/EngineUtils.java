/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.opengamma.core.config.Config;
import com.opengamma.master.security.ManageableSecurity;
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.function.Parameter;
import com.opengamma.sesame.proxy.ProxyInvocationHandler;
import com.opengamma.util.ArgumentChecker;
import com.thoughtworks.paranamer.AdaptiveParanamer;
import com.thoughtworks.paranamer.AnnotationParanamer;
import com.thoughtworks.paranamer.BytecodeReadingParanamer;
import com.thoughtworks.paranamer.CachingParanamer;
import com.thoughtworks.paranamer.Paranamer;
import com.thoughtworks.paranamer.PositionalParanamer;

/**
 * Utilities to assist working with functions in the calculation engine.
 */
public final class EngineUtils {

  /**
   * Cache of the results of {@link #getSupertypes}.
   */
  private static ConcurrentMap<Class<?>, Set<Class<?>>> s_supertypes = Maps.newConcurrentMap();
  /**
   * Cache of the results of {@link #getInterfaces}.
   */
  private static ConcurrentMap<Class<?>, Set<Class<?>>> s_interfaces = Maps.newConcurrentMap();
  /**
   * Paranamer instance.
   */
  private static Paranamer s_paranamer =
      new CachingParanamer(
          new AdaptiveParanamer(
              new BytecodeReadingParanamer(), new AnnotationParanamer(), new PositionalParanamer()));

  /**
   * Restricted constructor.
   */
  private EngineUtils() {
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a constructor for the engine to build instances of type.
   * <p>
   * Only public constructors are considered.
   * If there is only one constructor it is used. If there are multiple constructors
   * then one, and only one, must be annotated with {@link Inject}.
   * 
   * @param <T> the type
   * @param type  the type to find the constructor for, not null
   * @return the constructor the engine should use for building instances, not null
   * @throws IllegalArgumentException if there isn't a valid constructor
   */
  public static <T> Constructor<T> getConstructor(Class<T> type) {
    @SuppressWarnings("unchecked")
    Constructor<T>[] constructors = (Constructor<T>[]) type.getConstructors();
    if (constructors.length == 0) {
      throw new IllegalArgumentException("No public constructor found: " + type.getName());
    }
    // one constructor
    if (constructors.length == 1) {
      return constructors[0];
    }
    // many constructors
    List<Constructor<T>> injectableConstructors = new ArrayList<>();

    for (Constructor<T> constructor : constructors) {
      Inject annotation = constructor.getAnnotation(Inject.class);
      if (annotation != null) {
        injectableConstructors.add(constructor);
      }
    }
    if (injectableConstructors.size() > 1) {
      throw new IllegalArgumentException("Multiple public constructors annotated with @Inject, but only one is allowed: " + type.getName());
    }
    if (injectableConstructors.isEmpty()) {
      throw new IllegalArgumentException("Multiple public constructors found but none annotated with @Inject: " + type.getName());
    }
    return injectableConstructors.get(0);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the parameters of the method.
   * 
   * @param method  the method to examine, not null
   * @return the list of parameters, not null
   */
  public static List<Parameter> getParameters(Method method) {
    return getParameters(method,
                         method.getDeclaringClass(),
                         method.getGenericParameterTypes(),
                         method.getParameterAnnotations());
  }

  /**
   * Gets the parameters of the constructor.
   * 
   * @param constructor  the constructor to examine, not null
   * @return the list of parameters, not null
   */
  public static List<Parameter> getParameters(Constructor<?> constructor) {
    return getParameters(constructor,
                         constructor.getDeclaringClass(),
                         constructor.getGenericParameterTypes(),
                         constructor.getParameterAnnotations());
  }

  private static List<Parameter> getParameters(AccessibleObject ctorOrMethod,
                                               Class<?> declaringClass,
                                               Type[] genericTypes,
                                               Annotation[][] allAnnotations) {
    String[] paramNames = s_paranamer.lookupParameterNames(ctorOrMethod);
    List<Parameter> parameters = Lists.newArrayList();
    for (int i = 0; i < genericTypes.length; i++) {
      Map<Class<?>, Annotation> annotationMap = Maps.newHashMap();
      Type genericType = genericTypes[i];
      Annotation[] annotations = allAnnotations[i];
      for (Annotation annotation : annotations) {
        annotationMap.put(annotation.annotationType(), annotation);
      }
      parameters.add(new Parameter(declaringClass, paramNames[i], genericType, i, annotationMap));
    }
    return parameters;
  }

  //-------------------------------------------------------------------------
  /**
   * Creates function metadata for a named method on a class.
   * <p>
   * This is for testing and isn't intended to be robust.
   * 
   * @param functionType  the interface declaring the function method, not null
   * @param methodName  the name of the method, not null
   * @return the meta-data for the function, not null
   */
  public static FunctionMetadata createMetadata(Class<?> functionType, String methodName) {
    return new FunctionMetadata(getMethod(functionType, methodName));
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a named method on a type.
   * <p>
   * Only public methods are considered.
   * This only works if there is exactly one method with a matching name.
   * If there are zero or multiple methods with a matching name an exception is thrown.
   * 
   * @param type  the type declaring the method, not null
   * @param methodName  the name of the method, not null
   * @return the method, not null
   * @throws IllegalArgumentException if the method is not found,
   *  or there is more than one method in the class with a matching name
   */
  public static Method getMethod(Class<?> type, String methodName) {
    Method[] methods = type.getMethods();
    List<Method> foundMethods = new ArrayList<>();

    for (Method method : methods) {
      if (methodName.equals(method.getName())) {
        foundMethods.add(method);
      }
    }
    if (foundMethods.size() > 1) {
      throw new IllegalArgumentException("Multiple methods found matching name: " + methodName);
    }
    if (foundMethods.isEmpty()) {
      throw new IllegalArgumentException("No method found: " + methodName);
    }
    return foundMethods.get(0);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the complete set of supertypes of a class.
   * <p>
   * This includes superclasses and all interfaces.
   * 
   * @param type  the type to examine, not null
   * @return the set of supertypes, not null
   */
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

  /**
   * Gets the complete set of interfaces of a type.
   * 
   * @param type  the type to examine, not null
   * @return the set of interfaces, not null
   */
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

  //-------------------------------------------------------------------------
  /**
   * Checks if at least one method has the specified annotation.
   * 
   * @param type  the type to examine, not null
   * @param annotation  the annotation to find, not null
   * @return true if at least one method has the annotation
   */
  public static boolean hasMethodAnnotation(Class<?> type, Class<? extends Annotation> annotation) {
    for (Method method : type.getMethods()) {
      if (method.getAnnotation(annotation) != null) {
        return true;
      }
    }
    return false;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the real object behind a proxy.
   * <p>
   * If object isn't a proxy it is returned. If it's a proxy the underlying object is returned. If there are multiple
   * proxies this method recurses until it finds the real object.
   * All proxies must have an invocation handler of type {@link ProxyInvocationHandler}.
   * 
   * @param object  an object, possibly a proxy, not null
   * @return the real object behind the proxy, not null
   */
  public static Object getProxiedObject(Object object) {
    // if object isn't a proxy then we've reached the end of the chain of proxies
    if (!Proxy.isProxyClass(object.getClass())) {
      return object;
    }
    ProxyInvocationHandler invocationHandler = (ProxyInvocationHandler) Proxy.getInvocationHandler(object);
    return getProxiedObject(invocationHandler.getReceiver());
  }

  /**
   * Converts a set of inputs to security types.
   * 
   * @param securities  the collection of input securities
   * @return a set of the types of the securities
   */
  public static Set<Class<?>> getSecurityTypes(List<ManageableSecurity> securities) {
    Set<Class<?>> securityTypes = new HashSet<>();

    for (ManageableSecurity security : ArgumentChecker.notNull(securities, "securities")) {
      securityTypes.add(security.getClass());
    }
    return securityTypes;
  }

  /**
   * Returns the cause of an exception if it's more meaningful than
   * the exception itself. {@link InvocationTargetException} and
   * {@link UndeclaredThrowableException} are exceptions which are
   * thrown by the proxies in the engine when the underlying function
   * being proxied throws an exception. Generally these wrap the
   * underlying exceptions so they don't add anything except noise to
   * the stack traces. Unwrapping the underlying exceptions makes it
   * much easier to see what actually went wrong.
   *
   * @param ex  an exception
   * @return the underlying cause of the exception
   */
  public static Exception getCause(Exception ex) {
    return exceptionHasMoreMeaningfulCause(ex) ? getCause((Exception) ex.getCause()) : ex;
  }

  private static boolean exceptionHasMoreMeaningfulCause(Exception ex) {
    return (ex instanceof InvocationTargetException || ex instanceof UndeclaredThrowableException) &&
        ex.getCause() != null && ex.getCause() instanceof Exception;
  }

  /**
   * @param type a type
   * @return true if the type is annotated with {@link Config}
   */
  public static boolean isConfig(Class<?> type) {
    return type.getAnnotation(Config.class) != null;
  }
}
