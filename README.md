# xpres-agree



AGREE is a software to create instance diagrams for STEP files. 

AGREE was developed for KTH, starting in 2011. The libraries used is therefore quite old, and some deprecated. There has been no active development the last years, but since there is still interests in maintaining it, it is now open source should any of its users have resources to continue development. 

## Build project

Project is build using Maven.

`mvn clean compile assembly:single`

There are two dependencies that are served from a local Nexus instance, [JSDAI runtime](https://www.jsdai.net/download) and *stepmodules*. Only the first one is neccessary, and can be downloaded from the link above. Stepmodules contains a set of common STEP schemas.
Both of these will be added to a public maven repository in the near future.
