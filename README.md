# Prerequisites
- C++: installed avrogencpp and protoc
# Server-side, C++
To build the C++ part of the project, install dependencies or use nix shell:

	nix-shell

Then build the project with meson:

	meson setup builddir
	meson compile -C builddir

And finally run the project:

	./builddir/src/main/cpp/server <port> <format>

for example:

    ./builddir/src/main/cpp/server 12345 json

#### Manual compiling
You can also manually generate the C++ source files with the following commands
##### 1) Proto
Run protoc compiler inside of *proto* folder:

```
~/serialization/src/main/proto$ protoc -I=. --cpp_out=./../cpp ./measurements.proto
```
##### 2) Avro
Run avrogencpp in /cpp:
```
~/serialization/src/main/cpp avrogencpp -i ../avro/measurements.avsc -o measurements.hh -n esw_avro
```
# Client, Java
Generate classes by running `mvn compile` in the source directory, then run the application.


