/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.capfloor;

import static com.google.common.base.MoreObjects.firstNonNull;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.OptionalDouble;

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutablePreBuild;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.rate.OvernightCompoundedRateComputation;

/**
 * A period over which an binary caplet/floorlet on overnight composition in-arrears is paid. 
 * <p>
 * The pay-offs are, for an index composition 'I', a year fraction 'a' and an amount 'N'<br>
 * Overnight binary caplet: N * a * ( (I > K)?1:0 ) ; K=caplet<br>
 * Overnight binary floorlet: N * a * ( (I < K)?1:0 ) ; K=floorlet
 * <p>
 * The payoff depend on the level of the compounded rate over the period. The option is 
 * of Asian type with the averaging mechanism given by the composition.
 */

@BeanDefinition
public final class OvernightInArrearsCapletFloorletBinaryPeriod
    implements ImmutableBean, Serializable {

  /**
   * The primary currency of the payment period.
   * <p>
   * The amounts of the notional are usually expressed in terms of this currency,
   * however they can be converted from amounts in a different currency.
   */
  @PropertyDefinition(validate = "notNull")
  private final Currency currency;
  /**
   * The fixed amount when the option is in-the-money, positive if receiving (long), negative if paying (short).
   * <p>
   * The currency of the notional is specified by {@code currency}.
   */
  @PropertyDefinition
  private final double amount;
  /**
   * The start date of the payment period.
   * <p>
   * This is the first date in the period.
   * If the schedule adjusts for business days, then this is the adjusted date.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate startDate;
  /**
   * The end date of the payment period.
   * <p>
   * This is the last date in the period.
   * If the schedule adjusts for business days, then this is the adjusted date.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate endDate;
  /**
   * The unadjusted start date.
   * <p>
   * The start date before any business day adjustment is applied.
   * <p>
   * When building, this will default to the start date if not specified.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate unadjustedStartDate;
  /**
   * The unadjusted end date.
   * <p>
   * The end date before any business day adjustment is applied.
   * <p>
   * When building, this will default to the end date if not specified.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate unadjustedEndDate;
  /**
   * The year fraction that the accrual period represents.
   * <p>
   * The value is usually calculated using a {@link DayCount} which may be different to that of the index.
   * Typically the value will be close to 1 for one year and close to 0.5 for six months.
   * The fraction may be greater than 1, but not less than 0.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegative")
  private final double yearFraction;
  /**
   * The date that payment occurs.
   * <p>
   * If the schedule adjusts for business days, then this is the adjusted date.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate paymentDate;
  /**
   * The optional caplet strike.
   * <p>
   * This defines the strike value of a caplet.
   * <p>
   * If the period is not a caplet, this field will be absent.
   */
  @PropertyDefinition(get = "optional")
  private final Double caplet;
  /**
   * The optional floorlet strike.
   * <p>
   * This defines the strike value of a floorlet.
   * <p>
   * If the period is not a floorlet, this field will be absent.
   */
  @PropertyDefinition(get = "optional")
  private final Double floorlet;
  /**
   * The rate to be observed.
   * <p>
   * The value of the period is based on this overnight compounded rate.
   */
  @PropertyDefinition(validate = "notNull")
  private final OvernightCompoundedRateComputation overnightRate;

  //-------------------------------------------------------------------------
  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.overnightRate != null) {
      OvernightIndex index = builder.overnightRate.getIndex();
      if (builder.currency == null) {
        builder.currency = index.getCurrency();
      }
    }
    if (builder.paymentDate == null) {
      builder.paymentDate = builder.endDate;
    }
    if (builder.unadjustedStartDate == null) {
      builder.unadjustedStartDate = builder.startDate;
    }
    if (builder.unadjustedEndDate == null) {
      builder.unadjustedEndDate = builder.endDate;
    }
    ArgChecker.isFalse(builder.caplet != null && builder.floorlet != null,
        "Only caplet or floorlet must be set, not both");
    ArgChecker.isFalse(builder.caplet == null && builder.floorlet == null, "Either caplet or floorlet must be set");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the Ibor index.
   * 
   * @return the ibor index
   */
  public OvernightIndex getIndex() {
    return overnightRate.getIndex();
  }

  /**
   * Gets the strike value.
   * 
   * @return the strike
   */
  public double getStrike() {
    return firstNonNull(caplet, floorlet);
  }

  /**
   * Gets put or call.
   * <p>
   * CALL is returned for a caplet, whereas PUT is returned for a floorlet.
   * 
   * @return put or call
   */
  public PutCall getPutCall() {
    return getCaplet().isPresent() ? PutCall.CALL : PutCall.PUT;
  }

  /**
   * Returns the binary caplet/floorlet payoff for a given compounded rate.
   * 
   * @param fixing  the compounded rate
   * @return the payoff
   */
  public CurrencyAmount payoff(double fixing) {
    if (getCaplet().isPresent()) { // caplet
      return CurrencyAmount.of(currency, (fixing > caplet) ? amount * yearFraction : 0);
    }
    // floorlet
    return CurrencyAmount.of(currency, (fixing < floorlet) ? amount * yearFraction : 0);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code OvernightInArrearsCapletFloorletBinaryPeriod}.
   * @return the meta-bean, not null
   */
  public static OvernightInArrearsCapletFloorletBinaryPeriod.Meta meta() {
    return OvernightInArrearsCapletFloorletBinaryPeriod.Meta.INSTANCE;
  }

  static {
    MetaBean.register(OvernightInArrearsCapletFloorletBinaryPeriod.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static OvernightInArrearsCapletFloorletBinaryPeriod.Builder builder() {
    return new OvernightInArrearsCapletFloorletBinaryPeriod.Builder();
  }

  private OvernightInArrearsCapletFloorletBinaryPeriod(
      Currency currency,
      double amount,
      LocalDate startDate,
      LocalDate endDate,
      LocalDate unadjustedStartDate,
      LocalDate unadjustedEndDate,
      double yearFraction,
      LocalDate paymentDate,
      Double caplet,
      Double floorlet,
      OvernightCompoundedRateComputation overnightRate) {
    JodaBeanUtils.notNull(currency, "currency");
    JodaBeanUtils.notNull(startDate, "startDate");
    JodaBeanUtils.notNull(endDate, "endDate");
    JodaBeanUtils.notNull(unadjustedStartDate, "unadjustedStartDate");
    JodaBeanUtils.notNull(unadjustedEndDate, "unadjustedEndDate");
    ArgChecker.notNegative(yearFraction, "yearFraction");
    JodaBeanUtils.notNull(paymentDate, "paymentDate");
    JodaBeanUtils.notNull(overnightRate, "overnightRate");
    this.currency = currency;
    this.amount = amount;
    this.startDate = startDate;
    this.endDate = endDate;
    this.unadjustedStartDate = unadjustedStartDate;
    this.unadjustedEndDate = unadjustedEndDate;
    this.yearFraction = yearFraction;
    this.paymentDate = paymentDate;
    this.caplet = caplet;
    this.floorlet = floorlet;
    this.overnightRate = overnightRate;
  }

  @Override
  public OvernightInArrearsCapletFloorletBinaryPeriod.Meta metaBean() {
    return OvernightInArrearsCapletFloorletBinaryPeriod.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the primary currency of the payment period.
   * <p>
   * The amounts of the notional are usually expressed in terms of this currency,
   * however they can be converted from amounts in a different currency.
   * @return the value of the property, not null
   */
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the fixed amount when the option is in-the-money, positive if receiving (long), negative if paying (short).
   * <p>
   * The currency of the notional is specified by {@code currency}.
   * @return the value of the property
   */
  public double getAmount() {
    return amount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the start date of the payment period.
   * <p>
   * This is the first date in the period.
   * If the schedule adjusts for business days, then this is the adjusted date.
   * @return the value of the property, not null
   */
  public LocalDate getStartDate() {
    return startDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the end date of the payment period.
   * <p>
   * This is the last date in the period.
   * If the schedule adjusts for business days, then this is the adjusted date.
   * @return the value of the property, not null
   */
  public LocalDate getEndDate() {
    return endDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the unadjusted start date.
   * <p>
   * The start date before any business day adjustment is applied.
   * <p>
   * When building, this will default to the start date if not specified.
   * @return the value of the property, not null
   */
  public LocalDate getUnadjustedStartDate() {
    return unadjustedStartDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the unadjusted end date.
   * <p>
   * The end date before any business day adjustment is applied.
   * <p>
   * When building, this will default to the end date if not specified.
   * @return the value of the property, not null
   */
  public LocalDate getUnadjustedEndDate() {
    return unadjustedEndDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the year fraction that the accrual period represents.
   * <p>
   * The value is usually calculated using a {@link DayCount} which may be different to that of the index.
   * Typically the value will be close to 1 for one year and close to 0.5 for six months.
   * The fraction may be greater than 1, but not less than 0.
   * @return the value of the property
   */
  public double getYearFraction() {
    return yearFraction;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the date that payment occurs.
   * <p>
   * If the schedule adjusts for business days, then this is the adjusted date.
   * @return the value of the property, not null
   */
  public LocalDate getPaymentDate() {
    return paymentDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the optional caplet strike.
   * <p>
   * This defines the strike value of a caplet.
   * <p>
   * If the period is not a caplet, this field will be absent.
   * @return the optional value of the property, not null
   */
  public OptionalDouble getCaplet() {
    return caplet != null ? OptionalDouble.of(caplet) : OptionalDouble.empty();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the optional floorlet strike.
   * <p>
   * This defines the strike value of a floorlet.
   * <p>
   * If the period is not a floorlet, this field will be absent.
   * @return the optional value of the property, not null
   */
  public OptionalDouble getFloorlet() {
    return floorlet != null ? OptionalDouble.of(floorlet) : OptionalDouble.empty();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the rate to be observed.
   * <p>
   * The value of the period is based on this overnight compounded rate.
   * @return the value of the property, not null
   */
  public OvernightCompoundedRateComputation getOvernightRate() {
    return overnightRate;
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
      OvernightInArrearsCapletFloorletBinaryPeriod other = (OvernightInArrearsCapletFloorletBinaryPeriod) obj;
      return JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(amount, other.amount) &&
          JodaBeanUtils.equal(startDate, other.startDate) &&
          JodaBeanUtils.equal(endDate, other.endDate) &&
          JodaBeanUtils.equal(unadjustedStartDate, other.unadjustedStartDate) &&
          JodaBeanUtils.equal(unadjustedEndDate, other.unadjustedEndDate) &&
          JodaBeanUtils.equal(yearFraction, other.yearFraction) &&
          JodaBeanUtils.equal(paymentDate, other.paymentDate) &&
          JodaBeanUtils.equal(caplet, other.caplet) &&
          JodaBeanUtils.equal(floorlet, other.floorlet) &&
          JodaBeanUtils.equal(overnightRate, other.overnightRate);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(amount);
    hash = hash * 31 + JodaBeanUtils.hashCode(startDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(endDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(unadjustedStartDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(unadjustedEndDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(yearFraction);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(caplet);
    hash = hash * 31 + JodaBeanUtils.hashCode(floorlet);
    hash = hash * 31 + JodaBeanUtils.hashCode(overnightRate);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(384);
    buf.append("OvernightInArrearsCapletFloorletBinaryPeriod{");
    buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
    buf.append("amount").append('=').append(JodaBeanUtils.toString(amount)).append(',').append(' ');
    buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ');
    buf.append("endDate").append('=').append(JodaBeanUtils.toString(endDate)).append(',').append(' ');
    buf.append("unadjustedStartDate").append('=').append(JodaBeanUtils.toString(unadjustedStartDate)).append(',').append(' ');
    buf.append("unadjustedEndDate").append('=').append(JodaBeanUtils.toString(unadjustedEndDate)).append(',').append(' ');
    buf.append("yearFraction").append('=').append(JodaBeanUtils.toString(yearFraction)).append(',').append(' ');
    buf.append("paymentDate").append('=').append(JodaBeanUtils.toString(paymentDate)).append(',').append(' ');
    buf.append("caplet").append('=').append(JodaBeanUtils.toString(caplet)).append(',').append(' ');
    buf.append("floorlet").append('=').append(JodaBeanUtils.toString(floorlet)).append(',').append(' ');
    buf.append("overnightRate").append('=').append(JodaBeanUtils.toString(overnightRate));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code OvernightInArrearsCapletFloorletBinaryPeriod}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", OvernightInArrearsCapletFloorletBinaryPeriod.class, Currency.class);
    /**
     * The meta-property for the {@code amount} property.
     */
    private final MetaProperty<Double> amount = DirectMetaProperty.ofImmutable(
        this, "amount", OvernightInArrearsCapletFloorletBinaryPeriod.class, Double.TYPE);
    /**
     * The meta-property for the {@code startDate} property.
     */
    private final MetaProperty<LocalDate> startDate = DirectMetaProperty.ofImmutable(
        this, "startDate", OvernightInArrearsCapletFloorletBinaryPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code endDate} property.
     */
    private final MetaProperty<LocalDate> endDate = DirectMetaProperty.ofImmutable(
        this, "endDate", OvernightInArrearsCapletFloorletBinaryPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code unadjustedStartDate} property.
     */
    private final MetaProperty<LocalDate> unadjustedStartDate = DirectMetaProperty.ofImmutable(
        this, "unadjustedStartDate", OvernightInArrearsCapletFloorletBinaryPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code unadjustedEndDate} property.
     */
    private final MetaProperty<LocalDate> unadjustedEndDate = DirectMetaProperty.ofImmutable(
        this, "unadjustedEndDate", OvernightInArrearsCapletFloorletBinaryPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code yearFraction} property.
     */
    private final MetaProperty<Double> yearFraction = DirectMetaProperty.ofImmutable(
        this, "yearFraction", OvernightInArrearsCapletFloorletBinaryPeriod.class, Double.TYPE);
    /**
     * The meta-property for the {@code paymentDate} property.
     */
    private final MetaProperty<LocalDate> paymentDate = DirectMetaProperty.ofImmutable(
        this, "paymentDate", OvernightInArrearsCapletFloorletBinaryPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code caplet} property.
     */
    private final MetaProperty<Double> caplet = DirectMetaProperty.ofImmutable(
        this, "caplet", OvernightInArrearsCapletFloorletBinaryPeriod.class, Double.class);
    /**
     * The meta-property for the {@code floorlet} property.
     */
    private final MetaProperty<Double> floorlet = DirectMetaProperty.ofImmutable(
        this, "floorlet", OvernightInArrearsCapletFloorletBinaryPeriod.class, Double.class);
    /**
     * The meta-property for the {@code overnightRate} property.
     */
    private final MetaProperty<OvernightCompoundedRateComputation> overnightRate = DirectMetaProperty.ofImmutable(
        this, "overnightRate", OvernightInArrearsCapletFloorletBinaryPeriod.class, OvernightCompoundedRateComputation.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "currency",
        "amount",
        "startDate",
        "endDate",
        "unadjustedStartDate",
        "unadjustedEndDate",
        "yearFraction",
        "paymentDate",
        "caplet",
        "floorlet",
        "overnightRate");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return currency;
        case -1413853096:  // amount
          return amount;
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case 1457691881:  // unadjustedStartDate
          return unadjustedStartDate;
        case 31758114:  // unadjustedEndDate
          return unadjustedEndDate;
        case -1731780257:  // yearFraction
          return yearFraction;
        case -1540873516:  // paymentDate
          return paymentDate;
        case -1367656183:  // caplet
          return caplet;
        case 2022994575:  // floorlet
          return floorlet;
        case -821605692:  // overnightRate
          return overnightRate;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public OvernightInArrearsCapletFloorletBinaryPeriod.Builder builder() {
      return new OvernightInArrearsCapletFloorletBinaryPeriod.Builder();
    }

    @Override
    public Class<? extends OvernightInArrearsCapletFloorletBinaryPeriod> beanType() {
      return OvernightInArrearsCapletFloorletBinaryPeriod.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    /**
     * The meta-property for the {@code amount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> amount() {
      return amount;
    }

    /**
     * The meta-property for the {@code startDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> startDate() {
      return startDate;
    }

    /**
     * The meta-property for the {@code endDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> endDate() {
      return endDate;
    }

    /**
     * The meta-property for the {@code unadjustedStartDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> unadjustedStartDate() {
      return unadjustedStartDate;
    }

    /**
     * The meta-property for the {@code unadjustedEndDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> unadjustedEndDate() {
      return unadjustedEndDate;
    }

    /**
     * The meta-property for the {@code yearFraction} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> yearFraction() {
      return yearFraction;
    }

    /**
     * The meta-property for the {@code paymentDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> paymentDate() {
      return paymentDate;
    }

    /**
     * The meta-property for the {@code caplet} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> caplet() {
      return caplet;
    }

    /**
     * The meta-property for the {@code floorlet} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> floorlet() {
      return floorlet;
    }

    /**
     * The meta-property for the {@code overnightRate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<OvernightCompoundedRateComputation> overnightRate() {
      return overnightRate;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return ((OvernightInArrearsCapletFloorletBinaryPeriod) bean).getCurrency();
        case -1413853096:  // amount
          return ((OvernightInArrearsCapletFloorletBinaryPeriod) bean).getAmount();
        case -2129778896:  // startDate
          return ((OvernightInArrearsCapletFloorletBinaryPeriod) bean).getStartDate();
        case -1607727319:  // endDate
          return ((OvernightInArrearsCapletFloorletBinaryPeriod) bean).getEndDate();
        case 1457691881:  // unadjustedStartDate
          return ((OvernightInArrearsCapletFloorletBinaryPeriod) bean).getUnadjustedStartDate();
        case 31758114:  // unadjustedEndDate
          return ((OvernightInArrearsCapletFloorletBinaryPeriod) bean).getUnadjustedEndDate();
        case -1731780257:  // yearFraction
          return ((OvernightInArrearsCapletFloorletBinaryPeriod) bean).getYearFraction();
        case -1540873516:  // paymentDate
          return ((OvernightInArrearsCapletFloorletBinaryPeriod) bean).getPaymentDate();
        case -1367656183:  // caplet
          return ((OvernightInArrearsCapletFloorletBinaryPeriod) bean).caplet;
        case 2022994575:  // floorlet
          return ((OvernightInArrearsCapletFloorletBinaryPeriod) bean).floorlet;
        case -821605692:  // overnightRate
          return ((OvernightInArrearsCapletFloorletBinaryPeriod) bean).getOvernightRate();
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
   * The bean-builder for {@code OvernightInArrearsCapletFloorletBinaryPeriod}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<OvernightInArrearsCapletFloorletBinaryPeriod> {

    private Currency currency;
    private double amount;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate unadjustedStartDate;
    private LocalDate unadjustedEndDate;
    private double yearFraction;
    private LocalDate paymentDate;
    private Double caplet;
    private Double floorlet;
    private OvernightCompoundedRateComputation overnightRate;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(OvernightInArrearsCapletFloorletBinaryPeriod beanToCopy) {
      this.currency = beanToCopy.getCurrency();
      this.amount = beanToCopy.getAmount();
      this.startDate = beanToCopy.getStartDate();
      this.endDate = beanToCopy.getEndDate();
      this.unadjustedStartDate = beanToCopy.getUnadjustedStartDate();
      this.unadjustedEndDate = beanToCopy.getUnadjustedEndDate();
      this.yearFraction = beanToCopy.getYearFraction();
      this.paymentDate = beanToCopy.getPaymentDate();
      this.caplet = beanToCopy.caplet;
      this.floorlet = beanToCopy.floorlet;
      this.overnightRate = beanToCopy.getOvernightRate();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return currency;
        case -1413853096:  // amount
          return amount;
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case 1457691881:  // unadjustedStartDate
          return unadjustedStartDate;
        case 31758114:  // unadjustedEndDate
          return unadjustedEndDate;
        case -1731780257:  // yearFraction
          return yearFraction;
        case -1540873516:  // paymentDate
          return paymentDate;
        case -1367656183:  // caplet
          return caplet;
        case 2022994575:  // floorlet
          return floorlet;
        case -821605692:  // overnightRate
          return overnightRate;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case -1413853096:  // amount
          this.amount = (Double) newValue;
          break;
        case -2129778896:  // startDate
          this.startDate = (LocalDate) newValue;
          break;
        case -1607727319:  // endDate
          this.endDate = (LocalDate) newValue;
          break;
        case 1457691881:  // unadjustedStartDate
          this.unadjustedStartDate = (LocalDate) newValue;
          break;
        case 31758114:  // unadjustedEndDate
          this.unadjustedEndDate = (LocalDate) newValue;
          break;
        case -1731780257:  // yearFraction
          this.yearFraction = (Double) newValue;
          break;
        case -1540873516:  // paymentDate
          this.paymentDate = (LocalDate) newValue;
          break;
        case -1367656183:  // caplet
          this.caplet = (Double) newValue;
          break;
        case 2022994575:  // floorlet
          this.floorlet = (Double) newValue;
          break;
        case -821605692:  // overnightRate
          this.overnightRate = (OvernightCompoundedRateComputation) newValue;
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
    public OvernightInArrearsCapletFloorletBinaryPeriod build() {
      preBuild(this);
      return new OvernightInArrearsCapletFloorletBinaryPeriod(
          currency,
          amount,
          startDate,
          endDate,
          unadjustedStartDate,
          unadjustedEndDate,
          yearFraction,
          paymentDate,
          caplet,
          floorlet,
          overnightRate);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the primary currency of the payment period.
     * <p>
     * The amounts of the notional are usually expressed in terms of this currency,
     * however they can be converted from amounts in a different currency.
     * @param currency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder currency(Currency currency) {
      JodaBeanUtils.notNull(currency, "currency");
      this.currency = currency;
      return this;
    }

    /**
     * Sets the fixed amount when the option is in-the-money, positive if receiving (long), negative if paying (short).
     * <p>
     * The currency of the notional is specified by {@code currency}.
     * @param amount  the new value
     * @return this, for chaining, not null
     */
    public Builder amount(double amount) {
      this.amount = amount;
      return this;
    }

    /**
     * Sets the start date of the payment period.
     * <p>
     * This is the first date in the period.
     * If the schedule adjusts for business days, then this is the adjusted date.
     * @param startDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder startDate(LocalDate startDate) {
      JodaBeanUtils.notNull(startDate, "startDate");
      this.startDate = startDate;
      return this;
    }

    /**
     * Sets the end date of the payment period.
     * <p>
     * This is the last date in the period.
     * If the schedule adjusts for business days, then this is the adjusted date.
     * @param endDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder endDate(LocalDate endDate) {
      JodaBeanUtils.notNull(endDate, "endDate");
      this.endDate = endDate;
      return this;
    }

    /**
     * Sets the unadjusted start date.
     * <p>
     * The start date before any business day adjustment is applied.
     * <p>
     * When building, this will default to the start date if not specified.
     * @param unadjustedStartDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder unadjustedStartDate(LocalDate unadjustedStartDate) {
      JodaBeanUtils.notNull(unadjustedStartDate, "unadjustedStartDate");
      this.unadjustedStartDate = unadjustedStartDate;
      return this;
    }

    /**
     * Sets the unadjusted end date.
     * <p>
     * The end date before any business day adjustment is applied.
     * <p>
     * When building, this will default to the end date if not specified.
     * @param unadjustedEndDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder unadjustedEndDate(LocalDate unadjustedEndDate) {
      JodaBeanUtils.notNull(unadjustedEndDate, "unadjustedEndDate");
      this.unadjustedEndDate = unadjustedEndDate;
      return this;
    }

    /**
     * Sets the year fraction that the accrual period represents.
     * <p>
     * The value is usually calculated using a {@link DayCount} which may be different to that of the index.
     * Typically the value will be close to 1 for one year and close to 0.5 for six months.
     * The fraction may be greater than 1, but not less than 0.
     * @param yearFraction  the new value
     * @return this, for chaining, not null
     */
    public Builder yearFraction(double yearFraction) {
      ArgChecker.notNegative(yearFraction, "yearFraction");
      this.yearFraction = yearFraction;
      return this;
    }

    /**
     * Sets the date that payment occurs.
     * <p>
     * If the schedule adjusts for business days, then this is the adjusted date.
     * @param paymentDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentDate(LocalDate paymentDate) {
      JodaBeanUtils.notNull(paymentDate, "paymentDate");
      this.paymentDate = paymentDate;
      return this;
    }

    /**
     * Sets the optional caplet strike.
     * <p>
     * This defines the strike value of a caplet.
     * <p>
     * If the period is not a caplet, this field will be absent.
     * @param caplet  the new value
     * @return this, for chaining, not null
     */
    public Builder caplet(Double caplet) {
      this.caplet = caplet;
      return this;
    }

    /**
     * Sets the optional floorlet strike.
     * <p>
     * This defines the strike value of a floorlet.
     * <p>
     * If the period is not a floorlet, this field will be absent.
     * @param floorlet  the new value
     * @return this, for chaining, not null
     */
    public Builder floorlet(Double floorlet) {
      this.floorlet = floorlet;
      return this;
    }

    /**
     * Sets the rate to be observed.
     * <p>
     * The value of the period is based on this overnight compounded rate.
     * @param overnightRate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder overnightRate(OvernightCompoundedRateComputation overnightRate) {
      JodaBeanUtils.notNull(overnightRate, "overnightRate");
      this.overnightRate = overnightRate;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(384);
      buf.append("OvernightInArrearsCapletFloorletBinaryPeriod.Builder{");
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("amount").append('=').append(JodaBeanUtils.toString(amount)).append(',').append(' ');
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ');
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(endDate)).append(',').append(' ');
      buf.append("unadjustedStartDate").append('=').append(JodaBeanUtils.toString(unadjustedStartDate)).append(',').append(' ');
      buf.append("unadjustedEndDate").append('=').append(JodaBeanUtils.toString(unadjustedEndDate)).append(',').append(' ');
      buf.append("yearFraction").append('=').append(JodaBeanUtils.toString(yearFraction)).append(',').append(' ');
      buf.append("paymentDate").append('=').append(JodaBeanUtils.toString(paymentDate)).append(',').append(' ');
      buf.append("caplet").append('=').append(JodaBeanUtils.toString(caplet)).append(',').append(' ');
      buf.append("floorlet").append('=').append(JodaBeanUtils.toString(floorlet)).append(',').append(' ');
      buf.append("overnightRate").append('=').append(JodaBeanUtils.toString(overnightRate));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
