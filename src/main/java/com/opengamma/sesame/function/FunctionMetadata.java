/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.text.Position;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.core.position.Trade;
import com.opengamma.core.security.Security;
import com.opengamma.sesame.config.ConfigUtils;
import com.opengamma.util.ArgumentChecker;

/**
 * TODO should this be called OutputMetadata?
 * TODO should there be 2 subtypes, one that takes an input from the engine and the other that only uses params?
 * if so move the constructor logic 2 a factory method that decides what to create
 * or just have a boolean flag and a different invoker type?
 */
public class FunctionMetadata {

  private static final Set<Class<?>> s_inputTypes = ImmutableSet.of(Trade.class, Position.class, Security.class);

  private final Method _method;
  // TODO mapping from method args to params - how will this work?
  // inputs are a target and map of param names to arg values
  // need map of param name to index

  // index of target param
  // the invoker needs this
  // set of valid input types - hard-code for now but will need to expand for vectorization
  // search params for input types. zero (specific output) or one (portfolio output) are allowed
  // decorate security functions with a function taking position or trade
  // allow adapting from one input type (from the engine) to another (passed to the root function)
  // hard-code adaptor for positionOrTrade -> security for now
  // will probably have to expand that or make it configurable or smarter for vectorization

  // need to present the required args to the user in the UI
  // need to know the param names and types
  private final Map<String, Parameter> _parameters = Maps.newHashMap();
  // TODO invoker

  /** The input parameter, null if there isn't one. */
  private final Parameter _inputParameter;
  private final TypeMetadata _declaringType;

  /* package */ FunctionMetadata(Method method, TypeMetadata declaringType) {
    _declaringType = declaringType;
    _method = method;
    String outputName = method.getAnnotation(Output.class).value();
    List<Parameter> parameters = ConfigUtils.getParameters(method);
    Parameter inputParameter = null;
    for (Parameter parameter : parameters) {
      _parameters.put(parameter.getName(), parameter);
      for (Class<?> allowedInputType : s_inputTypes) {
        if (allowedInputType.isAssignableFrom(parameter.getType())) {
          if (inputParameter == null) {
            inputParameter = parameter;
          } else {
            throw new IllegalArgumentException("");
          }
        }
      }
    }
    _inputParameter = inputParameter;
  }

  public Invoker getInvoker(Object receiver) {
    ArgumentChecker.notNull(receiver, "receiver");
    return new MethodInvoker(receiver);
  }

  // TODO could replace with generating byte code on the fly if the reflection turns out to be a problem
  private class MethodInvoker implements Invoker {

    private final Object _receiver;

    private MethodInvoker(Object receiver) {
      // TODO check the receiver is compatible with _declaringType
      _receiver = receiver;
    }

    @Override
    public Object invoke(Object input, Map<String, Object> args) {
      Object[] argArray = new Object[_parameters.size()];
      for (Map.Entry<String, Object> entry : args.entrySet()) {
        String arg = entry.getKey();
        Parameter parameter = _parameters.get(arg);
        if (parameter != null) {
          argArray[parameter.getOrdinal()] = entry.getValue();
        }
      }
      if (_inputParameter != null) {
        argArray[_inputParameter.getOrdinal()] = input;
      }
      // TODO check for nulls provided fro non-nullable parameters, including target
      try {
        _method.invoke(_receiver, argArray);
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new OpenGammaRuntimeException("Failed to invoke method", e);
      }
    }
  }
}
