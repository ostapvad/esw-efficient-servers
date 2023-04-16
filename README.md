# Prerequisites
- installed avrocpp and protoc
# Server-side, C++


## Proto
Run protoc compiler inside of *proto* folder:

```
~/serialization/src/main/proto$ protoc -I=. --cpp_out=./../cpp ./measurements.proto
```
## Avro
Run avrogencpp in /cpp:
```
~/serialization/src/main/cpp avrogencpp -i ../avro/measurements.avsc -o measurements.hh -n c
```

# Client, Java
Generate classes by running `mvn compile` in the source directory, then run the application.