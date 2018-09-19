package com.opengamma.strata.pricer.impl.option.american;
import java.util.function.Function;
import com.google.common.primitives.Doubles;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.FunctionUtils;
import com.opengamma.strata.math.impl.rootfinding.BisectionSingleRootFinder;
import com.opengamma.strata.math.impl.statistics.distribution.BivariateNormalDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.ProbabilityDistribution;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.impl.option.BlackScholesFormulaRepository;
import com.opengamma.strata.pricer.impl.option.GenericImpliedVolatiltySolver;

/**
 * Roll-Geske-Whaley Model prices an American call option whose underlying pays one cash dividend at a certain time before the option expiry. 
 */
public class RollGeskeWhaleyModel {
  
  private static final ProbabilityDistribution<double[]> BIVARIATE_NORMAL = new BivariateNormalDistribution();
  private static final ProbabilityDistribution<Double> NORMAL = new NormalDistribution(0, 1);
  private static final double EPS = 1.e-12;
  
  /**
   * Default constructor
   */
  public RollGeskeWhaleyModel() {
  }
  
  /**
   * @param spot The spot price of underlying
   * @param strike The strike price
   * @param interestRate The interest rate
   * @param timeToExpiry The time to expiry 
   * @param volatility The volatility 
   * @param dividends The cash dividend amount
   * @param dividendTimes The time when the dividend is paid
   * @return The call option price
   */
  public double price(final double spot, final double strike, final double interestRate, final double timeToExpiry, final double volatility, final double[] dividends, final double[] dividendTimes) {
    ArgChecker.isTrue(spot > 0., "spot is not positive");
    ArgChecker.isTrue(strike > 0., "strike is not positive");
    ArgChecker.isFalse(Double.isNaN(interestRate), "interestRate is NaN");
    ArgChecker.isTrue(timeToExpiry > 0., "timeToExpiry is not positive");
    ArgChecker.isTrue(volatility >= -0., "volatility is negative");
    
    ArgChecker.notNull(dividends, "dividends");
    ArgChecker.notNull(dividendTimes, "dividendTimes");
    final int nDivs = dividends.length;
    ArgChecker.isTrue(nDivs == dividendTimes.length, "dividends and dividendTimes should be the same length");
    for (int i = 0; i < nDivs; ++i) {
      ArgChecker.isTrue(Doubles.isFinite(dividends[i]), "dividends contains infinity or NaN");
      ArgChecker.isTrue(Doubles.isFinite(dividendTimes[i]), "dividendTimes contains infinity or NaN");
    }
    
    if (dividendTimes[0] > timeToExpiry) {
      return BlackScholesFormulaRepository.price(spot, strike, timeToExpiry, volatility, interestRate, interestRate, true);
    }
    final int position = FunctionUtils.getLowerBoundIndex(DoubleArray.ofUnsafe(dividendTimes), timeToExpiry);
    double modSpot = spot;
    for (int i = 0; i < position; ++i) {
      modSpot -= (dividends[i] * Math.exp(-interestRate * dividendTimes[i]));
    }
    
    return price(modSpot, strike, interestRate, timeToExpiry, volatility, dividends[position], dividendTimes[position]);
  }
  
  /**
   * @param spot The spot price of underlying
   * @param strike The strike price
   * @param interestRate The interest rate
   * @param timeToExpiry The time to expiry 
   * @param volatility The volatility 
   * @param dividends The cash dividend amount
   * @param dividendTimes The time when the dividend is paid
   * @return The call option price and Greeks as an array {price, delta, dual delta, rho, theta(timeToExpiry), theta(divTime), vega, gamma}
   */
  public double[] getPriceAdjoint(final double spot, final double strike, final double interestRate, final double timeToExpiry, final double volatility, final double[] dividends,
      final double[] dividendTimes) {
    ArgChecker.isTrue(spot > 0., "spot is not positive");
    ArgChecker.isTrue(strike > 0., "strike is not positive");
    ArgChecker.isFalse(Double.isNaN(interestRate), "interestRate is NaN");
    ArgChecker.isTrue(timeToExpiry > 0., "timeToExpiry is not positive");
    ArgChecker.isTrue(volatility >= -0., "volatility is negative");
    
    ArgChecker.notNull(dividends, "dividends");
    ArgChecker.notNull(dividendTimes, "dividendTimes");
    final int nDivs = dividends.length;
    ArgChecker.isTrue(nDivs == dividendTimes.length, "dividends and dividendTimes should be the same length");
    for (int i = 0; i < nDivs; ++i) {
      ArgChecker.isTrue(Doubles.isFinite(dividends[i]), "dividends contains infinity or NaN");
      ArgChecker.isTrue(Doubles.isFinite(dividendTimes[i]), "dividendTimes contains infinity or NaN");
    }
    
    double[] res = null;
    if (dividendTimes[0] > timeToExpiry) {
      res = new double[8];
      res[0] = BlackScholesFormulaRepository.price(spot, strike, timeToExpiry, volatility, interestRate, interestRate, true);
      res[1] = BlackScholesFormulaRepository.delta(spot, strike, timeToExpiry, volatility, interestRate, interestRate, true);
      res[2] = BlackScholesFormulaRepository.dualDelta(spot, strike, timeToExpiry, volatility, interestRate, interestRate, true);
      res[3] = BlackScholesFormulaRepository.rho(spot, strike, timeToExpiry, volatility, interestRate, interestRate, true);
      res[4] = -BlackScholesFormulaRepository.theta(spot, strike, timeToExpiry, volatility, interestRate, interestRate, true);
      res[5] = 0.;
      res[6] = BlackScholesFormulaRepository.vega(spot, strike, timeToExpiry, volatility, interestRate, interestRate);
      res[7] = BlackScholesFormulaRepository.gamma(spot, strike, timeToExpiry, volatility, interestRate, interestRate);
      return res;
    }
    
    final int position = FunctionUtils.getLowerBoundIndex(DoubleArray.ofUnsafe(dividendTimes), timeToExpiry);
    double modSpot = spot;
    double diffRateSum = 0.;
    double diffDivSum = 0.;
    for (int i = 0; i < position; ++i) {
      final double df = Math.exp(-interestRate * dividendTimes[i]);
      modSpot -= (dividends[i] * df);
      diffRateSum += (dividends[i] * dividendTimes[i] * df);
      diffDivSum += (dividends[i] * df);
    }
    
    res = getPriceAdjoint(modSpot, strike, interestRate, timeToExpiry, volatility, dividends[position], dividendTimes[position]);
    res[3] += (res[1] * diffRateSum);
    res[5] += (res[1] * interestRate * diffDivSum);
    
    return res;
  }
  
  /**
   * @param price The call option price
   * @param spot The spot price of underlying
   * @param strike The strike price
   * @param interestRate The interest rate
   * @param timeToExpiry The time to expiry
   * @param dividends The cash dividend amount
   * @param dividendTimes The time when the dividend is paid
   * @return Implied volatility
   */
  public double impliedVolatility(final double price, final double spot, final double strike, final double interestRate, final double timeToExpiry, final double[] dividends,
      final double[] dividendTimes) {
    
    ArgChecker.notNull(dividends, "dividends");
    ArgChecker.notNull(dividendTimes, "dividendTimes");
    final int nDivs = dividends.length;
    ArgChecker.isTrue(nDivs == dividendTimes.length, "dividends and dividendTimes should be the same length");
    for (int i = 0; i < nDivs; ++i) {
      ArgChecker.isTrue(Doubles.isFinite(dividends[i]), "dividends contains infinity or NaN");
      ArgChecker.isTrue(Doubles.isFinite(dividendTimes[i]), "dividendTimes contains infinity or NaN");
    }
    
    if (dividendTimes[0] > timeToExpiry) {
      final double dfInv = Math.exp(interestRate * timeToExpiry);
      final double fwd = spot * dfInv;
      final double fwdPrice = price * dfInv;
      return BlackFormulaRepository.impliedVolatility(fwdPrice, fwd, strike, timeToExpiry, true);
    }
    
    final int position = FunctionUtils.getLowerBoundIndex(DoubleArray.ofUnsafe(dividendTimes), timeToExpiry);
    double modSpot = spot;
    for (int i = 0; i < position; ++i) {
      modSpot -= dividends[i] * Math.exp(-interestRate * dividendTimes[i]);
    }
    
    final Function<Double, double[]> func = getPriceAndVegaFunction(modSpot, strike, interestRate, timeToExpiry, dividends[position], dividendTimes[position]);
    GenericImpliedVolatiltySolver solver = new GenericImpliedVolatiltySolver(func);
    return solver.impliedVolatility(price, 0.15);
  }
  
  /**
   * @param spot The spot price of underlying
   * @param strike The strike price
   * @param interestRate The interest rate
   * @param timeToExpiry The time to expiry 
   * @param volatility The volatility 
   * @param dividendAmount The cash dividend amount
   * @param dividendTime The time when the dividend is paid
   * @return The call option price
   */
  public double price(final double spot, final double strike, final double interestRate, final double timeToExpiry, final double volatility, final double dividendAmount, final double dividendTime) {
    ArgChecker.isTrue(spot > 0., "spot is not positive");
    ArgChecker.isTrue(strike > 0., "strike is not positive");
    ArgChecker.isFalse(Double.isNaN(interestRate), "interestRate is NaN");
    ArgChecker.isTrue(timeToExpiry > 0., "timeToExpiry is not positive");
    ArgChecker.isTrue(volatility >= -0., "volatility is negative");
    ArgChecker.isTrue(dividendAmount >= 0. && dividendAmount < spot, "0. <= dividendAmount < spot should be true");
    ArgChecker.isTrue(dividendTime >= 0. && dividendTime < timeToExpiry, "0. <= dividendTime < timeToExpiry should be true");
    
    final double factor = Math.exp(interestRate * (timeToExpiry - dividendTime));
    final double pVal = dividendAmount * Math.exp(-interestRate * dividendTime);
    final double modSpot = spot - pVal;
    
    if (dividendAmount < EPS || dividendAmount < (1. - 1. / factor) * strike + EPS) {
      return BlackScholesFormulaRepository.price(modSpot, strike, timeToExpiry, volatility, interestRate, interestRate, true);
    }
    
    final double discountFactor = Math.exp(-interestRate * timeToExpiry);
    final double sStar = sStarFinder(spot, strike, interestRate, timeToExpiry, volatility, dividendAmount, dividendTime);
    if (dividendTime < EPS) {
      final double res = modSpot >= sStar ? spot - strike : BlackScholesFormulaRepository.price(modSpot, strike, timeToExpiry, volatility, interestRate, interestRate, true);
      return res;
    }
    
    final double corr = -Math.sqrt(dividendTime / timeToExpiry);
    
    final double sigRootT1 = volatility * Math.sqrt(dividendTime);
    final double sigRootT2 = volatility * Math.sqrt(timeToExpiry);
    
    final double d11 = (Math.log((spot - pVal) / sStar) + interestRate * dividendTime) / sigRootT1 + 0.5 * sigRootT1;
    final double d12 = d11 - sigRootT1;
    final double d21 = (Math.log((spot - pVal) / strike) + interestRate * timeToExpiry) / sigRootT2 + 0.5 * sigRootT2;
    final double d22 = d21 - sigRootT2;
    
    return modSpot * getNormalAndBinormal(d11, d21, corr, 1.) - strike * discountFactor * getNormalAndBinormal(d12, d22, corr, factor) + pVal *
        NORMAL.getCDF(d12);
  }
  
  /**
   * @param spot The spot price of underlying
   * @param strike The strike price
   * @param interestRate The interest rate
   * @param timeToExpiry The time to expiry 
   * @param volatility The volatility 
   * @param dividendAmount The cash dividend amount
   * @param dividendTime The time when the dividend is paid
   * @return The call option price and Greeks as an array {price, delta, dual delta, rho,theta(timeToExpiry), theta(divTime), vega, gamma}
   */
  public double[] getPriceAdjoint(final double spot, final double strike, final double interestRate, final double timeToExpiry, final double volatility, final double dividendAmount,
      final double dividendTime) {
    ArgChecker.isTrue(spot > 0., "spot is not positive");
    ArgChecker.isTrue(strike > 0., "strike is not positive");
    ArgChecker.isFalse(Double.isNaN(interestRate), "interestRate is NaN");
    ArgChecker.isTrue(timeToExpiry > 0., "timeToExpiry is not positive");
    ArgChecker.isTrue(volatility >= -0., "volatility is negative");
    ArgChecker.isTrue(dividendAmount >= 0. && dividendAmount < spot, "0. <= dividendAmount < spot should be true");
    ArgChecker.isTrue(dividendTime >= 0. && dividendTime < timeToExpiry, "0. <= dividendTime < timeToExpiry should be true");
    
    final double factor = Math.exp(interestRate * (timeToExpiry - dividendTime));
    final double pVal = dividendAmount * Math.exp(-interestRate * dividendTime);
    final double modSpot = spot - pVal;
    
    final double[] res = new double[8];
    
    if (dividendAmount < EPS || dividendAmount < (1. - 1. / factor) * strike + EPS) {
      return bsPriceAdjoint(modSpot, strike, timeToExpiry, volatility, interestRate, pVal, dividendTime);
    }
    
    final double discountFactor = Math.exp(-interestRate * timeToExpiry);
    final double[] sStarAdjoint = getSStarAdjoint(spot, strike, interestRate, timeToExpiry, volatility, dividendAmount, dividendTime);
    if (dividendTime < EPS) {
      if (modSpot > sStarAdjoint[0]) {
        res[0] = spot - strike;
        res[1] = 1.;
        res[2] = -1.;
        res[3] = 0.;
        res[4] = 0.;
        res[5] = interestRate * strike;
        res[6] = 0.;
        res[7] = 0.;
        return res;
      }
      return bsPriceAdjoint(modSpot, strike, timeToExpiry, volatility, interestRate, pVal, dividendTime);
    }
    
    final double dscStrike = strike * discountFactor;
    final double corr = -Math.sqrt(dividendTime / timeToExpiry);
    
    final double[][] d1Adjoint = getD1Adjoint(interestRate, volatility, dividendTime, pVal, modSpot, sStarAdjoint);
    final double[][] d2Adjoint = getD2Adjoint(strike, interestRate, timeToExpiry, volatility, dividendTime, pVal, modSpot);
    
    final double[] factorAdjoint = new double[] {factor, 0., 0., factor * (timeToExpiry - dividendTime), factor * interestRate, -factor * interestRate, 0., 0. };
    final double[] corrAdjoint = new double[] {corr, 0., 0., 0., -0.5 * corr / timeToExpiry, 0.5 * corr / dividendTime, 0., 0. };
    final double[] cdf1Adjoint = getCdfAdjoint(d1Adjoint[0], d2Adjoint[0], corrAdjoint, new double[] {1., 0., 0., 0., 0., 0., 0., 0. });
    final double[] cdf2Adjoint = getCdfAdjoint(d1Adjoint[1], d2Adjoint[1], corrAdjoint, factorAdjoint);
    final double[] cdfFracAdjoint = getNormalCdfAdjoint(d1Adjoint[1][0]);
    
    res[0] = modSpot * cdf1Adjoint[0] - dscStrike * cdf2Adjoint[0] + pVal * cdfFracAdjoint[0];
    res[1] = cdf1Adjoint[0] + modSpot * cdf1Adjoint[1] - dscStrike * cdf2Adjoint[1] + pVal * cdfFracAdjoint[1] * d1Adjoint[1][1];
    res[2] = modSpot * cdf1Adjoint[2] - discountFactor * cdf2Adjoint[0] - dscStrike * cdf2Adjoint[2] + pVal * cdfFracAdjoint[1] * d1Adjoint[1][2];
    res[3] = dividendTime * pVal * cdf1Adjoint[0] + modSpot * cdf1Adjoint[3] + timeToExpiry * dscStrike * cdf2Adjoint[0] - dscStrike * cdf2Adjoint[3] - dividendTime * pVal * cdfFracAdjoint[0] + pVal *
        cdfFracAdjoint[1] * d1Adjoint[1][3];
    res[4] = modSpot * cdf1Adjoint[4] + interestRate * dscStrike * cdf2Adjoint[0] - dscStrike * cdf2Adjoint[4] + pVal * cdfFracAdjoint[1] * d1Adjoint[1][4];
    res[5] = modSpot * cdf1Adjoint[5] + interestRate * pVal * cdf1Adjoint[0] - dscStrike * cdf2Adjoint[5] - interestRate * pVal * cdfFracAdjoint[0] + pVal * cdfFracAdjoint[1] * d1Adjoint[1][5];
    res[6] = modSpot * cdf1Adjoint[6] - dscStrike * cdf2Adjoint[6] + pVal * cdfFracAdjoint[1] * d1Adjoint[1][6];
    res[7] = 2. * cdf1Adjoint[1] + modSpot * cdf1Adjoint[7] - dscStrike * cdf2Adjoint[7] + pVal * cdfFracAdjoint[1] * d1Adjoint[1][7] + pVal * cdfFracAdjoint[2] * d1Adjoint[1][1] * d1Adjoint[1][1];
    
    return res;
  }
  
  /**
   * @param price The call option price
   * @param spot The spot price of underlying
   * @param strike The strike price
   * @param interestRate The interest rate
   * @param timeToExpiry The time to expiry
   * @param dividendAmount The cash dividend amount
   * @param dividendTime The time when the dividend is paid
   * @return Implied volatility
   */
  public double impliedVolatility(final double price, final double spot, final double strike, final double interestRate, final double timeToExpiry, final double dividendAmount,
      final double dividendTime) {
    final Function<Double, double[]> func = getPriceAndVegaFunction(spot, strike, interestRate, timeToExpiry, dividendAmount, dividendTime);
    GenericImpliedVolatiltySolver solver = new GenericImpliedVolatiltySolver(func);
    return solver.impliedVolatility(price, 0.15);
  }
  
  private double[] bsPriceAdjoint(final double modSpot, final double strike, final double timeToExpiry, final double volatility, final double interestRate, final double pVal,
      final double dividendTime) {
    final double[] res = new double[8];
    res[0] = BlackScholesFormulaRepository.price(modSpot, strike, timeToExpiry, volatility, interestRate, interestRate, true);
    res[1] = BlackScholesFormulaRepository.delta(modSpot, strike, timeToExpiry, volatility, interestRate, interestRate, true);
    res[2] = BlackScholesFormulaRepository.dualDelta(modSpot, strike, timeToExpiry, volatility, interestRate, interestRate, true);
    res[3] = BlackScholesFormulaRepository.rho(modSpot, strike, timeToExpiry, volatility, interestRate, interestRate, true) +
        BlackScholesFormulaRepository.delta(modSpot, strike, timeToExpiry, volatility, interestRate, interestRate, true) * pVal * dividendTime;
    res[4] = -BlackScholesFormulaRepository.theta(modSpot, strike, timeToExpiry, volatility, interestRate, interestRate, true);
    res[5] = BlackScholesFormulaRepository.delta(modSpot, strike, timeToExpiry, volatility, interestRate, interestRate, true) * pVal * interestRate;
    res[6] = BlackScholesFormulaRepository.vega(modSpot, strike, timeToExpiry, volatility, interestRate, interestRate);
    res[7] = BlackScholesFormulaRepository.gamma(modSpot, strike, timeToExpiry, volatility, interestRate, interestRate);
    return res;
  }
  
  private double[] getCdfAdjoint(final double[] d1Adjoint, final double[] d2Adjoint, final double[] corrAdjoint, final double[] factorAdjoint) {
    final double[] res = new double[8];
    final double[] normAdj = getNormalCdfAdjoint(d1Adjoint[0]);
    final double[] biAdj = getBivariateNormalCdfAdjoint(d2Adjoint[0], -d1Adjoint[0], corrAdjoint[0]);
    
    res[0] = factorAdjoint[0] * normAdj[0] + biAdj[0];
    for (int i = 1; i < 7; ++i) {
      res[i] = factorAdjoint[i] * normAdj[0] + factorAdjoint[0] * normAdj[1] * d1Adjoint[i] + biAdj[1] * d2Adjoint[i] - biAdj[2] * d1Adjoint[i] + biAdj[3] * corrAdjoint[i];
    }
    res[7] = factorAdjoint[0] * normAdj[1] * d1Adjoint[7] + factorAdjoint[0] * normAdj[2] * d1Adjoint[1] * d1Adjoint[1] + biAdj[1] * d2Adjoint[7] - biAdj[2] * d1Adjoint[7] + biAdj[4] * d2Adjoint[1] *
        d2Adjoint[1] + biAdj[5] * d1Adjoint[1] * d1Adjoint[1] - 2. * biAdj[6] * d1Adjoint[1] * d2Adjoint[1];
    return res;
  }
  
  private double[] getNormalCdfAdjoint(final double d) {
    final double[] res = new double[3];
    res[0] = NORMAL.getCDF(d);
    res[1] = NORMAL.getPDF(d);
    res[2] = -NORMAL.getPDF(d) * d;
    return res;
  }
  
  private double[] getBivariateNormalCdfAdjoint(final double d1, final double d2, final double rho) {
    final double[] res = new double[7];
    final double rhoBar = Math.sqrt(1. - rho * rho);
    res[0] = BIVARIATE_NORMAL.getCDF(new double[] {d1, d2, rho });
    res[1] = NORMAL.getPDF(d1) * NORMAL.getCDF((d2 - rho * d1) / rhoBar);
    res[2] = NORMAL.getPDF(d2) * NORMAL.getCDF((d1 - rho * d2) / rhoBar);
    res[3] = BIVARIATE_NORMAL.getPDF(new double[] {d1, d2, rho });
    res[4] = -d1 * NORMAL.getPDF(d1) * NORMAL.getCDF((d2 - rho * d1) / rhoBar) - rho / rhoBar * NORMAL.getPDF(d1) * NORMAL.getPDF((d2 - rho * d1) / rhoBar);
    res[5] = -d2 * NORMAL.getPDF(d2) * NORMAL.getCDF((d1 - rho * d2) / rhoBar) - rho / rhoBar * NORMAL.getPDF(d2) * NORMAL.getPDF((d1 - rho * d2) / rhoBar);
    res[6] = NORMAL.getPDF(d1) * NORMAL.getPDF((d2 - rho * d1) / rhoBar) / rhoBar;
    return res;
  }
  
  private double[][] getD1Adjoint(final double interestRate, final double volatility, final double dividendTime, final double pVal, final double modSpot,
      final double[] sStarAdjoint) {
    final double[][] res = new double[2][8];
    
    final double rootT1 = Math.sqrt(dividendTime);
    final double sigRootT1 = volatility * rootT1;
    
    final double part1 = Math.log(modSpot / sStarAdjoint[0]) / sigRootT1;
    final double part2 = interestRate * rootT1 / volatility;
    final double part = part1 + part2;
    final double d11 = part + 0.5 * sigRootT1;
    final double d12 = part - 0.5 * sigRootT1;
    
    final double delta = 1. / sigRootT1 / modSpot;
    final double dualDelta = -sStarAdjoint[2] / sigRootT1 / sStarAdjoint[0];
    final double rho = rootT1 * pVal / volatility / modSpot - sStarAdjoint[3] / sStarAdjoint[0] / sigRootT1 + rootT1 / volatility;
    final double theta = -sStarAdjoint[4] / sStarAdjoint[0] / sigRootT1;
    final double divTheta1 = interestRate * pVal / sigRootT1 / modSpot - sStarAdjoint[5] / sStarAdjoint[0] / sigRootT1 - 0.5 * part1 / dividendTime + 0.5 *
        (interestRate + 0.5 * volatility * volatility) / sigRootT1;
    final double divTheta2 = divTheta1 - 0.5 * volatility / rootT1;
    final double vegaPart = -part / volatility - sStarAdjoint[6] / sStarAdjoint[0] / sigRootT1;
    final double vega1 = vegaPart + 0.5 * rootT1;
    final double vega2 = vegaPart - 0.5 * rootT1;
    final double gamma = -delta / modSpot;
    
    res[0] = new double[] {d11, delta, dualDelta, rho, theta, divTheta1, vega1, gamma };
    res[1] = new double[] {d12, delta, dualDelta, rho, theta, divTheta2, vega2, gamma };
    return res;
  }
  
  private double[][] getD2Adjoint(final double strike, final double interestRate, final double timeToExpiry, final double volatility, final double dividendTime, final double pVal,
      final double modSpot) {
    final double[][] res = new double[2][8];
    
    final double rootT2 = Math.sqrt(timeToExpiry);
    final double sigRootT2 = volatility * rootT2;
    
    final double part = (Math.log(modSpot / strike) + interestRate * timeToExpiry) / sigRootT2;
    final double d21 = part + 0.5 * sigRootT2;
    final double d22 = part - 0.5 * sigRootT2;
    
    final double delta = 1. / sigRootT2 / modSpot;
    final double dualDelta = -1. / sigRootT2 / strike;
    final double rho = dividendTime * pVal / sigRootT2 / modSpot + rootT2 / volatility;
    final double theta1 = -0.5 * part / timeToExpiry + 0.5 * (interestRate + 0.5 * volatility * volatility) / sigRootT2;
    final double theta2 = theta1 - 0.5 * volatility / rootT2;
    final double divTheta = interestRate * pVal / sigRootT2 / modSpot;
    final double vega1 = -part / volatility + 0.5 * rootT2;
    final double vega2 = vega1 - rootT2;
    final double gamma = -delta / modSpot;
    
    res[0] = new double[] {d21, delta, dualDelta, rho, theta1, divTheta, vega1, gamma };
    res[1] = new double[] {d22, delta, dualDelta, rho, theta2, divTheta, vega2, gamma };
    return res;
  }
  
  private double[] getSStarAdjoint(final double spot, final double strike, final double interestRate, final double timeToExpiry, final double volatility, final double dividendAmount,
      final double dividendTime) {
    final double sStar = sStarFinder(spot, strike, interestRate, timeToExpiry, volatility, dividendAmount, dividendTime);
    final double blackDeltaBar = 1. - BlackScholesFormulaRepository.delta(sStar, strike, timeToExpiry - dividendTime, volatility, interestRate, interestRate, true);
    
    final double dualDelta = (1. + BlackScholesFormulaRepository.dualDelta(sStar, strike, timeToExpiry - dividendTime, volatility, interestRate, interestRate, true)) / blackDeltaBar;
    final double rho = BlackScholesFormulaRepository.rho(sStar, strike, timeToExpiry - dividendTime, volatility, interestRate, interestRate, true) / blackDeltaBar;
    final double theta = BlackScholesFormulaRepository.theta(sStar, strike, timeToExpiry - dividendTime, volatility, interestRate, interestRate, true) / blackDeltaBar;
    final double vega = BlackScholesFormulaRepository.vega(sStar, strike, timeToExpiry - dividendTime, volatility, interestRate, interestRate) / blackDeltaBar;
    
    return new double[] {sStar, 0., dualDelta, rho, -theta, theta, vega, 0. };
  }
  
  private Function<Double, double[]> getPriceAndVegaFunction(final double spot, final double strike, final double interestRate, final double timeToExpiry, final double dividendAmount,
      final double dividendTime) {
    
    return new Function<Double, double[]>() {
      @Override
      public double[] apply(final Double sigma) {
        final double[] greeks = getPriceAdjoint(spot, strike, interestRate, timeToExpiry, sigma, dividendAmount, dividendTime);
        return new double[] {greeks[0], greeks[6] };
      }
    };
  }
  
  //TODO The rootfinder needs to be improved especially for large sStar
  private double sStarFinder(final double spot, final double strike, final double interestRate, final double timeToExpiry, final double volatility, final double dividendAmount,
      final double dividendTime) {
    final Function<Double, Double> func = getPriceBsFunction(strike, interestRate, timeToExpiry, volatility, dividendAmount, dividendTime);
    final BisectionSingleRootFinder rtFinder = new BisectionSingleRootFinder(1.e-12);
    final double sStar = rtFinder.getRoot(func, strike - 1.05 * dividendAmount, 100. * spot);
    
    return sStar;
  }
  
  private Function<Double, Double> getPriceBsFunction(final double strike, final double interestRate, final double timeToExpiry, final double volatility, final double dividendAmount,
      final double dividendTime) {
    
    return new Function<Double, Double>() {
      @Override
      public Double apply(final Double sStar) {
        return BlackScholesFormulaRepository.price(sStar, strike, timeToExpiry - dividendTime, volatility, interestRate, interestRate, true) - sStar - dividendAmount + strike;
      }
    };
  }
  
  private double getNormalAndBinormal(final double d1, final double d2, final double corr, final double factor) {
    return factor * NORMAL.getCDF(d1) + BIVARIATE_NORMAL.getCDF(new double[] {d2, -d1, corr });
  }
}
