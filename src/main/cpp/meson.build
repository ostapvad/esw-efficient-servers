boost_regex_dep = meson.get_compiler('cpp').find_library('boost_regex')
thread_dep = dependency('threads')
jsoncpp_dep = dependency('jsoncpp')
protobuf_dep = dependency('protobuf', required : true)
protoc = find_program('protoc', required : true)
proto_dir = join_paths(meson.source_root(), 'src/main/proto')
gen = generator(protoc, \
  output    : ['@BASENAME@.pb.cc', '@BASENAME@.pb.h'],
  arguments : ['--proto_path=' + proto_dir, '--cpp_out=@BUILD_DIR@', '@INPUT@'])
protbuf_srcs = gen.process(join_paths(proto_dir, 'measurements.proto'))

srcs = ['dataset.cpp', 'main.cpp', 'measurementinfo.cpp', 'result.cpp'] + protbuf_srcs
executable('server', srcs,
  dependencies: [thread_dep, boost_regex_dep, jsoncpp_dep,  protobuf_dep])
