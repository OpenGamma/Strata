/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.ProbabilityDistribution;

/**
 * The primary repository for Black-Scholes formulas, including the price and greeks.
 * <p>
 * When the formula involves ambiguous quantities, a reference value (rather than NaN) is returned 
 * Note that the formulas are expressed in terms of interest rate (r) and cost of carry (b),
 * then d_1 and d_2 are d_{1,2} = \frac{\ln(S/X) + (b \pm \sigma^2 ) T}{\sigma \sqrt{T}}.
 */
public final class BlackScholesFormulaRepository {

  private static final ProbabilityDistribution<Double> NORMAL = new NormalDistribution(0, 1);
  private static final double SMALL = 1e-13;
  private static final double LARGE = 1e13;

  // restricted constructor
  private BlackScholesFormulaRepository() {
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the spot price.
   * 
   * @param spot  the spot value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @param interestRate  the interest rate
   * @param costOfCarry  the cost-of-carry rate
   * @param isCall  true for call, false for put
   * @return the spot price
   */
  public static double price(
      double spot,
      double strike,
      double timeToExpiry,
      double lognormalVol,
      double interestRate,
      double costOfCarry,
      boolean isCall) {

    ArgChecker.isTrue(spot >= 0d, "negative/NaN spot; have {}", spot);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);
    ArgChecker.isFalse(Double.isNaN(interestRate), "interestRate is NaN");
    ArgChecker.isFalse(Double.isNaN(costOfCarry), "costOfCarry is NaN");

    if (interestRate > LARGE) {
      return 0d;
    }
    if (-interestRate > LARGE) {
      return Double.POSITIVE_INFINITY;
    }
    double discount = Math.abs(interestRate) < SMALL ? 1d : Math.exp(-interestRate * timeToExpiry);

    if (costOfCarry > LARGE) {
      return isCall ? Double.POSITIVE_INFINITY : 0d;
    }
    if (-costOfCarry > LARGE) {
      double res = isCall ? 0d : (discount > SMALL ? strike * discount : 0d);
      return Double.isNaN(res) ? discount : res;
    }
    double factor = Math.exp(costOfCarry * timeToExpiry);

    if (spot > LARGE * strike) {
      double tmp = Math.exp((costOfCarry - interestRate) * timeToExpiry);
      return isCall ? (tmp > SMALL ? spot * tmp : 0d) : 0d;
    }
    if (LARGE * spot < strike) {
      return (isCall || discount < SMALL) ? 0d : strike * discount;
    }
    if (spot > LARGE && strike > LARGE) {
      double tmp = Math.exp((costOfCarry - interestRate) * timeToExpiry);
      return isCall ? (tmp > SMALL ? spot * tmp : 0d) : (discount > SMALL ? strike * discount : 0d);
    }

    double rootT = Math.sqrt(timeToExpiry);
    double sigmaRootT = lognormalVol * rootT;
    if (Double.isNaN(sigmaRootT)) {
      sigmaRootT = 1d; //ref value is returned
    }

    int sign = isCall ? 1 : -1;
    double rescaledSpot = factor * spot;
    if (sigmaRootT < SMALL) {
      double res =
          isCall ? (rescaledSpot > strike ? discount * (rescaledSpot - strike) : 0d) : (rescaledSpot < strike ? discount *
              (strike - rescaledSpot) : 0d);
      return Double.isNaN(res) ? sign * (spot - discount * strike) : res;
    }

    double d1 = 0d;
    double d2 = 0d;
    if (Math.abs(spot - strike) < SMALL || sigmaRootT > LARGE) {
      double coefD1 = (costOfCarry / lognormalVol + 0.5 * lognormalVol);
      double coefD2 = (costOfCarry / lognormalVol - 0.5 * lognormalVol);
      double tmpD1 = coefD1 * rootT;
      double tmpD2 = coefD2 * rootT;
      d1 = Double.isNaN(tmpD1) ? 0d : tmpD1;
      d2 = Double.isNaN(tmpD2) ? 0d : tmpD2;
    } else {
      double tmp = costOfCarry * rootT / lognormalVol;
      double sig = (costOfCarry >= 0d) ? 1d : -1d;
      double scnd =
          Double.isNaN(tmp) ? ((lognormalVol < LARGE && lognormalVol > SMALL) ? sig / lognormalVol : sig * rootT) : tmp;
      d1 = Math.log(spot / strike) / sigmaRootT + scnd + 0.5 * sigmaRootT;
      d2 = d1 - sigmaRootT;
    }
    double res = sign * discount * (rescaledSpot * NORMAL.getCDF(sign * d1) - strike * NORMAL.getCDF(sign * d2));
    return Double.isNaN(res) ? 0d : Math.max(res, 0d);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the spot delta.
   * 
   * @param spot  the spot value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @param interestRate  the interest rate
   * @param costOfCarry  the cost-of-carry rate
   * @param isCall  true for call, false for put
   * @return the spot delta
   */
  public static double delta(
      double spot,
      double strike,
      double timeToExpiry,
      double lognormalVol,
      double interestRate,
      double costOfCarry,
      boolean isCall) {

    ArgChecker.isTrue(spot >= 0d, "negative/NaN spot; have {}", spot);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);
    ArgChecker.isFalse(Double.isNaN(interestRate), "interestRate is NaN");
    ArgChecker.isFalse(Double.isNaN(costOfCarry), "costOfCarry is NaN");

    double coef = 0d;
    if ((interestRate > LARGE && costOfCarry > LARGE) || (-interestRate > LARGE && -costOfCarry > LARGE) ||
        Math.abs(costOfCarry - interestRate) < SMALL) {
      coef = 1d; //ref value is returned
    } else {
      double rate = costOfCarry - interestRate;
      if (rate > LARGE) {
        return isCall ? Double.POSITIVE_INFINITY : (costOfCarry > LARGE ? 0d : Double.NEGATIVE_INFINITY);
      }
      if (-rate > LARGE) {
        return 0d;
      }
      coef = Math.exp(rate * timeToExpiry);
    }

    if (spot > LARGE * strike) {
      return isCall ? coef : 0d;
    }
    if (spot < SMALL * strike) {
      return isCall ? 0d : -coef;
    }

    int sign = isCall ? 1 : -1;
    double rootT = Math.sqrt(timeToExpiry);
    double sigmaRootT = lognormalVol * rootT;
    if (Double.isNaN(sigmaRootT)) {
      sigmaRootT = 1d; //ref value is returned
    }

    double factor = Math.exp(costOfCarry * timeToExpiry);
    if (Double.isNaN(factor)) {
      factor = 1d; //ref value is returned
    }
    double rescaledSpot = spot * factor;

    double d1 = 0d;
    if (Math.abs(spot - strike) < SMALL || sigmaRootT > LARGE || (spot > LARGE && strike > LARGE)) {
      double coefD1 = (costOfCarry / lognormalVol + 0.5 * lognormalVol);
      double tmp = coefD1 * rootT;
      d1 = Double.isNaN(tmp) ? 0d : tmp;
    } else {
      if (sigmaRootT < SMALL) {
        return isCall ? (rescaledSpot > strike ? coef : 0d) : (rescaledSpot < strike ? -coef : 0d);
      }
      double tmp = costOfCarry * rootT / lognormalVol;
      double sig = (costOfCarry >= 0d) ? 1d : -1d;
      double scnd =
          Double.isNaN(tmp) ? ((lognormalVol < LARGE && lognormalVol > SMALL) ? sig / lognormalVol : sig * rootT) : tmp;
      d1 = Math.log(spot / strike) / sigmaRootT + scnd + 0.5 * sigmaRootT;
    }
    double norm = NORMAL.getCDF(sign * d1);

    return norm < SMALL ? 0d : sign * coef * norm;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the strike for the delta.
   * <p>
   * Note that the parameter range is more restricted for this method because the
   * strike is undetermined for infinite/zero valued parameters.
   * 
   * @param spot  the spot value of the underlying
   * @param spotDelta The spot delta
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @param interestRate  the interest rate
   * @param costOfCarry  the cost-of-carry rate
   * @param isCall  true for call, false for put
   * @return the strike
   */
  public static double strikeForDelta(
      double spot,
      double spotDelta,
      double timeToExpiry,
      double lognormalVol,
      double interestRate,
      double costOfCarry,
      boolean isCall) {

    ArgChecker.isTrue(spot > 0d, "non-positive/NaN spot; have {}", spot);
    ArgChecker.isTrue(timeToExpiry > 0d, "non-positive/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol > 0d, "non-positive/NaN lognormalVol; have {}", lognormalVol);
    ArgChecker.isFalse(Double.isNaN(interestRate), "interestRate is NaN");
    ArgChecker.isFalse(Double.isNaN(costOfCarry), "costOfCarry is NaN");

    ArgChecker.isFalse(Double.isInfinite(spot), "spot is infinite");
    ArgChecker.isFalse(Double.isInfinite(spotDelta), "spotDelta is infinite");
    ArgChecker.isFalse(Double.isInfinite(timeToExpiry), "timeToExpiry is infinite");
    ArgChecker.isFalse(Double.isInfinite(lognormalVol), "lognormalVol is infinite");
    ArgChecker.isFalse(Double.isInfinite(interestRate), "interestRate is infinite");
    ArgChecker.isFalse(Double.isInfinite(costOfCarry), "costOfCarry is infinite");

    double rescaledDelta = spotDelta * Math.exp((-costOfCarry + interestRate) * timeToExpiry);
    ArgChecker.isTrue((isCall && rescaledDelta > 0d && rescaledDelta < 1.) || (!isCall && spotDelta < 0d && rescaledDelta > -1.),
        "delta/Math.exp((costOfCarry - interestRate) * timeToExpiry) out of range, ", rescaledDelta);

    double sigmaRootT = lognormalVol * Math.sqrt(timeToExpiry);
    double rescaledSpot = spot * Math.exp(costOfCarry * timeToExpiry);

    int sign = isCall ? 1 : -1;
    double d1 = sign * NORMAL.getInverseCDF(sign * rescaledDelta);
    return rescaledSpot * Math.exp(-d1 * sigmaRootT + 0.5 * sigmaRootT * sigmaRootT);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the dual delta.
   * <p>
   * This is the first derivative of option price with respect to strike.
   * 
   * @param spot  the spot value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @param interestRate  the interest rate
   * @param costOfCarry  the cost-of-carry rate
   * @param isCall  true for call, false for put
   * @return the dual delta
   */
  public static double dualDelta(
      double spot,
      double strike,
      double timeToExpiry,
      double lognormalVol,
      double interestRate,
      double costOfCarry,
      boolean isCall) {

    ArgChecker.isTrue(spot >= 0d, "negative/NaN spot; have {}", spot);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);
    ArgChecker.isFalse(Double.isNaN(interestRate), "interestRate is NaN");
    ArgChecker.isFalse(Double.isNaN(costOfCarry), "costOfCarry is NaN");

    double discount = 0d;
    if (-interestRate > LARGE) {
      return isCall ? Double.NEGATIVE_INFINITY : (costOfCarry > LARGE ? 0d : Double.POSITIVE_INFINITY);
    }
    if (interestRate > LARGE) {
      return 0d;
    }
    discount = (Math.abs(interestRate) < SMALL && timeToExpiry > LARGE) ? 1d : Math.exp(-interestRate * timeToExpiry);

    if (spot > LARGE * strike) {
      return isCall ? -discount : 0d;
    }
    if (spot < SMALL * strike) {
      return isCall ? 0d : discount;
    }

    int sign = isCall ? 1 : -1;
    double rootT = Math.sqrt(timeToExpiry);
    double sigmaRootT = lognormalVol * rootT;
    if (Double.isNaN(sigmaRootT)) {
      sigmaRootT = 1d; //ref value is returned
    }

    double factor = Math.exp(costOfCarry * timeToExpiry);
    if (Double.isNaN(factor)) {
      factor = 1d; //ref value is returned
    }
    double rescaledSpot = spot * factor;

    double d2 = 0d;
    if (Math.abs(spot - strike) < SMALL || sigmaRootT > LARGE || (spot > LARGE && strike > LARGE)) {
      double coefD2 = (costOfCarry / lognormalVol - 0.5 * lognormalVol);
      double tmp = coefD2 * rootT;
      d2 = Double.isNaN(tmp) ? 0d : tmp;
    } else {
      if (sigmaRootT < SMALL) {
        return isCall ? (rescaledSpot > strike ? -discount : 0d) : (rescaledSpot < strike ? discount : 0d);
      }
      double tmp = costOfCarry * rootT / lognormalVol;
      double sig = (costOfCarry >= 0d) ? 1d : -1d;
      double scnd =
          Double.isNaN(tmp) ? ((lognormalVol < LARGE && lognormalVol > SMALL) ? sig / lognormalVol : sig * rootT) : tmp;
      d2 = Math.log(spot / strike) / sigmaRootT + scnd - 0.5 * sigmaRootT;
    }
    double norm = NORMAL.getCDF(sign * d2);

    return norm < SMALL ? 0d : -sign * discount * norm;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the spot gamma.
   * <p>
   * This is the second order sensitivity of the spot option value to the spot.
   * <p>
   * $\frac{\partial^2 FV}{\partial^2 f}$.
   * 
   * @param spot  the spot value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @param interestRate  the interest rate
   * @param costOfCarry  the cost-of-carry rate
   * @return the spot gamma
   */
  public static double gamma(
      double spot,
      double strike,
      double timeToExpiry,
      double lognormalVol,
      double interestRate,
      double costOfCarry) {

    ArgChecker.isTrue(spot >= 0d, "negative/NaN spot; have {}", spot);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);
    ArgChecker.isFalse(Double.isNaN(interestRate), "interestRate is NaN");
    ArgChecker.isFalse(Double.isNaN(costOfCarry), "costOfCarry is NaN");

    double coef = 0d;
    if ((interestRate > LARGE && costOfCarry > LARGE) || (-interestRate > LARGE && -costOfCarry > LARGE) ||
        Math.abs(costOfCarry - interestRate) < SMALL) {
      coef = 1d; //ref value is returned
    } else {
      double rate = costOfCarry - interestRate;
      if (rate > LARGE) {
        return costOfCarry > LARGE ? 0d : Double.POSITIVE_INFINITY;
      }
      if (-rate > LARGE) {
        return 0d;
      }
      coef = Math.exp(rate * timeToExpiry);
    }

    double rootT = Math.sqrt(timeToExpiry);
    double sigmaRootT = lognormalVol * rootT;
    if (Double.isNaN(sigmaRootT)) {
      sigmaRootT = 1d; //ref value is returned
    }
    if (spot > LARGE * strike || spot < SMALL * strike || sigmaRootT > LARGE) {
      return 0d;
    }

    double factor = Math.exp(costOfCarry * timeToExpiry);
    if (Double.isNaN(factor)) {
      factor = 1d; //ref value is returned
    }

    double d1 = 0d;
    if (Math.abs(spot - strike) < SMALL || (spot > LARGE && strike > LARGE)) {
      double coefD1 = (Math.abs(costOfCarry) < SMALL && lognormalVol < SMALL) ?
          Math.signum(costOfCarry) + 0.5 * lognormalVol :
          (costOfCarry / lognormalVol + 0.5 * lognormalVol);
      double tmp = coefD1 * rootT;
      d1 = Double.isNaN(tmp) ? 0d : tmp;
    } else {
      if (sigmaRootT < SMALL) {
        double scnd = (Math.abs(costOfCarry) > LARGE && rootT < SMALL) ? Math.signum(costOfCarry) : costOfCarry * rootT;
        double tmp = (Math.log(spot / strike) / rootT + scnd) / lognormalVol;
        d1 = Double.isNaN(tmp) ? 0d : tmp;
      } else {
        double tmp = costOfCarry * rootT / lognormalVol;
        double sig = (costOfCarry >= 0d) ? 1d : -1d;
        double scnd =
            Double.isNaN(tmp) ? ((lognormalVol < LARGE && lognormalVol > SMALL) ? sig / lognormalVol : sig * rootT) : tmp;
        d1 = Math.log(spot / strike) / sigmaRootT + scnd + 0.5 * sigmaRootT;
      }
    }
    double norm = NORMAL.getPDF(d1);

    double res = norm < SMALL ? 0d : coef * norm / spot / sigmaRootT;
    return Double.isNaN(res) ? Double.POSITIVE_INFINITY : res;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the dual gamma.
   * 
   * @param spot  the spot value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @param interestRate  the interest rate
   * @param costOfCarry  the cost-of-carry rate
   * @return the dual gamma
   */
  public static double dualGamma(
      double spot,
      double strike,
      double timeToExpiry,
      double lognormalVol,
      double interestRate,
      double costOfCarry) {

    ArgChecker.isTrue(spot >= 0d, "negative/NaN spot; have {}", spot);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);
    ArgChecker.isFalse(Double.isNaN(interestRate), "interestRate is NaN");
    ArgChecker.isFalse(Double.isNaN(costOfCarry), "costOfCarry is NaN");

    if (-interestRate > LARGE) {
      return costOfCarry > LARGE ? 0d : Double.POSITIVE_INFINITY;
    }
    if (interestRate > LARGE) {
      return 0d;
    }
    double discount = (Math.abs(interestRate) < SMALL && timeToExpiry > LARGE) ? 1d : Math.exp(-interestRate * timeToExpiry);

    double rootT = Math.sqrt(timeToExpiry);
    double sigmaRootT = lognormalVol * rootT;
    if (Double.isNaN(sigmaRootT)) {
      sigmaRootT = 1d; //ref value is returned
    }
    if (spot > LARGE * strike || spot < SMALL * strike || sigmaRootT > LARGE) {
      return 0d;
    }

    double factor = Math.exp(costOfCarry * timeToExpiry);
    if (Double.isNaN(factor)) {
      factor = 1d;
    }

    double d2 = 0d;
    if (Math.abs(spot - strike) < SMALL || (spot > LARGE && strike > LARGE)) {
      double coefD1 = (Math.abs(costOfCarry) < SMALL && lognormalVol < SMALL) ?
          Math.signum(costOfCarry) - 0.5 * lognormalVol :
          (costOfCarry / lognormalVol - 0.5 * lognormalVol);
      double tmp = coefD1 * rootT;
      d2 = Double.isNaN(tmp) ? 0d : tmp;
    } else {
      if (sigmaRootT < SMALL) {
        double scnd = (Math.abs(costOfCarry) > LARGE && rootT < SMALL) ? Math.signum(costOfCarry) : costOfCarry * rootT;
        double tmp = (Math.log(spot / strike) / rootT + scnd) / lognormalVol;
        d2 = Double.isNaN(tmp) ? 0d : tmp;
      } else {
        double tmp = costOfCarry * rootT / lognormalVol;
        double sig = (costOfCarry >= 0d) ? 1d : -1d;
        double scnd =
            Double.isNaN(tmp) ? ((lognormalVol < LARGE && lognormalVol > SMALL) ? sig / lognormalVol : sig * rootT) : tmp;
        d2 = Math.log(spot / strike) / sigmaRootT + scnd - 0.5 * sigmaRootT;
      }
    }
    double norm = NORMAL.getPDF(d2);

    double res = norm < SMALL ? 0d : discount * norm / strike / sigmaRootT;
    return Double.isNaN(res) ? Double.POSITIVE_INFINITY : res;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the cross gamma.
   * <p>
   * This is the sensitivity of the delta to the strike.
   * <p>
   * $\frac{\partial^2 V}{\partial f \partial K}$.
   * 
   * @param spot  the spot value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @param interestRate  the interest rate
   * @param costOfCarry  the cost-of-carry rate
   * @return the cross gamma
   */
  public static double crossGamma(double spot, double strike, double timeToExpiry, double lognormalVol,
      double interestRate, double costOfCarry) {
    ArgChecker.isTrue(spot >= 0d, "negative/NaN spot; have {}", spot);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);
    ArgChecker.isFalse(Double.isNaN(interestRate), "interestRate is NaN");
    ArgChecker.isFalse(Double.isNaN(costOfCarry), "costOfCarry is NaN");

    if (-interestRate > LARGE) {
      return costOfCarry > LARGE ? 0d : Double.NEGATIVE_INFINITY;
    }
    if (interestRate > LARGE) {
      return 0d;
    }
    double discount = (Math.abs(interestRate) < SMALL && timeToExpiry > LARGE) ? 1d : Math.exp(-interestRate * timeToExpiry);

    double rootT = Math.sqrt(timeToExpiry);
    double sigmaRootT = lognormalVol * rootT;
    if (Double.isNaN(sigmaRootT)) {
      sigmaRootT = 1d; //ref value is returned
    }
    if (spot > LARGE * strike || spot < SMALL * strike || sigmaRootT > LARGE) {
      return 0d;
    }

    double factor = Math.exp(costOfCarry * timeToExpiry);
    if (Double.isNaN(factor)) {
      factor = 1d; //ref value is returned
    }

    double d2 = 0d;
    if (Math.abs(spot - strike) < SMALL || (spot > LARGE && strike > LARGE)) {
      double coefD1 = (Math.abs(costOfCarry) < SMALL && lognormalVol < SMALL) ?
          Math.signum(costOfCarry) - 0.5 * lognormalVol :
          (costOfCarry / lognormalVol - 0.5 * lognormalVol);
      double tmp = coefD1 * rootT;
      d2 = Double.isNaN(tmp) ? 0d : tmp;
    } else {
      if (sigmaRootT < SMALL) {
        double scnd = (Math.abs(costOfCarry) > LARGE && rootT < SMALL) ? Math.signum(costOfCarry) : costOfCarry * rootT;
        double tmp = (Math.log(spot / strike) / rootT + scnd) / lognormalVol;
        d2 = Double.isNaN(tmp) ? 0d : tmp;
      } else {
        double tmp = costOfCarry * rootT / lognormalVol;
        double sig = (costOfCarry >= 0d) ? 1d : -1d;
        double scnd =
            Double.isNaN(tmp) ? ((lognormalVol < LARGE && lognormalVol > SMALL) ? sig / lognormalVol : sig * rootT) : tmp;
        d2 = Math.log(spot / strike) / sigmaRootT + scnd - 0.5 * sigmaRootT;
      }
    }
    double norm = NORMAL.getPDF(d2);

    double res = norm < SMALL ? 0d : -discount * norm / spot / sigmaRootT;
    return Double.isNaN(res) ? Double.NEGATIVE_INFINITY : res;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the theta.
   * <p>
   * This is the sensitivity of the present value to a change in time to maturity.
   * <p>
   * $\-frac{\partial V}{\partial T}$.
   *
   * @param spot  the spot value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @param interestRate  the interest rate
   * @param costOfCarry  the cost-of-carry rate
   * @param isCall  true for call, false for put
   * @return theta
   */
  public static double theta(
      double spot,
      double strike,
      double timeToExpiry,
      double lognormalVol,
      double interestRate,
      double costOfCarry,
      boolean isCall) {

    ArgChecker.isTrue(spot >= 0d, "negative/NaN spot; have {}", spot);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);
    ArgChecker.isFalse(Double.isNaN(interestRate), "interestRate is NaN");
    ArgChecker.isFalse(Double.isNaN(costOfCarry), "costOfCarry is NaN");

    if (Math.abs(interestRate) > LARGE) {
      return 0d;
    }
    double discount = (Math.abs(interestRate) < SMALL && timeToExpiry > LARGE) ? 1d : Math.exp(-interestRate * timeToExpiry);

    if (costOfCarry > LARGE) {
      return isCall ? Double.NEGATIVE_INFINITY : 0d;
    }
    if (-costOfCarry > LARGE) {
      double res = isCall ? 0d : (discount > SMALL ? strike * discount * interestRate : 0d);
      return Double.isNaN(res) ? discount : res;
    }

    if (spot > LARGE * strike) {
      double tmp = Math.exp((costOfCarry - interestRate) * timeToExpiry);
      double res = isCall ? (tmp > SMALL ? -(costOfCarry - interestRate) * spot * tmp : 0d) : 0d;
      return Double.isNaN(res) ? tmp : res;
    }
    if (LARGE * spot < strike) {
      double res = isCall ? 0d : (discount > SMALL ? strike * discount * interestRate : 0d);
      return Double.isNaN(res) ? discount : res;
    }
    if (spot > LARGE && strike > LARGE) {
      return Double.POSITIVE_INFINITY;
    }

    double rootT = Math.sqrt(timeToExpiry);
    double sigmaRootT = lognormalVol * rootT;
    if (Double.isNaN(sigmaRootT)) {
      sigmaRootT = 1d; //ref value is returned
    }

    int sign = isCall ? 1 : -1;
    double d1 = 0d;
    double d2 = 0d;
    if (Math.abs(spot - strike) < SMALL || sigmaRootT > LARGE) {
      double coefD1 = (Math.abs(costOfCarry) < SMALL && lognormalVol < SMALL) ?
          Math.signum(costOfCarry) + 0.5 * lognormalVol :
          (costOfCarry / lognormalVol + 0.5 * lognormalVol);
      double tmpD1 = Math.abs(coefD1) < SMALL ? 0d : coefD1 * rootT;
      d1 = Double.isNaN(tmpD1) ? Math.signum(coefD1) : tmpD1;
      double coefD2 = (Math.abs(costOfCarry) < SMALL && lognormalVol < SMALL) ?
          Math.signum(costOfCarry) - 0.5 * lognormalVol :
          (costOfCarry / lognormalVol - 0.5 * lognormalVol);
      double tmpD2 = Math.abs(coefD2) < SMALL ? 0d : coefD2 * rootT;
      d2 = Double.isNaN(tmpD2) ? Math.signum(coefD2) : tmpD2;
    } else {
      if (sigmaRootT < SMALL) {
        d1 = (Math.log(spot / strike) / rootT + costOfCarry * rootT) / lognormalVol;
        d2 = d1;
      } else {
        double tmp = (Math.abs(costOfCarry) < SMALL && lognormalVol < SMALL) ?
            rootT :
            ((Math.abs(costOfCarry) < SMALL && rootT > LARGE) ? 1d / lognormalVol : costOfCarry / lognormalVol * rootT);
        d1 = Math.log(spot / strike) / sigmaRootT + tmp + 0.5 * sigmaRootT;
        d2 = d1 - sigmaRootT;
      }
    }
    double norm = NORMAL.getPDF(d1);
    double rescaledSpot = Math.exp((costOfCarry - interestRate) * timeToExpiry) * spot;
    double rescaledStrike = discount * strike;
    double normForSpot = NORMAL.getCDF(sign * d1);
    double normForStrike = NORMAL.getCDF(sign * d2);
    double spotTerm = normForSpot < SMALL ?
        0d :
        (Double.isNaN(rescaledSpot) ? -sign * Math.signum((costOfCarry - interestRate)) * rescaledSpot : -sign *
            ((costOfCarry - interestRate) * rescaledSpot * normForSpot));
    double strikeTerm =
        normForStrike < SMALL ? 0d : (Double.isNaN(rescaledSpot) ? sign * (-Math.signum(interestRate) * discount) : sign *
            (-interestRate * rescaledStrike * normForStrike));

    double coef = rescaledSpot * lognormalVol / rootT;
    if (Double.isNaN(coef)) {
      coef = 1d; //ref value is returned
    }
    double dlTerm = norm < SMALL ? 0d : -0.5 * norm * coef;

    double res = dlTerm + spotTerm + strikeTerm;
    return Double.isNaN(res) ? 0d : res;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the charm.
   * <p>
   * This is the minus of second order derivative of option value, once spot and once time to maturity.
   * 
   * @param spot  the spot value of the underlying
   * @param strike  the strike 
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @param interestRate  The interest rate
   * @param costOfCarry  The cost of carry
   * @param isCall  true for call, false for put
   * @return the charm
   */
  public static double charm(
      double spot,
      double strike,
      double timeToExpiry,
      double lognormalVol,
      double interestRate,
      double costOfCarry,
      boolean isCall) {

    ArgChecker.isTrue(spot >= 0d, "negative/NaN spot; have {}", spot);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);
    ArgChecker.isFalse(Double.isNaN(interestRate), "interestRate is NaN");
    ArgChecker.isFalse(Double.isNaN(costOfCarry), "costOfCarry is NaN");

    double rootT = Math.sqrt(timeToExpiry);
    double sigmaRootT = lognormalVol * rootT;
    if (Double.isNaN(sigmaRootT)) {
      sigmaRootT = 1d; //ref value is returned
    }

    double coeff = Math.exp((costOfCarry - interestRate) * timeToExpiry);
    if (coeff < SMALL) {
      return 0d;
    }
    if (Double.isNaN(coeff)) {
      coeff = 1d; //ref value is returned
    }

    int sign = isCall ? 1 : -1;
    double d1 = 0d;
    double d2 = 0d;
    if (Math.abs(spot - strike) < SMALL || (spot > LARGE && strike > LARGE) || sigmaRootT > LARGE) {
      double coefD1 = Double.isNaN(Math.abs(costOfCarry) / lognormalVol) ?
          Math.signum(costOfCarry) + 0.5 * lognormalVol :
          (costOfCarry / lognormalVol + 0.5 * lognormalVol);
      double tmpD1 = Math.abs(coefD1) < SMALL ? 0d : coefD1 * rootT;
      d1 = Double.isNaN(tmpD1) ? Math.signum(coefD1) : tmpD1;
      double coefD2 = Double.isNaN(Math.abs(costOfCarry) / lognormalVol) ?
          Math.signum(costOfCarry) - 0.5 * lognormalVol :
          (costOfCarry / lognormalVol - 0.5 * lognormalVol);
      double tmpD2 = Math.abs(coefD2) < SMALL ? 0d : coefD2 * rootT;
      d2 = Double.isNaN(tmpD2) ? Math.signum(coefD2) : tmpD2;
    } else {
      if (sigmaRootT < SMALL) {
        double scnd = (Math.abs(costOfCarry) > LARGE && rootT < SMALL) ? Math.signum(costOfCarry) : costOfCarry * rootT;
        double tmp = (Math.log(spot / strike) / rootT + scnd) / lognormalVol;
        d1 = Double.isNaN(tmp) ? 0d : tmp;
        d2 = d1;
      } else {
        double tmp = costOfCarry * rootT / lognormalVol;
        double sig = (costOfCarry >= 0d) ? 1d : -1d;
        double scnd =
            Double.isNaN(tmp) ? ((lognormalVol < LARGE && lognormalVol > SMALL) ? sig / lognormalVol : sig * rootT) : tmp;
        double d1Tmp = Math.log(spot / strike) / sigmaRootT + scnd + 0.5 * sigmaRootT;
        double d2Tmp = Math.log(spot / strike) / sigmaRootT + scnd - 0.5 * sigmaRootT;
        d1 = Double.isNaN(d1Tmp) ? 0d : d1Tmp;
        d2 = Double.isNaN(d2Tmp) ? 0d : d2Tmp;
      }
    }
    double cocMod = costOfCarry / sigmaRootT;
    if (Double.isNaN(cocMod)) {
      cocMod = 1d; //ref value is returned
    }

    double tmp = d2 / timeToExpiry;
    tmp = Double.isNaN(tmp) ? (d2 >= 0d ? 1d : -1.) : tmp;
    double coefPdf = cocMod - 0.5 * tmp;

    double normPdf = NORMAL.getPDF(d1);
    double normCdf = NORMAL.getCDF(sign * d1);
    double first = normPdf < SMALL ? 0d : (Double.isNaN(coefPdf) ? 0d : normPdf * coefPdf);
    double second = normCdf < SMALL ? 0d : (costOfCarry - interestRate) * normCdf;
    double res = -coeff * (first + sign * second);

    return Double.isNaN(res) ? 0d : res;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the dual charm.
   * <p>
   * This is the minus of second order derivative of option value, once strike and once time to maturity.
   * 
   * @param spot  the spot value of the underlying
   * @param strike  the strike 
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @param interestRate  the interest rate
   * @param costOfCarry  the cost of carry
   * @param isCall  true for call, false for put
   * @return the dual charm
   */
  public static double dualCharm(
      double spot,
      double strike,
      double timeToExpiry,
      double lognormalVol,
      double interestRate,
      double costOfCarry,
      boolean isCall) {

    ArgChecker.isTrue(spot >= 0d, "negative/NaN spot; have {}", spot);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);
    ArgChecker.isFalse(Double.isNaN(interestRate), "interestRate is NaN");
    ArgChecker.isFalse(Double.isNaN(costOfCarry), "costOfCarry is NaN");

    double rootT = Math.sqrt(timeToExpiry);
    double sigmaRootT = lognormalVol * rootT;
    if (Double.isNaN(sigmaRootT)) {
      sigmaRootT = 1d; //ref value is returned
    }

    double discount = Math.exp(-interestRate * timeToExpiry);
    if (discount < SMALL) {
      return 0d;
    }
    if (Double.isNaN(discount)) {
      discount = 1d; //ref value is returned
    }

    int sign = isCall ? 1 : -1;
    double d1 = 0d;
    double d2 = 0d;
    if (Math.abs(spot - strike) < SMALL || (spot > LARGE && strike > LARGE) || sigmaRootT > LARGE) {
      double coefD1 = Double.isNaN(Math.abs(costOfCarry) / lognormalVol) ?
          Math.signum(costOfCarry) + 0.5 * lognormalVol :
          (costOfCarry / lognormalVol + 0.5 * lognormalVol);
      double tmpD1 = Math.abs(coefD1) < SMALL ? 0d : coefD1 * rootT;
      d1 = Double.isNaN(tmpD1) ? Math.signum(coefD1) : tmpD1;
      double coefD2 = Double.isNaN(Math.abs(costOfCarry) / lognormalVol) ?
          Math.signum(costOfCarry) - 0.5 * lognormalVol :
          (costOfCarry / lognormalVol - 0.5 * lognormalVol);
      double tmpD2 = Math.abs(coefD2) < SMALL ? 0d : coefD2 * rootT;
      d2 = Double.isNaN(tmpD2) ? Math.signum(coefD2) : tmpD2;
    } else {
      if (sigmaRootT < SMALL) {
        double scnd = (Math.abs(costOfCarry) > LARGE && rootT < SMALL) ? Math.signum(costOfCarry) : costOfCarry * rootT;
        double tmp = (Math.log(spot / strike) / rootT + scnd) / lognormalVol;
        d1 = Double.isNaN(tmp) ? 0d : tmp;
        d2 = d1;
      } else {
        double tmp = costOfCarry * rootT / lognormalVol;
        double sig = (costOfCarry >= 0d) ? 1d : -1d;
        double scnd = Double.isNaN(tmp) ?
            ((lognormalVol < LARGE && lognormalVol > SMALL) ?
                sig / lognormalVol :
                sig * rootT) :
            tmp;
        double d1Tmp = Math.log(spot / strike) / sigmaRootT + scnd + 0.5 * sigmaRootT;
        double d2Tmp = Math.log(spot / strike) / sigmaRootT + scnd - 0.5 * sigmaRootT;
        d1 = Double.isNaN(d1Tmp) ? 0d : d1Tmp;
        d2 = Double.isNaN(d2Tmp) ? 0d : d2Tmp;
      }
    }
    double coefPdf = 0d;
    if (timeToExpiry < SMALL) {
      coefPdf = (Math.abs(spot - strike) < SMALL || (spot > LARGE && strike > LARGE)) ?
          1d / sigmaRootT :
          Math.log(spot / strike) / sigmaRootT / timeToExpiry;
    } else {
      double cocMod = costOfCarry / sigmaRootT;
      if (Double.isNaN(cocMod)) {
        cocMod = 1d;
      }
      double tmp = d1 / timeToExpiry;
      tmp = Double.isNaN(tmp) ? (d1 >= 0d ? 1d : -1.) : tmp;
      coefPdf = cocMod - 0.5 * tmp;
    }

    double normPdf = NORMAL.getPDF(d2);
    double normCdf = NORMAL.getCDF(sign * d2);
    double first = normPdf < SMALL ? 0d : (Double.isNaN(coefPdf) ? 0d : normPdf * coefPdf);
    double second = normCdf < SMALL ? 0d : interestRate * normCdf;
    double res = discount * (first - sign * second);

    return Double.isNaN(res) ? 0d : res;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the spot vega.
   * <p>
   * This is the sensitivity of the option's spot price wrt the implied volatility
   * (which is just the spot vega divided by the numeraire).
   * 
   * @param spot  the spot value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @param interestRate  the interest rate
   * @param costOfCarry  the cost-of-carry rate
   * @return the spot vega
   */
  public static double vega(
      double spot,
      double strike,
      double timeToExpiry,
      double lognormalVol,
      double interestRate,
      double costOfCarry) {

    ArgChecker.isTrue(spot >= 0d, "negative/NaN spot; have {}", spot);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);
    ArgChecker.isFalse(Double.isNaN(interestRate), "interestRate is NaN");
    ArgChecker.isFalse(Double.isNaN(costOfCarry), "costOfCarry is NaN");

    double coef = 0d;
    if ((interestRate > LARGE && costOfCarry > LARGE) || (-interestRate > LARGE && -costOfCarry > LARGE) ||
        Math.abs(costOfCarry - interestRate) < SMALL) {
      coef = 1d; //ref value is returned
    } else {
      double rate = costOfCarry - interestRate;
      if (rate > LARGE) {
        return costOfCarry > LARGE ? 0d : Double.POSITIVE_INFINITY;
      }
      if (-rate > LARGE) {
        return 0d;
      }
      coef = Math.exp(rate * timeToExpiry);
    }

    double rootT = Math.sqrt(timeToExpiry);
    double sigmaRootT = lognormalVol * rootT;
    if (Double.isNaN(sigmaRootT)) {
      sigmaRootT = 1d; //ref value is returned
    }

    double factor = Math.exp(costOfCarry * timeToExpiry);
    if (Double.isNaN(factor)) {
      factor = 1d; //ref value is returned
    }

    double d1 = 0d;
    if (Math.abs(spot - strike) < SMALL || (spot > LARGE && strike > LARGE) || sigmaRootT > LARGE) {
      double coefD1 = (Math.abs(costOfCarry) < SMALL && lognormalVol < SMALL) ?
          Math.signum(costOfCarry) + 0.5 * lognormalVol :
          (costOfCarry / lognormalVol + 0.5 * lognormalVol);
      double tmp = coefD1 * rootT;
      d1 = Double.isNaN(tmp) ? 0d : tmp;
    } else {
      if (sigmaRootT < SMALL || spot > LARGE * strike || strike > LARGE * spot) {
        double scnd = (Math.abs(costOfCarry) > LARGE && rootT < SMALL) ? Math.signum(costOfCarry) : costOfCarry * rootT;
        double tmp = (Math.log(spot / strike) / rootT + scnd) / lognormalVol;
        d1 = Double.isNaN(tmp) ? 0d : tmp;
      } else {
        double tmp = costOfCarry * rootT / lognormalVol;
        double sig = (costOfCarry >= 0d) ? 1d : -1d;
        double scnd =
            Double.isNaN(tmp) ? ((lognormalVol < LARGE && lognormalVol > SMALL) ? sig / lognormalVol : sig * rootT) : tmp;
        d1 = Math.log(spot / strike) / sigmaRootT + scnd + 0.5 * sigmaRootT;
      }
    }
    double norm = NORMAL.getPDF(d1);

    double res = norm < SMALL ? 0d : coef * norm * spot * rootT;
    return Double.isNaN(res) ? Double.POSITIVE_INFINITY : res;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the vanna.
   * <p>
   * This is the second order derivative of the option value, once to the underlying spot and once to volatility.
   * <p>
   * $\frac{\partial^2 FV}{\partial f \partial \sigma}$.
   * 
   * @param spot  the spot value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @param interestRate  the interest rate
   * @param costOfCarry  the cost-of-carry rate
   * @return the spot vanna
   */
  public static double vanna(
      double spot,
      double strike,
      double timeToExpiry,
      double lognormalVol,
      double interestRate,
      double costOfCarry) {

    ArgChecker.isTrue(spot >= 0d, "negative/NaN spot; have {}", spot);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);
    ArgChecker.isFalse(Double.isNaN(interestRate), "interestRate is NaN");
    ArgChecker.isFalse(Double.isNaN(costOfCarry), "costOfCarry is NaN");

    double rootT = Math.sqrt(timeToExpiry);
    double sigmaRootT = lognormalVol * rootT;
    if (Double.isNaN(sigmaRootT)) {
      sigmaRootT = 1d; //ref value is returned
    }

    double d1 = 0d;
    double d2 = 0d;
    if (Math.abs(spot - strike) < SMALL || (spot > LARGE && strike > LARGE) || sigmaRootT > LARGE) {
      double coefD1 = Double.isNaN(Math.abs(costOfCarry) / lognormalVol) ?
          Math.signum(costOfCarry) + 0.5 * lognormalVol :
          (costOfCarry / lognormalVol + 0.5 * lognormalVol);
      double tmpD1 = Math.abs(coefD1) < SMALL ? 0d : coefD1 * rootT;
      d1 = Double.isNaN(tmpD1) ? Math.signum(coefD1) : tmpD1;
      double coefD2 = Double.isNaN(Math.abs(costOfCarry) / lognormalVol) ?
          Math.signum(costOfCarry) - 0.5 * lognormalVol :
          (costOfCarry / lognormalVol - 0.5 * lognormalVol);
      double tmpD2 = Math.abs(coefD2) < SMALL ? 0d : coefD2 * rootT;
      d2 = Double.isNaN(tmpD2) ? Math.signum(coefD2) : tmpD2;
    } else {
      if (sigmaRootT < SMALL) {
        double scnd = (Math.abs(costOfCarry) > LARGE && rootT < SMALL) ? Math.signum(costOfCarry) : costOfCarry * rootT;
        double tmp = (Math.log(spot / strike) / rootT + scnd) / lognormalVol;
        d1 = Double.isNaN(tmp) ? 0d : tmp;
        d2 = d1;
      } else {
        double tmp = costOfCarry * rootT / lognormalVol;
        double sig = (costOfCarry >= 0d) ? 1d : -1d;
        double scnd =
            Double.isNaN(tmp) ? ((lognormalVol < LARGE && lognormalVol > SMALL) ? sig / lognormalVol : sig * rootT) : tmp;
        double d1Tmp = Math.log(spot / strike) / sigmaRootT + scnd + 0.5 * sigmaRootT;
        double d2Tmp = Math.log(spot / strike) / sigmaRootT + scnd - 0.5 * sigmaRootT;
        d1 = Double.isNaN(d1Tmp) ? 0d : d1Tmp;
        d2 = Double.isNaN(d2Tmp) ? 0d : d2Tmp;
      }
    }

    double coef = 0d;
    if ((interestRate > LARGE && costOfCarry > LARGE) || (-interestRate > LARGE && -costOfCarry > LARGE) ||
        Math.abs(costOfCarry - interestRate) < SMALL) {
      coef = 1d; //ref value is returned
    } else {
      double rate = costOfCarry - interestRate;
      if (rate > LARGE) {
        return costOfCarry > LARGE ? 0d : (d2 >= 0d ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
      }
      if (-rate > LARGE) {
        return 0d;
      }
      coef = Math.exp(rate * timeToExpiry);
    }

    double norm = NORMAL.getPDF(d1);
    double tmp = d2 * coef / lognormalVol;
    if (Double.isNaN(tmp)) {
      tmp = coef;
    }
    return norm < SMALL ? 0d : -norm * tmp;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the dual vanna.
   * <p>
   * This is the second order derivative of the option value, once to the strike and once to volatility.
   * 
   * @param spot  the spot value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @param interestRate  the interest rate
   * @param costOfCarry  the cost-of-carry rate
   * @return the spot dual vanna
   */
  public static double dualVanna(
      double spot,
      double strike,
      double timeToExpiry,
      double lognormalVol,
      double interestRate,
      double costOfCarry) {

    ArgChecker.isTrue(spot >= 0d, "negative/NaN spot; have {}", spot);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);
    ArgChecker.isFalse(Double.isNaN(interestRate), "interestRate is NaN");
    ArgChecker.isFalse(Double.isNaN(costOfCarry), "costOfCarry is NaN");

    double rootT = Math.sqrt(timeToExpiry);
    double sigmaRootT = lognormalVol * rootT;
    if (Double.isNaN(sigmaRootT)) {
      sigmaRootT = 1d; //ref value is returned
    }

    double d1 = 0d;
    double d2 = 0d;
    if (Math.abs(spot - strike) < SMALL || (spot > LARGE && strike > LARGE) || sigmaRootT > LARGE) {
      double coefD1 = Double.isNaN(Math.abs(costOfCarry) / lognormalVol) ?
          Math.signum(costOfCarry) + 0.5 * lognormalVol :
          (costOfCarry / lognormalVol + 0.5 * lognormalVol);
      double tmpD1 = Math.abs(coefD1) < SMALL ? 0d : coefD1 * rootT;
      d1 = Double.isNaN(tmpD1) ? Math.signum(coefD1) : tmpD1;
      double coefD2 = Double.isNaN(Math.abs(costOfCarry) / lognormalVol) ?
          Math.signum(costOfCarry) - 0.5 * lognormalVol :
          (costOfCarry / lognormalVol - 0.5 * lognormalVol);
      double tmpD2 = Math.abs(coefD2) < SMALL ? 0d : coefD2 * rootT;
      d2 = Double.isNaN(tmpD2) ? Math.signum(coefD2) : tmpD2;
    } else {
      if (sigmaRootT < SMALL) {
        double scnd = (Math.abs(costOfCarry) > LARGE && rootT < SMALL) ? Math.signum(costOfCarry) : costOfCarry * rootT;
        double tmp = (Math.log(spot / strike) / rootT + scnd) / lognormalVol;
        d1 = Double.isNaN(tmp) ? 0d : tmp;
        d2 = d1;
      } else {
        double tmp = costOfCarry * rootT / lognormalVol;
        double sig = (costOfCarry >= 0d) ? 1d : -1d;
        double scnd =
            Double.isNaN(tmp) ? ((lognormalVol < LARGE && lognormalVol > SMALL) ? sig / lognormalVol : sig * rootT) : tmp;
        double d1Tmp = Math.log(spot / strike) / sigmaRootT + scnd + 0.5 * sigmaRootT;
        double d2Tmp = Math.log(spot / strike) / sigmaRootT + scnd - 0.5 * sigmaRootT;
        d1 = Double.isNaN(d1Tmp) ? 0d : d1Tmp;
        d2 = Double.isNaN(d2Tmp) ? 0d : d2Tmp;
      }
    }

    double coef = Math.exp(-interestRate * timeToExpiry);
    if (coef < SMALL) {
      return 0d;
    }
    if (Double.isNaN(coef)) {
      coef = 1d; //ref value is returned
    }

    double norm = NORMAL.getPDF(d2);
    double tmp = d1 * coef / lognormalVol;
    if (Double.isNaN(tmp)) {
      tmp = coef;
    }

    return norm < SMALL ? 0d : norm * tmp;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the vomma (aka volga).
   * <p>
   * This is the second order derivative of the option spot price with respect to the implied volatility.
   * 
   * @param spot  the spot value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @param interestRate  the interest rate
   * @param costOfCarry  the cost-of-carry rate
   * @return the spot vomma
   */
  public static double vomma(
      double spot,
      double strike,
      double timeToExpiry,
      double lognormalVol,
      double interestRate,
      double costOfCarry) {

    ArgChecker.isTrue(spot >= 0d, "negative/NaN spot; have {}", spot);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);
    ArgChecker.isFalse(Double.isNaN(interestRate), "interestRate is NaN");
    ArgChecker.isFalse(Double.isNaN(costOfCarry), "costOfCarry is NaN");

    double rootT = Math.sqrt(timeToExpiry);
    double sigmaRootT = lognormalVol * rootT;
    if (Double.isNaN(sigmaRootT)) {
      sigmaRootT = 1d; //ref value is returned
    }

    if (spot > LARGE * strike || strike > LARGE * spot || rootT < SMALL) {
      return 0d;
    }

    double d1 = 0d;
    double d1d2Mod = 0d;
    if (Math.abs(spot - strike) < SMALL || (spot > LARGE && strike > LARGE) || rootT > LARGE) {
      double costOvVol =
          (Math.abs(costOfCarry) < SMALL && lognormalVol < SMALL) ? Math.signum(costOfCarry) : costOfCarry / lognormalVol;
      double coefD1 = costOvVol + 0.5 * lognormalVol;
      double coefD1D2Mod = costOvVol * costOvVol / lognormalVol - 0.25 * lognormalVol;
      double tmpD1 = coefD1 * rootT;
      double tmpD1d2Mod = coefD1D2Mod * rootT * timeToExpiry;
      d1 = Double.isNaN(tmpD1) ? 0d : tmpD1;
      d1d2Mod = Double.isNaN(tmpD1d2Mod) ? 1d : tmpD1d2Mod;
    } else {
      if (lognormalVol > LARGE) {
        d1 = 0.5 * sigmaRootT;
        d1d2Mod = -0.25 * sigmaRootT * timeToExpiry;
      } else {
        if (lognormalVol < SMALL) {
          double d1Tmp = (Math.log(spot / strike) / rootT + costOfCarry * rootT) / lognormalVol;
          d1 = Double.isNaN(d1Tmp) ? 1d : d1Tmp;
          d1d2Mod = d1 * d1 * rootT / lognormalVol;
        } else {
          double tmp = Math.log(spot / strike) / sigmaRootT + costOfCarry * rootT / lognormalVol;
          d1 = tmp + 0.5 * sigmaRootT;
          d1d2Mod = (tmp * tmp - 0.25 * sigmaRootT * sigmaRootT) * rootT / lognormalVol;
        }
      }
    }

    double coef = 0d;
    if ((interestRate > LARGE && costOfCarry > LARGE) || (-interestRate > LARGE && -costOfCarry > LARGE) ||
        Math.abs(costOfCarry - interestRate) < SMALL) {
      coef = 1d; //ref value is returned
    } else {
      double rate = costOfCarry - interestRate;
      if (rate > LARGE) {
        return costOfCarry > LARGE ? 0d : (d1d2Mod >= 0d ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
      }
      if (-rate > LARGE) {
        return 0d;
      }
      coef = Math.exp(rate * timeToExpiry);
    }

    double norm = NORMAL.getPDF(d1);
    double tmp = d1d2Mod * spot * coef;
    if (Double.isNaN(tmp)) {
      tmp = coef;
    }

    return norm < SMALL ? 0d : norm * tmp;
  }

  //-------------------------------------------------------------------------
  /**
  * Computes the vega bleed.
  * <p>
  * This is the second order derivative of the option spot price, once to the volatility and once to the time.
  * 
  * @param spot  the spot value of the underlying
  * @param strike  the strike
  * @param timeToExpiry  the time to expiry
  * @param lognormalVol  the log-normal volatility
  * @param interestRate  the interest rate
  * @param costOfCarry  the cost-of-carry rate
  * @return the spot vomma
  */
  public static double vegaBleed(
      double spot,
      double strike,
      double timeToExpiry,
      double lognormalVol,
      double interestRate,
      double costOfCarry) {

    ArgChecker.isTrue(spot >= 0d, "negative/NaN spot; have {}", spot);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);
    ArgChecker.isFalse(Double.isNaN(interestRate), "interestRate is NaN");
    ArgChecker.isFalse(Double.isNaN(costOfCarry), "costOfCarry is NaN");

    double rootT = Math.sqrt(timeToExpiry);
    double sigmaRootT = lognormalVol * rootT;
    if (Double.isNaN(sigmaRootT)) {
      sigmaRootT = 1d; //ref value is returned
    }
    if (spot > LARGE * strike || strike > LARGE * spot || rootT < SMALL) {
      return 0d;
    }

    double d1 = 0d;
    double extra = 0d;
    if (Math.abs(spot - strike) < SMALL || (spot > LARGE && strike > LARGE) || rootT > LARGE) {
      double costOvVol =
          (Math.abs(costOfCarry) < SMALL && lognormalVol < SMALL) ? Math.signum(costOfCarry) : costOfCarry / lognormalVol;
      double coefD1 = costOvVol + 0.5 * lognormalVol;
      double tmpD1 = coefD1 * rootT;
      d1 = Double.isNaN(tmpD1) ? 0d : tmpD1;
      double coefExtra =
          interestRate - 0.5 * costOfCarry + 0.5 * costOvVol * costOvVol + 0.125 * lognormalVol * lognormalVol;
      double tmpExtra = Double.isNaN(coefExtra) ? rootT : coefExtra * rootT;
      extra = Double.isNaN(tmpExtra) ? 1d - 0.5 / rootT : tmpExtra - 0.5 / rootT;
    } else {
      if (lognormalVol > LARGE) {
        d1 = 0.5 * sigmaRootT;
        extra = 0.125 * lognormalVol * sigmaRootT;
      } else {
        if (lognormalVol < SMALL) {
          double resLogRatio = Math.log(spot / strike) / rootT;
          double d1Tmp = (resLogRatio + costOfCarry * rootT) / lognormalVol;
          d1 = Double.isNaN(d1Tmp) ? 1d : d1Tmp;
          double tmpExtra =
              (-0.5 * resLogRatio * resLogRatio / rootT + 0.5 * costOfCarry * costOfCarry * rootT) / lognormalVol / lognormalVol;
          extra = Double.isNaN(tmpExtra) ? 1d : extra;
        } else {
          double resLogRatio = Math.log(spot / strike) / sigmaRootT;
          double tmp = resLogRatio + costOfCarry * rootT / lognormalVol;
          d1 = tmp + 0.5 * sigmaRootT;
          double pDivTmp = interestRate - 0.5 * costOfCarry * (1d - costOfCarry / lognormalVol / lognormalVol);
          double pDiv = Double.isNaN(pDivTmp) ? rootT : pDivTmp * rootT;
          extra = pDiv - 0.5 / rootT - 0.5 * resLogRatio * resLogRatio / rootT + 0.125 * lognormalVol * sigmaRootT;
        }
      }
    }
    double coef = 0d;
    if ((interestRate > LARGE && costOfCarry > LARGE) || (-interestRate > LARGE && -costOfCarry > LARGE) ||
        Math.abs(costOfCarry - interestRate) < SMALL) {
      coef = 1d; //ref value is returned
    } else {
      double rate = costOfCarry - interestRate;
      if (rate > LARGE) {
        return costOfCarry > LARGE ? 0d : (extra >= 0d ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
      }
      if (-rate > LARGE) {
        return 0d;
      }
      coef = Math.exp(rate * timeToExpiry);
    }

    double norm = NORMAL.getPDF(d1);
    double tmp = spot * coef * extra;
    if (Double.isNaN(tmp)) {
      tmp = coef;
    }

    return norm < SMALL ? 0d : tmp * norm;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the rho.
   * <p>
   * This is the derivative of the option value with respect to the risk free interest rate .
   * Note that costOfCarry = interestRate - dividend, which the derivative also acts on.
   * 
   * @param spot  the spot value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @param interestRate  The interest rate
   * @param costOfCarry  the cost of carry
   * @param isCall  true for call, false for put
   * @return the rho
   */
  public static double rho(
      double spot,
      double strike,
      double timeToExpiry,
      double lognormalVol,
      double interestRate,
      double costOfCarry,
      boolean isCall) {

    ArgChecker.isTrue(spot >= 0d, "negative/NaN spot; have {}", spot);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);
    ArgChecker.isFalse(Double.isNaN(interestRate), "interestRate is NaN");
    ArgChecker.isFalse(Double.isNaN(costOfCarry), "costOfCarry is NaN");

    double discount = 0d;
    if (-interestRate > LARGE) {
      return isCall ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
    }
    if (interestRate > LARGE) {
      return 0d;
    }
    discount = (Math.abs(interestRate) < SMALL && timeToExpiry > LARGE) ? 1d : Math.exp(-interestRate * timeToExpiry);

    if (LARGE * spot < strike || timeToExpiry > LARGE) {
      double res = isCall ? 0d : -discount * strike * timeToExpiry;
      return Double.isNaN(res) ? -discount : res;
    }
    if (spot > LARGE * strike || timeToExpiry < SMALL) {
      double res = isCall ? discount * strike * timeToExpiry : 0d;
      return Double.isNaN(res) ? discount : res;
    }

    int sign = isCall ? 1 : -1;
    double rootT = Math.sqrt(timeToExpiry);
    double sigmaRootT = lognormalVol * rootT;
    double factor = Math.exp(costOfCarry * timeToExpiry);
    double rescaledSpot = spot * factor;

    double d2 = 0d;
    if (Math.abs(spot - strike) < SMALL || sigmaRootT > LARGE || (spot > LARGE && strike > LARGE)) {
      double coefD1 = (costOfCarry / lognormalVol - 0.5 * lognormalVol);
      double tmp = coefD1 * rootT;
      d2 = Double.isNaN(tmp) ? 0d : tmp;
    } else {
      if (sigmaRootT < SMALL) {
        return isCall ?
            (rescaledSpot > strike ? discount * strike * timeToExpiry : 0d) :
            (rescaledSpot < strike ? -discount * strike * timeToExpiry : 0d);
      }
      double tmp = costOfCarry * rootT / lognormalVol;
      double sig = (costOfCarry >= 0d) ? 1d : -1d;
      double scnd =
          Double.isNaN(tmp) ? ((lognormalVol < LARGE && lognormalVol > SMALL) ? sig / lognormalVol : sig * rootT) : tmp;
      d2 = Math.log(spot / strike) / sigmaRootT + scnd - 0.5 * sigmaRootT;
    }
    double norm = NORMAL.getCDF(sign * d2);
    double result = norm < SMALL ? 0d : sign * discount * strike * timeToExpiry * norm;
    return Double.isNaN(result) ? sign * discount : result;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the carry rho.
   * <p>
   * This is the derivative of the option value with respect to the cost of carry .
   * Note that costOfCarry = interestRate - dividend, which the derivative also acts on.
   * 
   * @param spot  the spot value of the underlying
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param lognormalVol  the log-normal volatility
   * @param interestRate  The interest rate
   * @param costOfCarry  The cost of carry
   * @param isCall  true for call, false for put
   * @return the carry rho
   */
  public static double carryRho(
      double spot,
      double strike,
      double timeToExpiry,
      double lognormalVol,
      double interestRate,
      double costOfCarry,
      boolean isCall) {

    ArgChecker.isTrue(spot >= 0d, "negative/NaN spot; have {}", spot);
    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);
    ArgChecker.isFalse(Double.isNaN(interestRate), "interestRate is NaN");
    ArgChecker.isFalse(Double.isNaN(costOfCarry), "costOfCarry is NaN");

    double coef = 0d;
    if ((interestRate > LARGE && costOfCarry > LARGE) || (-interestRate > LARGE && -costOfCarry > LARGE) ||
        Math.abs(costOfCarry - interestRate) < SMALL) {
      coef = 1d; //ref value is returned
    } else {
      double rate = costOfCarry - interestRate;
      if (rate > LARGE) {
        return isCall ? Double.POSITIVE_INFINITY : (costOfCarry > LARGE ? 0d : Double.NEGATIVE_INFINITY);
      }
      if (-rate > LARGE) {
        return 0d;
      }
      coef = Math.exp(rate * timeToExpiry);
    }

    if (spot > LARGE * strike || timeToExpiry > LARGE) {
      double res = isCall ? coef * spot * timeToExpiry : 0d;
      return Double.isNaN(res) ? coef : res;
    }
    if (LARGE * spot < strike || timeToExpiry < SMALL) {
      double res = isCall ? 0d : -coef * spot * timeToExpiry;
      return Double.isNaN(res) ? -coef : res;
    }

    int sign = isCall ? 1 : -1;
    double rootT = Math.sqrt(timeToExpiry);
    double sigmaRootT = lognormalVol * rootT;
    double factor = Math.exp(costOfCarry * timeToExpiry);
    double rescaledSpot = spot * factor;

    double d1 = 0d;
    if (Math.abs(spot - strike) < SMALL || sigmaRootT > LARGE || (spot > LARGE && strike > LARGE)) {
      double coefD1 = (costOfCarry / lognormalVol + 0.5 * lognormalVol);
      double tmp = coefD1 * rootT;
      d1 = Double.isNaN(tmp) ? 0d : tmp;
    } else {
      if (sigmaRootT < SMALL) {
        return isCall ? (rescaledSpot > strike ? coef * timeToExpiry * spot : 0d) : (rescaledSpot < strike ? -coef *
            timeToExpiry * spot : 0d);
      }
      double tmp = costOfCarry * rootT / lognormalVol;
      double sig = (costOfCarry >= 0d) ? 1d : -1d;
      double scnd =
          Double.isNaN(tmp) ? ((lognormalVol < LARGE && lognormalVol > SMALL) ? sig / lognormalVol : sig * rootT) : tmp;
      d1 = Math.log(spot / strike) / sigmaRootT + scnd + 0.5 * sigmaRootT;
    }
    double norm = NORMAL.getCDF(sign * d1);

    double result = norm < SMALL ? 0d : sign * coef * timeToExpiry * spot * norm;
    return Double.isNaN(result) ? sign * coef : result;
  }

}
