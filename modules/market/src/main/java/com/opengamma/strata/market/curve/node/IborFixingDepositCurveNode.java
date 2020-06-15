/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.node;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableDefaults;
import org.joda.beans.gen.ImmutablePreBuild;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.CurveNodeDate;
import com.opengamma.strata.market.curve.CurveNodeDateOrder;
import com.opengamma.strata.market.param.DatedParameterMetadata;
import com.opengamma.strata.market.param.LabelDateParameterMetadata;
import com.opengamma.strata.market.param.TenorDateParameterMetadata;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.deposit.IborFixingDepositTrade;
import com.opengamma.strata.product.deposit.ResolvedIborFixingDeposit;
import com.opengamma.strata.product.deposit.ResolvedIborFixingDepositTrade;
import com.opengamma.strata.product.deposit.type.IborFixingDepositTemplate;

/**
 * A curve node whose instrument is an Ibor fixing deposit.
 * <p>
 * The trade produced by the node will be BUY for a positive quantity and a SELL for a negative quantity.
 * This convention is line with other nodes where a positive quantity is similar to long a bond or deposit.
 */
@BeanDefinition
public final class IborFixingDepositCurveNode
    implements CurveNode, ImmutableBean, Serializable {

  /**
   * The template for the Ibor fixing deposit associated with this node.
   */
  @PropertyDefinition(validate = "notNull")
  private final IborFixingDepositTemplate template;
  /**
   * The identifier of the market data value that provides the rate.
   */
  @PropertyDefinition(validate = "notNull")
  private final ObservableId rateId;
  /**
   * The additional spread added to the rate.
   */
  @PropertyDefinition
  private final double additionalSpread;
  /**
   * The label to use for the node, defaulted.
   * <p>
   * When building, this will default based on the deposit period if not specified.
   */
  @PropertyDefinition(validate = "notEmpty", overrideGet = true)
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
   * Returns a curve node for an Ibor deposit using the specified template and rate key.
   * <p>
   * A suitable default label will be created.
   *
   * @param template  the template used for building the instrument for the node
   * @param rateId  the identifier of the market rate used when building the instrument for the node
   * @return a node whose instrument is built from the template using a market rate
   */
  public static IborFixingDepositCurveNode of(IborFixingDepositTemplate template, ObservableId rateId) {
    return of(template, rateId, 0d);
  }

  /**
   * Returns a curve node for an Ibor deposit using the specified template, rate key and spread.
   * <p>
   * A suitable default label will be created.
   *
   * @param template  the template defining the node instrument
   * @param rateId  the identifier of the market data providing the rate for the node instrument
   * @param additionalSpread  the additional spread amount added to the rate
   * @return a node whose instrument is built from the template using a market rate
   */
  public static IborFixingDepositCurveNode of(
      IborFixingDepositTemplate template,
      ObservableId rateId,
      double additionalSpread) {

    return builder()
        .template(template)
        .rateId(rateId)
        .additionalSpread(additionalSpread)
        .build();
  }

  /**
   * Returns a curve node for an Ibor deposit using the specified template, rate key, spread and label.
   *
   * @param template  the template defining the node instrument
   * @param rateId  the identifier of the market data providing the rate for the node instrument
   * @param additionalSpread  the additional spread amount added to the rate
   * @param label  the label to use for the node, if null or empty an appropriate default label will be used
   * @return a node whose instrument is built from the template using a market rate
   */
  public static IborFixingDepositCurveNode of(
      IborFixingDepositTemplate template,
      ObservableId rateId,
      double additionalSpread,
      String label) {

    return new IborFixingDepositCurveNode(
        template, rateId, additionalSpread, label, CurveNodeDate.END, CurveNodeDateOrder.DEFAULT);
  }

  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.date = CurveNodeDate.END;
    builder.dateOrder = CurveNodeDateOrder.DEFAULT;
  }

  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.label == null && builder.template != null) {
      builder.label = Tenor.of(builder.template.getDepositPeriod()).toString();
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public Set<ObservableId> requirements() {
    return ImmutableSet.of(rateId);
  }

  @Override
  public LocalDate date(LocalDate valuationDate, ReferenceData refData) {
    return date.calculate(
        () -> calculateEnd(valuationDate, refData),
        () -> calculateLastFixingDate(valuationDate, refData));
  }

  @Override
  public DatedParameterMetadata metadata(LocalDate valuationDate, ReferenceData refData) {
    LocalDate nodeDate = date(valuationDate, refData);
    if (date.isFixed()) {
      return LabelDateParameterMetadata.of(nodeDate, label);
    }
    Tenor tenor = Tenor.of(template.getDepositPeriod());
    return TenorDateParameterMetadata.of(nodeDate, tenor, label);
  }

  // calculate the end date
  private LocalDate calculateEnd(LocalDate valuationDate, ReferenceData refData) {
    IborFixingDepositTrade trade = template.createTrade(valuationDate, BuySell.BUY, 0d, 0d, refData);
    ResolvedIborFixingDeposit deposit = trade.getProduct().resolve(refData);
    return deposit.getEndDate();
  }

  // calculate the last fixing date
  private LocalDate calculateLastFixingDate(LocalDate valuationDate, ReferenceData refData) {
    IborFixingDepositTrade trade = template.createTrade(valuationDate, BuySell.BUY, 0d, 0d, refData);
    ResolvedIborFixingDeposit deposit = trade.getProduct().resolve(refData);
    return deposit.getFloatingRate().getFixingDate();
  }

  @Override
  public IborFixingDepositTrade trade(double quantity, MarketData marketData, ReferenceData refData) {
    double fixedRate = marketData.getValue(rateId) + additionalSpread;
    BuySell buySell = quantity > 0 ? BuySell.BUY : BuySell.SELL;
    return template.createTrade(marketData.getValuationDate(), buySell, Math.abs(quantity), fixedRate, refData);
  }

  @Override
  public ResolvedIborFixingDepositTrade resolvedTrade(double quantity, MarketData marketData, ReferenceData refData) {
    return trade(quantity, marketData, refData).resolve(refData);
  }

  @Override
  public ResolvedIborFixingDepositTrade sampleResolvedTrade(
      LocalDate valuationDate,
      FxRateProvider fxProvider,
      ReferenceData refData) {

    IborFixingDepositTrade trade = template.createTrade(valuationDate, BuySell.BUY, 1d, additionalSpread, refData);
    return trade.resolve(refData);
  }

  @Override
  public double initialGuess(MarketData marketData, ValueType valueType) {
    if (ValueType.ZERO_RATE.equals(valueType) || ValueType.FORWARD_RATE.equals(valueType)) {
      return marketData.getValue(rateId);
    }
    if (ValueType.DISCOUNT_FACTOR.equals(valueType)) {
      double approximateMaturity = template.getDepositPeriod().toTotalMonths() / 12.0d;
      return Math.exp(-approximateMaturity * marketData.getValue(rateId));
    }
    return 0d;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this node with the specified date.
   * 
   * @param date  the date to use
   * @return the node based on this node with the specified date
   */
  public IborFixingDepositCurveNode withDate(CurveNodeDate date) {
    return new IborFixingDepositCurveNode(template, rateId, additionalSpread, label, date, dateOrder);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code IborFixingDepositCurveNode}.
   * @return the meta-bean, not null
   */
  public static IborFixingDepositCurveNode.Meta meta() {
    return IborFixingDepositCurveNode.Meta.INSTANCE;
  }

  static {
    MetaBean.register(IborFixingDepositCurveNode.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static IborFixingDepositCurveNode.Builder builder() {
    return new IborFixingDepositCurveNode.Builder();
  }

  private IborFixingDepositCurveNode(
      IborFixingDepositTemplate template,
      ObservableId rateId,
      double additionalSpread,
      String label,
      CurveNodeDate date,
      CurveNodeDateOrder dateOrder) {
    JodaBeanUtils.notNull(template, "template");
    JodaBeanUtils.notNull(rateId, "rateId");
    JodaBeanUtils.notEmpty(label, "label");
    JodaBeanUtils.notNull(dateOrder, "dateOrder");
    this.template = template;
    this.rateId = rateId;
    this.additionalSpread = additionalSpread;
    this.label = label;
    this.date = date;
    this.dateOrder = dateOrder;
  }

  @Override
  public IborFixingDepositCurveNode.Meta metaBean() {
    return IborFixingDepositCurveNode.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the template for the Ibor fixing deposit associated with this node.
   * @return the value of the property, not null
   */
  public IborFixingDepositTemplate getTemplate() {
    return template;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the identifier of the market data value that provides the rate.
   * @return the value of the property, not null
   */
  public ObservableId getRateId() {
    return rateId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the additional spread added to the rate.
   * @return the value of the property
   */
  public double getAdditionalSpread() {
    return additionalSpread;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the label to use for the node, defaulted.
   * <p>
   * When building, this will default based on the deposit period if not specified.
   * @return the value of the property, not empty
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
      IborFixingDepositCurveNode other = (IborFixingDepositCurveNode) obj;
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
    buf.append("IborFixingDepositCurveNode{");
    buf.append("template").append('=').append(JodaBeanUtils.toString(template)).append(',').append(' ');
    buf.append("rateId").append('=').append(JodaBeanUtils.toString(rateId)).append(',').append(' ');
    buf.append("additionalSpread").append('=').append(JodaBeanUtils.toString(additionalSpread)).append(',').append(' ');
    buf.append("label").append('=').append(JodaBeanUtils.toString(label)).append(',').append(' ');
    buf.append("date").append('=').append(JodaBeanUtils.toString(date)).append(',').append(' ');
    buf.append("dateOrder").append('=').append(JodaBeanUtils.toString(dateOrder));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IborFixingDepositCurveNode}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code template} property.
     */
    private final MetaProperty<IborFixingDepositTemplate> template = DirectMetaProperty.ofImmutable(
        this, "template", IborFixingDepositCurveNode.class, IborFixingDepositTemplate.class);
    /**
     * The meta-property for the {@code rateId} property.
     */
    private final MetaProperty<ObservableId> rateId = DirectMetaProperty.ofImmutable(
        this, "rateId", IborFixingDepositCurveNode.class, ObservableId.class);
    /**
     * The meta-property for the {@code additionalSpread} property.
     */
    private final MetaProperty<Double> additionalSpread = DirectMetaProperty.ofImmutable(
        this, "additionalSpread", IborFixingDepositCurveNode.class, Double.TYPE);
    /**
     * The meta-property for the {@code label} property.
     */
    private final MetaProperty<String> label = DirectMetaProperty.ofImmutable(
        this, "label", IborFixingDepositCurveNode.class, String.class);
    /**
     * The meta-property for the {@code date} property.
     */
    private final MetaProperty<CurveNodeDate> date = DirectMetaProperty.ofImmutable(
        this, "date", IborFixingDepositCurveNode.class, CurveNodeDate.class);
    /**
     * The meta-property for the {@code dateOrder} property.
     */
    private final MetaProperty<CurveNodeDateOrder> dateOrder = DirectMetaProperty.ofImmutable(
        this, "dateOrder", IborFixingDepositCurveNode.class, CurveNodeDateOrder.class);
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
    public IborFixingDepositCurveNode.Builder builder() {
      return new IborFixingDepositCurveNode.Builder();
    }

    @Override
    public Class<? extends IborFixingDepositCurveNode> beanType() {
      return IborFixingDepositCurveNode.class;
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
    public MetaProperty<IborFixingDepositTemplate> template() {
      return template;
    }

    /**
     * The meta-property for the {@code rateId} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ObservableId> rateId() {
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
          return ((IborFixingDepositCurveNode) bean).getTemplate();
        case -938107365:  // rateId
          return ((IborFixingDepositCurveNode) bean).getRateId();
        case 291232890:  // additionalSpread
          return ((IborFixingDepositCurveNode) bean).getAdditionalSpread();
        case 102727412:  // label
          return ((IborFixingDepositCurveNode) bean).getLabel();
        case 3076014:  // date
          return ((IborFixingDepositCurveNode) bean).getDate();
        case -263699392:  // dateOrder
          return ((IborFixingDepositCurveNode) bean).getDateOrder();
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
   * The bean-builder for {@code IborFixingDepositCurveNode}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<IborFixingDepositCurveNode> {

    private IborFixingDepositTemplate template;
    private ObservableId rateId;
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
    private Builder(IborFixingDepositCurveNode beanToCopy) {
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
          this.template = (IborFixingDepositTemplate) newValue;
          break;
        case -938107365:  // rateId
          this.rateId = (ObservableId) newValue;
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
    public IborFixingDepositCurveNode build() {
      preBuild(this);
      return new IborFixingDepositCurveNode(
          template,
          rateId,
          additionalSpread,
          label,
          date,
          dateOrder);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the template for the Ibor fixing deposit associated with this node.
     * @param template  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder template(IborFixingDepositTemplate template) {
      JodaBeanUtils.notNull(template, "template");
      this.template = template;
      return this;
    }

    /**
     * Sets the identifier of the market data value that provides the rate.
     * @param rateId  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder rateId(ObservableId rateId) {
      JodaBeanUtils.notNull(rateId, "rateId");
      this.rateId = rateId;
      return this;
    }

    /**
     * Sets the additional spread added to the rate.
     * @param additionalSpread  the new value
     * @return this, for chaining, not null
     */
    public Builder additionalSpread(double additionalSpread) {
      this.additionalSpread = additionalSpread;
      return this;
    }

    /**
     * Sets the label to use for the node, defaulted.
     * <p>
     * When building, this will default based on the deposit period if not specified.
     * @param label  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder label(String label) {
      JodaBeanUtils.notEmpty(label, "label");
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
      buf.append("IborFixingDepositCurveNode.Builder{");
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

  //-------------------------- AUTOGENERATED END --------------------------
}
