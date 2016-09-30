/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.node;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.ImmutablePreBuild;
import org.joda.beans.ImmutableValidator;
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
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.CurveNodeDateOrder;
import com.opengamma.strata.market.param.DatedParameterMetadata;
import com.opengamma.strata.market.param.TenorDateParameterMetadata;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.credit.cds.CdsCalibrationTrade;
import com.opengamma.strata.product.credit.cds.CdsQuote;
import com.opengamma.strata.product.credit.cds.CdsTrade;
import com.opengamma.strata.product.credit.cds.ResolvedCdsTrade;
import com.opengamma.strata.product.credit.cds.type.CdsQuoteConvention;
import com.opengamma.strata.product.credit.cds.type.CdsTemplate;

/**
 * A curve node whose instrument is a credit default swap.
 * <p>
 * The trade produced by the node will be a protection receiver (BUY) for a positive quantity
 * and a protection payer (SELL) for a negative quantity.
 */
@BeanDefinition
public final class CdsCurveNode
    implements CurveNode, ImmutableBean, Serializable {

  /**
   * The template for the CDS associated with this node.
   */
  @PropertyDefinition(validate = "notNull")
  private final CdsTemplate template;
  /**
   * The identifier of the market data value that provides the quoted value.
   */
  @PropertyDefinition(validate = "notNull")
  private final ObservableId observableId;
  /**
   * The label to use for the node, defaulted.
   * <p>
   * When building, this will default based on the tenor if not specified.
   */
  @PropertyDefinition(validate = "notEmpty", overrideGet = true)
  private final String label;
  /**
   * The legal entity identifier.
   * <p>
   * This identifier is used for the reference legal entity of the CDS.
   */
  @PropertyDefinition(validate = "notNull")
  private final StandardId legalEntityId;
  /**
   * The node date.
   * <p>
   * The node date must be end date.
   * If this field is null, the end date is computed based on the semi-annual roll convention.
   */
  @PropertyDefinition(get = "optional")
  private final LocalDate endDate;
  /**
   * The date order rules.
   * <p>
   * This is used to ensure that the dates in the curve are in order.
   * If not specified, this will default to {@link CurveNodeDateOrder#DEFAULT}.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final CurveNodeDateOrder dateOrder;
  /**
   * The market quote convention.
   * <p>
   * The CDS is quoted in par spread, points upfront or quoted spread.
   * See {@link CdsQuoteConvention} for detail.
   */
  @PropertyDefinition(validate = "notNull")
  private final CdsQuoteConvention quoteConvention;
  /**
   * The fixed coupon rate.
   * <p>
   * This must be represented in decimal form.
   */
  @PropertyDefinition(get = "optional")
  private final Double fixedRate;

  //-------------------------------------------------------------------------

  /**
   * Returns a curve node with par spread convention.
   * 
   * @param template  the template
   * @param observableId  the observable ID
   * @param legalEntityId  the legal entity ID
   * @return the curve node
   */
  public static CdsCurveNode ofParSpread(
      CdsTemplate template,
      ObservableId observableId,
      StandardId legalEntityId) {

    return builder()
        .template(template)
        .observableId(observableId)
        .legalEntityId(legalEntityId)
        .quoteConvention(CdsQuoteConvention.PAR_SPREAD)
        .build();
  }

  /**
   * Returns a curve node with points upfront convention.
   * 
   * @param template  the template
   * @param observableId  the observable ID
   * @param legalEntityId  the legal entity ID
   * @param fixedRate  the fixed rate
   * @return the curve node
   */
  public static CdsCurveNode ofPointsUpfront(
      CdsTemplate template,
      ObservableId observableId,
      StandardId legalEntityId,
      Double fixedRate) {

    return builder()
        .template(template)
        .observableId(observableId)
        .legalEntityId(legalEntityId)
        .quoteConvention(CdsQuoteConvention.POINTS_UPFRONT)
        .fixedRate(fixedRate)
        .build();
  }

  /**
   * Returns a curve node with quoted spread convention.
   * 
   * @param template  the template
   * @param observableId  the observable ID
   * @param legalEntityId  the legal entity ID
   * @param fixedRate  the fixed rate
   * @return the curve node
   */
  public static CdsCurveNode ofQuotedSpread(
      CdsTemplate template,
      ObservableId observableId,
      StandardId legalEntityId,
      Double fixedRate) {

    return builder()
        .template(template)
        .observableId(observableId)
        .legalEntityId(legalEntityId)
        .quoteConvention(CdsQuoteConvention.QUOTED_SPREAD)
        .fixedRate(fixedRate)
        .build();
  }

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.dateOrder = CurveNodeDateOrder.DEFAULT;
  }

  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.label == null && builder.template != null) {
      builder.label = builder.template.getTenor().toString();
    }
  }

  @ImmutableValidator
  private void validate() {
    if (quoteConvention.equals(CdsQuoteConvention.PAR_SPREAD)) {
      ArgChecker.isTrue(fixedRate == null, "The fixed rate must be empty for par spread quote");
    } else {
      ArgChecker.isTrue(fixedRate != null,
          "The fixed rate must be specifed if quote convention is points upfront or quoted spread");
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public Set<ObservableId> requirements() {
    return ImmutableSet.of(observableId);
  }

  @Override
  public LocalDate date(LocalDate valuationDate, ReferenceData refData) {
    return getEndDate().orElse(calculateEnd(valuationDate, refData));
  }

  private LocalDate calculateEnd(LocalDate valuationDate, ReferenceData refData) {
    CdsTrade trade = template.createTrade(legalEntityId, valuationDate, BuySell.BUY, 1, 1, refData);
    return trade.getProduct().resolve(refData).getProtectionEndDate();
  }

  @Override
  public DatedParameterMetadata metadata(LocalDate valuationDate, ReferenceData refData) {
    LocalDate nodeDate = date(valuationDate, refData);
    return TenorDateParameterMetadata.of(nodeDate, template.getTenor(), label);
  }

  @Override
  public CdsCalibrationTrade trade(double quantity, MarketData marketData, ReferenceData refData) {
    BuySell buySell = quantity > 0 ? BuySell.BUY : BuySell.SELL;
    LocalDate valuationDate = marketData.getValuationDate();
    double quoteValue = marketData.getValue(observableId);
    CdsQuote quote = CdsQuote.of(quoteConvention, quoteValue);
    double notional = Math.abs(quantity);
    if (quoteConvention.equals(CdsQuoteConvention.PAR_SPREAD)) {
      return CdsCalibrationTrade.of(
          template.createTrade(legalEntityId, valuationDate, buySell, notional, quoteValue, refData), quote);
    }
    double coupon = getFixedRate().getAsDouble(); // always success
    return CdsCalibrationTrade.of(
        template.createTrade(legalEntityId, valuationDate, buySell, notional, coupon, refData), quote);
  }

  @Override
  public ResolvedCdsTrade resolvedTrade(double quantity, MarketData marketData, ReferenceData refData) {
    return trade(quantity, marketData, refData).getUnderlyingTrade().resolve(refData);
  }

  @Override
  public double initialGuess(MarketData marketData, ValueType valueType) {
    throw new IllegalArgumentException("Initial guess must be compuited in calibrator");
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CdsCurveNode}.
   * @return the meta-bean, not null
   */
  public static CdsCurveNode.Meta meta() {
    return CdsCurveNode.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CdsCurveNode.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static CdsCurveNode.Builder builder() {
    return new CdsCurveNode.Builder();
  }

  private CdsCurveNode(
      CdsTemplate template,
      ObservableId observableId,
      String label,
      StandardId legalEntityId,
      LocalDate endDate,
      CurveNodeDateOrder dateOrder,
      CdsQuoteConvention quoteConvention,
      Double fixedRate) {
    JodaBeanUtils.notNull(template, "template");
    JodaBeanUtils.notNull(observableId, "observableId");
    JodaBeanUtils.notEmpty(label, "label");
    JodaBeanUtils.notNull(legalEntityId, "legalEntityId");
    JodaBeanUtils.notNull(dateOrder, "dateOrder");
    JodaBeanUtils.notNull(quoteConvention, "quoteConvention");
    this.template = template;
    this.observableId = observableId;
    this.label = label;
    this.legalEntityId = legalEntityId;
    this.endDate = endDate;
    this.dateOrder = dateOrder;
    this.quoteConvention = quoteConvention;
    this.fixedRate = fixedRate;
    validate();
  }

  @Override
  public CdsCurveNode.Meta metaBean() {
    return CdsCurveNode.Meta.INSTANCE;
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
   * Gets the template for the CDS associated with this node.
   * @return the value of the property, not null
   */
  public CdsTemplate getTemplate() {
    return template;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the identifier of the market data value that provides the quoted value.
   * @return the value of the property, not null
   */
  public ObservableId getObservableId() {
    return observableId;
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
   * Gets the legal entity identifier.
   * <p>
   * This identifier is used for the reference legal entity of the CDS.
   * @return the value of the property, not null
   */
  public StandardId getLegalEntityId() {
    return legalEntityId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the node date.
   * <p>
   * The node date must be end date.
   * If this field is null, the end date is computed based on the semi-annual roll convention.
   * @return the optional value of the property, not null
   */
  public Optional<LocalDate> getEndDate() {
    return Optional.ofNullable(endDate);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the date order rules.
   * <p>
   * This is used to ensure that the dates in the curve are in order.
   * If not specified, this will default to {@link CurveNodeDateOrder#DEFAULT}.
   * @return the value of the property, not null
   */
  @Override
  public CurveNodeDateOrder getDateOrder() {
    return dateOrder;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the market quote convention.
   * <p>
   * The CDS is quoted in par spread, points upfront or quoted spread.
   * See {@link CdsQuoteConvention} for detail.
   * @return the value of the property, not null
   */
  public CdsQuoteConvention getQuoteConvention() {
    return quoteConvention;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the fixed coupon rate.
   * <p>
   * This must be represented in decimal form.
   * @return the optional value of the property, not null
   */
  public OptionalDouble getFixedRate() {
    return fixedRate != null ? OptionalDouble.of(fixedRate) : OptionalDouble.empty();
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
      CdsCurveNode other = (CdsCurveNode) obj;
      return JodaBeanUtils.equal(template, other.template) &&
          JodaBeanUtils.equal(observableId, other.observableId) &&
          JodaBeanUtils.equal(label, other.label) &&
          JodaBeanUtils.equal(legalEntityId, other.legalEntityId) &&
          JodaBeanUtils.equal(endDate, other.endDate) &&
          JodaBeanUtils.equal(dateOrder, other.dateOrder) &&
          JodaBeanUtils.equal(quoteConvention, other.quoteConvention) &&
          JodaBeanUtils.equal(fixedRate, other.fixedRate);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(template);
    hash = hash * 31 + JodaBeanUtils.hashCode(observableId);
    hash = hash * 31 + JodaBeanUtils.hashCode(label);
    hash = hash * 31 + JodaBeanUtils.hashCode(legalEntityId);
    hash = hash * 31 + JodaBeanUtils.hashCode(endDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(dateOrder);
    hash = hash * 31 + JodaBeanUtils.hashCode(quoteConvention);
    hash = hash * 31 + JodaBeanUtils.hashCode(fixedRate);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(288);
    buf.append("CdsCurveNode{");
    buf.append("template").append('=').append(template).append(',').append(' ');
    buf.append("observableId").append('=').append(observableId).append(',').append(' ');
    buf.append("label").append('=').append(label).append(',').append(' ');
    buf.append("legalEntityId").append('=').append(legalEntityId).append(',').append(' ');
    buf.append("endDate").append('=').append(endDate).append(',').append(' ');
    buf.append("dateOrder").append('=').append(dateOrder).append(',').append(' ');
    buf.append("quoteConvention").append('=').append(quoteConvention).append(',').append(' ');
    buf.append("fixedRate").append('=').append(JodaBeanUtils.toString(fixedRate));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CdsCurveNode}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code template} property.
     */
    private final MetaProperty<CdsTemplate> template = DirectMetaProperty.ofImmutable(
        this, "template", CdsCurveNode.class, CdsTemplate.class);
    /**
     * The meta-property for the {@code observableId} property.
     */
    private final MetaProperty<ObservableId> observableId = DirectMetaProperty.ofImmutable(
        this, "observableId", CdsCurveNode.class, ObservableId.class);
    /**
     * The meta-property for the {@code label} property.
     */
    private final MetaProperty<String> label = DirectMetaProperty.ofImmutable(
        this, "label", CdsCurveNode.class, String.class);
    /**
     * The meta-property for the {@code legalEntityId} property.
     */
    private final MetaProperty<StandardId> legalEntityId = DirectMetaProperty.ofImmutable(
        this, "legalEntityId", CdsCurveNode.class, StandardId.class);
    /**
     * The meta-property for the {@code endDate} property.
     */
    private final MetaProperty<LocalDate> endDate = DirectMetaProperty.ofImmutable(
        this, "endDate", CdsCurveNode.class, LocalDate.class);
    /**
     * The meta-property for the {@code dateOrder} property.
     */
    private final MetaProperty<CurveNodeDateOrder> dateOrder = DirectMetaProperty.ofImmutable(
        this, "dateOrder", CdsCurveNode.class, CurveNodeDateOrder.class);
    /**
     * The meta-property for the {@code quoteConvention} property.
     */
    private final MetaProperty<CdsQuoteConvention> quoteConvention = DirectMetaProperty.ofImmutable(
        this, "quoteConvention", CdsCurveNode.class, CdsQuoteConvention.class);
    /**
     * The meta-property for the {@code fixedRate} property.
     */
    private final MetaProperty<Double> fixedRate = DirectMetaProperty.ofImmutable(
        this, "fixedRate", CdsCurveNode.class, Double.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "template",
        "observableId",
        "label",
        "legalEntityId",
        "endDate",
        "dateOrder",
        "quoteConvention",
        "fixedRate");

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
        case -518800962:  // observableId
          return observableId;
        case 102727412:  // label
          return label;
        case 866287159:  // legalEntityId
          return legalEntityId;
        case -1607727319:  // endDate
          return endDate;
        case -263699392:  // dateOrder
          return dateOrder;
        case 2049149709:  // quoteConvention
          return quoteConvention;
        case 747425396:  // fixedRate
          return fixedRate;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public CdsCurveNode.Builder builder() {
      return new CdsCurveNode.Builder();
    }

    @Override
    public Class<? extends CdsCurveNode> beanType() {
      return CdsCurveNode.class;
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
    public MetaProperty<CdsTemplate> template() {
      return template;
    }

    /**
     * The meta-property for the {@code observableId} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ObservableId> observableId() {
      return observableId;
    }

    /**
     * The meta-property for the {@code label} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> label() {
      return label;
    }

    /**
     * The meta-property for the {@code legalEntityId} property.
     * @return the meta-property, not null
     */
    public MetaProperty<StandardId> legalEntityId() {
      return legalEntityId;
    }

    /**
     * The meta-property for the {@code endDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> endDate() {
      return endDate;
    }

    /**
     * The meta-property for the {@code dateOrder} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveNodeDateOrder> dateOrder() {
      return dateOrder;
    }

    /**
     * The meta-property for the {@code quoteConvention} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CdsQuoteConvention> quoteConvention() {
      return quoteConvention;
    }

    /**
     * The meta-property for the {@code fixedRate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> fixedRate() {
      return fixedRate;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1321546630:  // template
          return ((CdsCurveNode) bean).getTemplate();
        case -518800962:  // observableId
          return ((CdsCurveNode) bean).getObservableId();
        case 102727412:  // label
          return ((CdsCurveNode) bean).getLabel();
        case 866287159:  // legalEntityId
          return ((CdsCurveNode) bean).getLegalEntityId();
        case -1607727319:  // endDate
          return ((CdsCurveNode) bean).endDate;
        case -263699392:  // dateOrder
          return ((CdsCurveNode) bean).getDateOrder();
        case 2049149709:  // quoteConvention
          return ((CdsCurveNode) bean).getQuoteConvention();
        case 747425396:  // fixedRate
          return ((CdsCurveNode) bean).fixedRate;
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
   * The bean-builder for {@code CdsCurveNode}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<CdsCurveNode> {

    private CdsTemplate template;
    private ObservableId observableId;
    private String label;
    private StandardId legalEntityId;
    private LocalDate endDate;
    private CurveNodeDateOrder dateOrder;
    private CdsQuoteConvention quoteConvention;
    private Double fixedRate;

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
    private Builder(CdsCurveNode beanToCopy) {
      this.template = beanToCopy.getTemplate();
      this.observableId = beanToCopy.getObservableId();
      this.label = beanToCopy.getLabel();
      this.legalEntityId = beanToCopy.getLegalEntityId();
      this.endDate = beanToCopy.endDate;
      this.dateOrder = beanToCopy.getDateOrder();
      this.quoteConvention = beanToCopy.getQuoteConvention();
      this.fixedRate = beanToCopy.fixedRate;
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1321546630:  // template
          return template;
        case -518800962:  // observableId
          return observableId;
        case 102727412:  // label
          return label;
        case 866287159:  // legalEntityId
          return legalEntityId;
        case -1607727319:  // endDate
          return endDate;
        case -263699392:  // dateOrder
          return dateOrder;
        case 2049149709:  // quoteConvention
          return quoteConvention;
        case 747425396:  // fixedRate
          return fixedRate;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1321546630:  // template
          this.template = (CdsTemplate) newValue;
          break;
        case -518800962:  // observableId
          this.observableId = (ObservableId) newValue;
          break;
        case 102727412:  // label
          this.label = (String) newValue;
          break;
        case 866287159:  // legalEntityId
          this.legalEntityId = (StandardId) newValue;
          break;
        case -1607727319:  // endDate
          this.endDate = (LocalDate) newValue;
          break;
        case -263699392:  // dateOrder
          this.dateOrder = (CurveNodeDateOrder) newValue;
          break;
        case 2049149709:  // quoteConvention
          this.quoteConvention = (CdsQuoteConvention) newValue;
          break;
        case 747425396:  // fixedRate
          this.fixedRate = (Double) newValue;
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
    public CdsCurveNode build() {
      preBuild(this);
      return new CdsCurveNode(
          template,
          observableId,
          label,
          legalEntityId,
          endDate,
          dateOrder,
          quoteConvention,
          fixedRate);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the template for the CDS associated with this node.
     * @param template  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder template(CdsTemplate template) {
      JodaBeanUtils.notNull(template, "template");
      this.template = template;
      return this;
    }

    /**
     * Sets the identifier of the market data value that provides the quoted value.
     * @param observableId  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder observableId(ObservableId observableId) {
      JodaBeanUtils.notNull(observableId, "observableId");
      this.observableId = observableId;
      return this;
    }

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
     * Sets the legal entity identifier.
     * <p>
     * This identifier is used for the reference legal entity of the CDS.
     * @param legalEntityId  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder legalEntityId(StandardId legalEntityId) {
      JodaBeanUtils.notNull(legalEntityId, "legalEntityId");
      this.legalEntityId = legalEntityId;
      return this;
    }

    /**
     * Sets the node date.
     * <p>
     * The node date must be end date.
     * If this field is null, the end date is computed based on the semi-annual roll convention.
     * @param endDate  the new value
     * @return this, for chaining, not null
     */
    public Builder endDate(LocalDate endDate) {
      this.endDate = endDate;
      return this;
    }

    /**
     * Sets the date order rules.
     * <p>
     * This is used to ensure that the dates in the curve are in order.
     * If not specified, this will default to {@link CurveNodeDateOrder#DEFAULT}.
     * @param dateOrder  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dateOrder(CurveNodeDateOrder dateOrder) {
      JodaBeanUtils.notNull(dateOrder, "dateOrder");
      this.dateOrder = dateOrder;
      return this;
    }

    /**
     * Sets the market quote convention.
     * <p>
     * The CDS is quoted in par spread, points upfront or quoted spread.
     * See {@link CdsQuoteConvention} for detail.
     * @param quoteConvention  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder quoteConvention(CdsQuoteConvention quoteConvention) {
      JodaBeanUtils.notNull(quoteConvention, "quoteConvention");
      this.quoteConvention = quoteConvention;
      return this;
    }

    /**
     * Sets the fixed coupon rate.
     * <p>
     * This must be represented in decimal form.
     * @param fixedRate  the new value
     * @return this, for chaining, not null
     */
    public Builder fixedRate(Double fixedRate) {
      this.fixedRate = fixedRate;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(288);
      buf.append("CdsCurveNode.Builder{");
      buf.append("template").append('=').append(JodaBeanUtils.toString(template)).append(',').append(' ');
      buf.append("observableId").append('=').append(JodaBeanUtils.toString(observableId)).append(',').append(' ');
      buf.append("label").append('=').append(JodaBeanUtils.toString(label)).append(',').append(' ');
      buf.append("legalEntityId").append('=').append(JodaBeanUtils.toString(legalEntityId)).append(',').append(' ');
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(endDate)).append(',').append(' ');
      buf.append("dateOrder").append('=').append(JodaBeanUtils.toString(dateOrder)).append(',').append(' ');
      buf.append("quoteConvention").append('=').append(JodaBeanUtils.toString(quoteConvention)).append(',').append(' ');
      buf.append("fixedRate").append('=').append(JodaBeanUtils.toString(fixedRate));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
