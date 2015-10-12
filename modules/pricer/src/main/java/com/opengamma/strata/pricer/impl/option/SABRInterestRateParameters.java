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

import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.market.surface.NodalSurface;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.pricer.impl.volatility.VolatilityModel;
import com.opengamma.strata.pricer.impl.volatility.smile.function.SABRFormulaData;
import com.opengamma.strata.pricer.impl.volatility.smile.function.VolatilityFunctionProvider;

/**
 * The volatility surface description under SABR model.  
 * <p>
 * This is used in interest rate modeling. 
 * Each SABR parameter is {@link NodalSurface} spanned by expiration and tenor. 
 */
@BeanDefinition(builderScope = "private")
public final class SABRInterestRateParameters
    implements VolatilityModel<double[]>, ImmutableBean {

  /**
   * The alpha (volatility level) surface. 
   * <p>
   * The first dimension is the expiration and the second the tenor.
   */
  @PropertyDefinition(validate = "notNull")
  private final NodalSurface alphaSurface;
  /**
   * The beta (elasticity) surface. 
   * <p>
   * The first dimension is the expiration and the second the tenor.
   */
  @PropertyDefinition(validate = "notNull")
  private final NodalSurface betaSurface;
  /**
   * The rho (correlation) surface. 
   * <p>
   * The first dimension is the expiration and the second the tenor.
   */
  @PropertyDefinition(validate = "notNull")
  private final NodalSurface rhoSurface;
  /**
   * The nu (volatility of volatility) surface. 
   * <p>
   * The first dimension is the expiration and the second the tenor.
   */
  @PropertyDefinition(validate = "notNull")
  private final NodalSurface nuSurface;
  /**
   * The volatility function provider.
   * <p>
   * This returns functions containing the SABR volatility formula. 
   */
  @PropertyDefinition(validate = "notNull")
  private final VolatilityFunctionProvider<SABRFormulaData> sabrFunctionProvider;

  //-------------------------------------------------------------------------
  /**
   * Obtains {@code SABRInterestRateParameters} from nodal surfaces and volatility function provider.
   * 
   * @param alphaSurface  the alpha surface
   * @param betaSurface  the beta surface
   * @param rhoSurface  the rho surface
   * @param nuSurface  the nu surface
   * @param sabrFunctionProvider  the SABR surface
   * @return {@code SABRInterestRateParameters}
   */
  public static SABRInterestRateParameters of(
      NodalSurface alphaSurface,
      NodalSurface betaSurface,
      NodalSurface rhoSurface,
      NodalSurface nuSurface,
      VolatilityFunctionProvider<SABRFormulaData> sabrFunctionProvider) {

    return new SABRInterestRateParameters(alphaSurface, betaSurface, rhoSurface, nuSurface, sabrFunctionProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Return the alpha parameter for a pair of time to expiry and instrument tenor.
   * @param expirytenor The expiry/tenor pair.
   * @return The alpha parameter.
   */
  public double getAlpha(final DoublesPair expirytenor) {
    return alphaSurface.zValue(expirytenor);
  }

  /**
   * Return the beta parameter for a pair of time to expiry and instrument tenor.
   * @param expirytenor The expiry/tenor pair.
   * @return The beta parameter.
   */
  public double getBeta(final DoublesPair expirytenor) {
    return betaSurface.zValue(expirytenor);
  }

  /**
   * Return the rho parameter for a pair of time to expiry and instrument tenor.
   * @param expirytenor The expiry/tenor pair.
   * @return The rho parameter.
   */
  public double getRho(final DoublesPair expirytenor) {
    return rhoSurface.zValue(expirytenor);
  }

  /**
   * Return the nu parameter for a pair of time to expiry and instrument tenor.
   * @param expirytenor The expiry/tenor pair.
   * @return The nu parameter.
   */
  public double getNu(final DoublesPair expirytenor) {
    return nuSurface.zValue(expirytenor);
  }

  //-------------------------------------------------------------------------
  @Override
  public double getVolatility(double[] t) {
    ArgChecker.notNull(t, "data");
    ArgChecker.isTrue(t.length == 4, "data should have four components (expiry time, tenor, strike and forward");
    return getVolatility(t[0], t[1], t[2], t[3]);
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
    DoublesPair expirytTenor = DoublesPair.of(expiryTime, tenor);
    SABRFormulaData data = SABRFormulaData.of(
        getAlpha(expirytTenor), getBeta(expirytTenor), getRho(expirytTenor), getNu(expirytTenor));
    EuropeanVanillaOption option = EuropeanVanillaOption.of(strike, expiryTime, PutCall.CALL);
    Function1D<SABRFormulaData, Double> funcSabrLongPayer = sabrFunctionProvider.getVolatilityFunction(option, forward);
    return funcSabrLongPayer.evaluate(data);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SABRInterestRateParameters}.
   * @return the meta-bean, not null
   */
  public static SABRInterestRateParameters.Meta meta() {
    return SABRInterestRateParameters.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(SABRInterestRateParameters.Meta.INSTANCE);
  }

  private SABRInterestRateParameters(
      NodalSurface alphaSurface,
      NodalSurface betaSurface,
      NodalSurface rhoSurface,
      NodalSurface nuSurface,
      VolatilityFunctionProvider<SABRFormulaData> sabrFunctionProvider) {
    JodaBeanUtils.notNull(alphaSurface, "alphaSurface");
    JodaBeanUtils.notNull(betaSurface, "betaSurface");
    JodaBeanUtils.notNull(rhoSurface, "rhoSurface");
    JodaBeanUtils.notNull(nuSurface, "nuSurface");
    JodaBeanUtils.notNull(sabrFunctionProvider, "sabrFunctionProvider");
    this.alphaSurface = alphaSurface;
    this.betaSurface = betaSurface;
    this.rhoSurface = rhoSurface;
    this.nuSurface = nuSurface;
    this.sabrFunctionProvider = sabrFunctionProvider;
  }

  @Override
  public SABRInterestRateParameters.Meta metaBean() {
    return SABRInterestRateParameters.Meta.INSTANCE;
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
   * The first dimension is the expiration and the second the tenor.
   * @return the value of the property, not null
   */
  public NodalSurface getAlphaSurface() {
    return alphaSurface;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the beta (elasticity) surface.
   * <p>
   * The first dimension is the expiration and the second the tenor.
   * @return the value of the property, not null
   */
  public NodalSurface getBetaSurface() {
    return betaSurface;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the rho (correlation) surface.
   * <p>
   * The first dimension is the expiration and the second the tenor.
   * @return the value of the property, not null
   */
  public NodalSurface getRhoSurface() {
    return rhoSurface;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the nu (volatility of volatility) surface.
   * <p>
   * The first dimension is the expiration and the second the tenor.
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
  public VolatilityFunctionProvider<SABRFormulaData> getSabrFunctionProvider() {
    return sabrFunctionProvider;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      SABRInterestRateParameters other = (SABRInterestRateParameters) obj;
      return JodaBeanUtils.equal(getAlphaSurface(), other.getAlphaSurface()) &&
          JodaBeanUtils.equal(getBetaSurface(), other.getBetaSurface()) &&
          JodaBeanUtils.equal(getRhoSurface(), other.getRhoSurface()) &&
          JodaBeanUtils.equal(getNuSurface(), other.getNuSurface()) &&
          JodaBeanUtils.equal(getSabrFunctionProvider(), other.getSabrFunctionProvider());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getAlphaSurface());
    hash = hash * 31 + JodaBeanUtils.hashCode(getBetaSurface());
    hash = hash * 31 + JodaBeanUtils.hashCode(getRhoSurface());
    hash = hash * 31 + JodaBeanUtils.hashCode(getNuSurface());
    hash = hash * 31 + JodaBeanUtils.hashCode(getSabrFunctionProvider());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("SABRInterestRateParameters{");
    buf.append("alphaSurface").append('=').append(getAlphaSurface()).append(',').append(' ');
    buf.append("betaSurface").append('=').append(getBetaSurface()).append(',').append(' ');
    buf.append("rhoSurface").append('=').append(getRhoSurface()).append(',').append(' ');
    buf.append("nuSurface").append('=').append(getNuSurface()).append(',').append(' ');
    buf.append("sabrFunctionProvider").append('=').append(JodaBeanUtils.toString(getSabrFunctionProvider()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SABRInterestRateParameters}.
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
        this, "alphaSurface", SABRInterestRateParameters.class, NodalSurface.class);
    /**
     * The meta-property for the {@code betaSurface} property.
     */
    private final MetaProperty<NodalSurface> betaSurface = DirectMetaProperty.ofImmutable(
        this, "betaSurface", SABRInterestRateParameters.class, NodalSurface.class);
    /**
     * The meta-property for the {@code rhoSurface} property.
     */
    private final MetaProperty<NodalSurface> rhoSurface = DirectMetaProperty.ofImmutable(
        this, "rhoSurface", SABRInterestRateParameters.class, NodalSurface.class);
    /**
     * The meta-property for the {@code nuSurface} property.
     */
    private final MetaProperty<NodalSurface> nuSurface = DirectMetaProperty.ofImmutable(
        this, "nuSurface", SABRInterestRateParameters.class, NodalSurface.class);
    /**
     * The meta-property for the {@code sabrFunctionProvider} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<VolatilityFunctionProvider<SABRFormulaData>> sabrFunctionProvider = DirectMetaProperty.ofImmutable(
        this, "sabrFunctionProvider", SABRInterestRateParameters.class, (Class) VolatilityFunctionProvider.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "alphaSurface",
        "betaSurface",
        "rhoSurface",
        "nuSurface",
        "sabrFunctionProvider");

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
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends SABRInterestRateParameters> builder() {
      return new SABRInterestRateParameters.Builder();
    }

    @Override
    public Class<? extends SABRInterestRateParameters> beanType() {
      return SABRInterestRateParameters.class;
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
    public MetaProperty<VolatilityFunctionProvider<SABRFormulaData>> sabrFunctionProvider() {
      return sabrFunctionProvider;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 667823471:  // alphaSurface
          return ((SABRInterestRateParameters) bean).getAlphaSurface();
        case -526589795:  // betaSurface
          return ((SABRInterestRateParameters) bean).getBetaSurface();
        case 65433972:  // rhoSurface
          return ((SABRInterestRateParameters) bean).getRhoSurface();
        case 605272294:  // nuSurface
          return ((SABRInterestRateParameters) bean).getNuSurface();
        case 678202663:  // sabrFunctionProvider
          return ((SABRInterestRateParameters) bean).getSabrFunctionProvider();
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
   * The bean-builder for {@code SABRInterestRateParameters}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<SABRInterestRateParameters> {

    private NodalSurface alphaSurface;
    private NodalSurface betaSurface;
    private NodalSurface rhoSurface;
    private NodalSurface nuSurface;
    private VolatilityFunctionProvider<SABRFormulaData> sabrFunctionProvider;

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
          this.sabrFunctionProvider = (VolatilityFunctionProvider<SABRFormulaData>) newValue;
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
    public SABRInterestRateParameters build() {
      return new SABRInterestRateParameters(
          alphaSurface,
          betaSurface,
          rhoSurface,
          nuSurface,
          sabrFunctionProvider);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("SABRInterestRateParameters.Builder{");
      buf.append("alphaSurface").append('=').append(JodaBeanUtils.toString(alphaSurface)).append(',').append(' ');
      buf.append("betaSurface").append('=').append(JodaBeanUtils.toString(betaSurface)).append(',').append(' ');
      buf.append("rhoSurface").append('=').append(JodaBeanUtils.toString(rhoSurface)).append(',').append(' ');
      buf.append("nuSurface").append('=').append(JodaBeanUtils.toString(nuSurface)).append(',').append(' ');
      buf.append("sabrFunctionProvider").append('=').append(JodaBeanUtils.toString(sabrFunctionProvider));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
