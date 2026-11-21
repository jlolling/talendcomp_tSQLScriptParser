# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Talend component tSQLScriptParser: splits a SQL script into single statements for iteration.
User-facing behaviour and parameters are described in `README.md` and the manuals in `doc/`.

## Build

```bash
mvn clean package                      # test, assemble component(s), copy them to ${custom_component_folder}
mvn test -Dtest=SomeTest#someMethod   # single test class or method
```

Java 8; `cimt-talendcomp-maven-plugin` 3.1. `custom_component_folder` is defined in `~/.m2/settings.xml`.

## Structure

- `src/main/java` (`de.jlo.talendcomp.dbtools`): the runtime library. Generated job code calls these classes by fully qualified name.
- `src/main/components/<Name>/`: `<Name>_java.xml` (descriptor), `*.javajet` (code-generation templates), `<Name>_messages.properties` (labels, `LONG_NAME` required) and the icon. A parameter change usually touches all three plus the library.
- Generated at `package`, do not hand-edit: the `<IMPORTS>` block and HEADER `VERSION`/`RELEASE_DATE` in `_java.xml`, and jars in the component folders. Bump the version in `pom.xml`.
