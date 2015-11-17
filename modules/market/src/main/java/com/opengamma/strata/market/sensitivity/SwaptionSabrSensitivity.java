/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import java.io.Serializable;
import java.time.ZonedDateTime;
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

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxConvertible;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;

/**
 * Sensitivity of a swaption to SABR model parameters.
 * <p>
 * Holds the sensitivity vales to grid points of the SABR parameters.
 */
@BeanDefinition(builderScope = "private")
public final class SwaptionSabrSensitivity
    implements FxConvertible<SwaptionSabrSensitivity>, ImmutableBean, Serializable {

  /**
  * The convention of the swap for which the data is valid.
  */
  @PropertyDefinition(validate = "notNull")
  private final FixedIborSwapConvention convention;
  /**
  * The expiry date/time of the option.
  */
  @PropertyDefinition(validate = "notNull")
  private final ZonedDateTime expiry;
  /**
  * The underlying swap tenor.
  */
  @PropertyDefinition
  private final double tenor;
  /**
  * The swaption strike rate.
  */
  @PropertyDefinition
  private final double strike;
  /**
  * The underlying swap forward rate.
  */
  @PropertyDefinition
  private final double forward;
  /**
  * The currency of the sensitivity.
  */
  @PropertyDefinition
  private final Currency currency;
  /**
  * The value of the alpha sensitivity.
  */
  @PropertyDefinition
  private final double alphaSensitivity;
  /**
  * The value of the beta sensitivity.
  */
  @PropertyDefinition
  private final double betaSensitivity;
  /**
  * The value of the rho sensitivity.
  */
  @PropertyDefinition
  private final double rhoSensitivity;
  /**
  * The value of the nu sensitivity.
  */
  @PropertyDefinition
  private final double nuSensitivity;

  //-------------------------------------------------------------------------
  /**
   * Obtains a {@code SwaptionSabrSensitivity} from the specified elements. 
   * 
   * @param convention  the convention of the swap for which the data is valid
   * @param expiry  the expiry date/time of the option
   * @param tenor  the underlying swap tenor
   * @param strike  the swaption strike rate
   * @param forward  the underlying swap forward rate
   * @param sensitivityCurrency  the currency of the sensitivity
   * @param alphaSensitivity  the value of the alpha sensitivity
   * @param betaSensitivity  the value of the beta sensitivity
   * @param rhoSensitivity  the value of the rho sensitivity
   * @param nuSensitivity  the value of the nu sensitivity
   * @return the sensitivity object
   */
  public static SwaptionSabrSensitivity of(
      FixedIborSwapConvention convention,
      ZonedDateTime expiry,
      double tenor,
      double strike,
      double forward,
      Currency sensitivityCurrency,
      double alphaSensitivity,
      double betaSensitivity,
      double rhoSensitivity,
      double nuSensitivity) {

    return new SwaptionSabrSensitivity(convention, expiry, tenor, strike, forward, sensitivityCurrency,
        alphaSensitivity, betaSensitivity, rhoSensitivity, nuSensitivity);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an instance with the specified sensitivity currency. 
   * 
   * @param currency  the new currency
   * @return an instance with the specified currency
   */
  public SwaptionSabrSensitivity withCurrency(Currency currency) {
    if (this.currency.equals(currency)) {
      return this;
    }
    return new SwaptionSabrSensitivity(convention, expiry, tenor, strike, forward, currency,
        alphaSensitivity, betaSensitivity, rhoSensitivity, nuSensitivity);
  }

  /**
   * Multiplies the sensitivity values in this instance by the specified factor. 
   * 
   * @param factor  the factor
   * @return an instance with the sensitivity values rescaled
   */
  public SwaptionSabrSensitivity multipliedBy(double factor) {
    return new SwaptionSabrSensitivity(convention, expiry, tenor, strike, forward, currency,
        alphaSensitivity * factor, betaSensitivity * factor, rhoSensitivity * factor, nuSensitivity * factor);
  }

  @Override
  public SwaptionSabrSensitivity convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    if (getCurrency().equals(resultCurrency)) {
      return this;
    }
    double fxRate = rateProvider.fxRate(getCurrency(), resultCurrency);
    return withCurrency(resultCurrency).multipliedBy(fxRate);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SwaptionSabrSensitivity}.
   * @return the meta-bean, not null
   */
  public static SwaptionSabrSensitivity.Meta meta() {
    return SwaptionSabrSensitivity.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(SwaptionSabrSensitivity.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private SwaptionSabrSensitivity(
      FixedIborSwapConvention convention,
      ZonedDateTime expiry,
      double tenor,
      double strike,
      double forward,
      Currency currency,
      double alphaSensitivity,
      double betaSensitivity,
      double rhoSensitivity,
      double nuSensitivity) {
    JodaBeanUtils.notNull(convention, "convention");
    JodaBeanUtils.notNull(expiry, "expiry");
    this.convention = convention;
    this.expiry = expiry;
    this.tenor = tenor;
    this.strike = strike;
    this.forward = forward;
    this.currency = currency;
    this.alphaSensitivity = alphaSensitivity;
    this.betaSensitivity = betaSensitivity;
    this.rhoSensitivity = rhoSensitivity;
    this.nuSensitivity = nuSensitivity;
  }

  @Override
  public SwaptionSabrSensitivity.Meta metaBean() {
    return SwaptionSabrSensitivity.Meta.INSTANCE;
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
   * Gets the convention of the swap for which the data is valid.
   * @return the value of the property, not null
   */
  public FixedIborSwapConvention getConvention() {
    return convention;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the expiry date/time of the option.
   * @return the value of the property, not null
   */
  public ZonedDateTime getExpiry() {
    return expiry;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the underlying swap tenor.
   * @return the value of the property
   */
  public double getTenor() {
    return tenor;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the swaption strike rate.
   * @return the value of the property
   */
  public double getStrike() {
    return strike;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the underlying swap forward rate.
   * @return the value of the property
   */
  public double getForward() {
    return forward;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency of the sensitivity.
   * @return the value of the property
   */
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the value of the alpha sensitivity.
   * @return the value of the property
   */
  public double getAlphaSensitivity() {
    return alphaSensitivity;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the value of the beta sensitivity.
   * @return the value of the property
   */
  public double getBetaSensitivity() {
    return betaSensitivity;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the value of the rho sensitivity.
   * @return the value of the property
   */
  public double getRhoSensitivity() {
    return rhoSensitivity;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the value of the nu sensitivity.
   * @return the value of the property
   */
  public double getNuSensitivity() {
    return nuSensitivity;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      SwaptionSabrSensitivity other = (SwaptionSabrSensitivity) obj;
      return JodaBeanUtils.equal(convention, other.convention) &&
          JodaBeanUtils.equal(expiry, other.expiry) &&
          JodaBeanUtils.equal(tenor, other.tenor) &&
          JodaBeanUtils.equal(strike, other.strike) &&
          JodaBeanUtils.equal(forward, other.forward) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(alphaSensitivity, other.alphaSensitivity) &&
          JodaBeanUtils.equal(betaSensitivity, other.betaSensitivity) &&
          JodaBeanUtils.equal(rhoSensitivity, other.rhoSensitivity) &&
          JodaBeanUtils.equal(nuSensitivity, other.nuSensitivity);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(convention);
    hash = hash * 31 + JodaBeanUtils.hashCode(expiry);
    hash = hash * 31 + JodaBeanUtils.hashCode(tenor);
    hash = hash * 31 + JodaBeanUtils.hashCode(strike);
    hash = hash * 31 + JodaBeanUtils.hashCode(forward);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(alphaSensitivity);
    hash = hash * 31 + JodaBeanUtils.hashCode(betaSensitivity);
    hash = hash * 31 + JodaBeanUtils.hashCode(rhoSensitivity);
    hash = hash * 31 + JodaBeanUtils.hashCode(nuSensitivity);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(352);
    buf.append("SwaptionSabrSensitivity{");
    buf.append("convention").append('=').append(convention).append(',').append(' ');
    buf.append("expiry").append('=').append(expiry).append(',').append(' ');
    buf.append("tenor").append('=').append(tenor).append(',').append(' ');
    buf.append("strike").append('=').append(strike).append(',').append(' ');
    buf.append("forward").append('=').append(forward).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("alphaSensitivity").append('=').append(alphaSensitivity).append(',').append(' ');
    buf.append("betaSensitivity").append('=').append(betaSensitivity).append(',').append(' ');
    buf.append("rhoSensitivity").append('=').append(rhoSensitivity).append(',').append(' ');
    buf.append("nuSensitivity").append('=').append(JodaBeanUtils.toString(nuSensitivity));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SwaptionSabrSensitivity}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code convention} property.
     */
    private final MetaProperty<FixedIborSwapConvention> convention = DirectMetaProperty.ofImmutable(
        this, "convention", SwaptionSabrSensitivity.class, FixedIborSwapConvention.class);
    /**
     * The meta-property for the {@code expiry} property.
     */
    private final MetaProperty<ZonedDateTime> expiry = DirectMetaProperty.ofImmutable(
        this, "expiry", SwaptionSabrSensitivity.class, ZonedDateTime.class);
    /**
     * The meta-property for the {@code tenor} property.
     */
    private final MetaProperty<Double> tenor = DirectMetaProperty.ofImmutable(
        this, "tenor", SwaptionSabrSensitivity.class, Double.TYPE);
    /**
     * The meta-property for the {@code strike} property.
     */
    private final MetaProperty<Double> strike = DirectMetaProperty.ofImmutable(
        this, "strike", SwaptionSabrSensitivity.class, Double.TYPE);
    /**
     * The meta-property for the {@code forward} property.
     */
    private final MetaProperty<Double> forward = DirectMetaProperty.ofImmutable(
        this, "forward", SwaptionSabrSensitivity.class, Double.TYPE);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", SwaptionSabrSensitivity.class, Currency.class);
    /**
     * The meta-property for the {@code alphaSensitivity} property.
     */
    private final MetaProperty<Double> alphaSensitivity = DirectMetaProperty.ofImmutable(
        this, "alphaSensitivity", SwaptionSabrSensitivity.class, Double.TYPE);
    /**
     * The meta-property for the {@code betaSensitivity} property.
     */
    private final MetaProperty<Double> betaSensitivity = DirectMetaProperty.ofImmutable(
        this, "betaSensitivity", SwaptionSabrSensitivity.class, Double.TYPE);
    /**
     * The meta-property for the {@code rhoSensitivity} property.
     */
    private final MetaProperty<Double> rhoSensitivity = DirectMetaProperty.ofImmutable(
        this, "rhoSensitivity", SwaptionSabrSensitivity.class, Double.TYPE);
    /**
     * The meta-property for the {@code nuSensitivity} property.
     */
    private final MetaProperty<Double> nuSensitivity = DirectMetaProperty.ofImmutable(
        this, "nuSensitivity", SwaptionSabrSensitivity.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "convention",
        "expiry",
        "tenor",
        "strike",
        "forward",
        "currency",
        "alphaSensitivity",
        "betaSensitivity",
        "rhoSensitivity",
        "nuSensitivity");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 2039569265:  // convention
          return convention;
        case -1289159373:  // expiry
          return expiry;
        case 110246592:  // tenor
          return tenor;
        case -891985998:  // strike
          return strike;
        case -677145915:  // forward
          return forward;
        case 575402001:  // currency
          return currency;
        case -2039075231:  // alphaSensitivity
          return alphaSensitivity;
        case 87792271:  // betaSensitivity
          return betaSensitivity;
        case 1427302374:  // rhoSensitivity
          return rhoSensitivity;
        case -2054478248:  // nuSensitivity
          return nuSensitivity;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends SwaptionSabrSensitivity> builder() {
      return new SwaptionSabrSensitivity.Builder();
    }

    @Override
    public Class<? extends SwaptionSabrSensitivity> beanType() {
      return SwaptionSabrSensitivity.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code convention} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FixedIborSwapConvention> convention() {
      return convention;
    }

    /**
     * The meta-property for the {@code expiry} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ZonedDateTime> expiry() {
      return expiry;
    }

    /**
     * The meta-property for the {@code tenor} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> tenor() {
      return tenor;
    }

    /**
     * The meta-property for the {@code strike} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> strike() {
      return strike;
    }

    /**
     * The meta-property for the {@code forward} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> forward() {
      return forward;
    }

    /**
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    /**
     * The meta-property for the {@code alphaSensitivity} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> alphaSensitivity() {
      return alphaSensitivity;
    }

    /**
     * The meta-property for the {@code betaSensitivity} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> betaSensitivity() {
      return betaSensitivity;
    }

    /**
     * The meta-property for the {@code rhoSensitivity} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> rhoSensitivity() {
      return rhoSensitivity;
    }

    /**
     * The meta-property for the {@code nuSensitivity} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> nuSensitivity() {
      return nuSensitivity;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 2039569265:  // convention
          return ((SwaptionSabrSensitivity) bean).getConvention();
        case -1289159373:  // expiry
          return ((SwaptionSabrSensitivity) bean).getExpiry();
        case 110246592:  // tenor
          return ((SwaptionSabrSensitivity) bean).getTenor();
        case -891985998:  // strike
          return ((SwaptionSabrSensitivity) bean).getStrike();
        case -677145915:  // forward
          return ((SwaptionSabrSensitivity) bean).getForward();
        case 575402001:  // currency
          return ((SwaptionSabrSensitivity) bean).getCurrency();
        case -2039075231:  // alphaSensitivity
          return ((SwaptionSabrSensitivity) bean).getAlphaSensitivity();
        case 87792271:  // betaSensitivity
          return ((SwaptionSabrSensitivity) bean).getBetaSensitivity();
        case 1427302374:  // rhoSensitivity
          return ((SwaptionSabrSensitivity) bean).getRhoSensitivity();
        case -2054478248:  // nuSensitivity
          return ((SwaptionSabrSensitivity) bean).getNuSensitivity();
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
   * The bean-builder for {@code SwaptionSabrSensitivity}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<SwaptionSabrSensitivity> {

    private FixedIborSwapConvention convention;
    private ZonedDateTime expiry;
    private double tenor;
    private double strike;
    private double forward;
    private Currency currency;
    private double alphaSensitivity;
    private double betaSensitivity;
    private double rhoSensitivity;
    private double nuSensitivity;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 2039569265:  // convention
          return convention;
        case -1289159373:  // expiry
          return expiry;
        case 110246592:  // tenor
          return tenor;
        case -891985998:  // strike
          return strike;
        case -677145915:  // forward
          return forward;
        case 575402001:  // currency
          return currency;
        case -2039075231:  // alphaSensitivity
          return alphaSensitivity;
        case 87792271:  // betaSensitivity
          return betaSensitivity;
        case 1427302374:  // rhoSensitivity
          return rhoSensitivity;
        case -2054478248:  // nuSensitivity
          return nuSensitivity;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 2039569265:  // convention
          this.convention = (FixedIborSwapConvention) newValue;
          break;
        case -1289159373:  // expiry
          this.expiry = (ZonedDateTime) newValue;
          break;
        case 110246592:  // tenor
          this.tenor = (Double) newValue;
          break;
        case -891985998:  // strike
          this.strike = (Double) newValue;
          break;
        case -677145915:  // forward
          this.forward = (Double) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case -2039075231:  // alphaSensitivity
          this.alphaSensitivity = (Double) newValue;
          break;
        case 87792271:  // betaSensitivity
          this.betaSensitivity = (Double) newValue;
          break;
        case 1427302374:  // rhoSensitivity
          this.rhoSensitivity = (Double) newValue;
          break;
        case -2054478248:  // nuSensitivity
          this.nuSensitivity = (Double) newValue;
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
    public SwaptionSabrSensitivity build() {
      return new SwaptionSabrSensitivity(
          convention,
          expiry,
          tenor,
          strike,
          forward,
          currency,
          alphaSensitivity,
          betaSensitivity,
          rhoSensitivity,
          nuSensitivity);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(352);
      buf.append("SwaptionSabrSensitivity.Builder{");
      buf.append("convention").append('=').append(JodaBeanUtils.toString(convention)).append(',').append(' ');
      buf.append("expiry").append('=').append(JodaBeanUtils.toString(expiry)).append(',').append(' ');
      buf.append("tenor").append('=').append(JodaBeanUtils.toString(tenor)).append(',').append(' ');
      buf.append("strike").append('=').append(JodaBeanUtils.toString(strike)).append(',').append(' ');
      buf.append("forward").append('=').append(JodaBeanUtils.toString(forward)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("alphaSensitivity").append('=').append(JodaBeanUtils.toString(alphaSensitivity)).append(',').append(' ');
      buf.append("betaSensitivity").append('=').append(JodaBeanUtils.toString(betaSensitivity)).append(',').append(' ');
      buf.append("rhoSensitivity").append('=').append(JodaBeanUtils.toString(rhoSensitivity)).append(',').append(' ');
      buf.append("nuSensitivity").append('=').append(JodaBeanUtils.toString(nuSensitivity));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
