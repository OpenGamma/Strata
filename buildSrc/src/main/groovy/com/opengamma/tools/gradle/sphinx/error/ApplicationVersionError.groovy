/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.tools.gradle.sphinx.error

import groovy.transform.Immutable

@Immutable
class ApplicationVersionError implements EnvironmentError
{
	String application
	String foundVersion
	String requiredVersion

	public String getMessage()
	{
		"Require version ${-> requiredVersion} of ${-> application}, found ${-> foundVersion}"
	}

	@Override
	String getHelp()
	{
		"Update out-dated applications with OS package manager, or use pip install to update Python packages"
	}
}
