/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjuster;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutablePreBuild;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.index.FxIndexObservation;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.schedule.SchedulePeriod;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;

/**
 * A period within a swap that results in a known amount.
 * <p>
 * A swap leg consists of one or more periods that result in a payment.
 * The standard class, {@link RatePaymentPeriod}, represents a payment period calculated
 * from a fixed or floating rate. By contrast, this class represents a period
 * where the amount of the payment is known and fixed.
 */
@BeanDefinition
public final class KnownAmountNotionalSwapPaymentPeriod
    implements NotionalPaymentPeriod, ImmutableBean, Serializable {

  /**
   * The payment.
   * <p>
   * This includes the payment date and amount.
   * If the schedule adjusts for business days, then the date is the adjusted date.
   */
  @PropertyDefinition(validate = "notNull")
  private final Payment payment;
  /**
   * The start date of the payment period.
   * <p>
   * This is the first date in the period.
   * If the schedule adjusts for business days, then this is the adjusted date.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDate startDate;
  /**
   * The end date of the payment period.
   * <p>
   * This is the last date in the period.
   * If the schedule adjusts for business days, then this is the adjusted date.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
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
   * The notional amount, positive if receiving, negative if paying.
   * <p>
   * The notional amount applicable during the period.
   * The currency of the notional is typically the same as {@code currency}.
   * However, if FX reset applies, the currency will differ.
   */
  @PropertyDefinition(overrideGet = true)
  private final CurrencyAmount notionalAmount;
  /**
   * The FX reset definition, optional.
   * <p>
   * This property is used when the defined amount of the notional is specified in
   * a currency other than the currency of the swap leg. When this occurs, the notional
   * amount has to be converted using an FX rate to the swap leg currency.
   * <p>
   * The FX reset definition must be valid. The payment currency and the currency
   * of the notional must differ, and the currency pair must be that of the observation.
   */
  @PropertyDefinition(get = "optional", overrideGet = true)
  private final FxIndexObservation fxResetObservation;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance based on a payment, schedule period and notional.
   * 
   * @param payment  the payment
   * @param period  the schedule period
   * @param notional  the notional
   * @return the period
   */
  public static KnownAmountNotionalSwapPaymentPeriod of(
      Payment payment,
      SchedulePeriod period,
      CurrencyAmount notional) {

    return KnownAmountNotionalSwapPaymentPeriod.builder()
        .payment(payment)
        .startDate(period.getStartDate())
        .endDate(period.getEndDate())
        .unadjustedStartDate(period.getUnadjustedStartDate())
        .unadjustedEndDate(period.getUnadjustedEndDate())
        .notionalAmount(notional)
        .build();
  }

  /**
   * Obtains an instance based on a payment, schedule period, notional and FX reset.
   * 
   * @param payment  the payment
   * @param period  the schedule period
   * @param notional  the notional
   * @param fxResetObservation  the FX reset observation
   * @return the period
   */
  public static KnownAmountNotionalSwapPaymentPeriod of(
      Payment payment,
      SchedulePeriod period,
      CurrencyAmount notional,
      FxIndexObservation fxResetObservation) {

    return KnownAmountNotionalSwapPaymentPeriod.builder()
        .payment(payment)
        .startDate(period.getStartDate())
        .endDate(period.getEndDate())
        .unadjustedStartDate(period.getUnadjustedStartDate())
        .unadjustedEndDate(period.getUnadjustedEndDate())
        .notionalAmount(notional)
        .fxResetObservation(fxResetObservation)
        .build();
  }

  //-------------------------------------------------------------------------
  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.unadjustedStartDate == null && builder.startDate != null) {
      builder.unadjustedStartDate = builder.startDate;
    }
    if (builder.unadjustedEndDate == null && builder.endDate != null) {
      builder.unadjustedEndDate = builder.endDate;
    }
  }

  @ImmutableValidator
  private void validate() {
    // check for unadjusted must be after firstNonNull
    ArgChecker.inOrderNotEqual(startDate, endDate, "startDate", "endDate");
    ArgChecker.inOrderNotEqual(unadjustedStartDate, unadjustedEndDate, "unadjustedStartDate", "unadjustedEndDate");
    Currency payCcy = payment.getCurrency();
    Currency notionalCcy = notionalAmount.getCurrency();
    if (fxResetObservation != null) {
      if (notionalCcy.equals(payCcy)) {
        throw new IllegalArgumentException(Messages.format(
            "Payment currency {} must not equal notional currency {} when FX reset applies", payCcy, notionalCcy));
      }
      if (!fxResetObservation.getIndex().getCurrencyPair().contains(payCcy)) {
        throw new IllegalArgumentException(Messages.format(
            "Payment currency {} must be one of those in the FxReset index {}", payCcy, fxResetObservation.getIndex()));
      }
      if (!fxResetObservation.getIndex().getCurrencyPair().contains(notionalCcy)) {
        throw new IllegalArgumentException(Messages.format(
            "Notional currency {} must be one of those in the FxReset index {}", notionalCcy, fxResetObservation.getIndex()));
      }
    } else {
      if (!notionalCcy.equals(payCcy)) {
        throw new IllegalArgumentException(Messages.format(
            "Payment currency {} must equal notional currency {}", payCcy, notionalCcy));
      }
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public LocalDate getPaymentDate() {
    return payment.getDate();
  }

  /**
   * Gets the primary currency of the payment period.
   * <p>
   * This is the currency of the swap leg and the currency that interest calculation is made in.
   * <p>
   * The amounts of the notional are usually expressed in terms of this currency,
   * however they can be converted from amounts in a different currency.
   * See the optional {@code fxReset} property.
   * 
   * @return the primary currency
   */
  @Override
  public Currency getCurrency() {
    return payment.getCurrency();
  }

  //-------------------------------------------------------------------------
  @Override
  public KnownAmountNotionalSwapPaymentPeriod adjustPaymentDate(TemporalAdjuster adjuster) {
    Payment adjusted = payment.adjustDate(adjuster);
    return adjusted == payment ? this : toBuilder().payment(adjusted).build();
  }

  @Override
  public void collectIndices(ImmutableSet.Builder<Index> builder) {
    getFxResetObservation().ifPresent(fxReset -> builder.add(fxReset.getIndex()));
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code KnownAmountNotionalSwapPaymentPeriod}.
   * @return the meta-bean, not null
   */
  public static KnownAmountNotionalSwapPaymentPeriod.Meta meta() {
    return KnownAmountNotionalSwapPaymentPeriod.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(KnownAmountNotionalSwapPaymentPeriod.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static KnownAmountNotionalSwapPaymentPeriod.Builder builder() {
    return new KnownAmountNotionalSwapPaymentPeriod.Builder();
  }

  private KnownAmountNotionalSwapPaymentPeriod(
      Payment payment,
      LocalDate startDate,
      LocalDate endDate,
      LocalDate unadjustedStartDate,
      LocalDate unadjustedEndDate,
      CurrencyAmount notionalAmount,
      FxIndexObservation fxResetObservation) {
    JodaBeanUtils.notNull(payment, "payment");
    JodaBeanUtils.notNull(startDate, "startDate");
    JodaBeanUtils.notNull(endDate, "endDate");
    JodaBeanUtils.notNull(unadjustedStartDate, "unadjustedStartDate");
    JodaBeanUtils.notNull(unadjustedEndDate, "unadjustedEndDate");
    this.payment = payment;
    this.startDate = startDate;
    this.endDate = endDate;
    this.unadjustedStartDate = unadjustedStartDate;
    this.unadjustedEndDate = unadjustedEndDate;
    this.notionalAmount = notionalAmount;
    this.fxResetObservation = fxResetObservation;
    validate();
  }

  @Override
  public KnownAmountNotionalSwapPaymentPeriod.Meta metaBean() {
    return KnownAmountNotionalSwapPaymentPeriod.Meta.INSTANCE;
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
   * Gets the payment.
   * <p>
   * This includes the payment date and amount.
   * If the schedule adjusts for business days, then the date is the adjusted date.
   * @return the value of the property, not null
   */
  public Payment getPayment() {
    return payment;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the start date of the payment period.
   * <p>
   * This is the first date in the period.
   * If the schedule adjusts for business days, then this is the adjusted date.
   * @return the value of the property, not null
   */
  @Override
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
  @Override
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
   * Gets the notional amount, positive if receiving, negative if paying.
   * <p>
   * The notional amount applicable during the period.
   * The currency of the notional is typically the same as {@code currency}.
   * However, if FX reset applies, the currency will differ.
   * @return the value of the property
   */
  @Override
  public CurrencyAmount getNotionalAmount() {
    return notionalAmount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the FX reset definition, optional.
   * <p>
   * This property is used when the defined amount of the notional is specified in
   * a currency other than the currency of the swap leg. When this occurs, the notional
   * amount has to be converted using an FX rate to the swap leg currency.
   * <p>
   * The FX reset definition must be valid. The payment currency and the currency
   * of the notional must differ, and the currency pair must be that of the observation.
   * @return the optional value of the property, not null
   */
  @Override
  public Optional<FxIndexObservation> getFxResetObservation() {
    return Optional.ofNullable(fxResetObservation);
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
      KnownAmountNotionalSwapPaymentPeriod other = (KnownAmountNotionalSwapPaymentPeriod) obj;
      return JodaBeanUtils.equal(payment, other.payment) &&
          JodaBeanUtils.equal(startDate, other.startDate) &&
          JodaBeanUtils.equal(endDate, other.endDate) &&
          JodaBeanUtils.equal(unadjustedStartDate, other.unadjustedStartDate) &&
          JodaBeanUtils.equal(unadjustedEndDate, other.unadjustedEndDate) &&
          JodaBeanUtils.equal(notionalAmount, other.notionalAmount) &&
          JodaBeanUtils.equal(fxResetObservation, other.fxResetObservation);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(payment);
    hash = hash * 31 + JodaBeanUtils.hashCode(startDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(endDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(unadjustedStartDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(unadjustedEndDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(notionalAmount);
    hash = hash * 31 + JodaBeanUtils.hashCode(fxResetObservation);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(256);
    buf.append("KnownAmountNotionalSwapPaymentPeriod{");
    buf.append("payment").append('=').append(payment).append(',').append(' ');
    buf.append("startDate").append('=').append(startDate).append(',').append(' ');
    buf.append("endDate").append('=').append(endDate).append(',').append(' ');
    buf.append("unadjustedStartDate").append('=').append(unadjustedStartDate).append(',').append(' ');
    buf.append("unadjustedEndDate").append('=').append(unadjustedEndDate).append(',').append(' ');
    buf.append("notionalAmount").append('=').append(notionalAmount).append(',').append(' ');
    buf.append("fxResetObservation").append('=').append(JodaBeanUtils.toString(fxResetObservation));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code KnownAmountNotionalSwapPaymentPeriod}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code payment} property.
     */
    private final MetaProperty<Payment> payment = DirectMetaProperty.ofImmutable(
        this, "payment", KnownAmountNotionalSwapPaymentPeriod.class, Payment.class);
    /**
     * The meta-property for the {@code startDate} property.
     */
    private final MetaProperty<LocalDate> startDate = DirectMetaProperty.ofImmutable(
        this, "startDate", KnownAmountNotionalSwapPaymentPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code endDate} property.
     */
    private final MetaProperty<LocalDate> endDate = DirectMetaProperty.ofImmutable(
        this, "endDate", KnownAmountNotionalSwapPaymentPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code unadjustedStartDate} property.
     */
    private final MetaProperty<LocalDate> unadjustedStartDate = DirectMetaProperty.ofImmutable(
        this, "unadjustedStartDate", KnownAmountNotionalSwapPaymentPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code unadjustedEndDate} property.
     */
    private final MetaProperty<LocalDate> unadjustedEndDate = DirectMetaProperty.ofImmutable(
        this, "unadjustedEndDate", KnownAmountNotionalSwapPaymentPeriod.class, LocalDate.class);
    /**
     * The meta-property for the {@code notionalAmount} property.
     */
    private final MetaProperty<CurrencyAmount> notionalAmount = DirectMetaProperty.ofImmutable(
        this, "notionalAmount", KnownAmountNotionalSwapPaymentPeriod.class, CurrencyAmount.class);
    /**
     * The meta-property for the {@code fxResetObservation} property.
     */
    private final MetaProperty<FxIndexObservation> fxResetObservation = DirectMetaProperty.ofImmutable(
        this, "fxResetObservation", KnownAmountNotionalSwapPaymentPeriod.class, FxIndexObservation.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "payment",
        "startDate",
        "endDate",
        "unadjustedStartDate",
        "unadjustedEndDate",
        "notionalAmount",
        "fxResetObservation");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -786681338:  // payment
          return payment;
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case 1457691881:  // unadjustedStartDate
          return unadjustedStartDate;
        case 31758114:  // unadjustedEndDate
          return unadjustedEndDate;
        case -902123592:  // notionalAmount
          return notionalAmount;
        case 1301329999:  // fxResetObservation
          return fxResetObservation;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public KnownAmountNotionalSwapPaymentPeriod.Builder builder() {
      return new KnownAmountNotionalSwapPaymentPeriod.Builder();
    }

    @Override
    public Class<? extends KnownAmountNotionalSwapPaymentPeriod> beanType() {
      return KnownAmountNotionalSwapPaymentPeriod.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code payment} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Payment> payment() {
      return payment;
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
     * The meta-property for the {@code notionalAmount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurrencyAmount> notionalAmount() {
      return notionalAmount;
    }

    /**
     * The meta-property for the {@code fxResetObservation} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FxIndexObservation> fxResetObservation() {
      return fxResetObservation;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -786681338:  // payment
          return ((KnownAmountNotionalSwapPaymentPeriod) bean).getPayment();
        case -2129778896:  // startDate
          return ((KnownAmountNotionalSwapPaymentPeriod) bean).getStartDate();
        case -1607727319:  // endDate
          return ((KnownAmountNotionalSwapPaymentPeriod) bean).getEndDate();
        case 1457691881:  // unadjustedStartDate
          return ((KnownAmountNotionalSwapPaymentPeriod) bean).getUnadjustedStartDate();
        case 31758114:  // unadjustedEndDate
          return ((KnownAmountNotionalSwapPaymentPeriod) bean).getUnadjustedEndDate();
        case -902123592:  // notionalAmount
          return ((KnownAmountNotionalSwapPaymentPeriod) bean).getNotionalAmount();
        case 1301329999:  // fxResetObservation
          return ((KnownAmountNotionalSwapPaymentPeriod) bean).fxResetObservation;
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
   * The bean-builder for {@code KnownAmountNotionalSwapPaymentPeriod}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<KnownAmountNotionalSwapPaymentPeriod> {

    private Payment payment;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate unadjustedStartDate;
    private LocalDate unadjustedEndDate;
    private CurrencyAmount notionalAmount;
    private FxIndexObservation fxResetObservation;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(KnownAmountNotionalSwapPaymentPeriod beanToCopy) {
      this.payment = beanToCopy.getPayment();
      this.startDate = beanToCopy.getStartDate();
      this.endDate = beanToCopy.getEndDate();
      this.unadjustedStartDate = beanToCopy.getUnadjustedStartDate();
      this.unadjustedEndDate = beanToCopy.getUnadjustedEndDate();
      this.notionalAmount = beanToCopy.getNotionalAmount();
      this.fxResetObservation = beanToCopy.fxResetObservation;
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -786681338:  // payment
          return payment;
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case 1457691881:  // unadjustedStartDate
          return unadjustedStartDate;
        case 31758114:  // unadjustedEndDate
          return unadjustedEndDate;
        case -902123592:  // notionalAmount
          return notionalAmount;
        case 1301329999:  // fxResetObservation
          return fxResetObservation;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -786681338:  // payment
          this.payment = (Payment) newValue;
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
        case -902123592:  // notionalAmount
          this.notionalAmount = (CurrencyAmount) newValue;
          break;
        case 1301329999:  // fxResetObservation
          this.fxResetObservation = (FxIndexObservation) newValue;
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
    public KnownAmountNotionalSwapPaymentPeriod build() {
      preBuild(this);
      return new KnownAmountNotionalSwapPaymentPeriod(
          payment,
          startDate,
          endDate,
          unadjustedStartDate,
          unadjustedEndDate,
          notionalAmount,
          fxResetObservation);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the payment.
     * <p>
     * This includes the payment date and amount.
     * If the schedule adjusts for business days, then the date is the adjusted date.
     * @param payment  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder payment(Payment payment) {
      JodaBeanUtils.notNull(payment, "payment");
      this.payment = payment;
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
     * Sets the notional amount, positive if receiving, negative if paying.
     * <p>
     * The notional amount applicable during the period.
     * The currency of the notional is typically the same as {@code currency}.
     * However, if FX reset applies, the currency will differ.
     * @param notionalAmount  the new value
     * @return this, for chaining, not null
     */
    public Builder notionalAmount(CurrencyAmount notionalAmount) {
      this.notionalAmount = notionalAmount;
      return this;
    }

    /**
     * Sets the FX reset definition, optional.
     * <p>
     * This property is used when the defined amount of the notional is specified in
     * a currency other than the currency of the swap leg. When this occurs, the notional
     * amount has to be converted using an FX rate to the swap leg currency.
     * <p>
     * The FX reset definition must be valid. The payment currency and the currency
     * of the notional must differ, and the currency pair must be that of the observation.
     * @param fxResetObservation  the new value
     * @return this, for chaining, not null
     */
    public Builder fxResetObservation(FxIndexObservation fxResetObservation) {
      this.fxResetObservation = fxResetObservation;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(256);
      buf.append("KnownAmountNotionalSwapPaymentPeriod.Builder{");
      buf.append("payment").append('=').append(JodaBeanUtils.toString(payment)).append(',').append(' ');
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ');
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(endDate)).append(',').append(' ');
      buf.append("unadjustedStartDate").append('=').append(JodaBeanUtils.toString(unadjustedStartDate)).append(',').append(' ');
      buf.append("unadjustedEndDate").append('=').append(JodaBeanUtils.toString(unadjustedEndDate)).append(',').append(' ');
      buf.append("notionalAmount").append('=').append(JodaBeanUtils.toString(notionalAmount)).append(',').append(' ');
      buf.append("fxResetObservation").append('=').append(JodaBeanUtils.toString(fxResetObservation));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
