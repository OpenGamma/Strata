/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
 * TODO joda bean?
 */
public class FunctionMetadata {

  private static final Set<Class<?>> s_inputTypes = ImmutableSet.of(Trade.class, Position.class, Security.class);

  // TODO if this class needs to be serializable this needs to be stored in a different way
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

  // TODO would it be better to just refer to the class and constructor from here?
  // TODO and where TypeMetadata is constructed in the function repo just use a factory method returning fn meta
  private final Constructor<?> _constructor;
  private final String _outputName;

  /* package */ FunctionMetadata(FunctionMetadata copyFrom) {
    this(copyFrom._method, copyFrom._constructor);
  }

  /* package */ FunctionMetadata(Method method, Constructor<?> constructor) {
    _method = method;
    _constructor = constructor;
    _outputName = method.getAnnotation(Output.class).value();
    List<Parameter> parameters = ConfigUtils.getParameters(method);
    Parameter inputParameter = null;
    for (Parameter parameter : parameters) {
      _parameters.put(parameter.getName(), parameter);
      for (Class<?> allowedInputType : s_inputTypes) {
        if (allowedInputType.isAssignableFrom(parameter.getType())) {
          if (inputParameter == null) {
            inputParameter = parameter;
          } else {
            throw new IllegalArgumentException("Multiple parameters found with a type in the set of permitted input " +
                                                   "types. Only one is allowed. Input types: " + s_inputTypes);
          }
        }
      }
    }
    _inputParameter = inputParameter;
  }

  public InvokableFunction getInvokableFunction(Object receiver) {
    ArgumentChecker.notNull(receiver, "receiver");
    return new MethodInvokableFunction(receiver);
  }

  public Class<?> getDeclaringType() {
    return _constructor.getDeclaringClass();
  }

  public String getOutputName() {
    return _outputName;
  }

  public Class<?> getInputType() {
    if (_inputParameter == null) {
      return null;
    } else {
      return _inputParameter.getType();
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(_method, _parameters, _inputParameter, _constructor);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final FunctionMetadata other = (FunctionMetadata) obj;
    return
        Objects.equals(this._method, other._method) &&
        Objects.equals(this._parameters, other._parameters) &&
        Objects.equals(this._inputParameter, other._inputParameter) &&
        Objects.equals(this._constructor, other._constructor);
  }

  @Override
  public String toString() {
    return "FunctionMetadata [" +
        "_method=" + _method +
        ", _parameters=" + _parameters +
        ", _inputParameter=" + _inputParameter +
        ", _constructor=" + _constructor +
        "]";
  }

  // TODO return value wrapped in metadata (output name, trade/security etc)? for scaling, aggregation, ccy conversion
  private class MethodInvokableFunction implements InvokableFunction {

    private final Object _receiver;

    private MethodInvokableFunction(Object receiver) {
      // TODO check the receiver is compatible with _declaringType
      _receiver = receiver;
    }

    @Override
    public Object getReceiver() {
      return _receiver;
    }

    @Override
    public String getOutputName() {
      return _outputName;
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
      // TODO check for nulls provided for non-nullable parameters, including target?
      // TODO check for unexpected parameters?
      // TODO use @Nullable / @NotNull / @Nonnull
      try {
        return _method.invoke(_receiver, argArray);
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new OpenGammaRuntimeException("Failed to invoke method", e);
      }
    }
  }
}
