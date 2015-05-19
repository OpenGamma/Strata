/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.tools.gradle.sphinx

class SphinxExtension
{
	String userGuideArchiveName

    Set<String> requiredPackages =  [
            "numpy", "numpydoc", "rst2pdf", "pygments", "sphinx", "sphinx-rtd-theme"
    ].toSet()
}
