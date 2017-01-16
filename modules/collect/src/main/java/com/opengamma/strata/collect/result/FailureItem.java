/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;

/**
 * Details of a single failed item.
 * <p>
 * This is used in {@link Failure} and {@link FailureItems} to capture details of a single failure.
 * Details include the reason, message and stack trace.
 */
@BeanDefinition(builderScope = "private")
public final class FailureItem
    implements ImmutableBean, Serializable {

  /**
   * Header used when generating stack trace internally.
   */
  private static final String FAILURE_EXCEPTION = "com.opengamma.strata.collect.result.FailureItem: ";
  /**
   * Stack traces can take up a lot of memory if a large number of failures are stored.
   * They are often duplicated many times so interning them can save a significant amount of memory.
   */
  private static final Interner<String> INTERNER = Interners.newWeakInterner();

  /**
   * The reason associated with the failure.
   */
  @PropertyDefinition(validate = "notNull")
  private final FailureReason reason;
  /**
   * The error message associated with the failure.
   */
  @PropertyDefinition(validate = "notEmpty")
  private final String message;
  /**
   * Stack trace where the failure occurred.
   * If the failure was caused by an {@code Exception} its stack trace is used, otherwise it's the
   * location where the failure was created.
   */
  @PropertyDefinition(validate = "notNull")
  private final String stackTrace;
  /**
   * The type of the exception that caused the failure, not present if it wasn't caused by an exception.
   */
  @PropertyDefinition(get = "optional")
  private final Class<? extends Exception> causeType;

  //-------------------------------------------------------------------------
  /**
   * Obtains a failure from a reason and message.
   * <p>
   * The message is produced using a template that contains zero to many "{}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See {@link Messages#format(String, Object...)} for more details.
   * <p>
   * An exception will be created internally to obtain a stack trace.
   * The cause type will not be present in the resulting failure.
   * 
   * @param reason  the reason
   * @param message  a message explaining the failure, not empty, uses "{}" for inserting {@code messageArgs}
   * @param messageArgs  the arguments for the message
   * @return the failure
   */
  public static FailureItem of(FailureReason reason, String message, Object... messageArgs) {
    String msg = Messages.format(message, messageArgs);
    return of(reason, msg, 1);
  }

  /**
   * Obtains a failure from a reason and message.
   * <p>
   * The failure will still have a stack trace, but the cause type will not be present.
   * 
   * @param reason  the reason
   * @param message  the failure message, not empty
   * @param skipFrames  the number of caller frames to skip, not including this one
   * @return the failure
   */
  static FailureItem of(FailureReason reason, String message, int skipFrames) {
    ArgChecker.notNull(reason, "reason");
    ArgChecker.notEmpty(message, "message");
    String stackTrace = localGetStackTraceAsString(message, skipFrames);
    return new FailureItem(reason, message, stackTrace, null);
  }

  private static String localGetStackTraceAsString(String message, int skipFrames) {
    StringBuilder builder = new StringBuilder();
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    // simulate full stack trace, pretending this class is a Throwable subclass
    builder.append(FAILURE_EXCEPTION).append(message).append(System.lineSeparator());
    // drop the first few frames because they are part of the immediate calling code
    for (int i = skipFrames + 3; i < stackTrace.length; i++) {
      builder.append("\tat ").append(stackTrace[i]).append(System.lineSeparator());
    }
    return builder.toString();
  }

  /**
   * Obtains a failure from a reason and exception.
   * 
   * @param reason  the reason
   * @param cause  the cause
   * @return the failure
   */
  public static FailureItem of(FailureReason reason, Exception cause) {
    ArgChecker.notNull(reason, "reason");
    ArgChecker.notNull(cause, "cause");
    String causeMessage = cause.getMessage();
    String message = Strings.isNullOrEmpty(causeMessage) ? cause.getClass().getSimpleName() : causeMessage;
    return FailureItem.of(reason, cause, message);
  }

  /**
   * Obtains a failure from a reason, exception and message.
   * <p>
   * The message is produced using a template that contains zero to many "{}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See {@link Messages#format(String, Object...)} for more details.
   * 
   * @param reason  the reason
   * @param cause  the cause
   * @param message  a message explaining the failure, not empty, uses "{}" for inserting {@code messageArgs}
   * @param messageArgs  the arguments for the message
   * @return the failure
   */
  public static FailureItem of(FailureReason reason, Exception cause, String message, Object... messageArgs) {
    ArgChecker.notNull(reason, "reason");
    ArgChecker.notNull(cause, "cause");
    String msg = Messages.format(message, messageArgs);
    String stackTrace = Throwables.getStackTraceAsString(cause);
    return new FailureItem(reason, msg, stackTrace, cause.getClass());
  }

  //-------------------------------------------------------------------------
  @ImmutableConstructor
  private FailureItem(
      FailureReason reason,
      String message,
      String stackTrace,
      Class<? extends Exception> causeType) {
    JodaBeanUtils.notNull(reason, "reason");
    JodaBeanUtils.notEmpty(message, "message");
    JodaBeanUtils.notNull(stackTrace, "stackTrace");
    this.reason = reason;
    this.message = message;
    this.stackTrace = INTERNER.intern(stackTrace);
    this.causeType = causeType;
  }

  /**
   * Returns a string summary of the failure, as a single line excluding the stack trace.
   * 
   * @return the summary string
   */
  @Override
  public String toString() {
    if (stackTrace.startsWith(FAILURE_EXCEPTION)) {
      return reason + ": " + message;
    }
    String firstLine = stackTrace.substring(0, stackTrace.indexOf(System.lineSeparator()));
    if (firstLine.endsWith(": " + message)) {
      return reason + ": " + message + ": " + firstLine.substring(0, firstLine.length() - message.length() - 2);
    }
    return reason + ": " + message + ": " + firstLine;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FailureItem}.
   * @return the meta-bean, not null
   */
  public static FailureItem.Meta meta() {
    return FailureItem.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FailureItem.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public FailureItem.Meta metaBean() {
    return FailureItem.Meta.INSTANCE;
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
   * Gets the reason associated with the failure.
   * @return the value of the property, not null
   */
  public FailureReason getReason() {
    return reason;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the error message associated with the failure.
   * @return the value of the property, not empty
   */
  public String getMessage() {
    return message;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets stack trace where the failure occurred.
   * If the failure was caused by an {@code Exception} its stack trace is used, otherwise it's the
   * location where the failure was created.
   * @return the value of the property, not null
   */
  public String getStackTrace() {
    return stackTrace;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the type of the exception that caused the failure, not present if it wasn't caused by an exception.
   * @return the optional value of the property, not null
   */
  public Optional<Class<? extends Exception>> getCauseType() {
    return Optional.ofNullable(causeType);
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      FailureItem other = (FailureItem) obj;
      return JodaBeanUtils.equal(reason, other.reason) &&
          JodaBeanUtils.equal(message, other.message) &&
          JodaBeanUtils.equal(stackTrace, other.stackTrace) &&
          JodaBeanUtils.equal(causeType, other.causeType);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(reason);
    hash = hash * 31 + JodaBeanUtils.hashCode(message);
    hash = hash * 31 + JodaBeanUtils.hashCode(stackTrace);
    hash = hash * 31 + JodaBeanUtils.hashCode(causeType);
    return hash;
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FailureItem}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code reason} property.
     */
    private final MetaProperty<FailureReason> reason = DirectMetaProperty.ofImmutable(
        this, "reason", FailureItem.class, FailureReason.class);
    /**
     * The meta-property for the {@code message} property.
     */
    private final MetaProperty<String> message = DirectMetaProperty.ofImmutable(
        this, "message", FailureItem.class, String.class);
    /**
     * The meta-property for the {@code stackTrace} property.
     */
    private final MetaProperty<String> stackTrace = DirectMetaProperty.ofImmutable(
        this, "stackTrace", FailureItem.class, String.class);
    /**
     * The meta-property for the {@code causeType} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<Class<? extends Exception>> causeType = DirectMetaProperty.ofImmutable(
        this, "causeType", FailureItem.class, (Class) Class.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "reason",
        "message",
        "stackTrace",
        "causeType");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -934964668:  // reason
          return reason;
        case 954925063:  // message
          return message;
        case 2026279837:  // stackTrace
          return stackTrace;
        case -1443456189:  // causeType
          return causeType;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends FailureItem> builder() {
      return new FailureItem.Builder();
    }

    @Override
    public Class<? extends FailureItem> beanType() {
      return FailureItem.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code reason} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FailureReason> reason() {
      return reason;
    }

    /**
     * The meta-property for the {@code message} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> message() {
      return message;
    }

    /**
     * The meta-property for the {@code stackTrace} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> stackTrace() {
      return stackTrace;
    }

    /**
     * The meta-property for the {@code causeType} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Class<? extends Exception>> causeType() {
      return causeType;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -934964668:  // reason
          return ((FailureItem) bean).getReason();
        case 954925063:  // message
          return ((FailureItem) bean).getMessage();
        case 2026279837:  // stackTrace
          return ((FailureItem) bean).getStackTrace();
        case -1443456189:  // causeType
          return ((FailureItem) bean).causeType;
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
   * The bean-builder for {@code FailureItem}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<FailureItem> {

    private FailureReason reason;
    private String message;
    private String stackTrace;
    private Class<? extends Exception> causeType;

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
        case -934964668:  // reason
          return reason;
        case 954925063:  // message
          return message;
        case 2026279837:  // stackTrace
          return stackTrace;
        case -1443456189:  // causeType
          return causeType;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -934964668:  // reason
          this.reason = (FailureReason) newValue;
          break;
        case 954925063:  // message
          this.message = (String) newValue;
          break;
        case 2026279837:  // stackTrace
          this.stackTrace = (String) newValue;
          break;
        case -1443456189:  // causeType
          this.causeType = (Class<? extends Exception>) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public FailureItem build() {
      return new FailureItem(
          reason,
          message,
          stackTrace,
          causeType);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("FailureItem.Builder{");
      buf.append("reason").append('=').append(JodaBeanUtils.toString(reason)).append(',').append(' ');
      buf.append("message").append('=').append(JodaBeanUtils.toString(message)).append(',').append(' ');
      buf.append("stackTrace").append('=').append(JodaBeanUtils.toString(stackTrace)).append(',').append(' ');
      buf.append("causeType").append('=').append(JodaBeanUtils.toString(causeType));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
