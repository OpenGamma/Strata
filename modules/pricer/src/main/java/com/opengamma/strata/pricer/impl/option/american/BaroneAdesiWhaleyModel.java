/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option.american;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.MathException;
import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.ProbabilityDistribution;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.impl.option.GenericImpliedVolatiltySolver;

/**
 * The Barone-Adesi and Whaley approximation for the price of an American Option. <b>Note:</b> The Bjerksund and Stensland (2002) approximation ({@link BjerksundStenslandModel}) is
 * more accurate and should be used in place of this.
 */
public class BaroneAdesiWhaleyModel {
  /** A logger */
  private static final Logger s_logger = LoggerFactory.getLogger(BaroneAdesiWhaleyModel.class);
  /** Normal probability distribution */
  private static final ProbabilityDistribution<Double> NORMAL = new NormalDistribution(0, 1);
  
  /**
   * Get the price of an American option by the Barone-Adesi & Whaley approximation. <b>Note:</b> The Bjerksund and Stensland (2002) approximation ({@link BjerksundStenslandModel}) is
   * more accurate and should be used in place of this.
   * @param s0 The spot
   * @param k The strike
   * @param r The risk-free rate
   * @param b The cost-of-carry
   * @param t The time-to-expiry
   * @param sigma The volatility
   * @param isCall true for calls
   * @return The American option price
   */
  public double price(final double s0, final double k, final double r, final double b, final double t, final double sigma, final boolean isCall) {
    // TODO handle k = 0, t = 0 and sigma = 0
    ArgChecker.isTrue(s0 > 0.0, "spot must be greater than zero");
    ArgChecker.isTrue(k > 0.0, "strike must be greater than zero");
    ArgChecker.isTrue(t > 0.0, "t must be greater than zero");
    ArgChecker.isTrue(sigma > 0.0, "sigma must be greater than zero");
    
    if (isCall) {
      final CallSolver solver = new CallSolver(s0, k, r, b, t, sigma);
      return solver.getPrice();
    }
    
    final PutSolver solver = new PutSolver(s0, k, r, b, t, sigma);
    return solver.getPrice();
  }
  
  /**
   * Get the price of an American option by the Barone-Adesi & Whaley approximation and all the first order Greeks
   * <ol>
   * <li>price
   * <li>delta
   * <li>dual delta
   * <li>rho
   * <li>carry rho
   * <li>theta
   * <li>vega
   * </ol>
   * @param s0 The spot
   * @param k The strike
   * @param r The risk-free rate
   * @param b The cost-of-carry
   * @param t The time-to-expiry
   * @param sigma The volatility
   * @param isCall true for calls
   * @return length 7 arrays containing the price, then the sensitivities (Greeks): delta (spot), dual-delta (strike), rho (risk-free rate),
   * b-rho (cost-of-carry), theta (expiry), vega (sigma)
   */
  public double[] getPriceAdjoint(final double s0, final double k, final double r, final double b, final double t, final double sigma, final boolean isCall) {
    
    ArgChecker.isTrue(s0 > 0.0, "spot must be greater than zero");
    ArgChecker.isTrue(k > 0.0, "strike must be greater than zero");
    ArgChecker.isTrue(t > 0.0, "t must be greater than zero");
    // ArgChecker.isTrue(sigma > 0.0, "sigma must be greater than zero"); //sigma<0 is passed when implied volatility computed by using {@link BjerksundStenslandModel}
    
    if (isCall) {
      final CallSolver solver = new CallSolver(s0, k, r, b, t, sigma);
      return solver.getPriceAdjoint();
    }
    final PutSolver solver = new PutSolver(s0, k, r, b, t, sigma);
    return solver.getPriceAdjoint();
  }
  
  /**
   * Get the price, delta and gamma of an American option by the Barone-Adesi & Whaley approximation
   * @param s0 The spot
   * @param k The strike
   * @param r The risk-free rate
   * @param b The cost-of-carry
   * @param t The time-to-expiry
   * @param sigma The volatility
   * @param isCall true for calls
   * @return length 3 array of price, delta and gamma
   */
  public double[] getPriceDeltaGamma(final double s0, final double k, final double r, final double b, final double t, final double sigma, final boolean isCall) {
    ArgChecker.isTrue(s0 > 0.0, "spot must be greater than zero");
    ArgChecker.isTrue(k > 0.0, "strike must be greater than zero");
    ArgChecker.isTrue(t > 0.0, "t must be greater than zero");
    ArgChecker.isTrue(sigma > 0.0, "sigma must be greater than zero");
    
    if (isCall) {
      final CallSolver solver = new CallSolver(s0, k, r, b, t, sigma);
      return solver.getPriceDeltaGamma();
    }
    final PutSolver solver = new PutSolver(s0, k, r, b, t, sigma);
    return solver.getPriceDeltaGamma();
  }
  
  /**
   * Calculates the price and vega of an option and returns them as an array, with elements
   * <ol>
   * <li>price
   * <li>vega
   * </ol>
   * @param s0 The spot
   * @param k The strike
   * @param r The interest rate
   * @param b The cost-of-carry
   * @param t The time to expiry, greater than zero
   * @param sigma The volatility
   * @param isCall is the option a call
   * @return The price and vega of the option
   */
  public double[] getPriceAndVega(final double s0, final double k, final double r, final double b, final double t, final double sigma, final boolean isCall) {
    ArgChecker.isTrue(s0 > 0.0, "spot must be greater than zero");
    ArgChecker.isTrue(k > 0.0, "strike must be greater than zero");
    ArgChecker.isTrue(t > 0.0, "t must be greater than zero");
    ArgChecker.isTrue(sigma > 0.0, "sigma must be greater than zero");
    final double[] temp = getPriceAdjoint(s0, k, r, b, t, sigma, isCall);
    // TODO calculate vega separate from other Greeks
    return new double[] {temp[0], temp[6] };
  }
  
  /**
   * Get a function for the price and vega of an American option by the Barone-Adesi & Whaley approximation in terms of the volatility (sigma).
   * This is primarily used by the GenericImpliedVolatiltySolver to find a (Barone-Adesi & Whaley) implied volatility for a given market price of an American option
   * @param s0 The spot
   * @param k The strike
   * @param r The risk-free rate
   * @param b The cost-of-carry
   * @param t The time-to-expiry
   * @param isCall true for calls
   * @return A function from volatility (sigma) to price and vega
   */
  public Function<Double, double[]> getPriceAndVegaFunction(final double s0, final double k, final double r, final double b, final double t, final boolean isCall) {
    ArgChecker.isTrue(s0 > 0.0, "spot must be greater than zero");
    ArgChecker.isTrue(k > 0.0, "strike must be greater than zero");
    ArgChecker.isTrue(t > 0.0, "t must be greater than zero");
    return new Function<Double, double[]>() {
      @Override
      public double[] apply(final Double sigma) {
        return getPriceAndVega(s0, k, r, b, t, sigma, isCall);
      }
    };
  }
  
  /**
   * Get the implied volatility according to the Barone-Adesi & Whaley approximation for the price of an American option quoted in the market. It is the number that put into the
   * Barone-Adesi & Whaley approximation gives the market price. <b>This is not the same as the Black implied volatility</b> (which is only applicable to European options),
   * although it may be numerically close.
   * <p>
   * If the price indicates that the option should be exercised immediately (price = s0-k for calls and k-s0 for puts), then implied volatility does not exist, and zero is returned (with a warning)
   * @param price The market price of an American option
   * @param s0 The spot
   * @param k The strike
   * @param r The risk-free rate
   * @param b The cost-of-carry
   * @param t The time-to-expiry
   * @param isCall true for calls
   * @return The (Barone-Adesi & Whaley) implied volatility.
   */
  public double impliedVolatility(final double price, final double s0, final double k, final double r, final double b, final double t, final boolean isCall) {
    
    ArgChecker.isTrue((isCall && price >= (s0 - k)) || (!isCall && price >= (k - s0)), "The price is less than the exercised immediately price");
    ArgChecker.isTrue(s0 > 0.0, "spot must be greater than zero");
    ArgChecker.isTrue(k > 0.0, "strike must be greater than zero");
    ArgChecker.isTrue(t > 0.0, "t must be greater than zero");
    
    if ((isCall && Double.compare(price, s0 - k) == 0) || (!isCall && Double.compare(price, k - s0) == 0)) {
      s_logger.warn("The price indicates that this option should be exercised immediately, therefore there is no implied volatility. Zero is returned.");
      return 0.0;
    }
    final Function<Double, double[]> func = getPriceAndVegaFunction(s0, k, r, b, t, isCall);
    GenericImpliedVolatiltySolver solver = new GenericImpliedVolatiltySolver(func);
    return solver.impliedVolatility(price);
  }
  
  /**
   * critical spot price - when the spot is above (below) this for a call (put), it is optimal to excise early
   * @param s0 The spot
   * @param k The strike
   * @param r The risk-free rate
   * @param b The cost-of-carry
   * @param t The time-to-expiry
   * @param sigma The volatility
   * @param isCall true for calls
   * @return The critical spot price
   */
  protected double sCrit(final double s0, final double k, final double r, final double b, final double t, final double sigma, final boolean isCall) {
    if (isCall) {
      final CallSolver solver = new CallSolver(s0, k, r, b, t, sigma);
      return solver.getSStar();
    }
    final PutSolver solver = new PutSolver(s0, k, r, b, t, sigma);
    return solver.getSStar();
  }
  
  /**
   * The critical spot price (when the spot is above (below) this for a call (put), it is optimal to excise early) and its sensitivity to spot (r), strike (k),
   * risk-free rate (r), cost-of-carry (b), expiry (t) and volatility (sigma)
   * @param s0 The spot
   * @param k The strike
   * @param r The risk-free rate
   * @param b The cost-of-carry
   * @param t The time-to-expiry
   * @param sigma The volatility
   * @param isCall true for calls
   * @return The critical spot price and its sensitivities
   */
  protected double[] getsCritAdjoint(final double s0, final double k, final double r, final double b, final double t, final double sigma, final boolean isCall) {
    if (isCall) {
      final CallSolver solver = new CallSolver(s0, k, r, b, t, sigma);
      final double sStar = solver.getSStar();
      return solver.getSStarAdjoint(sStar, solver.getA2Adjoint(sStar, solver.getQ2Adjoint()));
    }
    final PutSolver solver = new PutSolver(s0, k, r, b, t, sigma);
    final double sStar = solver.getSStar();
    return solver.getSStarAdjoint(sStar, solver.getA1Adjoint(sStar, solver.getQ1Adjoint()));
  }
  
  /**
   * Calculates the price for a call
   */
  private static class CallSolver {
    /** The tolerance */
    private static final double TOL = 1e-8;
    /** The maximum number of iterations */
    private static final int MAX_INT = 50;
    /** The spot */
    private final double _s0;
    /** The strike */
    private final double _k;
    /** The interest rate */
    private final double _r;
    /** The cost of carry */
    private final double _b;
    /** The volatility */
    private final double _sigma;
    /** The time to expiry */
    private final double _t;
    
    /** Can the option be treated as European (i.e. is early exercise never optimal) */
    private final boolean _isEuropean;
    /** The square root of time */
    private final double _rootT;
    /** volatility * sqrt(t) */
    private final double _sigmaRootT;
    /** phi */
    private final double _phi;
    /** q2 */
    private final double _q2;
    /** discount factor */
    private final double _df1;
    /** discount factor including the cost-of-carry */
    private final double _df2;
    /** s star */
    private final double _sStar;
    
    public CallSolver(final double s0, final double k, final double r, final double b, final double t, final double sigma) {
      
      _s0 = s0;
      _k = k;
      _r = r;
      _b = b;
      _t = t;
      _sigma = sigma;
      
      _df1 = Math.exp(-r * t);
      _df2 = Math.exp((b - r) * t);
      _rootT = Math.sqrt(t);
      _sigmaRootT = sigma * _rootT;
      _phi = (b + sigma * sigma / 2) * t;
      
      if (b >= r) {
        _isEuropean = true;
        _q2 = 0;
        _sStar = Double.POSITIVE_INFINITY;
      } else {
        _isEuropean = false;
        final double x = 2 * r / sigma / sigma;
        final double y = 2 * b / sigma / sigma - 1;
        final double z = 1 - _df1;
        
        _q2 = (-y + Math.sqrt(y * y + 4 * x / z)) / 2;
        final double sInf = k / (1 - 2 / (-y + Math.sqrt(y * y + 4 * x)));
        final double h2 = -(b * t + 2 * _sigmaRootT) * k / (sInf - k);
        final double initial = k + (sInf - k) * (1 - Math.exp(h2));
        _sStar = getSStar(initial);
      }
    }
    
    public double getSStar() {
      return _sStar;
    }
    
    public double getPrice() {
      
      if (_isEuropean) {
        return getBSPrice(_s0);
      }
      
      if (_s0 >= _sStar) {
        return _s0 - _k;
      }
      final double a2 = getA2(_sStar);
      return getBSPrice(_s0) + a2 * Math.pow(_s0 / _sStar, _q2);
    }
    
    private double getd1(final double s) {
      ArgChecker.isTrue(s > 0, "s is negative");
      return (Math.log(s / _k) + _phi) / _sigmaRootT;
    }
    
    @SuppressWarnings("synthetic-access")
    private double getA2(final double s) {
      final double d1 = getd1(s);
      final double res = s * (1 - _df2 * NORMAL.getCDF(d1)) / _q2;
      return res;
    }
    
    private double getBSPrice(final double s) {
      return _df1 * BlackFormulaRepository.price(s * _df2 / _df1, _k, _t, _sigma, true);
    }
    
    @SuppressWarnings("synthetic-access")
    private double getSStar(final double initalValue) {
      
      double s = initalValue;
      double d1 = getd1(s);
      double cdfd1 = NORMAL.getCDF(d1);
      double rhs = getBSPrice(s) + (1 - _df2 * cdfd1) * s / _q2;
      double h = _df2 * cdfd1 * (1 - 1 / _q2) + (1 - _df2 * NORMAL.getPDF(d1) / _sigmaRootT) / _q2;
      double error = Math.abs(s - _k - rhs) / _k;
      
      int count = 0;
      while (error > TOL && count < MAX_INT) {
        s = (_k + rhs - h * s) / (1 - h);
        d1 = getd1(s);
        cdfd1 = NORMAL.getCDF(d1);
        rhs = getBSPrice(s) + (1 - _df2 * cdfd1) * s / _q2;
        h = _df2 * cdfd1 * (1 - 1 / _q2) + (1 - _df2 * NORMAL.getPDF(d1) / _sigmaRootT) / _q2;
        error = Math.abs(s - _k - rhs) / _k;
        count++;
      }
      if (count == MAX_INT) {
        throw new MathException("max iterations exceeded");
      }
      s = (_k + rhs - h * s) / (1 - h); // since we've calculated h & rhs, might as well update s
      
      return s;
    }
    
    /**
     * The sensitivity of price to the parameters s0, k, r, b, t, sigma
     * @return arrays in order s0 (delta), k (dual-delta), r (rho), b (b-rho), t (theta), sigma (vega)
     */
    public double[] getPriceAdjoint() {
      
      if (_isEuropean) {
        return getBSMPriceAdjoint(_s0);
      }
      
      final double[] res = new double[7];
      
      if (_s0 >= _sStar) {
        res[0] = _s0 - _k;
        res[1] = 1.0;
        res[2] = -1.0;
        return res;
      }
      
      final double[] q2Adjoint = getQ2Adjoint();
      final double[] a2Adjoint = getA2Adjoint(_sStar, q2Adjoint);
      final double[] sStarAdjoint = getSStarAdjoint(_sStar, a2Adjoint);
      final double[] bsmAdjoint = getBSMPriceAdjoint(_s0);
      final double q2 = q2Adjoint[0];
      final double a2 = a2Adjoint[0];
      final double x = Math.pow(_s0 / _sStar, q2);
      final double logRatio = Math.log(_s0 / _sStar);
      
      final double sStarBar = -a2 * q2 * x / _sStar;
      final double q2Bar = a2 * logRatio * x;
      
      res[0] = bsmAdjoint[0] + a2 * x;
      res[1] = bsmAdjoint[1] + a2 * q2 * x / _s0; // delta - no dependence on sStar
      res[2] = bsmAdjoint[2] + x * (a2Adjoint[2] + a2Adjoint[1] * sStarAdjoint[0]) + sStarAdjoint[0] * sStarBar; // dual-delta
      res[3] = bsmAdjoint[3] + x * (a2Adjoint[3] + a2Adjoint[1] * sStarAdjoint[1]) + sStarAdjoint[1] * sStarBar + q2Adjoint[1] * q2Bar; // rho
      res[4] = bsmAdjoint[4] + x * (a2Adjoint[4] + a2Adjoint[1] * sStarAdjoint[2]) + sStarAdjoint[2] * sStarBar + q2Adjoint[2] * q2Bar; // b-rho
      res[5] = bsmAdjoint[5] + x * (a2Adjoint[5] + a2Adjoint[1] * sStarAdjoint[3]) + sStarAdjoint[3] * sStarBar + q2Adjoint[3] * q2Bar; // theta
      res[6] = bsmAdjoint[6] + x * (a2Adjoint[6] + a2Adjoint[1] * sStarAdjoint[4]) + sStarAdjoint[4] * sStarBar + q2Adjoint[4] * q2Bar; // vega
      
      return res;
    }
    
    public double[] getPriceDeltaGamma() {
      
      final double[] bsm = getBSMPriceDeltaGamma(_s0);
      if (_isEuropean) {
        return bsm;
      }
      
      final double[] res = new double[3];
      if (_s0 >= _sStar) {
        res[0] = _s0 - _k;
        res[1] = 1.0;
        res[2] = 0.0;
      } else {
        final double a2 = getA2(_sStar);
        final double temp = a2 * Math.pow(_s0 / _sStar, _q2);
        res[0] = bsm[0] + temp;
        final double w1 = _q2 * temp / _s0;
        res[1] = bsm[1] + w1;
        res[2] = bsm[2] + w1 * (_q2 - 1) / _s0;
      }
      
      return res;
    }
    
    /**
     * The price and first order Greeks for BSM call
     * @param s spot level
     * @return Order is price, delta, dual-delta, rho, b-rho, theta and vega
     */
    @SuppressWarnings("synthetic-access")
    private double[] getBSMPriceAdjoint(final double s) {
      
      final double[] res = new double[7];
      
      final double d1 = getd1(s);
      final double d2 = d1 - _sigmaRootT;
      final double cnd1 = NORMAL.getCDF(d1);
      final double cnd2 = NORMAL.getCDF(d2);
      final double pnd1 = NORMAL.getPDF(d1);
      res[0] = _df2 * s * cnd1 - _df1 * _k * cnd2;
      res[1] = _df2 * cnd1; // delta
      res[2] = -_df1 * cnd2; // dual delta
      res[3] = -_t * (_df2 * s * cnd1 - _df1 * _k * cnd2); // rho (r sensitivity)
      res[4] = s * _t * _df2 * cnd1; // b sensitivity
      res[5] = _df2 * s * (_sigma / 2 / _rootT * pnd1 + (_b - _r) * cnd1) + _r * _k * _df1 * cnd2; // theta
      res[6] = s * _df2 * pnd1 * _rootT; // vega
      return res;
    }
    
    @SuppressWarnings("synthetic-access")
    private double[] getBSMPriceDeltaGamma(final double s) {
      final double[] res = new double[3];
      
      final double d1 = getd1(s);
      final double d2 = d1 - _sigmaRootT;
      final double cnd1 = NORMAL.getCDF(d1);
      final double cnd2 = NORMAL.getCDF(d2);
      final double pnd1 = NORMAL.getPDF(d1);
      res[0] = _df2 * s * cnd1 - _df1 * _k * cnd2;
      res[1] = _df2 * cnd1;
      res[2] = _df2 * pnd1 / s / _sigmaRootT;
      return res;
    }
    
    /**
     * Sensitivity of sStar to k, r, b, t & sigma
     * @param s sStar
     * @param a2Ajoint - get this by calling getA2Adjoint
     * @return array of sensitivities to k, r, b, t & sigma
     */
    public double[] getSStarAdjoint(final double s, final double[] a2Ajoint) {
      
      final double[] bsm = getBSMPriceAdjoint(s);
      final double sBar = bsm[1] + a2Ajoint[1] - 1.0;
      final double kBar = bsm[2] + a2Ajoint[2] + 1.0;
      final double rBar = bsm[3] + a2Ajoint[3];
      final double bBar = bsm[4] + a2Ajoint[4];
      final double tBar = bsm[5] + a2Ajoint[5];
      final double sigmaBar = bsm[6] + a2Ajoint[6];
      
      final double[] res = new double[5];
      res[0] = -kBar / sBar;
      res[1] = -rBar / sBar;
      res[2] = -bBar / sBar;
      res[3] = -tBar / sBar;
      res[4] = -sigmaBar / sBar;
      
      return res;
      
    }
    
    /**
     * The internal parameter A2 and its sensitivities
     * @param s the critical value of s (sCrit or sStar)
     * @param q2Adjoint get by calling getQ2Adjoint
     * @return A2 and sensitivity to s, k, r, b, t, sigma
     */
    @SuppressWarnings("synthetic-access")
    protected double[] getA2Adjoint(final double s, final double[] q2Adjoint) {
      
      final double w2 = Math.log(s / _k);
      final double w3 = _phi + w2;
      final double w4 = w3 / _sigmaRootT; // d1
      final double w5 = NORMAL.getCDF(w4); // N(d1)
      final double w6 = s / q2Adjoint[0];
      final double w7 = 1 - _df2 * w5;
      final double w8 = w6 * w7; // A2
      
      // backwards sweep
      final double w8Bar = 1.0;
      final double w7Bar = w6 * w8Bar;
      final double w6Bar = w7 * w8Bar;
      final double w5Bar = -_df2 * w7Bar;
      final double w4Bar = NORMAL.getPDF(w4) * w5Bar;
      final double w3Bar = 1 / _sigmaRootT * w4Bar;
      final double w2Bar = w3Bar;
      final double w1Bar = w3Bar;
      final double df2Bar = -w5 * w7Bar;
      final double q2Bar = -w6 / q2Adjoint[0] * w6Bar;
      final double sigmaRootTBar = -w4 / _sigmaRootT * w4Bar;
      
      final double[] res = new double[7];
      res[0] = w8; // A2
      res[1] = 1 / q2Adjoint[0] * w6Bar + 1 / s * w2Bar; // 'delta'
      res[2] = -1 / _k * w2Bar; // 'dual-delta'
      res[3] = -_t * _df2 * df2Bar + q2Bar * q2Adjoint[1]; // rho
      res[4] = _t * w1Bar + _t * _df2 * df2Bar + q2Bar * q2Adjoint[2]; // b-rho
      res[5] = (_b + _sigma * _sigma / 2) * w1Bar + 0.5 * _sigma / _rootT * sigmaRootTBar + (_b - _r) * _df2 * df2Bar + q2Bar * q2Adjoint[3]; // theta
      res[6] = _sigma * _t * w1Bar + _rootT * sigmaRootTBar + q2Bar * q2Adjoint[4]; // vega
      return res;
    }
    
    /**
     * The internal parameter q2 and its sensitivities
     * @return array of q2 and sensitivity to r,b,t,sigma
     */
    protected double[] getQ2Adjoint() {
      final double[] res = new double[5];
      
      final double w3 = 1 / _sigma / _sigma;
      final double w4 = 2 * _r * w3; // M
      final double w5 = 2 * _b * w3 - 1; // N-1
      final double w6 = 1 - _df1; // K
      final double w7 = w5 * w5; // (N-1)^2
      final double w8 = 4 * w4 / w6; // 4M/K
      final double w9 = w7 + w8; // (N-1)^2 + 4M/K
      final double w10 = Math.sqrt(w9); // sqrt((N-1)^2 + 4M/K)
      final double w11 = (-w5 + w10) / 2.0; // q2
      
      final double w11Bar = 1.0;
      final double w10Bar = 0.5 * w11Bar;
      final double w9Bar = 0.5 / w10 * w10Bar;
      final double w8Bar = w9Bar;
      final double w7Bar = w9Bar;
      final double w6Bar = -w8 / w6 * w8Bar;
      final double w5Bar = -0.5 * w11Bar + 2 * w5 * w7Bar;
      final double w4Bar = 4 / w6 * w8Bar;
      final double w3Bar = 2 * _b * w5Bar + 2 * _r * w4Bar;
      final double df1Bar = -w6Bar;
      
      res[0] = w11;
      res[1] = 2 * w3 * w4Bar - _t * _df1 * df1Bar;
      res[2] = 2 * w3 * w5Bar;
      res[3] = -_r * _df1 * df1Bar;
      res[4] = -2 * w3 / _sigma * w3Bar;
      
      return res;
    }
  }
  
  /**
   * Calculates the price for a put
   */
  private static class PutSolver {
    /** The tolerance */
    private static final double TOL = 1e-8;
    /** The maximum number of iterations */
    private static final int MAX_INT = 50;
    /** The spot */
    private final double _s0;
    /** The strike */
    private final double _k;
    /** The interest rate */
    private final double _r;
    /** The cost of carry */
    private final double _b;
    /** The volatility */
    private final double _sigma;
    /** The time to expiry */
    private final double _t;
    
    /** The square root of time */
    private final double _rootT;
    /** volatility * sqrt(t) */
    private final double _sigmaRootT;
    /** phi */
    private final double _phi;
    /** q1 */
    private final double _q1;
    /** discount factor */
    private final double _df1;
    /** discount factor including the cost-of-carry */
    private final double _df2;
    /** s star */
    private final double _sStar;
    /** Can the option be treated as European (i.e. is r <= 0) */
    private final boolean _isEuropean;
    
    public PutSolver(final double s0, final double k, final double r, final double b, final double t, final double sigma) {
      
      _s0 = s0;
      _k = k;
      _r = r;
      _b = b;
      _t = t;
      _sigma = sigma;
      
      _df1 = Math.exp(-r * t);
      _df2 = Math.exp((b - r) * t);
      _rootT = Math.sqrt(t);
      _sigmaRootT = sigma * _rootT;
      _phi = (b + sigma * sigma / 2) * t;
      
      if (r <= 0) {
        _isEuropean = true;
        _q1 = 0;
        _sStar = 0.0;
      } else {
        _isEuropean = false;
        final double x = 2 * r / sigma / sigma;
        final double y = 2 * b / sigma / sigma - 1;
        final double z = 1 - _df1;
        _q1 = (-y - Math.sqrt(y * y + 4 * x / z)) / 2;
        
        final double sInf = k / (1 - 2 / (-y - Math.sqrt(y * y + 4 * x)));
        final double h1 = (b * t - 2 * _sigmaRootT) * k / (k - sInf);
        final double inital = sInf + (k - sInf) * Math.exp(h1);
        _sStar = getSStar(inital);
      }
    }
    
    public double getPrice() {
      
      if (_isEuropean) {
        return getBSPrice(_s0);
      }
      
      if (_s0 <= _sStar) {
        return _k - _s0;
      }
      final double a1 = getA1(_sStar);
      return getBSPrice(_s0) + a1 * Math.pow(_s0 / _sStar, _q1);
    }
    
    private double getd1(final double s) {
      ArgChecker.isTrue(s > 0.0, "s is negative");
      return (Math.log(s / _k) + _phi) / _sigmaRootT;
    }
    
    @SuppressWarnings("synthetic-access")
    private double getA1(final double s) {
      final double d1 = getd1(s);
      final double res = -s * (1 - _df2 * NORMAL.getCDF(-d1)) / _q1;
      return res;
    }
    
    public double getBSPrice(final double s) {
      return _df1 * BlackFormulaRepository.price(s * _df2 / _df1, _k, _t, _sigma, false);
    }
    
    public double getSStar() {
      return _sStar;
    }
    
    @SuppressWarnings("synthetic-access")
    private double getSStar(final double initalValue) {
      
      double s = initalValue;
      double d1 = getd1(s);
      double cdfd1 = NORMAL.getCDF(-d1);
      double rhs = getBSPrice(s) - (1 - _df2 * cdfd1) * s / _q1;
      double h = -_df2 * cdfd1 * (1 - 1 / _q1) - (1 + _df2 * NORMAL.getPDF(-d1) / _sigmaRootT) / _q1;
      double error = Math.abs(_k - s - rhs) / _k;
      
      int count = 0;
      while (error > TOL && count < MAX_INT) {
        
        s = (_k - rhs + h * s) / (1 + h);
        d1 = getd1(s);
        cdfd1 = NORMAL.getCDF(-d1);
        rhs = getBSPrice(s) - (1 - _df2 * cdfd1) * s / _q1;
        h = -_df2 * cdfd1 * (1 - 1 / _q1) - (1 + _df2 * NORMAL.getPDF(-d1) / _sigmaRootT) / _q1;
        error = Math.abs(_k - s - rhs) / _k;
        count++;
      }
      if (count == MAX_INT) {
        throw new MathException("max iterations exceeded");
      }
      s = (_k - rhs + h * s) / (1 + h);
      
      return s;
    }
    
    /**
     * The price and its sensitivity to the parameters s0, k, r, b, t, sigma
     * @return arrays in order price, s0 (delta), k (dual-delta), r (rho), b (b-rho), t (theta), sigma (vega)
     */
    public double[] getPriceAdjoint() {
      
      if (_isEuropean) {
        return getBSMPriceAdjoint(_s0);
      }
      
      if (_s0 <= _sStar) {
        
        final double[] res = new double[7];
        res[0] = _k - _s0;
        res[1] = -1.0;
        res[2] = 1.0;
        return res;
      }
      
      final double[] q1Adjoint = getQ1Adjoint();
      final double[] a1Adjoint = getA1Adjoint(_sStar, q1Adjoint);
      final double[] sStarAdjoint = getSStarAdjoint(_sStar, a1Adjoint);
      final double[] bsmAdjoint = getBSMPriceAdjoint(_s0);
      final double q1 = q1Adjoint[0];
      final double a1 = a1Adjoint[0];
      final double x = Math.pow(_s0 / _sStar, q1);
      final double logRatio = Math.log(_s0 / _sStar);
      
      final double[] res = new double[7];
      final double sStarBar = -a1 * q1 * x / _sStar;
      final double q1Bar = a1 * logRatio * x;
      
      res[0] = bsmAdjoint[0] + a1 * x;
      res[1] = bsmAdjoint[1] + a1 * q1 * x / _s0; // delta - no dependence on sStar
      res[2] = bsmAdjoint[2] + x * (a1Adjoint[2] + a1Adjoint[1] * sStarAdjoint[0]) + sStarAdjoint[0] * sStarBar; // dual-delta
      res[3] = bsmAdjoint[3] + x * (a1Adjoint[3] + a1Adjoint[1] * sStarAdjoint[1]) + sStarAdjoint[1] * sStarBar + q1Adjoint[1] * q1Bar; // rho
      res[4] = bsmAdjoint[4] + x * (a1Adjoint[4] + a1Adjoint[1] * sStarAdjoint[2]) + sStarAdjoint[2] * sStarBar + q1Adjoint[2] * q1Bar; // b-rho
      res[5] = bsmAdjoint[5] + x * (a1Adjoint[5] + a1Adjoint[1] * sStarAdjoint[3]) + sStarAdjoint[3] * sStarBar + q1Adjoint[3] * q1Bar; // theta
      res[6] = bsmAdjoint[6] + x * (a1Adjoint[6] + a1Adjoint[1] * sStarAdjoint[4]) + sStarAdjoint[4] * sStarBar + q1Adjoint[4] * q1Bar; // vega
      
      return res;
    }
    
    public double[] getPriceDeltaGamma() {
      
      final double[] bsm = getBSMPriceDeltaGamma(_s0);
      
      final double[] res = new double[3];
      if (_s0 <= _sStar) {
        res[0] = _s0 - _k;
        res[1] = -1.0;
        res[2] = 0.0;
      } else {
        final double a1 = getA1(_sStar);
        final double temp = a1 * Math.pow(_s0 / _sStar, _q1);
        res[0] = bsm[0] + temp;
        final double w1 = _q1 * temp / _s0;
        res[1] = bsm[1] + w1;
        res[2] = bsm[2] + w1 * (_q1 - 1) / _s0;
      }
      
      return res;
    }
    
    /**
     * The price and first order Greeks for BSM put
     * @param s spot level
     * @return Order is price, delta, dual-delta, rho, b-rho, theta and vega
     */
    @SuppressWarnings("synthetic-access")
    private double[] getBSMPriceAdjoint(final double s) {
      
      final double[] res = new double[7];
      
      final double d1 = getd1(s);
      final double d2 = d1 - _sigmaRootT;
      final double cnd1 = NORMAL.getCDF(-d1);
      final double cnd2 = NORMAL.getCDF(-d2);
      final double pnd1 = NORMAL.getPDF(-d1);
      res[0] = _df1 * _k * cnd2 - _df2 * s * cnd1;
      res[1] = -_df2 * cnd1; // delta
      res[2] = _df1 * cnd2; // dual delta
      res[3] = _t * (_df2 * s * cnd1 - _df1 * _k * cnd2); // rho (r sensitivity)
      res[4] = -s * _t * _df2 * cnd1; // b sensitivity
      res[5] = _df2 * s * (_sigma / 2 / _rootT * pnd1 - (_b - _r) * cnd1) - _r * _k * _df1 * cnd2; // theta
      res[6] = s * _df2 * pnd1 * _rootT; // vega
      return res;
    }
    
    @SuppressWarnings("synthetic-access")
    private double[] getBSMPriceDeltaGamma(final double s) {
      final double[] res = new double[3];
      
      final double d1 = getd1(s);
      final double d2 = d1 - _sigmaRootT;
      final double cnd1 = NORMAL.getCDF(-d1);
      final double cnd2 = NORMAL.getCDF(-d2);
      final double pnd1 = NORMAL.getPDF(d1);
      res[0] = _df1 * _k * cnd2 - _df2 * s * cnd1;
      res[1] = -_df2 * cnd1;
      res[2] = _df2 * pnd1 / s / _sigmaRootT;
      return res;
    }
    
    /**
     * Sensitivity of sStar to k, r, b, t & sigma
     * @param s sStar
     * @param a1Ajoint - get this by calling getA1Adjoint
     * @return array of sensitivities to k, r, b, t & sigma
     */
    public double[] getSStarAdjoint(final double s, final double[] a1Ajoint) {
      
      final double[] bsm = getBSMPriceAdjoint(s);
      final double sBar = bsm[1] + a1Ajoint[1] + 1.0;
      final double kBar = bsm[2] + a1Ajoint[2] - 1.0;
      final double rBar = bsm[3] + a1Ajoint[3];
      final double bBar = bsm[4] + a1Ajoint[4];
      final double tBar = bsm[5] + a1Ajoint[5];
      final double sigmaBar = bsm[6] + a1Ajoint[6];
      
      final double[] res = new double[5];
      res[0] = -kBar / sBar;
      res[1] = -rBar / sBar;
      res[2] = -bBar / sBar;
      res[3] = -tBar / sBar;
      res[4] = -sigmaBar / sBar;
      
      return res;
    }
    
    /**
     * The internal parameter A1 and its sensitivities
     * @param s the critical value of s (sCrit or sStar)
     * @param q1Adjoint get by calling getQ2Adjoint
     * @return A1 and sensitivity to s, k, r, b, t, sigma
     */
    @SuppressWarnings("synthetic-access")
    protected double[] getA1Adjoint(final double s, final double[] q1Adjoint) {
      
      final double w2 = Math.log(s / _k);
      final double w3 = _phi + w2;
      final double w4 = w3 / _sigmaRootT; // d1
      final double w5 = NORMAL.getCDF(-w4); // N(-d1)
      final double w6 = s / q1Adjoint[0];
      final double w7 = 1 - _df2 * w5;
      final double w8 = -w6 * w7; // A1
      
      // backwards sweep
      final double w8Bar = 1.0;
      final double w7Bar = -w6 * w8Bar;
      final double w6Bar = -w7 * w8Bar;
      final double w5Bar = -_df2 * w7Bar;
      final double w4Bar = -NORMAL.getPDF(w4) * w5Bar;
      final double w3Bar = 1 / _sigmaRootT * w4Bar;
      final double w2Bar = w3Bar;
      final double w1Bar = w3Bar;
      final double df2Bar = -w5 * w7Bar;
      final double q1Bar = -w6 / q1Adjoint[0] * w6Bar;
      final double sigmaRootTBar = -w4 / _sigmaRootT * w4Bar;
      
      final double[] res = new double[7];
      res[0] = w8; // A1
      res[1] = 1 / q1Adjoint[0] * w6Bar + 1 / s * w2Bar; // 'delta'
      res[2] = -1 / _k * w2Bar; // 'dual-delta'
      res[3] = -_t * _df2 * df2Bar + q1Bar * q1Adjoint[1]; // rho
      res[4] = _t * w1Bar + _t * _df2 * df2Bar + q1Bar * q1Adjoint[2]; // b-rho
      res[5] = (_b + _sigma * _sigma / 2) * w1Bar + 0.5 * _sigma / _rootT * sigmaRootTBar + (_b - _r) * _df2 * df2Bar + q1Bar * q1Adjoint[3]; // theta
      res[6] = _sigma * _t * w1Bar + _rootT * sigmaRootTBar + q1Bar * q1Adjoint[4]; // vega
      return res;
    }
    
    /**
     * The internal parameter q1 and its sensitivities
     * @return array of q1 and sensitivity to r,b,t,sigma
     */
    protected double[] getQ1Adjoint() {
      final double[] res = new double[5];
      
      final double w3 = 1 / _sigma / _sigma;
      final double w4 = 2 * _r * w3; // M
      final double w5 = 2 * _b * w3 - 1; // N-1
      final double w6 = 1 - _df1; // K
      final double w7 = w5 * w5; // (N-1)^2
      final double w8 = 4 * w4 / w6; // 4M/K
      final double w9 = w7 + w8; // (N-1)^2 + 4M/K
      final double w10 = Math.sqrt(w9); // sqrt((N-1)^2 + 4M/K)
      final double w11 = -(w5 + w10) / 2.0; // q1
      
      final double w11Bar = 1.0;
      final double w10Bar = -0.5 * w11Bar;
      final double w9Bar = 0.5 / w10 * w10Bar;
      final double w8Bar = w9Bar;
      final double w7Bar = w9Bar;
      final double w6Bar = -w8 / w6 * w8Bar;
      final double w5Bar = -0.5 * w11Bar + 2 * w5 * w7Bar;
      final double w4Bar = 4 / w6 * w8Bar;
      final double w3Bar = 2 * _b * w5Bar + 2 * _r * w4Bar;
      final double df1Bar = -w6Bar;
      
      res[0] = w11;
      res[1] = 2 * w3 * w4Bar - _t * _df1 * df1Bar;
      res[2] = 2 * w3 * w5Bar;
      res[3] = -_r * _df1 * df1Bar;
      res[4] = -2 * w3 / _sigma * w3Bar;
      
      return res;
    }
    
  }
  
}
