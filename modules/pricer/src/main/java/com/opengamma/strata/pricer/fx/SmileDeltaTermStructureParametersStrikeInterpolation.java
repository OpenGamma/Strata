/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;


import static com.opengamma.strata.market.interpolator.CurveExtrapolators.FLAT;
import static com.opengamma.strata.market.interpolator.CurveInterpolators.LINEAR;
import static com.opengamma.strata.market.interpolator.CurveInterpolators.TIME_SQUARE;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.market.interpolator.BoundCurveInterpolator;
import com.opengamma.strata.market.interpolator.CurveExtrapolator;
import com.opengamma.strata.market.interpolator.CurveInterpolator;

/**
 * A term structure of smiles as used in Forex market.
 * <p>
 * The term structure defined here is composed of smile descriptions at different times. 
 * The data of each smile contains delta and volatility in {@link SmileDeltaParameters}. 
 * The delta values must be common to all of the smiles. 
 * <p>
 * Time interpolation and extrapolation are used to obtain a smile for the objective time.
 * Strike interpolation and extrapolation are used in the expiry-strike space where the delta values are converted to 
 * strikes using Black formula.
 * <p>
 * The default for the time direction is time squire interpolation with flat extrapolation.
 * The default for the strike direction is linear interpolation with flat extrapolation.
 */
@BeanDefinition(builderScope = "private")
public final class SmileDeltaTermStructureParametersStrikeInterpolation
    implements SmileDeltaTermStructureParameters, ImmutableBean, Serializable {

  /**
   * The name of the smile term structure.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final String name;
  /**
   * The smile description at the different time to expiry. All item should have the same deltas.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final ImmutableList<SmileDeltaParameters> volatilityTerm;
  /**
   * The left extrapolator used in the time dimension.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final CurveExtrapolator timeLeftExtrapolator;
  /**
   * The interpolator used in the time dimension.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final CurveInterpolator timeInterpolator;
  /**
   * The right extrapolator used in the time dimension.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final CurveExtrapolator timeRightExtrapolator;
  /**
   * The left extrapolator used in the strike dimension.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveExtrapolator strikeLeftExtrapolator;
  /**
   * The interpolator used in the strike dimension.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveInterpolator strikeInterpolator;
  /**
   * The right extrapolator used in the strike dimension.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveExtrapolator strikeRightExtrapolator;
  /**
   * A set of expiry times for the smile descriptions. 
   * <p>
   * This set must be consistent with time to expiry in {@code volatilityTerm}, thus can be derived if 
   * {@code volatilityTerm} is the primary input.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final DoubleArray timeToExpiry;

  //-------------------------------------------------------------------------
  /**
   * Obtains volatility term structure from a set of smile descriptions.
   * 
   * @param name  the name
   * @param volatilityTerm  the volatility descriptions
   * @return the instance
   */
  public static SmileDeltaTermStructureParametersStrikeInterpolation of(
      String name,
      List<SmileDeltaParameters> volatilityTerm) {

    return of(name, volatilityTerm, FLAT, TIME_SQUARE, FLAT, FLAT, LINEAR, FLAT);
  }

  /**
   * Obtains volatility term structure from a set of smile descriptions 
   * with strike interpolator and extrapolators specified.
   * 
   * @param name  the name
   * @param volatilityTerm  the volatility descriptions
   * @param strikeLeftExtrapolator  left extrapolator used in the strike dimension
   * @param strikeInterpolator  interpolator used in the strike dimension
   * @param strikeRightExtrapolator  right extrapolator used in the strike dimension
   * @return the instance
   */
  public static SmileDeltaTermStructureParametersStrikeInterpolation of(
      String name,
      List<SmileDeltaParameters> volatilityTerm,
      CurveExtrapolator strikeLeftExtrapolator,
      CurveInterpolator strikeInterpolator,
      CurveExtrapolator strikeRightExtrapolator) {
    
    return of(
        name,
        volatilityTerm,
        FLAT,
        TIME_SQUARE,
        FLAT,
        strikeLeftExtrapolator,
        strikeInterpolator,
        strikeRightExtrapolator);
  }

  /**
   * Obtains volatility term structure from a set of smile descriptions 
   * with interpolator and extrapolators fully specified.
   * 
   * @param name  the name
   * @param volatilityTerm  the volatility descriptions
   * @param timeLeftExtrapolator  left extrapolator used in the time dimension
   * @param timeInterpolator  interpolator used in the time dimension
   * @param timeRightExtrapolator  right extrapolator used in the time dimension
   * @param strikeLeftExtrapolator  left extrapolator used in the strike dimension
   * @param strikeInterpolator  interpolator used in the strike dimension
   * @param strikeRightExtrapolator  right extrapolator used in the strike dimension
   * @return the instance
   */
  public static SmileDeltaTermStructureParametersStrikeInterpolation of(
      String name,
      List<SmileDeltaParameters> volatilityTerm,
      CurveExtrapolator timeLeftExtrapolator,
      CurveInterpolator timeInterpolator,
      CurveExtrapolator timeRightExtrapolator,
      CurveExtrapolator strikeLeftExtrapolator,
      CurveInterpolator strikeInterpolator,
      CurveExtrapolator strikeRightExtrapolator) {

    ArgChecker.notEmpty(volatilityTerm, "volatilityTerm");
    int nSmiles = volatilityTerm.size();
    DoubleArray deltaBase = volatilityTerm.get(0).getDelta();
    for (int i = 1; i < nSmiles; ++i) {
      ArgChecker.isTrue(deltaBase.equals(volatilityTerm.get(i).getDelta()), "delta must be common to all smiles");
    }
    DoubleArray timeToExpiry =
        DoubleArray.copyOf(volatilityTerm.stream().map(vt -> vt.getTimeToExpiry()).collect(Collectors.toList()));
    return new SmileDeltaTermStructureParametersStrikeInterpolation(
        name,
        volatilityTerm,
        timeLeftExtrapolator,
        timeInterpolator,
        timeRightExtrapolator,
        strikeLeftExtrapolator,
        strikeInterpolator,
        strikeRightExtrapolator,
        timeToExpiry);
  }

  /**
   * Obtains volatility term structure from expiry times, delta values and volatilities. 
   * <p>
   * The market date consists of time to expiry, delta and volatility.
   * The delta must be positive and sorted in ascending order.
   * The range of delta is common to all time to expiry.
   * <p>
   * {@code volatility} should be {@code n * (2 * m + 1)}, where {@code n} is the length of {@code timeToExpiry}
   * and {@code m} is the length of {@code delta}.
   * 
   * @param name  the name
   * @param timeToExpiry  the expiry times of individual volatility smiles
   * @param delta  the delta values
   * @param volatility  the volatilities
   * @return the instance
   */
  public static SmileDeltaTermStructureParametersStrikeInterpolation of(
      String name,
      DoubleArray timeToExpiry,
      DoubleArray delta,
      DoubleMatrix volatility) {

    return of(name, timeToExpiry, delta, volatility, FLAT, TIME_SQUARE, FLAT, FLAT, LINEAR, FLAT);
  }

  /**
   * Obtains volatility term structure from expiry times, delta values and volatilities
   * with strike interpolator and extrapolators specified.
   * <p>
   * The market date consists of time to expiry, delta and volatility.
   * The delta must be positive and sorted in ascending order.
   * The range of delta is common to all time to expiry.
   * <p>
   * {@code volatility} should be {@code n * (2 * m + 1)}, where {@code n} is the length of {@code timeToExpiry}
   * and {@code m} is the length of {@code delta}.
   * 
   * @param name  the name
   * @param timeToExpiry  the expiry times of individual volatility smiles
   * @param delta  the delta values
   * @param volatility  the volatilities
   * @param strikeLeftExtrapolator  left extrapolator used in the strike dimension
   * @param strikeInterpolator  interpolator used in the strike dimension
   * @param strikeRightExtrapolator  right extrapolator used in the strike dimension
   * @return the instance
   */
  public static SmileDeltaTermStructureParametersStrikeInterpolation of(
      String name,
      DoubleArray timeToExpiry,
      DoubleArray delta,
      DoubleMatrix volatility,
      CurveExtrapolator strikeLeftExtrapolator,
      CurveInterpolator strikeInterpolator,
      CurveExtrapolator strikeRightExtrapolator) {

    return of(
        name,
        timeToExpiry,
        delta,
        volatility,
        FLAT,
        TIME_SQUARE,
        FLAT,
        strikeLeftExtrapolator,
        strikeInterpolator,
        strikeRightExtrapolator);
  }

  /**
   * Obtains volatility term structure from expiry times, delta values and volatilities 
   * with interpolator and extrapolators fully specified.
   * <p>
   * The market date consists of time to expiry, delta and volatility.
   * The delta must be positive and sorted in ascending order.
   * The range of delta is common to all time to expiry.
   * <p>
   * {@code volatility} should be {@code n * (2 * m + 1)}, where {@code n} is the length of {@code timeToExpiry}
   * and {@code m} is the length of {@code delta}.
   * 
   * @param name  the name
   * @param timeToExpiry  the expiry times of individual volatility smiles
   * @param delta  the delta values
   * @param volatility  the volatilities
   * @param timeLeftExtrapolator  left extrapolator used in the time dimension
   * @param timeInterpolator  interpolator used in the time dimension
   * @param timeRightExtrapolator  right extrapolator used in the time dimension
   * @param strikeLeftExtrapolator  left extrapolator used in the strike dimension
   * @param strikeInterpolator  interpolator used in the strike dimension
   * @param strikeRightExtrapolator  right extrapolator used in the strike dimension
   * @return the instance
   */
  public static SmileDeltaTermStructureParametersStrikeInterpolation of(
      String name,
      DoubleArray timeToExpiry,
      DoubleArray delta,
      DoubleMatrix volatility,
      CurveExtrapolator timeLeftExtrapolator,
      CurveInterpolator timeInterpolator,
      CurveExtrapolator timeRightExtrapolator,
      CurveExtrapolator strikeLeftExtrapolator,
      CurveInterpolator strikeInterpolator,
      CurveExtrapolator strikeRightExtrapolator) {

    ArgChecker.notNull(timeToExpiry, "time to expiry");
    ArgChecker.notNull(delta, "delta");
    ArgChecker.notNull(volatility, "volatility");
    ArgChecker.isTrue(delta.size() > 0,
        "Need more than one volatility value to perform strike interpolation");
    int nbExp = timeToExpiry.size();
    ArgChecker.isTrue(volatility.rowCount() == nbExp,
        "Volatility array length {} should be equal to the number of expiries {}", volatility.rowCount(), nbExp);
    ArgChecker.isTrue(volatility.columnCount() == 2 * delta.size() + 1,
        "Volatility array {} should be equal to (2 * number of deltas) + 1, have {}",
        volatility.columnCount(), 2 * delta.size() + 1);
    SmileDeltaParameters[] vt = new SmileDeltaParameters[nbExp];
    for (int loopexp = 0; loopexp < nbExp; loopexp++) {
      vt[loopexp] = SmileDeltaParameters.of(
          timeToExpiry.get(loopexp), delta, DoubleArray.copyOf(volatility.rowArray(loopexp)));
    }

    return new SmileDeltaTermStructureParametersStrikeInterpolation(
        name,
        ImmutableList.copyOf(vt),
        timeLeftExtrapolator,
        timeInterpolator,
        timeRightExtrapolator,
        strikeLeftExtrapolator,
        strikeInterpolator,
        strikeRightExtrapolator,
        timeToExpiry);
  }

  /**
   * Obtains volatility term structure from expiry times, delta values, ATM volatilities, risk reversal figures and
   * strangle figures.
   * <p>
   * The range of delta is common to all time to expiry.
   * {@code riskReversal} and {@code strangle} should be {@code n * m}, and the length of {@code atm} should {@code n}, 
   * where {@code n} is the length of {@code timeToExpiry} and {@code m} is the length of {@code delta}.
   * 
   * @param name  the name
   * @param timeToExpiry  the expiry times of individual volatility smiles
   * @param delta  the delta values
   * @param atm  the ATM volatilities
   * @param riskReversal  the risk reversal figures
   * @param strangle  the strangle figures
   * @return the instance
   */
  public static SmileDeltaTermStructureParametersStrikeInterpolation of(
      String name,
      DoubleArray timeToExpiry,
      DoubleArray delta,
      DoubleArray atm,
      DoubleMatrix riskReversal,
      DoubleMatrix strangle) {

    return of(name, timeToExpiry, delta, atm, riskReversal, strangle, FLAT, TIME_SQUARE, FLAT, FLAT, LINEAR, FLAT);
  }

  /**
   * Obtains volatility term structure from expiry times, delta values, ATM volatilities, risk reversal figures and
   * strangle figures with strike interpolator and extrapolators specified.
   * <p>
   * The range of delta is common to all time to expiry.
   * {@code riskReversal} and {@code strangle} should be {@code n * m}, and the length of {@code atm} should {@code n}, 
   * where {@code n} is the length of {@code timeToExpiry} and {@code m} is the length of {@code delta}.
   * 
   * @param name  the name
   * @param timeToExpiry  the expiry times of individual volatility smiles
   * @param delta  the delta values
   * @param atm  the ATM volatilities
   * @param riskReversal  the risk reversal figures
   * @param strangle  the strangle figures
   * @param strikeLeftExtrapolator  left extrapolator used in the strike dimension
   * @param strikeInterpolator  interpolator used in the strike dimension
   * @param strikeRightExtrapolator  right extrapolator used in the strike dimension
   * @return the instance
   */
  public static SmileDeltaTermStructureParametersStrikeInterpolation of(
      String name,
      DoubleArray timeToExpiry,
      DoubleArray delta,
      DoubleArray atm,
      DoubleMatrix riskReversal,
      DoubleMatrix strangle,
      CurveExtrapolator strikeLeftExtrapolator,
      CurveInterpolator strikeInterpolator,
      CurveExtrapolator strikeRightExtrapolator) {

    return of(
        name,
        timeToExpiry,
        delta,
        atm,
        riskReversal,
        strangle,
        FLAT,
        TIME_SQUARE,
        FLAT,
        strikeLeftExtrapolator,
        strikeInterpolator,
        strikeRightExtrapolator);
  }

  /**
   * Obtains volatility term structure from expiry times, delta values, ATM volatilities, risk reversal figures and
   * strangle figures with interpolator and extrapolators fully specified.
   * <p>
   * The range of delta is common to all time to expiry.
   * {@code riskReversal} and {@code strangle} should be {@code n * m}, and the length of {@code atm} should {@code n}, 
   * where {@code n} is the length of {@code timeToExpiry} and {@code m} is the length of {@code delta}.
   * 
   * @param name  the name
   * @param timeToExpiry  the expiry times of individual volatility smiles
   * @param delta  the delta values
   * @param atm  the ATM volatilities
   * @param riskReversal  the risk reversal figures
   * @param strangle  the strangle figures
   * @param timeLeftExtrapolator  left extrapolator used in the time dimension
   * @param timeInterpolator  interpolator used in the time dimension
   * @param timeRightExtrapolator  right extrapolator used in the time dimension
   * @param strikeLeftExtrapolator  left extrapolator used in the strike dimension
   * @param strikeInterpolator  interpolator used in the strike dimension
   * @param strikeRightExtrapolator  right extrapolator used in the strike dimension
   * @return the instance
   */
  public static SmileDeltaTermStructureParametersStrikeInterpolation of(
      String name,
      DoubleArray timeToExpiry,
      DoubleArray delta,
      DoubleArray atm,
      DoubleMatrix riskReversal,
      DoubleMatrix strangle,
      CurveExtrapolator timeLeftExtrapolator,
      CurveInterpolator timeInterpolator,
      CurveExtrapolator timeRightExtrapolator,
      CurveExtrapolator strikeLeftExtrapolator,
      CurveInterpolator strikeInterpolator,
      CurveExtrapolator strikeRightExtrapolator) {

    ArgChecker.notNull(timeToExpiry, "timeToExpiry");
    ArgChecker.notNull(delta, "delta");
    ArgChecker.notNull(atm, "ATM");
    ArgChecker.notNull(riskReversal, "risk reversal");
    ArgChecker.notNull(strangle, "strangle");
    int nbExp = timeToExpiry.size();
    ArgChecker.isTrue(atm.size() == nbExp, "ATM length should be coherent with time to expiry length");
    ArgChecker.isTrue(riskReversal.rowCount() == nbExp,
        "Risk reversal length should be coherent with time to expiry length");
    ArgChecker.isTrue(strangle.rowCount() == nbExp, "Strangle length should be coherent with time to expiry length");
    ArgChecker.isTrue(riskReversal.columnCount() == delta.size(),
        "Risk reversal size should be coherent with time to delta length");
    ArgChecker.isTrue(strangle.columnCount() == delta.size(),
        "Strangle size should be coherent with time to delta length");
    SmileDeltaParameters[] vt = new SmileDeltaParameters[nbExp];
    for (int loopexp = 0; loopexp < nbExp; loopexp++) {
      vt[loopexp] = SmileDeltaParameters.of(
          timeToExpiry.get(loopexp),
          atm.get(loopexp),
          delta,
          DoubleArray.copyOf(riskReversal.rowArray(loopexp)),
          DoubleArray.copyOf(strangle.rowArray(loopexp)));
    }

    return new SmileDeltaTermStructureParametersStrikeInterpolation(
        name,
        ImmutableList.copyOf(vt),
        timeLeftExtrapolator,
        timeInterpolator,
        timeRightExtrapolator,
        strikeLeftExtrapolator,
        strikeInterpolator,
        strikeRightExtrapolator,
        timeToExpiry);
  }

  //-------------------------------------------------------------------------
  @ImmutableConstructor
  private SmileDeltaTermStructureParametersStrikeInterpolation(
      String name,
      List<SmileDeltaParameters> volatilityTerm,
      CurveExtrapolator timeLeftExtrapolator,
      CurveInterpolator timeInterpolator,
      CurveExtrapolator timeRightExtrapolator,
      CurveExtrapolator strikeLeftExtrapolator,
      CurveInterpolator strikeInterpolator,
      CurveExtrapolator strikeRightExtrapolator,
      DoubleArray timeToExpiry) {
    JodaBeanUtils.notNull(name, "name");
    JodaBeanUtils.notNull(timeLeftExtrapolator, "timeLeftExtrapolator");
    JodaBeanUtils.notNull(timeInterpolator, "timeInterpolator");
    JodaBeanUtils.notNull(timeRightExtrapolator, "timeRightExtrapolator");
    JodaBeanUtils.notNull(strikeLeftExtrapolator, "strikeLeftExtrapolator");
    JodaBeanUtils.notNull(strikeInterpolator, "strikeInterpolator");
    JodaBeanUtils.notNull(strikeRightExtrapolator, "strikeRightExtrapolator");
    this.name = name;
    this.volatilityTerm = ImmutableList.copyOf(volatilityTerm);
    this.timeLeftExtrapolator = timeLeftExtrapolator;
    this.timeInterpolator = timeInterpolator;
    this.timeRightExtrapolator = timeRightExtrapolator;
    this.strikeLeftExtrapolator = strikeLeftExtrapolator;
    this.strikeInterpolator = strikeInterpolator;
    this.strikeRightExtrapolator = strikeRightExtrapolator;
    this.timeToExpiry = timeToExpiry;
  }

  //-------------------------------------------------------------------------
  @Override
  public SmileDeltaTermStructureParametersStrikeInterpolation copy() {
    return new SmileDeltaTermStructureParametersStrikeInterpolation(
        name, volatilityTerm, timeLeftExtrapolator, timeInterpolator, timeRightExtrapolator, strikeLeftExtrapolator,
        strikeInterpolator, strikeRightExtrapolator, timeToExpiry);
  }

  @Override
  public double getVolatility(double time, double strike, double forward) {
    ArgChecker.isTrue(time >= 0, "Positive time");
    SmileDeltaParameters smile = getSmileForTime(time);
    DoubleArray strikes = smile.getStrike(forward);
    BoundCurveInterpolator bound = strikeInterpolator.bind(
        strikes, smile.getVolatility(), strikeLeftExtrapolator, strikeRightExtrapolator);
    return bound.interpolate(strike);
  }

  @Override
  public VolatilityAndBucketedSensitivities getVolatilityAndSensitivities(double time, double strike, double forward) {
    ArgChecker.isTrue(time >= 0, "Positive time");
    SmileDeltaParameters smile = getSmileForTime(time);
    DoubleArray strikes = smile.getStrike(forward);
    BoundCurveInterpolator bound = strikeInterpolator.bind(
        strikes, smile.getVolatility(), strikeLeftExtrapolator, strikeRightExtrapolator);
    double volatility = bound.interpolate(strike);
    DoubleArray smileVolatilityBar = bound.parameterSensitivity(strike);
    SmileAndBucketedSensitivities smileAndSensitivities = getSmileAndSensitivitiesForTime(time, smileVolatilityBar);
    return VolatilityAndBucketedSensitivities.of(volatility, smileAndSensitivities.getSensitivities());
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SmileDeltaTermStructureParametersStrikeInterpolation}.
   * @return the meta-bean, not null
   */
  public static SmileDeltaTermStructureParametersStrikeInterpolation.Meta meta() {
    return SmileDeltaTermStructureParametersStrikeInterpolation.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(SmileDeltaTermStructureParametersStrikeInterpolation.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public SmileDeltaTermStructureParametersStrikeInterpolation.Meta metaBean() {
    return SmileDeltaTermStructureParametersStrikeInterpolation.Meta.INSTANCE;
  }

  @Override
  public <R> Property<R> property(String propertyName) {
    return metaBean().<R>metaProperty(propertyName).createProperty(this);
  }

  @Override
  public Set<String> propertyNames() {
    return metaBean().metaPropertyMap().keySet();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the name of the smile term structure.
   * @return the value of the property, not null
   */
  @Override
  public String getName() {
    return name;
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
   * Gets the left extrapolator used in the time dimension.
   * @return the value of the property, not null
   */
  @Override
  public CurveExtrapolator getTimeLeftExtrapolator() {
    return timeLeftExtrapolator;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the interpolator used in the time dimension.
   * @return the value of the property, not null
   */
  @Override
  public CurveInterpolator getTimeInterpolator() {
    return timeInterpolator;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the right extrapolator used in the time dimension.
   * @return the value of the property, not null
   */
  @Override
  public CurveExtrapolator getTimeRightExtrapolator() {
    return timeRightExtrapolator;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the left extrapolator used in the strike dimension.
   * @return the value of the property, not null
   */
  public CurveExtrapolator getStrikeLeftExtrapolator() {
    return strikeLeftExtrapolator;
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
   * Gets the right extrapolator used in the strike dimension.
   * @return the value of the property, not null
   */
  public CurveExtrapolator getStrikeRightExtrapolator() {
    return strikeRightExtrapolator;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets a set of expiry times for the smile descriptions.
   * <p>
   * This set must be consistent with time to expiry in {@code volatilityTerm}, thus can be derived if
   * {@code volatilityTerm} is the primary input.
   * @return the value of the property, not null
   */
  @Override
  public DoubleArray getTimeToExpiry() {
    return timeToExpiry;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      SmileDeltaTermStructureParametersStrikeInterpolation other = (SmileDeltaTermStructureParametersStrikeInterpolation) obj;
      return JodaBeanUtils.equal(name, other.name) &&
          JodaBeanUtils.equal(volatilityTerm, other.volatilityTerm) &&
          JodaBeanUtils.equal(timeLeftExtrapolator, other.timeLeftExtrapolator) &&
          JodaBeanUtils.equal(timeInterpolator, other.timeInterpolator) &&
          JodaBeanUtils.equal(timeRightExtrapolator, other.timeRightExtrapolator) &&
          JodaBeanUtils.equal(strikeLeftExtrapolator, other.strikeLeftExtrapolator) &&
          JodaBeanUtils.equal(strikeInterpolator, other.strikeInterpolator) &&
          JodaBeanUtils.equal(strikeRightExtrapolator, other.strikeRightExtrapolator) &&
          JodaBeanUtils.equal(timeToExpiry, other.timeToExpiry);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(name);
    hash = hash * 31 + JodaBeanUtils.hashCode(volatilityTerm);
    hash = hash * 31 + JodaBeanUtils.hashCode(timeLeftExtrapolator);
    hash = hash * 31 + JodaBeanUtils.hashCode(timeInterpolator);
    hash = hash * 31 + JodaBeanUtils.hashCode(timeRightExtrapolator);
    hash = hash * 31 + JodaBeanUtils.hashCode(strikeLeftExtrapolator);
    hash = hash * 31 + JodaBeanUtils.hashCode(strikeInterpolator);
    hash = hash * 31 + JodaBeanUtils.hashCode(strikeRightExtrapolator);
    hash = hash * 31 + JodaBeanUtils.hashCode(timeToExpiry);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(320);
    buf.append("SmileDeltaTermStructureParametersStrikeInterpolation{");
    buf.append("name").append('=').append(name).append(',').append(' ');
    buf.append("volatilityTerm").append('=').append(volatilityTerm).append(',').append(' ');
    buf.append("timeLeftExtrapolator").append('=').append(timeLeftExtrapolator).append(',').append(' ');
    buf.append("timeInterpolator").append('=').append(timeInterpolator).append(',').append(' ');
    buf.append("timeRightExtrapolator").append('=').append(timeRightExtrapolator).append(',').append(' ');
    buf.append("strikeLeftExtrapolator").append('=').append(strikeLeftExtrapolator).append(',').append(' ');
    buf.append("strikeInterpolator").append('=').append(strikeInterpolator).append(',').append(' ');
    buf.append("strikeRightExtrapolator").append('=').append(strikeRightExtrapolator).append(',').append(' ');
    buf.append("timeToExpiry").append('=').append(JodaBeanUtils.toString(timeToExpiry));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SmileDeltaTermStructureParametersStrikeInterpolation}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code name} property.
     */
    private final MetaProperty<String> name = DirectMetaProperty.ofImmutable(
        this, "name", SmileDeltaTermStructureParametersStrikeInterpolation.class, String.class);
    /**
     * The meta-property for the {@code volatilityTerm} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<SmileDeltaParameters>> volatilityTerm = DirectMetaProperty.ofImmutable(
        this, "volatilityTerm", SmileDeltaTermStructureParametersStrikeInterpolation.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code timeLeftExtrapolator} property.
     */
    private final MetaProperty<CurveExtrapolator> timeLeftExtrapolator = DirectMetaProperty.ofImmutable(
        this, "timeLeftExtrapolator", SmileDeltaTermStructureParametersStrikeInterpolation.class, CurveExtrapolator.class);
    /**
     * The meta-property for the {@code timeInterpolator} property.
     */
    private final MetaProperty<CurveInterpolator> timeInterpolator = DirectMetaProperty.ofImmutable(
        this, "timeInterpolator", SmileDeltaTermStructureParametersStrikeInterpolation.class, CurveInterpolator.class);
    /**
     * The meta-property for the {@code timeRightExtrapolator} property.
     */
    private final MetaProperty<CurveExtrapolator> timeRightExtrapolator = DirectMetaProperty.ofImmutable(
        this, "timeRightExtrapolator", SmileDeltaTermStructureParametersStrikeInterpolation.class, CurveExtrapolator.class);
    /**
     * The meta-property for the {@code strikeLeftExtrapolator} property.
     */
    private final MetaProperty<CurveExtrapolator> strikeLeftExtrapolator = DirectMetaProperty.ofImmutable(
        this, "strikeLeftExtrapolator", SmileDeltaTermStructureParametersStrikeInterpolation.class, CurveExtrapolator.class);
    /**
     * The meta-property for the {@code strikeInterpolator} property.
     */
    private final MetaProperty<CurveInterpolator> strikeInterpolator = DirectMetaProperty.ofImmutable(
        this, "strikeInterpolator", SmileDeltaTermStructureParametersStrikeInterpolation.class, CurveInterpolator.class);
    /**
     * The meta-property for the {@code strikeRightExtrapolator} property.
     */
    private final MetaProperty<CurveExtrapolator> strikeRightExtrapolator = DirectMetaProperty.ofImmutable(
        this, "strikeRightExtrapolator", SmileDeltaTermStructureParametersStrikeInterpolation.class, CurveExtrapolator.class);
    /**
     * The meta-property for the {@code timeToExpiry} property.
     */
    private final MetaProperty<DoubleArray> timeToExpiry = DirectMetaProperty.ofImmutable(
        this, "timeToExpiry", SmileDeltaTermStructureParametersStrikeInterpolation.class, DoubleArray.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "name",
        "volatilityTerm",
        "timeLeftExtrapolator",
        "timeInterpolator",
        "timeRightExtrapolator",
        "strikeLeftExtrapolator",
        "strikeInterpolator",
        "strikeRightExtrapolator",
        "timeToExpiry");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case 70074929:  // volatilityTerm
          return volatilityTerm;
        case 744543655:  // timeLeftExtrapolator
          return timeLeftExtrapolator;
        case -587914188:  // timeInterpolator
          return timeInterpolator;
        case 137585666:  // timeRightExtrapolator
          return timeRightExtrapolator;
        case -145000308:  // strikeLeftExtrapolator
          return strikeLeftExtrapolator;
        case 815202713:  // strikeInterpolator
          return strikeInterpolator;
        case -1668473411:  // strikeRightExtrapolator
          return strikeRightExtrapolator;
        case -1831499397:  // timeToExpiry
          return timeToExpiry;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends SmileDeltaTermStructureParametersStrikeInterpolation> builder() {
      return new SmileDeltaTermStructureParametersStrikeInterpolation.Builder();
    }

    @Override
    public Class<? extends SmileDeltaTermStructureParametersStrikeInterpolation> beanType() {
      return SmileDeltaTermStructureParametersStrikeInterpolation.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code name} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> name() {
      return name;
    }

    /**
     * The meta-property for the {@code volatilityTerm} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<SmileDeltaParameters>> volatilityTerm() {
      return volatilityTerm;
    }

    /**
     * The meta-property for the {@code timeLeftExtrapolator} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveExtrapolator> timeLeftExtrapolator() {
      return timeLeftExtrapolator;
    }

    /**
     * The meta-property for the {@code timeInterpolator} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveInterpolator> timeInterpolator() {
      return timeInterpolator;
    }

    /**
     * The meta-property for the {@code timeRightExtrapolator} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveExtrapolator> timeRightExtrapolator() {
      return timeRightExtrapolator;
    }

    /**
     * The meta-property for the {@code strikeLeftExtrapolator} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveExtrapolator> strikeLeftExtrapolator() {
      return strikeLeftExtrapolator;
    }

    /**
     * The meta-property for the {@code strikeInterpolator} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveInterpolator> strikeInterpolator() {
      return strikeInterpolator;
    }

    /**
     * The meta-property for the {@code strikeRightExtrapolator} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveExtrapolator> strikeRightExtrapolator() {
      return strikeRightExtrapolator;
    }

    /**
     * The meta-property for the {@code timeToExpiry} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DoubleArray> timeToExpiry() {
      return timeToExpiry;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return ((SmileDeltaTermStructureParametersStrikeInterpolation) bean).getName();
        case 70074929:  // volatilityTerm
          return ((SmileDeltaTermStructureParametersStrikeInterpolation) bean).getVolatilityTerm();
        case 744543655:  // timeLeftExtrapolator
          return ((SmileDeltaTermStructureParametersStrikeInterpolation) bean).getTimeLeftExtrapolator();
        case -587914188:  // timeInterpolator
          return ((SmileDeltaTermStructureParametersStrikeInterpolation) bean).getTimeInterpolator();
        case 137585666:  // timeRightExtrapolator
          return ((SmileDeltaTermStructureParametersStrikeInterpolation) bean).getTimeRightExtrapolator();
        case -145000308:  // strikeLeftExtrapolator
          return ((SmileDeltaTermStructureParametersStrikeInterpolation) bean).getStrikeLeftExtrapolator();
        case 815202713:  // strikeInterpolator
          return ((SmileDeltaTermStructureParametersStrikeInterpolation) bean).getStrikeInterpolator();
        case -1668473411:  // strikeRightExtrapolator
          return ((SmileDeltaTermStructureParametersStrikeInterpolation) bean).getStrikeRightExtrapolator();
        case -1831499397:  // timeToExpiry
          return ((SmileDeltaTermStructureParametersStrikeInterpolation) bean).getTimeToExpiry();
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
   * The bean-builder for {@code SmileDeltaTermStructureParametersStrikeInterpolation}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<SmileDeltaTermStructureParametersStrikeInterpolation> {

    private String name;
    private List<SmileDeltaParameters> volatilityTerm = ImmutableList.of();
    private CurveExtrapolator timeLeftExtrapolator;
    private CurveInterpolator timeInterpolator;
    private CurveExtrapolator timeRightExtrapolator;
    private CurveExtrapolator strikeLeftExtrapolator;
    private CurveInterpolator strikeInterpolator;
    private CurveExtrapolator strikeRightExtrapolator;
    private DoubleArray timeToExpiry;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case 70074929:  // volatilityTerm
          return volatilityTerm;
        case 744543655:  // timeLeftExtrapolator
          return timeLeftExtrapolator;
        case -587914188:  // timeInterpolator
          return timeInterpolator;
        case 137585666:  // timeRightExtrapolator
          return timeRightExtrapolator;
        case -145000308:  // strikeLeftExtrapolator
          return strikeLeftExtrapolator;
        case 815202713:  // strikeInterpolator
          return strikeInterpolator;
        case -1668473411:  // strikeRightExtrapolator
          return strikeRightExtrapolator;
        case -1831499397:  // timeToExpiry
          return timeToExpiry;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          this.name = (String) newValue;
          break;
        case 70074929:  // volatilityTerm
          this.volatilityTerm = (List<SmileDeltaParameters>) newValue;
          break;
        case 744543655:  // timeLeftExtrapolator
          this.timeLeftExtrapolator = (CurveExtrapolator) newValue;
          break;
        case -587914188:  // timeInterpolator
          this.timeInterpolator = (CurveInterpolator) newValue;
          break;
        case 137585666:  // timeRightExtrapolator
          this.timeRightExtrapolator = (CurveExtrapolator) newValue;
          break;
        case -145000308:  // strikeLeftExtrapolator
          this.strikeLeftExtrapolator = (CurveExtrapolator) newValue;
          break;
        case 815202713:  // strikeInterpolator
          this.strikeInterpolator = (CurveInterpolator) newValue;
          break;
        case -1668473411:  // strikeRightExtrapolator
          this.strikeRightExtrapolator = (CurveExtrapolator) newValue;
          break;
        case -1831499397:  // timeToExpiry
          this.timeToExpiry = (DoubleArray) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public Builder setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    @Override
    public Builder setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    @Override
    public Builder setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public SmileDeltaTermStructureParametersStrikeInterpolation build() {
      return new SmileDeltaTermStructureParametersStrikeInterpolation(
          name,
          volatilityTerm,
          timeLeftExtrapolator,
          timeInterpolator,
          timeRightExtrapolator,
          strikeLeftExtrapolator,
          strikeInterpolator,
          strikeRightExtrapolator,
          timeToExpiry);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(320);
      buf.append("SmileDeltaTermStructureParametersStrikeInterpolation.Builder{");
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("volatilityTerm").append('=').append(JodaBeanUtils.toString(volatilityTerm)).append(',').append(' ');
      buf.append("timeLeftExtrapolator").append('=').append(JodaBeanUtils.toString(timeLeftExtrapolator)).append(',').append(' ');
      buf.append("timeInterpolator").append('=').append(JodaBeanUtils.toString(timeInterpolator)).append(',').append(' ');
      buf.append("timeRightExtrapolator").append('=').append(JodaBeanUtils.toString(timeRightExtrapolator)).append(',').append(' ');
      buf.append("strikeLeftExtrapolator").append('=').append(JodaBeanUtils.toString(strikeLeftExtrapolator)).append(',').append(' ');
      buf.append("strikeInterpolator").append('=').append(JodaBeanUtils.toString(strikeInterpolator)).append(',').append(' ');
      buf.append("strikeRightExtrapolator").append('=').append(JodaBeanUtils.toString(strikeRightExtrapolator)).append(',').append(' ');
      buf.append("timeToExpiry").append('=').append(JodaBeanUtils.toString(timeToExpiry));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
