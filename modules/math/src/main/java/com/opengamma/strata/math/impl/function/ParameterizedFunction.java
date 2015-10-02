/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import com.opengamma.strata.collect.ArgChecker;

/**
 * This class defines a 1-D function that takes both its argument and
 * parameters inputs into the {@link #evaluate} method. The function can also
 * be converted into a 1-D function of the arguments or a 1-D function of the
 * parameters.
 *
 * For example, assume that there is a function $f(x, \overline{a})$ defined as:
 * $$
 * \begin{align*}
 * f(x, \overline{a}) = a_0 + a_1 x + a_2 x^2 + a_3 x^6
 * \end{align*}
 * $$
 * The {@link #evaluate} method takes the value $x$ and the parameters
 * $\overline{a}$ and returns the result. If the function is converted into a
 * function of the arguments, the resulting function $g(x)$ is:
 * $$
 * \begin{align*}
 * g(x) = a_0 + a_1 x + a_2 x^2 + a_3 x^6
 * \end{align*}
 * $$
 * with $\overline{a}$ constant. If the function is converted into a function
 * of the parameters, the resulting function $h(\overline{a})$ is:
 * $$
 * \begin{align*}
 * h(\overline{a}) = a_0 + a_1 x + a_2 x^2 + a_3 x^6
 * \end{align*}
 * $$
 * with $x$ constant.
 *
 * This class is particularly useful when trying to fit the parameters of a model,
 * such as in a Nelson Siegel Svennson bond curve model.
 * 
 * @param <S> Type of arguments
 * @param <T> Type of parameters
 * @param <U> Type of result
 */
public abstract class ParameterizedFunction<S, T, U> {

  /**
   * @param x The value at which the function is to be evaluated
   * @param parameters The parameters of the function
   * @return The value of the function at <i>x</i> with the parameters as input
   */
  public abstract U evaluate(S x, T parameters);

  /**
   * @param x The value at which the function is to be evaluated, not null
   * @return A function that is always evaluated at <i>x</i> for different values of the parameters
   */
  public Function1D<T, U> asFunctionOfParameters(final S x) {
    ArgChecker.notNull(x, "x");
    return new Function1D<T, U>() {

      @Override
      public final U evaluate(final T params) {
        return ParameterizedFunction.this.evaluate(x, params);
      }

    };
  }

  /**
   * @param params The parameters for which the function is to be evaluated, not null
   * @return A function that can be evaluated at different <i>x</i> with the input parameters
   */
  public Function1D<S, U> asFunctionOfArguments(final T params) {
    ArgChecker.notNull(params, "params");
    return new Function1D<S, U>() {

      @Override
      public U evaluate(final S x) {
        return ParameterizedFunction.this.evaluate(x, params);
      }

    };
  }

  /**
   * Get the number of parameters 
   * @return the number of parameters 
   */
  public abstract int getNumberOfParameters();

}
