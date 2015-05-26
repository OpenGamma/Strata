/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.tools.gradle.sphinx.error

public trait EnvironmentError
{
	abstract String getMessage()
	abstract String getHelp()

	public String toString()
	{
		return getMessage()
	}
}
