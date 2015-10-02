/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.calibration;

import java.util.LinkedHashMap;
import java.util.Map;

import org.joda.beans.PropertyDefinition;

import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;

/**
 * A builder for a bundle of curve building blocks.
 */
public final class CurveBuildingBlockBundleBuilder {

  /**
   * The map builder.
   */
  @PropertyDefinition(validate = "notNull")
  private final LinkedHashMap<CurveName, Pair<CurveBuildingBlock, DoubleMatrix2D>> blocks = new LinkedHashMap<>();

  /**
   * Creates an instance.
   */
  CurveBuildingBlockBundleBuilder() {
  }

  //-------------------------------------------------------------------------
  /**
   * Puts an entry into the bundle.
   * <p>
   * This uses {@link Map#put(Object, Object)} semantics using the name as the key.
   * 
   * @param name  the curve name
   * @return the matching block and matrix
   * @throws IllegalArgumentException if the name is not found
   */
  public Pair<CurveBuildingBlock, DoubleMatrix2D> get(CurveName name) {
    Pair<CurveBuildingBlock, DoubleMatrix2D> pair = blocks.get(name);
    if (pair == null) {
      throw new IllegalArgumentException("Curve name not found: " + name);
    }
    return pair;
  }

  /**
   * Puts an entry into the bundle.
   * <p>
   * This uses {@link Map#put(Object, Object)} semantics using the name as the key.
   * 
   * @param name  the curve name
   * @param block  the block
   * @param jacobianMatrix  the Jacobian matrix
   */
  public void put(CurveName name, CurveBuildingBlock block, DoubleMatrix2D jacobianMatrix) {
    blocks.put(name, Pair.of(block, jacobianMatrix));
  }

  //-------------------------------------------------------------------------
  /**
   * Builds the bundle.
   * 
   * @return the bundle
   */
  public CurveBuildingBlockBundle build() {
    return CurveBuildingBlockBundle.of(blocks);
  }

}
