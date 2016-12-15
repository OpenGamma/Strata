/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutablePreBuild;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.market.param.TenorDateParameterMetadata;

/**
 * An ISDA compliant curve node whose instrument is a standard Fixed-Ibor interest rate swap.
 * <p>
 * This node contains the information on the fixed leg of the swap. 
 * It is assumed that the compounding not involved, the common business day adjustment is applied to start date, 
 * end date and accrual schedule, and the fixed rate is paid on the end date of each payment period. 
 * <p>
 * {@code observableId} is used to access the market data value of the swap par rate. 
 */
@BeanDefinition
public final class SwapIsdaCreditCurveNode
    implements IsdaCreditCurveNode, ImmutableBean, Serializable {

  /**
   * The label to use for the node, defaulted.
   * <p>
   * When building, this will default based on the tenor if not specified.
   */
  @PropertyDefinition(validate = "notEmpty", overrideGet = true)
  private final String label;
  /**
   * The identifier of the market data value that provides the rate.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final ObservableId observableId;
  /**
   * The tenor of the swap.
   * <p>
   * This is the period from the first accrual date to the last accrual date.
   */
  @PropertyDefinition(validate = "notNull")
  private final Tenor tenor;
  /**
   * The offset of the start date from the trade date.
   * <p>
   * The offset is applied to the trade date and is typically plus 2 business days.
   */
  @PropertyDefinition(validate = "notNull")
  private final DaysAdjustment spotDateOffset;
  /**
   * The business day adjustment to apply to the start date, end date and accrual schedule.
   * <p>
   * The date property is an unadjusted date and as such might be a weekend or holiday.
   * The adjustment specified here is used to convert a relevant date to a valid business day.
   */
  @PropertyDefinition(validate = "notNull")
  private final BusinessDayAdjustment businessDayAdjustment;
  /**
   * The day count convention applicable.
   * <p>
   * This is used to convert schedule period dates to a numerical value.
   */
  @PropertyDefinition(validate = "notNull")
  private final DayCount dayCount;
  /**
   * The periodic frequency of payments, optional with defaulting getter.
   * <p>
   * Regular payments will be made at the specified periodic frequency.
   * The compounding is not allowed in this node. Thus the frequency is the same as the accrual periodic frequency.
   */
  @PropertyDefinition(validate = "notNull")
  private final Frequency paymentFrequency;

  //-------------------------------------------------------------------------
  /**
   * Returns a curve node for a standard fixed-Ibor swap.
   * <p>
   * The label will be created from {@code tenor}. 
   * 
   * @param observableId  the observable ID
   * @param spotDateOffset  the spot date offset
   * @param businessDayAdjustment  the business day adjustment
   * @param tenor  the tenor
   * @param dayCount  the day count
   * @param paymentFrequency  the payment frequency
   * @return the curve node
   */
  public static SwapIsdaCreditCurveNode of(
      ObservableId observableId,
      DaysAdjustment spotDateOffset,
      BusinessDayAdjustment businessDayAdjustment,
      Tenor tenor,
      DayCount dayCount,
      Frequency paymentFrequency) {

    return SwapIsdaCreditCurveNode.builder()
        .businessDayAdjustment(businessDayAdjustment)
        .dayCount(dayCount)
        .observableId(observableId)
        .spotDateOffset(spotDateOffset)
        .tenor(tenor)
        .paymentFrequency(paymentFrequency)
        .build();
  }

  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.label == null && builder.tenor != null) {
      builder.label = builder.tenor.toString();
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public LocalDate date(LocalDate tradeDate, ReferenceData refData) {
    LocalDate startDate = spotDateOffset.adjust(tradeDate, refData);
    LocalDate endDate = startDate.plus(tenor.getPeriod());
    return businessDayAdjustment.adjust(endDate, refData);
  }

  @Override
  public TenorDateParameterMetadata metadata(LocalDate nodeDate) {
    return TenorDateParameterMetadata.of(nodeDate, tenor);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SwapIsdaCreditCurveNode}.
   * @return the meta-bean, not null
   */
  public static SwapIsdaCreditCurveNode.Meta meta() {
    return SwapIsdaCreditCurveNode.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(SwapIsdaCreditCurveNode.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static SwapIsdaCreditCurveNode.Builder builder() {
    return new SwapIsdaCreditCurveNode.Builder();
  }

  private SwapIsdaCreditCurveNode(
      String label,
      ObservableId observableId,
      Tenor tenor,
      DaysAdjustment spotDateOffset,
      BusinessDayAdjustment businessDayAdjustment,
      DayCount dayCount,
      Frequency paymentFrequency) {
    JodaBeanUtils.notEmpty(label, "label");
    JodaBeanUtils.notNull(observableId, "observableId");
    JodaBeanUtils.notNull(tenor, "tenor");
    JodaBeanUtils.notNull(spotDateOffset, "spotDateOffset");
    JodaBeanUtils.notNull(businessDayAdjustment, "businessDayAdjustment");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    JodaBeanUtils.notNull(paymentFrequency, "paymentFrequency");
    this.label = label;
    this.observableId = observableId;
    this.tenor = tenor;
    this.spotDateOffset = spotDateOffset;
    this.businessDayAdjustment = businessDayAdjustment;
    this.dayCount = dayCount;
    this.paymentFrequency = paymentFrequency;
  }

  @Override
  public SwapIsdaCreditCurveNode.Meta metaBean() {
    return SwapIsdaCreditCurveNode.Meta.INSTANCE;
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
   * Gets the label to use for the node, defaulted.
   * <p>
   * When building, this will default based on the tenor if not specified.
   * @return the value of the property, not empty
   */
  @Override
  public String getLabel() {
    return label;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the identifier of the market data value that provides the rate.
   * @return the value of the property, not null
   */
  @Override
  public ObservableId getObservableId() {
    return observableId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the tenor of the swap.
   * <p>
   * This is the period from the first accrual date to the last accrual date.
   * @return the value of the property, not null
   */
  public Tenor getTenor() {
    return tenor;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the offset of the start date from the trade date.
   * <p>
   * The offset is applied to the trade date and is typically plus 2 business days.
   * @return the value of the property, not null
   */
  public DaysAdjustment getSpotDateOffset() {
    return spotDateOffset;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the business day adjustment to apply to the start date, end date and accrual schedule.
   * <p>
   * The date property is an unadjusted date and as such might be a weekend or holiday.
   * The adjustment specified here is used to convert a relevant date to a valid business day.
   * @return the value of the property, not null
   */
  public BusinessDayAdjustment getBusinessDayAdjustment() {
    return businessDayAdjustment;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count convention applicable.
   * <p>
   * This is used to convert schedule period dates to a numerical value.
   * @return the value of the property, not null
   */
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the periodic frequency of payments, optional with defaulting getter.
   * <p>
   * Regular payments will be made at the specified periodic frequency.
   * The compounding is not allowed in this node. Thus the frequency is the same as the accrual periodic frequency.
   * @return the value of the property, not null
   */
  public Frequency getPaymentFrequency() {
    return paymentFrequency;
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
      SwapIsdaCreditCurveNode other = (SwapIsdaCreditCurveNode) obj;
      return JodaBeanUtils.equal(label, other.label) &&
          JodaBeanUtils.equal(observableId, other.observableId) &&
          JodaBeanUtils.equal(tenor, other.tenor) &&
          JodaBeanUtils.equal(spotDateOffset, other.spotDateOffset) &&
          JodaBeanUtils.equal(businessDayAdjustment, other.businessDayAdjustment) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(paymentFrequency, other.paymentFrequency);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(label);
    hash = hash * 31 + JodaBeanUtils.hashCode(observableId);
    hash = hash * 31 + JodaBeanUtils.hashCode(tenor);
    hash = hash * 31 + JodaBeanUtils.hashCode(spotDateOffset);
    hash = hash * 31 + JodaBeanUtils.hashCode(businessDayAdjustment);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentFrequency);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(256);
    buf.append("SwapIsdaCreditCurveNode{");
    buf.append("label").append('=').append(label).append(',').append(' ');
    buf.append("observableId").append('=').append(observableId).append(',').append(' ');
    buf.append("tenor").append('=').append(tenor).append(',').append(' ');
    buf.append("spotDateOffset").append('=').append(spotDateOffset).append(',').append(' ');
    buf.append("businessDayAdjustment").append('=').append(businessDayAdjustment).append(',').append(' ');
    buf.append("dayCount").append('=').append(dayCount).append(',').append(' ');
    buf.append("paymentFrequency").append('=').append(JodaBeanUtils.toString(paymentFrequency));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SwapIsdaCreditCurveNode}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code label} property.
     */
    private final MetaProperty<String> label = DirectMetaProperty.ofImmutable(
        this, "label", SwapIsdaCreditCurveNode.class, String.class);
    /**
     * The meta-property for the {@code observableId} property.
     */
    private final MetaProperty<ObservableId> observableId = DirectMetaProperty.ofImmutable(
        this, "observableId", SwapIsdaCreditCurveNode.class, ObservableId.class);
    /**
     * The meta-property for the {@code tenor} property.
     */
    private final MetaProperty<Tenor> tenor = DirectMetaProperty.ofImmutable(
        this, "tenor", SwapIsdaCreditCurveNode.class, Tenor.class);
    /**
     * The meta-property for the {@code spotDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> spotDateOffset = DirectMetaProperty.ofImmutable(
        this, "spotDateOffset", SwapIsdaCreditCurveNode.class, DaysAdjustment.class);
    /**
     * The meta-property for the {@code businessDayAdjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> businessDayAdjustment = DirectMetaProperty.ofImmutable(
        this, "businessDayAdjustment", SwapIsdaCreditCurveNode.class, BusinessDayAdjustment.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", SwapIsdaCreditCurveNode.class, DayCount.class);
    /**
     * The meta-property for the {@code paymentFrequency} property.
     */
    private final MetaProperty<Frequency> paymentFrequency = DirectMetaProperty.ofImmutable(
        this, "paymentFrequency", SwapIsdaCreditCurveNode.class, Frequency.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "label",
        "observableId",
        "tenor",
        "spotDateOffset",
        "businessDayAdjustment",
        "dayCount",
        "paymentFrequency");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 102727412:  // label
          return label;
        case -518800962:  // observableId
          return observableId;
        case 110246592:  // tenor
          return tenor;
        case 746995843:  // spotDateOffset
          return spotDateOffset;
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
        case 1905311443:  // dayCount
          return dayCount;
        case 863656438:  // paymentFrequency
          return paymentFrequency;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public SwapIsdaCreditCurveNode.Builder builder() {
      return new SwapIsdaCreditCurveNode.Builder();
    }

    @Override
    public Class<? extends SwapIsdaCreditCurveNode> beanType() {
      return SwapIsdaCreditCurveNode.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code label} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> label() {
      return label;
    }

    /**
     * The meta-property for the {@code observableId} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ObservableId> observableId() {
      return observableId;
    }

    /**
     * The meta-property for the {@code tenor} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Tenor> tenor() {
      return tenor;
    }

    /**
     * The meta-property for the {@code spotDateOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DaysAdjustment> spotDateOffset() {
      return spotDateOffset;
    }

    /**
     * The meta-property for the {@code businessDayAdjustment} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BusinessDayAdjustment> businessDayAdjustment() {
      return businessDayAdjustment;
    }

    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    /**
     * The meta-property for the {@code paymentFrequency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Frequency> paymentFrequency() {
      return paymentFrequency;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 102727412:  // label
          return ((SwapIsdaCreditCurveNode) bean).getLabel();
        case -518800962:  // observableId
          return ((SwapIsdaCreditCurveNode) bean).getObservableId();
        case 110246592:  // tenor
          return ((SwapIsdaCreditCurveNode) bean).getTenor();
        case 746995843:  // spotDateOffset
          return ((SwapIsdaCreditCurveNode) bean).getSpotDateOffset();
        case -1065319863:  // businessDayAdjustment
          return ((SwapIsdaCreditCurveNode) bean).getBusinessDayAdjustment();
        case 1905311443:  // dayCount
          return ((SwapIsdaCreditCurveNode) bean).getDayCount();
        case 863656438:  // paymentFrequency
          return ((SwapIsdaCreditCurveNode) bean).getPaymentFrequency();
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
   * The bean-builder for {@code SwapIsdaCreditCurveNode}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<SwapIsdaCreditCurveNode> {

    private String label;
    private ObservableId observableId;
    private Tenor tenor;
    private DaysAdjustment spotDateOffset;
    private BusinessDayAdjustment businessDayAdjustment;
    private DayCount dayCount;
    private Frequency paymentFrequency;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(SwapIsdaCreditCurveNode beanToCopy) {
      this.label = beanToCopy.getLabel();
      this.observableId = beanToCopy.getObservableId();
      this.tenor = beanToCopy.getTenor();
      this.spotDateOffset = beanToCopy.getSpotDateOffset();
      this.businessDayAdjustment = beanToCopy.getBusinessDayAdjustment();
      this.dayCount = beanToCopy.getDayCount();
      this.paymentFrequency = beanToCopy.getPaymentFrequency();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 102727412:  // label
          return label;
        case -518800962:  // observableId
          return observableId;
        case 110246592:  // tenor
          return tenor;
        case 746995843:  // spotDateOffset
          return spotDateOffset;
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
        case 1905311443:  // dayCount
          return dayCount;
        case 863656438:  // paymentFrequency
          return paymentFrequency;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 102727412:  // label
          this.label = (String) newValue;
          break;
        case -518800962:  // observableId
          this.observableId = (ObservableId) newValue;
          break;
        case 110246592:  // tenor
          this.tenor = (Tenor) newValue;
          break;
        case 746995843:  // spotDateOffset
          this.spotDateOffset = (DaysAdjustment) newValue;
          break;
        case -1065319863:  // businessDayAdjustment
          this.businessDayAdjustment = (BusinessDayAdjustment) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        case 863656438:  // paymentFrequency
          this.paymentFrequency = (Frequency) newValue;
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
    public SwapIsdaCreditCurveNode build() {
      preBuild(this);
      return new SwapIsdaCreditCurveNode(
          label,
          observableId,
          tenor,
          spotDateOffset,
          businessDayAdjustment,
          dayCount,
          paymentFrequency);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the label to use for the node, defaulted.
     * <p>
     * When building, this will default based on the tenor if not specified.
     * @param label  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder label(String label) {
      JodaBeanUtils.notEmpty(label, "label");
      this.label = label;
      return this;
    }

    /**
     * Sets the identifier of the market data value that provides the rate.
     * @param observableId  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder observableId(ObservableId observableId) {
      JodaBeanUtils.notNull(observableId, "observableId");
      this.observableId = observableId;
      return this;
    }

    /**
     * Sets the tenor of the swap.
     * <p>
     * This is the period from the first accrual date to the last accrual date.
     * @param tenor  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder tenor(Tenor tenor) {
      JodaBeanUtils.notNull(tenor, "tenor");
      this.tenor = tenor;
      return this;
    }

    /**
     * Sets the offset of the start date from the trade date.
     * <p>
     * The offset is applied to the trade date and is typically plus 2 business days.
     * @param spotDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder spotDateOffset(DaysAdjustment spotDateOffset) {
      JodaBeanUtils.notNull(spotDateOffset, "spotDateOffset");
      this.spotDateOffset = spotDateOffset;
      return this;
    }

    /**
     * Sets the business day adjustment to apply to the start date, end date and accrual schedule.
     * <p>
     * The date property is an unadjusted date and as such might be a weekend or holiday.
     * The adjustment specified here is used to convert a relevant date to a valid business day.
     * @param businessDayAdjustment  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder businessDayAdjustment(BusinessDayAdjustment businessDayAdjustment) {
      JodaBeanUtils.notNull(businessDayAdjustment, "businessDayAdjustment");
      this.businessDayAdjustment = businessDayAdjustment;
      return this;
    }

    /**
     * Sets the day count convention applicable.
     * <p>
     * This is used to convert schedule period dates to a numerical value.
     * @param dayCount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      JodaBeanUtils.notNull(dayCount, "dayCount");
      this.dayCount = dayCount;
      return this;
    }

    /**
     * Sets the periodic frequency of payments, optional with defaulting getter.
     * <p>
     * Regular payments will be made at the specified periodic frequency.
     * The compounding is not allowed in this node. Thus the frequency is the same as the accrual periodic frequency.
     * @param paymentFrequency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentFrequency(Frequency paymentFrequency) {
      JodaBeanUtils.notNull(paymentFrequency, "paymentFrequency");
      this.paymentFrequency = paymentFrequency;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(256);
      buf.append("SwapIsdaCreditCurveNode.Builder{");
      buf.append("label").append('=').append(JodaBeanUtils.toString(label)).append(',').append(' ');
      buf.append("observableId").append('=').append(JodaBeanUtils.toString(observableId)).append(',').append(' ');
      buf.append("tenor").append('=').append(JodaBeanUtils.toString(tenor)).append(',').append(' ');
      buf.append("spotDateOffset").append('=').append(JodaBeanUtils.toString(spotDateOffset)).append(',').append(' ');
      buf.append("businessDayAdjustment").append('=').append(JodaBeanUtils.toString(businessDayAdjustment)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("paymentFrequency").append('=').append(JodaBeanUtils.toString(paymentFrequency));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
