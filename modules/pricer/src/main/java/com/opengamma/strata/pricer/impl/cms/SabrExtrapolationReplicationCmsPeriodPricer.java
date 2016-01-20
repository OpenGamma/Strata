/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.cms;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.OptionalDouble;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.SwaptionSabrSensitivity;
import com.opengamma.strata.math.impl.MathException;
import com.opengamma.strata.math.impl.integration.RungeKuttaIntegrator1D;
import com.opengamma.strata.pricer.cms.SabrExtrapolationReplicationCmsLegPricer;
import com.opengamma.strata.pricer.impl.option.SabrExtrapolationRightFunction;
import com.opengamma.strata.pricer.impl.option.SabrInterestRateParameters;
import com.opengamma.strata.pricer.impl.volatility.smile.function.SabrFormulaData;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.pricer.swaption.SabrParametersSwaptionVolatilities;
import com.opengamma.strata.pricer.swaption.SabrSwaptionVolatilities;
import com.opengamma.strata.product.cms.CmsPeriod;
import com.opengamma.strata.product.cms.CmsPeriodType;
import com.opengamma.strata.product.swap.ExpandedSwap;
import com.opengamma.strata.product.swap.ExpandedSwapLeg;
import com.opengamma.strata.product.swap.RatePaymentPeriod;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLegType;

/**
 *  Computes the price of a CMS coupon/caplet/floorlet by swaption replication on a SABR formula with extrapolation.
 *  <p>
 *  The replication requires numerical integration. This is completed by {@link RungeKuttaIntegrator1D}.
 *  <p>
 *  The consistency between {@code RatesProvider} and {@code SabrParametersSwaptionVolatilities} is not checked in this 
 *  class, but validated only once in {@link SabrExtrapolationReplicationCmsLegPricer}.
 *  <p>
 *  Reference: Hagan, P. S. (2003). Convexity conundrums: Pricing CMS swaps, caps, and floors. 
 *  Wilmott Magazine, March, pages 38--44.
 *  OpenGamma implementation note: Replication pricing for linear and TEC format CMS, Version 1.2, March 2011.
 *  OpenGamma implementation note for the extrapolation: Smile extrapolation, version 1.2, May 2011.
 */
public class SabrExtrapolationReplicationCmsPeriodPricer {

  /**
   * Logger.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(SabrExtrapolationReplicationCmsPeriodPricer.class);
  /**
   * The minimal number of iterations for the numerical integration.
   */
  private static final int NUM_ITER = 10;
  /**
   * The relative tolerance for the numerical integration in PV computation.
   */
  private static final double REL_TOL = 1.0e-10;
  /**
   * The relative tolerance for the numerical integration in sensitivity computation.
   */
  private static final double REL_TOL_STRIKE = 1.0e-5;
  /**
   * The relative tolerance for the numerical integration in sensitivity computation.
   */
  private static final double REL_TOL_VEGA = 1.0e-3;
  /**
   * The maximum iteration count.
   */
  private static final int MAX_COUNT = 10;

  /**
   * Pricer for the underlying swap. 
   */
  private final DiscountingSwapProductPricer swapPricer;
  /**
   * The cut-off strike. 
   * <p>
   * The smile is extrapolated above that level.
   */
  private final double cutOffStrike;
  /**
   * The tail thickness parameter.
   * <p>
   * This must be greater than 0 in order to ensure that the call price converges to 0 for infinite strike.
   */
  private final double mu;

  //-------------------------------------------------------------------------
  /**
   * Obtains the pricer. 
   * 
   * @param swapPricer  the pricer for underlying swap
   * @param cutOffStrike  the cut-off strike value
   * @param mu  the tail thickness
   * @return the pricer
   */
  public static SabrExtrapolationReplicationCmsPeriodPricer of(
      DiscountingSwapProductPricer swapPricer,
      double cutOffStrike,
      double mu) {

    return new SabrExtrapolationReplicationCmsPeriodPricer(swapPricer, cutOffStrike, mu);
  }

  /**
   * Obtains the pricer with default swap pricer. 
   * 
   * @param cutOffStrike  the cut-off strike value
   * @param mu  the tail thickness
   * @return the pricer
   */
  public static SabrExtrapolationReplicationCmsPeriodPricer of(double cutOffStrike, double mu) {
    return of(DiscountingSwapProductPricer.DEFAULT, cutOffStrike, mu);
  }

  private SabrExtrapolationReplicationCmsPeriodPricer(
      DiscountingSwapProductPricer swapPricer,
      double cutOffStrike,
      double mu) {

    this.swapPricer = ArgChecker.notNull(swapPricer, "swapPricer");
    this.cutOffStrike = cutOffStrike;
    this.mu = ArgChecker.notNegativeOrZero(mu, "mu");
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the present value by replication in SABR framework with extrapolation on the right.
   * 
   * @param cmsPeriod  the CMS 
   * @param provider  the rates provider
   * @param swaptionVolatilities  the swaption volatilities
   * @return the present value
   */
  public CurrencyAmount presentValue(
      CmsPeriod cmsPeriod,
      RatesProvider provider,
      SabrParametersSwaptionVolatilities swaptionVolatilities) {

    Currency ccy = cmsPeriod.getCurrency();
    if (provider.getValuationDate().isAfter(cmsPeriod.getPaymentDate())) {
      return CurrencyAmount.zero(ccy);
    }
    Swap swap = cmsPeriod.getUnderlyingSwap();
    ExpandedSwap expandedSwap = swap.expand();
    double dfPayment = provider.discountFactor(ccy, cmsPeriod.getPaymentDate());
    ZonedDateTime valuationDate = swaptionVolatilities.getValuationDateTime();
    LocalDate fixingDate = cmsPeriod.getFixingDate();
    double expiryTime = swaptionVolatilities.relativeTime(
        fixingDate.atTime(valuationDate.toLocalTime()).atZone(valuationDate.getZone()));
    double tenor = swaptionVolatilities.tenor(swap.getStartDate(), swap.getEndDate());
    double shift = swaptionVolatilities.getParameters().shift(expiryTime, tenor);
    double strike = cmsPeriod.getCmsPeriodType().equals(CmsPeriodType.COUPON) ? 0d : cmsPeriod.getStrike();
    if (!fixingDate.isAfter(valuationDate.toLocalDate())) {
      OptionalDouble fixedRate = provider.timeSeries(cmsPeriod.getIndex()).get(fixingDate);
      if (fixedRate.isPresent()) {
        double payoff = 0d;
        switch (cmsPeriod.getCmsPeriodType()) {
          case CAPLET:
            payoff = Math.max(fixedRate.getAsDouble() - strike, 0d);
            break;
          case FLOORLET:
            payoff = Math.max(strike - fixedRate.getAsDouble(), 0d);
            break;
          case COUPON:
            payoff = fixedRate.getAsDouble();
            break;
          default:
            throw new IllegalArgumentException("unsupported CMS type");
        }
        return CurrencyAmount.of(ccy, dfPayment * payoff * cmsPeriod.getNotional() * cmsPeriod.getYearFraction());
      } else if (fixingDate.isBefore(valuationDate.toLocalDate())) {
        throw new IllegalArgumentException(Messages.format(
            "Unable to get fixing for {} on date {}, no time-series supplied", cmsPeriod.getIndex(), fixingDate));
      }
    }
    strike = cmsPeriod.getCmsPeriodType().equals(CmsPeriodType.COUPON) ? 0d : strike + shift;
    double forward = swapPricer.parRate(expandedSwap, provider) + shift;
    double eta = cmsPeriod.getDayCount().relativeYearFraction(cmsPeriod.getPaymentDate(), swap.getStartDate());
    CmsIntegrantProvider intProv = new CmsIntegrantProvider(
        cmsPeriod, expandedSwap, swaptionVolatilities, forward, strike, expiryTime, tenor, cutOffStrike + shift, eta);
    double factor = dfPayment / intProv.h(forward) * intProv.g(forward);
    double strikePart = factor * intProv.k(strike) * intProv.bs(strike);
    double absoluteTolerance = 1d / (factor * Math.abs(cmsPeriod.getNotional()) * cmsPeriod.getYearFraction());
    RungeKuttaIntegrator1D integrator = new RungeKuttaIntegrator1D(absoluteTolerance, REL_TOL, NUM_ITER);
    double integralPart = 0d;
    Function<Double, Double> integrant = intProv.integrant();
    try {
      if (intProv.getPutCall().isCall()) {
        integralPart = dfPayment *
            integrateCall(integrator, integrant, swaptionVolatilities, forward, strike, expiryTime, tenor);
      } else {
        integralPart = dfPayment * integrator.integrate(integrant, 0d, strike);
      }
    } catch (Exception e) {
      throw new MathException(e);
    }
    double priceCMS = (strikePart + integralPart) * cmsPeriod.getNotional() * cmsPeriod.getYearFraction();
    return CurrencyAmount.of(ccy, priceCMS);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the present value curve sensitivity by replication in SABR framework with extrapolation on the right.
   * 
   * @param cmsPeriod  the CMS 
   * @param provider  the rates provider
   * @param swaptionVolatilities  the swaption volatilities
   * @return the present value sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivity(
      CmsPeriod cmsPeriod,
      RatesProvider provider,
      SabrParametersSwaptionVolatilities swaptionVolatilities) {

    Currency ccy = cmsPeriod.getCurrency();
    if (provider.getValuationDate().isAfter(cmsPeriod.getPaymentDate())) {
      return PointSensitivityBuilder.none();
    }
    Swap swap = cmsPeriod.getUnderlyingSwap();
    ExpandedSwap expandedSwap = swap.expand();
    double dfPayment = provider.discountFactor(ccy, cmsPeriod.getPaymentDate());
    ZonedDateTime valuationDate = swaptionVolatilities.getValuationDateTime();
    LocalDate fixingDate = cmsPeriod.getFixingDate();
    double expiryTime = swaptionVolatilities.relativeTime(
        fixingDate.atTime(valuationDate.toLocalTime()).atZone(valuationDate.getZone()));
    double tenor = swaptionVolatilities.tenor(swap.getStartDate(), swap.getEndDate());
    double shift = swaptionVolatilities.getParameters().shift(expiryTime, tenor);
    double strike = cmsPeriod.getStrike();
    if (!fixingDate.isAfter(valuationDate.toLocalDate())) {
      OptionalDouble fixedRate = provider.timeSeries(cmsPeriod.getIndex()).get(fixingDate);
      if (fixedRate.isPresent()) {
        double payoff = 0d;
        switch (cmsPeriod.getCmsPeriodType()) {
          case CAPLET:
            payoff = Math.max(fixedRate.getAsDouble() - strike, 0d);
            break;
          case FLOORLET:
            payoff = Math.max(strike - fixedRate.getAsDouble(), 0d);
            break;
          case COUPON:
            payoff = fixedRate.getAsDouble();
            break;
          default:
            throw new IllegalArgumentException("unsupported CMS type");
        }
        return provider.discountFactors(ccy).zeroRatePointSensitivity(
            cmsPeriod.getPaymentDate()).multipliedBy(payoff * cmsPeriod.getNotional() * cmsPeriod.getYearFraction());
      } else if (fixingDate.isBefore(valuationDate.toLocalDate())) {
        throw new IllegalArgumentException(Messages.format(
            "Unable to get fixing for {} on date {}, no time-series supplied", cmsPeriod.getIndex(), fixingDate));
      }
    }
    strike = cmsPeriod.getCmsPeriodType().equals(CmsPeriodType.COUPON) ? 0d : strike + shift;
    double forward = swapPricer.parRate(expandedSwap, provider) + shift;
    double eta = cmsPeriod.getDayCount().relativeYearFraction(cmsPeriod.getPaymentDate(), swap.getStartDate());
    CmsDeltaIntegrantProvider intProv = new CmsDeltaIntegrantProvider(
        cmsPeriod, expandedSwap, swaptionVolatilities, forward, strike, expiryTime, tenor, cutOffStrike + shift, eta);
    double factor = dfPayment / intProv.h(forward) * intProv.g(forward);
    double absoluteTolerance = 1d / (factor * Math.abs(cmsPeriod.getNotional()) * cmsPeriod.getYearFraction());
    RungeKuttaIntegrator1D integrator = new RungeKuttaIntegrator1D(absoluteTolerance, REL_TOL, NUM_ITER);
    double[] bs = intProv.bsbsp(strike);
    double[] n = intProv.getNnp();
    double strikePartPrice = intProv.k(strike) * n[0] * bs[0];
    double integralPartPrice = 0d;
    double integralPart = 0d;
    Function<Double, Double> integrant = intProv.integrant();
    Function<Double, Double> integrantDelta = intProv.integrantDelta();
    try {
      if (intProv.getPutCall().isCall()) {
        integralPartPrice = integrateCall(integrator, integrant, swaptionVolatilities, forward, strike, expiryTime,
            tenor);
        integralPart = dfPayment *
            integrateCall(integrator, integrantDelta, swaptionVolatilities, forward, strike, expiryTime, tenor);
      } else {
        integralPartPrice = integrator.integrate(integrant, 0d, strike);
        integralPart = dfPayment * integrator.integrate(integrantDelta, 0d, strike);
      }
    } catch (Exception e) {
      throw new MathException(e);
    }
    double deltaPD = (strikePartPrice + integralPartPrice) * cmsPeriod.getNotional() * cmsPeriod.getYearFraction();
    double strikePart = dfPayment * intProv.k(strike) * (n[1] * bs[0] + n[0] * bs[1]);
    double deltaFwd = (strikePart + integralPart) * cmsPeriod.getNotional() * cmsPeriod.getYearFraction();
    PointSensitivityBuilder sensiFwd = swapPricer.parRateSensitivity(expandedSwap, provider).multipliedBy(deltaFwd);
    PointSensitivityBuilder sensiDf = provider.discountFactors(ccy)
        .zeroRatePointSensitivity(cmsPeriod.getPaymentDate()).multipliedBy(deltaPD);
    return sensiFwd.combinedWith(sensiDf);
  }

  /**
   * Computes the present value sensitivity to SABR parameters by replication in SABR framework with extrapolation on the right.
   * 
   * @param cmsPeriod  the CMS 
   * @param provider  the rates provider
   * @param swaptionVolatilities  the swaption volatilities
   * @return the present value sensitivity
   */
  public SwaptionSabrSensitivity presentValueSensitivitySabrParameter(
      CmsPeriod cmsPeriod,
      RatesProvider provider,
      SabrParametersSwaptionVolatilities swaptionVolatilities) {

    Currency ccy = cmsPeriod.getCurrency();
    Swap swap = cmsPeriod.getUnderlyingSwap();
    ExpandedSwap expandedSwap = swap.expand();
    double dfPayment = provider.discountFactor(ccy, cmsPeriod.getPaymentDate());
    ZonedDateTime valuationDate = swaptionVolatilities.getValuationDateTime();
    LocalDate fixingDate = cmsPeriod.getFixingDate();
    ZonedDateTime expiryDate = fixingDate.atTime(valuationDate.toLocalTime()).atZone(valuationDate.getZone());
    double tenor = swaptionVolatilities.tenor(swap.getStartDate(), swap.getEndDate());
    if (provider.getValuationDate().isAfter(cmsPeriod.getPaymentDate())) {
      return SwaptionSabrSensitivity.of(
          cmsPeriod.getIndex().getTemplate().getConvention(), expiryDate, tenor, ccy, 0d, 0d, 0d, 0d);
    }
    if (!fixingDate.isAfter(valuationDate.toLocalDate())) {
      OptionalDouble fixedRate = provider.timeSeries(cmsPeriod.getIndex()).get(fixingDate);
      if (fixedRate.isPresent()) {
        return SwaptionSabrSensitivity.of(
            cmsPeriod.getIndex().getTemplate().getConvention(), expiryDate, tenor, ccy, 0d, 0d, 0d, 0d);
      } else if (fixingDate.isBefore(valuationDate.toLocalDate())) {
        throw new IllegalArgumentException(Messages.format(
            "Unable to get fixing for {} on date {}, no time-series supplied", cmsPeriod.getIndex(), fixingDate));
      }
    }
    double expiryTime = swaptionVolatilities.relativeTime(expiryDate);
    double shift = swaptionVolatilities.getParameters().shift(expiryTime, tenor);
    double strike = cmsPeriod.getCmsPeriodType().equals(CmsPeriodType.COUPON) ? 0d : cmsPeriod.getStrike() + shift;
    double forward = swapPricer.parRate(expandedSwap, provider) + shift;
    double eta = cmsPeriod.getDayCount().relativeYearFraction(cmsPeriod.getPaymentDate(), swap.getStartDate());
    CmsIntegrantProvider intProv = new CmsIntegrantProvider(
        cmsPeriod, expandedSwap, swaptionVolatilities, forward, strike, expiryTime, tenor, cutOffStrike + shift, eta);
    double factor = dfPayment / intProv.h(forward) * intProv.g(forward);
    double factor2 = factor * intProv.k(strike);
    double[] strikePartPrice = intProv.getSabrExtrapolation().priceAdjointSabr(strike, intProv.getPutCall())
        .getDerivatives().multipliedBy(factor2).toArray();
    double absoluteTolerance = 1d / (factor * Math.abs(cmsPeriod.getNotional()) * cmsPeriod.getYearFraction());
    RungeKuttaIntegrator1D integrator = new RungeKuttaIntegrator1D(absoluteTolerance, REL_TOL_VEGA, NUM_ITER);
    double[] totalSensi = new double[4];
    for (int loopparameter = 0; loopparameter < 4; loopparameter++) {
      double integralPart = 0d;
      Function<Double, Double> integrant = intProv.integrantVaga(loopparameter);
      try {
        if (intProv.getPutCall().isCall()) {
          integralPart = dfPayment *
              integrateCall(integrator, integrant, swaptionVolatilities, forward, strike, expiryTime, tenor);
        } else {
          integralPart = dfPayment * integrator.integrate(integrant, 0d, strike);
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      totalSensi[loopparameter] =
          (strikePartPrice[loopparameter] + integralPart) * cmsPeriod.getNotional() * cmsPeriod.getYearFraction();
    }
    return SwaptionSabrSensitivity.of(cmsPeriod.getIndex().getTemplate().getConvention(),
        expiryDate, tenor, ccy, totalSensi[0], totalSensi[1], totalSensi[2], totalSensi[3]);
  }

  /**
   * Computes the present value sensitivity to strike by replication in SABR framework with extrapolation on the right.
   * 
   * @param cmsPeriod  the CMS 
   * @param provider  the rates provider
   * @param swaptionVolatilities  the swaption volatilities
   * @return the present value sensitivity
   */
  public double presentValueSensitivityStrike(
      CmsPeriod cmsPeriod,
      RatesProvider provider,
      SabrParametersSwaptionVolatilities swaptionVolatilities) {

    ArgChecker.isFalse(cmsPeriod.getCmsPeriodType().equals(CmsPeriodType.COUPON),
        "presentValueSensitivityStrike is not relevant for CMS coupon");
    Currency ccy = cmsPeriod.getCurrency();
    if (provider.getValuationDate().isAfter(cmsPeriod.getPaymentDate())) {
      return 0d;
    }
    Swap swap = cmsPeriod.getUnderlyingSwap();
    ExpandedSwap expandedSwap = swap.expand();
    double dfPayment = provider.discountFactor(ccy, cmsPeriod.getPaymentDate());
    ZonedDateTime valuationDate = swaptionVolatilities.getValuationDateTime();
    LocalDate fixingDate = cmsPeriod.getFixingDate();
    double expiryTime = swaptionVolatilities.relativeTime(
        fixingDate.atTime(valuationDate.toLocalTime()).atZone(valuationDate.getZone()));
    double tenor = swaptionVolatilities.tenor(swap.getStartDate(), swap.getEndDate());
    double shift = swaptionVolatilities.getParameters().shift(expiryTime, tenor);
    double strike = cmsPeriod.getStrike();
    if (!fixingDate.isAfter(valuationDate.toLocalDate())) {
      OptionalDouble fixedRate = provider.timeSeries(cmsPeriod.getIndex()).get(fixingDate);
      if (fixedRate.isPresent()) {
        double payoff = 0d;
        switch (cmsPeriod.getCmsPeriodType()) {
          case CAPLET:
            payoff = fixedRate.getAsDouble() >= strike ? -1d : 0d;
            break;
          case FLOORLET:
            payoff = fixedRate.getAsDouble() < strike ? 1d : 0d;
            break;
          default:
            throw new IllegalArgumentException("unsupported CMS type");
        }
        return payoff * cmsPeriod.getNotional() * cmsPeriod.getYearFraction() * dfPayment;
      } else if (fixingDate.isBefore(valuationDate.toLocalDate())) {
        throw new IllegalArgumentException(Messages.format(
            "Unable to get fixing for {} on date {}, no time-series supplied", cmsPeriod.getIndex(), fixingDate));
      }
    }
    strike = cmsPeriod.getCmsPeriodType().equals(CmsPeriodType.COUPON) ? 0d : strike + shift;
    double forward = swapPricer.parRate(expandedSwap, provider) + shift;
    double eta = cmsPeriod.getDayCount().relativeYearFraction(cmsPeriod.getPaymentDate(), swap.getStartDate());
    CmsIntegrantProvider intProv = new CmsIntegrantProvider(
        cmsPeriod, expandedSwap, swaptionVolatilities, forward, strike, expiryTime, tenor, cutOffStrike + shift, eta);
    double factor = dfPayment * intProv.g(forward) / intProv.h(forward);
    double absoluteTolerance = 1.0E-9;
    RungeKuttaIntegrator1D integrator = new RungeKuttaIntegrator1D(absoluteTolerance, REL_TOL_STRIKE, NUM_ITER);
    double[] kpkpp = intProv.kpkpp(strike);
    double firstPart;
    double thirdPart;
    Function<Double, Double> integrant = intProv.integrantDualDelta();
    if (intProv.getPutCall().isCall()) {
      firstPart = -kpkpp[0] * intProv.bs(strike);
      thirdPart = integrateCall(integrator, integrant, swaptionVolatilities, forward, strike, expiryTime, tenor);
    } else {
      firstPart = 3d * kpkpp[0] * intProv.bs(strike);
      thirdPart = integrator.integrate(integrant, 0d, strike);
    }
    double secondPart =
        intProv.k(strike) * intProv.getSabrExtrapolation().priceDerivativeStrike(strike, intProv.getPutCall());
    return cmsPeriod.getNotional() * cmsPeriod.getYearFraction() * factor * (firstPart + secondPart + thirdPart);
  }

  private double integrateCall(
      RungeKuttaIntegrator1D integrator,
      Function<Double, Double> integrant,
      SabrSwaptionVolatilities swaptionVolatilities,
      double forward,
      double strike,
      double expiryTime,
      double tenor) {

    double res;
    double vol = swaptionVolatilities.volatility(expiryTime, tenor, forward, forward);
    double upper = forward * Math.exp(6d * vol * Math.sqrt(expiryTime));
    res = integrator.integrate(integrant, strike, upper);
    double reminder = integrant.apply(upper) * upper;
    double error = reminder / res;
    int count = 0;
    while (Math.abs(error) > integrator.getRelativeTolerance() && count < MAX_COUNT) {
      res += integrator.integrate(integrant, upper, 2d * upper);
      upper *= 2d;
      reminder = integrant.apply(upper) * upper;
      error = reminder / res;
      ++count;
      if (count == MAX_COUNT) {
        LOGGER.info("Maximum iteration count, " + MAX_COUNT
            + ", has been reached. Relative error is greater than " + integrator.getRelativeTolerance());
      }
    }
    return res;
  }

  //-------------------------------------------------------------------------
  /**
   * Inner class to implement the integration used in price replication.
   */
  private class CmsIntegrantProvider {
    /* Small parameter below which a value is regarded as 0. */
    protected static final double EPS = 1E-10;
    private final int nbFixedPeriod;
    private final int nbFixedPaymentYear;
    private final double tau;
    private final double eta;
    private final double strike;
    private final double factor;
    private final SabrExtrapolationRightFunction sabrExtrapolation;
    private final PutCall putCall;

    /**
     * Gets the nbFixedPeriod field.
     * 
     * @return the nbFixedPeriod
     */
    public int getNbFixedPeriod() {
      return nbFixedPeriod;
    }

    /**
     * Gets the nbFixedPaymentYear field.
     * 
     * @return the nbFixedPaymentYear
     */
    public int getNbFixedPaymentYear() {
      return nbFixedPaymentYear;
    }

    /**
     * Gets the tau field.
     * 
     * @return the tau
     */
    public double getTau() {
      return tau;
    }

    /**
     * Gets the eta field.
     * 
     * @return the eta
     */
    public double getEta() {
      return eta;
    }

    /**
     * Gets the putCall field.
     * 
     * @return the putCall
     */
    public PutCall getPutCall() {
      return putCall;
    }

    /**
     * Gets the strike field.
     * 
     * @return the strike
     */
    protected double getStrike() {
      return strike;
    }

    /**
     * Gets the sabrExtrapolation field.
     * 
     * @return the sabrExtrapolation
     */
    public SabrExtrapolationRightFunction getSabrExtrapolation() {
      return sabrExtrapolation;
    }

    public CmsIntegrantProvider(
        CmsPeriod cmsPeriod,
        ExpandedSwap swap,
        SabrParametersSwaptionVolatilities swaptionVolatilities,
        double forward,
        double strike,
        double timeToExpiry,
        double tenor,
        double shiftedCutOff,
        double eta) {

      ExpandedSwapLeg fixedLeg = swap.getLegs(SwapLegType.FIXED).get(0);
      this.nbFixedPeriod = fixedLeg.getPaymentPeriods().size();
      this.nbFixedPaymentYear = (int) Math.round(1d /
          ((RatePaymentPeriod) fixedLeg.getPaymentPeriods().get(0)).getAccrualPeriods().get(0).getYearFraction());
      this.tau = 1d / nbFixedPaymentYear;
      this.eta = eta;
      SabrInterestRateParameters params = swaptionVolatilities.getParameters();
      SabrFormulaData sabrPoint = SabrFormulaData.of(params.alpha(timeToExpiry, tenor),
          params.beta(timeToExpiry, tenor), params.rho(timeToExpiry, tenor), params.nu(timeToExpiry, tenor));
      this.sabrExtrapolation = SabrExtrapolationRightFunction.of(forward, sabrPoint, shiftedCutOff, timeToExpiry, mu);
      this.putCall = cmsPeriod.getCmsPeriodType().equals(CmsPeriodType.FLOORLET) ? PutCall.PUT : PutCall.CALL;
      this.strike = strike;
      this.factor = g(forward) / h(forward);
    }

    /**
     * Obtains the integrant used in price replication.
     * 
     * @return the integrant
     */
    Function<Double, Double> integrant(){
      return new Function<Double, Double>() {
        @Override
        public Double apply(Double x) {
          double[] kD = kpkpp(x);
          // Implementation note: kD[0] contains the first derivative of k; kD[1] the second derivative of k.
          return factor * (kD[1] * (x - strike) + 2d * kD[0]) * bs(x);
        }
      };
    }

    /**
     * Obtains the integrant sensitivity to the i-th SABR parameter.
     * 
     * @param i  the index of SABR parameters
     * @return the vega integrant
     */
    Function<Double, Double> integrantVaga(int i) {
      return new Function<Double, Double>() {
        @Override
        public Double apply(Double x) {
          double[] kD = kpkpp(x);
          // Implementation note: kD[0] contains the first derivative of k; kD[1] the second derivative of k.
          DoubleArray priceDerivativeSABR = getSabrExtrapolation().priceAdjointSabr(x, putCall).getDerivatives();
          return priceDerivativeSABR.get(i) * (factor * (kD[1] * (x - strike) + 2d * kD[0]));
        }
      };
    }

    /**
     * Obtains the integrant sensitivity to strike.
     * 
     * @return the dual delta integrant
     */
    Function<Double, Double> integrantDualDelta() {
      return new Function<Double, Double>() {
        @Override
        public Double apply(Double x) {
          double[] kD = kpkpp(x);
          // Implementation note: kD[0] contains the first derivative of k; kD[1] the second derivative of k.
          return -kD[1] * bs(x);
        }
      };
    }

    /**
     * The approximation of the discount factor as function of the swap rate.
     * 
     * @param x  the swap rate.
     * @return the discount factor.
     */
    double h(double x) {
      return Math.pow(1d + tau * x, eta);
    }

    /**
     * The cash annuity.
     * 
     * @param x  the swap rate.
     * @return the annuity.
     */
    double g(double x) {
      if (x >= EPS) {
        double periodFactor = 1d + x / nbFixedPaymentYear;
        double nPeriodDiscount = Math.pow(periodFactor, -nbFixedPeriod);
        return (1d - nPeriodDiscount) / x;
      }
      return ((double) nbFixedPeriod) / nbFixedPaymentYear;
    }

    /**
     * The factor used in the strike part and in the integration of the replication.
     * 
     * @param x  the swap rate.
     * @return the factor.
     */
    double k(double x) {
      double g;
      double h;
      if (x >= EPS) {
        double periodFactor = 1d + x / nbFixedPaymentYear;
        double nPeriodDiscount = Math.pow(periodFactor, -nbFixedPeriod);
        g = (1d - nPeriodDiscount) / x;
        h = Math.pow(1.0 + tau * x, eta);
      } else {
        g = ((double) nbFixedPeriod) / nbFixedPaymentYear;
        h = 1d;
      }
      return h / g;
    }

    /**
     * The first and second derivative of the function k.
     * <p>
     * The first element is the first derivative and the second element is second derivative.
     * 
     * @param x  the swap rate.
     * @return the derivatives
     */
    protected double[] kpkpp(double x) {
      double periodFactor = 1d + x / nbFixedPaymentYear;
      double nPeriodDiscount = Math.pow(periodFactor, -nbFixedPeriod);
      /**
       * The value of the annuity and its first and second derivative.
       */
      double g, gp, gpp;
      if (x >= EPS) {
        g = (1d - nPeriodDiscount) / x;
        gp = -g / x + nbFixedPeriod * nPeriodDiscount / (x * nbFixedPaymentYear * periodFactor);
        gpp = 2d / (x * x) * g - 2d * nbFixedPeriod * nPeriodDiscount / (x * x * nbFixedPaymentYear * periodFactor)
            - (nbFixedPeriod + 1d) * nbFixedPeriod * nPeriodDiscount
            / (x * nbFixedPaymentYear * nbFixedPaymentYear * periodFactor * periodFactor);
      } else {
        // Implementation comment: When x is (almost) 0, useful for CMS swaps which are priced as CMS cap of strike 0.
        g = ((double) nbFixedPeriod) / nbFixedPaymentYear;
        gp = -0.5d * nbFixedPeriod * (nbFixedPeriod + 1d) / (nbFixedPaymentYear * nbFixedPaymentYear);
        gpp = 0.5d * nbFixedPeriod * (nbFixedPeriod + 1d) * (1d + (nbFixedPeriod + 2d) / 3d) /
            (nbFixedPaymentYear * nbFixedPaymentYear * nbFixedPaymentYear);
      }
      double h = Math.pow(1d + tau * x, eta);
      double hp = eta * tau * h / periodFactor;
      double hpp = (eta - 1d) * tau * hp / periodFactor;
      double kp = hp / g - h * gp / (g * g);
      double kpp = hpp / g - 2d * hp * gp / (g * g) - h * (gpp / (g * g) - 2d * (gp * gp) / (g * g * g));
      return new double[] {kp, kpp };
    }

    /**
     * The Black price with numeraire 1 as function of the strike.
     * 
     * @param strike  the strike.
     * @return the Black prcie.
     */
    double bs(double strike) {
      return sabrExtrapolation.price(strike, putCall);
    }
  }
  
  /**
   * Inner class to implement the integration used for delta calculation.
   */
  private class CmsDeltaIntegrantProvider extends CmsIntegrantProvider {

    private final double[] nnp;

    public CmsDeltaIntegrantProvider(
        CmsPeriod cmsPeriod,
        ExpandedSwap swap,
        SabrParametersSwaptionVolatilities swaptionVolatilities,
        double forward,
        double strike,
        double timeToExpiry,
        double tenor,
        double shiftedCutOff,
        double eta) {
      super(cmsPeriod, swap, swaptionVolatilities, forward, strike, timeToExpiry, tenor, shiftedCutOff, eta);
      this.nnp = nnp(forward);
    }

    /**
     * Gets the nnp field.
     * 
     * @return the nnp
     */
    public double[] getNnp() {
      return nnp;
    }

    /**
     * Obtains the integrant sensitivity to forward.
     * 
     * @return the delta integrant
     */
    Function<Double, Double> integrantDelta() {
      return new Function<Double, Double>() {
        @Override
        public Double apply(Double x) {
          double[] kD = kpkpp(x);
          // Implementation note: kD[0] contains the first derivative of k; kD[1] the second derivative of k.
          double[] bs = bsbsp(x);
          return (kD[1] * (x - getStrike()) + 2d * kD[0]) * (nnp[1] * bs[0] + nnp[0] * bs[1]);
        }
      };
    }

    /**
     * The Black price and its derivative with respect to the forward.
     * 
     * @param strike  the strike.
     * @return the Black price and its derivative.
     */
    private double[] bsbsp(double strike) {
      double[] result = new double[2];
      result[0] = getSabrExtrapolation().price(strike, getPutCall());
      result[1] = getSabrExtrapolation().priceDerivativeForward(strike, getPutCall());
      return result;
    }

    private double[] nnp(double x) {
      double[] result = new double[2];
      double[] ggp = ggp(x);
      double[] hhp = hhp(x);
      result[0] = ggp[0] / hhp[0];
      result[1] = ggp[1] / hhp[0] - ggp[0] * hhp[1] / (hhp[0] * hhp[0]);
      return result;
    }

    private double[] ggp(double x) {
      double[] result = new double[2];
      if (x >= EPS) {
        double periodFactor = 1d + x / getNbFixedPaymentYear();
        double nPeriodDiscount = Math.pow(periodFactor, -getNbFixedPeriod());
        result[0] = (1d - nPeriodDiscount) / x;
        result[1] = -result[0] / x + getTau() * getNbFixedPeriod() * nPeriodDiscount / (x * periodFactor);
      } else {
        result[0] = getNbFixedPeriod() * getTau();
        result[1] = -0.5d * getNbFixedPeriod() * (getNbFixedPeriod() + 1d) * getTau() * getTau();
      }
      return result;
    }

    private double[] hhp(double x) {
      double[] result = new double[2];
      result[0] = Math.pow(1d + getTau() * x, getEta());
      result[1] = getEta() * getTau() * result[0] / (1d + x * getTau());
      return result;
    }
  }

}
