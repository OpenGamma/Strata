Strata releases
===============

The release process for Strata is as follows:

1. Ensure all required changes have been merged

1. Check out the master branch and ensure there are no local changes

1. If necessary, change the version number that is to be released next by changing the snapshot version: 
`mvn versions:set -DgenerateBackupPoms=false -DartifactId=* -DgroupId=com.opengamma.strata`

1. Update the version in README.md, commit and push

1. Add and push a tag named `release`: 
`git push origin HEAD:refs/tags/release`

1. CI will automatically detect the new tag and perform a release using `mvn release`.

1. Update docs website

1. Add Javadoc to docs website. Build on **Java 11**, not later or earlier versions: 
`mvn package -Ddist -DskipTests -DadditionalJOption=--no-module-directories`
Delete the old Javadoc, copy in new Javadoc.

1. Add forum post

1. Party!
