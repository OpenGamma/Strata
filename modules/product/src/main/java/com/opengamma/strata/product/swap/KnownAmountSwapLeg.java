/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.DerivedProperty;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.DateAdjuster;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.Schedule;
import com.opengamma.strata.basics.schedule.SchedulePeriod;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.product.common.PayReceive;

/**
 * A fixed swap leg defined in terms of known amounts.
 * <p>
 * Most fixed swap legs are calculated based on a fixed rate of interest.
 * By contrast, this leg defines a known payment amount for each period.
 * <p>
 * Each payment occurs relative to a <i>payment period</i>.
 * The payment periods are calculated relative to the <i>accrual periods</i>.
 * While the model allows the frequency of the accrual and payment periods to differ,
 * this will have no effect, as the amounts to be paid at each payment date are known.
 * This design is intended to match FpML.
 * 
 * @see RateCalculationSwapLeg
 * @see FixedRateCalculation
 */
@BeanDefinition
public final class KnownAmountSwapLeg
    implements ScheduledSwapLeg, ImmutableBean, Serializable {

  /**
   * Whether the leg is pay or receive.
   * <p>
   * A value of 'Pay' implies that the resulting amount is paid to the counterparty.
   * A value of 'Receive' implies that the resulting amount is received from the counterparty.
   * Note that negative interest rates can result in a payment in the opposite
   * direction to that implied by this indicator.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final PayReceive payReceive;
  /**
   * The accrual period schedule.
   * <p>
   * This is used to define the accrual periods.
   * These are used directly or indirectly to determine other dates in the swap.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final PeriodicSchedule accrualSchedule;
  /**
   * The payment period schedule.
   * <p>
   * This is used to define the payment periods, including any compounding.
   * The payment period dates are based on the accrual schedule.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final PaymentSchedule paymentSchedule;
  /**
   * The known amount schedule.
   * <p>
   * This defines the schedule of known amounts, relative to the payment schedule.
   * The schedule is defined as an initial amount, with optional changes during the tenor of the swap.
   * The amount is only permitted to change at payment period boundaries.
   * <p>
   * Note that the date of the payment is implied by the payment schedule.
   * Any dates in the known amount schedule refer to the payment schedule, not the payment date.
   * <p>
   * For example, consider a two year swap where each payment period is 3 months long.
   * This schedule could define two entries, one that defines the payment amounts as GBP 1000 for
   * the first year and one that defines the amount as GBP 500 for the second year.
   * In this case there will be eight payments in total, four payments of GBP 1000 in the first
   * year and four payments of GBP 500 in the second year.
   * Each payment will occur on the date specified using the offset in {@link PaymentSchedule}.
   */
  @PropertyDefinition(validate = "notNull")
  private final ValueSchedule amount;
  /**
   * The currency of the swap leg.
   * <p>
   * This is the currency of the known payments.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final Currency currency;

  //-------------------------------------------------------------------------
  @Override
  @DerivedProperty
  public SwapLegType getType() {
    return SwapLegType.FIXED;
  }

  @Override
  @DerivedProperty
  public AdjustableDate getStartDate() {
    return accrualSchedule.calculatedStartDate();
  }

  @Override
  @DerivedProperty
  public AdjustableDate getEndDate() {
    return accrualSchedule.calculatedEndDate();
  }

  @Override
  public void collectCurrencies(ImmutableSet.Builder<Currency> builder) {
    builder.add(currency);
  }

  @Override
  public void collectIndices(ImmutableSet.Builder<Index> builder) {
    // no indices
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an instance based on this leg with the start date replaced.
   * <p>
   * This uses {@link PeriodicSchedule#replaceStartDate(LocalDate)}.
   * 
   * @throws IllegalArgumentException if the start date cannot be replaced with the proposed start date
   */
  @Override
  public KnownAmountSwapLeg replaceStartDate(LocalDate adjustedStartDate) {
    return toBuilder().accrualSchedule(accrualSchedule.replaceStartDate(adjustedStartDate)).build();
  }

  /**
   * Converts this swap leg to the equivalent {@code ResolvedSwapLeg}.
   * <p>
   * An {@link ResolvedSwapLeg} represents the same data as this leg, but with
   * a complete schedule of dates defined using {@link KnownAmountSwapPaymentPeriod}.
   * 
   * @param refData  the reference data to use when resolving
   * @return the equivalent resolved swap leg
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   * @throws RuntimeException if unable to resolve due to an invalid swap schedule or definition
   */
  @Override
  public ResolvedSwapLeg resolve(ReferenceData refData) {
    Schedule resolvedAccruals = accrualSchedule.createSchedule(refData);
    Schedule resolvedPayments = paymentSchedule.createSchedule(resolvedAccruals, refData);
    List<SwapPaymentPeriod> payPeriods = createPaymentPeriods(resolvedPayments, refData);
    return new ResolvedSwapLeg(getType(), payReceive, payPeriods, ImmutableList.of(), currency);
  }

  // create the payment period
  private List<SwapPaymentPeriod> createPaymentPeriods(Schedule resolvedPayments, ReferenceData refData) {
    // resolve amount schedule against payment schedule
    DoubleArray amounts = amount.resolveValues(resolvedPayments);
    // resolve against reference data once
    DateAdjuster paymentDateAdjuster = paymentSchedule.getPaymentDateOffset().resolve(refData);
    // build up payment periods using schedule
    ImmutableList.Builder<SwapPaymentPeriod> paymentPeriods = ImmutableList.builder();
    for (int index = 0; index < resolvedPayments.size(); index++) {
      SchedulePeriod paymentPeriod = resolvedPayments.getPeriod(index);
      LocalDate baseDate = paymentSchedule.getPaymentRelativeTo().selectBaseDate(paymentPeriod);
      LocalDate paymentDate = paymentDateAdjuster.adjust(baseDate);
      double amount = payReceive.normalize(amounts.get(index));
      Payment payment = Payment.of(CurrencyAmount.of(currency, amount), paymentDate);
      paymentPeriods.add(KnownAmountSwapPaymentPeriod.of(payment, paymentPeriod));
    }
    return paymentPeriods.build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code KnownAmountSwapLeg}.
   * @return the meta-bean, not null
   */
  public static KnownAmountSwapLeg.Meta meta() {
    return KnownAmountSwapLeg.Meta.INSTANCE;
  }

  static {
    MetaBean.register(KnownAmountSwapLeg.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static KnownAmountSwapLeg.Builder builder() {
    return new KnownAmountSwapLeg.Builder();
  }

  private KnownAmountSwapLeg(
      PayReceive payReceive,
      PeriodicSchedule accrualSchedule,
      PaymentSchedule paymentSchedule,
      ValueSchedule amount,
      Currency currency) {
    JodaBeanUtils.notNull(payReceive, "payReceive");
    JodaBeanUtils.notNull(accrualSchedule, "accrualSchedule");
    JodaBeanUtils.notNull(paymentSchedule, "paymentSchedule");
    JodaBeanUtils.notNull(amount, "amount");
    JodaBeanUtils.notNull(currency, "currency");
    this.payReceive = payReceive;
    this.accrualSchedule = accrualSchedule;
    this.paymentSchedule = paymentSchedule;
    this.amount = amount;
    this.currency = currency;
  }

  @Override
  public KnownAmountSwapLeg.Meta metaBean() {
    return KnownAmountSwapLeg.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets whether the leg is pay or receive.
   * <p>
   * A value of 'Pay' implies that the resulting amount is paid to the counterparty.
   * A value of 'Receive' implies that the resulting amount is received from the counterparty.
   * Note that negative interest rates can result in a payment in the opposite
   * direction to that implied by this indicator.
   * @return the value of the property, not null
   */
  @Override
  public PayReceive getPayReceive() {
    return payReceive;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the accrual period schedule.
   * <p>
   * This is used to define the accrual periods.
   * These are used directly or indirectly to determine other dates in the swap.
   * @return the value of the property, not null
   */
  @Override
  public PeriodicSchedule getAccrualSchedule() {
    return accrualSchedule;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the payment period schedule.
   * <p>
   * This is used to define the payment periods, including any compounding.
   * The payment period dates are based on the accrual schedule.
   * @return the value of the property, not null
   */
  @Override
  public PaymentSchedule getPaymentSchedule() {
    return paymentSchedule;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the known amount schedule.
   * <p>
   * This defines the schedule of known amounts, relative to the payment schedule.
   * The schedule is defined as an initial amount, with optional changes during the tenor of the swap.
   * The amount is only permitted to change at payment period boundaries.
   * <p>
   * Note that the date of the payment is implied by the payment schedule.
   * Any dates in the known amount schedule refer to the payment schedule, not the payment date.
   * <p>
   * For example, consider a two year swap where each payment period is 3 months long.
   * This schedule could define two entries, one that defines the payment amounts as GBP 1000 for
   * the first year and one that defines the amount as GBP 500 for the second year.
   * In this case there will be eight payments in total, four payments of GBP 1000 in the first
   * year and four payments of GBP 500 in the second year.
   * Each payment will occur on the date specified using the offset in {@link PaymentSchedule}.
   * @return the value of the property, not null
   */
  public ValueSchedule getAmount() {
    return amount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency of the swap leg.
   * <p>
   * This is the currency of the known payments.
   * @return the value of the property, not null
   */
  @Override
  public Currency getCurrency() {
    return currency;
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
      KnownAmountSwapLeg other = (KnownAmountSwapLeg) obj;
      return JodaBeanUtils.equal(payReceive, other.payReceive) &&
          JodaBeanUtils.equal(accrualSchedule, other.accrualSchedule) &&
          JodaBeanUtils.equal(paymentSchedule, other.paymentSchedule) &&
          JodaBeanUtils.equal(amount, other.amount) &&
          JodaBeanUtils.equal(currency, other.currency);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(payReceive);
    hash = hash * 31 + JodaBeanUtils.hashCode(accrualSchedule);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentSchedule);
    hash = hash * 31 + JodaBeanUtils.hashCode(amount);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(288);
    buf.append("KnownAmountSwapLeg{");
    buf.append("payReceive").append('=').append(JodaBeanUtils.toString(payReceive)).append(',').append(' ');
    buf.append("accrualSchedule").append('=').append(JodaBeanUtils.toString(accrualSchedule)).append(',').append(' ');
    buf.append("paymentSchedule").append('=').append(JodaBeanUtils.toString(paymentSchedule)).append(',').append(' ');
    buf.append("amount").append('=').append(JodaBeanUtils.toString(amount)).append(',').append(' ');
    buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
    buf.append("type").append('=').append(JodaBeanUtils.toString(getType())).append(',').append(' ');
    buf.append("startDate").append('=').append(JodaBeanUtils.toString(getStartDate())).append(',').append(' ');
    buf.append("endDate").append('=').append(JodaBeanUtils.toString(getEndDate()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code KnownAmountSwapLeg}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code payReceive} property.
     */
    private final MetaProperty<PayReceive> payReceive = DirectMetaProperty.ofImmutable(
        this, "payReceive", KnownAmountSwapLeg.class, PayReceive.class);
    /**
     * The meta-property for the {@code accrualSchedule} property.
     */
    private final MetaProperty<PeriodicSchedule> accrualSchedule = DirectMetaProperty.ofImmutable(
        this, "accrualSchedule", KnownAmountSwapLeg.class, PeriodicSchedule.class);
    /**
     * The meta-property for the {@code paymentSchedule} property.
     */
    private final MetaProperty<PaymentSchedule> paymentSchedule = DirectMetaProperty.ofImmutable(
        this, "paymentSchedule", KnownAmountSwapLeg.class, PaymentSchedule.class);
    /**
     * The meta-property for the {@code amount} property.
     */
    private final MetaProperty<ValueSchedule> amount = DirectMetaProperty.ofImmutable(
        this, "amount", KnownAmountSwapLeg.class, ValueSchedule.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", KnownAmountSwapLeg.class, Currency.class);
    /**
     * The meta-property for the {@code type} property.
     */
    private final MetaProperty<SwapLegType> type = DirectMetaProperty.ofDerived(
        this, "type", KnownAmountSwapLeg.class, SwapLegType.class);
    /**
     * The meta-property for the {@code startDate} property.
     */
    private final MetaProperty<AdjustableDate> startDate = DirectMetaProperty.ofDerived(
        this, "startDate", KnownAmountSwapLeg.class, AdjustableDate.class);
    /**
     * The meta-property for the {@code endDate} property.
     */
    private final MetaProperty<AdjustableDate> endDate = DirectMetaProperty.ofDerived(
        this, "endDate", KnownAmountSwapLeg.class, AdjustableDate.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "payReceive",
        "accrualSchedule",
        "paymentSchedule",
        "amount",
        "currency",
        "type",
        "startDate",
        "endDate");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -885469925:  // payReceive
          return payReceive;
        case 304659814:  // accrualSchedule
          return accrualSchedule;
        case -1499086147:  // paymentSchedule
          return paymentSchedule;
        case -1413853096:  // amount
          return amount;
        case 575402001:  // currency
          return currency;
        case 3575610:  // type
          return type;
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public KnownAmountSwapLeg.Builder builder() {
      return new KnownAmountSwapLeg.Builder();
    }

    @Override
    public Class<? extends KnownAmountSwapLeg> beanType() {
      return KnownAmountSwapLeg.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code payReceive} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PayReceive> payReceive() {
      return payReceive;
    }

    /**
     * The meta-property for the {@code accrualSchedule} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PeriodicSchedule> accrualSchedule() {
      return accrualSchedule;
    }

    /**
     * The meta-property for the {@code paymentSchedule} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PaymentSchedule> paymentSchedule() {
      return paymentSchedule;
    }

    /**
     * The meta-property for the {@code amount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ValueSchedule> amount() {
      return amount;
    }

    /**
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    /**
     * The meta-property for the {@code type} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SwapLegType> type() {
      return type;
    }

    /**
     * The meta-property for the {@code startDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<AdjustableDate> startDate() {
      return startDate;
    }

    /**
     * The meta-property for the {@code endDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<AdjustableDate> endDate() {
      return endDate;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -885469925:  // payReceive
          return ((KnownAmountSwapLeg) bean).getPayReceive();
        case 304659814:  // accrualSchedule
          return ((KnownAmountSwapLeg) bean).getAccrualSchedule();
        case -1499086147:  // paymentSchedule
          return ((KnownAmountSwapLeg) bean).getPaymentSchedule();
        case -1413853096:  // amount
          return ((KnownAmountSwapLeg) bean).getAmount();
        case 575402001:  // currency
          return ((KnownAmountSwapLeg) bean).getCurrency();
        case 3575610:  // type
          return ((KnownAmountSwapLeg) bean).getType();
        case -2129778896:  // startDate
          return ((KnownAmountSwapLeg) bean).getStartDate();
        case -1607727319:  // endDate
          return ((KnownAmountSwapLeg) bean).getEndDate();
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
   * The bean-builder for {@code KnownAmountSwapLeg}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<KnownAmountSwapLeg> {

    private PayReceive payReceive;
    private PeriodicSchedule accrualSchedule;
    private PaymentSchedule paymentSchedule;
    private ValueSchedule amount;
    private Currency currency;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(KnownAmountSwapLeg beanToCopy) {
      this.payReceive = beanToCopy.getPayReceive();
      this.accrualSchedule = beanToCopy.getAccrualSchedule();
      this.paymentSchedule = beanToCopy.getPaymentSchedule();
      this.amount = beanToCopy.getAmount();
      this.currency = beanToCopy.getCurrency();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -885469925:  // payReceive
          return payReceive;
        case 304659814:  // accrualSchedule
          return accrualSchedule;
        case -1499086147:  // paymentSchedule
          return paymentSchedule;
        case -1413853096:  // amount
          return amount;
        case 575402001:  // currency
          return currency;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -885469925:  // payReceive
          this.payReceive = (PayReceive) newValue;
          break;
        case 304659814:  // accrualSchedule
          this.accrualSchedule = (PeriodicSchedule) newValue;
          break;
        case -1499086147:  // paymentSchedule
          this.paymentSchedule = (PaymentSchedule) newValue;
          break;
        case -1413853096:  // amount
          this.amount = (ValueSchedule) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
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
    public KnownAmountSwapLeg build() {
      return new KnownAmountSwapLeg(
          payReceive,
          accrualSchedule,
          paymentSchedule,
          amount,
          currency);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets whether the leg is pay or receive.
     * <p>
     * A value of 'Pay' implies that the resulting amount is paid to the counterparty.
     * A value of 'Receive' implies that the resulting amount is received from the counterparty.
     * Note that negative interest rates can result in a payment in the opposite
     * direction to that implied by this indicator.
     * @param payReceive  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder payReceive(PayReceive payReceive) {
      JodaBeanUtils.notNull(payReceive, "payReceive");
      this.payReceive = payReceive;
      return this;
    }

    /**
     * Sets the accrual period schedule.
     * <p>
     * This is used to define the accrual periods.
     * These are used directly or indirectly to determine other dates in the swap.
     * @param accrualSchedule  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder accrualSchedule(PeriodicSchedule accrualSchedule) {
      JodaBeanUtils.notNull(accrualSchedule, "accrualSchedule");
      this.accrualSchedule = accrualSchedule;
      return this;
    }

    /**
     * Sets the payment period schedule.
     * <p>
     * This is used to define the payment periods, including any compounding.
     * The payment period dates are based on the accrual schedule.
     * @param paymentSchedule  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentSchedule(PaymentSchedule paymentSchedule) {
      JodaBeanUtils.notNull(paymentSchedule, "paymentSchedule");
      this.paymentSchedule = paymentSchedule;
      return this;
    }

    /**
     * Sets the known amount schedule.
     * <p>
     * This defines the schedule of known amounts, relative to the payment schedule.
     * The schedule is defined as an initial amount, with optional changes during the tenor of the swap.
     * The amount is only permitted to change at payment period boundaries.
     * <p>
     * Note that the date of the payment is implied by the payment schedule.
     * Any dates in the known amount schedule refer to the payment schedule, not the payment date.
     * <p>
     * For example, consider a two year swap where each payment period is 3 months long.
     * This schedule could define two entries, one that defines the payment amounts as GBP 1000 for
     * the first year and one that defines the amount as GBP 500 for the second year.
     * In this case there will be eight payments in total, four payments of GBP 1000 in the first
     * year and four payments of GBP 500 in the second year.
     * Each payment will occur on the date specified using the offset in {@link PaymentSchedule}.
     * @param amount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder amount(ValueSchedule amount) {
      JodaBeanUtils.notNull(amount, "amount");
      this.amount = amount;
      return this;
    }

    /**
     * Sets the currency of the swap leg.
     * <p>
     * This is the currency of the known payments.
     * @param currency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder currency(Currency currency) {
      JodaBeanUtils.notNull(currency, "currency");
      this.currency = currency;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(288);
      buf.append("KnownAmountSwapLeg.Builder{");
      buf.append("payReceive").append('=').append(JodaBeanUtils.toString(payReceive)).append(',').append(' ');
      buf.append("accrualSchedule").append('=').append(JodaBeanUtils.toString(accrualSchedule)).append(',').append(' ');
      buf.append("paymentSchedule").append('=').append(JodaBeanUtils.toString(paymentSchedule)).append(',').append(' ');
      buf.append("amount").append('=').append(JodaBeanUtils.toString(amount)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("type").append('=').append(JodaBeanUtils.toString(null)).append(',').append(' ');
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(null)).append(',').append(' ');
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(null));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
