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
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.ArgChecker;

/**
 * An expanded swap leg, with dates calculated ready for pricing.
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
 * An {@code ExpandedSwapLeg} contains information based on holiday calendars.
 * If a holiday calendar changes, the adjusted dates may no longer be correct.
 * Care must be taken when placing the expanded form in a cache or persistence layer.
 */
@BeanDefinition
public final class ExpandedSwapLeg
    implements SwapLeg, ImmutableBean, Serializable {

  /**
   * The type of the leg, such as Fixed or Ibor.
   * <p>
   * This provides a high level categorization of the swap leg.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
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
  @PropertyDefinition(validate = "notNull", overrideGet = true)
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
  @PropertyDefinition(validate = "notEmpty", builderType = "List<? extends PaymentPeriod>")
  private final ImmutableList<PaymentPeriod> paymentPeriods;
  /**
   * The payment events that are associated with the swap leg.
   * <p>
   * Payment events include notional exchange and fees.
   */
  @PropertyDefinition(validate = "notNull", builderType = "List<? extends PaymentEvent>")
  private final ImmutableList<PaymentEvent> paymentEvents;
  /**
   * The currency of the leg.
   */
  private final Currency currency;  // not a property, derived and cached from input data

  //-------------------------------------------------------------------------
  @ImmutableConstructor
  private ExpandedSwapLeg(
      SwapLegType type,
      PayReceive payReceive,
      List<? extends PaymentPeriod> paymentPeriods,
      List<? extends PaymentEvent> paymentEvents) {

    this.type = ArgChecker.notNull(type, "type");
    this.payReceive = ArgChecker.notNull(payReceive, "payReceive");
    this.paymentPeriods = ImmutableList.copyOf(paymentPeriods);
    this.paymentEvents = ImmutableList.copyOf(paymentEvents);
    // determine and validate currency, with explicit error message
    Stream<Currency> periodCurrencies = paymentPeriods.stream().map(PaymentPeriod::getCurrency);
    Stream<Currency> eventCurrencies = paymentEvents.stream().map(PaymentEvent::getCurrency);
    Set<Currency> currencies = Stream.concat(periodCurrencies, eventCurrencies).collect(Collectors.toSet());
    if (currencies.size() > 1) {
      throw new IllegalArgumentException("Swap leg must have a single currency, found: " + currencies);
    }
    this.currency = Iterables.getOnlyElement(currencies);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the start date of the leg.
   * <p>
   * This is the first accrual date in the leg, often known as the effective date.
   * This date has been adjusted to be a valid business day.
   * 
   * @return the start date of the leg
   */
  @Override
  public LocalDate getStartDate() {
    return paymentPeriods.get(0).getStartDate();
  }

  /**
   * Gets the end date of the leg.
   * <p>
   * This is the last accrual date in the leg, often known as the maturity date.
   * This date has been adjusted to be a valid business day.
   * 
   * @return the end date of the leg
   */
  @Override
  public LocalDate getEndDate() {
    return paymentPeriods.get(paymentPeriods.size() - 1).getEndDate();
  }

  /**
   * Gets the currency of the swap leg.
   * <p>
   * All periods in the leg will have this currency.
   * 
   * @return the currency
   */
  @Override
  public Currency getCurrency() {
    return currency;
  }

  //-------------------------------------------------------------------------
  /**
   * Finds the payment period applicable on the specified date.
   * <p>
   * Each payment period is considered to contain the end date but not the start date.
   * If no payment period contains the date, an empty optional is returned.
   * 
   * @param date  the date to find
   * @return the payment period applicable at the date
   */
  public Optional<PaymentPeriod> findPaymentPeriod(LocalDate date) {
    return paymentPeriods.stream()
        .filter(period -> period.getStartDate().compareTo(date) < 0 && date.compareTo(period.getEndDate()) <= 0)
        .findFirst();
  }

  @Override
  public void collectIndices(ImmutableSet.Builder<Index> builder) {
    paymentPeriods.stream().forEach(period -> period.collectIndices(builder));
  }

  /**
   * Expands this swap leg, trivially returning {@code this}.
   * 
   * @return this swap leg
   */
  @Override
  public ExpandedSwapLeg expand() {
    return this;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ExpandedSwapLeg}.
   * @return the meta-bean, not null
   */
  public static ExpandedSwapLeg.Meta meta() {
    return ExpandedSwapLeg.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ExpandedSwapLeg.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ExpandedSwapLeg.Builder builder() {
    return new ExpandedSwapLeg.Builder();
  }

  @Override
  public ExpandedSwapLeg.Meta metaBean() {
    return ExpandedSwapLeg.Meta.INSTANCE;
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
  @Override
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
  @Override
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
  public ImmutableList<PaymentPeriod> getPaymentPeriods() {
    return paymentPeriods;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the payment events that are associated with the swap leg.
   * <p>
   * Payment events include notional exchange and fees.
   * @return the value of the property, not null
   */
  public ImmutableList<PaymentEvent> getPaymentEvents() {
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
      ExpandedSwapLeg other = (ExpandedSwapLeg) obj;
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
    buf.append("ExpandedSwapLeg{");
    buf.append("type").append('=').append(type).append(',').append(' ');
    buf.append("payReceive").append('=').append(payReceive).append(',').append(' ');
    buf.append("paymentPeriods").append('=').append(paymentPeriods).append(',').append(' ');
    buf.append("paymentEvents").append('=').append(JodaBeanUtils.toString(paymentEvents));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ExpandedSwapLeg}.
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
        this, "type", ExpandedSwapLeg.class, SwapLegType.class);
    /**
     * The meta-property for the {@code payReceive} property.
     */
    private final MetaProperty<PayReceive> payReceive = DirectMetaProperty.ofImmutable(
        this, "payReceive", ExpandedSwapLeg.class, PayReceive.class);
    /**
     * The meta-property for the {@code paymentPeriods} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<PaymentPeriod>> paymentPeriods = DirectMetaProperty.ofImmutable(
        this, "paymentPeriods", ExpandedSwapLeg.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code paymentEvents} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<PaymentEvent>> paymentEvents = DirectMetaProperty.ofImmutable(
        this, "paymentEvents", ExpandedSwapLeg.class, (Class) ImmutableList.class);
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
    public ExpandedSwapLeg.Builder builder() {
      return new ExpandedSwapLeg.Builder();
    }

    @Override
    public Class<? extends ExpandedSwapLeg> beanType() {
      return ExpandedSwapLeg.class;
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
    public MetaProperty<ImmutableList<PaymentPeriod>> paymentPeriods() {
      return paymentPeriods;
    }

    /**
     * The meta-property for the {@code paymentEvents} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<PaymentEvent>> paymentEvents() {
      return paymentEvents;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3575610:  // type
          return ((ExpandedSwapLeg) bean).getType();
        case -885469925:  // payReceive
          return ((ExpandedSwapLeg) bean).getPayReceive();
        case -1674414612:  // paymentPeriods
          return ((ExpandedSwapLeg) bean).getPaymentPeriods();
        case 1031856831:  // paymentEvents
          return ((ExpandedSwapLeg) bean).getPaymentEvents();
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
   * The bean-builder for {@code ExpandedSwapLeg}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ExpandedSwapLeg> {

    private SwapLegType type;
    private PayReceive payReceive;
    private List<? extends PaymentPeriod> paymentPeriods = ImmutableList.of();
    private List<? extends PaymentEvent> paymentEvents = ImmutableList.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ExpandedSwapLeg beanToCopy) {
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
          this.paymentPeriods = (List<? extends PaymentPeriod>) newValue;
          break;
        case 1031856831:  // paymentEvents
          this.paymentEvents = (List<? extends PaymentEvent>) newValue;
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
    public ExpandedSwapLeg build() {
      return new ExpandedSwapLeg(
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
    public Builder paymentPeriods(List<? extends PaymentPeriod> paymentPeriods) {
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
    public Builder paymentPeriods(PaymentPeriod... paymentPeriods) {
      return paymentPeriods(ImmutableList.copyOf(paymentPeriods));
    }

    /**
     * Sets the payment events that are associated with the swap leg.
     * <p>
     * Payment events include notional exchange and fees.
     * @param paymentEvents  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentEvents(List<? extends PaymentEvent> paymentEvents) {
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
    public Builder paymentEvents(PaymentEvent... paymentEvents) {
      return paymentEvents(ImmutableList.copyOf(paymentEvents));
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("ExpandedSwapLeg.Builder{");
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
