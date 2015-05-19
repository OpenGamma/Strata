/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.tools.gradle.release

import com.github.zafarkhaja.semver.Version
import spock.lang.Specification
import spock.lang.Unroll

class SnapshotVersionDeriverSpec extends Specification
{
//	@Unroll
    void "Test version deriver"(
		    String baseTagDescription,
		    String commitDescription,
		    String releaseTagTemplate,
		    Integer buildNumber,
		    String expected)
    {
        setup:
        SnapshotVersionDeriver versionDeriver = new SnapshotVersionDeriver(
		        baseTagDescription,
		        commitDescription,
		        releaseTagTemplate,
		        buildNumber)

        when:
        Version v = versionDeriver.deriveSnapshot()

	    then:
	    v == Version.valueOf(expected)

	    where:
	    baseTagDescription  | commitDescription         | releaseTagTemplate   | buildNumber | expected
	    "v1.0.0"            | "v1.0.0-10-g123abc"       | "v@version@"         | null        | "1.0.0+10.g123abc"
	    "v1.0.0"            | "v1.0.0-10-g123abc"       | "v@version@"         | 120         | "1.0.0+10.g123abc.b120"
	    "rel/test-2.2.1"    | "rel/test-2.2.1"          | "rel/test-@version@" | null        | "2.2.1"
	    "rel/test-2.2.1"    | "rel/test-2.2.1"          | "rel/test-@version@" | 201         | "2.2.1+b201"
	    "v1.0.0-beta2"      | "v1.0.0-beta2-10-g123abc" | "v@version@"         | null        | "1.0.0-beta2+10.g123abc"
	    "v1.0.0-beta2"      | "v1.0.0-beta2-10-g123abc" | "v@version@"         | 120         | "1.0.0-beta2+10.g123abc.b120"
	    "rel/test-2.2.1-M3" | "rel/test-2.2.1-M3"       | "rel/test-@version@" | null        | "2.2.1-M3"
	    "rel/test-2.2.1-M3" | "rel/test-2.2.1-M3"       | "rel/test-@version@" | 201         | "2.2.1-M3+b201"
    }
}
