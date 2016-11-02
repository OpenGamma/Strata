/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static java.time.temporal.ChronoUnit.MONTHS;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.YearMonth;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.PropertyDefinition;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ShiftType;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.UnitParameterSensitivity;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.joda.beans.Bean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.BeanBuilder;

/**
 * Curve specifically designed for inflation with features for seasonality and initial point.
 * <p>
 * The curve details with the initial point and the total seasonal adjustment for each month can be directly provided.
 * Alternatively, the curve without the initial point and the seasonality as a month-on-month can be provided and the 
 * final curve computed from there.
 */
@BeanDefinition(builderScope = "private")
public class SeasonalNodalCurve
    implements NodalCurve, ImmutableBean, Serializable {

  /**
   * The underlying curve, before the adjustment. 
   * With fixed initial value. Not a parameter.
   */
  @PropertyDefinition(validate = "notNull")
  private final NodalCurve underlying;
  /**
   * Describes the seasonal adjustments.
   * The array has a dimension of 12, one element for each month.
   * The adjustments are described as a perturbation to the existing values.
   * No adjustment to the fixing value.
   */
  @PropertyDefinition(validate = "notNull")
  private final DoubleArray seasonality;
  /**
   * The shift type applied to the unadjusted value and the adjustment.
   * (value, seasonality) -> adjustmentType.applyShift(value, seasonality).
   */
  @PropertyDefinition(validate = "notNull")
  private final ShiftType adjustmentType;
  
  private final double xFixing;  // cached, not a property
  private final double yFixing;  // cached, not a property
  
  /**
   * Create an instance of the curve.
   * <p>
   * The seasonal adjustment is the total adjustment for each month, starting with January. 
   * See the other 'of' method for month-on-month adjustments and the adjustment starting point.
   * No extra point is added to represent the last fixing. See the other 'of' method to deal with this.
   * 
   * @param curve  the curve with initial fixing
   * @param seasonality  the total seasonal adjustment for each month, with the first element the January adjustment
   * @param adjustmentType  the adjustment type
   * @return the seasonal curve instance
   */
  public static SeasonalNodalCurve of(
      NodalCurve curve, 
      DoubleArray seasonality, 
      ShiftType adjustmentType) {
    
    return new SeasonalNodalCurve(curve, seasonality, adjustmentType);
  }
  
  /**
   * Create an instance from a curve without initial fixing point and month-on-month seasonal adjustment.
   * <p>
   * The total adjustment is computed by accumulation of the monthly adjustment, starting with no adjustment for the 
   * last fixing month.
   * 
   * @param curveWithoutFixing  the curve without the fixing
   * @param valuationDate  the valuation date of the curve
   * @param lastMonth  the last month for which the fixing is known
   * @param lastFixingValue  the value of the last fixing
   * @param seasonalityDefinition  the seasonality definition, which is made of month-on-month adjustment 
   * and the adjustment type
   * @return the seasonal curve instance
   */
  public static SeasonalNodalCurve of(
      NodalCurve curveWithoutFixing, 
      LocalDate valuationDate,
      YearMonth lastMonth,
      double lastFixingValue,
      SeasonalityDefinition seasonalityDefinition) {

    ArgChecker.isTrue(lastMonth.isBefore(YearMonth.from(valuationDate)), 
        "Last fixing month must be before valuation date");
    double nbMonth = YearMonth.from(valuationDate).until(lastMonth, MONTHS);
    DoubleArray x = curveWithoutFixing.getXValues();
    ArgChecker.isTrue(nbMonth < x.get(0), "The first estimation month should be after the last known index fixing");
    NodalCurve extendedCurve = curveWithoutFixing.withNode(nbMonth, lastFixingValue, ParameterMetadata.empty());
    double[] seasonalityCompoundedArray = new double[12];
    int lastMonthIndex = lastMonth.getMonth().getValue() - 1;
    seasonalityCompoundedArray[(int) ((nbMonth + 12 + 1) % 12)] = 
        seasonalityDefinition.getSeasonalityMonthOnMonth().get(lastMonthIndex % 12);
    for (int i = 1; i < 12; i++) {
      int j = (int) ((nbMonth + 12 + 1 + i) % 12);
      seasonalityCompoundedArray[j] = seasonalityDefinition.getAdjustmentType()
          .applyShift(seasonalityCompoundedArray[(j - 1 + 12) % 12], 
              seasonalityDefinition.getSeasonalityMonthOnMonth().get((lastMonthIndex + i) % 12));
    }
    return new SeasonalNodalCurve(
        extendedCurve, 
        DoubleArray.ofUnsafe(seasonalityCompoundedArray), 
        seasonalityDefinition.getAdjustmentType());
  }

  //-------------------------------------------------------------------------
  // restricted constructor
  @ImmutableConstructor
  private SeasonalNodalCurve(
      NodalCurve curve, 
      DoubleArray seasonality, 
      ShiftType adjustmentType) {
    this.underlying = curve;
    this.seasonality = seasonality;
    this.xFixing = curve.getXValues().get(0);
    this.yFixing = curve.getYValues().get(0);
    int i = seasonalityIndex(xFixing);
    ArgChecker.isTrue(adjustmentType.applyShift(yFixing, seasonality.get(i) ) - yFixing < 1.0E-10, 
        "Fixing value should be unadjusted");
    this.adjustmentType = adjustmentType;
  }

  @Override
  public CurveMetadata getMetadata() {
    return underlying.getMetadata();
  }

  @Override
  public double yValue(double x) {
    int i = seasonalityIndex(x);
    double adjustment = seasonality.get(i);
    return adjustmentType.applyShift(underlying.yValue(x), adjustment);
  }
  
  // The index on the seasonality vector associated to a time (nb months).
  private int seasonalityIndex(double x) {
    long xLong = Math.round(x);
    return (int) (((xLong % 12) + 12) % 12); // Shift by 12 has java compute the remainder of negative numbers as negative
  }

  @Override
  public int getParameterCount() {
    return underlying.getParameterCount() - 1;
  }

  @Override
  public double getParameter(int parameterIndex) {
    return underlying.getParameter(parameterIndex + 1);
  }

  @Override
  public ParameterMetadata getParameterMetadata(int parameterIndex) {
    return underlying.getParameterMetadata(parameterIndex + 1);
  }

  @Override
  public DoubleArray getXValues() {
    return underlying.getXValues().subArray(1);
  }

  @Override
  public DoubleArray getYValues() {
    return underlying.getYValues().subArray(1);
  }

  @Override
  public UnitParameterSensitivity yValueParameterSensitivity(double x) {
    int i = seasonalityIndex(x);
    double adjustment = seasonality.get(i);
    double derivativeFactor = 0.0;
    if (adjustmentType.equals(ShiftType.ABSOLUTE)) {
      derivativeFactor = 1.0;
    } else if (adjustmentType.equals(ShiftType.SCALED)) {
      derivativeFactor = adjustment;
    } else {
      throw new IllegalArgumentException("ShiftType " + adjustmentType + " is not supported for sensitivities");
    }
    UnitParameterSensitivity u = underlying.yValueParameterSensitivity(x);
    UnitParameterSensitivity u2 =
        UnitParameterSensitivity.of(
            u.getMarketDataName(),
            u.getParameterMetadata().subList(1, u.getParameterMetadata().size()),
            u.getSensitivity().subArray(1));
    return u2.multipliedBy(derivativeFactor);
  }

  @Override
  public double firstDerivative(double x) {
    throw new UnsupportedOperationException("Value implemented only at discrete (monthly) values; no derivative available");
  }

  @Override
  public SeasonalNodalCurve withMetadata(CurveMetadata metadata) {
    return new SeasonalNodalCurve(underlying.withMetadata(metadata), seasonality, adjustmentType);
  }

  @Override
  public SeasonalNodalCurve withYValues(DoubleArray values) {
    DoubleArray yExtended = DoubleArray.of(yFixing).concat(values);
    return new SeasonalNodalCurve(underlying.withYValues(yExtended), seasonality, adjustmentType);
  }

  @Override
  public SeasonalNodalCurve withValues(DoubleArray xValues, DoubleArray yValues) {
    DoubleArray xExtended = DoubleArray.of(xFixing).concat(xValues);
    DoubleArray yExtended = DoubleArray.of(yFixing).concat(yValues);
    return new SeasonalNodalCurve(underlying.withValues(xExtended, yExtended), seasonality, adjustmentType);
  }

  @Override
  public SeasonalNodalCurve withParameter(int parameterIndex, double newValue) {
    return new SeasonalNodalCurve(underlying.withParameter(parameterIndex + 1, newValue), seasonality, adjustmentType);
  }

  @Override
  public SeasonalNodalCurve withNode(double x, double y, ParameterMetadata paramMetadata) {
    ArgChecker.isTrue(xFixing < x, "node can be added only after the fixing anchor");
    return new SeasonalNodalCurve(underlying.withNode(x, y, paramMetadata), seasonality, adjustmentType);
  }
  
  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SeasonalNodalCurve}.
   * @return the meta-bean, not null
   */
  public static SeasonalNodalCurve.Meta meta() {
    return SeasonalNodalCurve.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(SeasonalNodalCurve.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public SeasonalNodalCurve.Meta metaBean() {
    return SeasonalNodalCurve.Meta.INSTANCE;
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
   * Gets the underlying curve, before the adjustment.
   * With fixed initial value. Not a parameter.
   * @return the value of the property, not null
   */
  public NodalCurve getUnderlying() {
    return underlying;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets describes the seasonal adjustments.
   * The array has a dimension of 12, one element for each month.
   * The adjustments are described as a perturbation to the existing values.
   * No adjustment to the fixing value.
   * @return the value of the property, not null
   */
  public DoubleArray getSeasonality() {
    return seasonality;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the shift type applied to the unadjusted value and the adjustment.
   * (value, seasonality) -> adjustmentType.applyShift(value, seasonality).
   * @return the value of the property, not null
   */
  public ShiftType getAdjustmentType() {
    return adjustmentType;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      SeasonalNodalCurve other = (SeasonalNodalCurve) obj;
      return JodaBeanUtils.equal(underlying, other.underlying) &&
          JodaBeanUtils.equal(seasonality, other.seasonality) &&
          JodaBeanUtils.equal(adjustmentType, other.adjustmentType);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(underlying);
    hash = hash * 31 + JodaBeanUtils.hashCode(seasonality);
    hash = hash * 31 + JodaBeanUtils.hashCode(adjustmentType);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("SeasonalNodalCurve{");
    int len = buf.length();
    toString(buf);
    if (buf.length() > len) {
      buf.setLength(buf.length() - 2);
    }
    buf.append('}');
    return buf.toString();
  }

  protected void toString(StringBuilder buf) {
    buf.append("underlying").append('=').append(JodaBeanUtils.toString(underlying)).append(',').append(' ');
    buf.append("seasonality").append('=').append(JodaBeanUtils.toString(seasonality)).append(',').append(' ');
    buf.append("adjustmentType").append('=').append(JodaBeanUtils.toString(adjustmentType)).append(',').append(' ');
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SeasonalNodalCurve}.
   */
  public static class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code underlying} property.
     */
    private final MetaProperty<NodalCurve> underlying = DirectMetaProperty.ofImmutable(
        this, "underlying", SeasonalNodalCurve.class, NodalCurve.class);
    /**
     * The meta-property for the {@code seasonality} property.
     */
    private final MetaProperty<DoubleArray> seasonality = DirectMetaProperty.ofImmutable(
        this, "seasonality", SeasonalNodalCurve.class, DoubleArray.class);
    /**
     * The meta-property for the {@code adjustmentType} property.
     */
    private final MetaProperty<ShiftType> adjustmentType = DirectMetaProperty.ofImmutable(
        this, "adjustmentType", SeasonalNodalCurve.class, ShiftType.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "underlying",
        "seasonality",
        "adjustmentType");

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1770633379:  // underlying
          return underlying;
        case -857898080:  // seasonality
          return seasonality;
        case -1002343865:  // adjustmentType
          return adjustmentType;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends SeasonalNodalCurve> builder() {
      return new SeasonalNodalCurve.Builder();
    }

    @Override
    public Class<? extends SeasonalNodalCurve> beanType() {
      return SeasonalNodalCurve.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code underlying} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<NodalCurve> underlying() {
      return underlying;
    }

    /**
     * The meta-property for the {@code seasonality} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<DoubleArray> seasonality() {
      return seasonality;
    }

    /**
     * The meta-property for the {@code adjustmentType} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<ShiftType> adjustmentType() {
      return adjustmentType;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1770633379:  // underlying
          return ((SeasonalNodalCurve) bean).getUnderlying();
        case -857898080:  // seasonality
          return ((SeasonalNodalCurve) bean).getSeasonality();
        case -1002343865:  // adjustmentType
          return ((SeasonalNodalCurve) bean).getAdjustmentType();
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
   * The bean-builder for {@code SeasonalNodalCurve}.
   */
  private static class Builder extends DirectFieldsBeanBuilder<SeasonalNodalCurve> {

    private NodalCurve underlying;
    private DoubleArray seasonality;
    private ShiftType adjustmentType;

    /**
     * Restricted constructor.
     */
    protected Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1770633379:  // underlying
          return underlying;
        case -857898080:  // seasonality
          return seasonality;
        case -1002343865:  // adjustmentType
          return adjustmentType;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1770633379:  // underlying
          this.underlying = (NodalCurve) newValue;
          break;
        case -857898080:  // seasonality
          this.seasonality = (DoubleArray) newValue;
          break;
        case -1002343865:  // adjustmentType
          this.adjustmentType = (ShiftType) newValue;
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
    public SeasonalNodalCurve build() {
      return new SeasonalNodalCurve(
          underlying,
          seasonality,
          adjustmentType);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("SeasonalNodalCurve.Builder{");
      int len = buf.length();
      toString(buf);
      if (buf.length() > len) {
        buf.setLength(buf.length() - 2);
      }
      buf.append('}');
      return buf.toString();
    }

    protected void toString(StringBuilder buf) {
      buf.append("underlying").append('=').append(JodaBeanUtils.toString(underlying)).append(',').append(' ');
      buf.append("seasonality").append('=').append(JodaBeanUtils.toString(seasonality)).append(',').append(' ');
      buf.append("adjustmentType").append('=').append(JodaBeanUtils.toString(adjustmentType)).append(',').append(' ');
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
