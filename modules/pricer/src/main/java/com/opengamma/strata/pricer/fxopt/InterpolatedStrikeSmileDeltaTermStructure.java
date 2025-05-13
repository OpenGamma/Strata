/*
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.FLAT;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.TIME_SQUARE;
import static java.util.stream.Collectors.toList;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableConstructor;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.market.curve.interpolator.BoundCurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.ParameterizedData;
import com.opengamma.strata.market.param.ParameterizedDataCombiner;

/**
 * An interpolated term structure of smiles as used in Forex market.
 * <p>
 * The term structure defined here is composed of smile descriptions at different times.
 * The data of each smile contains delta and volatility in {@link SmileDeltaParameters}. 
 * The delta values must be common to all of the smiles.
 * <p>
 * Time interpolation and extrapolation are used to obtain a smile for the objective time.
 * Strike interpolation and extrapolation are used in the expiry-strike space where the
 * delta values are converted to strikes using the Black formula.
 * <p>
 * The default for the time direction is time squire interpolation with flat extrapolation.
 * The default for the strike direction is linear interpolation with flat extrapolation.
 */
@BeanDefinition(builderScope = "private")
public final class InterpolatedStrikeSmileDeltaTermStructure
    implements SmileDeltaTermStructure, ParameterizedData, ImmutableBean, Serializable {

  /**
   * The relative shift used for calculating sensitivities of the volatility values to input parameters by the finite
   * difference approximation.
   */
  private static final double EPS = 1e-6;

  /**
   * The smile description at the different time to expiry. All item should have the same deltas.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final ImmutableList<SmileDeltaParameters> volatilityTerm;
  /**
   * The day count convention used for the expiry.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final DayCount dayCount;
  /**
   * The interpolator used in the time dimension.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveInterpolator timeInterpolator;
  /**
   * The left extrapolator used in the time dimension.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveExtrapolator timeExtrapolatorLeft;
  /**
   * The right extrapolator used in the time dimension.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveExtrapolator timeExtrapolatorRight;
  /**
   * The interpolator used in the strike dimension.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveInterpolator strikeInterpolator;
  /**
   * The left extrapolator used in the strike dimension.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveExtrapolator strikeExtrapolatorLeft;
  /**
   * The right extrapolator used in the strike dimension.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveExtrapolator strikeExtrapolatorRight;
  /**
   * A set of expiry times for the smile descriptions.
   * <p>
   * This set must be consistent with the expiry values in {@code volatilityTerm},
   * thus can be derived if {@code volatilityTerm} is the primary input.
   */
  private final transient DoubleArray expiries;  // derived
  /**
   * The parameter combiner.
   */
  private final transient ParameterizedDataCombiner paramCombiner;  // not a property

  //-------------------------------------------------------------------------
  /**
   * Obtains volatility term structure from a set of smile descriptions.
   * <p>
   * The time dimension will use 'TimeSquare' interpolation with flat extrapolation.
   * The strike dimension will use 'Linear' interpolation with flat extrapolation.
   * 
   * @param volatilityTerm  the volatility descriptions
   * @param dayCount  the day count used for the expiry year-fraction
   * @return the instance
   */
  public static InterpolatedStrikeSmileDeltaTermStructure of(
      List<SmileDeltaParameters> volatilityTerm,
      DayCount dayCount) {

    return of(volatilityTerm, dayCount, TIME_SQUARE, FLAT, FLAT, LINEAR, FLAT, FLAT);
  }

  /**
   * Obtains volatility term structure from a set of smile descriptions 
   * with strike interpolator and extrapolators specified.
   * <p>
   * The time dimension will use 'TimeSquare' interpolation with flat extrapolation.
   * 
   * @param volatilityTerm  the volatility descriptions
   * @param dayCount  the day count used for the expiry year-fraction
   * @param strikeInterpolator  interpolator used in the strike dimension
   * @param strikeExtrapolatorLeft  left extrapolator used in the strike dimension
   * @param strikeExtrapolatorRight  right extrapolator used in the strike dimension
   * @return the instance
   */
  public static InterpolatedStrikeSmileDeltaTermStructure of(
      List<SmileDeltaParameters> volatilityTerm,
      DayCount dayCount,
      CurveInterpolator strikeInterpolator,
      CurveExtrapolator strikeExtrapolatorLeft,
      CurveExtrapolator strikeExtrapolatorRight) {

    return of(
        volatilityTerm,
        dayCount,
        TIME_SQUARE,
        FLAT,
        FLAT,
        strikeInterpolator,
        strikeExtrapolatorLeft,
        strikeExtrapolatorRight);
  }

  /**
   * Obtains volatility term structure from a set of smile descriptions 
   * with interpolator and extrapolators fully specified.
   * 
   * @param volatilityTerm  the volatility descriptions
   * @param dayCount  the day count used for the expiry year-fraction
   * @param timeExtrapolatorLeft  left extrapolator used in the time dimension
   * @param timeInterpolator  interpolator used in the time dimension
   * @param timeExtrapolatorRight  right extrapolator used in the time dimension
   * @param strikeExtrapolatorLeft  left extrapolator used in the strike dimension
   * @param strikeInterpolator  interpolator used in the strike dimension
   * @param strikeExtrapolatorRight  right extrapolator used in the strike dimension
   * @return the instance
   * @deprecated Use variant with correct interpolator/extrapolator order
   */
  @Deprecated
  public static InterpolatedStrikeSmileDeltaTermStructure of(
      List<SmileDeltaParameters> volatilityTerm,
      DayCount dayCount,
      CurveExtrapolator timeExtrapolatorLeft,
      CurveInterpolator timeInterpolator,
      CurveExtrapolator timeExtrapolatorRight,
      CurveExtrapolator strikeExtrapolatorLeft,
      CurveInterpolator strikeInterpolator,
      CurveExtrapolator strikeExtrapolatorRight) {

    return of(
        volatilityTerm,
        dayCount,
        timeInterpolator,
        timeExtrapolatorLeft,
        timeExtrapolatorRight,
        strikeInterpolator,
        strikeExtrapolatorLeft,
        strikeExtrapolatorRight);
  }

  /**
   * Obtains volatility term structure from a set of smile descriptions 
   * with interpolator and extrapolators fully specified.
   * 
   * @param volatilityTerm  the volatility descriptions
   * @param dayCount  the day count used for the expiry year-fraction
   * @param timeInterpolator  interpolator used in the time dimension
   * @param timeExtrapolatorLeft  left extrapolator used in the time dimension
   * @param timeExtrapolatorRight  right extrapolator used in the time dimension
   * @param strikeInterpolator  interpolator used in the strike dimension
   * @param strikeExtrapolatorLeft  left extrapolator used in the strike dimension
   * @param strikeExtrapolatorRight  right extrapolator used in the strike dimension
   * @return the instance
   */
  public static InterpolatedStrikeSmileDeltaTermStructure of(
      List<SmileDeltaParameters> volatilityTerm,
      DayCount dayCount,
      CurveInterpolator timeInterpolator,
      CurveExtrapolator timeExtrapolatorLeft,
      CurveExtrapolator timeExtrapolatorRight,
      CurveInterpolator strikeInterpolator,
      CurveExtrapolator strikeExtrapolatorLeft,
      CurveExtrapolator strikeExtrapolatorRight) {

    ArgChecker.notEmpty(volatilityTerm, "volatilityTerm");
    ArgChecker.notNull(dayCount, "dayCount");
    int nSmiles = volatilityTerm.size();
    DoubleArray deltaBase = volatilityTerm.get(0).getDelta();
    for (int i = 1; i < nSmiles; ++i) {
      ArgChecker.isTrue(deltaBase.equals(volatilityTerm.get(i).getDelta()), "delta must be common to all smiles");
    }
    return new InterpolatedStrikeSmileDeltaTermStructure(
        volatilityTerm,
        dayCount,
        timeInterpolator,
        timeExtrapolatorLeft,
        timeExtrapolatorRight,
        strikeInterpolator,
        strikeExtrapolatorLeft,
        strikeExtrapolatorRight);
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains volatility term structure from expiry times, delta values and volatilities.
   * <p>
   * The market date consists of time to expiry, delta and volatility.
   * The delta must be positive and sorted in ascending order.
   * The range of delta is common to all time to expiry.
   * <p>
   * {@code volatility} should be {@code n * (2 * m + 1)}, where {@code n} is the length of {@code expiry}
   * and {@code m} is the length of {@code delta}.
   * <p>
   * The time dimension will use 'TimeSquare' interpolation with flat extrapolation.
   * The strike dimension will use 'Linear' interpolation with flat extrapolation.
   * 
   * @param expiries  the expiry times of individual volatility smiles
   * @param delta  the delta values
   * @param volatility  the volatilities
   * @param dayCount  the day count used for the expiry year-fraction
   * @return the instance
   */
  public static InterpolatedStrikeSmileDeltaTermStructure of(
      DoubleArray expiries,
      DoubleArray delta,
      DoubleMatrix volatility,
      DayCount dayCount) {

    return of(expiries, delta, volatility, dayCount, TIME_SQUARE, FLAT, FLAT, LINEAR, FLAT, FLAT);
  }

  /**
   * Obtains volatility term structure from expiry times, delta values and volatilities
   * with strike interpolator and extrapolators specified.
   * <p>
   * The market date consists of time to expiry, delta and volatility.
   * The delta must be positive and sorted in ascending order.
   * The range of delta is common to all time to expiry.
   * <p>
   * {@code volatility} should be {@code n * (2 * m + 1)}, where {@code n} is the length of {@code expiry}
   * and {@code m} is the length of {@code delta}.
   * <p>
   * The time dimension will use 'TimeSquare' interpolation with flat extrapolation.
   * 
   * @param expiries  the expiry times of individual volatility smiles
   * @param delta  the delta values
   * @param volatility  the volatilities
   * @param dayCount  the day count used for the expiry year-fraction
   * @param strikeInterpolator  interpolator used in the strike dimension
   * @param strikeExtrapolatorLeft  left extrapolator used in the strike dimension
   * @param strikeExtrapolatorRight  right extrapolator used in the strike dimension
   * @return the instance
   */
  public static InterpolatedStrikeSmileDeltaTermStructure of(
      DoubleArray expiries,
      DoubleArray delta,
      DoubleMatrix volatility,
      DayCount dayCount,
      CurveInterpolator strikeInterpolator,
      CurveExtrapolator strikeExtrapolatorLeft,
      CurveExtrapolator strikeExtrapolatorRight) {

    return of(
        expiries,
        delta,
        volatility,
        dayCount,
        TIME_SQUARE,
        FLAT,
        FLAT,
        strikeInterpolator,
        strikeExtrapolatorLeft,
        strikeExtrapolatorRight);
  }

  /**
   * Obtains volatility term structure from expiry times, delta values and volatilities 
   * with interpolator and extrapolators fully specified.
   * <p>
   * The market date consists of time to expiry, delta and volatility.
   * The delta must be positive and sorted in ascending order.
   * The range of delta is common to all time to expiry.
   * <p>
   * {@code volatility} should be {@code n * (2 * m + 1)}, where {@code n} is the length of {@code expiry}
   * and {@code m} is the length of {@code delta}.
   * 
   * @param expiries  the expiry times of individual volatility smiles
   * @param delta  the delta values
   * @param volatility  the volatilities
   * @param dayCount  the day count used for the expiry year-fraction
   * @param timeInterpolator  interpolator used in the time dimension
   * @param timeExtrapolatorLeft  left extrapolator used in the time dimension
   * @param timeExtrapolatorRight  right extrapolator used in the time dimension
   * @param strikeInterpolator  interpolator used in the strike dimension
   * @param strikeExtrapolatorLeft  left extrapolator used in the strike dimension
   * @param strikeExtrapolatorRight  right extrapolator used in the strike dimension
   * @return the instance
   */
  public static InterpolatedStrikeSmileDeltaTermStructure of(
      DoubleArray expiries,
      DoubleArray delta,
      DoubleMatrix volatility,
      DayCount dayCount,
      CurveInterpolator timeInterpolator,
      CurveExtrapolator timeExtrapolatorLeft,
      CurveExtrapolator timeExtrapolatorRight,
      CurveInterpolator strikeInterpolator,
      CurveExtrapolator strikeExtrapolatorLeft,
      CurveExtrapolator strikeExtrapolatorRight) {

    ArgChecker.notNull(delta, "delta");
    ArgChecker.notNull(volatility, "volatility");
    ArgChecker.notNull(expiries, "expiries");
    ArgChecker.notNull(dayCount, "dayCount");
    ArgChecker.isTrue(delta.size() > 0,
        "Need more than one volatility value to perform strike interpolation");
    int nbExp = expiries.size();
    ArgChecker.isTrue(volatility.rowCount() == nbExp,
        "Volatility array length {} should be equal to the number of expiries {}", volatility.rowCount(), nbExp);
    ArgChecker.isTrue(volatility.columnCount() == 2 * delta.size() + 1,
        "Volatility array {} should be equal to (2 * number of deltas) + 1, have {}",
        volatility.columnCount(), 2 * delta.size() + 1);
    ImmutableList.Builder<SmileDeltaParameters> vt = ImmutableList.builder();
    for (int loopexp = 0; loopexp < nbExp; loopexp++) {
      vt.add(SmileDeltaParameters.of(expiries.get(loopexp), delta, volatility.row(loopexp)));
    }
    return new InterpolatedStrikeSmileDeltaTermStructure(
        vt.build(),
        dayCount,
        timeInterpolator,
        timeExtrapolatorLeft,
        timeExtrapolatorRight,
        strikeInterpolator,
        strikeExtrapolatorLeft,
        strikeExtrapolatorRight,
        expiries);
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains volatility term structure from expiry times, delta values, ATM volatilities, risk reversal figures and
   * strangle figures.
   * <p>
   * The range of delta is common to all time to expiry.
   * {@code riskReversal} and {@code strangle} should be {@code n * m}, and the length of {@code atm} should {@code n}, 
   * where {@code n} is the length of {@code expiry} and {@code m} is the length of {@code delta}.
   * <p>
   * The time dimension will use 'TimeSquare' interpolation with flat extrapolation.
   * The strike dimension will use 'Linear' interpolation with flat extrapolation.
   * 
   * @param expiries  the expiry times of individual volatility smiles
   * @param delta  the delta values
   * @param atm  the ATM volatilities
   * @param riskReversal  the risk reversal figures
   * @param strangle  the strangle figures
   * @param dayCount  the day count used for the expiry year-fraction
   * @return the instance
   */
  public static InterpolatedStrikeSmileDeltaTermStructure of(
      DoubleArray expiries,
      DoubleArray delta,
      DoubleArray atm,
      DoubleMatrix riskReversal,
      DoubleMatrix strangle,
      DayCount dayCount) {

    return of(expiries, delta, atm, riskReversal, strangle, dayCount, TIME_SQUARE, FLAT, FLAT, LINEAR, FLAT, FLAT);
  }

  /**
   * Obtains volatility term structure from expiry times, delta values, ATM volatilities, risk reversal figures and
   * strangle figures with strike interpolator and extrapolators specified.
   * <p>
   * The range of delta is common to all time to expiry.
   * {@code riskReversal} and {@code strangle} should be {@code n * m}, and the length of {@code atm} should {@code n}, 
   * where {@code n} is the length of {@code expiry} and {@code m} is the length of {@code delta}.
   * <p>
   * The time dimension will use 'TimeSquare' interpolation with flat extrapolation.
   * 
   * @param expiries  the expiry times of individual volatility smiles
   * @param delta  the delta values
   * @param atm  the ATM volatilities
   * @param riskReversal  the risk reversal figures
   * @param strangle  the strangle figures
   * @param dayCount  the day count used for the expiry year-fraction
   * @param strikeInterpolator  interpolator used in the strike dimension
   * @param strikeExtrapolatorLeft  left extrapolator used in the strike dimension
   * @param strikeExtrapolatorRight  right extrapolator used in the strike dimension
   * @return the instance
   */
  public static InterpolatedStrikeSmileDeltaTermStructure of(
      DoubleArray expiries,
      DoubleArray delta,
      DoubleArray atm,
      DoubleMatrix riskReversal,
      DoubleMatrix strangle,
      DayCount dayCount,
      CurveInterpolator strikeInterpolator,
      CurveExtrapolator strikeExtrapolatorLeft,
      CurveExtrapolator strikeExtrapolatorRight) {

    return of(
        expiries,
        delta,
        atm,
        riskReversal,
        strangle,
        dayCount,
        TIME_SQUARE,
        FLAT,
        FLAT,
        strikeInterpolator,
        strikeExtrapolatorLeft,
        strikeExtrapolatorRight);
  }

  /**
   * Obtains volatility term structure from expiry times, delta values, ATM volatilities, risk reversal figures and
   * strangle figures with interpolator and extrapolators fully specified.
   * <p>
   * The range of delta is common to all time to expiry.
   * {@code riskReversal} and {@code strangle} should be {@code n * m}, and the length of {@code atm} should {@code n}, 
   * where {@code n} is the length of {@code expiry} and {@code m} is the length of {@code delta}.
   * 
   * @param expiries  the expiry times of individual volatility smiles
   * @param delta  the delta values
   * @param atm  the ATM volatilities
   * @param riskReversal  the risk reversal figures
   * @param strangle  the strangle figures
   * @param dayCount  the day count used for the expiry year-fraction
   * @param timeInterpolator  interpolator used in the time dimension
   * @param timeExtrapolatorLeft  left extrapolator used in the time dimension
   * @param timeExtrapolatorRight  right extrapolator used in the time dimension
   * @param strikeInterpolator  interpolator used in the strike dimension
   * @param strikeExtrapolatorLeft  left extrapolator used in the strike dimension
   * @param strikeExtrapolatorRight  right extrapolator used in the strike dimension
   * @return the instance
   */
  public static InterpolatedStrikeSmileDeltaTermStructure of(
      DoubleArray expiries,
      DoubleArray delta,
      DoubleArray atm,
      DoubleMatrix riskReversal,
      DoubleMatrix strangle,
      DayCount dayCount,
      CurveInterpolator timeInterpolator,
      CurveExtrapolator timeExtrapolatorLeft,
      CurveExtrapolator timeExtrapolatorRight,
      CurveInterpolator strikeInterpolator,
      CurveExtrapolator strikeExtrapolatorLeft,
      CurveExtrapolator strikeExtrapolatorRight) {

    ArgChecker.notNull(expiries, "expiries");
    ArgChecker.notNull(delta, "delta");
    ArgChecker.notNull(atm, "ATM");
    ArgChecker.notNull(riskReversal, "risk reversal");
    ArgChecker.notNull(strangle, "strangle");
    ArgChecker.notNull(dayCount, "dayCount");
    int nbExp = expiries.size();
    ArgChecker.isTrue(atm.size() == nbExp, "ATM length should be coherent with time to expiry length");
    ArgChecker.isTrue(riskReversal.rowCount() == nbExp,
        "Risk reversal length should be coherent with time to expiry length");
    ArgChecker.isTrue(strangle.rowCount() == nbExp, "Strangle length should be coherent with time to expiry length");
    ArgChecker.isTrue(riskReversal.columnCount() == delta.size(),
        "Risk reversal size should be coherent with time to delta length");
    ArgChecker.isTrue(strangle.columnCount() == delta.size(),
        "Strangle size should be coherent with time to delta length");
    ImmutableList.Builder<SmileDeltaParameters> vt = ImmutableList.builder();
    for (int loopexp = 0; loopexp < nbExp; loopexp++) {
      vt.add(SmileDeltaParameters.of(
          expiries.get(loopexp),
          atm.get(loopexp),
          delta,
          riskReversal.row(loopexp),
          strangle.row(loopexp)));
    }
    return new InterpolatedStrikeSmileDeltaTermStructure(
        vt.build(),
        dayCount,
        timeInterpolator,
        timeExtrapolatorLeft,
        timeExtrapolatorRight,
        strikeInterpolator,
        strikeExtrapolatorLeft,
        strikeExtrapolatorRight,
        expiries);
  }

  //-------------------------------------------------------------------------
  @ImmutableConstructor
  private InterpolatedStrikeSmileDeltaTermStructure(
      List<SmileDeltaParameters> volatilityTerm,
      DayCount dayCount,
      CurveInterpolator timeInterpolator,
      CurveExtrapolator timeExtrapolatorLeft,
      CurveExtrapolator timeExtrapolatorRight,
      CurveInterpolator strikeInterpolator,
      CurveExtrapolator strikeExtrapolatorLeft,
      CurveExtrapolator strikeExtrapolatorRight) {

    this(volatilityTerm,
        dayCount,
        timeInterpolator,
        timeExtrapolatorLeft,
        timeExtrapolatorRight,
        strikeInterpolator,
        strikeExtrapolatorLeft,
        strikeExtrapolatorRight,
        DoubleArray.copyOf(volatilityTerm.stream().map(vt -> vt.getExpiry()).collect(toList())));
  }

  private InterpolatedStrikeSmileDeltaTermStructure(
      List<SmileDeltaParameters> volatilityTerm,
      DayCount dayCount,
      CurveInterpolator timeInterpolator,
      CurveExtrapolator timeExtrapolatorLeft,
      CurveExtrapolator timeExtrapolatorRight,
      CurveInterpolator strikeInterpolator,
      CurveExtrapolator strikeExtrapolatorLeft,
      CurveExtrapolator strikeExtrapolatorRight,
      DoubleArray expiries) {

    JodaBeanUtils.notNull(volatilityTerm, "volatilityTerm");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    JodaBeanUtils.notNull(timeInterpolator, "timeInterpolator");
    JodaBeanUtils.notNull(timeExtrapolatorLeft, "timeExtrapolatorLeft");
    JodaBeanUtils.notNull(timeExtrapolatorRight, "timeExtrapolatorRight");
    JodaBeanUtils.notNull(strikeInterpolator, "strikeInterpolator");
    JodaBeanUtils.notNull(strikeExtrapolatorLeft, "strikeExtrapolatorLeft");
    JodaBeanUtils.notNull(strikeExtrapolatorRight, "strikeExtrapolatorRight");
    this.volatilityTerm = ImmutableList.copyOf(volatilityTerm);
    this.dayCount = dayCount;
    this.timeExtrapolatorLeft = timeExtrapolatorLeft;
    this.timeInterpolator = timeInterpolator;
    this.timeExtrapolatorRight = timeExtrapolatorRight;
    this.strikeExtrapolatorLeft = strikeExtrapolatorLeft;
    this.strikeInterpolator = strikeInterpolator;
    this.strikeExtrapolatorRight = strikeExtrapolatorRight;
    this.expiries = expiries;
    this.paramCombiner = ParameterizedDataCombiner.of(volatilityTerm);
  }

  private Object readResolve() {
    return new InterpolatedStrikeSmileDeltaTermStructure(
        volatilityTerm,
        dayCount,
        timeInterpolator,
        timeExtrapolatorLeft,
        timeExtrapolatorRight,
        strikeInterpolator,
        strikeExtrapolatorLeft,
        strikeExtrapolatorRight);
  }

  //-------------------------------------------------------------------------
  @Override
  public int getParameterCount() {
    return paramCombiner.getParameterCount();
  }

  @Override
  public double getParameter(int parameterIndex) {
    return paramCombiner.getParameter(parameterIndex);
  }

  @Override
  public ParameterMetadata getParameterMetadata(int parameterIndex) {
    return paramCombiner.getParameterMetadata(parameterIndex);
  }

  @Override
  public InterpolatedStrikeSmileDeltaTermStructure withParameter(int parameterIndex, double newValue) {
    List<SmileDeltaParameters> updated = paramCombiner.withParameter(SmileDeltaParameters.class, parameterIndex, newValue);
    return new InterpolatedStrikeSmileDeltaTermStructure(
        updated,
        dayCount,
        timeInterpolator,
        timeExtrapolatorLeft,
        timeExtrapolatorRight,
        strikeInterpolator,
        strikeExtrapolatorLeft,
        strikeExtrapolatorRight);
  }

  @Override
  public InterpolatedStrikeSmileDeltaTermStructure withPerturbation(ParameterPerturbation perturbation) {
    List<SmileDeltaParameters> updated = paramCombiner.withPerturbation(SmileDeltaParameters.class, perturbation);
    return new InterpolatedStrikeSmileDeltaTermStructure(
        updated,
        dayCount,
        timeInterpolator,
        timeExtrapolatorLeft,
        timeExtrapolatorRight,
        strikeInterpolator,
        strikeExtrapolatorLeft,
        strikeExtrapolatorRight);
  }

  //-------------------------------------------------------------------------
  @Override
  public DoubleArray getExpiries() {
    return expiries;
  }

  @Override
  public List<Optional<Tenor>> getExpiryTenors() {
    return volatilityTerm.stream()
        .map(smileDeltaParams -> smileDeltaParams.getExpiryTenor())
        .collect(toImmutableList());
  }

  //-------------------------------------------------------------------------
  @Override
  public double volatility(double time, double strike, double forward) {
    ArgChecker.isTrue(time >= 0, "Positive time");
    SmileDeltaParameters smile = smileForExpiry(time);
    DoubleArray strikes = smile.strike(forward);
    BoundCurveInterpolator bound = strikeInterpolator.bind(
        strikes, smile.getVolatility(), strikeExtrapolatorLeft, strikeExtrapolatorRight);
    return bound.interpolate(strike);
  }

  @Override
  public VolatilityAndBucketedSensitivities volatilityAndSensitivities(double time, double strike, double forward) {
    ArgChecker.isTrue(time >= 0, "Positive time");
    SmileDeltaParameters smile = smileForExpiry(time);
    DoubleArray strikes = smile.strike(forward);
    BoundCurveInterpolator bound = strikeInterpolator.bind(
        strikes, smile.getVolatility(), strikeExtrapolatorLeft, strikeExtrapolatorRight);
    double volatility = bound.interpolate(strike);
    DoubleArray smileVolatilityBar = bound.parameterSensitivity(strike);
    SmileAndBucketedSensitivities smileAndSensitivities = smileAndSensitivitiesForExpiry(time, smileVolatilityBar);
    return VolatilityAndBucketedSensitivities.of(volatility, smileAndSensitivities.getSensitivities());
  }

  @Override
  public ValueDerivatives partialFirstDerivatives(double expiry, double strike, double forward) {
    ArgChecker.isTrue(expiry >= 0, "Positive time");
    SmileDeltaParameters smile = smileForExpiry(expiry);
    DoubleArray strikes = smile.strike(forward);
    BoundCurveInterpolator volBound = strikeInterpolator.bind(
        strikes, smile.getVolatility(), strikeExtrapolatorLeft, strikeExtrapolatorRight);
    double vol = volBound.interpolate(strike);
    double dVoldStrike = volBound.firstDerivative(strike);

    DoubleArray dNodeStrikesdExpiryDirect = smile.impliedStrikesDerivativeToExpiry(forward);
    DoubleArray dNodeStrikesdNodeVols = smile.impliedStrikesDerivativeToSmileVols(forward);
    DoubleArray dNodeVolsdExpiry = smileVolsDerivativeToExpiry(expiry);
    DoubleArray dNodeStrikesdExpiry = dNodeStrikesdExpiryDirect.plus(dNodeStrikesdNodeVols.multipliedBy(dNodeVolsdExpiry));
    DoubleArray dVoldNodeStrikes = DoubleArray.of(
        strikes.size(),
        index -> dVoldNodeStrike(strikes, smile.getVolatility(), strike, index));
    DoubleArray dVoldSmileVols = volBound.parameterSensitivity(strike);
    double dVoldExpiry = dNodeStrikesdExpiry.multipliedBy(dVoldNodeStrikes).sum() +
        dVoldSmileVols.multipliedBy(dNodeVolsdExpiry).sum();
    double dVoldForward = smile.impliedStrikesDerivativeToForward(forward).multipliedBy(dVoldNodeStrikes).sum();

    return ValueDerivatives.of(vol, DoubleArray.of(dVoldExpiry, dVoldStrike, dVoldForward));
  }

  // calculates sensitivity of volatility value at {@code strike} to {@code index}-th element of {@code strikes}.
  private double dVoldNodeStrike(
      DoubleArray strikes,
      DoubleArray volatilities,
      double strike,
      int index) {

    DoubleArray upStrikes = strikes.with(index, strikes.get(index) * (1d + EPS));
    BoundCurveInterpolator boundUp = strikeInterpolator.bind(
        upStrikes,
        volatilities,
        strikeExtrapolatorLeft,
        strikeExtrapolatorRight);
    DoubleArray downStrikes = strikes.with(index, strikes.get(index) * (1d - EPS));
    BoundCurveInterpolator boundDown = strikeInterpolator.bind(
        downStrikes,
        volatilities,
        strikeExtrapolatorLeft,
        strikeExtrapolatorRight);
    return 0.5 * (boundUp.interpolate(strike) - boundDown.interpolate(strike)) / (strikes.get(index) * EPS);
  }

  //-------------------------------------------------------------------------
  @Override
  public SmileDeltaParameters smileForExpiry(double expiry) {
    int nbVol = getStrikeCount();
    int nbTime = getSmileCount();
    ArgChecker.isTrue(nbTime > 1, "Need more than one time value to perform interpolation");
    double[] volatilityT = new double[nbVol];
    for (int loopvol = 0; loopvol < nbVol; loopvol++) {
      double[] volDelta = new double[nbTime];
      for (int looptime = 0; looptime < nbTime; looptime++) {
        volDelta[looptime] = volatilityTerm.get(looptime).getVolatility().get(loopvol);
      }
      BoundCurveInterpolator bound = timeInterpolator.bind(
          getExpiries(), DoubleArray.ofUnsafe(volDelta), timeExtrapolatorLeft, timeExtrapolatorRight);
      volatilityT[loopvol] = bound.interpolate(expiry);
    }
    return SmileDeltaParameters.of(expiry, getDelta(), DoubleArray.ofUnsafe(volatilityT));
  }

  //-------------------------------------------------------------------------
  private DoubleArray smileVolsDerivativeToExpiry(double expiry) {
    int nbVol = getStrikeCount();
    int nbTime = getSmileCount();
    ArgChecker.isTrue(nbTime > 1, "Need more than one time value to perform interpolation");
    double[] derivatives = new double[nbVol];
    for (int loopvol = 0; loopvol < nbVol; loopvol++) {
      double[] volDelta = new double[nbTime];
      for (int looptime = 0; looptime < nbTime; looptime++) {
        volDelta[looptime] = volatilityTerm.get(looptime).getVolatility().get(loopvol);
      }
      BoundCurveInterpolator bound = timeInterpolator.bind(
          getExpiries(), DoubleArray.ofUnsafe(volDelta), timeExtrapolatorLeft, timeExtrapolatorRight);
      derivatives[loopvol] = bound.firstDerivative(expiry);
    }
    return DoubleArray.ofUnsafe(derivatives);
  }

  @Override
  public SmileAndBucketedSensitivities smileAndSensitivitiesForExpiry(
      double expiry,
      DoubleArray volatilityAtTimeSensitivity) {

    int nbVol = getStrikeCount();
    ArgChecker.isTrue(volatilityAtTimeSensitivity.size() == nbVol, "Sensitivity with incorrect size");
    ArgChecker.isTrue(nbVol > 1, "Need more than one volatility value to perform interpolation");
    int nbTime = getSmileCount();
    ArgChecker.isTrue(nbTime > 1, "Need more than one time value to perform interpolation");
    double[] volatilityT = new double[nbVol];
    double[][] volatilitySensitivity = new double[nbTime][nbVol];
    for (int loopvol = 0; loopvol < nbVol; loopvol++) {
      double[] volDelta = new double[nbTime];
      for (int looptime = 0; looptime < nbTime; looptime++) {
        volDelta[looptime] = volatilityTerm.get(looptime).getVolatility().get(loopvol);
      }
      BoundCurveInterpolator bound = timeInterpolator.bind(
          getExpiries(), DoubleArray.ofUnsafe(volDelta), timeExtrapolatorLeft, timeExtrapolatorRight);
      DoubleArray volatilitySensitivityVol = bound.parameterSensitivity(expiry);
      for (int looptime = 0; looptime < nbTime; looptime++) {
        volatilitySensitivity[looptime][loopvol] =
            volatilitySensitivityVol.get(looptime) * volatilityAtTimeSensitivity.get(loopvol);
      }
      volatilityT[loopvol] = bound.interpolate(expiry);
    }
    SmileDeltaParameters smile = SmileDeltaParameters.of(expiry, getDelta(), DoubleArray.ofUnsafe(volatilityT));
    return SmileAndBucketedSensitivities.of(smile, DoubleMatrix.ofUnsafe(volatilitySensitivity));
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code InterpolatedStrikeSmileDeltaTermStructure}.
   * @return the meta-bean, not null
   */
  public static InterpolatedStrikeSmileDeltaTermStructure.Meta meta() {
    return InterpolatedStrikeSmileDeltaTermStructure.Meta.INSTANCE;
  }

  static {
    MetaBean.register(InterpolatedStrikeSmileDeltaTermStructure.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public InterpolatedStrikeSmileDeltaTermStructure.Meta metaBean() {
    return InterpolatedStrikeSmileDeltaTermStructure.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the smile description at the different time to expiry. All item should have the same deltas.
   * @return the value of the property, not null
   */
  @Override
  public ImmutableList<SmileDeltaParameters> getVolatilityTerm() {
    return volatilityTerm;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count convention used for the expiry.
   * @return the value of the property, not null
   */
  @Override
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the interpolator used in the time dimension.
   * @return the value of the property, not null
   */
  public CurveInterpolator getTimeInterpolator() {
    return timeInterpolator;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the left extrapolator used in the time dimension.
   * @return the value of the property, not null
   */
  public CurveExtrapolator getTimeExtrapolatorLeft() {
    return timeExtrapolatorLeft;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the right extrapolator used in the time dimension.
   * @return the value of the property, not null
   */
  public CurveExtrapolator getTimeExtrapolatorRight() {
    return timeExtrapolatorRight;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the interpolator used in the strike dimension.
   * @return the value of the property, not null
   */
  public CurveInterpolator getStrikeInterpolator() {
    return strikeInterpolator;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the left extrapolator used in the strike dimension.
   * @return the value of the property, not null
   */
  public CurveExtrapolator getStrikeExtrapolatorLeft() {
    return strikeExtrapolatorLeft;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the right extrapolator used in the strike dimension.
   * @return the value of the property, not null
   */
  public CurveExtrapolator getStrikeExtrapolatorRight() {
    return strikeExtrapolatorRight;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      InterpolatedStrikeSmileDeltaTermStructure other = (InterpolatedStrikeSmileDeltaTermStructure) obj;
      return JodaBeanUtils.equal(volatilityTerm, other.volatilityTerm) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(timeInterpolator, other.timeInterpolator) &&
          JodaBeanUtils.equal(timeExtrapolatorLeft, other.timeExtrapolatorLeft) &&
          JodaBeanUtils.equal(timeExtrapolatorRight, other.timeExtrapolatorRight) &&
          JodaBeanUtils.equal(strikeInterpolator, other.strikeInterpolator) &&
          JodaBeanUtils.equal(strikeExtrapolatorLeft, other.strikeExtrapolatorLeft) &&
          JodaBeanUtils.equal(strikeExtrapolatorRight, other.strikeExtrapolatorRight);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(volatilityTerm);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(timeInterpolator);
    hash = hash * 31 + JodaBeanUtils.hashCode(timeExtrapolatorLeft);
    hash = hash * 31 + JodaBeanUtils.hashCode(timeExtrapolatorRight);
    hash = hash * 31 + JodaBeanUtils.hashCode(strikeInterpolator);
    hash = hash * 31 + JodaBeanUtils.hashCode(strikeExtrapolatorLeft);
    hash = hash * 31 + JodaBeanUtils.hashCode(strikeExtrapolatorRight);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(288);
    buf.append("InterpolatedStrikeSmileDeltaTermStructure{");
    buf.append("volatilityTerm").append('=').append(JodaBeanUtils.toString(volatilityTerm)).append(',').append(' ');
    buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
    buf.append("timeInterpolator").append('=').append(JodaBeanUtils.toString(timeInterpolator)).append(',').append(' ');
    buf.append("timeExtrapolatorLeft").append('=').append(JodaBeanUtils.toString(timeExtrapolatorLeft)).append(',').append(' ');
    buf.append("timeExtrapolatorRight").append('=').append(JodaBeanUtils.toString(timeExtrapolatorRight)).append(',').append(' ');
    buf.append("strikeInterpolator").append('=').append(JodaBeanUtils.toString(strikeInterpolator)).append(',').append(' ');
    buf.append("strikeExtrapolatorLeft").append('=').append(JodaBeanUtils.toString(strikeExtrapolatorLeft)).append(',').append(' ');
    buf.append("strikeExtrapolatorRight").append('=').append(JodaBeanUtils.toString(strikeExtrapolatorRight));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code InterpolatedStrikeSmileDeltaTermStructure}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code volatilityTerm} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<SmileDeltaParameters>> volatilityTerm = DirectMetaProperty.ofImmutable(
        this, "volatilityTerm", InterpolatedStrikeSmileDeltaTermStructure.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", InterpolatedStrikeSmileDeltaTermStructure.class, DayCount.class);
    /**
     * The meta-property for the {@code timeInterpolator} property.
     */
    private final MetaProperty<CurveInterpolator> timeInterpolator = DirectMetaProperty.ofImmutable(
        this, "timeInterpolator", InterpolatedStrikeSmileDeltaTermStructure.class, CurveInterpolator.class);
    /**
     * The meta-property for the {@code timeExtrapolatorLeft} property.
     */
    private final MetaProperty<CurveExtrapolator> timeExtrapolatorLeft = DirectMetaProperty.ofImmutable(
        this, "timeExtrapolatorLeft", InterpolatedStrikeSmileDeltaTermStructure.class, CurveExtrapolator.class);
    /**
     * The meta-property for the {@code timeExtrapolatorRight} property.
     */
    private final MetaProperty<CurveExtrapolator> timeExtrapolatorRight = DirectMetaProperty.ofImmutable(
        this, "timeExtrapolatorRight", InterpolatedStrikeSmileDeltaTermStructure.class, CurveExtrapolator.class);
    /**
     * The meta-property for the {@code strikeInterpolator} property.
     */
    private final MetaProperty<CurveInterpolator> strikeInterpolator = DirectMetaProperty.ofImmutable(
        this, "strikeInterpolator", InterpolatedStrikeSmileDeltaTermStructure.class, CurveInterpolator.class);
    /**
     * The meta-property for the {@code strikeExtrapolatorLeft} property.
     */
    private final MetaProperty<CurveExtrapolator> strikeExtrapolatorLeft = DirectMetaProperty.ofImmutable(
        this, "strikeExtrapolatorLeft", InterpolatedStrikeSmileDeltaTermStructure.class, CurveExtrapolator.class);
    /**
     * The meta-property for the {@code strikeExtrapolatorRight} property.
     */
    private final MetaProperty<CurveExtrapolator> strikeExtrapolatorRight = DirectMetaProperty.ofImmutable(
        this, "strikeExtrapolatorRight", InterpolatedStrikeSmileDeltaTermStructure.class, CurveExtrapolator.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "volatilityTerm",
        "dayCount",
        "timeInterpolator",
        "timeExtrapolatorLeft",
        "timeExtrapolatorRight",
        "strikeInterpolator",
        "strikeExtrapolatorLeft",
        "strikeExtrapolatorRight");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 70074929:  // volatilityTerm
          return volatilityTerm;
        case 1905311443:  // dayCount
          return dayCount;
        case -587914188:  // timeInterpolator
          return timeInterpolator;
        case -286652761:  // timeExtrapolatorLeft
          return timeExtrapolatorLeft;
        case -290640004:  // timeExtrapolatorRight
          return timeExtrapolatorRight;
        case 815202713:  // strikeInterpolator
          return strikeInterpolator;
        case -1176196724:  // strikeExtrapolatorLeft
          return strikeExtrapolatorLeft;
        case -2096699081:  // strikeExtrapolatorRight
          return strikeExtrapolatorRight;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends InterpolatedStrikeSmileDeltaTermStructure> builder() {
      return new InterpolatedStrikeSmileDeltaTermStructure.Builder();
    }

    @Override
    public Class<? extends InterpolatedStrikeSmileDeltaTermStructure> beanType() {
      return InterpolatedStrikeSmileDeltaTermStructure.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code volatilityTerm} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<SmileDeltaParameters>> volatilityTerm() {
      return volatilityTerm;
    }

    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    /**
     * The meta-property for the {@code timeInterpolator} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveInterpolator> timeInterpolator() {
      return timeInterpolator;
    }

    /**
     * The meta-property for the {@code timeExtrapolatorLeft} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveExtrapolator> timeExtrapolatorLeft() {
      return timeExtrapolatorLeft;
    }

    /**
     * The meta-property for the {@code timeExtrapolatorRight} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveExtrapolator> timeExtrapolatorRight() {
      return timeExtrapolatorRight;
    }

    /**
     * The meta-property for the {@code strikeInterpolator} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveInterpolator> strikeInterpolator() {
      return strikeInterpolator;
    }

    /**
     * The meta-property for the {@code strikeExtrapolatorLeft} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveExtrapolator> strikeExtrapolatorLeft() {
      return strikeExtrapolatorLeft;
    }

    /**
     * The meta-property for the {@code strikeExtrapolatorRight} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveExtrapolator> strikeExtrapolatorRight() {
      return strikeExtrapolatorRight;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 70074929:  // volatilityTerm
          return ((InterpolatedStrikeSmileDeltaTermStructure) bean).getVolatilityTerm();
        case 1905311443:  // dayCount
          return ((InterpolatedStrikeSmileDeltaTermStructure) bean).getDayCount();
        case -587914188:  // timeInterpolator
          return ((InterpolatedStrikeSmileDeltaTermStructure) bean).getTimeInterpolator();
        case -286652761:  // timeExtrapolatorLeft
          return ((InterpolatedStrikeSmileDeltaTermStructure) bean).getTimeExtrapolatorLeft();
        case -290640004:  // timeExtrapolatorRight
          return ((InterpolatedStrikeSmileDeltaTermStructure) bean).getTimeExtrapolatorRight();
        case 815202713:  // strikeInterpolator
          return ((InterpolatedStrikeSmileDeltaTermStructure) bean).getStrikeInterpolator();
        case -1176196724:  // strikeExtrapolatorLeft
          return ((InterpolatedStrikeSmileDeltaTermStructure) bean).getStrikeExtrapolatorLeft();
        case -2096699081:  // strikeExtrapolatorRight
          return ((InterpolatedStrikeSmileDeltaTermStructure) bean).getStrikeExtrapolatorRight();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      metaProperty(propertyName);
      if (quiet) {
        return;
      }
      throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
    }

  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code InterpolatedStrikeSmileDeltaTermStructure}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<InterpolatedStrikeSmileDeltaTermStructure> {

    private List<SmileDeltaParameters> volatilityTerm = ImmutableList.of();
    private DayCount dayCount;
    private CurveInterpolator timeInterpolator;
    private CurveExtrapolator timeExtrapolatorLeft;
    private CurveExtrapolator timeExtrapolatorRight;
    private CurveInterpolator strikeInterpolator;
    private CurveExtrapolator strikeExtrapolatorLeft;
    private CurveExtrapolator strikeExtrapolatorRight;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 70074929:  // volatilityTerm
          return volatilityTerm;
        case 1905311443:  // dayCount
          return dayCount;
        case -587914188:  // timeInterpolator
          return timeInterpolator;
        case -286652761:  // timeExtrapolatorLeft
          return timeExtrapolatorLeft;
        case -290640004:  // timeExtrapolatorRight
          return timeExtrapolatorRight;
        case 815202713:  // strikeInterpolator
          return strikeInterpolator;
        case -1176196724:  // strikeExtrapolatorLeft
          return strikeExtrapolatorLeft;
        case -2096699081:  // strikeExtrapolatorRight
          return strikeExtrapolatorRight;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 70074929:  // volatilityTerm
          this.volatilityTerm = (List<SmileDeltaParameters>) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        case -587914188:  // timeInterpolator
          this.timeInterpolator = (CurveInterpolator) newValue;
          break;
        case -286652761:  // timeExtrapolatorLeft
          this.timeExtrapolatorLeft = (CurveExtrapolator) newValue;
          break;
        case -290640004:  // timeExtrapolatorRight
          this.timeExtrapolatorRight = (CurveExtrapolator) newValue;
          break;
        case 815202713:  // strikeInterpolator
          this.strikeInterpolator = (CurveInterpolator) newValue;
          break;
        case -1176196724:  // strikeExtrapolatorLeft
          this.strikeExtrapolatorLeft = (CurveExtrapolator) newValue;
          break;
        case -2096699081:  // strikeExtrapolatorRight
          this.strikeExtrapolatorRight = (CurveExtrapolator) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public InterpolatedStrikeSmileDeltaTermStructure build() {
      return new InterpolatedStrikeSmileDeltaTermStructure(
          volatilityTerm,
          dayCount,
          timeInterpolator,
          timeExtrapolatorLeft,
          timeExtrapolatorRight,
          strikeInterpolator,
          strikeExtrapolatorLeft,
          strikeExtrapolatorRight);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(288);
      buf.append("InterpolatedStrikeSmileDeltaTermStructure.Builder{");
      buf.append("volatilityTerm").append('=').append(JodaBeanUtils.toString(volatilityTerm)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("timeInterpolator").append('=').append(JodaBeanUtils.toString(timeInterpolator)).append(',').append(' ');
      buf.append("timeExtrapolatorLeft").append('=').append(JodaBeanUtils.toString(timeExtrapolatorLeft)).append(',').append(' ');
      buf.append("timeExtrapolatorRight").append('=').append(JodaBeanUtils.toString(timeExtrapolatorRight)).append(',').append(' ');
      buf.append("strikeInterpolator").append('=').append(JodaBeanUtils.toString(strikeInterpolator)).append(',').append(' ');
      buf.append("strikeExtrapolatorLeft").append('=').append(JodaBeanUtils.toString(strikeExtrapolatorLeft)).append(',').append(' ');
      buf.append("strikeExtrapolatorRight").append('=').append(JodaBeanUtils.toString(strikeExtrapolatorRight));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
