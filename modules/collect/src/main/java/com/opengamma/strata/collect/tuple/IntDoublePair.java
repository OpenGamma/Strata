/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.tuple;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;
import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.ArgChecker;

/**
 * An immutable pair consisting of an {@code int} and {@code double}.
 * <p>
 * This class is similar to {@link Pair} but is based on two primitive elements.
 * <p>
 * This class is immutable and thread-safe.
 */
@BeanDefinition(builderScope = "private")
public final class IntDoublePair
    implements ImmutableBean, Tuple, Comparable<IntDoublePair>, Serializable {

  /**
   * The first element in this pair.
   */
  @PropertyDefinition
  private final int first;
  /**
   * The second element in this pair.
   */
  @PropertyDefinition
  private final double second;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from an {@code int} and a {@code double}.
   * 
   * @param first  the first element
   * @param second  the second element
   * @return a pair formed from the two parameters
   */
  public static IntDoublePair of(int first, double second) {
    return new IntDoublePair(first, second);
  }

  /**
   * Obtains an instance from a {@code Pair}.
   * 
   * @param pair  the pair to convert
   * @return a pair formed by extracting values from the pair
   */
  public static IntDoublePair ofPair(Pair<Integer, Double> pair) {
    ArgChecker.notNull(pair, "pair");
    return new IntDoublePair(pair.getFirst(), pair.getSecond());
  }

  //-------------------------------------------------------------------------
  /**
   * Parses an {@code IntDoublePair} from the standard string format.
   * <p>
   * The standard format is '[$first, $second]'. Spaces around the values are trimmed.
   * 
   * @param pairStr  the text to parse
   * @return the parsed pair
   * @throws IllegalArgumentException if the pair cannot be parsed
   */
  @FromString
  public static IntDoublePair parse(String pairStr) {
    ArgChecker.notNull(pairStr, "pairStr");
    if (pairStr.length() < 5) {
      throw new IllegalArgumentException("Invalid pair format, too short: " + pairStr);
    }
    if (pairStr.charAt(0) != '[') {
      throw new IllegalArgumentException("Invalid pair format, must start with [: " + pairStr);
    }
    if (pairStr.charAt(pairStr.length() - 1) != ']') {
      throw new IllegalArgumentException("Invalid pair format, must end with ]: " + pairStr);
    }
    String content = pairStr.substring(1, pairStr.length() - 1);
    List<String> split = Splitter.on(',').trimResults().splitToList(content);
    if (split.size() != 2) {
      throw new IllegalArgumentException("Invalid pair format, must have two values: " + pairStr);
    }
    int first = Integer.parseInt(split.get(0));
    double second = Double.parseDouble(split.get(1));
    return new IntDoublePair(first, second);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the number of elements held by this pair.
   * 
   * @return size 2
   */
  @Override
  public int size() {
    return 2;
  }

  /**
   * Gets the elements from this pair as a list.
   * <p>
   * The list returns each element in the pair in order.
   * 
   * @return the elements as an immutable list
   */
  @Override
  public ImmutableList<Object> elements() {
    return ImmutableList.of(first, second);
  }

  //-------------------------------------------------------------------------
  /**
   * Converts this pair to an object-based {@code Pair}.
   * 
   * @return the object-based pair
   */
  public Pair<Integer, Double> toPair() {
    return Pair.of(first, second);
  }

  //-------------------------------------------------------------------------
  /**
   * Compares the pair based on the first element followed by the second element.
   * <p>
   * This compares the first elements, then the second elements.
   * 
   * @param other  the other pair
   * @return negative if this is less, zero if equal, positive if greater
   */
  @Override
  public int compareTo(IntDoublePair other) {
    int cmp = Integer.compare(first, other.first);
    if (cmp == 0) {
      cmp = Double.compare(second, other.second);
    }
    return cmp;
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof IntDoublePair) {
      IntDoublePair other = (IntDoublePair) obj;
      return this.first == other.first && JodaBeanUtils.equal(second, other.second);
    }
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    // see Map.Entry API specification
    long s = Double.doubleToLongBits(second);
    return first ^ ((int) (s ^ (s >>> 32)));
  }

  /**
   * Gets the pair using a standard string format.
   * <p>
   * The standard format is '[$first, $second]'. Spaces around the values are trimmed.
   * 
   * @return the pair as a string
   */
  @Override
  @ToString
  public String toString() {
    return new StringBuilder()
        .append('[')
        .append(first)
        .append(", ")
        .append(second)
        .append(']')
        .toString();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IntDoublePair}.
   * @return the meta-bean, not null
   */
  public static IntDoublePair.Meta meta() {
    return IntDoublePair.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(IntDoublePair.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private IntDoublePair(
      int first,
      double second) {
    this.first = first;
    this.second = second;
  }

  @Override
  public IntDoublePair.Meta metaBean() {
    return IntDoublePair.Meta.INSTANCE;
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
   * Gets the first element in this pair.
   * @return the value of the property
   */
  public int getFirst() {
    return first;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the second element in this pair.
   * @return the value of the property
   */
  public double getSecond() {
    return second;
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IntDoublePair}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code first} property.
     */
    private final MetaProperty<Integer> first = DirectMetaProperty.ofImmutable(
        this, "first", IntDoublePair.class, Integer.TYPE);
    /**
     * The meta-property for the {@code second} property.
     */
    private final MetaProperty<Double> second = DirectMetaProperty.ofImmutable(
        this, "second", IntDoublePair.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "first",
        "second");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 97440432:  // first
          return first;
        case -906279820:  // second
          return second;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends IntDoublePair> builder() {
      return new IntDoublePair.Builder();
    }

    @Override
    public Class<? extends IntDoublePair> beanType() {
      return IntDoublePair.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code first} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Integer> first() {
      return first;
    }

    /**
     * The meta-property for the {@code second} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> second() {
      return second;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 97440432:  // first
          return ((IntDoublePair) bean).getFirst();
        case -906279820:  // second
          return ((IntDoublePair) bean).getSecond();
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
   * The bean-builder for {@code IntDoublePair}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<IntDoublePair> {

    private int first;
    private double second;

    /**
     * Restricted constructor.
     */
    private Builder() {
      super(meta());
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 97440432:  // first
          return first;
        case -906279820:  // second
          return second;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 97440432:  // first
          this.first = (Integer) newValue;
          break;
        case -906279820:  // second
          this.second = (Double) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public IntDoublePair build() {
      return new IntDoublePair(
          first,
          second);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("IntDoublePair.Builder{");
      buf.append("first").append('=').append(JodaBeanUtils.toString(first)).append(',').append(' ');
      buf.append("second").append('=').append(JodaBeanUtils.toString(second));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
