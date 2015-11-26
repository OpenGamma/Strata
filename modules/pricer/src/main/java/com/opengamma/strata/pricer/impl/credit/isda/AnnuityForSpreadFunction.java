/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import java.util.function.Function;

/**
 * For a given quoted spread (aka 'flat' spread), this function returns the risky annuity
 * (aka risky PV01, RPV01 or risky duration).
 * Exactly how this is done depends on the concrete implementation.   
 */
public abstract class AnnuityForSpreadFunction implements Function<Double, Double> {
}
