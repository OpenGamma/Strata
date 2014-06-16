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
public class ViewInputsDeserializer {

  private final InputStream _inputStream;

  /**
   * Create a deserializer for the specified input stream.
   *
   * @param inputStream the stream to create a deserializer for
   */
  public ViewInputsDeserializer(InputStream inputStream) {
    _inputStream = ArgumentChecker.notNull(inputStream, "inputStream");
  }

  /**
   * Deserialize the view inputs from the input stream.
   */
  public ViewInputs deserialize() {

    // Configure view from file
    FudgeContext ctx = OpenGammaFudgeContext.getInstance();
    FudgeXMLStreamReader reader = new FudgeXMLStreamReader(ctx, new InputStreamReader(_inputStream));
    FudgeMsgReader fudgeMsgReader = new FudgeMsgReader(reader);
    FudgeDeserializer deserializer = new FudgeDeserializer(ctx);
    return deserializer.fudgeMsgToObject(ViewInputs.class, fudgeMsgReader.nextMessage());
  }
}
