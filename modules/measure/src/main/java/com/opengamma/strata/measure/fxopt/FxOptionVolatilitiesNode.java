/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fxopt;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.NoSuchElementException;

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutablePreBuild;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.option.Strike;

/**
 * A node in the configuration specifying how to build FX option volatilities.
 * <p>
 * Each node is not necessarily associated with an instrument, 
 * but provides the necessary information to create {@code FxOptionVolatilities}. 
 */
@BeanDefinition
public final class FxOptionVolatilitiesNode
    implements ImmutableBean, Serializable {

  /**
   * The currency pair.
   * <p>
   * The quote must be based on this currency pair and direction.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurrencyPair currencyPair;
  /**
   * The label to use for the node.
   */
  @PropertyDefinition(validate = "notNull")
  private final String label;
  /**
   * The offset of the spot value date from the valuation date.
   * <p>
   * Typically this is the same as the standard convention of the spot date offset of the underlying FX forward.
   */
  @PropertyDefinition(validate = "notNull")
  private final DaysAdjustment spotDateOffset;
  /**
   * The business day adjustment to apply to the delivery date.
   * <p>
   * Typically this is the same as the standard convention of the business day adjustment 
   * applied to the delivery date of the underlying FX forward.
   */
  @PropertyDefinition(validate = "notNull")
  private final BusinessDayAdjustment businessDayAdjustment;
  /**
   * The offset of the expiry date from the delivery date.
   * <p>
   * By default the expiry date offset is the inverse of {@code spotDateOffset}. 
   * In this case {@code BusinessDayAdjustment} in {@code spotDateOffset} must be {@code NONE}.
   */
  @PropertyDefinition(validate = "notNull")
  private final DaysAdjustment expiryDateOffset;
  /**
   * The value type of the quote.
   */
  @PropertyDefinition(validate = "notNull")
  private final ValueType quoteValueType;
  /**
   * The quote ID.
   */
  @PropertyDefinition(validate = "notNull")
  private final QuoteId quoteId;
  /**
   * The tenor.
   * <p>
   * Typically the tenor is coherent to that of the underlying FX forward. 
   * Thus it spans the period between spot date to delivery date.
   */
  @PropertyDefinition(validate = "notNull")
  private final Tenor tenor;
  /**
   * The strike.
   */
  @PropertyDefinition(validate = "notNull")
  private final Strike strike;

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   * <p>
   * The label is created from {@code quoteId}.
   * 
   * @param currencyPair  the currency pair
   * @param spotDateOffset  the spot date offset
   * @param businessDayAdjustment  the business day adjustment
   * @param quoteValueType  the quote value type
   * @param quoteId  the quote ID
   * @param tenor  the tenor
   * @param strike  the strike
   * @return the instance
   */
  public static FxOptionVolatilitiesNode of(
      CurrencyPair currencyPair,
      DaysAdjustment spotDateOffset,
      BusinessDayAdjustment businessDayAdjustment,
      ValueType quoteValueType,
      QuoteId quoteId,
      Tenor tenor,
      Strike strike) {

    DaysAdjustment expiryDateOffset = expiryDateOffset(spotDateOffset);

    return new FxOptionVolatilitiesNode(
        currencyPair,
        quoteId.toString(),
        spotDateOffset,
        businessDayAdjustment,
        expiryDateOffset,
        quoteValueType,
        quoteId,
        tenor,
        strike);
  }

  private static DaysAdjustment expiryDateOffset(DaysAdjustment spotDateOffset) {
    ArgChecker.isTrue(spotDateOffset.getAdjustment().equals(BusinessDayAdjustment.NONE),
        "BusinessDayAdjustment in spotDateOffset must be NONE if expiryDateOffset is created from spotDateOffset");
    DaysAdjustment adj = spotDateOffset.toBuilder().days(-spotDateOffset.getDays()).build();
    return adj;
  }

  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.expiryDateOffset == null) {
      if (builder.spotDateOffset != null) {
        builder.expiryDateOffset = expiryDateOffset(builder.spotDateOffset);
      }
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the time to expiry for the valuation date time.
   * 
   * @param valuationDateTime  the valuation date time
   * @param dayCount  the day count
   * @param refData  the reference data
   * @return the time to expiry
   */
  public double timeToExpiry(ZonedDateTime valuationDateTime, DayCount dayCount, ReferenceData refData) {
    LocalDate valuationDate = valuationDateTime.toLocalDate();
    LocalDate spotDate = spotDateOffset.adjust(valuationDate, refData);
    LocalDate deliveryDate = businessDayAdjustment.adjust(spotDate.plus(tenor), refData);
    LocalDate expiryDate = expiryDateOffset.adjust(deliveryDate, refData);
    return dayCount.relativeYearFraction(valuationDate, expiryDate);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code FxOptionVolatilitiesNode}.
   * @return the meta-bean, not null
   */
  public static FxOptionVolatilitiesNode.Meta meta() {
    return FxOptionVolatilitiesNode.Meta.INSTANCE;
  }

  static {
    MetaBean.register(FxOptionVolatilitiesNode.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static FxOptionVolatilitiesNode.Builder builder() {
    return new FxOptionVolatilitiesNode.Builder();
  }

  private FxOptionVolatilitiesNode(
      CurrencyPair currencyPair,
      String label,
      DaysAdjustment spotDateOffset,
      BusinessDayAdjustment businessDayAdjustment,
      DaysAdjustment expiryDateOffset,
      ValueType quoteValueType,
      QuoteId quoteId,
      Tenor tenor,
      Strike strike) {
    JodaBeanUtils.notNull(currencyPair, "currencyPair");
    JodaBeanUtils.notNull(label, "label");
    JodaBeanUtils.notNull(spotDateOffset, "spotDateOffset");
    JodaBeanUtils.notNull(businessDayAdjustment, "businessDayAdjustment");
    JodaBeanUtils.notNull(expiryDateOffset, "expiryDateOffset");
    JodaBeanUtils.notNull(quoteValueType, "quoteValueType");
    JodaBeanUtils.notNull(quoteId, "quoteId");
    JodaBeanUtils.notNull(tenor, "tenor");
    JodaBeanUtils.notNull(strike, "strike");
    this.currencyPair = currencyPair;
    this.label = label;
    this.spotDateOffset = spotDateOffset;
    this.businessDayAdjustment = businessDayAdjustment;
    this.expiryDateOffset = expiryDateOffset;
    this.quoteValueType = quoteValueType;
    this.quoteId = quoteId;
    this.tenor = tenor;
    this.strike = strike;
  }

  @Override
  public FxOptionVolatilitiesNode.Meta metaBean() {
    return FxOptionVolatilitiesNode.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency pair.
   * <p>
   * The quote must be based on this currency pair and direction.
   * @return the value of the property, not null
   */
  public CurrencyPair getCurrencyPair() {
    return currencyPair;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the label to use for the node.
   * @return the value of the property, not null
   */
  public String getLabel() {
    return label;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the offset of the spot value date from the valuation date.
   * <p>
   * Typically this is the same as the standard convention of the spot date offset of the underlying FX forward.
   * @return the value of the property, not null
   */
  public DaysAdjustment getSpotDateOffset() {
    return spotDateOffset;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the business day adjustment to apply to the delivery date.
   * <p>
   * Typically this is the same as the standard convention of the business day adjustment
   * applied to the delivery date of the underlying FX forward.
   * @return the value of the property, not null
   */
  public BusinessDayAdjustment getBusinessDayAdjustment() {
    return businessDayAdjustment;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the offset of the expiry date from the delivery date.
   * <p>
   * By default the expiry date offset is the inverse of {@code spotDateOffset}.
   * In this case {@code BusinessDayAdjustment} in {@code spotDateOffset} must be {@code NONE}.
   * @return the value of the property, not null
   */
  public DaysAdjustment getExpiryDateOffset() {
    return expiryDateOffset;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the value type of the quote.
   * @return the value of the property, not null
   */
  public ValueType getQuoteValueType() {
    return quoteValueType;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the quote ID.
   * @return the value of the property, not null
   */
  public QuoteId getQuoteId() {
    return quoteId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the tenor.
   * <p>
   * Typically the tenor is coherent to that of the underlying FX forward.
   * Thus it spans the period between spot date to delivery date.
   * @return the value of the property, not null
   */
  public Tenor getTenor() {
    return tenor;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the strike.
   * @return the value of the property, not null
   */
  public Strike getStrike() {
    return strike;
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
      FxOptionVolatilitiesNode other = (FxOptionVolatilitiesNode) obj;
      return JodaBeanUtils.equal(currencyPair, other.currencyPair) &&
          JodaBeanUtils.equal(label, other.label) &&
          JodaBeanUtils.equal(spotDateOffset, other.spotDateOffset) &&
          JodaBeanUtils.equal(businessDayAdjustment, other.businessDayAdjustment) &&
          JodaBeanUtils.equal(expiryDateOffset, other.expiryDateOffset) &&
          JodaBeanUtils.equal(quoteValueType, other.quoteValueType) &&
          JodaBeanUtils.equal(quoteId, other.quoteId) &&
          JodaBeanUtils.equal(tenor, other.tenor) &&
          JodaBeanUtils.equal(strike, other.strike);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(currencyPair);
    hash = hash * 31 + JodaBeanUtils.hashCode(label);
    hash = hash * 31 + JodaBeanUtils.hashCode(spotDateOffset);
    hash = hash * 31 + JodaBeanUtils.hashCode(businessDayAdjustment);
    hash = hash * 31 + JodaBeanUtils.hashCode(expiryDateOffset);
    hash = hash * 31 + JodaBeanUtils.hashCode(quoteValueType);
    hash = hash * 31 + JodaBeanUtils.hashCode(quoteId);
    hash = hash * 31 + JodaBeanUtils.hashCode(tenor);
    hash = hash * 31 + JodaBeanUtils.hashCode(strike);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(320);
    buf.append("FxOptionVolatilitiesNode{");
    buf.append("currencyPair").append('=').append(currencyPair).append(',').append(' ');
    buf.append("label").append('=').append(label).append(',').append(' ');
    buf.append("spotDateOffset").append('=').append(spotDateOffset).append(',').append(' ');
    buf.append("businessDayAdjustment").append('=').append(businessDayAdjustment).append(',').append(' ');
    buf.append("expiryDateOffset").append('=').append(expiryDateOffset).append(',').append(' ');
    buf.append("quoteValueType").append('=').append(quoteValueType).append(',').append(' ');
    buf.append("quoteId").append('=').append(quoteId).append(',').append(' ');
    buf.append("tenor").append('=').append(tenor).append(',').append(' ');
    buf.append("strike").append('=').append(JodaBeanUtils.toString(strike));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FxOptionVolatilitiesNode}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code currencyPair} property.
     */
    private final MetaProperty<CurrencyPair> currencyPair = DirectMetaProperty.ofImmutable(
        this, "currencyPair", FxOptionVolatilitiesNode.class, CurrencyPair.class);
    /**
     * The meta-property for the {@code label} property.
     */
    private final MetaProperty<String> label = DirectMetaProperty.ofImmutable(
        this, "label", FxOptionVolatilitiesNode.class, String.class);
    /**
     * The meta-property for the {@code spotDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> spotDateOffset = DirectMetaProperty.ofImmutable(
        this, "spotDateOffset", FxOptionVolatilitiesNode.class, DaysAdjustment.class);
    /**
     * The meta-property for the {@code businessDayAdjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> businessDayAdjustment = DirectMetaProperty.ofImmutable(
        this, "businessDayAdjustment", FxOptionVolatilitiesNode.class, BusinessDayAdjustment.class);
    /**
     * The meta-property for the {@code expiryDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> expiryDateOffset = DirectMetaProperty.ofImmutable(
        this, "expiryDateOffset", FxOptionVolatilitiesNode.class, DaysAdjustment.class);
    /**
     * The meta-property for the {@code quoteValueType} property.
     */
    private final MetaProperty<ValueType> quoteValueType = DirectMetaProperty.ofImmutable(
        this, "quoteValueType", FxOptionVolatilitiesNode.class, ValueType.class);
    /**
     * The meta-property for the {@code quoteId} property.
     */
    private final MetaProperty<QuoteId> quoteId = DirectMetaProperty.ofImmutable(
        this, "quoteId", FxOptionVolatilitiesNode.class, QuoteId.class);
    /**
     * The meta-property for the {@code tenor} property.
     */
    private final MetaProperty<Tenor> tenor = DirectMetaProperty.ofImmutable(
        this, "tenor", FxOptionVolatilitiesNode.class, Tenor.class);
    /**
     * The meta-property for the {@code strike} property.
     */
    private final MetaProperty<Strike> strike = DirectMetaProperty.ofImmutable(
        this, "strike", FxOptionVolatilitiesNode.class, Strike.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "currencyPair",
        "label",
        "spotDateOffset",
        "businessDayAdjustment",
        "expiryDateOffset",
        "quoteValueType",
        "quoteId",
        "tenor",
        "strike");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1005147787:  // currencyPair
          return currencyPair;
        case 102727412:  // label
          return label;
        case 746995843:  // spotDateOffset
          return spotDateOffset;
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
        case 508197748:  // expiryDateOffset
          return expiryDateOffset;
        case 758636847:  // quoteValueType
          return quoteValueType;
        case 664377527:  // quoteId
          return quoteId;
        case 110246592:  // tenor
          return tenor;
        case -891985998:  // strike
          return strike;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public FxOptionVolatilitiesNode.Builder builder() {
      return new FxOptionVolatilitiesNode.Builder();
    }

    @Override
    public Class<? extends FxOptionVolatilitiesNode> beanType() {
      return FxOptionVolatilitiesNode.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code currencyPair} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurrencyPair> currencyPair() {
      return currencyPair;
    }

    /**
     * The meta-property for the {@code label} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> label() {
      return label;
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
     * The meta-property for the {@code expiryDateOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DaysAdjustment> expiryDateOffset() {
      return expiryDateOffset;
    }

    /**
     * The meta-property for the {@code quoteValueType} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ValueType> quoteValueType() {
      return quoteValueType;
    }

    /**
     * The meta-property for the {@code quoteId} property.
     * @return the meta-property, not null
     */
    public MetaProperty<QuoteId> quoteId() {
      return quoteId;
    }

    /**
     * The meta-property for the {@code tenor} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Tenor> tenor() {
      return tenor;
    }

    /**
     * The meta-property for the {@code strike} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Strike> strike() {
      return strike;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1005147787:  // currencyPair
          return ((FxOptionVolatilitiesNode) bean).getCurrencyPair();
        case 102727412:  // label
          return ((FxOptionVolatilitiesNode) bean).getLabel();
        case 746995843:  // spotDateOffset
          return ((FxOptionVolatilitiesNode) bean).getSpotDateOffset();
        case -1065319863:  // businessDayAdjustment
          return ((FxOptionVolatilitiesNode) bean).getBusinessDayAdjustment();
        case 508197748:  // expiryDateOffset
          return ((FxOptionVolatilitiesNode) bean).getExpiryDateOffset();
        case 758636847:  // quoteValueType
          return ((FxOptionVolatilitiesNode) bean).getQuoteValueType();
        case 664377527:  // quoteId
          return ((FxOptionVolatilitiesNode) bean).getQuoteId();
        case 110246592:  // tenor
          return ((FxOptionVolatilitiesNode) bean).getTenor();
        case -891985998:  // strike
          return ((FxOptionVolatilitiesNode) bean).getStrike();
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
   * The bean-builder for {@code FxOptionVolatilitiesNode}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<FxOptionVolatilitiesNode> {

    private CurrencyPair currencyPair;
    private String label;
    private DaysAdjustment spotDateOffset;
    private BusinessDayAdjustment businessDayAdjustment;
    private DaysAdjustment expiryDateOffset;
    private ValueType quoteValueType;
    private QuoteId quoteId;
    private Tenor tenor;
    private Strike strike;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(FxOptionVolatilitiesNode beanToCopy) {
      this.currencyPair = beanToCopy.getCurrencyPair();
      this.label = beanToCopy.getLabel();
      this.spotDateOffset = beanToCopy.getSpotDateOffset();
      this.businessDayAdjustment = beanToCopy.getBusinessDayAdjustment();
      this.expiryDateOffset = beanToCopy.getExpiryDateOffset();
      this.quoteValueType = beanToCopy.getQuoteValueType();
      this.quoteId = beanToCopy.getQuoteId();
      this.tenor = beanToCopy.getTenor();
      this.strike = beanToCopy.getStrike();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1005147787:  // currencyPair
          return currencyPair;
        case 102727412:  // label
          return label;
        case 746995843:  // spotDateOffset
          return spotDateOffset;
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
        case 508197748:  // expiryDateOffset
          return expiryDateOffset;
        case 758636847:  // quoteValueType
          return quoteValueType;
        case 664377527:  // quoteId
          return quoteId;
        case 110246592:  // tenor
          return tenor;
        case -891985998:  // strike
          return strike;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 1005147787:  // currencyPair
          this.currencyPair = (CurrencyPair) newValue;
          break;
        case 102727412:  // label
          this.label = (String) newValue;
          break;
        case 746995843:  // spotDateOffset
          this.spotDateOffset = (DaysAdjustment) newValue;
          break;
        case -1065319863:  // businessDayAdjustment
          this.businessDayAdjustment = (BusinessDayAdjustment) newValue;
          break;
        case 508197748:  // expiryDateOffset
          this.expiryDateOffset = (DaysAdjustment) newValue;
          break;
        case 758636847:  // quoteValueType
          this.quoteValueType = (ValueType) newValue;
          break;
        case 664377527:  // quoteId
          this.quoteId = (QuoteId) newValue;
          break;
        case 110246592:  // tenor
          this.tenor = (Tenor) newValue;
          break;
        case -891985998:  // strike
          this.strike = (Strike) newValue;
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
    public FxOptionVolatilitiesNode build() {
      preBuild(this);
      return new FxOptionVolatilitiesNode(
          currencyPair,
          label,
          spotDateOffset,
          businessDayAdjustment,
          expiryDateOffset,
          quoteValueType,
          quoteId,
          tenor,
          strike);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the currency pair.
     * <p>
     * The quote must be based on this currency pair and direction.
     * @param currencyPair  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder currencyPair(CurrencyPair currencyPair) {
      JodaBeanUtils.notNull(currencyPair, "currencyPair");
      this.currencyPair = currencyPair;
      return this;
    }

    /**
     * Sets the label to use for the node.
     * @param label  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder label(String label) {
      JodaBeanUtils.notNull(label, "label");
      this.label = label;
      return this;
    }

    /**
     * Sets the offset of the spot value date from the valuation date.
     * <p>
     * Typically this is the same as the standard convention of the spot date offset of the underlying FX forward.
     * @param spotDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder spotDateOffset(DaysAdjustment spotDateOffset) {
      JodaBeanUtils.notNull(spotDateOffset, "spotDateOffset");
      this.spotDateOffset = spotDateOffset;
      return this;
    }

    /**
     * Sets the business day adjustment to apply to the delivery date.
     * <p>
     * Typically this is the same as the standard convention of the business day adjustment
     * applied to the delivery date of the underlying FX forward.
     * @param businessDayAdjustment  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder businessDayAdjustment(BusinessDayAdjustment businessDayAdjustment) {
      JodaBeanUtils.notNull(businessDayAdjustment, "businessDayAdjustment");
      this.businessDayAdjustment = businessDayAdjustment;
      return this;
    }

    /**
     * Sets the offset of the expiry date from the delivery date.
     * <p>
     * By default the expiry date offset is the inverse of {@code spotDateOffset}.
     * In this case {@code BusinessDayAdjustment} in {@code spotDateOffset} must be {@code NONE}.
     * @param expiryDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder expiryDateOffset(DaysAdjustment expiryDateOffset) {
      JodaBeanUtils.notNull(expiryDateOffset, "expiryDateOffset");
      this.expiryDateOffset = expiryDateOffset;
      return this;
    }

    /**
     * Sets the value type of the quote.
     * @param quoteValueType  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder quoteValueType(ValueType quoteValueType) {
      JodaBeanUtils.notNull(quoteValueType, "quoteValueType");
      this.quoteValueType = quoteValueType;
      return this;
    }

    /**
     * Sets the quote ID.
     * @param quoteId  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder quoteId(QuoteId quoteId) {
      JodaBeanUtils.notNull(quoteId, "quoteId");
      this.quoteId = quoteId;
      return this;
    }

    /**
     * Sets the tenor.
     * <p>
     * Typically the tenor is coherent to that of the underlying FX forward.
     * Thus it spans the period between spot date to delivery date.
     * @param tenor  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder tenor(Tenor tenor) {
      JodaBeanUtils.notNull(tenor, "tenor");
      this.tenor = tenor;
      return this;
    }

    /**
     * Sets the strike.
     * @param strike  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder strike(Strike strike) {
      JodaBeanUtils.notNull(strike, "strike");
      this.strike = strike;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(320);
      buf.append("FxOptionVolatilitiesNode.Builder{");
      buf.append("currencyPair").append('=').append(JodaBeanUtils.toString(currencyPair)).append(',').append(' ');
      buf.append("label").append('=').append(JodaBeanUtils.toString(label)).append(',').append(' ');
      buf.append("spotDateOffset").append('=').append(JodaBeanUtils.toString(spotDateOffset)).append(',').append(' ');
      buf.append("businessDayAdjustment").append('=').append(JodaBeanUtils.toString(businessDayAdjustment)).append(',').append(' ');
      buf.append("expiryDateOffset").append('=').append(JodaBeanUtils.toString(expiryDateOffset)).append(',').append(' ');
      buf.append("quoteValueType").append('=').append(JodaBeanUtils.toString(quoteValueType)).append(',').append(' ');
      buf.append("quoteId").append('=').append(JodaBeanUtils.toString(quoteId)).append(',').append(' ');
      buf.append("tenor").append('=').append(JodaBeanUtils.toString(tenor)).append(',').append(' ');
      buf.append("strike").append('=').append(JodaBeanUtils.toString(strike));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
