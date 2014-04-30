/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.core.link.Link;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.config.EngineUtils;
import com.opengamma.sesame.config.FunctionArguments;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;

/**
 * {@link InvokableFunction} implementation that wraps a {@link Method} object and invokes it using reflection.
 */
/* package */ class MethodInvokableFunction implements InvokableFunction {

  private static final Logger s_logger = LoggerFactory.getLogger(MethodInvokableFunction.class);

  /** The receiver of the method call. */
  private final Object _receiver;

  /** The underlying (i.e. non-proxied) receiver of the method call. */
  private final Object _underlyingReceiver;

  // TODO if this class needs to be serializable this needs to be stored in a different way
  /** The method that is the function implementation. */
  private final Method _method;

  /** Method parameters keyed by name. */
  private final Map<String, Parameter> _parameters;

  /** The environment parameter, null if there isn't one. */
  private final Parameter _environmentParameter;

  /** The input parameter, null if there isn't one. */
  private final Parameter _inputParameter;

  /* package */ MethodInvokableFunction(Object receiver,
                                        Map<String, Parameter> parameters,
                                        Parameter environmentParameter,
                                        Parameter inputParameter,
                                        Method method) {
    _environmentParameter = environmentParameter;
    _method = ArgumentChecker.notNull(method, "method");
    // TODO check the receiver is compatible with _declaringType
    _receiver = ArgumentChecker.notNull(receiver, "receiver");
    _underlyingReceiver = EngineUtils.getProxiedObject(_receiver);
    _parameters = ArgumentChecker.notNull(parameters, "parameters");
    _inputParameter = inputParameter;
  }

  @Override
  public Object invoke(Environment env, Object input, FunctionArguments args) {
    Object[] argArray = new Object[_parameters.size()];

    if (_environmentParameter != null) {
      argArray[_environmentParameter.getOrdinal()] = env;
    }
    if (_inputParameter != null) {
      argArray[_inputParameter.getOrdinal()] = input;
    }
    boolean argsMissing = false;

    for (Parameter parameter : _parameters.values()) {
      if (parameter != _inputParameter && parameter != _environmentParameter) {
        Object arg = args.getArgument(parameter.getName());
        Object resolvedArg = resolveArgument(parameter, arg);
        argArray[parameter.getOrdinal()] = resolvedArg;

        if (resolvedArg == null && !parameter.isNullable()) {
          argsMissing = true;
        }
      }
    }
    if (argsMissing) {
      return missingArguments(argArray);
    }
    // TODO check for unexpected parameters?
    try {
      return _method.invoke(_receiver, argArray);
    } catch (IllegalAccessException e) {
      throw new OpenGammaRuntimeException("Unable to access method", e);
    } catch (InvocationTargetException e) {
      Exception cause = EngineUtils.getCause(e);
      String methodName = _method.getDeclaringClass().getSimpleName() + "." + _method.getName() + "()";
      s_logger.warn("Exception invoking " + methodName, cause);
      return Result.failure(cause);
    }
  }

  /**
   * Returns a failure result when no arguments or null arguments are provided for non-nullable parameters.
   *
   * @param argArray the method arguments
   * @return a failure result with an error message about missing arguments
   */
  private Result<Object> missingArguments(Object[] argArray) {
    Result<Object> result;
    List<Parameter> missingArgs = new ArrayList<>();

    for (Parameter parameter : _parameters.values()) {
      Object arg = argArray[parameter.getOrdinal()];

      if (arg == null && !parameter.isNullable()) {
        missingArgs.add(parameter);
      }
    }
    Collections.sort(missingArgs, new Comparator<Parameter>() {
      @Override
      public int compare(Parameter o1, Parameter o2) {
        return Integer.compare(o1.getOrdinal(), o2.getOrdinal());
      }
    });

    String message;
    String methodName = _method.getDeclaringClass().getSimpleName() + "." + _method.getName() + "()";

    if (missingArgs.size() == 1) {
      Parameter parameter = missingArgs.get(0);
      message = "No argument provided for non-nullable parameter for method " + methodName + ", " +
          "parameter '" + parameter.getName() + "', type " + parameter.getType().getName();
    } else {
      String messagePrefix = "No arguments provided for non-nullable parameters of method " + methodName + ", parameters ";
      List<String> paramDescriptions = new ArrayList<>(missingArgs.size());

      for (Parameter parameter : missingArgs) {
        paramDescriptions.add(parameter.getType().getSimpleName() + " " + parameter.getName());
      }
      message = messagePrefix + paramDescriptions;
    }
    result = Result.failure(FailureStatus.MISSING_ARGUMENT, message);
    return result;
  }

  /**
   * If the argument is a {@link Link} and the parameter doesn't expect a link then this method resolves and returns
   * the link, otherwise it returns the argument unchanged.
   *
   * @param parameter The parameter corresponding to the argument
   * @param arg The argument
   * @return The resolved argument
   */
  private static Object resolveArgument(Parameter parameter, Object arg) {
    if (!(arg instanceof Link) || Link.class.isAssignableFrom(parameter.getClass())) {
      // the arg isn't a link, just return it
      return arg;
    } else {
      return ((Link<?>) arg).resolve();
    }
  }

  @Override
  public Object getReceiver() {
    return _receiver;
  }

  @Override
  public Object getUnderlyingReceiver() {
    return _underlyingReceiver;
  }

  @Override
  public Class<?> getDeclaringClass() {
    return _method.getDeclaringClass();
  }
}
