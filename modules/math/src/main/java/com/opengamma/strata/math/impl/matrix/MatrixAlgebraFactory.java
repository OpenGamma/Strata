/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.matrix;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * Factory class for various types of matrix algebra calculators.
 */
public final class MatrixAlgebraFactory {

  /** Label for Commons matrix algebra */
  public static final String COMMONS = "Commons";
  /** Label for OpenGamma matrix algebra */
  public static final String OG = "OG";
  /** {@link CommonsMatrixAlgebra} */
  public static final CommonsMatrixAlgebra COMMONS_ALGEBRA = new CommonsMatrixAlgebra();
  /** {@link OGMatrixAlgebra} */
  public static final OGMatrixAlgebra OG_ALGEBRA = new OGMatrixAlgebra();
  private static final Map<String, MatrixAlgebra> STATIC_INSTANCES;
  private static final Map<Class<?>, String> INSTANCE_NAMES;

  static {
    STATIC_INSTANCES = new HashMap<>();
    INSTANCE_NAMES = new HashMap<>();
    STATIC_INSTANCES.put(COMMONS, COMMONS_ALGEBRA);
    INSTANCE_NAMES.put(CommonsMatrixAlgebra.class, COMMONS);
    STATIC_INSTANCES.put(OG, OG_ALGEBRA);
    INSTANCE_NAMES.put(OGMatrixAlgebra.class, OG);
  }

  private MatrixAlgebraFactory() {
  }

  /**
   * Given a name, returns an instance of the matrix algebra calculator.
   * 
   * @param algebraName The name of the matrix algebra calculator
   * @return The matrix algebra calculator
   * @throws IllegalArgumentException If the calculator name is null or there is no calculator for that name
   */
  public static MatrixAlgebra getMatrixAlgebra(String algebraName) {
    if (STATIC_INSTANCES.containsKey(algebraName)) {
      return STATIC_INSTANCES.get(algebraName);
    }
    throw new IllegalArgumentException("Matrix algebra " + algebraName + " not found");
  }

  /**
   * Given a matrix algebra calculator, returns its name.
   * 
   * @param algebra The algebra
   * @return The name of that calculator (null if not found)
   */
  public static String getMatrixAlgebraName(MatrixAlgebra algebra) {
    if (algebra == null) {
      return null;
    }
    return INSTANCE_NAMES.get(algebra.getClass());
  }

}
