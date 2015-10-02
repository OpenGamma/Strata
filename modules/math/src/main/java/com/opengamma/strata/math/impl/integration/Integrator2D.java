/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.integration;

import com.opengamma.strata.math.impl.function.Function2D;

/**
 * Class for defining the integration of 2-D functions.
 *  
 * @param <T> Type of the function output and result
 * @param <U> Type of the function inputs and integration bounds
 */
public abstract class Integrator2D<T, U> implements Integrator<T, U, Function2D<U, T>> {

}
