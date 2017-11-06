# Kotlin-Scala.js 

[![Build Status](https://travis-ci.org/flonso/Kotlin-Scala.js.svg?branch=master)](https://travis-ci.org/flonso/Kotlin-Scala.js)

Compile Kotlin code to JavaScript through the Scala.js compiler

## Running the compiler
```bash
sbt "run {list_of_source_files} -d {output_js_dir} -kotlin-home {kotlin_home_directory}"
```
Note : Use -Xallow-kotlin-package option to ignore the package kotlin reservation

 
## Testing

You will need Kotlin compiler version 1.1.1.

To do so, either install it from here https://kotlinlang.org/docs/tutorials/command-line.html and/or change the path
to the directory by changing the KOTLIN_HOME value in BlackBoxTest.scala

You will also need to package the scalajs source code as a library to be referenced inside BlackBoxTest.scala