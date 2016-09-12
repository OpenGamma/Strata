/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.node;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.CurveNodeDate;
import com.opengamma.strata.market.curve.CurveNodeDateOrder;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.DatedParameterMetadata;
import com.opengamma.strata.market.param.YearMonthDateParameterMetadata;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.index.IborFutureTrade;
import com.opengamma.strata.product.index.ResolvedIborFutureTrade;
import com.opengamma.strata.product.index.type.IborFutureTemplate;

/**
 * A curve node whose instrument is an Ibor Future.
 * <p>
 * The trade produced by the node will be a long for a positive quantity and a short for a negative quantity.
 * This convention is line with other nodes where a positive quantity is similar to long a bond or deposit.
 */
@BeanDefinition
public final class IborFutureCurveNode
    implements CurveNode, ImmutableBean, Serializable {

  /**
   * The template for the Ibor Futures associated with this node.
   */
  @PropertyDefinition(validate = "notNull")
  private final IborFutureTemplate template;
  /**
   * The identifier of the market data value which provides the price.
   */
  @PropertyDefinition(validate = "notNull")
  private final QuoteId rateId;
  /**
   * The additional spread added to the price.
   * This amount is directly added to the price, where 0.993 represents a 0.7% rate.
   */
  @PropertyDefinition
  private final double additionalSpread;
  /**
   * The label to use for the node, may be empty.
   * <p>
   * If empty, a default label will be created when the metadata is built.
   * The default label depends on the valuation date, so cannot be created in the node.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final String label;
  /**
   * The method by which the date of the node is calculated, defaulted to 'End'.
   */
  @PropertyDefinition
  private final CurveNodeDate date;
  /**
   * The date order rules, used to ensure that the dates in the curve are in order.
   * If not specified, this will default to {@link CurveNodeDateOrder#DEFAULT}.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final CurveNodeDateOrder dateOrder;

  //-------------------------------------------------------------------------
  /**
   * Obtains a curve node for an Ibor Future using the specified template and rate key.
   *
   * @param template  the template used for building the instrument for the node
   * @param rateId  the identifier of the market rate for the security
   * @return a node whose instrument is built from the template using a market rate
   */
  public static IborFutureCurveNode of(IborFutureTemplate template, QuoteId rateId) {
    return of(template, rateId, 0d);
  }

  /**
   * Obtains a curve node for an Ibor Future using the specified template, rate key and spread.
   *
   * @param template  the template defining the node instrument
   * @param rateId  the identifier of the market rate for the security
   * @param additionalSpread  the additional spread amount added to the rate
   * @return a node whose instrument is built from the template using a market rate
   */
  public static IborFutureCurveNode of(
      IborFutureTemplate template,
      QuoteId rateId,
      double additionalSpread) {

    return of(template, rateId, additionalSpread, "");
  }

  /**
   * Obtains a curve node for an Ibor Future using the specified template, rate key, spread and label.
   *
   * @param template  the template defining the node instrument
   * @param rateId  the identifier of the market rate for the security
   * @param additionalSpread  the additional spread amount added to the rate
   * @param label  the label to use for the node, if empty an appropriate default label will be generated
   * @return a node whose instrument is built from the template using a market rate
   */
  public static IborFutureCurveNode of(
      IborFutureTemplate template,
      QuoteId rateId,
      double additionalSpread,
      String label) {

    return new IborFutureCurveNode(
        template, rateId, additionalSpread, label, CurveNodeDate.END, CurveNodeDateOrder.DEFAULT);
  }

  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.date = CurveNodeDate.END;
    builder.dateOrder = CurveNodeDateOrder.DEFAULT;
  }

  //-------------------------------------------------------------------------
  @Override
  public Set<ObservableId> requirements() {
    return ImmutableSet.of(rateId);
  }

  @Override
  public LocalDate date(LocalDate valuationDate, ReferenceData refData) {
    LocalDate referenceDate = template.calculateReferenceDateFromTradeDate(valuationDate, refData);
    return date.calculate(
        () -> calculateEnd(referenceDate, refData),
        () -> calculateLastFixingDate(valuationDate, refData));
  }

  @Override
  public DatedParameterMetadata metadata(LocalDate valuationDate, ReferenceData refData) {
    LocalDate nodeDate = date(valuationDate, refData);
    LocalDate referenceDate = template.calculateReferenceDateFromTradeDate(valuationDate, refData);
    if (label.isEmpty()) {
      return YearMonthDateParameterMetadata.of(nodeDate, YearMonth.from(referenceDate));
    }
    return YearMonthDateParameterMetadata.of(nodeDate, YearMonth.from(referenceDate), label);
  }

  // calculate the end date
  private LocalDate calculateEnd(LocalDate referenceDate, ReferenceData refData) {
    return template.getIndex().calculateMaturityFromEffective(referenceDate, refData);
  }

  // calculate the last fixing date
  private LocalDate calculateLastFixingDate(LocalDate valuationDate, ReferenceData refData) {
    SecurityId secId = SecurityId.of(rateId.getStandardId());  // quote must also be security
    IborFutureTrade trade = template.createTrade(valuationDate, secId, 1, 1, 1, refData);
    return trade.getProduct().getFixingDate();
  }

  @Override
  public IborFutureTrade trade(double quantity, MarketData marketData, ReferenceData refData) {
    LocalDate valuationDate = marketData.getValuationDate();
    double price = marketPrice(marketData) + additionalSpread;
    SecurityId secId = SecurityId.of(rateId.getStandardId());  // quote must also be security
    return template.createTrade(valuationDate, secId, quantity, 1d, price, refData);
  }

  @Override
  public ResolvedIborFutureTrade resolvedTrade(double quantity, MarketData marketData, ReferenceData refData) {
    return trade(quantity, marketData, refData).resolve(refData);
  }

  @Override
  public double initialGuess(MarketData marketData, ValueType valueType) {
    double rate = 1d - marketPrice(marketData);
    if (ValueType.ZERO_RATE.equals(valueType) || ValueType.FORWARD_RATE.equals(valueType)) {
      return rate;
    }
    if (ValueType.DISCOUNT_FACTOR.equals(valueType)) {
      double approximateMaturity = template.approximateMaturity(marketData.getValuationDate());
      return Math.exp(-approximateMaturity * rate);
    }
    return 0d;
  }

  // check if market value is correct
  private double marketPrice(MarketData marketData) {
    double price = marketData.getValue(rateId);
    ArgChecker.isTrue(price < 2, "Price must be in decimal form, such as 0.993 for a 0.7% rate, but was: {}", price);
    return price;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this node with the specified date.
   * 
   * @param date  the date to use
   * @return the node based on this node with the specified date
   */
  public IborFutureCurveNode withDate(CurveNodeDate date) {
    return new IborFutureCurveNode(template, rateId, additionalSpread, label, date, dateOrder);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IborFutureCurveNode}.
   * @return the meta-bean, not null
   */
  public static IborFutureCurveNode.Meta meta() {
    return IborFutureCurveNode.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(IborFutureCurveNode.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static IborFutureCurveNode.Builder builder() {
    return new IborFutureCurveNode.Builder();
  }

  private IborFutureCurveNode(
      IborFutureTemplate template,
      QuoteId rateId,
      double additionalSpread,
      String label,
      CurveNodeDate date,
      CurveNodeDateOrder dateOrder) {
    JodaBeanUtils.notNull(template, "template");
    JodaBeanUtils.notNull(rateId, "rateId");
    JodaBeanUtils.notNull(label, "label");
    JodaBeanUtils.notNull(dateOrder, "dateOrder");
    this.template = template;
    this.rateId = rateId;
    this.additionalSpread = additionalSpread;
    this.label = label;
    this.date = date;
    this.dateOrder = dateOrder;
  }

  @Override
  public IborFutureCurveNode.Meta metaBean() {
    return IborFutureCurveNode.Meta.INSTANCE;
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
   * Gets the template for the Ibor Futures associated with this node.
   * @return the value of the property, not null
   */
  public IborFutureTemplate getTemplate() {
    return template;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the identifier of the market data value which provides the price.
   * @return the value of the property, not null
   */
  public QuoteId getRateId() {
    return rateId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the additional spread added to the price.
   * This amount is directly added to the price, where 0.993 represents a 0.7% rate.
   * @return the value of the property
   */
  public double getAdditionalSpread() {
    return additionalSpread;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the label to use for the node, may be empty.
   * <p>
   * If empty, a default label will be created when the metadata is built.
   * The default label depends on the valuation date, so cannot be created in the node.
   * @return the value of the property, not null
   */
  @Override
  public String getLabel() {
    return label;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the method by which the date of the node is calculated, defaulted to 'End'.
   * @return the value of the property
   */
  public CurveNodeDate getDate() {
    return date;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the date order rules, used to ensure that the dates in the curve are in order.
   * If not specified, this will default to {@link CurveNodeDateOrder#DEFAULT}.
   * @return the value of the property, not null
   */
  @Override
  public CurveNodeDateOrder getDateOrder() {
    return dateOrder;
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
      IborFutureCurveNode other = (IborFutureCurveNode) obj;
      return JodaBeanUtils.equal(template, other.template) &&
          JodaBeanUtils.equal(rateId, other.rateId) &&
          JodaBeanUtils.equal(additionalSpread, other.additionalSpread) &&
          JodaBeanUtils.equal(label, other.label) &&
          JodaBeanUtils.equal(date, other.date) &&
          JodaBeanUtils.equal(dateOrder, other.dateOrder);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(template);
    hash = hash * 31 + JodaBeanUtils.hashCode(rateId);
    hash = hash * 31 + JodaBeanUtils.hashCode(additionalSpread);
    hash = hash * 31 + JodaBeanUtils.hashCode(label);
    hash = hash * 31 + JodaBeanUtils.hashCode(date);
    hash = hash * 31 + JodaBeanUtils.hashCode(dateOrder);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(224);
    buf.append("IborFutureCurveNode{");
    buf.append("template").append('=').append(template).append(',').append(' ');
    buf.append("rateId").append('=').append(rateId).append(',').append(' ');
    buf.append("additionalSpread").append('=').append(additionalSpread).append(',').append(' ');
    buf.append("label").append('=').append(label).append(',').append(' ');
    buf.append("date").append('=').append(date).append(',').append(' ');
    buf.append("dateOrder").append('=').append(JodaBeanUtils.toString(dateOrder));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IborFutureCurveNode}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code template} property.
     */
    private final MetaProperty<IborFutureTemplate> template = DirectMetaProperty.ofImmutable(
        this, "template", IborFutureCurveNode.class, IborFutureTemplate.class);
    /**
     * The meta-property for the {@code rateId} property.
     */
    private final MetaProperty<QuoteId> rateId = DirectMetaProperty.ofImmutable(
        this, "rateId", IborFutureCurveNode.class, QuoteId.class);
    /**
     * The meta-property for the {@code additionalSpread} property.
     */
    private final MetaProperty<Double> additionalSpread = DirectMetaProperty.ofImmutable(
        this, "additionalSpread", IborFutureCurveNode.class, Double.TYPE);
    /**
     * The meta-property for the {@code label} property.
     */
    private final MetaProperty<String> label = DirectMetaProperty.ofImmutable(
        this, "label", IborFutureCurveNode.class, String.class);
    /**
     * The meta-property for the {@code date} property.
     */
    private final MetaProperty<CurveNodeDate> date = DirectMetaProperty.ofImmutable(
        this, "date", IborFutureCurveNode.class, CurveNodeDate.class);
    /**
     * The meta-property for the {@code dateOrder} property.
     */
    private final MetaProperty<CurveNodeDateOrder> dateOrder = DirectMetaProperty.ofImmutable(
        this, "dateOrder", IborFutureCurveNode.class, CurveNodeDateOrder.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "template",
        "rateId",
        "additionalSpread",
        "label",
        "date",
        "dateOrder");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1321546630:  // template
          return template;
        case -938107365:  // rateId
          return rateId;
        case 291232890:  // additionalSpread
          return additionalSpread;
        case 102727412:  // label
          return label;
        case 3076014:  // date
          return date;
        case -263699392:  // dateOrder
          return dateOrder;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public IborFutureCurveNode.Builder builder() {
      return new IborFutureCurveNode.Builder();
    }

    @Override
    public Class<? extends IborFutureCurveNode> beanType() {
      return IborFutureCurveNode.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code template} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IborFutureTemplate> template() {
      return template;
    }

    /**
     * The meta-property for the {@code rateId} property.
     * @return the meta-property, not null
     */
    public MetaProperty<QuoteId> rateId() {
      return rateId;
    }

    /**
     * The meta-property for the {@code additionalSpread} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> additionalSpread() {
      return additionalSpread;
    }

    /**
     * The meta-property for the {@code label} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> label() {
      return label;
    }

    /**
     * The meta-property for the {@code date} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveNodeDate> date() {
      return date;
    }

    /**
     * The meta-property for the {@code dateOrder} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveNodeDateOrder> dateOrder() {
      return dateOrder;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1321546630:  // template
          return ((IborFutureCurveNode) bean).getTemplate();
        case -938107365:  // rateId
          return ((IborFutureCurveNode) bean).getRateId();
        case 291232890:  // additionalSpread
          return ((IborFutureCurveNode) bean).getAdditionalSpread();
        case 102727412:  // label
          return ((IborFutureCurveNode) bean).getLabel();
        case 3076014:  // date
          return ((IborFutureCurveNode) bean).getDate();
        case -263699392:  // dateOrder
          return ((IborFutureCurveNode) bean).getDateOrder();
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
   * The bean-builder for {@code IborFutureCurveNode}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<IborFutureCurveNode> {

    private IborFutureTemplate template;
    private QuoteId rateId;
    private double additionalSpread;
    private String label;
    private CurveNodeDate date;
    private CurveNodeDateOrder dateOrder;

    /**
     * Restricted constructor.
     */
    private Builder() {
      applyDefaults(this);
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(IborFutureCurveNode beanToCopy) {
      this.template = beanToCopy.getTemplate();
      this.rateId = beanToCopy.getRateId();
      this.additionalSpread = beanToCopy.getAdditionalSpread();
      this.label = beanToCopy.getLabel();
      this.date = beanToCopy.getDate();
      this.dateOrder = beanToCopy.getDateOrder();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1321546630:  // template
          return template;
        case -938107365:  // rateId
          return rateId;
        case 291232890:  // additionalSpread
          return additionalSpread;
        case 102727412:  // label
          return label;
        case 3076014:  // date
          return date;
        case -263699392:  // dateOrder
          return dateOrder;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1321546630:  // template
          this.template = (IborFutureTemplate) newValue;
          break;
        case -938107365:  // rateId
          this.rateId = (QuoteId) newValue;
          break;
        case 291232890:  // additionalSpread
          this.additionalSpread = (Double) newValue;
          break;
        case 102727412:  // label
          this.label = (String) newValue;
          break;
        case 3076014:  // date
          this.date = (CurveNodeDate) newValue;
          break;
        case -263699392:  // dateOrder
          this.dateOrder = (CurveNodeDateOrder) newValue;
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
    public IborFutureCurveNode build() {
      return new IborFutureCurveNode(
          template,
          rateId,
          additionalSpread,
          label,
          date,
          dateOrder);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the template for the Ibor Futures associated with this node.
     * @param template  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder template(IborFutureTemplate template) {
      JodaBeanUtils.notNull(template, "template");
      this.template = template;
      return this;
    }

    /**
     * Sets the identifier of the market data value which provides the price.
     * @param rateId  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder rateId(QuoteId rateId) {
      JodaBeanUtils.notNull(rateId, "rateId");
      this.rateId = rateId;
      return this;
    }

    /**
     * Sets the additional spread added to the price.
     * This amount is directly added to the price, where 0.993 represents a 0.7% rate.
     * @param additionalSpread  the new value
     * @return this, for chaining, not null
     */
    public Builder additionalSpread(double additionalSpread) {
      this.additionalSpread = additionalSpread;
      return this;
    }

    /**
     * Sets the label to use for the node, may be empty.
     * <p>
     * If empty, a default label will be created when the metadata is built.
     * The default label depends on the valuation date, so cannot be created in the node.
     * @param label  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder label(String label) {
      JodaBeanUtils.notNull(label, "label");
      this.label = label;
      return this;
    }

    /**
     * Sets the method by which the date of the node is calculated, defaulted to 'End'.
     * @param date  the new value
     * @return this, for chaining, not null
     */
    public Builder date(CurveNodeDate date) {
      this.date = date;
      return this;
    }

    /**
     * Sets the date order rules, used to ensure that the dates in the curve are in order.
     * If not specified, this will default to {@link CurveNodeDateOrder#DEFAULT}.
     * @param dateOrder  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dateOrder(CurveNodeDateOrder dateOrder) {
      JodaBeanUtils.notNull(dateOrder, "dateOrder");
      this.dateOrder = dateOrder;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(224);
      buf.append("IborFutureCurveNode.Builder{");
      buf.append("template").append('=').append(JodaBeanUtils.toString(template)).append(',').append(' ');
      buf.append("rateId").append('=').append(JodaBeanUtils.toString(rateId)).append(',').append(' ');
      buf.append("additionalSpread").append('=').append(JodaBeanUtils.toString(additionalSpread)).append(',').append(' ');
      buf.append("label").append('=').append(JodaBeanUtils.toString(label)).append(',').append(' ');
      buf.append("date").append('=').append(JodaBeanUtils.toString(date)).append(',').append(' ');
      buf.append("dateOrder").append('=').append(JodaBeanUtils.toString(dateOrder));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
