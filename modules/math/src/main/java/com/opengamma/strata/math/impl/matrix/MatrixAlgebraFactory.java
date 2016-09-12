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
  private static final Map<String, MatrixAlgebra> s_staticInstances;
  private static final Map<Class<?>, String> s_instanceNames;

  static {
    s_staticInstances = new HashMap<>();
    s_instanceNames = new HashMap<>();
    s_staticInstances.put(COMMONS, COMMONS_ALGEBRA);
    s_instanceNames.put(CommonsMatrixAlgebra.class, COMMONS);
    s_staticInstances.put(OG, OG_ALGEBRA);
    s_instanceNames.put(OGMatrixAlgebra.class, OG);
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
    if (s_staticInstances.containsKey(algebraName)) {
      return s_staticInstances.get(algebraName);
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
    return s_instanceNames.get(algebra.getClass());
  }

}
