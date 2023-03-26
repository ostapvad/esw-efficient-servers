### C++ Server Component

To build the C++ part of the project, install dependencies or use nix shell:

	nix-shell

Then build the project with meson:

	meson setup builddir
	meson compile -C builddir

And finally run the project:

	./builddir/src/main/cpp/server <port> <format>

for example:

    ./builddir/src/main/cpp/server 12345 json

### Java Client Component

To build the Java part of the project, install JDK 17 or higher or use nix shell:

    nix-shell

Then build the project using maven:

    mvn compile

And run it:

    mvn exec:java -Dexec.mainClass="cz.esw.serialization.App" -Dexec.args="<host> <port> <format>"

for example:

    mvn exec:java -Dexec.mainClass="cz.esw.serialization.App" -Dexec.args="localhost 12345 json"

Or use any IDE and import it as a maven project and to see how it works for JSON run `AppTest.java`.
    


