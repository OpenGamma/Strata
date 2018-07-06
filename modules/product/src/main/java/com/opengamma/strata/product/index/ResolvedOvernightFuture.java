/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableDefaults;
import org.joda.beans.gen.ImmutablePreBuild;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.value.Rounding;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.ResolvedProduct;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.rate.OvernightRateComputation;

/**
 * A futures contract based on an Overnight index, resolved for pricing.
 * <p>
 * This is the resolved form of {@link OvernightFuture} and is an input to the pricers.
 * Applications will typically create a {@code ResolvedOvernightFuture} from a {@code OvernightFuture}
 * using {@link OvernightFuture#resolve(ReferenceData)}.
 * <p>
 * A {@code ResolvedOvernightFuture} is bound to data that changes over time, such as holiday calendars.
 * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
 * Care must be taken when placing the resolved form in a cache or persistence layer.
 * 
 * <h4>Price</h4>
 * The price of an Overnight rate future is based on the interest rate of the underlying index.
 * It is defined as {@code (100 - percentRate)}.
 * <p>
 * Strata uses <i>decimal prices</i> for Overnight rate futures in the trade model, pricers and market data.
 * The decimal price is based on the decimal rate equivalent to the percentage.
 * For example, a price of 99.32 implies an interest rate of 0.68% which is represented in Strata by 0.9932.
 */
@BeanDefinition(constructorScope = "package")
public final class ResolvedOvernightFuture
    implements ResolvedProduct, ImmutableBean, Serializable {

  /**
   * The security identifier.
   * <p>
   * This identifier uniquely identifies the security within the system.
   */
  @PropertyDefinition(validate = "notNull")
  private final SecurityId securityId;
  /**
   * The currency that the future is traded in.
   */
  @PropertyDefinition(validate = "notNull")
  private final Currency currency;
  /**
   * The notional amount.
   * <p>
   * This is the full notional of the deposit, such as 5 million dollars.
   * The notional expressed here must be positive.
   * The currency of the notional is specified by {@code currency}.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegativeOrZero")
  private final double notional;
  /**
   * The accrual factor, defaulted from the index if not set.
   * <p>
   * This is the year fraction of the contract, typically 1/12 for a 30-day future.
   * The year fraction must be positive.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegativeOrZero")
  private final double accrualFactor;
  /**
   * The last date of trading.
   * <p>
   * This must be a valid business day on the fixing calendar of {@code index}.
   * The last trade date is typically the last business day of the month.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate lastTradeDate;
  /**
   * The Overnight rate observation.
   * <p>
   * The future is based on this index.
   * It will be a well known market index such as 'USD-FED-FUND'.
   */
  @PropertyDefinition(validate = "notNull")
  private final OvernightRateComputation overnightRate;
  /**
   * The definition of how to round the futures price, defaulted to no rounding.
   * <p>
   * The price is represented in decimal form, not percentage form.
   * As such, the decimal places expressed by the rounding refers to this decimal form.
   * For example, the common market price of 99.7125 for a 0.2875% rate is
   * represented as 0.997125 which has 6 decimal places.
   */
  @PropertyDefinition(validate = "notNull")
  private final Rounding rounding;

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.rounding(Rounding.none());
  }

  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.overnightRate != null) {
      if (builder.currency == null) {
        builder.currency = builder.overnightRate.getIndex().getCurrency();
      }
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the Overnight index that the future is based on.
   * 
   * @return the Overnight index
   */
  public OvernightIndex getIndex() {
    return overnightRate.getIndex();
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code ResolvedOvernightFuture}.
   * @return the meta-bean, not null
   */
  public static ResolvedOvernightFuture.Meta meta() {
    return ResolvedOvernightFuture.Meta.INSTANCE;
  }

  static {
    MetaBean.register(ResolvedOvernightFuture.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ResolvedOvernightFuture.Builder builder() {
    return new ResolvedOvernightFuture.Builder();
  }

  /**
   * Creates an instance.
   * @param securityId  the value of the property, not null
   * @param currency  the value of the property, not null
   * @param notional  the value of the property
   * @param accrualFactor  the value of the property
   * @param lastTradeDate  the value of the property, not null
   * @param overnightRate  the value of the property, not null
   * @param rounding  the value of the property, not null
   */
  ResolvedOvernightFuture(
      SecurityId securityId,
      Currency currency,
      double notional,
      double accrualFactor,
      LocalDate lastTradeDate,
      OvernightRateComputation overnightRate,
      Rounding rounding) {
    JodaBeanUtils.notNull(securityId, "securityId");
    JodaBeanUtils.notNull(currency, "currency");
    ArgChecker.notNegativeOrZero(notional, "notional");
    ArgChecker.notNegativeOrZero(accrualFactor, "accrualFactor");
    JodaBeanUtils.notNull(lastTradeDate, "lastTradeDate");
    JodaBeanUtils.notNull(overnightRate, "overnightRate");
    JodaBeanUtils.notNull(rounding, "rounding");
    this.securityId = securityId;
    this.currency = currency;
    this.notional = notional;
    this.accrualFactor = accrualFactor;
    this.lastTradeDate = lastTradeDate;
    this.overnightRate = overnightRate;
    this.rounding = rounding;
  }

  @Override
  public ResolvedOvernightFuture.Meta metaBean() {
    return ResolvedOvernightFuture.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the security identifier.
   * <p>
   * This identifier uniquely identifies the security within the system.
   * @return the value of the property, not null
   */
  public SecurityId getSecurityId() {
    return securityId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency that the future is traded in.
   * @return the value of the property, not null
   */
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the notional amount.
   * <p>
   * This is the full notional of the deposit, such as 5 million dollars.
   * The notional expressed here must be positive.
   * The currency of the notional is specified by {@code currency}.
   * @return the value of the property
   */
  public double getNotional() {
    return notional;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the accrual factor, defaulted from the index if not set.
   * <p>
   * This is the year fraction of the contract, typically 1/12 for a 30-day future.
   * The year fraction must be positive.
   * @return the value of the property
   */
  public double getAccrualFactor() {
    return accrualFactor;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the last date of trading.
   * <p>
   * This must be a valid business day on the fixing calendar of {@code index}.
   * The last trade date is typically the last business day of the month.
   * @return the value of the property, not null
   */
  public LocalDate getLastTradeDate() {
    return lastTradeDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the Overnight rate observation.
   * <p>
   * The future is based on this index.
   * It will be a well known market index such as 'USD-FED-FUND'.
   * @return the value of the property, not null
   */
  public OvernightRateComputation getOvernightRate() {
    return overnightRate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the definition of how to round the futures price, defaulted to no rounding.
   * <p>
   * The price is represented in decimal form, not percentage form.
   * As such, the decimal places expressed by the rounding refers to this decimal form.
   * For example, the common market price of 99.7125 for a 0.2875% rate is
   * represented as 0.997125 which has 6 decimal places.
   * @return the value of the property, not null
   */
  public Rounding getRounding() {
    return rounding;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      ResolvedOvernightFuture other = (ResolvedOvernightFuture) obj;
      return JodaBeanUtils.equal(securityId, other.securityId) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(notional, other.notional) &&
          JodaBeanUtils.equal(accrualFactor, other.accrualFactor) &&
          JodaBeanUtils.equal(lastTradeDate, other.lastTradeDate) &&
          JodaBeanUtils.equal(overnightRate, other.overnightRate) &&
          JodaBeanUtils.equal(rounding, other.rounding);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(securityId);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(notional);
    hash = hash * 31 + JodaBeanUtils.hashCode(accrualFactor);
    hash = hash * 31 + JodaBeanUtils.hashCode(lastTradeDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(overnightRate);
    hash = hash * 31 + JodaBeanUtils.hashCode(rounding);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(256);
    buf.append("ResolvedOvernightFuture{");
    buf.append("securityId").append('=').append(securityId).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("notional").append('=').append(notional).append(',').append(' ');
    buf.append("accrualFactor").append('=').append(accrualFactor).append(',').append(' ');
    buf.append("lastTradeDate").append('=').append(lastTradeDate).append(',').append(' ');
    buf.append("overnightRate").append('=').append(overnightRate).append(',').append(' ');
    buf.append("rounding").append('=').append(JodaBeanUtils.toString(rounding));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ResolvedOvernightFuture}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code securityId} property.
     */
    private final MetaProperty<SecurityId> securityId = DirectMetaProperty.ofImmutable(
        this, "securityId", ResolvedOvernightFuture.class, SecurityId.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", ResolvedOvernightFuture.class, Currency.class);
    /**
     * The meta-property for the {@code notional} property.
     */
    private final MetaProperty<Double> notional = DirectMetaProperty.ofImmutable(
        this, "notional", ResolvedOvernightFuture.class, Double.TYPE);
    /**
     * The meta-property for the {@code accrualFactor} property.
     */
    private final MetaProperty<Double> accrualFactor = DirectMetaProperty.ofImmutable(
        this, "accrualFactor", ResolvedOvernightFuture.class, Double.TYPE);
    /**
     * The meta-property for the {@code lastTradeDate} property.
     */
    private final MetaProperty<LocalDate> lastTradeDate = DirectMetaProperty.ofImmutable(
        this, "lastTradeDate", ResolvedOvernightFuture.class, LocalDate.class);
    /**
     * The meta-property for the {@code overnightRate} property.
     */
    private final MetaProperty<OvernightRateComputation> overnightRate = DirectMetaProperty.ofImmutable(
        this, "overnightRate", ResolvedOvernightFuture.class, OvernightRateComputation.class);
    /**
     * The meta-property for the {@code rounding} property.
     */
    private final MetaProperty<Rounding> rounding = DirectMetaProperty.ofImmutable(
        this, "rounding", ResolvedOvernightFuture.class, Rounding.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "securityId",
        "currency",
        "notional",
        "accrualFactor",
        "lastTradeDate",
        "overnightRate",
        "rounding");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1574023291:  // securityId
          return securityId;
        case 575402001:  // currency
          return currency;
        case 1585636160:  // notional
          return notional;
        case -1540322338:  // accrualFactor
          return accrualFactor;
        case -1041950404:  // lastTradeDate
          return lastTradeDate;
        case -821605692:  // overnightRate
          return overnightRate;
        case -142444:  // rounding
          return rounding;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ResolvedOvernightFuture.Builder builder() {
      return new ResolvedOvernightFuture.Builder();
    }

    @Override
    public Class<? extends ResolvedOvernightFuture> beanType() {
      return ResolvedOvernightFuture.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code securityId} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SecurityId> securityId() {
      return securityId;
    }

    /**
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    /**
     * The meta-property for the {@code notional} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> notional() {
      return notional;
    }

    /**
     * The meta-property for the {@code accrualFactor} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> accrualFactor() {
      return accrualFactor;
    }

    /**
     * The meta-property for the {@code lastTradeDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> lastTradeDate() {
      return lastTradeDate;
    }

    /**
     * The meta-property for the {@code overnightRate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<OvernightRateComputation> overnightRate() {
      return overnightRate;
    }

    /**
     * The meta-property for the {@code rounding} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Rounding> rounding() {
      return rounding;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1574023291:  // securityId
          return ((ResolvedOvernightFuture) bean).getSecurityId();
        case 575402001:  // currency
          return ((ResolvedOvernightFuture) bean).getCurrency();
        case 1585636160:  // notional
          return ((ResolvedOvernightFuture) bean).getNotional();
        case -1540322338:  // accrualFactor
          return ((ResolvedOvernightFuture) bean).getAccrualFactor();
        case -1041950404:  // lastTradeDate
          return ((ResolvedOvernightFuture) bean).getLastTradeDate();
        case -821605692:  // overnightRate
          return ((ResolvedOvernightFuture) bean).getOvernightRate();
        case -142444:  // rounding
          return ((ResolvedOvernightFuture) bean).getRounding();
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
   * The bean-builder for {@code ResolvedOvernightFuture}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ResolvedOvernightFuture> {

    private SecurityId securityId;
    private Currency currency;
    private double notional;
    private double accrualFactor;
    private LocalDate lastTradeDate;
    private OvernightRateComputation overnightRate;
    private Rounding rounding;

    /**
     * Restricted constructor.
     */
    private Builder() {
      applyDefaults(this);
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ResolvedOvernightFuture beanToCopy) {
      this.securityId = beanToCopy.getSecurityId();
      this.currency = beanToCopy.getCurrency();
      this.notional = beanToCopy.getNotional();
      this.accrualFactor = beanToCopy.getAccrualFactor();
      this.lastTradeDate = beanToCopy.getLastTradeDate();
      this.overnightRate = beanToCopy.getOvernightRate();
      this.rounding = beanToCopy.getRounding();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1574023291:  // securityId
          return securityId;
        case 575402001:  // currency
          return currency;
        case 1585636160:  // notional
          return notional;
        case -1540322338:  // accrualFactor
          return accrualFactor;
        case -1041950404:  // lastTradeDate
          return lastTradeDate;
        case -821605692:  // overnightRate
          return overnightRate;
        case -142444:  // rounding
          return rounding;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 1574023291:  // securityId
          this.securityId = (SecurityId) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 1585636160:  // notional
          this.notional = (Double) newValue;
          break;
        case -1540322338:  // accrualFactor
          this.accrualFactor = (Double) newValue;
          break;
        case -1041950404:  // lastTradeDate
          this.lastTradeDate = (LocalDate) newValue;
          break;
        case -821605692:  // overnightRate
          this.overnightRate = (OvernightRateComputation) newValue;
          break;
        case -142444:  // rounding
          this.rounding = (Rounding) newValue;
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
    public ResolvedOvernightFuture build() {
      preBuild(this);
      return new ResolvedOvernightFuture(
          securityId,
          currency,
          notional,
          accrualFactor,
          lastTradeDate,
          overnightRate,
          rounding);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the security identifier.
     * <p>
     * This identifier uniquely identifies the security within the system.
     * @param securityId  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder securityId(SecurityId securityId) {
      JodaBeanUtils.notNull(securityId, "securityId");
      this.securityId = securityId;
      return this;
    }

    /**
     * Sets the currency that the future is traded in.
     * @param currency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder currency(Currency currency) {
      JodaBeanUtils.notNull(currency, "currency");
      this.currency = currency;
      return this;
    }

    /**
     * Sets the notional amount.
     * <p>
     * This is the full notional of the deposit, such as 5 million dollars.
     * The notional expressed here must be positive.
     * The currency of the notional is specified by {@code currency}.
     * @param notional  the new value
     * @return this, for chaining, not null
     */
    public Builder notional(double notional) {
      ArgChecker.notNegativeOrZero(notional, "notional");
      this.notional = notional;
      return this;
    }

    /**
     * Sets the accrual factor, defaulted from the index if not set.
     * <p>
     * This is the year fraction of the contract, typically 1/12 for a 30-day future.
     * The year fraction must be positive.
     * @param accrualFactor  the new value
     * @return this, for chaining, not null
     */
    public Builder accrualFactor(double accrualFactor) {
      ArgChecker.notNegativeOrZero(accrualFactor, "accrualFactor");
      this.accrualFactor = accrualFactor;
      return this;
    }

    /**
     * Sets the last date of trading.
     * <p>
     * This must be a valid business day on the fixing calendar of {@code index}.
     * The last trade date is typically the last business day of the month.
     * @param lastTradeDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder lastTradeDate(LocalDate lastTradeDate) {
      JodaBeanUtils.notNull(lastTradeDate, "lastTradeDate");
      this.lastTradeDate = lastTradeDate;
      return this;
    }

    /**
     * Sets the Overnight rate observation.
     * <p>
     * The future is based on this index.
     * It will be a well known market index such as 'USD-FED-FUND'.
     * @param overnightRate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder overnightRate(OvernightRateComputation overnightRate) {
      JodaBeanUtils.notNull(overnightRate, "overnightRate");
      this.overnightRate = overnightRate;
      return this;
    }

    /**
     * Sets the definition of how to round the futures price, defaulted to no rounding.
     * <p>
     * The price is represented in decimal form, not percentage form.
     * As such, the decimal places expressed by the rounding refers to this decimal form.
     * For example, the common market price of 99.7125 for a 0.2875% rate is
     * represented as 0.997125 which has 6 decimal places.
     * @param rounding  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder rounding(Rounding rounding) {
      JodaBeanUtils.notNull(rounding, "rounding");
      this.rounding = rounding;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(256);
      buf.append("ResolvedOvernightFuture.Builder{");
      buf.append("securityId").append('=').append(JodaBeanUtils.toString(securityId)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("accrualFactor").append('=').append(JodaBeanUtils.toString(accrualFactor)).append(',').append(' ');
      buf.append("lastTradeDate").append('=').append(JodaBeanUtils.toString(lastTradeDate)).append(',').append(' ');
      buf.append("overnightRate").append('=').append(JodaBeanUtils.toString(overnightRate)).append(',').append(' ');
      buf.append("rounding").append('=').append(JodaBeanUtils.toString(rounding));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
