/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.market.surface.ConstantNodalSurface;
import com.opengamma.strata.market.surface.NodalSurface;
import com.opengamma.strata.pricer.impl.volatility.VolatilityModel;
import com.opengamma.strata.pricer.impl.volatility.smile.function.SabrFormulaData;
import com.opengamma.strata.pricer.impl.volatility.smile.function.VolatilityFunctionProvider;

/**
 * The volatility surface description under SABR model.  
 * <p>
 * This is used in interest rate modeling. 
 * Each SABR parameter is {@link NodalSurface} spanned by expiry and tenor.
 * <p>
 * The implementation allows for shifted SABR model. 
 * The shift parameter is also {@link NodalSurface} spanned by expiry and tenor.
 */
@BeanDefinition(builderScope = "private")
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
   * Obtains {@code SABRInterestRateParameters} without shift from nodal surfaces and volatility function provider.
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
   * Obtains {@code SABRInterestRateParameters} with shift from nodal surfaces and volatility function provider.
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
   * Return the alpha parameter for a pair of time to expiry and instrument tenor.
   * 
   * @param expirytenor The expiry/tenor pair
   * @return The alpha parameter
   */
  public double getAlpha(DoublesPair expirytenor) {
    return alphaSurface.zValue(expirytenor);
  }

  /**
   * Return the beta parameter for a pair of time to expiry and instrument tenor.
   * 
   * @param expirytenor The expiry/tenor pair
   * @return The beta parameter
   */
  public double getBeta(DoublesPair expirytenor) {
    return betaSurface.zValue(expirytenor);
  }

  /**
   * Return the rho parameter for a pair of time to expiry and instrument tenor.
   * 
   * @param expirytenor The expiry/tenor pair
   * @return The rho parameter
   */
  public double getRho(DoublesPair expirytenor) {
    return rhoSurface.zValue(expirytenor);
  }

  /**
   * Return the nu parameter for a pair of time to expiry and instrument tenor.
   * 
   * @param expirytenor The expiry/tenor pair
   * @return The nu parameter
   */
  public double getNu(DoublesPair expirytenor) {
    return nuSurface.zValue(expirytenor);
  }

  /**
   * Return the nu parameter for a pair of time to expiry and instrument tenor.
   * 
   * @param expirytenor The expiry/tenor pair
   * @return The nu parameter
   */
  public double getShift(DoublesPair expirytenor) {
    return shiftSurface.zValue(expirytenor);
  }

  //-------------------------------------------------------------------------
  @Override
  public double getVolatility(DoubleArray t) {
    ArgChecker.notNull(t, "data");
    ArgChecker.isTrue(t.size() == 4, "data should have four components (expiry time, tenor, strike and forward");
    return getVolatility(t.get(0), t.get(1), t.get(2), t.get(3));
  }

  /**
   * Returns the volatility for given expiry, tenor, strike and forward rate.
   * 
   * @param expiryTime  time to expiry
   * @param tenor  tenor
   * @param strike  the strike
   * @param forward  the forward
   * @return the volatility
   */
  public double getVolatility(double expiryTime, double tenor, double strike, double forward) {
    DoublesPair expiryTenor = DoublesPair.of(expiryTime, tenor);
    SabrFormulaData data = SabrFormulaData.of(
        getAlpha(expiryTenor), getBeta(expiryTenor), getRho(expiryTenor), getNu(expiryTenor));
    double shift = getShift(expiryTenor);
    return sabrFunctionProvider.getVolatility(forward + shift, strike + shift, expiryTime, data);
  }

  /**
   * Returns the volatility sensitivity to forward, strike and the SABR model parameters.
   * <p>
   * The derivatives are stored in an array with [0] Derivative w.r.t the forward, [1] the derivative w.r.t the strike, 
   * [2] the derivative w.r.t. to alpha, [3] the derivative w.r.t. to beta, [4] the derivative w.r.t. to rho, and 
   * [5] the derivative w.r.t. to nu.
   * 
   * @param expiryTime  time to expiry
   * @param tenor  tenor
   * @param strike  the strike
   * @param forward  the forward
   * @return the sensitivities
   */
  public ValueDerivatives getVolatilityAdjoint(double expiryTime, double tenor, double strike, double forward) {
    DoublesPair expirytTenor = DoublesPair.of(expiryTime, tenor);
    SabrFormulaData data = SabrFormulaData.of(
        getAlpha(expirytTenor), getBeta(expirytTenor), getRho(expirytTenor), getNu(expirytTenor));
    double shift = getShift(expirytTenor);
    return sabrFunctionProvider.getVolatilityAdjoint(forward + shift, strike + shift, expiryTime, data);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SabrInterestRateParameters}.
   * @return the meta-bean, not null
   */
  public static SabrInterestRateParameters.Meta meta() {
    return SabrInterestRateParameters.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(SabrInterestRateParameters.Meta.INSTANCE);
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
  public SabrInterestRateParameters.Meta metaBean() {
    return SabrInterestRateParameters.Meta.INSTANCE;
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

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SabrInterestRateParameters}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code alphaSurface} property.
     */
    private final MetaProperty<NodalSurface> alphaSurface = DirectMetaProperty.ofImmutable(
        this, "alphaSurface", SabrInterestRateParameters.class, NodalSurface.class);
    /**
     * The meta-property for the {@code betaSurface} property.
     */
    private final MetaProperty<NodalSurface> betaSurface = DirectMetaProperty.ofImmutable(
        this, "betaSurface", SabrInterestRateParameters.class, NodalSurface.class);
    /**
     * The meta-property for the {@code rhoSurface} property.
     */
    private final MetaProperty<NodalSurface> rhoSurface = DirectMetaProperty.ofImmutable(
        this, "rhoSurface", SabrInterestRateParameters.class, NodalSurface.class);
    /**
     * The meta-property for the {@code nuSurface} property.
     */
    private final MetaProperty<NodalSurface> nuSurface = DirectMetaProperty.ofImmutable(
        this, "nuSurface", SabrInterestRateParameters.class, NodalSurface.class);
    /**
     * The meta-property for the {@code sabrFunctionProvider} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<VolatilityFunctionProvider<SabrFormulaData>> sabrFunctionProvider = DirectMetaProperty.ofImmutable(
        this, "sabrFunctionProvider", SabrInterestRateParameters.class, (Class) VolatilityFunctionProvider.class);
    /**
     * The meta-property for the {@code shiftSurface} property.
     */
    private final MetaProperty<NodalSurface> shiftSurface = DirectMetaProperty.ofImmutable(
        this, "shiftSurface", SabrInterestRateParameters.class, NodalSurface.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "alphaSurface",
        "betaSurface",
        "rhoSurface",
        "nuSurface",
        "sabrFunctionProvider",
        "shiftSurface");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 667823471:  // alphaSurface
          return alphaSurface;
        case -526589795:  // betaSurface
          return betaSurface;
        case 65433972:  // rhoSurface
          return rhoSurface;
        case 605272294:  // nuSurface
          return nuSurface;
        case 678202663:  // sabrFunctionProvider
          return sabrFunctionProvider;
        case 1038377419:  // shiftSurface
          return shiftSurface;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends SabrInterestRateParameters> builder() {
      return new SabrInterestRateParameters.Builder();
    }

    @Override
    public Class<? extends SabrInterestRateParameters> beanType() {
      return SabrInterestRateParameters.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code alphaSurface} property.
     * @return the meta-property, not null
     */
    public MetaProperty<NodalSurface> alphaSurface() {
      return alphaSurface;
    }

    /**
     * The meta-property for the {@code betaSurface} property.
     * @return the meta-property, not null
     */
    public MetaProperty<NodalSurface> betaSurface() {
      return betaSurface;
    }

    /**
     * The meta-property for the {@code rhoSurface} property.
     * @return the meta-property, not null
     */
    public MetaProperty<NodalSurface> rhoSurface() {
      return rhoSurface;
    }

    /**
     * The meta-property for the {@code nuSurface} property.
     * @return the meta-property, not null
     */
    public MetaProperty<NodalSurface> nuSurface() {
      return nuSurface;
    }

    /**
     * The meta-property for the {@code sabrFunctionProvider} property.
     * @return the meta-property, not null
     */
    public MetaProperty<VolatilityFunctionProvider<SabrFormulaData>> sabrFunctionProvider() {
      return sabrFunctionProvider;
    }

    /**
     * The meta-property for the {@code shiftSurface} property.
     * @return the meta-property, not null
     */
    public MetaProperty<NodalSurface> shiftSurface() {
      return shiftSurface;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 667823471:  // alphaSurface
          return ((SabrInterestRateParameters) bean).getAlphaSurface();
        case -526589795:  // betaSurface
          return ((SabrInterestRateParameters) bean).getBetaSurface();
        case 65433972:  // rhoSurface
          return ((SabrInterestRateParameters) bean).getRhoSurface();
        case 605272294:  // nuSurface
          return ((SabrInterestRateParameters) bean).getNuSurface();
        case 678202663:  // sabrFunctionProvider
          return ((SabrInterestRateParameters) bean).getSabrFunctionProvider();
        case 1038377419:  // shiftSurface
          return ((SabrInterestRateParameters) bean).getShiftSurface();
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
   * The bean-builder for {@code SabrInterestRateParameters}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<SabrInterestRateParameters> {

    private NodalSurface alphaSurface;
    private NodalSurface betaSurface;
    private NodalSurface rhoSurface;
    private NodalSurface nuSurface;
    private VolatilityFunctionProvider<SabrFormulaData> sabrFunctionProvider;
    private NodalSurface shiftSurface;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 667823471:  // alphaSurface
          return alphaSurface;
        case -526589795:  // betaSurface
          return betaSurface;
        case 65433972:  // rhoSurface
          return rhoSurface;
        case 605272294:  // nuSurface
          return nuSurface;
        case 678202663:  // sabrFunctionProvider
          return sabrFunctionProvider;
        case 1038377419:  // shiftSurface
          return shiftSurface;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 667823471:  // alphaSurface
          this.alphaSurface = (NodalSurface) newValue;
          break;
        case -526589795:  // betaSurface
          this.betaSurface = (NodalSurface) newValue;
          break;
        case 65433972:  // rhoSurface
          this.rhoSurface = (NodalSurface) newValue;
          break;
        case 605272294:  // nuSurface
          this.nuSurface = (NodalSurface) newValue;
          break;
        case 678202663:  // sabrFunctionProvider
          this.sabrFunctionProvider = (VolatilityFunctionProvider<SabrFormulaData>) newValue;
          break;
        case 1038377419:  // shiftSurface
          this.shiftSurface = (NodalSurface) newValue;
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
    public SabrInterestRateParameters build() {
      return new SabrInterestRateParameters(
          alphaSurface,
          betaSurface,
          rhoSurface,
          nuSurface,
          sabrFunctionProvider,
          shiftSurface);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(224);
      buf.append("SabrInterestRateParameters.Builder{");
      buf.append("alphaSurface").append('=').append(JodaBeanUtils.toString(alphaSurface)).append(',').append(' ');
      buf.append("betaSurface").append('=').append(JodaBeanUtils.toString(betaSurface)).append(',').append(' ');
      buf.append("rhoSurface").append('=').append(JodaBeanUtils.toString(rhoSurface)).append(',').append(' ');
      buf.append("nuSurface").append('=').append(JodaBeanUtils.toString(nuSurface)).append(',').append(' ');
      buf.append("sabrFunctionProvider").append('=').append(JodaBeanUtils.toString(sabrFunctionProvider)).append(',').append(' ');
      buf.append("shiftSurface").append('=').append(JodaBeanUtils.toString(shiftSurface));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
