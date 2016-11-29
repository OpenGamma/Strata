/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.integration;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory class for 1-D integrators that do not take arguments.
 */
public final class RealFunctionIntegrator1DFactory {
  // TODO add more integration types
  /** Romberg integrator name */
  public static final String ROMBERG = "Romberg";
  /** {@link RombergIntegrator1D} */
  public static final RombergIntegrator1D ROMBERG_INSTANCE = new RombergIntegrator1D();
  /** Simpson integrator name */
  public static final String SIMPSON = "Simpson";
  /** {@link SimpsonIntegrator1D} */
  public static final SimpsonIntegrator1D SIMPSON_INSTANCE = new SimpsonIntegrator1D();
  /** Extended trapezoid integrator name */
  public static final String EXTENDED_TRAPEZOID = "ExtendedTrapezoid";
  /** {@link ExtendedTrapezoidIntegrator1D} */
  public static final ExtendedTrapezoidIntegrator1D EXTENDED_TRAPEZOID_INSTANCE = new ExtendedTrapezoidIntegrator1D();

  private static final Map<String, Integrator1D<Double, Double>> STATIC_INSTANCES;
  private static final Map<Class<?>, String> INSTANCE_NAMES;

  static {
    final Map<String, Integrator1D<Double, Double>> staticInstances = new HashMap<>();
    final Map<Class<?>, String> instanceNames = new HashMap<>();
    staticInstances.put(ROMBERG, ROMBERG_INSTANCE);
    instanceNames.put(ROMBERG_INSTANCE.getClass(), ROMBERG);
    staticInstances.put(SIMPSON, SIMPSON_INSTANCE);
    instanceNames.put(SIMPSON_INSTANCE.getClass(), SIMPSON);
    staticInstances.put(EXTENDED_TRAPEZOID, EXTENDED_TRAPEZOID_INSTANCE);
    instanceNames.put(EXTENDED_TRAPEZOID_INSTANCE.getClass(), EXTENDED_TRAPEZOID);
    STATIC_INSTANCES = new HashMap<>(staticInstances);
    INSTANCE_NAMES = new HashMap<>(instanceNames);
  }

  private RealFunctionIntegrator1DFactory() {
  }

  /**
   * Given a name, returns an instance of that integrator.
   * 
   * @param integratorName  the name of the integrator
   * @return the integrator
   * @throws IllegalArgumentException if the integrator name is null or there is no integrator for that name
   */
  public static Integrator1D<Double, Double> getIntegrator(String integratorName) {
    Integrator1D<Double, Double> integrator = STATIC_INSTANCES.get(integratorName);
    if (integrator != null) {
      return integrator;
    }
    throw new IllegalArgumentException("Integrator " + integratorName + " not handled");
  }

  /**
   * Given an integrator, returns its name.
   * 
   * @param integrator  the integrator
   * @return the name of that integrator (null if not found)
   */
  public static String getIntegratorName(Integrator1D<Double, Double> integrator) {
    if (integrator == null) {
      return null;
    }
    return INSTANCE_NAMES.get(integrator.getClass());
  }

}
