/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.ParameterizedData;
import com.opengamma.strata.market.param.ParameterizedDataCombiner;
import com.opengamma.strata.market.surface.ConstantSurface;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.SurfaceInfoType;
import com.opengamma.strata.market.surface.Surfaces;

/**
 * The volatility surface description under SABR model.
 * <p>
 * This is used in interest rate modeling.
 * Each SABR parameter is a {@link Surface} defined by expiry and tenor.
 * <p>
 * The implementation allows for shifted SABR model.
 * The shift parameter is also {@link Surface} defined by expiry and tenor.
 */
@BeanDefinition(style = "light")
public final class SabrInterestRateParameters
    implements ParameterizedData, ImmutableBean, Serializable {

  /**
   * A surface used to apply no shift.
   */
  private static final ConstantSurface ZERO_SHIFT = ConstantSurface.of("Zero shift", 0d);

  /**
   * The alpha (volatility level) surface.
   * <p>
   * The first dimension is the expiry and the second the tenor.
   */
  @PropertyDefinition(validate = "notNull")
  private final Surface alphaSurface;
  /**
   * The beta (elasticity) surface.
   * <p>
   * The first dimension is the expiry and the second the tenor.
   */
  @PropertyDefinition(validate = "notNull")
  private final Surface betaSurface;
  /**
   * The rho (correlation) surface.
   * <p>
   * The first dimension is the expiry and the second the tenor.
   */
  @PropertyDefinition(validate = "notNull")
  private final Surface rhoSurface;
  /**
   * The nu (volatility of volatility) surface.
   * <p>
   * The first dimension is the expiry and the second the tenor.
   */
  @PropertyDefinition(validate = "notNull")
  private final Surface nuSurface;
  /**
   * The shift parameter of shifted SABR model.
   * <p>
   * The first dimension is the expiry and the second the tenor.
   * The shift is set to be 0 unless specified.
   */
  @PropertyDefinition(validate = "notNull")
  private final Surface shiftSurface;
  /**
   * The SABR volatility formula.
   */
  @PropertyDefinition(validate = "notNull")
  private final SabrVolatilityFormula sabrVolatilityFormula;
  /**
   * The day count convention of the surfaces.
   */
  private final DayCount dayCount;  // cached, not a property
  /**
   * The parameter combiner.
   */
  private final ParameterizedDataCombiner paramCombiner;  // cached, not a property

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance without shift from nodal surfaces and volatility function provider.
   * <p>
   * Each surface is specified by an instance of {@link Surface}, such as {@link InterpolatedNodalSurface}.
   * The surfaces must contain the correct metadata:
   * <ul>
   * <li>The x-value type must be {@link ValueType#YEAR_FRACTION}
   * <li>The y-value type must be {@link ValueType#YEAR_FRACTION}
   * <li>The z-value type must be {@link ValueType#SABR_ALPHA}, {@link ValueType#SABR_BETA},
   *   {@link ValueType#SABR_RHO} or {@link ValueType#SABR_NU}
   * <li>The day count must be set in the additional information of the Alpha surface using
   *   {@link SurfaceInfoType#DAY_COUNT}, if present on other surfaces it must match that on the Alpha
   * </ul>
   * Suitable surface metadata can be created using
   * {@link Surfaces#sabrParameterByExpiryTenor(String, DayCount, ValueType)}.
   * 
   * @param alphaSurface  the alpha surface
   * @param betaSurface  the beta surface
   * @param rhoSurface  the rho surface
   * @param nuSurface  the nu surface
   * @param sabrFormula  the SABR formula
   * @return {@code SabrInterestRateParameters}
   */
  @SuppressWarnings("javadoc")
  public static SabrInterestRateParameters of(
      Surface alphaSurface,
      Surface betaSurface,
      Surface rhoSurface,
      Surface nuSurface,
      SabrVolatilityFormula sabrFormula) {

    return new SabrInterestRateParameters(
        alphaSurface, betaSurface, rhoSurface, nuSurface, ZERO_SHIFT, sabrFormula);
  }

  /**
   * Obtains an instance with shift from nodal surfaces and volatility function provider.
   * <p>
   * Each surface is specified by an instance of {@link Surface}, such as {@link InterpolatedNodalSurface}.
   * The surfaces must contain the correct metadata:
   * <ul>
   * <li>The x-value type must be {@link ValueType#YEAR_FRACTION}
   * <li>The y-value type must be {@link ValueType#YEAR_FRACTION}
   * <li>The z-value type must be {@link ValueType#SABR_ALPHA}, {@link ValueType#SABR_BETA},
   *   {@link ValueType#SABR_RHO} or {@link ValueType#SABR_NU} as appropriate
   * <li>The day count must be set in the additional information of the alpha surface using
   *   {@link SurfaceInfoType#DAY_COUNT}, if present on other surfaces it must match that on the alpha
   * </ul>
   * The shift surface does not have to contain any metadata.
   * If it does, the day count and convention must match that on the alpha surface.
   * <p>
   * Suitable surface metadata can be created using
   * {@link Surfaces#sabrParameterByExpiryTenor(String, DayCount, ValueType)}.
   * 
   * @param alphaSurface  the alpha surface
   * @param betaSurface  the beta surface
   * @param rhoSurface  the rho surface
   * @param nuSurface  the nu surface
   * @param shiftSurface  the shift surface
   * @param sabrFormula  the SABR formula
   * @return {@code SabrInterestRateParameters}
   */
  public static SabrInterestRateParameters of(
      Surface alphaSurface,
      Surface betaSurface,
      Surface rhoSurface,
      Surface nuSurface,
      Surface shiftSurface,
      SabrVolatilityFormula sabrFormula) {

    return new SabrInterestRateParameters(
        alphaSurface, betaSurface, rhoSurface, nuSurface, shiftSurface, sabrFormula);
  }

  @ImmutableConstructor
  private SabrInterestRateParameters(
      Surface alphaSurface,
      Surface betaSurface,
      Surface rhoSurface,
      Surface nuSurface,
      Surface shiftSurface,
      SabrVolatilityFormula sabrFormula) {

    validate(alphaSurface, "alphaSurface", ValueType.SABR_ALPHA);
    validate(betaSurface, "betaSurface", ValueType.SABR_BETA);
    validate(rhoSurface, "rhoSurface", ValueType.SABR_RHO);
    validate(nuSurface, "nuSurface", ValueType.SABR_NU);
    ArgChecker.notNull(shiftSurface, "shiftSurface");
    ArgChecker.notNull(sabrFormula, "sabrFormula");
    DayCount dayCount = alphaSurface.getMetadata().findInfo(SurfaceInfoType.DAY_COUNT)
        .orElseThrow(() -> new IllegalArgumentException("Incorrect surface metadata, missing DayCount"));
    validate(betaSurface, dayCount);
    validate(rhoSurface, dayCount);
    validate(nuSurface, dayCount);
    validate(shiftSurface, dayCount);

    this.alphaSurface = alphaSurface;
    this.betaSurface = betaSurface;
    this.rhoSurface = rhoSurface;
    this.nuSurface = nuSurface;
    this.shiftSurface = shiftSurface;
    this.sabrVolatilityFormula = sabrFormula;
    this.dayCount = dayCount;
    this.paramCombiner = ParameterizedDataCombiner.of(alphaSurface, betaSurface, rhoSurface, nuSurface, shiftSurface);
  }

  // basic value tpe checks
  private static void validate(Surface surface, String name, ValueType zType) {
    ArgChecker.notNull(surface, name);
    surface.getMetadata().getXValueType().checkEquals(
        ValueType.YEAR_FRACTION, "Incorrect x-value type for SABR volatilities");
    surface.getMetadata().getYValueType().checkEquals(
        ValueType.YEAR_FRACTION, "Incorrect y-value type for SABR volatilities");
    ValueType zValueType = surface.getMetadata().getZValueType();
    zValueType.checkEquals(
        zType, "Incorrect z-value type for SABR volatilities");
  }

  // ensure all surfaces that specify convention or day count are consistent
  private static void validate(Surface surface, DayCount dayCount) {
    if (!surface.getMetadata().findInfo(SurfaceInfoType.DAY_COUNT).orElse(dayCount).equals(dayCount)) {
      throw new IllegalArgumentException("SABR surfaces must have the same day count");
    }
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
  public SabrInterestRateParameters withParameter(int parameterIndex, double newValue) {
    return new SabrInterestRateParameters(
        paramCombiner.underlyingWithParameter(0, Surface.class, parameterIndex, newValue),
        paramCombiner.underlyingWithParameter(1, Surface.class, parameterIndex, newValue),
        paramCombiner.underlyingWithParameter(2, Surface.class, parameterIndex, newValue),
        paramCombiner.underlyingWithParameter(3, Surface.class, parameterIndex, newValue),
        paramCombiner.underlyingWithParameter(4, Surface.class, parameterIndex, newValue),
        sabrVolatilityFormula);
  }

  @Override
  public SabrInterestRateParameters withPerturbation(ParameterPerturbation perturbation) {
    return new SabrInterestRateParameters(
        paramCombiner.underlyingWithPerturbation(0, Surface.class, perturbation),
        paramCombiner.underlyingWithPerturbation(1, Surface.class, perturbation),
        paramCombiner.underlyingWithPerturbation(2, Surface.class, perturbation),
        paramCombiner.underlyingWithPerturbation(3, Surface.class, perturbation),
        paramCombiner.underlyingWithPerturbation(4, Surface.class, perturbation),
        sabrVolatilityFormula);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the alpha parameter for a pair of time to expiry and instrument tenor.
   * 
   * @param expiry  the time to expiry as a year fraction
   * @param tenor  the tenor of the instrument as a year fraction
   * @return the alpha parameter
   */
  public double alpha(double expiry, double tenor) {
    return alphaSurface.zValue(expiry, tenor);
  }

  /**
   * Calculates the beta parameter for a pair of time to expiry and instrument tenor.
   * 
   * @param expiry  the time to expiry as a year fraction
   * @param tenor  the tenor of the instrument as a year fraction
   * @return the beta parameter
   */
  public double beta(double expiry, double tenor) {
    return betaSurface.zValue(expiry, tenor);
  }

  /**
   * Calculates the rho parameter for a pair of time to expiry and instrument tenor.
   * 
   * @param expiry  the time to expiry as a year fraction
   * @param tenor  the tenor of the instrument as a year fraction
   * @return the rho parameter
   */
  public double rho(double expiry, double tenor) {
    return rhoSurface.zValue(expiry, tenor);
  }

  /**
   * Calculates the nu parameter for a pair of time to expiry and instrument tenor.
   * 
   * @param expiry  the time to expiry as a year fraction
   * @param tenor  the tenor of the instrument as a year fraction
   * @return the nu parameter
   */
  public double nu(double expiry, double tenor) {
    return nuSurface.zValue(expiry, tenor);
  }

  /**
   * Calculates the shift parameter for a pair of time to expiry and instrument tenor.
   * 
   * @param expiry  the time to expiry as a year fraction
   * @param tenor  the tenor of the instrument as a year fraction
   * @return the shift parameter
   */
  public double shift(double expiry, double tenor) {
    return shiftSurface.zValue(expiry, tenor);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the volatility for given expiry, tenor, strike and forward rate.
   * 
   * @param expiry  the time to expiry as a year fraction
   * @param tenor  the tenor as a year fraction
   * @param strike  the strike
   * @param forward  the forward
   * @return the volatility
   */
  public double volatility(double expiry, double tenor, double strike, double forward) {
    double alpha = alpha(expiry, tenor);
    double beta = beta(expiry, tenor);
    double rho = rho(expiry, tenor);
    double nu = nu(expiry, tenor);
    double shift = shift(expiry, tenor);
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
   * @param tenor  the tenor of the instrument as a year fraction
   * @param strike  the strike
   * @param forward  the forward
   * @return the volatility and associated derivatives
   */
  public ValueDerivatives volatilityAdjoint(double expiry, double tenor, double strike, double forward) {
    double alpha = alpha(expiry, tenor);
    double beta = beta(expiry, tenor);
    double rho = rho(expiry, tenor);
    double nu = nu(expiry, tenor);
    double shift = shift(expiry, tenor);
    return sabrVolatilityFormula.volatilityAdjoint(forward + shift, strike + shift, expiry, alpha, beta, rho, nu);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SabrInterestRateParameters}.
   */
  private static MetaBean META_BEAN = LightMetaBean.of(SabrInterestRateParameters.class);

  /**
   * The meta-bean for {@code SabrInterestRateParameters}.
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
   * Gets the alpha (volatility level) surface.
   * <p>
   * The first dimension is the expiry and the second the tenor.
   * @return the value of the property, not null
   */
  public Surface getAlphaSurface() {
    return alphaSurface;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the beta (elasticity) surface.
   * <p>
   * The first dimension is the expiry and the second the tenor.
   * @return the value of the property, not null
   */
  public Surface getBetaSurface() {
    return betaSurface;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the rho (correlation) surface.
   * <p>
   * The first dimension is the expiry and the second the tenor.
   * @return the value of the property, not null
   */
  public Surface getRhoSurface() {
    return rhoSurface;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the nu (volatility of volatility) surface.
   * <p>
   * The first dimension is the expiry and the second the tenor.
   * @return the value of the property, not null
   */
  public Surface getNuSurface() {
    return nuSurface;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the shift parameter of shifted SABR model.
   * <p>
   * The first dimension is the expiry and the second the tenor.
   * The shift is set to be 0 unless specified.
   * @return the value of the property, not null
   */
  public Surface getShiftSurface() {
    return shiftSurface;
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
      SabrInterestRateParameters other = (SabrInterestRateParameters) obj;
      return JodaBeanUtils.equal(alphaSurface, other.alphaSurface) &&
          JodaBeanUtils.equal(betaSurface, other.betaSurface) &&
          JodaBeanUtils.equal(rhoSurface, other.rhoSurface) &&
          JodaBeanUtils.equal(nuSurface, other.nuSurface) &&
          JodaBeanUtils.equal(shiftSurface, other.shiftSurface) &&
          JodaBeanUtils.equal(sabrVolatilityFormula, other.sabrVolatilityFormula);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(alphaSurface);
    hash = hash * 31 + JodaBeanUtils.hashCode(betaSurface);
    hash = hash * 31 + JodaBeanUtils.hashCode(rhoSurface);
    hash = hash * 31 + JodaBeanUtils.hashCode(nuSurface);
    hash = hash * 31 + JodaBeanUtils.hashCode(shiftSurface);
    hash = hash * 31 + JodaBeanUtils.hashCode(sabrVolatilityFormula);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(224);
    buf.append("SabrInterestRateParameters{");
    buf.append("alphaSurface").append('=').append(alphaSurface).append(',').append(' ');
    buf.append("betaSurface").append('=').append(betaSurface).append(',').append(' ');
    buf.append("rhoSurface").append('=').append(rhoSurface).append(',').append(' ');
    buf.append("nuSurface").append('=').append(nuSurface).append(',').append(' ');
    buf.append("shiftSurface").append('=').append(shiftSurface).append(',').append(' ');
    buf.append("sabrVolatilityFormula").append('=').append(JodaBeanUtils.toString(sabrVolatilityFormula));
    buf.append('}');
    return buf.toString();
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
