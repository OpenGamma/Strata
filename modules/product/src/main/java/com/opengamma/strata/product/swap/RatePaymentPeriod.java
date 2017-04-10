/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjuster;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.FxIndexObservation;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.Messages;

/**
 * A period over which a rate of interest is paid.
 * <p>
 * A swap leg consists of one or more periods that are the basis of accrual.
 * The payment period is formed from one or more accrual periods which
 * detail the type of interest to be accrued, fixed or floating.
 * <p>
 * This class specifies the data necessary to calculate the value of the period.
 * Any combination of accrual periods is supported in the data model, however
 * there is no guarantee that exotic combinations will price sensibly.
 */
@BeanDefinition(constructorScope = "package")
public final class RatePaymentPeriod
    implements NotionalPaymentPeriod, ImmutableBean, Serializable {

  /**
   * The date that payment occurs.
   * <p>
   * The date that payment is made for the accrual periods.
   * If the schedule adjusts for business days, then this is the adjusted date.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDate paymentDate;
  /**
   * The accrual periods that combine to form the payment period.
   * <p>
   * Each accrual period includes the applicable dates and details of how to observe the rate.
   * In most cases, there will be one accrual period.
   * If there is more than one accrual period then compounding may apply.
   */
  @PropertyDefinition(validate = "notEmpty")
  private final ImmutableList<RateAccrualPeriod> accrualPeriods;
  /**
   * The day count convention.
   * <p>
   * Each accrual period contains a year fraction calculated using this day count.
   * This day count is used when there is a need to perform further calculations.
   */
  @PropertyDefinition(validate = "notNull")
  private final DayCount dayCount;
  /**
   * The primary currency of the payment period.
   * <p>
   * This is the currency of the swap leg and the currency that interest calculation is made in.
   * <p>
   * The amounts of the notional are usually expressed in terms of this currency,
   * however they can be converted from amounts in a different currency.
   * See the optional {@code fxReset} property.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final Currency currency;
  /**
   * The FX reset definition, optional.
   * <p>
   * This property is used when the defined amount of the notional is specified in
   * a currency other than the currency of the swap leg. When this occurs, the notional
   * amount has to be converted using an FX rate to the swap leg currency.
   * <p>
   * The FX reset definition must be valid. It must have a reference currency that is
   * different to that of this period, and the currency of this period must be
   * one of those defined by the FX reset index.
   */
  @PropertyDefinition(get = "optional")
  private final FxReset fxReset;
  /**
   * The notional amount, positive if receiving, negative if paying.
   * <p>
   * The notional amount applicable during the period.
   * The currency of the notional is specified by {@code currency} unless there
   * is the {@code fxReset} property is present.
   */
  @PropertyDefinition
  private final double notional;
  /**
   * The compounding method to use when there is more than one accrual period, default is 'None'.
   * <p>
   * Compounding is used when combining accrual periods.
   */
  @PropertyDefinition(validate = "notNull")
  private final CompoundingMethod compoundingMethod;

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.compoundingMethod(CompoundingMethod.NONE);
  }

  @ImmutableValidator
  private void validate() {
    if (fxReset != null) {
      Currency notionalCcy = fxReset.getReferenceCurrency();
      if (fxReset.getReferenceCurrency().equals(currency)) {
        throw new IllegalArgumentException(Messages.format(
            "Payment currency {} must not equal notional currency {} when FX reset applies", currency, notionalCcy));
      }
      if (!fxReset.getIndex().getCurrencyPair().contains(currency)) {
        throw new IllegalArgumentException(Messages.format(
            "Payment currency {} must be one of those in the FxReset index {}", currency, fxReset.getIndex()));
      }
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the accrual start date of the period.
   * <p>
   * This is the first accrual date in the period.
   * This date has typically been adjusted to be a valid business day.
   * 
   * @return the start date of the period
   */
  @Override
  public LocalDate getStartDate() {
    return accrualPeriods.get(0).getStartDate();
  }

  /**
   * Gets the accrual end date of the period.
   * <p>
   * This is the last accrual date in the period.
   * This date has typically been adjusted to be a valid business day.
   * 
   * @return the end date of the period
   */
  @Override
  public LocalDate getEndDate() {
    return accrualPeriods.get(accrualPeriods.size() - 1).getEndDate();
  }

  /**
   * Gets the notional amount, positive if receiving, negative if paying.
   * <p>
   * This is the notional amount applicable during the period.
   * The currency of the notional is specified by {@code currency} unless there
   * is the {@code fxReset} property is present.
   * 
   * @return the notional as a {@code CurrencyAmount}
   */
  @Override
  public CurrencyAmount getNotionalAmount() {
    if (fxReset != null) {
      return CurrencyAmount.of(fxReset.getReferenceCurrency(), notional);
    }
    return CurrencyAmount.of(currency, notional);
  }

  @Override
  public Optional<FxIndexObservation> getFxResetObservation() {
    return getFxReset().map(fxr -> fxr.getObservation());
  }

  /**
   * Checks whether compounding applies.
   * <p>
   * Compounding applies if there is more than one accrual period and the
   * compounding method is not 'None'.
   * 
   * @return true if compounding applies
   */
  public boolean isCompoundingApplicable() {
    return accrualPeriods.size() > 1 && compoundingMethod != CompoundingMethod.NONE;
  }

  //-------------------------------------------------------------------------
  @Override
  public RatePaymentPeriod adjustPaymentDate(TemporalAdjuster adjuster) {
    LocalDate adjusted = paymentDate.with(adjuster);
    return adjusted.equals(paymentDate) ? this : toBuilder().paymentDate(adjusted).build();
  }

  @Override
  public void collectIndices(ImmutableSet.Builder<Index> builder) {
    accrualPeriods.stream().forEach(accrual -> accrual.getRateComputation().collectIndices(builder));
    getFxReset().ifPresent(fxReset -> builder.add(fxReset.getIndex()));
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code RatePaymentPeriod}.
   * @return the meta-bean, not null
   */
  public static RatePaymentPeriod.Meta meta() {
    return RatePaymentPeriod.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(RatePaymentPeriod.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static RatePaymentPeriod.Builder builder() {
    return new RatePaymentPeriod.Builder();
  }

  /**
   * Creates an instance.
   * @param paymentDate  the value of the property, not null
   * @param accrualPeriods  the value of the property, not empty
   * @param dayCount  the value of the property, not null
   * @param currency  the value of the property, not null
   * @param fxReset  the value of the property
   * @param notional  the value of the property
   * @param compoundingMethod  the value of the property, not null
   */
  RatePaymentPeriod(
      LocalDate paymentDate,
      List<RateAccrualPeriod> accrualPeriods,
      DayCount dayCount,
      Currency currency,
      FxReset fxReset,
      double notional,
      CompoundingMethod compoundingMethod) {
    JodaBeanUtils.notNull(paymentDate, "paymentDate");
    JodaBeanUtils.notEmpty(accrualPeriods, "accrualPeriods");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    JodaBeanUtils.notNull(currency, "currency");
    JodaBeanUtils.notNull(compoundingMethod, "compoundingMethod");
    this.paymentDate = paymentDate;
    this.accrualPeriods = ImmutableList.copyOf(accrualPeriods);
    this.dayCount = dayCount;
    this.currency = currency;
    this.fxReset = fxReset;
    this.notional = notional;
    this.compoundingMethod = compoundingMethod;
    validate();
  }

  @Override
  public RatePaymentPeriod.Meta metaBean() {
    return RatePaymentPeriod.Meta.INSTANCE;
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
   * Gets the date that payment occurs.
   * <p>
   * The date that payment is made for the accrual periods.
   * If the schedule adjusts for business days, then this is the adjusted date.
   * @return the value of the property, not null
   */
  @Override
  public LocalDate getPaymentDate() {
    return paymentDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the accrual periods that combine to form the payment period.
   * <p>
   * Each accrual period includes the applicable dates and details of how to observe the rate.
   * In most cases, there will be one accrual period.
   * If there is more than one accrual period then compounding may apply.
   * @return the value of the property, not empty
   */
  public ImmutableList<RateAccrualPeriod> getAccrualPeriods() {
    return accrualPeriods;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count convention.
   * <p>
   * Each accrual period contains a year fraction calculated using this day count.
   * This day count is used when there is a need to perform further calculations.
   * @return the value of the property, not null
   */
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the primary currency of the payment period.
   * <p>
   * This is the currency of the swap leg and the currency that interest calculation is made in.
   * <p>
   * The amounts of the notional are usually expressed in terms of this currency,
   * however they can be converted from amounts in a different currency.
   * See the optional {@code fxReset} property.
   * @return the value of the property, not null
   */
  @Override
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the FX reset definition, optional.
   * <p>
   * This property is used when the defined amount of the notional is specified in
   * a currency other than the currency of the swap leg. When this occurs, the notional
   * amount has to be converted using an FX rate to the swap leg currency.
   * <p>
   * The FX reset definition must be valid. It must have a reference currency that is
   * different to that of this period, and the currency of this period must be
   * one of those defined by the FX reset index.
   * @return the optional value of the property, not null
   */
  public Optional<FxReset> getFxReset() {
    return Optional.ofNullable(fxReset);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the notional amount, positive if receiving, negative if paying.
   * <p>
   * The notional amount applicable during the period.
   * The currency of the notional is specified by {@code currency} unless there
   * is the {@code fxReset} property is present.
   * @return the value of the property
   */
  public double getNotional() {
    return notional;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the compounding method to use when there is more than one accrual period, default is 'None'.
   * <p>
   * Compounding is used when combining accrual periods.
   * @return the value of the property, not null
   */
  public CompoundingMethod getCompoundingMethod() {
    return compoundingMethod;
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
      RatePaymentPeriod other = (RatePaymentPeriod) obj;
      return JodaBeanUtils.equal(paymentDate, other.paymentDate) &&
          JodaBeanUtils.equal(accrualPeriods, other.accrualPeriods) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(fxReset, other.fxReset) &&
          JodaBeanUtils.equal(notional, other.notional) &&
          JodaBeanUtils.equal(compoundingMethod, other.compoundingMethod);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(accrualPeriods);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(fxReset);
    hash = hash * 31 + JodaBeanUtils.hashCode(notional);
    hash = hash * 31 + JodaBeanUtils.hashCode(compoundingMethod);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(256);
    buf.append("RatePaymentPeriod{");
    buf.append("paymentDate").append('=').append(paymentDate).append(',').append(' ');
    buf.append("accrualPeriods").append('=').append(accrualPeriods).append(',').append(' ');
    buf.append("dayCount").append('=').append(dayCount).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("fxReset").append('=').append(fxReset).append(',').append(' ');
    buf.append("notional").append('=').append(notional).append(',').append(' ');
    buf.append("compoundingMethod").append('=').append(JodaBeanUtils.toString(compoundingMethod));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code RatePaymentPeriod}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code paymentDate} property.
     */
    private final MetaProperty<LocalDate> paymentDate = DirectMetaProperty.ofImmutable(
        this, "paymentDate", RatePaymentPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code accrualPeriods} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<RateAccrualPeriod>> accrualPeriods = DirectMetaProperty.ofImmutable(
        this, "accrualPeriods", RatePaymentPeriod.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", RatePaymentPeriod.class, DayCount.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", RatePaymentPeriod.class, Currency.class);
    /**
     * The meta-property for the {@code fxReset} property.
     */
    private final MetaProperty<FxReset> fxReset = DirectMetaProperty.ofImmutable(
        this, "fxReset", RatePaymentPeriod.class, FxReset.class);
    /**
     * The meta-property for the {@code notional} property.
     */
    private final MetaProperty<Double> notional = DirectMetaProperty.ofImmutable(
        this, "notional", RatePaymentPeriod.class, Double.TYPE);
    /**
     * The meta-property for the {@code compoundingMethod} property.
     */
    private final MetaProperty<CompoundingMethod> compoundingMethod = DirectMetaProperty.ofImmutable(
        this, "compoundingMethod", RatePaymentPeriod.class, CompoundingMethod.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "paymentDate",
        "accrualPeriods",
        "dayCount",
        "currency",
        "fxReset",
        "notional",
        "compoundingMethod");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1540873516:  // paymentDate
          return paymentDate;
        case -92208605:  // accrualPeriods
          return accrualPeriods;
        case 1905311443:  // dayCount
          return dayCount;
        case 575402001:  // currency
          return currency;
        case -449555555:  // fxReset
          return fxReset;
        case 1585636160:  // notional
          return notional;
        case -1376171496:  // compoundingMethod
          return compoundingMethod;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public RatePaymentPeriod.Builder builder() {
      return new RatePaymentPeriod.Builder();
    }

    @Override
    public Class<? extends RatePaymentPeriod> beanType() {
      return RatePaymentPeriod.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code paymentDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> paymentDate() {
      return paymentDate;
    }

    /**
     * The meta-property for the {@code accrualPeriods} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<RateAccrualPeriod>> accrualPeriods() {
      return accrualPeriods;
    }

    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    /**
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    /**
     * The meta-property for the {@code fxReset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FxReset> fxReset() {
      return fxReset;
    }

    /**
     * The meta-property for the {@code notional} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> notional() {
      return notional;
    }

    /**
     * The meta-property for the {@code compoundingMethod} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CompoundingMethod> compoundingMethod() {
      return compoundingMethod;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1540873516:  // paymentDate
          return ((RatePaymentPeriod) bean).getPaymentDate();
        case -92208605:  // accrualPeriods
          return ((RatePaymentPeriod) bean).getAccrualPeriods();
        case 1905311443:  // dayCount
          return ((RatePaymentPeriod) bean).getDayCount();
        case 575402001:  // currency
          return ((RatePaymentPeriod) bean).getCurrency();
        case -449555555:  // fxReset
          return ((RatePaymentPeriod) bean).fxReset;
        case 1585636160:  // notional
          return ((RatePaymentPeriod) bean).getNotional();
        case -1376171496:  // compoundingMethod
          return ((RatePaymentPeriod) bean).getCompoundingMethod();
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
   * The bean-builder for {@code RatePaymentPeriod}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<RatePaymentPeriod> {

    private LocalDate paymentDate;
    private List<RateAccrualPeriod> accrualPeriods = ImmutableList.of();
    private DayCount dayCount;
    private Currency currency;
    private FxReset fxReset;
    private double notional;
    private CompoundingMethod compoundingMethod;

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
    private Builder(RatePaymentPeriod beanToCopy) {
      this.paymentDate = beanToCopy.getPaymentDate();
      this.accrualPeriods = beanToCopy.getAccrualPeriods();
      this.dayCount = beanToCopy.getDayCount();
      this.currency = beanToCopy.getCurrency();
      this.fxReset = beanToCopy.fxReset;
      this.notional = beanToCopy.getNotional();
      this.compoundingMethod = beanToCopy.getCompoundingMethod();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1540873516:  // paymentDate
          return paymentDate;
        case -92208605:  // accrualPeriods
          return accrualPeriods;
        case 1905311443:  // dayCount
          return dayCount;
        case 575402001:  // currency
          return currency;
        case -449555555:  // fxReset
          return fxReset;
        case 1585636160:  // notional
          return notional;
        case -1376171496:  // compoundingMethod
          return compoundingMethod;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1540873516:  // paymentDate
          this.paymentDate = (LocalDate) newValue;
          break;
        case -92208605:  // accrualPeriods
          this.accrualPeriods = (List<RateAccrualPeriod>) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case -449555555:  // fxReset
          this.fxReset = (FxReset) newValue;
          break;
        case 1585636160:  // notional
          this.notional = (Double) newValue;
          break;
        case -1376171496:  // compoundingMethod
          this.compoundingMethod = (CompoundingMethod) newValue;
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
    public RatePaymentPeriod build() {
      return new RatePaymentPeriod(
          paymentDate,
          accrualPeriods,
          dayCount,
          currency,
          fxReset,
          notional,
          compoundingMethod);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the date that payment occurs.
     * <p>
     * The date that payment is made for the accrual periods.
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
     * Sets the accrual periods that combine to form the payment period.
     * <p>
     * Each accrual period includes the applicable dates and details of how to observe the rate.
     * In most cases, there will be one accrual period.
     * If there is more than one accrual period then compounding may apply.
     * @param accrualPeriods  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder accrualPeriods(List<RateAccrualPeriod> accrualPeriods) {
      JodaBeanUtils.notEmpty(accrualPeriods, "accrualPeriods");
      this.accrualPeriods = accrualPeriods;
      return this;
    }

    /**
     * Sets the {@code accrualPeriods} property in the builder
     * from an array of objects.
     * @param accrualPeriods  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder accrualPeriods(RateAccrualPeriod... accrualPeriods) {
      return accrualPeriods(ImmutableList.copyOf(accrualPeriods));
    }

    /**
     * Sets the day count convention.
     * <p>
     * Each accrual period contains a year fraction calculated using this day count.
     * This day count is used when there is a need to perform further calculations.
     * @param dayCount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      JodaBeanUtils.notNull(dayCount, "dayCount");
      this.dayCount = dayCount;
      return this;
    }

    /**
     * Sets the primary currency of the payment period.
     * <p>
     * This is the currency of the swap leg and the currency that interest calculation is made in.
     * <p>
     * The amounts of the notional are usually expressed in terms of this currency,
     * however they can be converted from amounts in a different currency.
     * See the optional {@code fxReset} property.
     * @param currency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder currency(Currency currency) {
      JodaBeanUtils.notNull(currency, "currency");
      this.currency = currency;
      return this;
    }

    /**
     * Sets the FX reset definition, optional.
     * <p>
     * This property is used when the defined amount of the notional is specified in
     * a currency other than the currency of the swap leg. When this occurs, the notional
     * amount has to be converted using an FX rate to the swap leg currency.
     * <p>
     * The FX reset definition must be valid. It must have a reference currency that is
     * different to that of this period, and the currency of this period must be
     * one of those defined by the FX reset index.
     * @param fxReset  the new value
     * @return this, for chaining, not null
     */
    public Builder fxReset(FxReset fxReset) {
      this.fxReset = fxReset;
      return this;
    }

    /**
     * Sets the notional amount, positive if receiving, negative if paying.
     * <p>
     * The notional amount applicable during the period.
     * The currency of the notional is specified by {@code currency} unless there
     * is the {@code fxReset} property is present.
     * @param notional  the new value
     * @return this, for chaining, not null
     */
    public Builder notional(double notional) {
      this.notional = notional;
      return this;
    }

    /**
     * Sets the compounding method to use when there is more than one accrual period, default is 'None'.
     * <p>
     * Compounding is used when combining accrual periods.
     * @param compoundingMethod  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder compoundingMethod(CompoundingMethod compoundingMethod) {
      JodaBeanUtils.notNull(compoundingMethod, "compoundingMethod");
      this.compoundingMethod = compoundingMethod;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(256);
      buf.append("RatePaymentPeriod.Builder{");
      buf.append("paymentDate").append('=').append(JodaBeanUtils.toString(paymentDate)).append(',').append(' ');
      buf.append("accrualPeriods").append('=').append(JodaBeanUtils.toString(accrualPeriods)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("fxReset").append('=').append(JodaBeanUtils.toString(fxReset)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("compoundingMethod").append('=').append(JodaBeanUtils.toString(compoundingMethod));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
