/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.deposit;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.Resolvable;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DateAdjuster;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.Product;
import com.opengamma.strata.product.common.BuySell;

/**
 * A term deposit.
 * <p>
 * A term deposit is a financial instrument that provides a fixed rate of interest on
 * an amount for a specific term.
 * For example, investing GBP 1,000 for 3 months at a 1% interest rate.
 * <p>
 * The instrument has two payments, one at the start date and one at the end date.
 * For example, investing  GBP 1,000 for 3 months implies an initial payment to the counterparty
 * of GBP 1,000 and a final payment from the counterparty of GBP 1,000 plus interest.
 */
@BeanDefinition
public final class TermDeposit
    implements Product, Resolvable<ResolvedTermDeposit>, ImmutableBean, Serializable {

  /**
   * Whether the term deposit is 'Buy' or 'Sell'.
   * <p>
   * A value of 'Buy' implies payment of the principal at the start date and receipt of the
   * principal plus interest at the end date. A value of 'Sell' implies the opposite.
   * In other words, 'Buy' refers to buying the service of "money storage", with the benefit
   * of receiving interest.
   */
  @PropertyDefinition(validate = "notNull")
  private final BuySell buySell;
  /**
   * The primary currency.
   * <p>
   * This is the currency of the term deposit and the currency that payment is made in.
   */
  @PropertyDefinition(validate = "notNull")
  private final Currency currency;
  /**
   * The notional amount.
   * <p>
   * The notional represents the principal amount, and must be non-negative.
   * The currency of the notional is specified by {@code currency}.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegative")
  private final double notional;
  /**
   * The start date of the deposit.
   * <p>
   * Interest accrues from this date.
   * This date is typically set to be a valid business day.
   * Optionally, the {@code businessDayAdjustment} property may be set to provide a rule for adjustment.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate startDate;
  /**
   * The end date of the deposit.
   * <p>
   * Interest accrues until this date.
   * This date is typically set to be a valid business day.
   * Optionally, the {@code businessDayAdjustment} property may be set to provide a rule for adjustment.
   * This date must be after the start date.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate endDate;
  /**
   * The business day adjustment to apply to the start and end date, optional.
   * <p>
   * The start and end date are typically defined as valid business days and thus
   * do not need to be adjusted. If this optional property is present, then the
   * start and end date will be adjusted as defined here.
   */
  @PropertyDefinition(get = "optional")
  private final BusinessDayAdjustment businessDayAdjustment;
  /**
   * The day count convention.
   * <p>
   * This is used to convert dates to a numerical value.
   */
  @PropertyDefinition(validate = "notNull")
  private final DayCount dayCount;
  /**
   * The fixed interest rate to be paid.
   * A 5% rate will be expressed as 0.05.
   */
  @PropertyDefinition
  private final double rate;

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    ArgChecker.inOrderNotEqual(startDate, endDate, "startDate", "endDate");
  }

  //-------------------------------------------------------------------------
  @Override
  public ResolvedTermDeposit resolve(ReferenceData refData) {
    DateAdjuster bda = getBusinessDayAdjustment().orElse(BusinessDayAdjustment.NONE).resolve(refData);
    LocalDate start = bda.adjust(startDate);
    LocalDate end = bda.adjust(endDate);
    double yearFraction = dayCount.yearFraction(start, end);
    return ResolvedTermDeposit.builder()
        .startDate(start)
        .endDate(end)
        .yearFraction(yearFraction)
        .currency(getCurrency())
        .notional(buySell.normalize(notional))
        .rate(rate)
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code TermDeposit}.
   * @return the meta-bean, not null
   */
  public static TermDeposit.Meta meta() {
    return TermDeposit.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(TermDeposit.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static TermDeposit.Builder builder() {
    return new TermDeposit.Builder();
  }

  private TermDeposit(
      BuySell buySell,
      Currency currency,
      double notional,
      LocalDate startDate,
      LocalDate endDate,
      BusinessDayAdjustment businessDayAdjustment,
      DayCount dayCount,
      double rate) {
    JodaBeanUtils.notNull(buySell, "buySell");
    JodaBeanUtils.notNull(currency, "currency");
    ArgChecker.notNegative(notional, "notional");
    JodaBeanUtils.notNull(startDate, "startDate");
    JodaBeanUtils.notNull(endDate, "endDate");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    this.buySell = buySell;
    this.currency = currency;
    this.notional = notional;
    this.startDate = startDate;
    this.endDate = endDate;
    this.businessDayAdjustment = businessDayAdjustment;
    this.dayCount = dayCount;
    this.rate = rate;
    validate();
  }

  @Override
  public TermDeposit.Meta metaBean() {
    return TermDeposit.Meta.INSTANCE;
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
   * Gets whether the term deposit is 'Buy' or 'Sell'.
   * <p>
   * A value of 'Buy' implies payment of the principal at the start date and receipt of the
   * principal plus interest at the end date. A value of 'Sell' implies the opposite.
   * In other words, 'Buy' refers to buying the service of "money storage", with the benefit
   * of receiving interest.
   * @return the value of the property, not null
   */
  public BuySell getBuySell() {
    return buySell;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the primary currency.
   * <p>
   * This is the currency of the term deposit and the currency that payment is made in.
   * @return the value of the property, not null
   */
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the notional amount.
   * <p>
   * The notional represents the principal amount, and must be non-negative.
   * The currency of the notional is specified by {@code currency}.
   * @return the value of the property
   */
  public double getNotional() {
    return notional;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the start date of the deposit.
   * <p>
   * Interest accrues from this date.
   * This date is typically set to be a valid business day.
   * Optionally, the {@code businessDayAdjustment} property may be set to provide a rule for adjustment.
   * @return the value of the property, not null
   */
  public LocalDate getStartDate() {
    return startDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the end date of the deposit.
   * <p>
   * Interest accrues until this date.
   * This date is typically set to be a valid business day.
   * Optionally, the {@code businessDayAdjustment} property may be set to provide a rule for adjustment.
   * This date must be after the start date.
   * @return the value of the property, not null
   */
  public LocalDate getEndDate() {
    return endDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the business day adjustment to apply to the start and end date, optional.
   * <p>
   * The start and end date are typically defined as valid business days and thus
   * do not need to be adjusted. If this optional property is present, then the
   * start and end date will be adjusted as defined here.
   * @return the optional value of the property, not null
   */
  public Optional<BusinessDayAdjustment> getBusinessDayAdjustment() {
    return Optional.ofNullable(businessDayAdjustment);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count convention.
   * <p>
   * This is used to convert dates to a numerical value.
   * @return the value of the property, not null
   */
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the fixed interest rate to be paid.
   * A 5% rate will be expressed as 0.05.
   * @return the value of the property
   */
  public double getRate() {
    return rate;
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
      TermDeposit other = (TermDeposit) obj;
      return JodaBeanUtils.equal(buySell, other.buySell) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(notional, other.notional) &&
          JodaBeanUtils.equal(startDate, other.startDate) &&
          JodaBeanUtils.equal(endDate, other.endDate) &&
          JodaBeanUtils.equal(businessDayAdjustment, other.businessDayAdjustment) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(rate, other.rate);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(buySell);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(notional);
    hash = hash * 31 + JodaBeanUtils.hashCode(startDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(endDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(businessDayAdjustment);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(rate);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(288);
    buf.append("TermDeposit{");
    buf.append("buySell").append('=').append(buySell).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("notional").append('=').append(notional).append(',').append(' ');
    buf.append("startDate").append('=').append(startDate).append(',').append(' ');
    buf.append("endDate").append('=').append(endDate).append(',').append(' ');
    buf.append("businessDayAdjustment").append('=').append(businessDayAdjustment).append(',').append(' ');
    buf.append("dayCount").append('=').append(dayCount).append(',').append(' ');
    buf.append("rate").append('=').append(JodaBeanUtils.toString(rate));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code TermDeposit}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code buySell} property.
     */
    private final MetaProperty<BuySell> buySell = DirectMetaProperty.ofImmutable(
        this, "buySell", TermDeposit.class, BuySell.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", TermDeposit.class, Currency.class);
    /**
     * The meta-property for the {@code notional} property.
     */
    private final MetaProperty<Double> notional = DirectMetaProperty.ofImmutable(
        this, "notional", TermDeposit.class, Double.TYPE);
    /**
     * The meta-property for the {@code startDate} property.
     */
    private final MetaProperty<LocalDate> startDate = DirectMetaProperty.ofImmutable(
        this, "startDate", TermDeposit.class, LocalDate.class);
    /**
     * The meta-property for the {@code endDate} property.
     */
    private final MetaProperty<LocalDate> endDate = DirectMetaProperty.ofImmutable(
        this, "endDate", TermDeposit.class, LocalDate.class);
    /**
     * The meta-property for the {@code businessDayAdjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> businessDayAdjustment = DirectMetaProperty.ofImmutable(
        this, "businessDayAdjustment", TermDeposit.class, BusinessDayAdjustment.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", TermDeposit.class, DayCount.class);
    /**
     * The meta-property for the {@code rate} property.
     */
    private final MetaProperty<Double> rate = DirectMetaProperty.ofImmutable(
        this, "rate", TermDeposit.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "buySell",
        "currency",
        "notional",
        "startDate",
        "endDate",
        "businessDayAdjustment",
        "dayCount",
        "rate");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 244977400:  // buySell
          return buySell;
        case 575402001:  // currency
          return currency;
        case 1585636160:  // notional
          return notional;
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
        case 1905311443:  // dayCount
          return dayCount;
        case 3493088:  // rate
          return rate;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public TermDeposit.Builder builder() {
      return new TermDeposit.Builder();
    }

    @Override
    public Class<? extends TermDeposit> beanType() {
      return TermDeposit.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code buySell} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BuySell> buySell() {
      return buySell;
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
     * The meta-property for the {@code businessDayAdjustment} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BusinessDayAdjustment> businessDayAdjustment() {
      return businessDayAdjustment;
    }

    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    /**
     * The meta-property for the {@code rate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> rate() {
      return rate;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 244977400:  // buySell
          return ((TermDeposit) bean).getBuySell();
        case 575402001:  // currency
          return ((TermDeposit) bean).getCurrency();
        case 1585636160:  // notional
          return ((TermDeposit) bean).getNotional();
        case -2129778896:  // startDate
          return ((TermDeposit) bean).getStartDate();
        case -1607727319:  // endDate
          return ((TermDeposit) bean).getEndDate();
        case -1065319863:  // businessDayAdjustment
          return ((TermDeposit) bean).businessDayAdjustment;
        case 1905311443:  // dayCount
          return ((TermDeposit) bean).getDayCount();
        case 3493088:  // rate
          return ((TermDeposit) bean).getRate();
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
   * The bean-builder for {@code TermDeposit}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<TermDeposit> {

    private BuySell buySell;
    private Currency currency;
    private double notional;
    private LocalDate startDate;
    private LocalDate endDate;
    private BusinessDayAdjustment businessDayAdjustment;
    private DayCount dayCount;
    private double rate;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(TermDeposit beanToCopy) {
      this.buySell = beanToCopy.getBuySell();
      this.currency = beanToCopy.getCurrency();
      this.notional = beanToCopy.getNotional();
      this.startDate = beanToCopy.getStartDate();
      this.endDate = beanToCopy.getEndDate();
      this.businessDayAdjustment = beanToCopy.businessDayAdjustment;
      this.dayCount = beanToCopy.getDayCount();
      this.rate = beanToCopy.getRate();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 244977400:  // buySell
          return buySell;
        case 575402001:  // currency
          return currency;
        case 1585636160:  // notional
          return notional;
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
        case 1905311443:  // dayCount
          return dayCount;
        case 3493088:  // rate
          return rate;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 244977400:  // buySell
          this.buySell = (BuySell) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 1585636160:  // notional
          this.notional = (Double) newValue;
          break;
        case -2129778896:  // startDate
          this.startDate = (LocalDate) newValue;
          break;
        case -1607727319:  // endDate
          this.endDate = (LocalDate) newValue;
          break;
        case -1065319863:  // businessDayAdjustment
          this.businessDayAdjustment = (BusinessDayAdjustment) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        case 3493088:  // rate
          this.rate = (Double) newValue;
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
    public TermDeposit build() {
      return new TermDeposit(
          buySell,
          currency,
          notional,
          startDate,
          endDate,
          businessDayAdjustment,
          dayCount,
          rate);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets whether the term deposit is 'Buy' or 'Sell'.
     * <p>
     * A value of 'Buy' implies payment of the principal at the start date and receipt of the
     * principal plus interest at the end date. A value of 'Sell' implies the opposite.
     * In other words, 'Buy' refers to buying the service of "money storage", with the benefit
     * of receiving interest.
     * @param buySell  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder buySell(BuySell buySell) {
      JodaBeanUtils.notNull(buySell, "buySell");
      this.buySell = buySell;
      return this;
    }

    /**
     * Sets the primary currency.
     * <p>
     * This is the currency of the term deposit and the currency that payment is made in.
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
     * The notional represents the principal amount, and must be non-negative.
     * The currency of the notional is specified by {@code currency}.
     * @param notional  the new value
     * @return this, for chaining, not null
     */
    public Builder notional(double notional) {
      ArgChecker.notNegative(notional, "notional");
      this.notional = notional;
      return this;
    }

    /**
     * Sets the start date of the deposit.
     * <p>
     * Interest accrues from this date.
     * This date is typically set to be a valid business day.
     * Optionally, the {@code businessDayAdjustment} property may be set to provide a rule for adjustment.
     * @param startDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder startDate(LocalDate startDate) {
      JodaBeanUtils.notNull(startDate, "startDate");
      this.startDate = startDate;
      return this;
    }

    /**
     * Sets the end date of the deposit.
     * <p>
     * Interest accrues until this date.
     * This date is typically set to be a valid business day.
     * Optionally, the {@code businessDayAdjustment} property may be set to provide a rule for adjustment.
     * This date must be after the start date.
     * @param endDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder endDate(LocalDate endDate) {
      JodaBeanUtils.notNull(endDate, "endDate");
      this.endDate = endDate;
      return this;
    }

    /**
     * Sets the business day adjustment to apply to the start and end date, optional.
     * <p>
     * The start and end date are typically defined as valid business days and thus
     * do not need to be adjusted. If this optional property is present, then the
     * start and end date will be adjusted as defined here.
     * @param businessDayAdjustment  the new value
     * @return this, for chaining, not null
     */
    public Builder businessDayAdjustment(BusinessDayAdjustment businessDayAdjustment) {
      this.businessDayAdjustment = businessDayAdjustment;
      return this;
    }

    /**
     * Sets the day count convention.
     * <p>
     * This is used to convert dates to a numerical value.
     * @param dayCount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      JodaBeanUtils.notNull(dayCount, "dayCount");
      this.dayCount = dayCount;
      return this;
    }

    /**
     * Sets the fixed interest rate to be paid.
     * A 5% rate will be expressed as 0.05.
     * @param rate  the new value
     * @return this, for chaining, not null
     */
    public Builder rate(double rate) {
      this.rate = rate;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(288);
      buf.append("TermDeposit.Builder{");
      buf.append("buySell").append('=').append(JodaBeanUtils.toString(buySell)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ');
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(endDate)).append(',').append(' ');
      buf.append("businessDayAdjustment").append('=').append(JodaBeanUtils.toString(businessDayAdjustment)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("rate").append('=').append(JodaBeanUtils.toString(rate));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
