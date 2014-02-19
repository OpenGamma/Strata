/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.core.link.Link;
import com.opengamma.sesame.OutputName;
import com.opengamma.sesame.config.FunctionArguments;
import com.opengamma.util.ArgumentChecker;

/**
 *
 */
/* package */ class MethodInvokableFunction implements InvokableFunction {

  /** The receiver of the method call. */
  private final Object _receiver;

  // TODO if this class needs to be serializable this needs to be stored in a different way
  /** The method that is the function implementation. */
  private final Method _method;

  /** Method parameters keyed by name. */
  private final Map<String, Parameter> _parameters;

  /** The input parameter, null if there isn't one. */
  private final Parameter _inputParameter;

  /** The name of the output produced by the function. */
  private final OutputName _outputName;

  /* package */ MethodInvokableFunction(Object receiver,
                                        Map<String, Parameter> parameters,
                                        Parameter inputParameter,
                                        OutputName outputName,
                                        Method method) {
    _method = ArgumentChecker.notNull(method, "method");
    // TODO check the receiver is compatible with _declaringType
    _receiver = ArgumentChecker.notNull(receiver, "receiver");
    _parameters = ArgumentChecker.notNull(parameters, "parameters");
    _inputParameter = inputParameter;
    _outputName = ArgumentChecker.notNull(outputName, "outputName");
  }

  @Override
  public Object invoke(Object input, FunctionArguments args) {
    Object[] argArray = new Object[_parameters.size()];
    for (Parameter parameter : _parameters.values()) {
      Object arg = args.getArgument(parameter.getName());
      Object resolvedArg = resolveArgument(parameter, arg);
      argArray[parameter.getOrdinal()] = resolvedArg;
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

  /**
   * If the argument is a {@link Link} and the parameter doesn't expect a link then this method resolves and returns
   * the link, otherwise it returns the argument unchanged.
   * @param parameter The parameter corresponding to the argument
   * @param arg The argument
   * @return The resolved argument
   */
  private static Object resolveArgument(Parameter parameter, Object arg) {
    Object resolvedArg;
    if (arg instanceof Link) {
      if (Link.class.isAssignableFrom(parameter.getClass())) {
        // the arg is a link and the parameter is a link, just return the arg
        resolvedArg = arg;
      } else {
        // the arg is a link but the parameter isn't - resolve the link and return the result
        resolvedArg = ((Link) arg).resolve();
      }
    } else {
      // the arg isn't a link, just return it
      resolvedArg = arg;
    }
    return resolvedArg;
  }

  @Override
  public Object getReceiver() {
    return _receiver;
  }

  @Override
  public OutputName getOutputName() {
    return _outputName;
  }
}
