/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda.e2e;

import java.util.Arrays;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.pricer.impl.credit.isda.AnalyticCdsPricer;
import com.opengamma.strata.pricer.impl.credit.isda.CdsAnalytic;
import com.opengamma.strata.pricer.impl.credit.isda.CdsPriceType;
import com.opengamma.strata.pricer.impl.credit.isda.CreditCurveCalibrator;
import com.opengamma.strata.pricer.impl.credit.isda.InterestRateSensitivityCalculator;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantCreditCurve;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantYieldCurve;

/**
 * 
 */
public class CdsIndexCalculator {
  // this code has been moved from src/main/java to src/test/java

  private static final double ONE_BPS = 1e-4;
  private final AnalyticCdsPricer _pricer;

  public CdsIndexCalculator() {
    _pricer = new AnalyticCdsPricer();
  }

  /**
   * The Points-Up-Front (PUF) of an index. This is the (clean) price of a unit notional index.
   * The actual clean price is this multiplied by the (current) index notional 
   * (i.e. the initial notional times the index factor).
   * 
   * @param indexCDS analytic description of a CDS traded at a certain time
   * @param indexCoupon The coupon of the index (as a fraction)
   * @param yieldCurve The yield curve
   * @param intrinsicData credit curves, weights and recover
   * @return PUF of an index 
   */
  public double indexPUF(
      CdsAnalytic indexCDS,
      double indexCoupon,
      IsdaCompliantYieldCurve yieldCurve,
      IntrinsicIndexDataBundle intrinsicData) {

    ArgChecker.notNull(intrinsicData, "intrinsicData");
    if (intrinsicData.getNumOfDefaults() == intrinsicData.getIndexSize()) {
      throw new IllegalArgumentException("Index completely defaulted - not possible to rescale for PUF");
    }
    return indexPV(indexCDS, indexCoupon, yieldCurve, intrinsicData) / intrinsicData.getIndexFactor();
  }

  /**
  * Intrinsic (normalised) price an index from the credit curves of the individual single names.
  * To get the actual index value, this multiplied by the <b>initial</b>  notional of the index.
  * 
   * @param indexCDS analytic description of a CDS traded at a certain time
   * @param indexCoupon The coupon of the index (as a fraction)
   * @param yieldCurve The yield curve
   * @param intrinsicData credit curves, weights and recovery rates of the intrinsic names
   * @return The index value for a unit  notional.
   */
  public double indexPV(
      CdsAnalytic indexCDS,
      double indexCoupon,
      IsdaCompliantYieldCurve yieldCurve,
      IntrinsicIndexDataBundle intrinsicData) {

    double prot = indexProtLeg(indexCDS, yieldCurve, intrinsicData);
    double annuity = indexAnnuity(indexCDS, yieldCurve, intrinsicData);
    return prot - indexCoupon * annuity;
  }

  /**
   * Intrinsic (normalised) price an index from the credit curves of the individual single names.
   * To get the actual index value, this multiplied by the <b>initial</b>  notional of the index.
   * 
   * @param indexCDS analytic description of a CDS traded at a certain time
   * @param indexCoupon The coupon of the index (as a fraction)
   * @param yieldCurve The yield curve
   * @param intrinsicData credit curves, weights and recovery rates of the intrinsic names
   * @param priceType Clean or dirty price
   * @return The index value for a unit  notional.
   */
  public double indexPV(
      CdsAnalytic indexCDS,
      double indexCoupon,
      IsdaCompliantYieldCurve yieldCurve,
      IntrinsicIndexDataBundle intrinsicData,
      CdsPriceType priceType) {

    double prot = indexProtLeg(indexCDS, yieldCurve, intrinsicData);
    double annuity = indexAnnuity(indexCDS, yieldCurve, intrinsicData, priceType);
    return prot - indexCoupon * annuity;
  }

  /**
   * Intrinsic (normalised) price an index from the credit curves of the individual single names.
   * To get the actual index value, this multiplied by the <b>initial</b>  notional of the index.
   * 
   * @param indexCDS analytic description of a CDS traded at a certain time
   * @param indexCoupon The coupon of the index (as a fraction)
   * @param yieldCurve The yield curve
   * @param intrinsicData credit curves, weights and recovery rates of the intrinsic names
   * @param priceType Clean or dirty price
   * @param valuationTime The leg value is calculated for today (t=0), then rolled
   *  forward (using the risk free yield curve) to the valuation time.
   *  This is because cash payments occur on the cash-settlement-date, which is usually
   *  three working days after the trade date (today) 
   * @return The index value for a unit  notional.
   */
  public double indexPV(
      CdsAnalytic indexCDS,
      double indexCoupon,
      IsdaCompliantYieldCurve yieldCurve,
      IntrinsicIndexDataBundle intrinsicData,
      CdsPriceType priceType,
      double valuationTime) {

    double prot = indexProtLeg(indexCDS, yieldCurve, intrinsicData, valuationTime);
    double annuity = indexAnnuity(indexCDS, yieldCurve, intrinsicData, priceType, valuationTime);
    return prot - indexCoupon * annuity;
  }

  /**
   * The intrinsic index spread. this is defined as the ratio of the intrinsic protection leg to the intrinsic annuity.
   * 
   * @see #averageSpread
   * @param indexCDS analytic description of a CDS traded at a certain time
   * @param yieldCurve The yield curve
   * @param intrinsicData credit curves, weights and recovery rates of the intrinsic names
   * @return intrinsic index spread (as a fraction)
   */
  public double intrinsicIndexSpread(
      CdsAnalytic indexCDS,
      IsdaCompliantYieldCurve yieldCurve,
      IntrinsicIndexDataBundle intrinsicData) {

    ArgChecker.notNull(intrinsicData, "intrinsicData");
    if (intrinsicData.getNumOfDefaults() == intrinsicData.getIndexSize()) {
      throw new IllegalArgumentException("Every name in the index is defaulted - cannot calculate a spread");
    }
    double prot = indexProtLeg(indexCDS, yieldCurve, intrinsicData);
    double annuity = indexAnnuity(indexCDS, yieldCurve, intrinsicData);
    return prot / annuity;
  }

  /**
   * The normalised intrinsic value of the protection leg of a CDS portfolio (index).
   * The actual value of the leg is this multiplied by the <b>initial</b>  notional of the index.
   * 
   * @param indexCDS representation of the index cashflows (seen from today). 
   * @param yieldCurve The current yield curves 
   * @param intrinsicData credit curves, weights and recovery rates of the intrinsic names
   * @return The normalised intrinsic value of the protection leg.
   */
  public double indexProtLeg(
      CdsAnalytic indexCDS,
      IsdaCompliantYieldCurve yieldCurve,
      IntrinsicIndexDataBundle intrinsicData) {

    ArgChecker.notNull(indexCDS, "indexCDS");
    return indexProtLeg(indexCDS, yieldCurve, intrinsicData, indexCDS.getCashSettleTime());
  }

  /**
   * The normalised intrinsic value of the protection leg of a CDS portfolio (index).
   * The actual value of the leg is this multiplied by the <b>initial</b>  notional of the index.
   * 
   * @param indexCDS representation of the index cashflows (seen from today). 
   * @param yieldCurve The current yield curves 
   * @param intrinsicData credit curves, weights and recovery rates of the intrinsic names
   * @param valuationTime Valuation time. The leg value is calculated for today (t=0),
   *  then rolled forward (using the risk free yield curve) to the valuation time.
   *  This is because cash payments occur on the cash-settlement-date, which is usually
   *  three working days after the trade date (today) 
   * @return The normalised intrinsic value of the protection leg.
   */
  public double indexProtLeg(
      CdsAnalytic indexCDS,
      IsdaCompliantYieldCurve yieldCurve,
      IntrinsicIndexDataBundle intrinsicData,
      double valuationTime) {

    ArgChecker.notNull(indexCDS, "indexCDS");
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    ArgChecker.notNull(intrinsicData, "intrinsicData");

    CdsAnalytic cds = indexCDS.withRecoveryRate(0.0);
    int n = intrinsicData.getIndexSize();
    double protLeg = 0;
    for (int i = 0; i < n; i++) {
      if (!intrinsicData.isDefaulted(i)) {
        protLeg += intrinsicData.getWeight(i) * intrinsicData.getLGD(i) *
            _pricer.protectionLeg(cds, yieldCurve, intrinsicData.getCreditCurve(i), 0);
      }
    }
    protLeg /= yieldCurve.getDiscountFactor(valuationTime);
    return protLeg;
  }

  /**
   * The  intrinsic annuity of a CDS portfolio (index) for a unit (initial) notional.
   * The value of the premium leg is this multiplied by the <b> initial</b> notional of the index
   * and the index coupon (as a fraction).
   * 
   * @param indexCDS representation of the index cashflows (seen from today). 
   * @param yieldCurve The current yield curves 
   * @param intrinsicData credit curves, weights and recovery rates of the intrinsic names
   * @param valuationTime Valuation time. The leg value is calculated for today (t=0), then rolled
   *  forward (using the risk free yield curve) to the valuation time.
   *  This is because cash payments occur on the cash-settlement-date, which is usually
   *  three working days after the trade date (today) 
   * @return The  intrinsic annuity of a CDS portfolio (index)
   */
  public double indexAnnuity(
      CdsAnalytic indexCDS,
      IsdaCompliantYieldCurve yieldCurve,
      IntrinsicIndexDataBundle intrinsicData,
      double valuationTime) {

    return indexAnnuity(indexCDS, yieldCurve, intrinsicData, CdsPriceType.CLEAN, valuationTime);
  }

  /**
   * The  intrinsic annuity of a CDS portfolio (index) for a unit (initial) notional.
   * The value of the premium leg is this multiplied by the <b> initial</b> notional of the index 
   * and the index coupon (as a fraction).
   * 
   * @param indexCDS representation of the index cashflows (seen from today). 
   * @param yieldCurve The current yield curves 
   * @param intrinsicData credit curves, weights and recovery rates of the intrinsic names
   * @return The normalised intrinsic annuity of a CDS portfolio (index)
   */
  public double indexAnnuity(
      CdsAnalytic indexCDS,
      IsdaCompliantYieldCurve yieldCurve,
      IntrinsicIndexDataBundle intrinsicData) {

    return indexAnnuity(indexCDS, yieldCurve, intrinsicData, CdsPriceType.CLEAN, indexCDS.getCashSettleTime());
  }

  /**
   * The  intrinsic annuity of a CDS portfolio (index) for a unit (initial) notional.
   * The value of the premium leg is this multiplied by the <b> initial</b> notional of the index 
   * and the index coupon (as a fraction).
   * 
   * @param indexCDS representation of the index cashflows (seen from today). 
   * @param yieldCurve The current yield curves 
   * @param intrinsicData credit curves, weights and recovery rates of the intrinsic names
   * @param priceType Clean or dirty 
   * @return The normalised intrinsic annuity of a CDS portfolio (index)
   */
  public double indexAnnuity(
      CdsAnalytic indexCDS,
      IsdaCompliantYieldCurve yieldCurve,
      IntrinsicIndexDataBundle intrinsicData,
      CdsPriceType priceType) {

    ArgChecker.notNull(indexCDS, "indexCDS");
    return indexAnnuity(indexCDS, yieldCurve, intrinsicData, priceType, indexCDS.getCashSettleTime());
  }

  /**
   * The  intrinsic annuity of a CDS portfolio (index) for a unit (initial) notional.
   * The value of the premium leg is this multiplied by the <b> initial</b> notional of the index 
   * and the index coupon (as a fraction).
   * 
   * @param indexCDS representation of the index cashflows (seen from today). 
   * @param yieldCurve The current yield curves 
   * @param intrinsicData credit curves, weights and recovery rates of the intrinsic names
   * @param priceType Clean or dirty 
   * @param valuationTime Valuation time. The leg value is calculated for today (t=0),
   *  then rolled forward (using the risk free yield curve) to the valuation time.
   *  This is because cash payments occur on the cash-settlement-date, which is usually
   *  three working days after the trade date (today) 
   * @return The  intrinsic annuity of a CDS portfolio (index)
   */
  public double indexAnnuity(
      CdsAnalytic indexCDS,
      IsdaCompliantYieldCurve yieldCurve,
      IntrinsicIndexDataBundle intrinsicData,
      CdsPriceType priceType,
      double valuationTime) {

    ArgChecker.notNull(indexCDS, "indexCDS");
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    ArgChecker.notNull(intrinsicData, "intrinsicData");

    int n = intrinsicData.getIndexSize();
    double a = 0;
    for (int i = 0; i < n; i++) {
      if (!intrinsicData.isDefaulted(i)) {
        a += intrinsicData.getWeight(i) * _pricer.annuity(indexCDS, yieldCurve, intrinsicData.getCreditCurve(i), priceType, 0);
      }
    }
    a /= yieldCurve.getDiscountFactor(valuationTime);

    return a;
  }

  /**
   * The average spread of a CDS portfolio (index), defined as the weighted average of the
   * (implied) par spreads of the constituent names.
   * 
   * @see #intrinsicIndexSpread
   * @param indexCDS representation of the index cashflows (seen from today). 
   * @param yieldCurve The current yield curves 
   * @param intrinsicData credit curves, weights and recovery rates of the intrinsic names
   * @return The average spread 
   */
  public double averageSpread(
      CdsAnalytic indexCDS,
      IsdaCompliantYieldCurve yieldCurve,
      IntrinsicIndexDataBundle intrinsicData) {

    ArgChecker.notNull(indexCDS, "indexCDS");
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    ArgChecker.notNull(intrinsicData, "intrinsicData");
    if (intrinsicData.getNumOfDefaults() == intrinsicData.getIndexSize()) {
      throw new IllegalArgumentException("Every name in the index is defaulted - cannot calculate a spread");
    }

    CdsAnalytic cds = indexCDS.withRecoveryRate(0.0);
    int n = intrinsicData.getIndexSize();
    double sum = 0;
    for (int i = 0; i < n; i++) {
      if (!intrinsicData.isDefaulted(i)) {
        double protLeg = intrinsicData.getLGD(i) * _pricer.protectionLeg(cds, yieldCurve, intrinsicData.getCreditCurve(i));
        double annuity = _pricer.annuity(cds, yieldCurve, intrinsicData.getCreditCurve(i));
        double s = protLeg / annuity;
        sum += intrinsicData.getWeight(i) * s;
      }
    }
    sum /= intrinsicData.getIndexFactor();
    return sum;
  }

  /**
   * Imply a single (pseudo) credit curve for an index that will give the same index values
   * at a set of terms (supplied via pillarCDS) as the intrinsic value.
   * 
   * @param pillarCDS Point to build the curve 
   * @param indexCoupon The index coupon 
   * @param yieldCurve The current yield curves 
   * @param intrinsicData credit curves, weights and recovery rates of the intrinsic names
   * @return A (pseudo) credit curve for an index
   */
  public IsdaCompliantCreditCurve impliedIndexCurve(
      CdsAnalytic[] pillarCDS,
      double indexCoupon,
      IsdaCompliantYieldCurve yieldCurve,
      IntrinsicIndexDataBundle intrinsicData) {

    ArgChecker.noNulls(pillarCDS, "pillarCDS");
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    ArgChecker.notNull(intrinsicData, "intrinsicData");
    if (intrinsicData.getNumOfDefaults() == intrinsicData.getIndexSize()) {
      throw new IllegalArgumentException("Every name in the index is defaulted - cannot calculate implied index curve");
    }
    int n = pillarCDS.length;
    double[] puf = new double[n];
    double indexFactor = intrinsicData.getIndexFactor();
    for (int i = 0; i < n; i++) {
      // PUF are always given for full index
      puf[i] = indexPV(pillarCDS[i], indexCoupon, yieldCurve, intrinsicData) / indexFactor;
    }
    CreditCurveCalibrator calibrator = new CreditCurveCalibrator(pillarCDS, yieldCurve);
    double[] coupons = new double[n];
    Arrays.fill(coupons, indexCoupon);
    return calibrator.calibrate(coupons, puf);
  }

  //*******************************************************************************************************************
  //* Forward values adjusted for defaults 
  //****************************************************************************************************************

  /**
   * For a future expiry date, the default adjusted forward index value is the expected (full)
   * value of the index plus the cash settlement of any defaults before
   * the expiry date, valued on the (forward) cash settlement date (usually 3 working days after
   * the expiry date - i.e. the expiry settlement date). 
   * 
   * @param fwdStartingCDS A forward starting CDS to represent cash flows in the index.
   *  The stepin date should be one day after the expiry and the cashSettlement 
   *  date (usually) 3 working days after expiry.
   * @param timeToExpiry the time in years between the trade date and expiry.
   *  This should use the same DCC as the curves (ACT365F unless manually changed). 
   * @param yieldCurve The yield curve 
   * @param indexCoupon The coupon of the index 
   * @param intrinsicData credit curves, weights and recovery rates of the intrinsic names
   *  initially 100 entries, and the realised recovery rates are 0.2 and 0.35, the this value is (0.8 + 0.65)/100 )  
   * @return the default adjusted forward index value
   */
  public double defaultAdjustedForwardIndexValue(
      CdsAnalytic fwdStartingCDS,
      double timeToExpiry,
      IsdaCompliantYieldCurve yieldCurve,
      double indexCoupon,
      IntrinsicIndexDataBundle intrinsicData) {

    ArgChecker.notNull(fwdStartingCDS, "fwdCDS");
    ArgChecker.isTrue(timeToExpiry >= 0.0, "timeToExpiry given as {}", timeToExpiry);
    ArgChecker.isTrue(
        fwdStartingCDS.getEffectiveProtectionStart() >= timeToExpiry,
        "effective protection start of {} is less than time to expiry of {}. Must provide a forward starting CDS",
        fwdStartingCDS.getEffectiveProtectionStart(), timeToExpiry);

    //the expected value of the index (not including default settlement) at the expiry settlement date 
    double indexPV = indexPV(fwdStartingCDS, indexCoupon, yieldCurve, intrinsicData);
    double d = expectedDefaultSettlementValue(timeToExpiry, intrinsicData);
    return indexPV + d;
  }

  /**
   * For a future expiry date, the default adjusted forward index value is the expected (full)
   * value of the index plus the cash settlement of any defaults before
   * the expiry date, valued on the (forward) cash settlement date (usually 3 working days after
   * the expiry date - i.e. the expiry settlement date). 
   * This calculation assumes an homogeneous pool that can be described by a single index curve.
   * 
   * @param fwdStartingCDS A forward starting CDS to represent cash flows in the index.
   *  The stepin date should be one day after the expiry and the cashSettlement 
   *  date (usually) 3 working days after expiry. This must contain the index recovery rate.
   * @param timeToExpiry the time in years between the trade date and expiry.
   *  This should use the same DCC as the curves (ACT365F unless manually changed). 
   * @param yieldCurve The yield curve 
   * @param indexCoupon The coupon of the index 
   * @param indexCurve  Pseudo credit curve for the index.
   * @return the default adjusted forward index value
   */
  public double defaultAdjustedForwardIndexValue(
      CdsAnalytic fwdStartingCDS,
      double timeToExpiry,
      IsdaCompliantYieldCurve yieldCurve,
      double indexCoupon,
      IsdaCompliantCreditCurve indexCurve) {

    ArgChecker.notNull(fwdStartingCDS, "fwdCDS");
    ArgChecker.isTrue(timeToExpiry >= 0.0, "timeToExpiry given as {}", timeToExpiry);
    ArgChecker.isTrue(
        fwdStartingCDS.getEffectiveProtectionStart() >= timeToExpiry,
        "effective protection start of {} is less than time to expiry of {}. Must provide a forward starting CDS",
        fwdStartingCDS.getEffectiveProtectionStart(), timeToExpiry);

    double defSet = expectedDefaultSettlementValue(timeToExpiry, indexCurve, fwdStartingCDS.getLGD());
    return defSet + _pricer.pv(fwdStartingCDS, yieldCurve, indexCurve, indexCoupon);
  }

  /**
   * For a future expiry date, the default adjusted forward index value is the expected (full)
   * value of the index plus the cash settlement of any defaults before
   * the expiry date, valued on the (forward) cash settlement date (usually 3 working days after
   * the expiry date - i.e. the expiry settlement date). 
   * This calculation assumes an homogeneous pool that can be described by a single index curve.
   * 
   * @param fwdStartingCDS A forward starting CDS to represent cash flows in the index.
   *  The stepin date should be one day after the expiry and the cashSettlement 
   *  date (usually) 3 working days after expiry. This must contain the index recovery rate.
   * @param timeToExpiry the time in years between the trade date and expiry.
   *  This should use the same DCC as the curves (ACT365F unless manually changed). 
   * @param initialIndexSize The initial number of names in the index 
   * @param yieldCurve The yield curve 
   * @param indexCoupon The coupon of the index 
   * @param indexCurve  Pseudo credit curve for the index.
   * @param initialDefaultSettlement The (normalised) value of any defaults that have already
   *  occurred (e.g. if two defaults have occurred from an index with
   *  initially 100 entries, and the realised recovery rates are 0.2 and 0.35, the this value is (0.8 + 0.65)/100 )  
   * @param numDefaults The number of defaults that have already occurred 
   * @return the default adjusted forward index value
   */
  public double defaultAdjustedForwardIndexValue(
      CdsAnalytic fwdStartingCDS,
      double timeToExpiry,
      int initialIndexSize,
      IsdaCompliantYieldCurve yieldCurve,
      double indexCoupon,
      IsdaCompliantCreditCurve indexCurve,
      double initialDefaultSettlement,
      int numDefaults) {

    ArgChecker.notNull(fwdStartingCDS, "fwdCDS");
    ArgChecker.isTrue(timeToExpiry >= 0.0, "timeToExpiry given as {}", timeToExpiry);
    ArgChecker.isTrue(
        fwdStartingCDS.getEffectiveProtectionStart() >= timeToExpiry,
        "effective protection start of {} is less than time to expiry of {}. Must provide a forward starting CDS",
        fwdStartingCDS.getEffectiveProtectionStart(), timeToExpiry);

    double f = (initialIndexSize - numDefaults) / ((double) initialIndexSize);
    double defSet = expectedDefaultSettlementValue(initialIndexSize, timeToExpiry, indexCurve, fwdStartingCDS.getLGD(),
        initialDefaultSettlement, numDefaults);
    return defSet + f * _pricer.pv(fwdStartingCDS, yieldCurve, indexCurve, indexCoupon);
  }

  /**
   * The (default adjusted) intrinsic forward spread of an index.
   * This is defined as the ratio of expected value of the protection leg and default settlement to
   * the expected value of the annuity at expiry.
   * 
   * @param fwdStartingCDS  forward starting CDS to represent cash flows in the index.
   *  The stepin date should be one day after the expiry and the cashSettlement 
   *  date (usually) 3 working days after expiry the time in years between the trade date and expiry.
   *  This should use the same DCC as the curves (ACT365F unless manually changed). 
   * @param timeToExpiry the time in years between the trade date and expiry.
   *  This should use the same DCC as the curves (ACT365F unless manually changed). 
   * @param yieldCurve The yield curve 
   * @param intrinsicData credit curves, weights and recovery rates of the intrinsic names
   *  initially 100 entries, and the realised recovery rates are 0.2 and 0.35, the this value is (0.8 + 0.65)/100 )  
   * @return The (default adjusted) forward spread (as a fraction)
   */
  public double defaultAdjustedForwardSpread(
      CdsAnalytic fwdStartingCDS,
      double timeToExpiry,
      IsdaCompliantYieldCurve yieldCurve,
      IntrinsicIndexDataBundle intrinsicData) {

    ArgChecker.notNull(fwdStartingCDS, "fwdStartingCDS");
    ArgChecker.isTrue(timeToExpiry >= 0.0, "timeToExpiry given as {}", timeToExpiry);
    ArgChecker.isTrue(
        fwdStartingCDS.getEffectiveProtectionStart() >= timeToExpiry,
        "effective protection start of {} is less than time to expiry of {}. Must provide a forward starting CDS",
        fwdStartingCDS.getEffectiveProtectionStart(), timeToExpiry);

    // Note: these values are all calculated for payment on the (forward) cash settlement date
    // there is no point discounting to today 
    double protLeg = indexProtLeg(fwdStartingCDS, yieldCurve, intrinsicData);
    double defSettle = expectedDefaultSettlementValue(timeToExpiry, intrinsicData);
    double ann = indexAnnuity(fwdStartingCDS, yieldCurve, intrinsicData);
    return (protLeg + defSettle) / ann;
  }

  /**
   * The (default adjusted) intrinsic forward spread of an index <b>when no defaults have yet occurred</b>.
   * This is defined as the ratio of expected value of the 
   * protection leg and default settlement to the expected value of the annuity at expiry.
   * This calculation assumes an homogeneous pool that can be described by a single index curve.
   * 
  * @param fwdStartingCDS forward starting CDS to represent cash flows in the index.
  *  The stepin date should be one day after the expiry and the cashSettlement 
   * date (usually) 3 working days after expiry
   * @param timeToExpiry the time in years between the trade date and expiry.
   *  This should use the same DCC as the curves (ACT365F unless manually changed). 
   * @param yieldCurve The yield curve 
   * @param indexCurve Pseudo credit curve for the index.
   * @return The normalised expected default settlement value
   */
  public double defaultAdjustedForwardSpread(
      CdsAnalytic fwdStartingCDS,
      double timeToExpiry,
      IsdaCompliantYieldCurve yieldCurve,
      IsdaCompliantCreditCurve indexCurve) {

    ArgChecker.notNull(fwdStartingCDS, "fwdCDS");
    ArgChecker.isTrue(timeToExpiry >= 0.0, "timeToExpiry given as {}", timeToExpiry);
    ArgChecker.isTrue(
        fwdStartingCDS.getEffectiveProtectionStart() >= timeToExpiry,
        "effective protection start of {} is less than time to expiry of {}. Must provide a forward starting CDS",
        fwdStartingCDS.getEffectiveProtectionStart(), timeToExpiry);
    double defSettle = expectedDefaultSettlementValue(timeToExpiry, indexCurve, fwdStartingCDS.getLGD());
    double protLeg = _pricer.protectionLeg(fwdStartingCDS, yieldCurve, indexCurve);
    double ann = _pricer.annuity(fwdStartingCDS, yieldCurve, indexCurve);
    return (protLeg + defSettle) / ann;
  }

  /**
   * The (default adjusted) intrinsic forward spread of an index.
   * This is defined as the ratio of expected value of the protection leg and default settlement to
   * the expected value of the annuity at expiry.  This calculation assumes an homogeneous pool that
   * can be described by a single index curve.
   * 
   * @param fwdStartingCDS forward starting CDS to represent cash flows in the index.
   *  The stepin date should be one day after the expiry and the cashSettlement 
   *  date (usually) 3 working days after expiry
   * @param timeToExpiry the time in years between the trade date and expiry.
   *  This should use the same DCC as the curves (ACT365F unless manually changed). 
   * @param initialIndexSize The initial number of names in the index 
   * @param yieldCurve The yield curve 
   * @param indexCurve Pseudo credit curve for the index.
   * @param initialDefaultSettlement The (normalised) value of any defaults that have
   *  already occurred (e.g. if two defaults have occurred from an index with
   *  initially 100 entries, and the realised recovery rates are 0.2 and 0.35, the this value is (0.8 + 0.65)/100 )  
   * @param numDefaults The number of defaults that have already occurred 
   * @return The normalised expected default settlement value
   */
  public double defaultAdjustedForwardSpread(
      CdsAnalytic fwdStartingCDS,
      double timeToExpiry,
      int initialIndexSize,
      IsdaCompliantYieldCurve yieldCurve,
      IsdaCompliantCreditCurve indexCurve,
      double initialDefaultSettlement,
      int numDefaults) {

    ArgChecker.notNull(fwdStartingCDS, "fwdCDS");
    ArgChecker.isTrue(timeToExpiry >= 0.0, "timeToExpiry given as {}", timeToExpiry);
    ArgChecker.isTrue(
        fwdStartingCDS.getEffectiveProtectionStart() >= timeToExpiry,
        "effective protection start of {} is less than time to expiry of {}. Must provide a forward starting CDS",
        fwdStartingCDS.getEffectiveProtectionStart(), timeToExpiry);
    double f = (initialIndexSize - numDefaults) / ((double) initialIndexSize);
    double defSettle = expectedDefaultSettlementValue(initialIndexSize, timeToExpiry, indexCurve, fwdStartingCDS.getLGD(),
        initialDefaultSettlement, numDefaults);
    double protLeg = f * _pricer.protectionLeg(fwdStartingCDS, yieldCurve, indexCurve);
    double ann = f * _pricer.annuity(fwdStartingCDS, yieldCurve, indexCurve);
    return (protLeg + defSettle) / ann;
  }

  /**
   * The normalised expected default settlement value paid on the  exercise settlement date.
   * The actual default settlement is this multiplied by the (initial) index notional.
   * 
   * @param timeToExpiry Time to expiry 
   * @param intrinsicData credit curves, weights and recovery rates of the intrinsic names
   * @return The normalised expected default settlement value
   */
  public double expectedDefaultSettlementValue(
      double timeToExpiry,
      IntrinsicIndexDataBundle intrinsicData) {

    ArgChecker.notNull(intrinsicData, "intrinsicData");
    int indexSize = intrinsicData.getIndexSize();
    double d = 0.0; //computed the expected default settlement amount (paid on the  expiry settlement date)
    for (int i = 0; i < indexSize; i++) {
      double qBar = intrinsicData.isDefaulted(i) ? 1.0 : 1.0 - intrinsicData.getCreditCurve(i).getSurvivalProbability(
          timeToExpiry);
      d += intrinsicData.getWeight(i) * intrinsicData.getLGD(i) * qBar;
    }
    return d;
  }

  /**
    * The normalised expected default settlement value paid on the exercise settlement
    * date <b>when no defaults have yet occurred</b>.
    * The actual default settlement is this multiplied by the (initial) 
   * index notional. This calculation assumes an homogeneous pool that can be described by a single index curve.
   * 
   * @param timeToExpiry Time to expiry 
   * @param indexCurve Pseudo credit curve for the index.
   * @param lgd The index Loss Given Default (LGD)
   * @return  The normalised expected default settlement value
   */
  public double expectedDefaultSettlementValue(
      double timeToExpiry,
      IsdaCompliantCreditCurve indexCurve,
      double lgd) {

    ArgChecker.notNull(indexCurve, "indexCurve");
    ArgChecker.inRangeInclusive(lgd, 0, 1, "lgd");
    double q = indexCurve.getSurvivalProbability(timeToExpiry);
    double d = lgd * (1 - q);
    return d;
  }

  /**
   * The normalised expected default settlement value paid on the  exercise settlement date.
   * The actual default settlement is this multiplied by the (initial) 
   * index notional.   This calculation assumes an homogeneous pool that can be described by a single index curve.
   * @param initialIndexSize Initial index size 
   * @param timeToExpiry Time to expiry 
   * @param indexCurve Pseudo credit curve for the index.
   * @param lgd The index Loss Given Default (LGD)
   * @param initialDefaultSettlement  The (normalised) value of any defaults that have already occurred
   *  (e.g. if two defaults have occurred from an index with
   *  initially 100 entries, and the realised recovery rates are 0.2 and 0.35, the this value is (0.8 + 0.65)/100 )  
   * @param numDefaults The number of defaults that have already occurred 
   * @return The normalised expected default settlement value
   */
  public double expectedDefaultSettlementValue(
      int initialIndexSize,
      double timeToExpiry,
      IsdaCompliantCreditCurve indexCurve,
      double lgd,
      double initialDefaultSettlement,
      int numDefaults) {

    ArgChecker.isTrue(initialIndexSize > 1, "initialIndexSize is {}", initialIndexSize);
    ArgChecker.notNull(indexCurve, "indexCurve");
    ArgChecker.isTrue(numDefaults >= 0, "negative numDefaults");
    ArgChecker.isTrue(
        numDefaults <= initialIndexSize, "More defaults ({}) than size of index ({})", numDefaults, initialIndexSize);
    double defFrac = numDefaults / ((double) initialIndexSize);
    // this upper range is if all current defaults have zero recovery
    ArgChecker.inRangeInclusive(initialDefaultSettlement, 0, defFrac, "initialDefaultSettlement");
    ArgChecker.inRangeInclusive(lgd, 0, 1, "lgd");

    double q = indexCurve.getSurvivalProbability(timeToExpiry);
    double d = (1 - defFrac) * lgd * (1 - q) + initialDefaultSettlement;
    return d;
  }

  /**
   * The change in the intrinsic value of a CDS index when the yield curve is bumped by 1bps.
   * If the index is priced as a single name CDS, use {@link InterestRateSensitivityCalculator}.
   * 
   * @param indexCDS The CDS index
   * @param indexCoupon The index coupon
   * @param yieldCurve The yield curve
   * @param intrinsicData Credit curves, weights and recovery rates of the intrinsic names
   * @return parallel IR01
   */
  public double parallelIR01(
      CdsAnalytic indexCDS,
      double indexCoupon,
      IsdaCompliantYieldCurve yieldCurve,
      IntrinsicIndexDataBundle intrinsicData) {

    ArgChecker.notNull(indexCDS, "indexCDS");
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    ArgChecker.notNull(intrinsicData, "intrinsicData");
    double pv = indexPV(indexCDS, indexCoupon, yieldCurve, intrinsicData, CdsPriceType.DIRTY);
    int nKnots = yieldCurve.getNumberOfKnots();
    double[] rates = yieldCurve.getKnotZeroRates();
    for (int i = 0; i < nKnots; ++i) {
      rates[i] += ONE_BPS;
    }
    IsdaCompliantYieldCurve yieldCurveUp = yieldCurve.withRates(rates);
    double pvUp = indexPV(indexCDS, indexCoupon, yieldCurveUp, intrinsicData, CdsPriceType.DIRTY);
    return pvUp - pv;
  }

  /**
   * The change in the intrinsic value of a CDS index when zero rate at node points of the yield curve is bumped by 1bps.
   * If the index is priced as a single name CDS, use {@link InterestRateSensitivityCalculator}.
   * 
   * @param indexCDS The CDS index
   * @param indexCoupon The index coupon
   * @param yieldCurve The yield curve
   * @param intrinsicData Credit curves, weights and recovery rates of the intrinsic names
   * @return bucketed IR01
   */
  public double[] bucketedIR01(
      CdsAnalytic indexCDS,
      double indexCoupon,
      IsdaCompliantYieldCurve yieldCurve,
      IntrinsicIndexDataBundle intrinsicData) {

    ArgChecker.notNull(indexCDS, "indexCDS");
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    ArgChecker.notNull(intrinsicData, "intrinsicData");
    double basePV = indexPV(indexCDS, indexCoupon, yieldCurve, intrinsicData, CdsPriceType.DIRTY);
    int n = yieldCurve.getNumberOfKnots();
    double[] res = new double[n];
    for (int i = 0; i < n; ++i) {
      IsdaCompliantYieldCurve bumpedYieldCurve = yieldCurve.withRate(yieldCurve.getZeroRateAtIndex(i) + ONE_BPS, i);
      double bumpedPV = indexPV(indexCDS, indexCoupon, bumpedYieldCurve, intrinsicData, CdsPriceType.DIRTY);
      res[i] = bumpedPV - basePV;
    }
    return res;
  }

  /**
   * Sensitivity of the intrinsic value of a CDS index to intrinsic CDS recovery rates.
   * 
   * @param indexCDS The CDS index
   * @param indexCoupon The index coupon
   * @param yieldCurve The yield curve
   * @param intrinsicData Credit curves, weights and recovery rates of the intrinsic names
   * @return The sensitivity
   */
  public double[] recovery01(
      CdsAnalytic indexCDS,
      double indexCoupon,
      IsdaCompliantYieldCurve yieldCurve,
      IntrinsicIndexDataBundle intrinsicData) {

    ArgChecker.notNull(indexCDS, "indexCDS");
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    ArgChecker.notNull(intrinsicData, "intrinsicData");
    CdsAnalytic zeroRR = indexCDS.withRecoveryRate(0.0);
    int indexSize = intrinsicData.getIndexSize();
    double[] res = new double[indexSize];
    for (int i = 0; i < indexSize; ++i) {
      if (intrinsicData.isDefaulted(i)) {
        res[i] = 0.0;
      } else {
        res[i] = -_pricer.protectionLeg(zeroRR, yieldCurve, intrinsicData.getCreditCurve(i)) *
            intrinsicData.getWeight(i);
      }
    }
    return res;
  }

  /**
   * Values on per-name default
   * @param indexCDS The CDS index
   * @param indexCoupon The index coupon
   * @param yieldCurve The yield curve
   * @param intrinsicData Credit curves, weights and recovery rates of the intrinsic names
   * @return The jump to default
   */
  public double[] jumpToDefault(
      CdsAnalytic indexCDS,
      double indexCoupon,
      IsdaCompliantYieldCurve yieldCurve,
      IntrinsicIndexDataBundle intrinsicData) {

    ArgChecker.notNull(indexCDS, "indexCDS");
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    ArgChecker.notNull(intrinsicData, "intrinsicData");
    int indexSize = intrinsicData.getIndexSize();
    double[] res = new double[indexSize];
    for (int i = 0; i < indexSize; ++i) {
      if (intrinsicData.isDefaulted(i)) {
        res[i] = 0.0;
      } else {
        res[i] = decomposedValueOnDefault(indexCDS, indexCoupon, yieldCurve, intrinsicData, i);
      }
    }
    return res;
  }

  private double decomposedValueOnDefault(
      CdsAnalytic indexCDS,
      double indexCoupon,
      IsdaCompliantYieldCurve yieldCurve,
      IntrinsicIndexDataBundle intrinsicData,
      int singleName) {

    double weight = intrinsicData.getWeight(singleName);
    double protection = intrinsicData.getLGD(singleName);
    double singleNamePV = _pricer.pv(indexCDS, yieldCurve, intrinsicData.getCreditCurves()[singleName], indexCoupon);
    return weight * (protection - singleNamePV);
  }

}
