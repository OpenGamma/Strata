package com.opengamma.tools.gradle.release

import com.github.zafarkhaja.semver.ParseException
import com.github.zafarkhaja.semver.UnexpectedCharacterException
import com.github.zafarkhaja.semver.Version

class ClassEnhancer
{
    public static void enhanceVersion()
    {
        final MetaMethod originalMethod = Version.metaClass.getStaticMetaMethod("valueOf", String)
        Version.metaClass.'static'.valueOf = { String input ->
            try {
                return originalMethod.invoke(null, input)
            } catch(UnexpectedCharacterException ucex)
            {
                throw new ParseException("Problem parsing Version for input String '${input}'", ucex)
            }
        }
    }
}
