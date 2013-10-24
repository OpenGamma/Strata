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
/* package */ class Injector {

  // TODO multiple instances per type?
  private final Map<Class<?>, Object> _infrastructure;

  /* package */ Injector(Map<Class<?>, Object> infrastructure) {
    ArgumentChecker.notNull(infrastructure, "infrastructure");
    _infrastructure = infrastructure;
  }

  /* package */ Injector() {
    this(Collections.<Class<?>, Object>emptyMap());
  }

  @SuppressWarnings("unchecked")
  /* package */ <T> T create(Class<T> type, ColumnRequirement columnRequirement) {
    Class<?> implType = columnRequirement.getImplementation(type);
    Constructor<?> constructor = ConfigUtils.getConstructor(implType);
    List<ConstructorParameter> parameters = ConfigUtils.getParameters(constructor);
    List<Object> arguments = Lists.newArrayListWithCapacity(parameters.size());
    FunctionArguments functionArguments;
    if (columnRequirement.getFunctionArguments().containsKey(implType)) {
      functionArguments = columnRequirement.getFunctionArguments().get(implType);
    } else {
      functionArguments = FunctionArguments.EMPTY;
    }
    for (ConstructorParameter parameter : parameters) {
      Object argument = getArgument(parameter, functionArguments);
      if (argument != null) {
        arguments.add(argument);
      } else {
        arguments.add(create(parameter.getType(), columnRequirement));
      }
    }
    try {
      return (T) constructor.newInstance(arguments.toArray());
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new OpenGammaRuntimeException("Failed to create object of type " + type.getName() +
                                              " with requirement" + columnRequirement, e);
    }
  }

  // TODO will need the columnRequirements as an arg if we're going to recurse
  private Object getArgument(ConstructorParameter parameter, FunctionArguments functionArguments) {
    /*
    there are 4 types of argument:
      infrastructure components
      other functions (need to recurse)
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
        // TODO check the default type is supported and throw more informative exception
        return StringConvert.INSTANCE.convertFromString(parameter.getType(), paramAnnotation.defaultValue());
      }
    } else {
      return null;
    }
  }
}
