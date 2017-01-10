/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.cms;

import static com.opengamma.strata.market.model.SabrParameterType.ALPHA;
import static com.opengamma.strata.market.model.SabrParameterType.BETA;
import static com.opengamma.strata.market.model.SabrParameterType.NU;
import static com.opengamma.strata.market.model.SabrParameterType.RHO;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.OptionalDouble;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.math.MathException;
import com.opengamma.strata.math.impl.integration.RungeKuttaIntegrator1D;
import com.opengamma.strata.pricer.impl.option.SabrExtrapolationRightFunction;
import com.opengamma.strata.pricer.impl.volatility.smile.SabrFormulaData;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.pricer.swaption.SabrSwaptionVolatilities;
import com.opengamma.strata.pricer.swaption.SwaptionSabrSensitivity;
import com.opengamma.strata.pricer.swaption.SwaptionVolatilitiesName;
import com.opengamma.strata.product.cms.CmsPeriod;
import com.opengamma.strata.product.cms.CmsPeriodType;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.swap.RatePaymentPeriod;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;
import com.opengamma.strata.product.swap.SwapIndex;
import com.opengamma.strata.product.swap.SwapLegType;

/**
 *  Computes the price of a CMS coupon/caplet/floorlet by swaption replication on a shifted SABR formula with extrapolation.
 *  <p>
 *  The extrapolation is done on call prices above a certain strike. See {@link SabrExtrapolationRightFunction} for
 *  more details on the extrapolation method.
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
public final class SabrExtrapolationReplicationCmsPeriodPricer {

  /**
   * Logger.
   */
  private static final Logger log = LoggerFactory.getLogger(SabrExtrapolationReplicationCmsPeriodPricer.class);

  /**
   * The minimal number of iterations for the numerical integration.
   */
  private static final int NUM_ITER = 10;
  /** The relative tolerance for the numerical integration in PV computation. */
  private static final double REL_TOL = 1.0e-10;
  /** The absolute tolerance for the numerical integration in PV computation.
   * The numerical integration stops when the difference between two steps is below the absolute tolerance
   * plus the relative tolerance multiplied by the value.*/
  private static final double ABS_TOL = 1.0e-8;
  /**
   * The relative tolerance for the numerical integration in sensitivity computation.
   */
  private static final double REL_TOL_STRIKE = 1e-5;
  /**
   * The relative tolerance for the numerical integration in sensitivity computation.
   */
  private static final double REL_TOL_VEGA = 1e-3;
  /**
   * The maximum iteration count.
   */
  private static final int MAX_COUNT = 10;
  /**
   * Shift from zero bound for floor.
   * To avoid numerical instability of the SABR function around 0. Shift by 0.01 bps.
   */
  private static final double ZERO_SHIFT = 1e-6;
  /**
   * The minimal time for which the convexity adjustment is computed. The time is less than a day.
   * For expiry below that value, the forward rate is used for present value.
   */
  private static final double MIN_TIME = 1e-4;

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
      SabrSwaptionVolatilities swaptionVolatilities) {

    Currency ccy = cmsPeriod.getCurrency();
    if (provider.getValuationDate().isAfter(cmsPeriod.getPaymentDate())) {
      return CurrencyAmount.zero(ccy);
    }
    SwapIndex index = cmsPeriod.getIndex();
    ResolvedSwap swap = cmsPeriod.getUnderlyingSwap();
    double dfPayment = provider.discountFactor(ccy, cmsPeriod.getPaymentDate());
    ZonedDateTime valuationDate = swaptionVolatilities.getValuationDateTime();
    LocalDate fixingDate = cmsPeriod.getFixingDate();
    double expiryTime = swaptionVolatilities.relativeTime(
        fixingDate.atTime(index.getFixingTime()).atZone(index.getFixingZone()));
    double tenor = swaptionVolatilities.tenor(swap.getStartDate(), swap.getEndDate());
    double shift = swaptionVolatilities.shift(expiryTime, tenor);
    double strikeCpn = cmsPeriod.getCmsPeriodType().equals(CmsPeriodType.COUPON) ? -shift : cmsPeriod.getStrike();
    if (!fixingDate.isAfter(valuationDate.toLocalDate())) {
      OptionalDouble fixedRate = provider.timeSeries(cmsPeriod.getIndex()).get(fixingDate);
      if (fixedRate.isPresent()) {
        double payoff = payOff(cmsPeriod.getCmsPeriodType(), strikeCpn, fixedRate.getAsDouble());
        return CurrencyAmount.of(ccy, dfPayment * payoff * cmsPeriod.getNotional() * cmsPeriod.getYearFraction());
      } else if (fixingDate.isBefore(valuationDate.toLocalDate())) {
        throw new IllegalArgumentException(Messages.format(
            "Unable to get fixing for {} on date {}, no time-series supplied", cmsPeriod.getIndex(), fixingDate));
      }
    }
    double forward = swapPricer.parRate(swap, provider);
    if (expiryTime < MIN_TIME) {
      double payoff = payOff(cmsPeriod.getCmsPeriodType(), strikeCpn, forward);
      return CurrencyAmount.of(ccy, dfPayment * payoff * cmsPeriod.getNotional() * cmsPeriod.getYearFraction());
    }
    double eta = index.getTemplate().getConvention().getFixedLeg().getDayCount()
        .relativeYearFraction(cmsPeriod.getPaymentDate(), swap.getStartDate());
    CmsIntegrantProvider intProv = new CmsIntegrantProvider(
        cmsPeriod, swap, swaptionVolatilities, forward, strikeCpn, expiryTime, tenor, cutOffStrike, eta);
    double factor = dfPayment / intProv.h(forward) * intProv.g(forward);
    double strikePart = factor * intProv.k(strikeCpn) * intProv.bs(strikeCpn);
    RungeKuttaIntegrator1D integrator = new RungeKuttaIntegrator1D(ABS_TOL, REL_TOL, NUM_ITER);
    double integralPart = 0d;
    Function<Double, Double> integrant = intProv.integrant();
    try {
      if (intProv.getPutCall().isCall()) {
        integralPart = dfPayment *
            integrateCall(integrator, integrant, swaptionVolatilities, forward, strikeCpn, expiryTime, tenor);
      } else {
        integralPart = -dfPayment * integrator.integrate(integrant, -shift + ZERO_SHIFT, strikeCpn);
      }
    } catch (Exception e) {
      throw new MathException(e);
    }
    double priceCMS = (strikePart + integralPart);
    if (cmsPeriod.getCmsPeriodType().equals(CmsPeriodType.COUPON)) {
      priceCMS -= dfPayment * shift;
    }
    priceCMS *= cmsPeriod.getNotional() * cmsPeriod.getYearFraction();
    return CurrencyAmount.of(ccy, priceCMS);
  }

  /**
   * Computes the adjusted forward rate for a CMS coupon.
   * <p>
   * The adjusted forward rate, is the number such that, multiplied by the notional, the year fraction and the payment
   * date discount factor, it produces the present value. In other terms, it is the number which used in the same
   * formula used for Ibor coupon pricing will provide the correct present value.
   * <p>
   * For period already fixed, this number will be equal to the swap index fixing.
   * <p>
   * For cap or floor the result is the adjusted forward rate for the coupon equivalent to the cap/floor, 
   * i.e. the coupon with the same dates and index but with no cap or floor strike.
   * 
   * @param cmsPeriod  the CMS period, which should be of the type {@link CmsPeriodType#COUPON}
   * @param provider  the rates provider
   * @param swaptionVolatilities  the swaption volatilities
   * @return the adjusted forward rate
   */
  public double adjustedForwardRate(
      CmsPeriod cmsPeriod,
      RatesProvider provider,
      SabrSwaptionVolatilities swaptionVolatilities) {

    CmsPeriod coupon = cmsPeriod.toCouponEquivalent();
    Currency ccy = cmsPeriod.getCurrency();
    double dfPayment = provider.discountFactor(ccy, coupon.getPaymentDate());
    double pv = presentValue(coupon, provider, swaptionVolatilities).getAmount();
    return pv / (coupon.getNotional() * coupon.getYearFraction() * dfPayment);
  }

  /**
   * Computes the adjustment to the forward rate for a CMS coupon.
   * <p>
   * The adjustment to the forward rate, is the quantity that need to be added to the forward rate to obtain the 
   * adjusted forward rate. The adjusted forward rate is the number which used in the same formula used for 
   * Ibor coupon pricing (forward * notional * accrual factor * discount factor) will provide the correct present value.
   * <p>
   * For cap or floor the result is the adjustment to the forward rate for the coupon equivalent to the cap/floor, 
   * i.e. the coupon with the same dates and index but with no cap or floor strike.
   * 
   * @param cmsPeriod  the CMS period, which should be of the type {@link CmsPeriodType#COUPON}
   * @param provider  the rates provider
   * @param swaptionVolatilities  the swaption volatilities
   * @return the adjusted forward rate
   */
  public double adjustmentToForwardRate(
      CmsPeriod cmsPeriod,
      RatesProvider provider,
      SabrSwaptionVolatilities swaptionVolatilities) {

    CmsPeriod coupon = cmsPeriod.toCouponEquivalent();
    double adjustedForwardRate = adjustedForwardRate(coupon, provider, swaptionVolatilities);
    double forward = swapPricer.parRate(coupon.getUnderlyingSwap(), provider);
    return adjustedForwardRate - forward;
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
  public PointSensitivityBuilder presentValueSensitivityRates(
      CmsPeriod cmsPeriod,
      RatesProvider provider,
      SabrSwaptionVolatilities swaptionVolatilities) {

    Currency ccy = cmsPeriod.getCurrency();
    if (provider.getValuationDate().isAfter(cmsPeriod.getPaymentDate())) {
      return PointSensitivityBuilder.none();
    }
    SwapIndex index = cmsPeriod.getIndex();
    ResolvedSwap swap = cmsPeriod.getUnderlyingSwap();
    double dfPayment = provider.discountFactor(ccy, cmsPeriod.getPaymentDate());
    ZonedDateTime valuationDate = swaptionVolatilities.getValuationDateTime();
    LocalDate fixingDate = cmsPeriod.getFixingDate();
    double expiryTime = swaptionVolatilities.relativeTime(
        fixingDate.atTime(index.getFixingTime()).atZone(index.getFixingZone()));
    double tenor = swaptionVolatilities.tenor(swap.getStartDate(), swap.getEndDate());
    double shift = swaptionVolatilities.shift(expiryTime, tenor);
    double strikeCpn = cmsPeriod.getCmsPeriodType().equals(CmsPeriodType.COUPON) ? -shift : cmsPeriod.getStrike();
    if (!fixingDate.isAfter(valuationDate.toLocalDate())) {
      OptionalDouble fixedRate = provider.timeSeries(cmsPeriod.getIndex()).get(fixingDate);
      if (fixedRate.isPresent()) {
        double payoff = payOff(cmsPeriod.getCmsPeriodType(), strikeCpn, fixedRate.getAsDouble());
        return provider.discountFactors(ccy).zeroRatePointSensitivity(
            cmsPeriod.getPaymentDate()).multipliedBy(payoff * cmsPeriod.getNotional() * cmsPeriod.getYearFraction());
      } else if (fixingDate.isBefore(valuationDate.toLocalDate())) {
        throw new IllegalArgumentException(Messages.format(
            "Unable to get fixing for {} on date {}, no time-series supplied", cmsPeriod.getIndex(), fixingDate));
      }
    }
    double forward = swapPricer.parRate(swap, provider);
    double eta = index.getTemplate().getConvention().getFixedLeg().getDayCount()
        .relativeYearFraction(cmsPeriod.getPaymentDate(), swap.getStartDate());
    CmsDeltaIntegrantProvider intProv = new CmsDeltaIntegrantProvider(
        cmsPeriod, swap, swaptionVolatilities, forward, strikeCpn, expiryTime, tenor, cutOffStrike, eta);
    RungeKuttaIntegrator1D integrator = new RungeKuttaIntegrator1D(ABS_TOL, REL_TOL, NUM_ITER);
    double[] bs = intProv.bsbsp(strikeCpn);
    double[] n = intProv.getNnp();
    double strikePartPrice = intProv.k(strikeCpn) * n[0] * bs[0];
    double integralPartPrice = 0d;
    double integralPart = 0d;
    Function<Double, Double> integrant = intProv.integrant();
    Function<Double, Double> integrantDelta = intProv.integrantDelta();
    try {
      if (intProv.getPutCall().isCall()) {
        integralPartPrice =
            integrateCall(integrator, integrant, swaptionVolatilities, forward, strikeCpn, expiryTime, tenor);
        integralPart = dfPayment *
            integrateCall(integrator, integrantDelta, swaptionVolatilities, forward, strikeCpn, expiryTime, tenor);
      } else {
        integralPartPrice = -integrator.integrate(integrant, -shift + ZERO_SHIFT, strikeCpn);
        integralPart = -dfPayment * integrator.integrate(integrantDelta, -shift, strikeCpn);
      }
    } catch (Exception e) {
      throw new MathException(e);
    }
    double deltaPD = strikePartPrice + integralPartPrice;
    if (cmsPeriod.getCmsPeriodType().equals(CmsPeriodType.COUPON)) {
      deltaPD -= shift;
    }
    deltaPD *= cmsPeriod.getNotional() * cmsPeriod.getYearFraction();
    double strikePart = dfPayment * intProv.k(strikeCpn) * (n[1] * bs[0] + n[0] * bs[1]);
    double deltaFwd = (strikePart + integralPart) * cmsPeriod.getNotional() * cmsPeriod.getYearFraction();
    PointSensitivityBuilder sensiFwd = swapPricer.parRateSensitivity(swap, provider).multipliedBy(deltaFwd);
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
  public PointSensitivityBuilder presentValueSensitivityModelParamsSabr(
      CmsPeriod cmsPeriod,
      RatesProvider provider,
      SabrSwaptionVolatilities swaptionVolatilities) {

    Currency ccy = cmsPeriod.getCurrency();
    SwapIndex index = cmsPeriod.getIndex();
    ResolvedSwap swap = cmsPeriod.getUnderlyingSwap();
    double dfPayment = provider.discountFactor(ccy, cmsPeriod.getPaymentDate());
    ZonedDateTime valuationDate = swaptionVolatilities.getValuationDateTime();
    LocalDate fixingDate = cmsPeriod.getFixingDate();
    ZonedDateTime expiryDate = fixingDate.atTime(index.getFixingTime()).atZone(index.getFixingZone());
    double tenor = swaptionVolatilities.tenor(swap.getStartDate(), swap.getEndDate());
    if (provider.getValuationDate().isAfter(cmsPeriod.getPaymentDate())) {
      return PointSensitivityBuilder.none();
    }
    if (!fixingDate.isAfter(valuationDate.toLocalDate())) {
      OptionalDouble fixedRate = provider.timeSeries(cmsPeriod.getIndex()).get(fixingDate);
      if (fixedRate.isPresent()) {
        return PointSensitivityBuilder.none();
      } else if (fixingDate.isBefore(valuationDate.toLocalDate())) {
        throw new IllegalArgumentException(Messages.format(
            "Unable to get fixing for {} on date {}, no time-series supplied", cmsPeriod.getIndex(), fixingDate));
      }
    }
    double expiryTime = swaptionVolatilities.relativeTime(expiryDate);
    double shift = swaptionVolatilities.shift(expiryTime, tenor);
    double strikeCpn = cmsPeriod.getCmsPeriodType().equals(CmsPeriodType.COUPON) ? -shift : cmsPeriod.getStrike();
    double forward = swapPricer.parRate(swap, provider);
    double eta = index.getTemplate().getConvention().getFixedLeg().getDayCount()
        .relativeYearFraction(cmsPeriod.getPaymentDate(), swap.getStartDate());
    CmsIntegrantProvider intProv = new CmsIntegrantProvider(
        cmsPeriod, swap, swaptionVolatilities, forward, strikeCpn, expiryTime, tenor, cutOffStrike, eta);
    double factor = dfPayment / intProv.h(forward) * intProv.g(forward);
    double factor2 = factor * intProv.k(strikeCpn);
    double[] strikePartPrice = intProv.getSabrExtrapolation()
        .priceAdjointSabr(Math.max(0d, strikeCpn + shift), intProv.getPutCall()) // handle tiny but negative number
        .getDerivatives().multipliedBy(factor2).toArray();
    RungeKuttaIntegrator1D integrator = new RungeKuttaIntegrator1D(ABS_TOL, REL_TOL_VEGA, NUM_ITER);
    double[] totalSensi = new double[4];
    for (int loopparameter = 0; loopparameter < 4; loopparameter++) {
      double integralPart = 0d;
      Function<Double, Double> integrant = intProv.integrantVega(loopparameter);
      try {
        if (intProv.getPutCall().isCall()) {
          integralPart = dfPayment *
              integrateCall(integrator, integrant, swaptionVolatilities, forward, strikeCpn, expiryTime, tenor);
        } else {
          integralPart = -dfPayment * integrator.integrate(integrant, -shift + ZERO_SHIFT, strikeCpn);
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      totalSensi[loopparameter] =
          (strikePartPrice[loopparameter] + integralPart) * cmsPeriod.getNotional() * cmsPeriod.getYearFraction();
    }
    SwaptionVolatilitiesName name = swaptionVolatilities.getName();
    return PointSensitivityBuilder.of(
        SwaptionSabrSensitivity.of(name, expiryTime, tenor, ALPHA, ccy, totalSensi[0]),
        SwaptionSabrSensitivity.of(name, expiryTime, tenor, BETA, ccy, totalSensi[1]),
        SwaptionSabrSensitivity.of(name, expiryTime, tenor, RHO, ccy, totalSensi[2]),
        SwaptionSabrSensitivity.of(name, expiryTime, tenor, NU, ccy, totalSensi[3]));
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
      SabrSwaptionVolatilities swaptionVolatilities) {

    ArgChecker.isFalse(
        cmsPeriod.getCmsPeriodType().equals(CmsPeriodType.COUPON),
        "presentValueSensitivityStrike is not relevant for CMS coupon");
    Currency ccy = cmsPeriod.getCurrency();
    SwapIndex index = cmsPeriod.getIndex();
    if (provider.getValuationDate().isAfter(cmsPeriod.getPaymentDate())) {
      return 0d;
    }
    ResolvedSwap swap = cmsPeriod.getUnderlyingSwap();
    double dfPayment = provider.discountFactor(ccy, cmsPeriod.getPaymentDate());
    ZonedDateTime valuationDate = swaptionVolatilities.getValuationDateTime();
    LocalDate fixingDate = cmsPeriod.getFixingDate();
    double tenor = swaptionVolatilities.tenor(swap.getStartDate(), swap.getEndDate());
    ZonedDateTime expiryDate = fixingDate.atTime(index.getFixingTime()).atZone(index.getFixingZone());
    double expiryTime = swaptionVolatilities.relativeTime(expiryDate);
    double strike = cmsPeriod.getStrike();
    double shift = swaptionVolatilities.shift(expiryTime, tenor);
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
    double forward = swapPricer.parRate(swap, provider);
    double eta = index.getTemplate().getConvention().getFixedLeg().getDayCount()
        .relativeYearFraction(cmsPeriod.getPaymentDate(), swap.getStartDate());
    CmsIntegrantProvider intProv = new CmsIntegrantProvider(
        cmsPeriod, swap, swaptionVolatilities, forward, strike, expiryTime, tenor, cutOffStrike, eta);
    double factor = dfPayment * intProv.g(forward) / intProv.h(forward);
    RungeKuttaIntegrator1D integrator = new RungeKuttaIntegrator1D(ABS_TOL, REL_TOL_STRIKE, NUM_ITER);
    double[] kpkpp = intProv.kpkpp(strike);
    double firstPart;
    double thirdPart;
    Function<Double, Double> integrant = intProv.integrantDualDelta();
    if (intProv.getPutCall().isCall()) {
      firstPart = -kpkpp[0] * intProv.bs(strike);
      thirdPart = integrateCall(integrator, integrant, swaptionVolatilities, forward, strike, expiryTime, tenor);
    } else {
      firstPart = -kpkpp[0] * intProv.bs(strike);
      thirdPart = -integrator.integrate(integrant, -shift + ZERO_SHIFT, strike);
    }
    double secondPart =
        intProv.k(strike) * intProv.getSabrExtrapolation().priceDerivativeStrike(strike + shift, intProv.getPutCall());
    return cmsPeriod.getNotional() * cmsPeriod.getYearFraction() * factor * (firstPart + secondPart + thirdPart);
  }

  private double payOff(CmsPeriodType cmsPeriodType, double strikeCpn, Double fixedRate) {
    double payoff = 0d;
    switch (cmsPeriodType) {
      case CAPLET:
        payoff = Math.max(fixedRate - strikeCpn, 0d);
        break;
      case FLOORLET:
        payoff = Math.max(strikeCpn - fixedRate, 0d);
        break;
      case COUPON:
        payoff = fixedRate;
        break;
      default:
        throw new IllegalArgumentException("unsupported CMS type");
    }
    return payoff;
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
    double upper0 = Math.max(
        forward * Math.exp(6d * vol * Math.sqrt(expiryTime)),
        Math.max(cutOffStrike, 2d * strike));  // To ensure that the integral covers a good part of the smile
    double upper = Math.min(upper0, 1d); // To ensure that we don't miss the meaningful part
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
        log.info("Maximum iteration count, " + MAX_COUNT + ", has been reached. Relative error is greater than " +
            integrator.getRelativeTolerance());
      }
    }
    return res;
  }

  //explain PV for an Cms period
  public void explainPresentValue(
      CmsPeriod period,
      RatesProvider ratesProvider,
      SabrSwaptionVolatilities swaptionVolatilities,
      ExplainMapBuilder builder) {

    String type = period.getCmsPeriodType().toString();
    Currency ccy = period.getCurrency();
    LocalDate paymentDate = period.getPaymentDate();
    builder.put(ExplainKey.ENTRY_TYPE, "Cms" + type + "Period");
    builder.put(ExplainKey.STRIKE_VALUE, period.getStrike());
    builder.put(ExplainKey.NOTIONAL, CurrencyAmount.of(ccy, period.getNotional()));
    builder.put(ExplainKey.PAYMENT_DATE, period.getPaymentDate());
    builder.put(ExplainKey.DISCOUNT_FACTOR, ratesProvider.discountFactor(ccy, paymentDate));
    builder.put(ExplainKey.START_DATE, period.getStartDate());
    builder.put(ExplainKey.END_DATE, period.getEndDate());
    builder.put(ExplainKey.FIXING_DATE, period.getFixingDate());
    builder.put(ExplainKey.ACCRUAL_YEAR_FRACTION, period.getYearFraction());
    builder.put(ExplainKey.PRESENT_VALUE, presentValue(period, ratesProvider, swaptionVolatilities));
    builder.put(ExplainKey.FORWARD_RATE, swapPricer.parRate(period.getUnderlyingSwap(), ratesProvider));
    builder.put(ExplainKey.CONVEXITY_ADJUSTED_RATE, adjustedForwardRate(period, ratesProvider, swaptionVolatilities));
  }

  //-------------------------------------------------------------------------
  /**
   * Inner class to implement the integration used in price replication.
   */
  private class CmsIntegrantProvider {
    /* Small parameter below which a value is regarded as 0. */
    protected static final double EPS = 1.0E-4;
    private final int nbFixedPeriod;
    private final int nbFixedPaymentYear;
    private final double tau;
    private final double eta;
    private final double strike;
    private final double shift;
    private final double factor;
    private final SabrExtrapolationRightFunction sabrExtrapolation;
    private final PutCall putCall;
    private final double[] g0;

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
     * Gets the shift field.
     * 
     * @return the shift
     */
    protected double getShift() {
      return shift;
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
        ResolvedSwap swap,
        SabrSwaptionVolatilities swaptionVolatilities,
        double forward,
        double strike,
        double timeToExpiry,
        double tenor,
        double cutOffStrike,
        double eta) {

      ResolvedSwapLeg fixedLeg = swap.getLegs(SwapLegType.FIXED).get(0);
      this.nbFixedPeriod = fixedLeg.getPaymentPeriods().size();
      this.nbFixedPaymentYear = (int) Math.round(1d /
          ((RatePaymentPeriod) fixedLeg.getPaymentPeriods().get(0)).getAccrualPeriods().get(0).getYearFraction());
      this.tau = 1d / nbFixedPaymentYear;
      this.eta = eta;
      SabrFormulaData sabrPoint = SabrFormulaData.of(
          swaptionVolatilities.alpha(timeToExpiry, tenor),
          swaptionVolatilities.beta(timeToExpiry, tenor),
          swaptionVolatilities.rho(timeToExpiry, tenor),
          swaptionVolatilities.nu(timeToExpiry, tenor));
      this.shift = swaptionVolatilities.shift(timeToExpiry, tenor);
      this.sabrExtrapolation = SabrExtrapolationRightFunction
          .of(forward + shift, timeToExpiry, sabrPoint, cutOffStrike + shift, mu);
      this.putCall = cmsPeriod.getCmsPeriodType().equals(CmsPeriodType.FLOORLET) ? PutCall.PUT : PutCall.CALL;
      this.strike = strike;
      this.factor = g(forward) / h(forward);
      this.g0 = new double[4];
      g0[0] = nbFixedPeriod * tau;
      g0[1] = -0.5 * nbFixedPeriod * (nbFixedPeriod + 1.0d) * tau * tau;
      g0[2] = -2.0d / 3.0d * g0[1] * (nbFixedPeriod + 2.0d) * tau;
      g0[3] = -3.0d / 4.0d * g0[2] * (nbFixedPeriod + 2.0d) * tau;
    }

    /**
     * Obtains the integrant used in price replication.
     * 
     * @return the integrant
     */
    Function<Double, Double> integrant() {
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
    Function<Double, Double> integrantVega(int i) {
      return new Function<Double, Double>() {
        @Override
        public Double apply(Double x) {
          double[] kD = kpkpp(x);
          // Implementation note: kD[0] contains the first derivative of k; kD[1] the second derivative of k.
          double xShifted = Math.max(x + shift, 0d); // handle tiny but negative number
          DoubleArray priceDerivativeSabr = getSabrExtrapolation().priceAdjointSabr(xShifted, putCall).getDerivatives();
          return priceDerivativeSabr.get(i) * (factor * (kD[1] * (x - strike) + 2d * kD[0]));
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
      if (Math.abs(x) >= EPS) {
        double periodFactor = 1d + x / nbFixedPaymentYear;
        double nPeriodDiscount = Math.pow(periodFactor, -nbFixedPeriod);
        return (1d - nPeriodDiscount) / x;
      }
      // Special case when x ~ 0: expansion of g around 0
      return g0[0] + g0[1] * x + 0.5 * g0[2] * x * x + g0[3] * x * x * x / 6.0d;
    }

    /**
     * The cash annuity.
     * 
     * @param x  the swap rate.
     * @return the annuity.
     */
    double[] ggpgpp(double x) {
      if (Math.abs(x) >= EPS) {
        double periodFactor = 1d + x / nbFixedPaymentYear;
        double nPeriodDiscount = Math.pow(periodFactor, -nbFixedPeriod);
        double[] ggpgpp = new double[3];
        ggpgpp[0] = (1d - nPeriodDiscount) / x;
        ggpgpp[1] = -ggpgpp[0] / x + nbFixedPeriod * nPeriodDiscount / (x * nbFixedPaymentYear * periodFactor);
        ggpgpp[2] = 2d / (x * x) * ggpgpp[0] -
            2d * nbFixedPeriod * nPeriodDiscount / (x * x * nbFixedPaymentYear * periodFactor) - (nbFixedPeriod + 1d) *
                nbFixedPeriod * nPeriodDiscount / (x * nbFixedPaymentYear * nbFixedPaymentYear * periodFactor * periodFactor);
        return ggpgpp;
      }
      // Special case when x ~ 0: expansion of g around 0
      return new double[] {g0[0] + g0[1] * x + 0.5 * g0[2] * x * x + g0[3] * x * x * x / 6.0d,
          g0[1] + g0[2] * x + 0.5 * g0[3] * x * x, g0[2] + g0[3] * x};
    }

    /**
     * The factor used in the strike part and in the integration of the replication.
     * 
     * @param x  the swap rate.
     * @return the factor.
     */
    double k(double x) {
      double g = g(x);
      double h = Math.pow(1.0 + tau * x, eta);
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
      /**
       * The value of the annuity and its first and second derivative.
       */
      double[] ggpgpp = ggpgpp(x);
      double h = Math.pow(1d + tau * x, eta);
      double hp = eta * tau * h / periodFactor;
      double hpp = (eta - 1d) * tau * hp / periodFactor;
      double kp = hp / ggpgpp[0] - h * ggpgpp[1] / (ggpgpp[0] * ggpgpp[0]);
      double kpp = hpp / ggpgpp[0] - 2d * hp * ggpgpp[1] / (ggpgpp[0] * ggpgpp[0]) -
          h * (ggpgpp[2] / (ggpgpp[0] * ggpgpp[0]) - 2d * (ggpgpp[1] * ggpgpp[1]) / (ggpgpp[0] * ggpgpp[0] * ggpgpp[0]));
      return new double[] {kp, kpp};
    }

    /**
     * The Black price with numeraire 1 as function of the strike.
     * 
     * @param strike  the strike.
     * @return the Black prcie.
     */
    double bs(double strike) {
      double strikeShifted = Math.max(strike + getShift(), 0d); // handle tiny but negative number
      return sabrExtrapolation.price(strikeShifted, putCall);
    }
  }

  /**
   * Inner class to implement the integration used for delta calculation.
   */
  private class CmsDeltaIntegrantProvider extends CmsIntegrantProvider {

    private final double[] nnp;

    public CmsDeltaIntegrantProvider(
        CmsPeriod cmsPeriod,
        ResolvedSwap swap,
        SabrSwaptionVolatilities swaptionVolatilities,
        double forward,
        double strike,
        double timeToExpiry,
        double tenor,
        double cutOffStrike,
        double eta) {
      super(cmsPeriod, swap, swaptionVolatilities, forward, strike, timeToExpiry, tenor, cutOffStrike, eta);
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
      double strikeShifted = Math.max(strike + getShift(), 0d); // handle tiny but negative number
      result[0] = getSabrExtrapolation().price(strikeShifted, getPutCall());
      result[1] = getSabrExtrapolation().priceDerivativeForward(strikeShifted, getPutCall());
      return result;
    }

    private double[] nnp(double x) {
      double[] result = new double[2];
      double[] ggpgpp = ggpgpp(x);
      double[] hhp = hhp(x);
      result[0] = ggpgpp[0] / hhp[0];
      result[1] = ggpgpp[1] / hhp[0] - ggpgpp[0] * hhp[1] / (hhp[0] * hhp[0]);
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
