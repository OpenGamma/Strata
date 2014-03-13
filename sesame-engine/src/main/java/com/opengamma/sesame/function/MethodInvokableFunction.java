/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.core.link.Link;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.OutputName;
import com.opengamma.sesame.config.EngineUtils;
import com.opengamma.sesame.config.FunctionArguments;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;

/**
 * TODO javadocs
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

  /** The name of the output produced by the function. */
  private final OutputName _outputName;

  /* package */ MethodInvokableFunction(Object receiver,
                                        Map<String, Parameter> parameters,
                                        Parameter environmentParameter,
                                        Parameter inputParameter,
                                        OutputName outputName,
                                        Method method) {
    _environmentParameter = environmentParameter;
    _method = ArgumentChecker.notNull(method, "method");
    // TODO check the receiver is compatible with _declaringType
    _receiver = ArgumentChecker.notNull(receiver, "receiver");
    _underlyingReceiver = EngineUtils.getProxiedObject(_receiver);
    _parameters = ArgumentChecker.notNull(parameters, "parameters");
    _inputParameter = inputParameter;
    _outputName = ArgumentChecker.notNull(outputName, "outputName");
  }

  @Override
  public Object invoke(Environment env, Object input, FunctionArguments args) {
    Object[] argArray = new Object[_parameters.size()];

    for (Parameter parameter : _parameters.values()) {
      Object arg = args.getArgument(parameter.getName());
      Object resolvedArg = resolveArgument(parameter, arg);
      argArray[parameter.getOrdinal()] = resolvedArg;
    }
    if (_environmentParameter != null) {
      argArray[_environmentParameter.getOrdinal()] = env;
    }
    if (_inputParameter != null) {
      argArray[_inputParameter.getOrdinal()] = input;
    }
    // TODO check for nulls provided for non-nullable parameters, including target?
    // TODO check for unexpected parameters?
    // TODO use @Nullable / @NotNull / @Nonnull
    try {
      return _method.invoke(_receiver, argArray);
    } catch (IllegalAccessException e) {
      throw new OpenGammaRuntimeException("Unable to access method", e);
    } catch (InvocationTargetException e) {
      Exception cause = getCause(e);
      String methodName = _method.getDeclaringClass().getSimpleName() + "." + _method.getName() + "()";
      s_logger.warn("Exception invoking " + methodName, cause);
      return Result.failure(cause);
    }
  }

  /**
   * Returns the cause of an exception if it's an {@link InvocationTargetException} or an
   * {@link UndeclaredThrowableException}. These are the exception types that always wrap the underlying exceptions
   * throw inside a proxy so they don't add anything except noise to the stack traces. Unwrapping the underlying
   * exceptions makes it much easier to see what actually went wrong.
   *
   * @param e an exception
   * @return the underlying cause of the exception
   */
  private static Exception getCause(Exception e) {
    if (!(e instanceof InvocationTargetException) && !(e instanceof UndeclaredThrowableException)) {
      return e;
    }
    if (e.getCause() != null && e.getCause() instanceof Exception) {
      return getCause((Exception) e.getCause());
    } else {
      return e;
    }
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
      return ((Link<?, ?>) arg).resolve();
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
  public OutputName getOutputName() {
    return _outputName;
  }
}
