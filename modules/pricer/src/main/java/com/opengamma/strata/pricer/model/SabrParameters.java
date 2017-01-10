/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.model;

import java.io.Serializable;
import java.util.Set;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveInfoType;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.ParameterizedData;
import com.opengamma.strata.market.param.ParameterizedDataCombiner;

/**
 * The volatility surface description under SABR model.
 * <p>
 * This is used in interest rate modeling.
 * Each SABR parameter is a {@link Curve} defined by expiry.
 * <p>
 * The implementation allows for shifted SABR model.
 * The shift parameter is also {@link Curve} defined by expiry.
 */
@BeanDefinition(style = "light")
public final class SabrParameters
    implements ParameterizedData, ImmutableBean, Serializable {

  /**
   * A Curve used to apply no shift.
   */
  private static final ConstantCurve ZERO_SHIFT = ConstantCurve.of("Zero shift", 0d);

  /**
   * The alpha (volatility level) curve.
   * <p>
   * The x value of the curve is the expiry.
   */
  @PropertyDefinition(validate = "notNull")
  private final Curve alphaCurve;
  /**
   * The beta (elasticity) curve.
   * <p>
   * The x value of the curve is the expiry.
   */
  @PropertyDefinition(validate = "notNull")
  private final Curve betaCurve;
  /**
   * The rho (correlation) curve.
   * <p>
   * The x value of the curve is the expiry.
   */
  @PropertyDefinition(validate = "notNull")
  private final Curve rhoCurve;
  /**
   * The nu (volatility of volatility) curve.
   * <p>
   * The x value of the curve is the expiry.
   */
  @PropertyDefinition(validate = "notNull")
  private final Curve nuCurve;
  /**
   * The shift parameter of shifted SABR model.
   * <p>
   * The x value of the curve is the expiry.
   * The shift is set to be 0 unless specified.
   */
  @PropertyDefinition(validate = "notNull")
  private final Curve shiftCurve;
  /**
   * The SABR volatility formula.
   */
  @PropertyDefinition(validate = "notNull")
  private final SabrVolatilityFormula sabrVolatilityFormula;
  /**
   * The day count convention of the curves.
   */
  private final transient DayCount dayCount;  // cached, not a property
  /**
   * The parameter combiner.
   */
  private final transient ParameterizedDataCombiner paramCombiner;  // cached, not a property

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance without shift from nodal curves and volatility function provider.
   * <p>
   * Each curve is specified by an instance of {@link Curve}, such as {@link InterpolatedNodalCurve}.
   * The curves must contain the correct metadata:
   * <ul>
   * <li>The x-value type must be {@link ValueType#YEAR_FRACTION}
   * <li>The y-value type must be {@link ValueType#SABR_ALPHA}, {@link ValueType#SABR_BETA},
   *   {@link ValueType#SABR_RHO} or {@link ValueType#SABR_NU}
   * <li>The day count must be set in the additional information of the Alpha curve using
   *   {@link CurveInfoType#DAY_COUNT}, if present on other curves it must match that on the Alpha
   * </ul>
   * Suitable curve metadata can be created using
   * {@link Curves#sabrParameterByExpiry(String, DayCount, ValueType)}.
   * 
   * @param alphaCurve  the alpha curve
   * @param betaCurve  the beta curve
   * @param rhoCurve  the rho curve
   * @param nuCurve  the nu curve
   * @param sabrFormula  the SABR formula
   * @return {@code SabrParameters}
   */
  public static SabrParameters of(
      Curve alphaCurve,
      Curve betaCurve,
      Curve rhoCurve,
      Curve nuCurve,
      SabrVolatilityFormula sabrFormula) {

    return new SabrParameters(alphaCurve, betaCurve, rhoCurve, nuCurve, ZERO_SHIFT, sabrFormula);
  }

  /**
   * Obtains an instance with shift from nodal curves and volatility function provider.
   * <p>
   * Each curve is specified by an instance of {@link Curve}, such as {@link InterpolatedNodalCurve}.
   * The curves must contain the correct metadata:
   * <ul>
   * <li>The x-value type must be {@link ValueType#YEAR_FRACTION}
   * <li>The y-value type must be {@link ValueType#YEAR_FRACTION}
   * <li>The z-value type must be {@link ValueType#SABR_ALPHA}, {@link ValueType#SABR_BETA},
   *   {@link ValueType#SABR_RHO} or {@link ValueType#SABR_NU} as appropriate
   * <li>The day count must be set in the additional information of the alpha curve using
   *   {@link CurveInfoType#DAY_COUNT}, if present on other curves it must match that on the alpha
   * </ul>
   * The shift curve does not have to contain any metadata.
   * If it does, the day count and convention must match that on the alpha curve.
   * <p>
   * Suitable curve metadata can be created using
   * {@link Curves#sabrParameterByExpiry(String, DayCount, ValueType)}.
   * 
   * @param alphaCurve  the alpha curve
   * @param betaCurve  the beta curve
   * @param rhoCurve  the rho curve
   * @param nuCurve  the nu curve
   * @param shiftCurve  the shift curve
   * @param sabrFormula  the SABR formula
   * @return {@code SabrParameters}
   */
  public static SabrParameters of(
      Curve alphaCurve,
      Curve betaCurve,
      Curve rhoCurve,
      Curve nuCurve,
      Curve shiftCurve,
      SabrVolatilityFormula sabrFormula) {

    return new SabrParameters(alphaCurve, betaCurve, rhoCurve, nuCurve, shiftCurve, sabrFormula);
  }

  @ImmutableConstructor
  private SabrParameters(
      Curve alphaCurve,
      Curve betaCurve,
      Curve rhoCurve,
      Curve nuCurve,
      Curve shiftCurve,
      SabrVolatilityFormula sabrFormula) {

    validate(alphaCurve, "alphaCurve", ValueType.SABR_ALPHA);
    validate(betaCurve, "betaCurve", ValueType.SABR_BETA);
    validate(rhoCurve, "rhoCurve", ValueType.SABR_RHO);
    validate(nuCurve, "nuCurve", ValueType.SABR_NU);
    ArgChecker.notNull(shiftCurve, "shiftCurve");
    ArgChecker.notNull(sabrFormula, "sabrFormula");
    DayCount dayCount = alphaCurve.getMetadata().findInfo(CurveInfoType.DAY_COUNT)
        .orElseThrow(() -> new IllegalArgumentException("Incorrect curve metadata, missing DayCount"));
    validate(betaCurve, dayCount);
    validate(rhoCurve, dayCount);
    validate(nuCurve, dayCount);
    validate(shiftCurve, dayCount);

    this.alphaCurve = alphaCurve;
    this.betaCurve = betaCurve;
    this.rhoCurve = rhoCurve;
    this.nuCurve = nuCurve;
    this.shiftCurve = shiftCurve;
    this.sabrVolatilityFormula = sabrFormula;
    this.dayCount = dayCount;
    this.paramCombiner = ParameterizedDataCombiner.of(alphaCurve, betaCurve, rhoCurve, nuCurve, shiftCurve);
  }

  // basic value tpe checks
  private static void validate(Curve curve, String name, ValueType yType) {
    ArgChecker.notNull(curve, name);
    curve.getMetadata().getXValueType().checkEquals(
        ValueType.YEAR_FRACTION, "Incorrect x-value type for SABR volatilities");
    ValueType yValueType = curve.getMetadata().getYValueType();
    yValueType.checkEquals(yType, "Incorrect y-value type for SABR volatilities");
  }

  // ensure all curves that specify convention or day count are consistent
  private static void validate(Curve curve, DayCount dayCount) {
    if (!curve.getMetadata().findInfo(CurveInfoType.DAY_COUNT).orElse(dayCount).equals(dayCount)) {
      throw new IllegalArgumentException("SABR curves must have the same day count");
    }
  }

  // ensure standard constructor is invoked
  private Object readResolve() {
    return new SabrParameters(alphaCurve, betaCurve, rhoCurve, nuCurve, shiftCurve, sabrVolatilityFormula);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the day count used to calculate the expiry year fraction.
   * 
   * @return the day count
   */
  public DayCount getDayCount() {
    return dayCount;
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
  public SabrParameters withParameter(int parameterIndex, double newValue) {
    return new SabrParameters(
        paramCombiner.underlyingWithParameter(0, Curve.class, parameterIndex, newValue),
        paramCombiner.underlyingWithParameter(1, Curve.class, parameterIndex, newValue),
        paramCombiner.underlyingWithParameter(2, Curve.class, parameterIndex, newValue),
        paramCombiner.underlyingWithParameter(3, Curve.class, parameterIndex, newValue),
        paramCombiner.underlyingWithParameter(4, Curve.class, parameterIndex, newValue),
        sabrVolatilityFormula);
  }

  @Override
  public SabrParameters withPerturbation(ParameterPerturbation perturbation) {
    return new SabrParameters(
        paramCombiner.underlyingWithPerturbation(0, Curve.class, perturbation),
        paramCombiner.underlyingWithPerturbation(1, Curve.class, perturbation),
        paramCombiner.underlyingWithPerturbation(2, Curve.class, perturbation),
        paramCombiner.underlyingWithPerturbation(3, Curve.class, perturbation),
        paramCombiner.underlyingWithPerturbation(4, Curve.class, perturbation),
        sabrVolatilityFormula);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the alpha parameter for time to expiry.
   * 
   * @param expiry  the time to expiry as a year fraction
   * @return the alpha parameter
   */
  public double alpha(double expiry) {
    return alphaCurve.yValue(expiry);
  }

  /**
   * Calculates the beta parameter for time to expiry.
   * 
   * @param expiry  the time to expiry as a year fraction
   * @return the beta parameter
   */
  public double beta(double expiry) {
    return betaCurve.yValue(expiry);
  }

  /**
   * Calculates the rho parameter for time to expiry.
   * 
   * @param expiry  the time to expiry as a year fraction
   * @return the rho parameter
   */
  public double rho(double expiry) {
    return rhoCurve.yValue(expiry);
  }

  /**
   * Calculates the nu parameter for time to expiry.
   * 
   * @param expiry  the time to expiry as a year fraction
   * @return the nu parameter
   */
  public double nu(double expiry) {
    return nuCurve.yValue(expiry);
  }

  /**
   * Calculates the shift parameter for time to expiry.
   * 
   * @param expiry  the time to expiry as a year fraction
   * @return the shift parameter
   */
  public double shift(double expiry) {
    return shiftCurve.yValue(expiry);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the volatility for given expiry, strike and forward rate.
   * 
   * @param expiry  the time to expiry as a year fraction
   * @param strike  the strike
   * @param forward  the forward
   * @return the volatility
   */
  public double volatility(double expiry, double strike, double forward) {
    double alpha = alpha(expiry);
    double beta = beta(expiry);
    double rho = rho(expiry);
    double nu = nu(expiry);
    double shift = shift(expiry);
    return sabrVolatilityFormula.volatility(forward + shift, strike + shift, expiry, alpha, beta, rho, nu);
  }

  /**
   * Calculates the volatility and associated sensitivities.
   * <p>
   * The derivatives are stored in an array with:
   * <ul>
   * <li>[0] derivative with respect to the forward
   * <li>[1] derivative with respect to the forward strike
   * <li>[2] derivative with respect to the alpha
   * <li>[3] derivative with respect to the beta
   * <li>[4] derivative with respect to the rho
   * <li>[5] derivative with respect to the nu
   * </ul>
   * 
   * @param expiry  the time to expiry as a year fraction
   * @param strike  the strike
   * @param forward  the forward
   * @return the volatility and associated derivatives
   */
  public ValueDerivatives volatilityAdjoint(double expiry, double strike, double forward) {
    double alpha = alpha(expiry);
    double beta = beta(expiry);
    double rho = rho(expiry);
    double nu = nu(expiry);
    double shift = shift(expiry);
    return sabrVolatilityFormula.volatilityAdjoint(forward + shift, strike + shift, expiry, alpha, beta, rho, nu);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SabrParameters}.
   */
  private static MetaBean META_BEAN = LightMetaBean.of(SabrParameters.class);

  /**
   * The meta-bean for {@code SabrParameters}.
   * @return the meta-bean, not null
   */
  public static MetaBean meta() {
    return META_BEAN;
  }

  static {
    JodaBeanUtils.registerMetaBean(META_BEAN);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public MetaBean metaBean() {
    return META_BEAN;
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
   * Gets the alpha (volatility level) curve.
   * <p>
   * The x value of the curve is the expiry.
   * @return the value of the property, not null
   */
  public Curve getAlphaCurve() {
    return alphaCurve;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the beta (elasticity) curve.
   * <p>
   * The x value of the curve is the expiry.
   * @return the value of the property, not null
   */
  public Curve getBetaCurve() {
    return betaCurve;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the rho (correlation) curve.
   * <p>
   * The x value of the curve is the expiry.
   * @return the value of the property, not null
   */
  public Curve getRhoCurve() {
    return rhoCurve;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the nu (volatility of volatility) curve.
   * <p>
   * The x value of the curve is the expiry.
   * @return the value of the property, not null
   */
  public Curve getNuCurve() {
    return nuCurve;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the shift parameter of shifted SABR model.
   * <p>
   * The x value of the curve is the expiry.
   * The shift is set to be 0 unless specified.
   * @return the value of the property, not null
   */
  public Curve getShiftCurve() {
    return shiftCurve;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the SABR volatility formula.
   * @return the value of the property, not null
   */
  public SabrVolatilityFormula getSabrVolatilityFormula() {
    return sabrVolatilityFormula;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      SabrParameters other = (SabrParameters) obj;
      return JodaBeanUtils.equal(alphaCurve, other.alphaCurve) &&
          JodaBeanUtils.equal(betaCurve, other.betaCurve) &&
          JodaBeanUtils.equal(rhoCurve, other.rhoCurve) &&
          JodaBeanUtils.equal(nuCurve, other.nuCurve) &&
          JodaBeanUtils.equal(shiftCurve, other.shiftCurve) &&
          JodaBeanUtils.equal(sabrVolatilityFormula, other.sabrVolatilityFormula);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(alphaCurve);
    hash = hash * 31 + JodaBeanUtils.hashCode(betaCurve);
    hash = hash * 31 + JodaBeanUtils.hashCode(rhoCurve);
    hash = hash * 31 + JodaBeanUtils.hashCode(nuCurve);
    hash = hash * 31 + JodaBeanUtils.hashCode(shiftCurve);
    hash = hash * 31 + JodaBeanUtils.hashCode(sabrVolatilityFormula);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(224);
    buf.append("SabrParameters{");
    buf.append("alphaCurve").append('=').append(alphaCurve).append(',').append(' ');
    buf.append("betaCurve").append('=').append(betaCurve).append(',').append(' ');
    buf.append("rhoCurve").append('=').append(rhoCurve).append(',').append(' ');
    buf.append("nuCurve").append('=').append(nuCurve).append(',').append(' ');
    buf.append("shiftCurve").append('=').append(shiftCurve).append(',').append(' ');
    buf.append("sabrVolatilityFormula").append('=').append(JodaBeanUtils.toString(sabrVolatilityFormula));
    buf.append('}');
    return buf.toString();
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------

}
