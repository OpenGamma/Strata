/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.integration;

import java.util.function.BiFunction;

/**
 * Class for defining the integration of 2-D functions.
 *  
 * @param <T> the type of the function output and result
 * @param <U> the type of the function inputs and integration bounds
 */
public abstract class Integrator2D<T, U>
    implements Integrator<T, U, BiFunction<U, U, T>> {

}
