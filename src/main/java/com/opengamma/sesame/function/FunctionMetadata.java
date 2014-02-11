/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Maps;
import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.sesame.config.EngineFunctionUtils;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

/**
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

  /** The types of arguments provided by the engine, e.g. trades, positions etc. */
  private final Set<Class<?>> _inputTypes;

  public FunctionMetadata(FunctionMetadata copyFrom) {
    _method = copyFrom._method;
    _parameters = copyFrom._parameters;
    _inputParameter = copyFrom._inputParameter;
    _outputName = copyFrom._outputName;
    _inputTypes = copyFrom._inputTypes;
  }

  public FunctionMetadata(Method method) {
    this(method, Collections.<Class<?>>emptySet());
  }

  public FunctionMetadata(Method method, Set<Class<?>> inputTypes) {
    _inputTypes = inputTypes;
    _method = ArgumentChecker.notNull(method, "method");
    Output annotation = method.getAnnotation(Output.class);
    if (annotation == null) {
      throw new IllegalArgumentException("method " + method + " isn't annotated with @Output");
    }
    _outputName = annotation.value();
    Pair<Map<String, Parameter>, Parameter> parameters = getParameters(method, inputTypes);
    _parameters = parameters.getFirst();
    _inputParameter = parameters.getSecond();
  }

  private static Pair<Map<String, Parameter>, Parameter> getParameters(Method method, Set<Class<?>> inputTypes) {
    Map<String, Parameter> parameterMap = Maps.newHashMap();
    List<Parameter> parameters = EngineFunctionUtils.getParameters(method);
    Parameter inputParameter = null;
    params: for (Parameter parameter : parameters) {
      parameterMap.put(parameter.getName(), parameter);
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
    return Pairs.of(parameterMap, inputParameter);
  }
  
  public InvokableFunction getInvokableFunction(Object receiver) {
    // receiver might be a proxy. we need the real object so we can get the parameter names from the bytecode
    Object realReceiver = EngineFunctionUtils.getProxiedObject(receiver);
    if (_method.getDeclaringClass().isInterface()) {
      Class<?> receiverClass = realReceiver.getClass();
      Method method;
      try {
        method = receiverClass.getMethod(_method.getName(), _method.getParameterTypes());
      } catch (NoSuchMethodException e) {
        // this shouldn't happen - the receiver must have the same method as the proxy
        throw new OpenGammaRuntimeException("Unexpected exception", e);
      }
      Pair<Map<String, Parameter>, Parameter> parameters = getParameters(method, _inputTypes);
      return new MethodInvokableFunction(receiver, parameters.getFirst(), parameters.getSecond(), _outputName, _method);
    } else {
      return new MethodInvokableFunction(receiver, _parameters, _inputParameter, _outputName, _method);
    }
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

}
