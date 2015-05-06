package com.opengamma.strata.finance.rate.swap;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.opengamma.strata.basics.value.ValueSchedule.ALWAYS_1;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.basics.index.PriceIndices;
import com.opengamma.strata.basics.schedule.Schedule;
import com.opengamma.strata.basics.schedule.SchedulePeriod;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.rate.InflationInterpolatedRateObservation;
import com.opengamma.strata.finance.rate.InflationMonthlyRateObservation;
import com.opengamma.strata.finance.rate.RateObservation;

/**
 * Defines the calculation of a swap leg of a zero-coupon inflation coupon based on a price index. 
 * <p>
 * This defines the data necessary to calculate the amount payable on the leg.
 * The amount is based on the observed value of a price index.
 * <p>
 * The index for a given month is given in the yield curve or in the time series on the first of the month.
 * The pay-off for a unit notional is (Index_End / Index_Start - 1).
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
   * The lag in month between the index validity and the coupon dates for the actual product. 
   */
  @PropertyDefinition(validate = "ArgChecker.notNegative")
  private final int monthLag;
  /**
   * Defines the calculation of the reference index. 
   * <p>
   * If true, the reference index is linearly interpolated between two months. 
   * The interpolation is done with the number of days of the payment month. 
   * <p>
   * If false, the reference index is the price index of a month.  
   * The reference month is linked to the payment date. 
   * 
   */
  @PropertyDefinition(validate = "notNull")
  private final boolean isInterpolated;
  /**
   * The gearing multiplier, optional.
   * <p>
   * This defines the gearing as an initial value and a list of adjustments.
   * <p>
   * When calculating the index, the gearing act as a overall factor of pay-off, 
   * i.e., the pay-off is Gearing_Factor * (Index_End / Index_Start - 1).
   * A gearing of 1 has no effect.
   * <p>
   * If this property is not present, then no gearing applies.
   * <p>
   * Gearing is also known as <i>leverage</i>.
   */
  @PropertyDefinition(get = "optional")
  private final ValueSchedule gearing;

  /**
   * Creates {@link InflationRateCalculation} from price index, month lag and isInterpolated. 
   * @param index The price index. 
   * @param monthLag The month lag. 
   * @param isInterpolated If true, the reference index is interpolated. 
   * @return The inflation rate calculation
   */
  public static InflationRateCalculation of(PriceIndex index, int monthLag, boolean isInterpolated) {
    return InflationRateCalculation.builder().index(index).monthLag(monthLag).isInterpolated(isInterpolated).build();
  }

  /**
   * Creates {@link InflationRateCalculation} from price index, month lag, isInterpolated and ValueSchedule. 
   * @param index The price index.  
   * @param monthLag The month lag. 
   * @param isInterpolated If true, the reference index is interpolated. 
   * @param gearing The gearing multiplier. 
   * @return The inflation rate calculation
   */
  public static InflationRateCalculation of(
      PriceIndex index,
      int monthLag,
      boolean isInterpolated,
      ValueSchedule gearing) {
    return InflationRateCalculation.builder()
        .index(index)
        .monthLag(monthLag)
        .isInterpolated(isInterpolated)
        .gearing(gearing)
        .build();
  }

  @Override
  public SwapLegType getType() {
    return SwapLegType.INFLATION;
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
          .yearFraction(1.0)
          .rateObservation(createRateObservation(period))
          .gearing(resolvedGearings.get(i))
          .build());
    }
    return accrualPeriods.build();
  }

  // creates the rate observation
  private RateObservation createRateObservation(SchedulePeriod period) {
    // TODO  need adjuster (c.f., DaysAdjustment for Ibor)?
    if (isInterpolated) {
      YearMonth referenceStartMonths1 = YearMonth.from(period.getStartDate().minusMonths(monthLag));
      YearMonth referenceStartMonths2 = YearMonth.from(referenceStartMonths1.plusMonths(1));
      YearMonth referenceEndMonths1 = YearMonth.from(period.getEndDate().minusMonths(monthLag));
      YearMonth referenceEndMonths2 = YearMonth.from(referenceEndMonths1.plusMonths(1));
      double weight = 1.0 - (period.getEndDate().getDayOfMonth() - 1.0) / period.getEndDate().lengthOfMonth();
      return InflationInterpolatedRateObservation.of(
          index, referenceStartMonths1, referenceStartMonths2, referenceEndMonths1, referenceEndMonths2, weight);
    }
    LocalDate referenceStartDate = period.getStartDate().minusMonths(monthLag).with(TemporalAdjusters.lastDayOfMonth());
    LocalDate referenceEndDate = period.getEndDate().minusMonths(monthLag).with(TemporalAdjusters.lastDayOfMonth());
    return InflationMonthlyRateObservation.of(
        index, YearMonth.from(referenceStartDate), YearMonth.from(referenceEndDate));
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
      int monthLag,
      boolean isInterpolated,
      ValueSchedule gearing) {
    JodaBeanUtils.notNull(index, "index");
    ArgChecker.notNegative(monthLag, "monthLag");
    JodaBeanUtils.notNull(isInterpolated, "isInterpolated");
    this.index = index;
    this.monthLag = monthLag;
    this.isInterpolated = isInterpolated;
    this.gearing = gearing;
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
   * Gets the lag in month between the index validity and the coupon dates for the actual product.
   * @return the value of the property
   */
  public int getMonthLag() {
    return monthLag;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets defines the calculation of the reference index.
   * <p>
   * If true, the reference index is linearly interpolated between two months.
   * The interpolation is done with the number of days of the payment month.
   * <p>
   * If false, the reference index is the price index of a month.
   * The reference month is linked to the payment date.
   * 
   * @return the value of the property, not null
   */
  public boolean isIsInterpolated() {
    return isInterpolated;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the gearing multiplier, optional.
   * <p>
   * This defines the gearing as an initial value and a list of adjustments.
   * <p>
   * When calculating the index, the gearing act as a overall factor of pay-off,
   * i.e., the pay-off is Gearing_Factor * (Index_End / Index_Start - 1).
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
          (getMonthLag() == other.getMonthLag()) &&
          (isIsInterpolated() == other.isIsInterpolated()) &&
          JodaBeanUtils.equal(gearing, other.gearing);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getIndex());
    hash = hash * 31 + JodaBeanUtils.hashCode(getMonthLag());
    hash = hash * 31 + JodaBeanUtils.hashCode(isIsInterpolated());
    hash = hash * 31 + JodaBeanUtils.hashCode(gearing);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("InflationRateCalculation{");
    buf.append("index").append('=').append(getIndex()).append(',').append(' ');
    buf.append("monthLag").append('=').append(getMonthLag()).append(',').append(' ');
    buf.append("isInterpolated").append('=').append(isIsInterpolated()).append(',').append(' ');
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
     * The meta-property for the {@code monthLag} property.
     */
    private final MetaProperty<Integer> monthLag = DirectMetaProperty.ofImmutable(
        this, "monthLag", InflationRateCalculation.class, Integer.TYPE);
    /**
     * The meta-property for the {@code isInterpolated} property.
     */
    private final MetaProperty<Boolean> isInterpolated = DirectMetaProperty.ofImmutable(
        this, "isInterpolated", InflationRateCalculation.class, Boolean.TYPE);
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
        "monthLag",
        "isInterpolated",
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
        case -319031566:  // monthLag
          return monthLag;
        case 1334917581:  // isInterpolated
          return isInterpolated;
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
     * The meta-property for the {@code monthLag} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Integer> monthLag() {
      return monthLag;
    }

    /**
     * The meta-property for the {@code isInterpolated} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Boolean> isInterpolated() {
      return isInterpolated;
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
        case -319031566:  // monthLag
          return ((InflationRateCalculation) bean).getMonthLag();
        case 1334917581:  // isInterpolated
          return ((InflationRateCalculation) bean).isIsInterpolated();
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
    private int monthLag;
    private boolean isInterpolated;
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
      this.monthLag = beanToCopy.getMonthLag();
      this.isInterpolated = beanToCopy.isIsInterpolated();
      this.gearing = beanToCopy.gearing;
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case -319031566:  // monthLag
          return monthLag;
        case 1334917581:  // isInterpolated
          return isInterpolated;
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
        case -319031566:  // monthLag
          this.monthLag = (Integer) newValue;
          break;
        case 1334917581:  // isInterpolated
          this.isInterpolated = (Boolean) newValue;
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
          monthLag,
          isInterpolated,
          gearing);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code index} property in the builder.
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(PriceIndex index) {
      JodaBeanUtils.notNull(index, "index");
      this.index = index;
      return this;
    }

    /**
     * Sets the {@code monthLag} property in the builder.
     * @param monthLag  the new value
     * @return this, for chaining, not null
     */
    public Builder monthLag(int monthLag) {
      ArgChecker.notNegative(monthLag, "monthLag");
      this.monthLag = monthLag;
      return this;
    }

    /**
     * Sets the {@code isInterpolated} property in the builder.
     * @param isInterpolated  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder isInterpolated(boolean isInterpolated) {
      JodaBeanUtils.notNull(isInterpolated, "isInterpolated");
      this.isInterpolated = isInterpolated;
      return this;
    }

    /**
     * Sets the {@code gearing} property in the builder.
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
      buf.append("monthLag").append('=').append(JodaBeanUtils.toString(monthLag)).append(',').append(' ');
      buf.append("isInterpolated").append('=').append(JodaBeanUtils.toString(isInterpolated)).append(',').append(' ');
      buf.append("gearing").append('=').append(JodaBeanUtils.toString(gearing));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
