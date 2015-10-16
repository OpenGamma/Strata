/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding.newton;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * 
 */
public interface NewtonRootFinderDirectionFunction {

  DoubleArray getDirection(DoubleMatrix estimate, DoubleArray y);

}
