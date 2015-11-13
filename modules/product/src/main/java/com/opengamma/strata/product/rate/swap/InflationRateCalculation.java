/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate.swap;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.opengamma.strata.basics.value.ValueSchedule.ALWAYS_1;

import java.io.Serializable;
import java.time.Period;
import java.time.YearMonth;
import java.util.List;
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.basics.index.PriceIndices;
import com.opengamma.strata.basics.schedule.Schedule;
import com.opengamma.strata.basics.schedule.SchedulePeriod;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.rate.InflationInterpolatedRateObservation;
import com.opengamma.strata.product.rate.InflationMonthlyRateObservation;
import com.opengamma.strata.product.rate.RateObservation;

/**
 * Defines the calculation of a swap leg of a zero-coupon inflation coupon based on a price index. 
 * <p>
 * This defines the data necessary to calculate the amount payable on the leg.
 * The amount is based on the observed value of a price index.
 * <p>
 * The index for a given month is given in the yield curve or in the time series.
 * The pay-off for a unit notional is {@code (Index_End / Index_Start - 1)}.
 */
@BeanDefinition
public final class InflationRateCalculation
    implements RateCalculation, ImmutableBean, Serializable {

  /**
   * The index of prices.
   * <p>
   * The pay-off is computed based on this index
   * The most common implementations are provided in {@link PriceIndices}.
   */
  @PropertyDefinition(validate = "notNull")
  private final PriceIndex index;
  /**
   * The positive period between the price index and the accrual date,
   * typically a number of months.
   * <p>
   * A price index is typically published monthly and has a delay before publication.
   * The lag is subtracted from the accrual start and end date to locate the
   * month of the data to be observed.
   * <p>
   * For example, the September data may be published in October or November.
   * A 3 month lag will cause an accrual date in December to be based on the
   * observed data for September, which should be available by then.
   */
  @PropertyDefinition(validate = "notNull")
  private final Period lag;
  /**
   * How the reference index calculation occurs, defaulted to false.
   * <p>
   * If true, the reference index is linearly interpolated between two months.
   * The interpolation is done with the number of days of the payment month.
   * <p>
   * If false, the reference index is the price index of a month.
   * The reference month is linked to the payment date.
   */
  @PropertyDefinition(validate = "notNull")
  private final boolean interpolated;
  /**
   * The gearing multiplier, optional.
   * <p>
   * This defines the gearing as an initial value and a list of adjustments.
   * <p>
   * When calculating the index, the gearing acts as a overall factor of pay-off.
   * The pay-off is {@code Gearing_Factor * (Index_End / Index_Start - 1)}.
   * A gearing of 1 has no effect.
   * <p>
   * If this property is not present, then no gearing applies.
   * <p>
   * Gearing is also known as <i>leverage</i>.
   */
  @PropertyDefinition(get = "optional")
  private final ValueSchedule gearing;

  //-------------------------------------------------------------------------
  /**
   * Obtains a rate calculation for the specified price index.
   * <p>
   * The calculation will use the specified month lag.
   * All optional fields will be set to their default values.
   * Thus, fixing will be in advance, with no gearing.
   * If this method provides insufficient control, use the {@linkplain #builder() builder}.
   * 
   * @param index  the price index
   * @param monthLag  the month lag
   * @param isInterpolated  true if the reference index is interpolated
   * @return the inflation rate calculation
   */
  public static InflationRateCalculation of(PriceIndex index, int monthLag, boolean isInterpolated) {
    return InflationRateCalculation.builder()
        .index(index)
        .lag(Period.ofMonths(monthLag))
        .interpolated(isInterpolated)
        .build();
  }

  @ImmutableValidator
  private void validate() {
    ArgChecker.isFalse(lag.isZero() || lag.isNegative(), "Lag must be positive");
  }

  //-------------------------------------------------------------------------
  @Override
  public SwapLegType getType() {
    return SwapLegType.INFLATION;
  }

  @Override
  public DayCount getDayCount() {
    return DayCounts.ONE_ONE;  // inflation does not use a day count
  }

  @Override
  public void collectIndices(ImmutableSet.Builder<Index> builder) {
    builder.add(index);
  }

  @Override
  public ImmutableList<RateAccrualPeriod> expand(Schedule accrualSchedule, Schedule paymentSchedule) {
    ArgChecker.notNull(accrualSchedule, "accrualSchedule");
    ArgChecker.notNull(paymentSchedule, "paymentSchedule");
    // resolve data by schedule
    List<Double> resolvedGearings = firstNonNull(gearing, ALWAYS_1).resolveValues(accrualSchedule.getPeriods());
    // build accrual periods
    ImmutableList.Builder<RateAccrualPeriod> accrualPeriods = ImmutableList.builder();
    for (int i = 0; i < accrualSchedule.size(); i++) {
      SchedulePeriod period = accrualSchedule.getPeriod(i);
      accrualPeriods.add(RateAccrualPeriod.builder(period)
          .yearFraction(1d)  // inflation does not use a day count
          .rateObservation(createRateObservation(period))
          .gearing(resolvedGearings.get(i))
          .build());
    }
    return accrualPeriods.build();
  }

  // creates the rate observation
  private RateObservation createRateObservation(SchedulePeriod period) {
    YearMonth referenceStartMonth = YearMonth.from(period.getStartDate().minus(lag));
    YearMonth referenceEndMonth = YearMonth.from(period.getEndDate().minus(lag));
    if (interpolated) {
      // interpolate between data from two different months
      double weight = 1.0 - (period.getEndDate().getDayOfMonth() - 1.0) / period.getEndDate().lengthOfMonth();
      return InflationInterpolatedRateObservation.of(index, referenceStartMonth, referenceEndMonth, weight);
    } else {
      // no interpolation
      return InflationMonthlyRateObservation.of(index, referenceStartMonth, referenceEndMonth);
    }
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code InflationRateCalculation}.
   * @return the meta-bean, not null
   */
  public static InflationRateCalculation.Meta meta() {
    return InflationRateCalculation.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(InflationRateCalculation.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static InflationRateCalculation.Builder builder() {
    return new InflationRateCalculation.Builder();
  }

  private InflationRateCalculation(
      PriceIndex index,
      Period lag,
      boolean interpolated,
      ValueSchedule gearing) {
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(lag, "lag");
    JodaBeanUtils.notNull(interpolated, "interpolated");
    this.index = index;
    this.lag = lag;
    this.interpolated = interpolated;
    this.gearing = gearing;
    validate();
  }

  @Override
  public InflationRateCalculation.Meta metaBean() {
    return InflationRateCalculation.Meta.INSTANCE;
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
   * Gets the index of prices.
   * <p>
   * The pay-off is computed based on this index
   * The most common implementations are provided in {@link PriceIndices}.
   * @return the value of the property, not null
   */
  public PriceIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the positive period between the price index and the accrual date,
   * typically a number of months.
   * <p>
   * A price index is typically published monthly and has a delay before publication.
   * The lag is subtracted from the accrual start and end date to locate the
   * month of the data to be observed.
   * <p>
   * For example, the September data may be published in October or November.
   * A 3 month lag will cause an accrual date in December to be based on the
   * observed data for September, which should be available by then.
   * @return the value of the property, not null
   */
  public Period getLag() {
    return lag;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets how the reference index calculation occurs, defaulted to false.
   * <p>
   * If true, the reference index is linearly interpolated between two months.
   * The interpolation is done with the number of days of the payment month.
   * <p>
   * If false, the reference index is the price index of a month.
   * The reference month is linked to the payment date.
   * @return the value of the property, not null
   */
  public boolean isInterpolated() {
    return interpolated;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the gearing multiplier, optional.
   * <p>
   * This defines the gearing as an initial value and a list of adjustments.
   * <p>
   * When calculating the index, the gearing acts as a overall factor of pay-off.
   * The pay-off is {@code Gearing_Factor * (Index_End / Index_Start - 1)}.
   * A gearing of 1 has no effect.
   * <p>
   * If this property is not present, then no gearing applies.
   * <p>
   * Gearing is also known as <i>leverage</i>.
   * @return the optional value of the property, not null
   */
  public Optional<ValueSchedule> getGearing() {
    return Optional.ofNullable(gearing);
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
      InflationRateCalculation other = (InflationRateCalculation) obj;
      return JodaBeanUtils.equal(getIndex(), other.getIndex()) &&
          JodaBeanUtils.equal(getLag(), other.getLag()) &&
          (isInterpolated() == other.isInterpolated()) &&
          JodaBeanUtils.equal(gearing, other.gearing);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getIndex());
    hash = hash * 31 + JodaBeanUtils.hashCode(getLag());
    hash = hash * 31 + JodaBeanUtils.hashCode(isInterpolated());
    hash = hash * 31 + JodaBeanUtils.hashCode(gearing);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("InflationRateCalculation{");
    buf.append("index").append('=').append(getIndex()).append(',').append(' ');
    buf.append("lag").append('=').append(getLag()).append(',').append(' ');
    buf.append("interpolated").append('=').append(isInterpolated()).append(',').append(' ');
    buf.append("gearing").append('=').append(JodaBeanUtils.toString(gearing));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code InflationRateCalculation}.
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
        this, "index", InflationRateCalculation.class, PriceIndex.class);
    /**
     * The meta-property for the {@code lag} property.
     */
    private final MetaProperty<Period> lag = DirectMetaProperty.ofImmutable(
        this, "lag", InflationRateCalculation.class, Period.class);
    /**
     * The meta-property for the {@code interpolated} property.
     */
    private final MetaProperty<Boolean> interpolated = DirectMetaProperty.ofImmutable(
        this, "interpolated", InflationRateCalculation.class, Boolean.TYPE);
    /**
     * The meta-property for the {@code gearing} property.
     */
    private final MetaProperty<ValueSchedule> gearing = DirectMetaProperty.ofImmutable(
        this, "gearing", InflationRateCalculation.class, ValueSchedule.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "index",
        "lag",
        "interpolated",
        "gearing");

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
        case 106898:  // lag
          return lag;
        case 2096252803:  // interpolated
          return interpolated;
        case -91774989:  // gearing
          return gearing;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public InflationRateCalculation.Builder builder() {
      return new InflationRateCalculation.Builder();
    }

    @Override
    public Class<? extends InflationRateCalculation> beanType() {
      return InflationRateCalculation.class;
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
     * The meta-property for the {@code lag} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Period> lag() {
      return lag;
    }

    /**
     * The meta-property for the {@code interpolated} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Boolean> interpolated() {
      return interpolated;
    }

    /**
     * The meta-property for the {@code gearing} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ValueSchedule> gearing() {
      return gearing;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return ((InflationRateCalculation) bean).getIndex();
        case 106898:  // lag
          return ((InflationRateCalculation) bean).getLag();
        case 2096252803:  // interpolated
          return ((InflationRateCalculation) bean).isInterpolated();
        case -91774989:  // gearing
          return ((InflationRateCalculation) bean).gearing;
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
   * The bean-builder for {@code InflationRateCalculation}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<InflationRateCalculation> {

    private PriceIndex index;
    private Period lag;
    private boolean interpolated;
    private ValueSchedule gearing;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(InflationRateCalculation beanToCopy) {
      this.index = beanToCopy.getIndex();
      this.lag = beanToCopy.getLag();
      this.interpolated = beanToCopy.isInterpolated();
      this.gearing = beanToCopy.gearing;
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case 106898:  // lag
          return lag;
        case 2096252803:  // interpolated
          return interpolated;
        case -91774989:  // gearing
          return gearing;
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
        case 106898:  // lag
          this.lag = (Period) newValue;
          break;
        case 2096252803:  // interpolated
          this.interpolated = (Boolean) newValue;
          break;
        case -91774989:  // gearing
          this.gearing = (ValueSchedule) newValue;
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
    public InflationRateCalculation build() {
      return new InflationRateCalculation(
          index,
          lag,
          interpolated,
          gearing);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the index of prices.
     * <p>
     * The pay-off is computed based on this index
     * The most common implementations are provided in {@link PriceIndices}.
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(PriceIndex index) {
      JodaBeanUtils.notNull(index, "index");
      this.index = index;
      return this;
    }

    /**
     * Sets the positive period between the price index and the accrual date,
     * typically a number of months.
     * <p>
     * A price index is typically published monthly and has a delay before publication.
     * The lag is subtracted from the accrual start and end date to locate the
     * month of the data to be observed.
     * <p>
     * For example, the September data may be published in October or November.
     * A 3 month lag will cause an accrual date in December to be based on the
     * observed data for September, which should be available by then.
     * @param lag  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder lag(Period lag) {
      JodaBeanUtils.notNull(lag, "lag");
      this.lag = lag;
      return this;
    }

    /**
     * Sets how the reference index calculation occurs, defaulted to false.
     * <p>
     * If true, the reference index is linearly interpolated between two months.
     * The interpolation is done with the number of days of the payment month.
     * <p>
     * If false, the reference index is the price index of a month.
     * The reference month is linked to the payment date.
     * @param interpolated  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder interpolated(boolean interpolated) {
      JodaBeanUtils.notNull(interpolated, "interpolated");
      this.interpolated = interpolated;
      return this;
    }

    /**
     * Sets the gearing multiplier, optional.
     * <p>
     * This defines the gearing as an initial value and a list of adjustments.
     * <p>
     * When calculating the index, the gearing acts as a overall factor of pay-off.
     * The pay-off is {@code Gearing_Factor * (Index_End / Index_Start - 1)}.
     * A gearing of 1 has no effect.
     * <p>
     * If this property is not present, then no gearing applies.
     * <p>
     * Gearing is also known as <i>leverage</i>.
     * @param gearing  the new value
     * @return this, for chaining, not null
     */
    public Builder gearing(ValueSchedule gearing) {
      this.gearing = gearing;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("InflationRateCalculation.Builder{");
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("lag").append('=').append(JodaBeanUtils.toString(lag)).append(',').append(' ');
      buf.append("interpolated").append('=').append(JodaBeanUtils.toString(interpolated)).append(',').append(' ');
      buf.append("gearing").append('=').append(JodaBeanUtils.toString(gearing));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
