/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
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

import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.product.swap.InflationRateCalculation;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;

/**
 * A market convention for the floating leg of rate swap trades based on a price index.
 * <p>
 * This defines the market convention for a floating leg based on the observed value
 * of a Price index such as 'GB_HICP' or 'US_CPI_U'.
 */
@BeanDefinition
public final class InflationRateSwapLegConvention
    implements SwapLegConvention, ImmutableBean, Serializable {

  /**
   * The Price index.
   * <p>
   * The floating rate to be paid is based on this price index
   * It will be a well known price index such as 'GB_HICP'.
   */
  @PropertyDefinition(validate = "notNull")
  private final PriceIndex index;

  /**
   * The leg currency.
   * <p>
   * This is the currency of the swap leg and the currency that payment is made in.
   * The data model permits this currency to differ from that of the index,
   * however the two are typically the same.
   * <p>
   * This will default to the currency of the index if not specified.
   */
  @PropertyDefinition(get = "field")
  private final Currency currency;
  
  /**
   * Sets how the reference index calculation occurs.
   * <p>
   * If true, the reference index is linearly interpolated between two months. The interpolation is done with 
   * the number of days of the payment month. 
   * <p>
   * If false, the reference index is the price index of a month. The reference month is linked to the payment date
   */
  @PropertyDefinition(get = "field")
  private final boolean interpolated;
  
  /**
   * The flag indicating whether to exchange the notional.
   * <p>
   * If 'true', the notional there is both an initial exchange and a final exchange of notional.
   * <p>
   * This will default to 'false' if not specified.
   */
  @PropertyDefinition
  private final boolean notionalExchange;

  //-------------------------------------------------------------------------
  /**
   * Obtains a convention based on the specified index.
   * <p>
   * The standard market convention for an Inflation rate leg is based on the index
   * Use the {@linkplain #builder() builder} for unusual conventions.
   * 
   * @param index  the index, the market convention values are extracted from the index
   * @return the convention
   */
  public static InflationRateSwapLegConvention of(PriceIndex index) {
    return InflationRateSwapLegConvention.builder()
        .index(index)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the leg currency.
   * <p>
   * This is the currency of the swap leg and the currency that payment is made in.
   * The data model permits this currency to differ from that of the index,
   * however the two are typically the same.
   * <p>
   * This will default to the currency of the index if not specified.
   * 
   * @return the start date business day adjustment, not null
   */
  public Currency getCurrency() {
    return currency != null ? currency : index.getCurrency();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a leg based on this convention.
   * <p>
   * This returns a leg based on the specified date.
   * The notional is unsigned, with pay/receive determining the direction of the leg.
   * If the leg is 'Pay', the fixed rate is paid to the counterparty.
   * If the leg is 'Receive', the fixed rate is received from the counterparty.
   *
   * @param startDate  the start date
   * @param endDate  the end date
   * @param payReceive  determines if the leg is to be paid or received
   * @param lag  the positive period between the price index and the accrual date, typically a number of months
   * @param paymentDateOffset an adjustment that alters the payment date by adding a period of days
   * @param businessDayAdjustment the business day adjustment to apply. 
   * @param notional  the notional
   * @return the leg
   */
  public RateCalculationSwapLeg toLeg(
      LocalDate startDate,
      LocalDate endDate,
      PayReceive payReceive,
      Period lag,
      BusinessDayAdjustment businessDayAdjustment,
      DaysAdjustment paymentDateOffset,
      double notional) {

    return RateCalculationSwapLeg
        .builder()
        .payReceive(payReceive)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(startDate)
            .endDate(endDate)
            .frequency(Frequency.TERM)
            .businessDayAdjustment(businessDayAdjustment)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.TERM)
            .paymentDateOffset(paymentDateOffset)
            .build())
        .calculation(InflationRateCalculation.builder()
            .index(index)
            .interpolated(interpolated)
            .lag(lag)
            .build())
        .notionalSchedule(NotionalSchedule.of(getCurrency(), notional))
        .build();
  }
  
  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code InflationRateSwapLegConvention}.
   * @return the meta-bean, not null
   */
  public static InflationRateSwapLegConvention.Meta meta() {
    return InflationRateSwapLegConvention.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(InflationRateSwapLegConvention.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static InflationRateSwapLegConvention.Builder builder() {
    return new InflationRateSwapLegConvention.Builder();
  }

  private InflationRateSwapLegConvention(
      PriceIndex index,
      Currency currency,
      boolean interpolated,
      boolean notionalExchange) {
    JodaBeanUtils.notNull(index, "index");
    this.index = index;
    this.currency = currency;
    this.interpolated = interpolated;
    this.notionalExchange = notionalExchange;
  }

  @Override
  public InflationRateSwapLegConvention.Meta metaBean() {
    return InflationRateSwapLegConvention.Meta.INSTANCE;
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
   * Gets the Price index.
   * <p>
   * The floating rate to be paid is based on this price index
   * It will be a well known price index such as 'GB_HICP'.
   * @return the value of the property, not null
   */
  public PriceIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the flag indicating whether to exchange the notional.
   * <p>
   * If 'true', the notional there is both an initial exchange and a final exchange of notional.
   * <p>
   * This will default to 'false' if not specified.
   * @return the value of the property
   */
  public boolean isNotionalExchange() {
    return notionalExchange;
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
      InflationRateSwapLegConvention other = (InflationRateSwapLegConvention) obj;
      return JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          (interpolated == other.interpolated) &&
          (notionalExchange == other.notionalExchange);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(index);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(interpolated);
    hash = hash * 31 + JodaBeanUtils.hashCode(notionalExchange);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("InflationRateSwapLegConvention{");
    buf.append("index").append('=').append(index).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("interpolated").append('=').append(interpolated).append(',').append(' ');
    buf.append("notionalExchange").append('=').append(JodaBeanUtils.toString(notionalExchange));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code InflationRateSwapLegConvention}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<PriceIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", InflationRateSwapLegConvention.class, PriceIndex.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", InflationRateSwapLegConvention.class, Currency.class);
    /**
     * The meta-property for the {@code interpolated} property.
     */
    private final MetaProperty<Boolean> interpolated = DirectMetaProperty.ofImmutable(
        this, "interpolated", InflationRateSwapLegConvention.class, Boolean.TYPE);
    /**
     * The meta-property for the {@code notionalExchange} property.
     */
    private final MetaProperty<Boolean> notionalExchange = DirectMetaProperty.ofImmutable(
        this, "notionalExchange", InflationRateSwapLegConvention.class, Boolean.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "index",
        "currency",
        "interpolated",
        "notionalExchange");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case 575402001:  // currency
          return currency;
        case 2096252803:  // interpolated
          return interpolated;
        case -159410813:  // notionalExchange
          return notionalExchange;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public InflationRateSwapLegConvention.Builder builder() {
      return new InflationRateSwapLegConvention.Builder();
    }

    @Override
    public Class<? extends InflationRateSwapLegConvention> beanType() {
      return InflationRateSwapLegConvention.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code index} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PriceIndex> index() {
      return index;
    }

    /**
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    /**
     * The meta-property for the {@code interpolated} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Boolean> interpolated() {
      return interpolated;
    }

    /**
     * The meta-property for the {@code notionalExchange} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Boolean> notionalExchange() {
      return notionalExchange;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return ((InflationRateSwapLegConvention) bean).getIndex();
        case 575402001:  // currency
          return ((InflationRateSwapLegConvention) bean).currency;
        case 2096252803:  // interpolated
          return ((InflationRateSwapLegConvention) bean).interpolated;
        case -159410813:  // notionalExchange
          return ((InflationRateSwapLegConvention) bean).isNotionalExchange();
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
   * The bean-builder for {@code InflationRateSwapLegConvention}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<InflationRateSwapLegConvention> {

    private PriceIndex index;
    private Currency currency;
    private boolean interpolated;
    private boolean notionalExchange;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(InflationRateSwapLegConvention beanToCopy) {
      this.index = beanToCopy.getIndex();
      this.currency = beanToCopy.currency;
      this.interpolated = beanToCopy.interpolated;
      this.notionalExchange = beanToCopy.isNotionalExchange();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case 575402001:  // currency
          return currency;
        case 2096252803:  // interpolated
          return interpolated;
        case -159410813:  // notionalExchange
          return notionalExchange;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          this.index = (PriceIndex) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 2096252803:  // interpolated
          this.interpolated = (Boolean) newValue;
          break;
        case -159410813:  // notionalExchange
          this.notionalExchange = (Boolean) newValue;
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
    public InflationRateSwapLegConvention build() {
      return new InflationRateSwapLegConvention(
          index,
          currency,
          interpolated,
          notionalExchange);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the Price index.
     * <p>
     * The floating rate to be paid is based on this price index
     * It will be a well known price index such as 'GB_HICP'.
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(PriceIndex index) {
      JodaBeanUtils.notNull(index, "index");
      this.index = index;
      return this;
    }

    /**
     * Sets the leg currency.
     * <p>
     * This is the currency of the swap leg and the currency that payment is made in.
     * The data model permits this currency to differ from that of the index,
     * however the two are typically the same.
     * <p>
     * This will default to the currency of the index if not specified.
     * @param currency  the new value
     * @return this, for chaining, not null
     */
    public Builder currency(Currency currency) {
      this.currency = currency;
      return this;
    }

    /**
     * Sets sets how the reference index calculation occurs.
     * <p>
     * If true, the reference index is linearly interpolated between two months. The interpolation is done with
     * the number of days of the payment month.
     * <p>
     * If false, the reference index is the price index of a month. The reference month is linked to the payment date
     * @param interpolated  the new value
     * @return this, for chaining, not null
     */
    public Builder interpolated(boolean interpolated) {
      this.interpolated = interpolated;
      return this;
    }

    /**
     * Sets the flag indicating whether to exchange the notional.
     * <p>
     * If 'true', the notional there is both an initial exchange and a final exchange of notional.
     * <p>
     * This will default to 'false' if not specified.
     * @param notionalExchange  the new value
     * @return this, for chaining, not null
     */
    public Builder notionalExchange(boolean notionalExchange) {
      this.notionalExchange = notionalExchange;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("InflationRateSwapLegConvention.Builder{");
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("interpolated").append('=').append(JodaBeanUtils.toString(interpolated)).append(',').append(' ');
      buf.append("notionalExchange").append('=').append(JodaBeanUtils.toString(notionalExchange));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
