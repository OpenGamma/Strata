/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.joda.convert.StringConvert;

import com.google.common.collect.Lists;
import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.util.ArgumentChecker;

/**
 *
 */
public class Injector {

  // TODO multiple instances per type?
  private final Map<Class<?>, Object> _infrastructure;

  public Injector(Map<Class<?>, Object> infrastructure) {
    ArgumentChecker.notNull(infrastructure, "infrastructure");
    _infrastructure = infrastructure;
  }

  public Injector() {
    this(Collections.<Class<?>, Object>emptyMap());
  }
  public <T> T create(Class<T> type) {
    return create(type, FunctionConfig.EMPTY);
  }

  // TODO only need half the contents of columnRequirement, it's a bit jarring ATM to create valueName and targetType
  @SuppressWarnings("unchecked")
  public  <T> T create(Class<T> type, FunctionConfig config) {
    Class<?> implType = config.getFunctionImplementation(type);
    Constructor<?> constructor = ConfigUtils.getConstructor(implType);
    List<ConstructorParameter> parameters = ConfigUtils.getParameters(constructor);
    FunctionArguments functionArguments = config.getFunctionArguments(implType);
    List<Object> constructorArguments = Lists.newArrayListWithCapacity(parameters.size());
    for (ConstructorParameter parameter : parameters) {
      Object argument = getArgument(parameter, functionArguments);
      if (argument != null) {
        constructorArguments.add(argument);
      } else {
        // TODO cyclic dependencies
        constructorArguments.add(create(parameter.getType(), config));
      }
    }
    try {
      return (T) constructor.newInstance(constructorArguments.toArray());
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new OpenGammaRuntimeException("Failed to create object of type " + type.getName() +
                                              " with requirement" + config, e);
    }
  }

  // TODO will need the columnRequirements as an arg if we're going to recurse
  private Object getArgument(ConstructorParameter parameter, FunctionArguments functionArguments) {
    /*
    there are 4 types of argument:
      infrastructure components
      other functions (return null for these)
      user arguments
      user arguments with defaults generated from @UserParam
    */
    if (_infrastructure.containsKey(parameter.getType())) {
      return _infrastructure.get(parameter.getType());
    } else if (parameter.getAnnotations().containsKey(UserParam.class)) {
      UserParam paramAnnotation = (UserParam) parameter.getAnnotations().get(UserParam.class);
      String paramName = paramAnnotation.name();
      if (functionArguments.hasArgument(paramName)) {
        return functionArguments.getArgument(paramName);
      } else {
        return StringConvert.INSTANCE.convertFromString(parameter.getType(), paramAnnotation.defaultValue());
      }
    } else {
      return null;
    }
  }
}
