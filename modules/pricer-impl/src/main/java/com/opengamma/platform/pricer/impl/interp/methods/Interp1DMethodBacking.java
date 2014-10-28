/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.interp.methods;

import com.opengamma.platform.pricer.impl.interp.PP_t;

/**
 * The interface for classes contracted to undertake 1D interpolation.
 */
public interface Interp1DMethodBacking {

  /**
   * The mutation that should be applied to the 'y' variable prior to passing it to a method that creates a PP_t.
   * The default mutation is a NOP.
   * @param y the 'y' value to mutate.
   * @return the mutated 'y' value.
   */
  public default double mutateYAsCtor(double y)
  {
    return y;
  }

  /**
   * The mutation that should be applied to the 'y' variable prior to passing it to a method that creates a PP_t.
   * The default mutation is a NOP (a literal pass through, no additional memory is used).
   * @param y the 'y' value to mutate.
   * @return the mutated 'y' value.
   */
  public default double[] mutateYAsCtor(double[] y)
  {
    return y;
  }

  /**
   * The required assembly phase, this phase takes a mutated 'y' from the mutateYasCtor() method and a non mutated x and must return a suitable PP_t. 
   * @param x the 'x' values used in the interpolation
   * @param y the mutated 'y' values used in the interpolation
   * @return a PP_t struct representing 'x' and mutated 'y' in pp form.
   */
  public PP_t assembler(double[] x, double[] y);

  /**
   * Convenience method to create a 'pp' form that takes care of mutating the 'y' coordinate as specified.
   * @param x the 'x' values used in the interpolation
   * @param y the 'y' values used in the interpolation
   * @return a PP_t struct representing 'x' and mutated 'y' in pp form.
   */
  public default PP_t createpp(double[] x, double[] y) {
    assert (x.length == y.length);
    return assembler(x, mutateYAsCtor(y));
  }

  /**
   * Post calling PPval in the Interpolator1D methods this mutation will be applied to the interpolated 'y' values.
   * The default mutation is a NOP.
   * @param y the interpolated 'y' value to which the mutation shall be applied. 
   * @return the mutated 'y' value.
   */
  public default double mutatePPvalresultWith(double y)
  {
    return y;
  }

  /**
   * Post calling PPval in the Interpolator1D methods this mutation will be applied to the interpolated 'y' values.
   * The default mutation is a NOP (a literal pass through, no additional memory is used).
   * @param y the 'y' values to mutate.
   * @return the mutated 'y' values.
   */
  public default double[] mutatePPvalresult(double[] y)
  {
    return y;
  }
}
