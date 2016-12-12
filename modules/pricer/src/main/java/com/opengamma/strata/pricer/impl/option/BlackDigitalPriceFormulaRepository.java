/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.ProbabilityDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The price function to compute the price of digital option in the Black world using call spread approximation.
 */
public class BlackDigitalPriceFormulaRepository {

    /**
     * Small parameter.
     */
    private static final double SMALL = 1.0e-6;

    /** use logger and Normal distribution
     */
    private static final Logger log = LoggerFactory.getLogger(BlackFormulaRepository.class);
    private static final ProbabilityDistribution<Double> NORMAL = new NormalDistribution(0, 1);

    /**

     * @param forward        the spot
     * @param strike       the strike
     * @param timeToExpiry the time to expiry
     * @param lognormalVol the lognormal volatility
     * @param isCall       true if call, false otherwise
     * @param N            the Notional
     * @return the price
     */
    // Computes the forward price of a Domestic Cash Digital option using the call spread approximation.
    public static double price(
            double forward,
            double strike,
            double timeToExpiry,
            double lognormalVol,
            boolean isCall,
            double N)
    {
        double Eps = SMALL;
        double Kplus = strike * (1 + Eps);
        double Kminus = strike * (1 - Eps);
        int sign = isCall ? 1 : -1;
        double M = N / (Kplus - Kminus);

        // check eps
        if(2* strike * Eps < SMALL)
        {
            ArgChecker.isTrue(2* strike * Eps < SMALL, "Epsilon for call spread adjustment is too small with value {}", Eps);
        }
        // Black.price is forward price
        double output = sign * M * (BlackFormulaRepository.price(forward,Kminus,timeToExpiry,lognormalVol,isCall)
                    - BlackFormulaRepository.price(forward,Kplus,timeToExpiry,lognormalVol,isCall));
        //double costCarry = Rd-Rf;
        //Check for the price
        if(output < 0.0)
        {
            ArgChecker.isTrue(output < 0, "Price value is below zero with value {}", output);

        }
        return output;
    }



    // Computes the forward delta Domestic Cash Digital option - call spread approximation.
    public static double delta(
            double forward,
            double strike,
            double timeToExpiry,
            double lognormalVol,
            boolean isCall,
            double N)
    {
        double Eps = SMALL;
        double Kplus = strike * (1+Eps);
        double Kminus = strike * (1-Eps);
        int sign = isCall ? 1 : -1;
        double M = N / (Kplus-Kminus);

        double output = sign * M * (BlackFormulaRepository.delta(forward,Kminus,timeToExpiry,lognormalVol,isCall)
                - BlackFormulaRepository.delta(forward,Kplus,timeToExpiry,lognormalVol,isCall));
        return output;
    }

    // Computes the forward gamma Domestic Cash Digital option - call spread approximation.
    public static double gamma(
            double forward,
            double strike,
            double timeToExpiry,
            double lognormalVol,
            boolean isCall,
            double N)
    {
        double Eps = SMALL;
        double Kplus = strike * (1+Eps);
        double Kminus = strike * (1-Eps);
        int sign = isCall ? 1 : -1;
        double M = N / (Kplus-Kminus);

        double output = sign * M * (BlackFormulaRepository.gamma(forward,Kminus,timeToExpiry,lognormalVol)
                - BlackFormulaRepository.gamma(forward,Kplus,timeToExpiry,lognormalVol));

        return output;
    }

    // Computes the forward vega Domestic Cash Digital option - call spread approximation.
    public static double vega(
            double forward,
            double strike,
            double timeToExpiry,
            double lognormalVol,
            boolean isCall,
            double N)
    {
        double Eps = SMALL;
        double Kplus = strike * (1+Eps);
        double Kminus = strike * (1-Eps);
        int sign = isCall ? 1 : -1;
        double M = N / (Kplus-Kminus);

        double output = sign * M * (BlackFormulaRepository.vega(forward,Kminus,timeToExpiry,lognormalVol)
                - BlackFormulaRepository.vega(forward,Kplus,timeToExpiry,lognormalVol));

        return output;
    }

    // Computes the forward theta Domestic Cash Digital option - call spread approximation.
    public static double theta(
            double forward,
            double strike,
            double timeToExpiry,
            double Rd,
            double lognormalVol,
            boolean isCall,
            double N)
    {
        double Eps = SMALL;
        double Kplus = strike * (1+Eps);
        double Kminus = strike * (1-Eps);
        int sign = isCall ? 1 : -1;
        double M = N / (Kplus-Kminus);

        double output = sign * M * (BlackFormulaRepository.theta(forward,Kminus,timeToExpiry,lognormalVol,isCall,Rd)
                - BlackFormulaRepository.theta(forward,Kplus,timeToExpiry,lognormalVol,isCall,Rd));

        return output;
    }





    // Computes the valuation date price: Domestic Cash Digital option - BS.
    /**
     * @param forward        the spot
     * @param strike       the strike
     * @param timeToExpiry the time to expiry
     * @param Rd           the Domestic rate
     * @param Rf           the foreign rate
     * @param lognormalVol the lognormal volatility
     * @param isCall       true if call, false otherwise
     * @param N            the Notional
     * @return the price
     */
    public static double priceBS(
            double forward,
            double strike,
            double timeToExpiry,
            double lognormalVol,
            double Rd, // in %
            double Rf,
            boolean isCall,
            double N)
    {
        int sign = isCall ? 1 : -1;
        double d1 = 0d;
        double d2 = 0d;
        double sigmaRootT = lognormalVol * Math.sqrt(timeToExpiry);

        ArgChecker.isTrue(forward >= 0d, "negative/NaN forward; have {}", forward);
        ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
        ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
        ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);


        // check sigma.sqrtT
        if (Double.isNaN(sigmaRootT)) {
            log.info("lognormalVol * Math.sqrt(timeToExpiry) ambiguous");
            sigmaRootT = 1d;
        }

        d1 = Math.log(forward / strike) / sigmaRootT + 0.5 * sigmaRootT;
        d2 = d1 - sigmaRootT;

        double nS = NORMAL.getCDF(sign * d2);
        double DCdiscountfactor = Math.exp(-1.0 * Rd * timeToExpiry);
        nS = nS * N * DCdiscountfactor;

        return nS;
    }
}