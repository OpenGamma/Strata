/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option;

import com.google.common.math.DoubleMath;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.ProbabilityDistribution;
import com.opengamma.strata.product.option.SimpleConstantContinuousBarrier;

/**
 * The price function to compute the price of barrier option in the Black world.
 * Reference: E. G. Haug (2007) The complete guide to Option Pricing Formulas. Mc Graw Hill. Section 4.17.1.
 */
public class BlackDigitalPriceFormulaRepository {

    /**
     * Small parameter.
     */
    private static final double SMALL = 1.0e-6;

    /**
     * Computes the price of a Domestic Cash Digital option using the call spread approximation
     *
     * @param spot         the spot
     * @param strike       the strike
     * @param timeToExpiry the time to expiry
     * @param Rd           the Domestic rate
     * @param Rf           the foreign rate
     * @param lognormalVol the lognormal volatility
     * @param isCall       true if call, false otherwise
     * @param N            the Notional
     * @return the price
     */
    public double price(
            double Forward,
            double strike,
            double timeToExpiry,
            double Rd,
            double Rf,
            double lognormalVol,
            boolean isCall,
            double N)
    {
        double Eps = SMALL;
        double Kplus = strike * (1+Eps);
        double Kminus = strike * (1-Eps);
        int sign = isCall ? 1 : -1;
        double M = N / (Kplus-Kminus);
        double output = sign * M * (BlackFormulaRepository.price(Forward, Kminus, timeToExpiry, lognormalVol, isCall)
                    - BlackFormulaRepository.price(Forward, Kplus, timeToExpiry, lognormalVol, isCall));
        /* Check for the price
         */
        if(output < 0.0)
        {
            output = 0.0;
        }
        return output;
    }

    public double delta(
            double Forward,
            double strike,
            double timeToExpiry,
            double Rd,
            double Rf,
            double lognormalVol,
            boolean isCall,
            double N)
    {
        double Eps = SMALL;
        double Kplus = strike * (1+Eps);
        double Kminus = strike * (1-Eps);
        int sign = isCall ? 1 : -1;
        double M = N / (Kplus-Kminus);
        double output = sign * M * (BlackFormulaRepository.delta(Forward, Kminus, timeToExpiry, lognormalVol, isCall)
                - BlackFormulaRepository.delta(Forward, Kplus, timeToExpiry, lognormalVol, isCall));
        /* Check for the price
         */
        if(output < 0.0)
        {
            output = 0.0;
        }
        return output;
    }

    public double gamma(
            double Forward,
            double strike,
            double timeToExpiry,
            double Rd,
            double Rf,
            double lognormalVol,
            boolean isCall,
            double N)
    {
        double Eps = SMALL;
        double Kplus = strike * (1+Eps);
        double Kminus = strike * (1-Eps);
        int sign = isCall ? 1 : -1;
        double M = N / (Kplus-Kminus);
        double output = sign * M * (BlackFormulaRepository.gamma(Forward,Kplus, timeToExpiry, lognormalVol)
                - BlackFormulaRepository.gamma(Forward,Kplus, timeToExpiry, lognormalVol));

        if(output < 0.0)
        {
            output = 0.0;
        }
        return output;
    }
}