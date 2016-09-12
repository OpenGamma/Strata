/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.opengamma.strata.basics.value.ValueSchedule.ALWAYS_1;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalDouble;
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
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.basics.index.PriceIndices;
import com.opengamma.strata.basics.schedule.Schedule;
import com.opengamma.strata.basics.schedule.SchedulePeriod;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.product.rate.InflationEndInterpolatedRateComputation;
import com.opengamma.strata.product.rate.InflationEndMonthRateComputation;
import com.opengamma.strata.product.rate.InflationInterpolatedRateComputation;
import com.opengamma.strata.product.rate.InflationMonthlyRateComputation;
import com.opengamma.strata.product.rate.RateComputation;

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
   * Reference price index calculation method.
   * <p>
   * This specifies how the reference index calculation occurs.
   */
  @PropertyDefinition(validate = "notNull")
  private final PriceIndexCalculationMethod indexCalculationMethod;
  /**
   * The initial value of the index, optional.
   * <p>
   * This optional field specifies the initial value of the index.
   * The value is applicable for the first <i>regular</i> accrual period.
   * It is used in place of an observed fixing.
   * Other calculation elements, such as gearing or spread, still apply.
   * After the first accrual period, the rate is observed via the normal fixing process.
   * <p>
   * The method {@link InflationRateCalculation#createRateComputation(LocalDate)}
   * allows this field to be used as the base for any end date, as typically seen
   * in capital indexed bonds.
   * <p>
   * If this property is not present, then the first value is observed via the normal fixing process.
   */
  @PropertyDefinition(get = "optional")
  private final Double firstIndexValue;
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
   * @param indexCalculationMethod  the reference price index calculation method
   * @return the inflation rate calculation
   */
  public static InflationRateCalculation of(
      PriceIndex index,
      int monthLag,
      PriceIndexCalculationMethod indexCalculationMethod) {

    return InflationRateCalculation.builder()
        .index(index)
        .lag(Period.ofMonths(monthLag))
        .indexCalculationMethod(indexCalculationMethod)
        .build();
  }

  /**
   * Obtains a rate calculation for the specified price index with known start index value.
   * <p>
   * The calculation will use the specified month lag.
   * The first index value will be set to the specified value
   * All other optional fields will be set to their default values.
   * Thus, fixing will be in advance, with no gearing.
   * If this method provides insufficient control, use the {@linkplain #builder() builder}.
   * 
   * @param index  the price index
   * @param monthLag  the month lag
   * @param indexCalculationMethod  the reference price index calculation method
   * @param firstIndexValue  the first index value
   * @return the inflation rate calculation
   */
  public static InflationRateCalculation of(
      PriceIndex index,
      int monthLag,
      PriceIndexCalculationMethod indexCalculationMethod,
      double firstIndexValue) {

    return InflationRateCalculation.builder()
        .index(index)
        .lag(Period.ofMonths(monthLag))
        .indexCalculationMethod(indexCalculationMethod)
        .firstIndexValue(firstIndexValue)
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
  public ImmutableList<RateAccrualPeriod> createAccrualPeriods(
      Schedule accrualSchedule,
      Schedule paymentSchedule,
      ReferenceData refData) {

    // resolve data by schedule
    DoubleArray resolvedGearings = firstNonNull(gearing, ALWAYS_1).resolveValues(accrualSchedule);
    // build accrual periods
    ImmutableList.Builder<RateAccrualPeriod> accrualPeriods = ImmutableList.builder();
    for (int i = 0; i < accrualSchedule.size(); i++) {
      SchedulePeriod period = accrualSchedule.getPeriod(i);
      // inflation does not use a day count, so year fraction is 1d
      accrualPeriods.add(new RateAccrualPeriod(
          period, 1d, createRateComputation(period, i), resolvedGearings.get(i), 0d, NegativeRateMethod.ALLOW_NEGATIVE));
    }
    return accrualPeriods.build();
  }

  // creates the rate computation
  private RateComputation createRateComputation(SchedulePeriod period, int scheduleIndex) {

    // handle where index value at start date is known
    LocalDate endDate = period.getEndDate();
    if (firstIndexValue != null && scheduleIndex == 0) {
      return createRateComputation(endDate);
    }
    YearMonth referenceStartMonth = YearMonth.from(period.getStartDate().minus(lag));
    YearMonth referenceEndMonth = YearMonth.from(endDate.minus(lag));
    if (indexCalculationMethod.equals(PriceIndexCalculationMethod.INTERPOLATED)) {
      // interpolate between data from two different months
      double weight = 1d - (endDate.getDayOfMonth() - 1d) / endDate.lengthOfMonth();
      return InflationInterpolatedRateComputation.of(index, referenceStartMonth, referenceEndMonth, weight);
    } else if (indexCalculationMethod.equals(PriceIndexCalculationMethod.MONTHLY)) {
      // no interpolation
      return InflationMonthlyRateComputation.of(index, referenceStartMonth, referenceEndMonth);
    } else {
      throw new IllegalArgumentException(
          "PriceIndexCalculationMethod " + indexCalculationMethod.toString() + " is not supported");
    }
  }

  /**
   * Creates a rate observation where the start index value is known.
   * <p>
   * This is typically used for capital indexed bonds.
   * The rate is calculated between the value of {@code firstIndexValue}
   * and the observed value at the end month linked to the specified end date.
   * This method requires that {@code firstIndexValue} is present.
   * 
   * @param endDate  the end date of the period
   * @return the rate observation
   */
  public RateComputation createRateComputation(LocalDate endDate) {
    if (firstIndexValue == null) {
      throw new IllegalStateException("First index value must be specified");
    }
    YearMonth referenceEndMonth = YearMonth.from(endDate.minus(lag));
    if (indexCalculationMethod.equals(PriceIndexCalculationMethod.INTERPOLATED)) {
      // interpolate between data from two different months
      double weight = 1d - (endDate.getDayOfMonth() - 1d) / endDate.lengthOfMonth();
      return InflationEndInterpolatedRateComputation.of(index, firstIndexValue, referenceEndMonth, weight);
    } else if (indexCalculationMethod.equals(PriceIndexCalculationMethod.MONTHLY)) {
      // no interpolation
      return InflationEndMonthRateComputation.of(index, firstIndexValue, referenceEndMonth);
    } else if (indexCalculationMethod.equals(PriceIndexCalculationMethod.INTERPOLATED_JAPAN)) {
      // interpolation, Japan
      double weight = 1d;
      int dayOfMonth = endDate.getDayOfMonth();
      if (dayOfMonth > 10) {
        weight -= (dayOfMonth - 10d) / endDate.lengthOfMonth();
      } else if (dayOfMonth < 10) {
        weight -= (dayOfMonth + endDate.minusMonths(1).lengthOfMonth() - 10d) / endDate.minusMonths(1).lengthOfMonth();
        referenceEndMonth = referenceEndMonth.minusMonths(1);
      }
      return InflationEndInterpolatedRateComputation.of(index, firstIndexValue, referenceEndMonth, weight);
    } else {
      throw new IllegalArgumentException(
          "PriceIndexCalculationMethod " + indexCalculationMethod.toString() + " is not supported");
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
      PriceIndexCalculationMethod indexCalculationMethod,
      Double firstIndexValue,
      ValueSchedule gearing) {
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(lag, "lag");
    JodaBeanUtils.notNull(indexCalculationMethod, "indexCalculationMethod");
    this.index = index;
    this.lag = lag;
    this.indexCalculationMethod = indexCalculationMethod;
    this.firstIndexValue = firstIndexValue;
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
   * Gets reference price index calculation method.
   * <p>
   * This specifies how the reference index calculation occurs.
   * @return the value of the property, not null
   */
  public PriceIndexCalculationMethod getIndexCalculationMethod() {
    return indexCalculationMethod;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the initial value of the index, optional.
   * <p>
   * This optional field specifies the initial value of the index.
   * The value is applicable for the first <i>regular</i> accrual period.
   * It is used in place of an observed fixing.
   * Other calculation elements, such as gearing or spread, still apply.
   * After the first accrual period, the rate is observed via the normal fixing process.
   * <p>
   * The method {@link InflationRateCalculation#createRateComputation(LocalDate)}
   * allows this field to be used as the base for any end date, as typically seen
   * in capital indexed bonds.
   * <p>
   * If this property is not present, then the first value is observed via the normal fixing process.
   * @return the optional value of the property, not null
   */
  public OptionalDouble getFirstIndexValue() {
    return firstIndexValue != null ? OptionalDouble.of(firstIndexValue) : OptionalDouble.empty();
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
      return JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(lag, other.lag) &&
          JodaBeanUtils.equal(indexCalculationMethod, other.indexCalculationMethod) &&
          JodaBeanUtils.equal(firstIndexValue, other.firstIndexValue) &&
          JodaBeanUtils.equal(gearing, other.gearing);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(index);
    hash = hash * 31 + JodaBeanUtils.hashCode(lag);
    hash = hash * 31 + JodaBeanUtils.hashCode(indexCalculationMethod);
    hash = hash * 31 + JodaBeanUtils.hashCode(firstIndexValue);
    hash = hash * 31 + JodaBeanUtils.hashCode(gearing);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("InflationRateCalculation{");
    buf.append("index").append('=').append(index).append(',').append(' ');
    buf.append("lag").append('=').append(lag).append(',').append(' ');
    buf.append("indexCalculationMethod").append('=').append(indexCalculationMethod).append(',').append(' ');
    buf.append("firstIndexValue").append('=').append(firstIndexValue).append(',').append(' ');
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
     * The meta-property for the {@code indexCalculationMethod} property.
     */
    private final MetaProperty<PriceIndexCalculationMethod> indexCalculationMethod = DirectMetaProperty.ofImmutable(
        this, "indexCalculationMethod", InflationRateCalculation.class, PriceIndexCalculationMethod.class);
    /**
     * The meta-property for the {@code firstIndexValue} property.
     */
    private final MetaProperty<Double> firstIndexValue = DirectMetaProperty.ofImmutable(
        this, "firstIndexValue", InflationRateCalculation.class, Double.class);
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
        "indexCalculationMethod",
        "firstIndexValue",
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
        case -1409010088:  // indexCalculationMethod
          return indexCalculationMethod;
        case 922631823:  // firstIndexValue
          return firstIndexValue;
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
     * The meta-property for the {@code indexCalculationMethod} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PriceIndexCalculationMethod> indexCalculationMethod() {
      return indexCalculationMethod;
    }

    /**
     * The meta-property for the {@code firstIndexValue} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> firstIndexValue() {
      return firstIndexValue;
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
        case -1409010088:  // indexCalculationMethod
          return ((InflationRateCalculation) bean).getIndexCalculationMethod();
        case 922631823:  // firstIndexValue
          return ((InflationRateCalculation) bean).firstIndexValue;
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
    private PriceIndexCalculationMethod indexCalculationMethod;
    private Double firstIndexValue;
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
      this.indexCalculationMethod = beanToCopy.getIndexCalculationMethod();
      this.firstIndexValue = beanToCopy.firstIndexValue;
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
        case -1409010088:  // indexCalculationMethod
          return indexCalculationMethod;
        case 922631823:  // firstIndexValue
          return firstIndexValue;
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
        case -1409010088:  // indexCalculationMethod
          this.indexCalculationMethod = (PriceIndexCalculationMethod) newValue;
          break;
        case 922631823:  // firstIndexValue
          this.firstIndexValue = (Double) newValue;
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
          indexCalculationMethod,
          firstIndexValue,
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
     * Sets reference price index calculation method.
     * <p>
     * This specifies how the reference index calculation occurs.
     * @param indexCalculationMethod  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder indexCalculationMethod(PriceIndexCalculationMethod indexCalculationMethod) {
      JodaBeanUtils.notNull(indexCalculationMethod, "indexCalculationMethod");
      this.indexCalculationMethod = indexCalculationMethod;
      return this;
    }

    /**
     * Sets the initial value of the index, optional.
     * <p>
     * This optional field specifies the initial value of the index.
     * The value is applicable for the first <i>regular</i> accrual period.
     * It is used in place of an observed fixing.
     * Other calculation elements, such as gearing or spread, still apply.
     * After the first accrual period, the rate is observed via the normal fixing process.
     * <p>
     * The method {@link InflationRateCalculation#createRateComputation(LocalDate)}
     * allows this field to be used as the base for any end date, as typically seen
     * in capital indexed bonds.
     * <p>
     * If this property is not present, then the first value is observed via the normal fixing process.
     * @param firstIndexValue  the new value
     * @return this, for chaining, not null
     */
    public Builder firstIndexValue(Double firstIndexValue) {
      this.firstIndexValue = firstIndexValue;
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
      StringBuilder buf = new StringBuilder(192);
      buf.append("InflationRateCalculation.Builder{");
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("lag").append('=').append(JodaBeanUtils.toString(lag)).append(',').append(' ');
      buf.append("indexCalculationMethod").append('=').append(JodaBeanUtils.toString(indexCalculationMethod)).append(',').append(' ');
      buf.append("firstIndexValue").append('=').append(JodaBeanUtils.toString(firstIndexValue)).append(',').append(' ');
      buf.append("gearing").append('=').append(JodaBeanUtils.toString(gearing));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
