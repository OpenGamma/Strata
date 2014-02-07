/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Maps;
import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.sesame.config.ConfigUtils;
import com.opengamma.sesame.config.FunctionArguments;
import com.opengamma.util.ArgumentChecker;

/**
 * TODO should this be called OutputMetadata?
 * TODO joda bean?
 */
public class FunctionMetadata {

  // TODO if this class needs to be serializable this needs to be stored in a different way
  /** The method that is the function implementation. */
  private final Method _method;

  /** Method parameters keyed by name. */
  private final Map<String, Parameter> _parameters;

  /** The input parameter, null if there isn't one. */
  private final Parameter _inputParameter;

  /** The name of the output produced by the function. */
  private final String _outputName;

  public FunctionMetadata(FunctionMetadata copyFrom) {
    _method = copyFrom._method;
    _parameters = copyFrom._parameters;
    _inputParameter = copyFrom._inputParameter;
    _outputName = copyFrom._outputName;
  }

  public FunctionMetadata(Method method) {
    this(method, Collections.<Class<?>>emptySet());
  }

  public FunctionMetadata(Method method, Set<Class<?>> inputTypes) {
    _method = method;
    _parameters = Maps.newHashMap();
    Output annotation = method.getAnnotation(Output.class);
    if (annotation == null) {
      throw new IllegalArgumentException("method " + method + " isn't annotated with @Output");
    }
    _outputName = annotation.value();
    List<Parameter> parameters = ConfigUtils.getParameters(method);
    Parameter inputParameter = null;
    params: for (Parameter parameter : parameters) {
      _parameters.put(parameter.getName(), parameter);
      // TODO this logic is broken
      for (Class<?> allowedInputType : inputTypes) {
        if (allowedInputType.isAssignableFrom(parameter.getType())) {
          if (inputParameter == null) {
            inputParameter = parameter;
            continue params;
          } else {
            throw new IllegalArgumentException("Multiple parameters found with a type in the set of permitted input " +
                                                   "types. Only one is allowed. Input types: " + inputTypes);
          }
        }
      }
    }
    _inputParameter = inputParameter;
  }

  public InvokableFunction getInvokableFunction(Object receiver) {
    // TODO can we get the method parameter names from the receiver method? will it be a class? a proxy?
    // if it's a proxy can I drill through to the underlying object and get the class?
    return new MethodInvokableFunction(ArgumentChecker.notNull(receiver, "receiver"));
  }

  public Class<?> getDeclaringType() {
    return _method.getDeclaringClass();
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
    return Objects.hash(_method, _parameters, _inputParameter, _outputName);
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
        Objects.equals(this._outputName, other._outputName);
  }

  @Override
  public String toString() {
    return "FunctionMetadata [" +
        "_method=" + _method +
        ", _parameters=" + _parameters +
        ", _inputParameter=" + _inputParameter +
        ", _outputName='" + _outputName + "'" +
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
    public Object invoke(Object input, FunctionArguments args) {
      Object[] argArray = new Object[_parameters.size()];
      for (Parameter parameter : _parameters.values()) {
        argArray[parameter.getOrdinal()] = args.getArgument(parameter.getName());
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
