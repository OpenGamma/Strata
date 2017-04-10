/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.ImmutablePreBuild;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.value.Rounding;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.ResolvedProduct;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.rate.IborRateComputation;

/**
 * A futures contract based on an Ibor index, resolved for pricing.
 * <p>
 * This is the resolved form of {@link IborFuture} and is an input to the pricers.
 * Applications will typically create a {@code ResolvedIborFuture} from a {@code IborFuture}
 * using {@link IborFuture#resolve(ReferenceData)}.
 * <p>
 * A {@code ResolvedIborFuture} is bound to data that changes over time, such as holiday calendars.
 * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
 * Care must be taken when placing the resolved form in a cache or persistence layer.
 * 
 * <h4>Price</h4>
 * The price of an Ibor future is based on the interest rate of the underlying index.
 * It is defined as {@code (100 - percentRate)}.
 * <p>
 * Strata uses <i>decimal prices</i> for Ibor futures in the trade model, pricers and market data.
 * The decimal price is based on the decimal rate equivalent to the percentage.
 * For example, a price of 99.32 implies an interest rate of 0.68% which is represented in Strata by 0.9932.
 */
@BeanDefinition(constructorScope = "package")
public final class ResolvedIborFuture
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
   * This is the full notional of the deposit, such as 1 million dollars.
   * The notional expressed here must be positive.
   * The currency of the notional is specified by {@code currency}.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegativeOrZero")
  private final double notional;
  /**
   * The accrual factor, defaulted from the index if not set.
   * <p>
   * This is the year fraction of the contract, typically 0.25 for a 3 month deposit.
   * <p>
   * When building, this will default to the number of months in the index divided by 12
   * if not specified. However, if the index is not month-based, no defaulting will occur.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegativeOrZero")
  private final double accrualFactor;
  /**
   * The Ibor rate observation.
   * <p>
   * The future is based on this index.
   * It will be a well known market index such as 'USD-LIBOR-3M'.
   */
  @PropertyDefinition(validate = "notNull")
  private final IborRateComputation iborRate;
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
    if (builder.iborRate != null) {
      if (builder.accrualFactor == 0d && builder.iborRate.getIndex().getTenor().isMonthBased()) {
        builder.accrualFactor(builder.iborRate.getIndex().getTenor().getPeriod().toTotalMonths() / 12d);
      }
      if (builder.currency == null) {
        builder.currency = builder.iborRate.getIndex().getCurrency();
      }
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the Ibor index that the future is based on.
   * 
   * @return the Ibor index
   */
  public IborIndex getIndex() {
    return iborRate.getIndex();
  }

  /**
   * Gets the last date of trading, which is the same as the fixing date.
   * <p>
   * This is typically 2 business days before the IMM date (3rd Wednesday of the month).
   * By including this method, it allows for the possibility of a future where the fixing date
   * and last trade date differ.
   * 
   * @return the last trade date
   */
  public LocalDate getLastTradeDate() {
    return iborRate.getFixingDate();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ResolvedIborFuture}.
   * @return the meta-bean, not null
   */
  public static ResolvedIborFuture.Meta meta() {
    return ResolvedIborFuture.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ResolvedIborFuture.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ResolvedIborFuture.Builder builder() {
    return new ResolvedIborFuture.Builder();
  }

  /**
   * Creates an instance.
   * @param securityId  the value of the property, not null
   * @param currency  the value of the property, not null
   * @param notional  the value of the property
   * @param accrualFactor  the value of the property
   * @param iborRate  the value of the property, not null
   * @param rounding  the value of the property, not null
   */
  ResolvedIborFuture(
      SecurityId securityId,
      Currency currency,
      double notional,
      double accrualFactor,
      IborRateComputation iborRate,
      Rounding rounding) {
    JodaBeanUtils.notNull(securityId, "securityId");
    JodaBeanUtils.notNull(currency, "currency");
    ArgChecker.notNegativeOrZero(notional, "notional");
    ArgChecker.notNegativeOrZero(accrualFactor, "accrualFactor");
    JodaBeanUtils.notNull(iborRate, "iborRate");
    JodaBeanUtils.notNull(rounding, "rounding");
    this.securityId = securityId;
    this.currency = currency;
    this.notional = notional;
    this.accrualFactor = accrualFactor;
    this.iborRate = iborRate;
    this.rounding = rounding;
  }

  @Override
  public ResolvedIborFuture.Meta metaBean() {
    return ResolvedIborFuture.Meta.INSTANCE;
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
   * This is the full notional of the deposit, such as 1 million dollars.
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
   * This is the year fraction of the contract, typically 0.25 for a 3 month deposit.
   * <p>
   * When building, this will default to the number of months in the index divided by 12
   * if not specified. However, if the index is not month-based, no defaulting will occur.
   * @return the value of the property
   */
  public double getAccrualFactor() {
    return accrualFactor;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the Ibor rate observation.
   * <p>
   * The future is based on this index.
   * It will be a well known market index such as 'USD-LIBOR-3M'.
   * @return the value of the property, not null
   */
  public IborRateComputation getIborRate() {
    return iborRate;
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
      ResolvedIborFuture other = (ResolvedIborFuture) obj;
      return JodaBeanUtils.equal(securityId, other.securityId) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(notional, other.notional) &&
          JodaBeanUtils.equal(accrualFactor, other.accrualFactor) &&
          JodaBeanUtils.equal(iborRate, other.iborRate) &&
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
    hash = hash * 31 + JodaBeanUtils.hashCode(iborRate);
    hash = hash * 31 + JodaBeanUtils.hashCode(rounding);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(224);
    buf.append("ResolvedIborFuture{");
    buf.append("securityId").append('=').append(securityId).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("notional").append('=').append(notional).append(',').append(' ');
    buf.append("accrualFactor").append('=').append(accrualFactor).append(',').append(' ');
    buf.append("iborRate").append('=').append(iborRate).append(',').append(' ');
    buf.append("rounding").append('=').append(JodaBeanUtils.toString(rounding));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ResolvedIborFuture}.
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
        this, "securityId", ResolvedIborFuture.class, SecurityId.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", ResolvedIborFuture.class, Currency.class);
    /**
     * The meta-property for the {@code notional} property.
     */
    private final MetaProperty<Double> notional = DirectMetaProperty.ofImmutable(
        this, "notional", ResolvedIborFuture.class, Double.TYPE);
    /**
     * The meta-property for the {@code accrualFactor} property.
     */
    private final MetaProperty<Double> accrualFactor = DirectMetaProperty.ofImmutable(
        this, "accrualFactor", ResolvedIborFuture.class, Double.TYPE);
    /**
     * The meta-property for the {@code iborRate} property.
     */
    private final MetaProperty<IborRateComputation> iborRate = DirectMetaProperty.ofImmutable(
        this, "iborRate", ResolvedIborFuture.class, IborRateComputation.class);
    /**
     * The meta-property for the {@code rounding} property.
     */
    private final MetaProperty<Rounding> rounding = DirectMetaProperty.ofImmutable(
        this, "rounding", ResolvedIborFuture.class, Rounding.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "securityId",
        "currency",
        "notional",
        "accrualFactor",
        "iborRate",
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
        case -1621804100:  // iborRate
          return iborRate;
        case -142444:  // rounding
          return rounding;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ResolvedIborFuture.Builder builder() {
      return new ResolvedIborFuture.Builder();
    }

    @Override
    public Class<? extends ResolvedIborFuture> beanType() {
      return ResolvedIborFuture.class;
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
     * The meta-property for the {@code iborRate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IborRateComputation> iborRate() {
      return iborRate;
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
          return ((ResolvedIborFuture) bean).getSecurityId();
        case 575402001:  // currency
          return ((ResolvedIborFuture) bean).getCurrency();
        case 1585636160:  // notional
          return ((ResolvedIborFuture) bean).getNotional();
        case -1540322338:  // accrualFactor
          return ((ResolvedIborFuture) bean).getAccrualFactor();
        case -1621804100:  // iborRate
          return ((ResolvedIborFuture) bean).getIborRate();
        case -142444:  // rounding
          return ((ResolvedIborFuture) bean).getRounding();
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
   * The bean-builder for {@code ResolvedIborFuture}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ResolvedIborFuture> {

    private SecurityId securityId;
    private Currency currency;
    private double notional;
    private double accrualFactor;
    private IborRateComputation iborRate;
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
    private Builder(ResolvedIborFuture beanToCopy) {
      this.securityId = beanToCopy.getSecurityId();
      this.currency = beanToCopy.getCurrency();
      this.notional = beanToCopy.getNotional();
      this.accrualFactor = beanToCopy.getAccrualFactor();
      this.iborRate = beanToCopy.getIborRate();
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
        case -1621804100:  // iborRate
          return iborRate;
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
        case -1621804100:  // iborRate
          this.iborRate = (IborRateComputation) newValue;
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

    /**
     * @deprecated Use Joda-Convert in application code
     */
    @Override
    @Deprecated
    public Builder setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    /**
     * @deprecated Use Joda-Convert in application code
     */
    @Override
    @Deprecated
    public Builder setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    /**
     * @deprecated Loop in application code
     */
    @Override
    @Deprecated
    public Builder setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public ResolvedIborFuture build() {
      preBuild(this);
      return new ResolvedIborFuture(
          securityId,
          currency,
          notional,
          accrualFactor,
          iborRate,
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
     * This is the full notional of the deposit, such as 1 million dollars.
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
     * This is the year fraction of the contract, typically 0.25 for a 3 month deposit.
     * <p>
     * When building, this will default to the number of months in the index divided by 12
     * if not specified. However, if the index is not month-based, no defaulting will occur.
     * @param accrualFactor  the new value
     * @return this, for chaining, not null
     */
    public Builder accrualFactor(double accrualFactor) {
      ArgChecker.notNegativeOrZero(accrualFactor, "accrualFactor");
      this.accrualFactor = accrualFactor;
      return this;
    }

    /**
     * Sets the Ibor rate observation.
     * <p>
     * The future is based on this index.
     * It will be a well known market index such as 'USD-LIBOR-3M'.
     * @param iborRate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder iborRate(IborRateComputation iborRate) {
      JodaBeanUtils.notNull(iborRate, "iborRate");
      this.iborRate = iborRate;
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
      StringBuilder buf = new StringBuilder(224);
      buf.append("ResolvedIborFuture.Builder{");
      buf.append("securityId").append('=').append(JodaBeanUtils.toString(securityId)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("accrualFactor").append('=').append(JodaBeanUtils.toString(accrualFactor)).append(',').append(' ');
      buf.append("iborRate").append('=').append(JodaBeanUtils.toString(iborRate)).append(',').append(' ');
      buf.append("rounding").append('=').append(JodaBeanUtils.toString(rounding));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
