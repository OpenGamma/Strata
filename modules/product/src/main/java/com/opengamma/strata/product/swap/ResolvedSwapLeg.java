/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
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
import com.google.common.collect.Iterables;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.common.PayReceive;

/**
 * A resolved swap leg, with dates calculated ready for pricing.
 * <p>
 * A swap is a financial instrument that represents the exchange of streams of payments.
 * The swap is formed of legs, where each leg typically represents the obligations
 * of the seller or buyer of the swap.
 * <p>
 * This class defines a single swap leg in the form of a list of payment periods.
 * Each payment period typically consists of one or more accrual periods.
 * <p>
 * Any combination of payment and accrual periods is supported in the data model,
 * however there is no guarantee that exotic combinations will price sensibly.
 * <p>
 * All periods and events must be in the same currency.
 * <p>
 * A {@code ResolvedSwapLeg} contains information based on holiday calendars.
 * If a holiday calendar changes, the adjusted dates may no longer be correct.
 * Care must be taken when placing the resolved form in a cache or persistence layer.
 */
@BeanDefinition
public final class ResolvedSwapLeg
    implements ImmutableBean, Serializable {

  /**
   * The type of the leg, such as Fixed or Ibor.
   * <p>
   * This provides a high level categorization of the swap leg.
   */
  @PropertyDefinition(validate = "notNull")
  private final SwapLegType type;
  /**
   * Whether the leg is pay or receive.
   * <p>
   * A value of 'Pay' implies that the resulting amount is paid to the counterparty.
   * A value of 'Receive' implies that the resulting amount is received from the counterparty.
   * Note that negative interest rates can result in a payment in the opposite
   * direction to that implied by this indicator.
   * <p>
   * The value of this flag should match the signs of the payment period notionals.
   */
  @PropertyDefinition(validate = "notNull")
  private final PayReceive payReceive;
  /**
   * The payment periods that combine to form the swap leg.
   * <p>
   * Each payment period represents part of the life-time of the leg.
   * In most cases, the periods do not overlap. However, since each payment period
   * is essentially independent the data model allows overlapping periods.
   * <p>
   * The start date and end date of the leg are determined from the first and last period.
   * As such, the periods should be sorted.
   */
  @PropertyDefinition(validate = "notEmpty", builderType = "List<? extends SwapPaymentPeriod>")
  private final ImmutableList<SwapPaymentPeriod> paymentPeriods;
  /**
   * The payment events that are associated with the swap leg.
   * <p>
   * Payment events include notional exchange and fees.
   */
  @PropertyDefinition(validate = "notNull", builderType = "List<? extends SwapPaymentEvent>")
  private final ImmutableList<SwapPaymentEvent> paymentEvents;
  /**
   * The currency of the leg.
   */
  private final transient Currency currency;  // not a property, derived and cached from input data

  //-------------------------------------------------------------------------
  @ImmutableConstructor
  private ResolvedSwapLeg(
      SwapLegType type,
      PayReceive payReceive,
      List<? extends SwapPaymentPeriod> paymentPeriods,
      List<? extends SwapPaymentEvent> paymentEvents) {

    this.type = ArgChecker.notNull(type, "type");
    this.payReceive = ArgChecker.notNull(payReceive, "payReceive");
    this.paymentPeriods = ImmutableList.copyOf(paymentPeriods);
    this.paymentEvents = ImmutableList.copyOf(paymentEvents);
    // determine and validate currency, with explicit error message
    Stream<Currency> periodCurrencies = paymentPeriods.stream().map(SwapPaymentPeriod::getCurrency);
    Stream<Currency> eventCurrencies = paymentEvents.stream().map(SwapPaymentEvent::getCurrency);
    Set<Currency> currencies = Stream.concat(periodCurrencies, eventCurrencies).collect(Collectors.toSet());
    if (currencies.size() > 1) {
      throw new IllegalArgumentException("Swap leg must have a single currency, found: " + currencies);
    }
    this.currency = Iterables.getOnlyElement(currencies);
  }

  // trusted constructor
  ResolvedSwapLeg(
      SwapLegType type,
      PayReceive payReceive,
      List<? extends SwapPaymentPeriod> paymentPeriods,
      List<? extends SwapPaymentEvent> paymentEvents,
      Currency currency) {

    this.type = type;
    this.payReceive = payReceive;
    this.paymentPeriods = ImmutableList.copyOf(paymentPeriods);
    this.paymentEvents = ImmutableList.copyOf(paymentEvents);
    this.currency = currency;
  }

  // ensure standard constructor is invoked
  private Object readResolve() {
    return new ResolvedSwapLeg(type, payReceive, paymentPeriods, paymentEvents);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the accrual start date of the leg.
   * <p>
   * This is the first accrual date in the leg, often known as the effective date.
   * This date has typically been adjusted to be a valid business day.
   * 
   * @return the start date of the leg
   */
  public LocalDate getStartDate() {
    return paymentPeriods.get(0).getStartDate();
  }

  /**
   * Gets the accrual end date of the leg.
   * <p>
   * This is the last accrual date in the leg, often known as the termination date.
   * This date has typically been adjusted to be a valid business day.
   * 
   * @return the end date of the leg
   */
  public LocalDate getEndDate() {
    return paymentPeriods.get(paymentPeriods.size() - 1).getEndDate();
  }

  /**
   * Gets the primary currency of the swap leg.
   * <p>
   * Any currency associated with FX reset is not included.
   * 
   * @return the currency
   */
  public Currency getCurrency() {
    return currency;
  }

  //-------------------------------------------------------------------------
  /**
   * Finds the payment period applicable for the specified accrual date.
   * <p>
   * Each payment period contains one or more accrual periods.
   * This method finds the matching accrual period and returns the payment period that holds it.
   * Periods are considered to contain the end date but not the start date
   * If no accrual period contains the date, an empty optional is returned.
   * 
   * @param date  the date to find
   * @return the payment period applicable at the date
   */
  public Optional<SwapPaymentPeriod> findPaymentPeriod(LocalDate date) {
    return paymentPeriods.stream()
        .filter(period -> period.getStartDate().compareTo(date) < 0 && date.compareTo(period.getEndDate()) <= 0)
        .findFirst();
  }

  /**
   * Collects all the indices referred to by this leg.
   * <p>
   * A swap leg will typically refer to at least one index, such as 'GBP-LIBOR-3M'.
   * Each index that is referred to must be added to the specified builder.
   * 
   * @param builder  the builder to use
   */
  public void collectIndices(ImmutableSet.Builder<Index> builder) {
    paymentPeriods.stream().forEach(period -> period.collectIndices(builder));
  }

  /**
   * Finds the notional on the specified date.
   * <p>
   * If the date falls before the start, the initial notional will be returned.
   * If the date falls after the end, the final notional will be returned.
   * <p>
   * An empty optional is returned if the leg has no notional, for example if the payment amount
   * is known and explicitly specified.
   *
   * @param date  the date on which the notional is required
   * @return the notional on the specified date, if available
   */
  public Optional<CurrencyAmount> findNotional(LocalDate date) {
    SwapPaymentPeriod paymentPeriod;

    if (!date.isAfter(paymentPeriods.get(0).getStartDate())) {
      // Use the first payment period if the date is before the start
      paymentPeriod = paymentPeriods.get(0);
    } else if (date.isAfter(paymentPeriods.get(paymentPeriods.size() - 1).getEndDate())) {
      // Use the last payment period if the date is after the end
      paymentPeriod = paymentPeriods.get(paymentPeriods.size() - 1);
    } else {
      paymentPeriod = findPaymentPeriod(date).get();
    }
    if (!(paymentPeriod instanceof NotionalPaymentPeriod)) {
      return Optional.empty();
    }
    return Optional.of(((NotionalPaymentPeriod) paymentPeriod).getNotionalAmount());
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ResolvedSwapLeg}.
   * @return the meta-bean, not null
   */
  public static ResolvedSwapLeg.Meta meta() {
    return ResolvedSwapLeg.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ResolvedSwapLeg.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ResolvedSwapLeg.Builder builder() {
    return new ResolvedSwapLeg.Builder();
  }

  @Override
  public ResolvedSwapLeg.Meta metaBean() {
    return ResolvedSwapLeg.Meta.INSTANCE;
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
   * Gets the type of the leg, such as Fixed or Ibor.
   * <p>
   * This provides a high level categorization of the swap leg.
   * @return the value of the property, not null
   */
  public SwapLegType getType() {
    return type;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets whether the leg is pay or receive.
   * <p>
   * A value of 'Pay' implies that the resulting amount is paid to the counterparty.
   * A value of 'Receive' implies that the resulting amount is received from the counterparty.
   * Note that negative interest rates can result in a payment in the opposite
   * direction to that implied by this indicator.
   * <p>
   * The value of this flag should match the signs of the payment period notionals.
   * @return the value of the property, not null
   */
  public PayReceive getPayReceive() {
    return payReceive;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the payment periods that combine to form the swap leg.
   * <p>
   * Each payment period represents part of the life-time of the leg.
   * In most cases, the periods do not overlap. However, since each payment period
   * is essentially independent the data model allows overlapping periods.
   * <p>
   * The start date and end date of the leg are determined from the first and last period.
   * As such, the periods should be sorted.
   * @return the value of the property, not empty
   */
  public ImmutableList<SwapPaymentPeriod> getPaymentPeriods() {
    return paymentPeriods;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the payment events that are associated with the swap leg.
   * <p>
   * Payment events include notional exchange and fees.
   * @return the value of the property, not null
   */
  public ImmutableList<SwapPaymentEvent> getPaymentEvents() {
    return paymentEvents;
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
      ResolvedSwapLeg other = (ResolvedSwapLeg) obj;
      return JodaBeanUtils.equal(type, other.type) &&
          JodaBeanUtils.equal(payReceive, other.payReceive) &&
          JodaBeanUtils.equal(paymentPeriods, other.paymentPeriods) &&
          JodaBeanUtils.equal(paymentEvents, other.paymentEvents);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(type);
    hash = hash * 31 + JodaBeanUtils.hashCode(payReceive);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentPeriods);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentEvents);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("ResolvedSwapLeg{");
    buf.append("type").append('=').append(type).append(',').append(' ');
    buf.append("payReceive").append('=').append(payReceive).append(',').append(' ');
    buf.append("paymentPeriods").append('=').append(paymentPeriods).append(',').append(' ');
    buf.append("paymentEvents").append('=').append(JodaBeanUtils.toString(paymentEvents));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ResolvedSwapLeg}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code type} property.
     */
    private final MetaProperty<SwapLegType> type = DirectMetaProperty.ofImmutable(
        this, "type", ResolvedSwapLeg.class, SwapLegType.class);
    /**
     * The meta-property for the {@code payReceive} property.
     */
    private final MetaProperty<PayReceive> payReceive = DirectMetaProperty.ofImmutable(
        this, "payReceive", ResolvedSwapLeg.class, PayReceive.class);
    /**
     * The meta-property for the {@code paymentPeriods} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<SwapPaymentPeriod>> paymentPeriods = DirectMetaProperty.ofImmutable(
        this, "paymentPeriods", ResolvedSwapLeg.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code paymentEvents} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<SwapPaymentEvent>> paymentEvents = DirectMetaProperty.ofImmutable(
        this, "paymentEvents", ResolvedSwapLeg.class, (Class) ImmutableList.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "type",
        "payReceive",
        "paymentPeriods",
        "paymentEvents");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3575610:  // type
          return type;
        case -885469925:  // payReceive
          return payReceive;
        case -1674414612:  // paymentPeriods
          return paymentPeriods;
        case 1031856831:  // paymentEvents
          return paymentEvents;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ResolvedSwapLeg.Builder builder() {
      return new ResolvedSwapLeg.Builder();
    }

    @Override
    public Class<? extends ResolvedSwapLeg> beanType() {
      return ResolvedSwapLeg.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code type} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SwapLegType> type() {
      return type;
    }

    /**
     * The meta-property for the {@code payReceive} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PayReceive> payReceive() {
      return payReceive;
    }

    /**
     * The meta-property for the {@code paymentPeriods} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<SwapPaymentPeriod>> paymentPeriods() {
      return paymentPeriods;
    }

    /**
     * The meta-property for the {@code paymentEvents} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<SwapPaymentEvent>> paymentEvents() {
      return paymentEvents;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3575610:  // type
          return ((ResolvedSwapLeg) bean).getType();
        case -885469925:  // payReceive
          return ((ResolvedSwapLeg) bean).getPayReceive();
        case -1674414612:  // paymentPeriods
          return ((ResolvedSwapLeg) bean).getPaymentPeriods();
        case 1031856831:  // paymentEvents
          return ((ResolvedSwapLeg) bean).getPaymentEvents();
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
   * The bean-builder for {@code ResolvedSwapLeg}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ResolvedSwapLeg> {

    private SwapLegType type;
    private PayReceive payReceive;
    private List<? extends SwapPaymentPeriod> paymentPeriods = ImmutableList.of();
    private List<? extends SwapPaymentEvent> paymentEvents = ImmutableList.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ResolvedSwapLeg beanToCopy) {
      this.type = beanToCopy.getType();
      this.payReceive = beanToCopy.getPayReceive();
      this.paymentPeriods = beanToCopy.getPaymentPeriods();
      this.paymentEvents = beanToCopy.getPaymentEvents();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3575610:  // type
          return type;
        case -885469925:  // payReceive
          return payReceive;
        case -1674414612:  // paymentPeriods
          return paymentPeriods;
        case 1031856831:  // paymentEvents
          return paymentEvents;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3575610:  // type
          this.type = (SwapLegType) newValue;
          break;
        case -885469925:  // payReceive
          this.payReceive = (PayReceive) newValue;
          break;
        case -1674414612:  // paymentPeriods
          this.paymentPeriods = (List<? extends SwapPaymentPeriod>) newValue;
          break;
        case 1031856831:  // paymentEvents
          this.paymentEvents = (List<? extends SwapPaymentEvent>) newValue;
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
    public ResolvedSwapLeg build() {
      return new ResolvedSwapLeg(
          type,
          payReceive,
          paymentPeriods,
          paymentEvents);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the type of the leg, such as Fixed or Ibor.
     * <p>
     * This provides a high level categorization of the swap leg.
     * @param type  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder type(SwapLegType type) {
      JodaBeanUtils.notNull(type, "type");
      this.type = type;
      return this;
    }

    /**
     * Sets whether the leg is pay or receive.
     * <p>
     * A value of 'Pay' implies that the resulting amount is paid to the counterparty.
     * A value of 'Receive' implies that the resulting amount is received from the counterparty.
     * Note that negative interest rates can result in a payment in the opposite
     * direction to that implied by this indicator.
     * <p>
     * The value of this flag should match the signs of the payment period notionals.
     * @param payReceive  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder payReceive(PayReceive payReceive) {
      JodaBeanUtils.notNull(payReceive, "payReceive");
      this.payReceive = payReceive;
      return this;
    }

    /**
     * Sets the payment periods that combine to form the swap leg.
     * <p>
     * Each payment period represents part of the life-time of the leg.
     * In most cases, the periods do not overlap. However, since each payment period
     * is essentially independent the data model allows overlapping periods.
     * <p>
     * The start date and end date of the leg are determined from the first and last period.
     * As such, the periods should be sorted.
     * @param paymentPeriods  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder paymentPeriods(List<? extends SwapPaymentPeriod> paymentPeriods) {
      JodaBeanUtils.notEmpty(paymentPeriods, "paymentPeriods");
      this.paymentPeriods = paymentPeriods;
      return this;
    }

    /**
     * Sets the {@code paymentPeriods} property in the builder
     * from an array of objects.
     * @param paymentPeriods  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder paymentPeriods(SwapPaymentPeriod... paymentPeriods) {
      return paymentPeriods(ImmutableList.copyOf(paymentPeriods));
    }

    /**
     * Sets the payment events that are associated with the swap leg.
     * <p>
     * Payment events include notional exchange and fees.
     * @param paymentEvents  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentEvents(List<? extends SwapPaymentEvent> paymentEvents) {
      JodaBeanUtils.notNull(paymentEvents, "paymentEvents");
      this.paymentEvents = paymentEvents;
      return this;
    }

    /**
     * Sets the {@code paymentEvents} property in the builder
     * from an array of objects.
     * @param paymentEvents  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentEvents(SwapPaymentEvent... paymentEvents) {
      return paymentEvents(ImmutableList.copyOf(paymentEvents));
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("ResolvedSwapLeg.Builder{");
      buf.append("type").append('=').append(JodaBeanUtils.toString(type)).append(',').append(' ');
      buf.append("payReceive").append('=').append(JodaBeanUtils.toString(payReceive)).append(',').append(' ');
      buf.append("paymentPeriods").append('=').append(JodaBeanUtils.toString(paymentPeriods)).append(',').append(' ');
      buf.append("paymentEvents").append('=').append(JodaBeanUtils.toString(paymentEvents));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
