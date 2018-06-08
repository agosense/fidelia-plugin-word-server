[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
# fidelia-plugin-word-server
## Overview
This project provides the server component for the agosense.fidelia word plugin as a standalone HTTP server that can:
* Transformation DOCX documents to XHTML
* Imports data into agosense.fidelia
* Serves the Javascript based user interface for the agosense.fidelia plugin
## Configuration
The server is configured using environment variables as explained in the following table:

Variable Name | Default | Description
---|---|---
FIDELIA_WORD_SERVER_STATIC_PATH | /static | Path where the distributable client files are located
FIDELIA_WORD_SERVER_ADDRESS | 0.0.0.0 | IP address to which the server binds
FIDELIA_WORD_SERVER_PORT | 8181 | TCP port at which the server listens
 
## Build
The build of the project requires [Maven](https://maven.apache.org/) in version `3.5.3` and a JDK Version `8`.
After cloning the project to a local folder `work`, run the following command from that folder:
```
mvn clean install
``` 
This will produce a JAR archive `server-<version>.jar` in the `target` subfolder of your `work` folder.
The server can be started by running the following command:
```
java -jar server-x.y.z.jar
``` 

## Known issues
* The used Library opensagres xdocreport for converting the word file does not support charts, so this plugin cannot import charts into agosense.fidelia
* Some docx Documents can cause problems while importing them. Loading them in MS Word and saving them again will solve the problem.

## License
This project is licensed under GPLv3, which means you can freely distribute and/or modify the source code, as long as you share your changes with us
