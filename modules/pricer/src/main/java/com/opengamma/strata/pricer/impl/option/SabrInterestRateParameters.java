/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option;

import java.util.Set;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.surface.ConstantNodalSurface;
import com.opengamma.strata.market.surface.NodalSurface;
import com.opengamma.strata.pricer.impl.volatility.VolatilityModel;
import com.opengamma.strata.pricer.impl.volatility.smile.function.SabrFormulaData;
import com.opengamma.strata.pricer.impl.volatility.smile.function.VolatilityFunctionProvider;

/**
 * The volatility surface description under SABR model.  
 * <p>
 * This is used in interest rate modeling. 
 * Each SABR parameter is a {@link NodalSurface} defined by expiry and tenor.
 * <p>
 * The implementation allows for shifted SABR model. 
 * The shift parameter is also {@link NodalSurface} defined by expiry and tenor.
 */
@BeanDefinition(style = "light")
public final class SabrInterestRateParameters
    implements VolatilityModel<DoubleArray>, ImmutableBean {

  /**
   * The alpha (volatility level) surface. 
   * <p>
   * The first dimension is the expiry and the second the tenor.
   */
  @PropertyDefinition(validate = "notNull")
  private final NodalSurface alphaSurface;
  /**
   * The beta (elasticity) surface. 
   * <p>
   * The first dimension is the expiry and the second the tenor.
   */
  @PropertyDefinition(validate = "notNull")
  private final NodalSurface betaSurface;
  /**
   * The rho (correlation) surface. 
   * <p>
   * The first dimension is the expiry and the second the tenor.
   */
  @PropertyDefinition(validate = "notNull")
  private final NodalSurface rhoSurface;
  /**
   * The nu (volatility of volatility) surface. 
   * <p>
   * The first dimension is the expiry and the second the tenor.
   */
  @PropertyDefinition(validate = "notNull")
  private final NodalSurface nuSurface;
  /**
   * The volatility function provider.
   * <p>
   * This returns functions containing the SABR volatility formula. 
   */
  @PropertyDefinition(validate = "notNull")
  private final VolatilityFunctionProvider<SabrFormulaData> sabrFunctionProvider;
  /**
   * The shift parameter of shifted SABR model.
   * <p>
   * The first dimension is the expiry and the second the tenor.
   * The shift is set to be 0 unless specified.
   */
  @PropertyDefinition(validate = "notNull")
  private final NodalSurface shiftSurface;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance without shift from nodal surfaces and volatility function provider.
   * 
   * @param alphaSurface  the alpha surface
   * @param betaSurface  the beta surface
   * @param rhoSurface  the rho surface
   * @param nuSurface  the nu surface
   * @param sabrFunctionProvider  the SABR surface
   * @return {@code SABRInterestRateParameters}
   */
  public static SabrInterestRateParameters of(
      NodalSurface alphaSurface,
      NodalSurface betaSurface,
      NodalSurface rhoSurface,
      NodalSurface nuSurface,
      VolatilityFunctionProvider<SabrFormulaData> sabrFunctionProvider) {

    NodalSurface shiftSurface = ConstantNodalSurface.of("zero shift", 0d);
    return new SabrInterestRateParameters(
        alphaSurface, betaSurface, rhoSurface, nuSurface, sabrFunctionProvider, shiftSurface);
  }

  /**
   * Obtains an instance with shift from nodal surfaces and volatility function provider.
   * 
   * @param alphaSurface  the alpha surface
   * @param betaSurface  the beta surface
   * @param rhoSurface  the rho surface
   * @param nuSurface  the nu surface
   * @param sabrFunctionProvider  the SABR surface
   * @param shiftSurface  the shift surface
   * @return {@code SABRInterestRateParameters}
   */
  public static SabrInterestRateParameters of(
      NodalSurface alphaSurface,
      NodalSurface betaSurface,
      NodalSurface rhoSurface,
      NodalSurface nuSurface,
      VolatilityFunctionProvider<SabrFormulaData> sabrFunctionProvider,
      NodalSurface shiftSurface) {

    return new SabrInterestRateParameters(
        alphaSurface, betaSurface, rhoSurface, nuSurface, sabrFunctionProvider, shiftSurface);
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
  @Override
  public double volatility(DoubleArray t) {
    ArgChecker.notNull(t, "data");
    ArgChecker.isTrue(t.size() == 4, "data should have four components (expiry time, tenor, strike and forward");
    return volatility(t.get(0), t.get(1), t.get(2), t.get(3));
  }

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
    SabrFormulaData data = SabrFormulaData.of(
        alpha(expiry, tenor), beta(expiry, tenor), rho(expiry, tenor), nu(expiry, tenor));
    double shift = shift(expiry, tenor);
    return sabrFunctionProvider.getVolatility(forward + shift, strike + shift, expiry, data);
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
    SabrFormulaData data = SabrFormulaData.of(
        alpha(expiry, tenor), beta(expiry, tenor), rho(expiry, tenor), nu(expiry, tenor));
    double shift = shift(expiry, tenor);
    return sabrFunctionProvider.getVolatilityAdjoint(forward + shift, strike + shift, expiry, data);
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

  private SabrInterestRateParameters(
      NodalSurface alphaSurface,
      NodalSurface betaSurface,
      NodalSurface rhoSurface,
      NodalSurface nuSurface,
      VolatilityFunctionProvider<SabrFormulaData> sabrFunctionProvider,
      NodalSurface shiftSurface) {
    JodaBeanUtils.notNull(alphaSurface, "alphaSurface");
    JodaBeanUtils.notNull(betaSurface, "betaSurface");
    JodaBeanUtils.notNull(rhoSurface, "rhoSurface");
    JodaBeanUtils.notNull(nuSurface, "nuSurface");
    JodaBeanUtils.notNull(sabrFunctionProvider, "sabrFunctionProvider");
    JodaBeanUtils.notNull(shiftSurface, "shiftSurface");
    this.alphaSurface = alphaSurface;
    this.betaSurface = betaSurface;
    this.rhoSurface = rhoSurface;
    this.nuSurface = nuSurface;
    this.sabrFunctionProvider = sabrFunctionProvider;
    this.shiftSurface = shiftSurface;
  }

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
  public NodalSurface getAlphaSurface() {
    return alphaSurface;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the beta (elasticity) surface.
   * <p>
   * The first dimension is the expiry and the second the tenor.
   * @return the value of the property, not null
   */
  public NodalSurface getBetaSurface() {
    return betaSurface;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the rho (correlation) surface.
   * <p>
   * The first dimension is the expiry and the second the tenor.
   * @return the value of the property, not null
   */
  public NodalSurface getRhoSurface() {
    return rhoSurface;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the nu (volatility of volatility) surface.
   * <p>
   * The first dimension is the expiry and the second the tenor.
   * @return the value of the property, not null
   */
  public NodalSurface getNuSurface() {
    return nuSurface;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the volatility function provider.
   * <p>
   * This returns functions containing the SABR volatility formula.
   * @return the value of the property, not null
   */
  public VolatilityFunctionProvider<SabrFormulaData> getSabrFunctionProvider() {
    return sabrFunctionProvider;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the shift parameter of shifted SABR model.
   * <p>
   * The first dimension is the expiry and the second the tenor.
   * The shift is set to be 0 unless specified.
   * @return the value of the property, not null
   */
  public NodalSurface getShiftSurface() {
    return shiftSurface;
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
          JodaBeanUtils.equal(sabrFunctionProvider, other.sabrFunctionProvider) &&
          JodaBeanUtils.equal(shiftSurface, other.shiftSurface);
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
    hash = hash * 31 + JodaBeanUtils.hashCode(sabrFunctionProvider);
    hash = hash * 31 + JodaBeanUtils.hashCode(shiftSurface);
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
    buf.append("sabrFunctionProvider").append('=').append(sabrFunctionProvider).append(',').append(' ');
    buf.append("shiftSurface").append('=').append(JodaBeanUtils.toString(shiftSurface));
    buf.append('}');
    return buf.toString();
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
