/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.function.Function1D;

/**
 * Base class for matrix decompositions (e.g. SVD, LU etc).
 * @param <S> The type of the decomposition result
 */

public abstract class Decomposition<S extends DecompositionResult> extends Function1D<DoubleMatrix, S> {

}
