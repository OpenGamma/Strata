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
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.ImmutablePreBuild;
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
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.data.FxRateId;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.CurveNodeDate;
import com.opengamma.strata.market.curve.CurveNodeDateOrder;
import com.opengamma.strata.market.param.DatedParameterMetadata;
import com.opengamma.strata.market.param.LabelDateParameterMetadata;
import com.opengamma.strata.market.param.TenorDateParameterMetadata;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.fx.FxSwapTrade;
import com.opengamma.strata.product.fx.ResolvedFxSwapTrade;
import com.opengamma.strata.product.fx.type.FxSwapTemplate;

/**
 * A curve node whose instrument is an FX Swap.
 * <p>
 * The trade produced by the node will pay near and receive far in the second currency (BUY)
 * for a positive quantity and a receive near and pay far (SELL) for a negative quantity.
 * This convention is line with other nodes where a positive quantity is similar to long a bond or deposit,
 * here the long deposit-like is in the second currency.
 */
@BeanDefinition
public final class FxSwapCurveNode
    implements CurveNode, ImmutableBean, Serializable {

  /**
   * The template for the FX Swap associated with this node.
   */
  @PropertyDefinition(validate = "notNull")
  private final FxSwapTemplate template;
  /**
   * The identifier used to obtain the FX rate market value, defaulted from the template.
   * This only needs to be specified if using multiple market data sources.
   */
  @PropertyDefinition(validate = "notNull")
  private final FxRateId fxRateId;
  /**
   * The identifier of the market data value which provides the FX forward points.
   */
  @PropertyDefinition(validate = "notNull")
  private final ObservableId farForwardPointsId;
  /**
   * The label to use for the node, defaulted.
   * <p>
   * When building, this will default based on the far period if not specified.
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
   * Returns a curve node for an FX Swap using the specified instrument template and keys.
   * <p>
   * A suitable default label will be created.
   *
   * @param template  the template used for building the instrument for the node
   * @param farForwardPointsId  the identifier of the FX points at the far date
   * @return a node whose instrument is built from the template using a market rate
   */
  public static FxSwapCurveNode of(FxSwapTemplate template, ObservableId farForwardPointsId) {
    return builder()
        .template(template)
        .farForwardPointsId(farForwardPointsId)
        .build();
  }

  /**
   * Returns a curve node for an FX Swap using the specified instrument template and keys and label.
   *
   * @param template  the template used for building the instrument for the node
   * @param farForwardPointsId  the identifier of the FX points at the far date
   * @param label  the label to use for the node
   * @return a node whose instrument is built from the template using a market rate
   */
  public static FxSwapCurveNode of(FxSwapTemplate template, ObservableId farForwardPointsId, String label) {
    return builder()
        .template(template)
        .farForwardPointsId(farForwardPointsId)
        .label(label)
        .build();
  }

  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.date = CurveNodeDate.END;
    builder.dateOrder = CurveNodeDateOrder.DEFAULT;
  }

  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.template != null) {
      if (builder.label == null) {
        builder.label = Tenor.of(builder.template.getPeriodToFar()).toString();
      }
      if (builder.fxRateId == null) {
        builder.fxRateId = FxRateId.of(builder.template.getCurrencyPair());
      } else {
        ArgChecker.isTrue(
            builder.fxRateId.getPair().toConventional().equals(builder.template.getCurrencyPair().toConventional()),
            "FxRateId currency pair '{}' must match that of the template '{}'",
            builder.fxRateId.getPair(),
            builder.template.getCurrencyPair());
      }
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public Set<? extends MarketDataId<?>> requirements() {
    // TODO: extra identifier for near forward points
    return ImmutableSet.of(fxRateId, farForwardPointsId);
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
    Tenor tenor = Tenor.of(template.getPeriodToFar());
    return TenorDateParameterMetadata.of(nodeDate, tenor, label);
  }

  // calculate the end date
  private LocalDate calculateEnd(LocalDate valuationDate, ReferenceData refData) {
    FxSwapTrade trade = template.createTrade(valuationDate, BuySell.BUY, 1, 1, 0, refData);
    return trade.getProduct().getFarLeg().resolve(refData).getPaymentDate();
  }

  // calculate the last fixing date
  private LocalDate calculateLastFixingDate(LocalDate valuationDate, ReferenceData refData) {
    throw new UnsupportedOperationException("Node date of 'LastFixing' is not supported for FxSwap");
  }

  @Override
  public FxSwapTrade trade(double quantity, MarketData marketData, ReferenceData refData) {
    FxRate fxRate = marketData.getValue(fxRateId);
    double rate = fxRate.fxRate(template.getCurrencyPair());
    double fxPts = marketData.getValue(farForwardPointsId);
    BuySell buySell = quantity > 0 ? BuySell.BUY : BuySell.SELL;
    return template.createTrade(marketData.getValuationDate(), buySell, Math.abs(quantity), rate, fxPts, refData);
  }

  @Override
  public ResolvedFxSwapTrade resolvedTrade(double quantity, MarketData marketData, ReferenceData refData) {
    return trade(quantity, marketData, refData).resolve(refData);
  }

  @Override
  public double initialGuess(MarketData marketData, ValueType valueType) {
    if (ValueType.DISCOUNT_FACTOR.equals(valueType)) {
      return 1d;
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
  public FxSwapCurveNode withDate(CurveNodeDate date) {
    return new FxSwapCurveNode(template, fxRateId, farForwardPointsId, label, date, dateOrder);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FxSwapCurveNode}.
   * @return the meta-bean, not null
   */
  public static FxSwapCurveNode.Meta meta() {
    return FxSwapCurveNode.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FxSwapCurveNode.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static FxSwapCurveNode.Builder builder() {
    return new FxSwapCurveNode.Builder();
  }

  private FxSwapCurveNode(
      FxSwapTemplate template,
      FxRateId fxRateId,
      ObservableId farForwardPointsId,
      String label,
      CurveNodeDate date,
      CurveNodeDateOrder dateOrder) {
    JodaBeanUtils.notNull(template, "template");
    JodaBeanUtils.notNull(fxRateId, "fxRateId");
    JodaBeanUtils.notNull(farForwardPointsId, "farForwardPointsId");
    JodaBeanUtils.notEmpty(label, "label");
    JodaBeanUtils.notNull(dateOrder, "dateOrder");
    this.template = template;
    this.fxRateId = fxRateId;
    this.farForwardPointsId = farForwardPointsId;
    this.label = label;
    this.date = date;
    this.dateOrder = dateOrder;
  }

  @Override
  public FxSwapCurveNode.Meta metaBean() {
    return FxSwapCurveNode.Meta.INSTANCE;
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
   * Gets the template for the FX Swap associated with this node.
   * @return the value of the property, not null
   */
  public FxSwapTemplate getTemplate() {
    return template;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the identifier used to obtain the FX rate market value, defaulted from the template.
   * This only needs to be specified if using multiple market data sources.
   * @return the value of the property, not null
   */
  public FxRateId getFxRateId() {
    return fxRateId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the identifier of the market data value which provides the FX forward points.
   * @return the value of the property, not null
   */
  public ObservableId getFarForwardPointsId() {
    return farForwardPointsId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the label to use for the node, defaulted.
   * <p>
   * When building, this will default based on the far period if not specified.
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
      FxSwapCurveNode other = (FxSwapCurveNode) obj;
      return JodaBeanUtils.equal(template, other.template) &&
          JodaBeanUtils.equal(fxRateId, other.fxRateId) &&
          JodaBeanUtils.equal(farForwardPointsId, other.farForwardPointsId) &&
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
    hash = hash * 31 + JodaBeanUtils.hashCode(fxRateId);
    hash = hash * 31 + JodaBeanUtils.hashCode(farForwardPointsId);
    hash = hash * 31 + JodaBeanUtils.hashCode(label);
    hash = hash * 31 + JodaBeanUtils.hashCode(date);
    hash = hash * 31 + JodaBeanUtils.hashCode(dateOrder);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(224);
    buf.append("FxSwapCurveNode{");
    buf.append("template").append('=').append(template).append(',').append(' ');
    buf.append("fxRateId").append('=').append(fxRateId).append(',').append(' ');
    buf.append("farForwardPointsId").append('=').append(farForwardPointsId).append(',').append(' ');
    buf.append("label").append('=').append(label).append(',').append(' ');
    buf.append("date").append('=').append(date).append(',').append(' ');
    buf.append("dateOrder").append('=').append(JodaBeanUtils.toString(dateOrder));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FxSwapCurveNode}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code template} property.
     */
    private final MetaProperty<FxSwapTemplate> template = DirectMetaProperty.ofImmutable(
        this, "template", FxSwapCurveNode.class, FxSwapTemplate.class);
    /**
     * The meta-property for the {@code fxRateId} property.
     */
    private final MetaProperty<FxRateId> fxRateId = DirectMetaProperty.ofImmutable(
        this, "fxRateId", FxSwapCurveNode.class, FxRateId.class);
    /**
     * The meta-property for the {@code farForwardPointsId} property.
     */
    private final MetaProperty<ObservableId> farForwardPointsId = DirectMetaProperty.ofImmutable(
        this, "farForwardPointsId", FxSwapCurveNode.class, ObservableId.class);
    /**
     * The meta-property for the {@code label} property.
     */
    private final MetaProperty<String> label = DirectMetaProperty.ofImmutable(
        this, "label", FxSwapCurveNode.class, String.class);
    /**
     * The meta-property for the {@code date} property.
     */
    private final MetaProperty<CurveNodeDate> date = DirectMetaProperty.ofImmutable(
        this, "date", FxSwapCurveNode.class, CurveNodeDate.class);
    /**
     * The meta-property for the {@code dateOrder} property.
     */
    private final MetaProperty<CurveNodeDateOrder> dateOrder = DirectMetaProperty.ofImmutable(
        this, "dateOrder", FxSwapCurveNode.class, CurveNodeDateOrder.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "template",
        "fxRateId",
        "farForwardPointsId",
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
        case -1054985843:  // fxRateId
          return fxRateId;
        case -566044884:  // farForwardPointsId
          return farForwardPointsId;
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
    public FxSwapCurveNode.Builder builder() {
      return new FxSwapCurveNode.Builder();
    }

    @Override
    public Class<? extends FxSwapCurveNode> beanType() {
      return FxSwapCurveNode.class;
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
    public MetaProperty<FxSwapTemplate> template() {
      return template;
    }

    /**
     * The meta-property for the {@code fxRateId} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FxRateId> fxRateId() {
      return fxRateId;
    }

    /**
     * The meta-property for the {@code farForwardPointsId} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ObservableId> farForwardPointsId() {
      return farForwardPointsId;
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
          return ((FxSwapCurveNode) bean).getTemplate();
        case -1054985843:  // fxRateId
          return ((FxSwapCurveNode) bean).getFxRateId();
        case -566044884:  // farForwardPointsId
          return ((FxSwapCurveNode) bean).getFarForwardPointsId();
        case 102727412:  // label
          return ((FxSwapCurveNode) bean).getLabel();
        case 3076014:  // date
          return ((FxSwapCurveNode) bean).getDate();
        case -263699392:  // dateOrder
          return ((FxSwapCurveNode) bean).getDateOrder();
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
   * The bean-builder for {@code FxSwapCurveNode}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<FxSwapCurveNode> {

    private FxSwapTemplate template;
    private FxRateId fxRateId;
    private ObservableId farForwardPointsId;
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
    private Builder(FxSwapCurveNode beanToCopy) {
      this.template = beanToCopy.getTemplate();
      this.fxRateId = beanToCopy.getFxRateId();
      this.farForwardPointsId = beanToCopy.getFarForwardPointsId();
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
        case -1054985843:  // fxRateId
          return fxRateId;
        case -566044884:  // farForwardPointsId
          return farForwardPointsId;
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
          this.template = (FxSwapTemplate) newValue;
          break;
        case -1054985843:  // fxRateId
          this.fxRateId = (FxRateId) newValue;
          break;
        case -566044884:  // farForwardPointsId
          this.farForwardPointsId = (ObservableId) newValue;
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
    public FxSwapCurveNode build() {
      preBuild(this);
      return new FxSwapCurveNode(
          template,
          fxRateId,
          farForwardPointsId,
          label,
          date,
          dateOrder);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the template for the FX Swap associated with this node.
     * @param template  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder template(FxSwapTemplate template) {
      JodaBeanUtils.notNull(template, "template");
      this.template = template;
      return this;
    }

    /**
     * Sets the identifier used to obtain the FX rate market value, defaulted from the template.
     * This only needs to be specified if using multiple market data sources.
     * @param fxRateId  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder fxRateId(FxRateId fxRateId) {
      JodaBeanUtils.notNull(fxRateId, "fxRateId");
      this.fxRateId = fxRateId;
      return this;
    }

    /**
     * Sets the identifier of the market data value which provides the FX forward points.
     * @param farForwardPointsId  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder farForwardPointsId(ObservableId farForwardPointsId) {
      JodaBeanUtils.notNull(farForwardPointsId, "farForwardPointsId");
      this.farForwardPointsId = farForwardPointsId;
      return this;
    }

    /**
     * Sets the label to use for the node, defaulted.
     * <p>
     * When building, this will default based on the far period if not specified.
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
      buf.append("FxSwapCurveNode.Builder{");
      buf.append("template").append('=').append(JodaBeanUtils.toString(template)).append(',').append(' ');
      buf.append("fxRateId").append('=').append(JodaBeanUtils.toString(fxRateId)).append(',').append(' ');
      buf.append("farForwardPointsId").append('=').append(JodaBeanUtils.toString(farForwardPointsId)).append(',').append(' ');
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
