/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import java.io.InputStream;
import java.io.InputStreamReader;

import org.fudgemsg.FudgeContext;
import org.fudgemsg.mapping.FudgeDeserializer;
import org.fudgemsg.wire.FudgeMsgReader;
import org.fudgemsg.wire.xml.FudgeXMLStreamReader;

import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.fudgemsg.OpenGammaFudgeContext;

/**
 * Responsible for deserializing a ViewInputs object from
 * an input stream. Primarily a wrapper to hide underlying
 * fudge deserialization constructs.
 */
public class ViewResultsDeserializer {

  private final InputStream _inputStream;

  /**
   * Create a deserializer for the specified input stream.
   *
   * @param inputStream the stream to create a deserializer for
   */
  public ViewResultsDeserializer(InputStream inputStream) {
    _inputStream = ArgumentChecker.notNull(inputStream, "inputStream");
  }

  /**
   * Deserialize the requested object from the input stream.
   * Generally, this will either be a {@link ViewInputs} or a
   * {@link ViewOutputs} object.
   *
   * @param clss the class for the type of object to be returned
   * @param <T> the type of the object to be returned
   * @return the deserialized object
   */
  public <T> T deserialize(Class<T> clss) {

    // Configure view from file
    FudgeContext ctx = OpenGammaFudgeContext.getInstance();
    FudgeXMLStreamReader reader = new FudgeXMLStreamReader(ctx, new InputStreamReader(_inputStream));
    FudgeMsgReader fudgeMsgReader = new FudgeMsgReader(reader);
    FudgeDeserializer deserializer = new FudgeDeserializer(ctx);
    return deserializer.fudgeMsgToObject(clss, fudgeMsgReader.nextMessage());
  }
}
