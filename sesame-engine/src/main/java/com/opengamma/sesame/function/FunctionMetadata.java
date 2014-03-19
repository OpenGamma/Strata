/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.Maps;
import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.OutputName;
import com.opengamma.sesame.config.EngineUtils;
import com.opengamma.util.ArgumentChecker;

/**
 * TODO joda bean?
 */
public class FunctionMetadata {

  // TODO if this class needs to be serializable this needs to be stored in a different way
  /** The method that is the function implementation. */
  private final Method _method;

  /** Method parameters keyed by name. */
  private final Map<String, Parameter> _parameters;

  /** The environment parameter, null if there isn't one. */
  private final Parameter _environmentParameter;

  /** The input parameter, null if there isn't one. */
  private final Parameter _inputParameter;

  /** The name of the output produced by the function. */
  private final OutputName _outputName;

  /** The types of arguments provided by the engine, e.g. trades, positions etc. */
  private final Set<Class<?>> _inputTypes;

  public FunctionMetadata(FunctionMetadata copyFrom) {
    _method = copyFrom._method;
    _parameters = copyFrom._parameters;
    _environmentParameter = copyFrom._environmentParameter;
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
    _outputName = OutputName.of(annotation.value());
    FunctionParameters parameters = getParameters(method, inputTypes);
    _parameters = parameters._parametersByName;
    _environmentParameter = parameters._environmentParameter;
    _inputParameter = parameters._inputParameter;
  }

  private static FunctionParameters getParameters(Method method, Set<Class<?>> inputTypes) {
    Map<String, Parameter> parameterMap = Maps.newHashMap();
    List<Parameter> parameters = EngineUtils.getParameters(method);
    List<Parameter> inputParameters = new ArrayList<>();
    List<Parameter> environmentParameters = new ArrayList<>();

    for (Parameter parameter : parameters) {
      parameterMap.put(parameter.getName(), parameter);

      if (parameter.getType().equals(Environment.class)) {
        environmentParameters.add(parameter);
      } else {
        Parameter inputParameter = findInputParameter(inputTypes, parameter);
        if (inputParameter != null) {
          inputParameters.add(inputParameter);
        }
      }
    }
    Parameter environmentParameter = getSingleParameter(environmentParameters);
    Parameter inputParameter = getSingleParameter(inputParameters);
    return new FunctionParameters(parameterMap, environmentParameter, inputParameter);
  }

  private static Parameter getSingleParameter(List<Parameter> parameters) {
    if (parameters.size() > 1) {
      throw new IllegalArgumentException("Multiple parameters found where only one is expected");
    } else if (parameters.size() == 1) {
      return parameters.get(0);
    } else {
      return null;
    }
  }

  private static Parameter findInputParameter(Set<Class<?>> inputTypes, Parameter parameter) {
    for (Class<?> allowedInputType : inputTypes) {
      if (allowedInputType.isAssignableFrom(parameter.getType())) {
        return parameter;
      }
    }
    return null;
  }

  public InvokableFunction getInvokableFunction(Object receiver) {
    // receiver might be a proxy. we need the real object so we can get the parameter names from the bytecode
    Object realReceiver = EngineUtils.getProxiedObject(receiver);
    if (_method.getDeclaringClass().isInterface()) {
      Class<?> receiverClass = realReceiver.getClass();
      Method method;
      try {
        method = receiverClass.getMethod(_method.getName(), _method.getParameterTypes());
      } catch (NoSuchMethodException e) {
        // this shouldn't happen - the receiver must have the same method as the proxy
        throw new OpenGammaRuntimeException("Unexpected exception", e);
      }
      FunctionParameters parameters = getParameters(method, _inputTypes);
      return new MethodInvokableFunction(receiver,
                                         parameters._parametersByName,
                                         parameters._environmentParameter,
                                         parameters._inputParameter,
                                         _method);
    } else {
      return new MethodInvokableFunction(receiver,
                                         _parameters,
                                         _environmentParameter,
                                         _inputParameter,
                                         _method);
    }
  }

  public Class<?> getDeclaringType() {
    return _method.getDeclaringClass();
  }

  public OutputName getOutputName() {
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

  private static final class FunctionParameters {

    private final Map<String, Parameter> _parametersByName;

    @Nullable
    private final Parameter _environmentParameter;

    @Nullable
    private final Parameter _inputParameter;

    private FunctionParameters(Map<String, Parameter> parametersByName,
                               Parameter environmentParameter,
                               Parameter inputParameter) {
      _parametersByName = parametersByName;
      _environmentParameter = environmentParameter;
      _inputParameter = inputParameter;
    }
  }
}
