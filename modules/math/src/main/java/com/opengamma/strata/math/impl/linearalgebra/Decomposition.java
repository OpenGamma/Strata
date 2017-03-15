/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import java.util.function.Function;

import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Base class for matrix decompositions (e.g. SVD, LU etc).
 * @param <S> The type of the decomposition result
 */

public abstract class Decomposition<S extends DecompositionResult> implements Function<DoubleMatrix, S> {

}
