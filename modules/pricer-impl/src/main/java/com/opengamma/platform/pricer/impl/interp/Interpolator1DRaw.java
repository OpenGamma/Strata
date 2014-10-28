/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.interp;

import java.util.HashMap;

import com.opengamma.platform.pricer.impl.interp.methods.Interp1DMethodBacking;
import com.opengamma.platform.pricer.impl.interp.methods.Linear;
import com.opengamma.platform.pricer.impl.interp.methods.LogNaturalCubicMonotonicityPreserving;

/**
 * Interpolation in 1 dimension.
 */
public abstract class Interpolator1DRaw {

  private static HashMap<InterpMethod, Interp1DMethodBacking> backing = new HashMap<InterpMethod, Interp1DMethodBacking>();
  static
  {
    backing.put(InterpMethod.LINEAR, Linear.s_instance);
    backing.put(InterpMethod.LOG_PCHIP_HYMAN, LogNaturalCubicMonotonicityPreserving.s_instance);
  }

  /**
   * Create a pp struct for a given interpolation method from a set of points.
   * @param x the x values
   * @param y the y values
   * @param meth the method of interpolation
   * @return a pp struct suitable for interpolation using method 'meth', note that the Interp1DMethodBacking.mutateYAsCtor() method
   * will have been applied to obtain the PP_t and so if a Interp1DMethodBacking.mutatePPvalresult() is applicable it will need
   * to be manually applied to results from the PPval class.
   */
  public static PP_t interp(double[] x, double[] y, InterpMethod meth) {
    Interp1DMethodBacking thisbacking = backing.get(meth);
    PP_t thePP = thisbacking.createpp(x, y);
    return thePP;
  }

  /**
   * Interpolate from a set of points.
   * @param x the x values
   * @param y the y values
   * @param xx the points in 'x' at which to interpolate a value
   * @param meth the method of interpolation
   * @return the 'y' values at locations 'xx' interpolated via method 'meth'
   */
  public static double[] interp(double[] x, double[] y, double[] xx, InterpMethod meth) {
    Interp1DMethodBacking thisbacking = backing.get(meth);
    PP_t thePP = thisbacking.createpp(x, y);
    return thisbacking.mutatePPvalresult(PPval.ppval(thePP, xx));
  }

  /**
   * Interpolate from a set of points.
   * @param x the x values
   * @param y the y values
   * @param xx the points in 'x' at which to interpolate a value
   * @param lookups the index in 'x' of left sided knot for each 'xx', infinite intervals are assumed.
   * @param meth the method of interpolation
   * @return the 'y' values at locations 'xx' interpolated via method 'meth'
   */
  public static double[] interp(double[] x, double[] y, double[] xx, int[] lookups, InterpMethod meth) {
    Interp1DMethodBacking thisbacking = backing.get(meth);
    PP_t thePP = thisbacking.createpp(x, y);
    return thisbacking.mutatePPvalresult(PPval.ppval_preempt_lookup(thePP, xx, lookups));
  }

  /**
   * Interpolate from a precomputed pp struct.
   * @param xx the points in 'x' at which to interpolate a value
   * @param meth the method of interpolation
   * @return the 'y' values at locations 'xx' interpolated via method 'meth'
   */
  public static double[] interp(double[] xx, InterpMethod meth, PP_t pp) {
    Interp1DMethodBacking thisbacking = backing.get(meth);
    return thisbacking.mutatePPvalresult(PPval.ppval(pp, xx));
  }

  /**
   * Interpolate from a precomputed pp struct and precomputed lookup table.
   * @param xx the points in 'x' at which to interpolate a value
   * @param lookups the index in 'x' of left sided knot for each 'xx', infinite intervals are assumed.
   * @param meth the method of interpolation
   * @return the 'y' values at locations 'xx' interpolated via method 'meth'
   */
  public static double[] interp(double[] xx, int[] lookups, InterpMethod meth, PP_t pp) {
    Interp1DMethodBacking thisbacking = backing.get(meth);
    return thisbacking.mutatePPvalresult(PPval.ppval_preempt_lookup(pp, xx, lookups));
  }

}
