/*
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableConstructor;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.tuple.Pair;

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
   * Attribute used to store the exception message.
   */
  public static final String EXCEPTION_MESSAGE_ATTRIBUTE = FailureAttributeKeys.EXCEPTION_MESSAGE;
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
   * The attributes associated with this failure.
   * Attributes can contain additional information about the failure. For example, a line number in a file or the ID of a trade.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<String, String> attributes;
  /**
   * Stack trace where the failure occurred.
   * If the failure was caused by an {@code Exception} its stack trace is used, otherwise it's the
   * location where the failure was created.
   */
  @PropertyDefinition(validate = "notNull")
  private final String stackTrace;
  /**
   * The type of the throwable that caused the failure, not present if it wasn't caused by a throwable.
   */
  @PropertyDefinition(get = "optional")
  private final Class<? extends Throwable> causeType;

  //-------------------------------------------------------------------------
  /**
   * Obtains a failure from a reason and message.
   * <p>
   * The message is produced using a template that contains zero to many "{}" or "{abc}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If the placeholder has a name, its value is added to the attributes map with the name as a key.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See {@link Messages#formatWithAttributes(String, Object...)} for more details.
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
    Pair<String, Map<String, String>> msg = Messages.formatWithAttributes(message, messageArgs);
    return of(reason, msg.getFirst(), msg.getSecond());
  }

  /**
   * Obtains a failure from a reason and message.
   * <p>
   * The failure will still have a stack trace, but the cause type will not be present.
   *
   * @param skipFrames  the number of caller frames to skip, not including this one
   * @param reason  the reason
   * @param message  the failure message, not empty
   * @param messageArgs  the arguments for the message
   * @return the failure
   */
  static FailureItem ofAutoStackTrace(int skipFrames, FailureReason reason, String message, Object... messageArgs) {
    ArgChecker.notNull(reason, "reason");
    ArgChecker.notEmpty(message, "message");
    Pair<String, Map<String, String>> messageArgPair = Messages.formatWithAttributes(message, messageArgs);
    String stackTrace = localGetStackTraceAsString(messageArgPair.getFirst(), skipFrames);
    return new FailureItem(reason, messageArgPair.getFirst(), messageArgPair.getSecond(), stackTrace, null);
  }

  /**
   * Obtains a failure from a reason and message.
   * <p>
   * The failure will still have a stack trace, but the cause type will not be present.
   *
   * @param reason  the reason
   * @param message  the failure message, not empty
   * @param attributes the attributes associated with this failure
   * @return the failure
   */
  private static FailureItem of(FailureReason reason, String message, Map<String, String> attributes) {
    ArgChecker.notNull(reason, "reason");
    ArgChecker.notEmpty(message, "message");
    String stackTrace = localGetStackTraceAsString(message, 1);
    return new FailureItem(reason, message, attributes, stackTrace, null);
  }

  private static String localGetStackTraceAsString(String message, int skipFrames) {
    StringBuilder builder = new StringBuilder();
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    // simulate full stack trace, pretending this class is a Throwable subclass
    builder.append(FAILURE_EXCEPTION).append(message).append("\n");
    // drop the first few frames because they are part of the immediate calling code
    for (int i = skipFrames + 3; i < stackTrace.length; i++) {
      builder.append("\tat ").append(stackTrace[i]).append("\n");
    }
    return builder.toString();
  }

  /**
   * Obtains a failure from a reason and exception.
   * <p>
   * This recognizes and handles {@link FailureItemProvider} exceptions.
   * 
   * @param reason  the reason
   * @param cause  the cause
   * @return the failure
   */
  public static FailureItem of(FailureReason reason, Throwable cause) {
    ArgChecker.notNull(reason, "reason");
    ArgChecker.notNull(cause, "cause");
    String causeMessage = extractCauseMessage(cause);
    return FailureItem.of(reason, cause, causeMessage);
  }

  /**
   * Obtains a failure from a reason, throwable and message.
   * <p>
   * The message is produced using a template that contains zero to many "{}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See {@link Messages#formatWithAttributes(String, Object...)} for more details.
   * <p>
   * It can be useful to capture the underlying exception message. This should be achieved by adding
   * ': {exceptionMessage}' to the template and 'cause.toString()' or 'cause.getMessage()' to the arguments.
   * <p>
   * This recognizes and handles {@link FailureItemProvider} exceptions.
   * 
   * @param reason  the reason
   * @param cause  the cause
   * @param messageTemplate  a message explaining the failure, not empty, uses "{}" for inserting {@code messageArgs}
   * @param messageArgs  the arguments for the message
   * @return the failure
   */
  public static FailureItem of(FailureReason reason, Throwable cause, String messageTemplate, Object... messageArgs) {
    ArgChecker.notNull(reason, "reason");
    ArgChecker.notNull(cause, "cause");
    if (cause instanceof FailureItemProvider) {
      return ofWrappedFailureItem(((FailureItemProvider) cause).getFailureItem(), reason, messageTemplate, messageArgs);
    }
    Pair<String, Map<String, String>> msg = Messages.formatWithAttributes(messageTemplate, messageArgs);
    String stackTrace = Throwables.getStackTraceAsString(cause).replace(System.lineSeparator(), "\n");
    FailureItem base = new FailureItem(reason, msg.getFirst(), msg.getSecond(), stackTrace, cause.getClass());
    String causeMessage = extractCauseMessage(cause);
    if (!base.getAttributes().containsKey(EXCEPTION_MESSAGE_ATTRIBUTE) && !Strings.isNullOrEmpty(causeMessage)) {
      return base.withAttribute(EXCEPTION_MESSAGE_ATTRIBUTE, causeMessage);
    }
    return base;
  }

  // extracts the cause message
  private static String extractCauseMessage(Throwable cause) {
    String causeMessage = cause.getMessage();
    return Strings.isNullOrEmpty(causeMessage) ? cause.getClass().getSimpleName() : causeMessage;
  }

  // handles a cause that contains a FailureItem
  private static FailureItem ofWrappedFailureItem(
      FailureItem underlying,
      FailureReason reason,
      String messageTemplate,
      Object... messageArgs) {

    // strip trailing 'exceptionMessage'
    if (messageTemplate.endsWith("{exceptionMessage}")) {
      String adjustedTemplate = messageTemplate.substring(0, messageTemplate.length() - 18).trim();
      adjustedTemplate = adjustedTemplate.endsWith(":") ? adjustedTemplate.substring(0, adjustedTemplate.length() - 1) : adjustedTemplate;
      Object[] adjustedArgs = messageArgs.length >= 1 ? Arrays.copyOfRange(messageArgs, 0, messageArgs.length - 1) : messageArgs;
      return ofWrappedFailureItem(underlying, reason, adjustedTemplate, adjustedArgs);
    }

    // format and combine message
    Pair<String, Map<String, String>> msgPair = Messages.formatWithAttributes(messageTemplate, messageArgs);
    String baseMsg = msgPair.getFirst() + ": ";
    Map<String, String> baseAttrs = msgPair.getSecond();
    String combinedMsg = baseMsg + underlying.message;

    // combine attributes
    String underlyingLocation = underlying.attributes.getOrDefault(FailureAttributeKeys.TEMPLATE_LOCATION, "");
    String baseLocation = baseAttrs.getOrDefault(FailureAttributeKeys.TEMPLATE_LOCATION, "");
    Map<String, String> combinedAttrs = new LinkedHashMap<>(baseAttrs);
    combinedAttrs.remove(FailureAttributeKeys.TEMPLATE_LOCATION);
    for (Entry<String, String> entry : underlying.attributes.entrySet()) {
      String key = entry.getKey();
      if (!FailureAttributeKeys.TEMPLATE_LOCATION.equals(key)) {
        String adjKey = key;
        int count = 1;
        while (combinedAttrs.containsKey(adjKey)) {
          adjKey = key + (count++);
        }
        if (!adjKey.equals(key)) {
          underlyingLocation = underlyingLocation.replace(key, adjKey);
        }
        combinedAttrs.put(adjKey, entry.getValue());
      }
    }

    // adjust location
    String mergedLocation = Messages.mergeTemplateLocations(baseLocation, underlyingLocation, baseMsg.length());
    if (!mergedLocation.isEmpty()) {
      combinedAttrs.put(FailureAttributeKeys.TEMPLATE_LOCATION, mergedLocation);
    }
    return new FailureItem(underlying.reason, combinedMsg, combinedAttrs, underlying.stackTrace, underlying.causeType);
  }

  /**
   * Creates a failure item from the throwable.
   * <p>
   * This recognizes and handles {@link FailureItemProvider} exceptions.
   *
   * @param th  the throwable to be processed
   * @return the failure item
   */
  public static FailureItem from(Throwable th) {
    try {
      throw th;
    } catch (Throwable ex) {
      if (ex instanceof FailureItemProvider) {
        return ((FailureItemProvider) ex).getFailureItem();
      }
      return of(FailureReason.ERROR, ex);
    }
  }

  //-------------------------------------------------------------------------
  @ImmutableConstructor
  private FailureItem(
      FailureReason reason,
      String message,
      Map<String, String> attributes,
      String stackTrace,
      Class<? extends Throwable> causeType) {
    this.attributes = ImmutableMap.copyOf(attributes);
    JodaBeanUtils.notNull(reason, "reason");
    JodaBeanUtils.notEmpty(message, "message");
    JodaBeanUtils.notNull(stackTrace, "stackTrace");
    this.reason = reason;
    this.message = message;
    this.stackTrace = INTERNER.intern(stackTrace);
    this.causeType = causeType;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the message template that was used to create the message.
   * <p>
   * This method derives the template from 'templateLocation' in the attributes.
   * This only works if the template location correctly matches the message.
   * 
   * @return the message template
   */
  public String getMessageTemplate() {
    return Messages.recreateTemplate(message, attributes.get(FailureAttributeKeys.TEMPLATE_LOCATION));
  }

  /**
   * Returns an instance with the specified attribute added.
   * <p>
   * If the attribute map of this instance has the specified key, the value is replaced.
   * 
   * @param key  the key to add
   * @param value  the value to add
   * @return the new failure item
   */
  public FailureItem withAttribute(String key, String value) {
    Map<String, String> attributes = new HashMap<>(this.attributes);
    attributes.put(key, value);
    return new FailureItem(reason, message, attributes, stackTrace, causeType);
  }

  /**
   * Returns an instance with the specified attributes added.
   * <p>
   * If the attribute map of this instance has any of the new attribute keys, the values are replaced.
   *
   * @param attributes  the new attributes to add
   * @return the new failure item
   */
  public FailureItem withAttributes(Map<String, String> attributes) {
    Map<String, String> newAttributes = new HashMap<>(this.attributes);
    newAttributes.putAll(attributes);
    return new FailureItem(reason, message, newAttributes, stackTrace, causeType);
  }

  /**
   * Processes the failure item by applying a function that alters the message.
   * <p>
   * This operation allows wrapping a failure message with additional information that may have not been available
   * to the code that created the original failure.
   *
   * @param function  the function to transform the message with
   * @return the transformed instance
   */
  public FailureItem mapMessage(Function<String, String> function) {
    return new FailureItem(reason, function.apply(message), attributes, stackTrace, causeType);
  }

  /**
   * Returns a string summary of the stack trace.
   * 
   * @return the summary stack trace
   */
  public Optional<String> summarizeStackTrace() {
    if (stackTrace.startsWith(FAILURE_EXCEPTION)) {
      return Optional.empty();
    }
    int endLine = stackTrace.indexOf("\n");
    String firstLine = endLine < 0 ? stackTrace : stackTrace.substring(0, endLine);
    firstLine = firstLine.endsWith(": " + message) ? firstLine.substring(0, firstLine.length() - message.length() - 2) : firstLine;
    return Optional.of(firstLine);
  }

  /**
   * Returns a string summary of the failure, as a single line excluding the stack trace.
   * 
   * @return the summary string
   */
  @Override
  public String toString() {
    return reason + ": " + message + summarizeStackTrace().map(str -> ": " + str).orElse("");
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code FailureItem}.
   * @return the meta-bean, not null
   */
  public static FailureItem.Meta meta() {
    return FailureItem.Meta.INSTANCE;
  }

  static {
    MetaBean.register(FailureItem.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public FailureItem.Meta metaBean() {
    return FailureItem.Meta.INSTANCE;
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
   * Gets the attributes associated with this failure.
   * Attributes can contain additional information about the failure. For example, a line number in a file or the ID of a trade.
   * @return the value of the property, not null
   */
  public ImmutableMap<String, String> getAttributes() {
    return attributes;
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
   * Gets the type of the throwable that caused the failure, not present if it wasn't caused by a throwable.
   * @return the optional value of the property, not null
   */
  public Optional<Class<? extends Throwable>> getCauseType() {
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
          JodaBeanUtils.equal(attributes, other.attributes) &&
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
    hash = hash * 31 + JodaBeanUtils.hashCode(attributes);
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
     * The meta-property for the {@code attributes} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<String, String>> attributes = DirectMetaProperty.ofImmutable(
        this, "attributes", FailureItem.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code stackTrace} property.
     */
    private final MetaProperty<String> stackTrace = DirectMetaProperty.ofImmutable(
        this, "stackTrace", FailureItem.class, String.class);
    /**
     * The meta-property for the {@code causeType} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<Class<? extends Throwable>> causeType = DirectMetaProperty.ofImmutable(
        this, "causeType", FailureItem.class, (Class) Class.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "reason",
        "message",
        "attributes",
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
        case 405645655:  // attributes
          return attributes;
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
     * The meta-property for the {@code attributes} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<String, String>> attributes() {
      return attributes;
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
    public MetaProperty<Class<? extends Throwable>> causeType() {
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
        case 405645655:  // attributes
          return ((FailureItem) bean).getAttributes();
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
    private Map<String, String> attributes = ImmutableMap.of();
    private String stackTrace;
    private Class<? extends Throwable> causeType;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -934964668:  // reason
          return reason;
        case 954925063:  // message
          return message;
        case 405645655:  // attributes
          return attributes;
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
        case 405645655:  // attributes
          this.attributes = (Map<String, String>) newValue;
          break;
        case 2026279837:  // stackTrace
          this.stackTrace = (String) newValue;
          break;
        case -1443456189:  // causeType
          this.causeType = (Class<? extends Throwable>) newValue;
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
          attributes,
          stackTrace,
          causeType);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("FailureItem.Builder{");
      buf.append("reason").append('=').append(JodaBeanUtils.toString(reason)).append(',').append(' ');
      buf.append("message").append('=').append(JodaBeanUtils.toString(message)).append(',').append(' ');
      buf.append("attributes").append('=').append(JodaBeanUtils.toString(attributes)).append(',').append(' ');
      buf.append("stackTrace").append('=').append(JodaBeanUtils.toString(stackTrace)).append(',').append(' ');
      buf.append("causeType").append('=').append(JodaBeanUtils.toString(causeType));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
