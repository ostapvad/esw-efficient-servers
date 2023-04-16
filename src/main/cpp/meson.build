boost_regex_dep = meson.get_compiler('cpp').find_library('boost_regex')
thread_dep = dependency('threads')
jsoncpp_dep = dependency('jsoncpp')
protobuf_dep = dependency('protobuf', required : true)
protoc = find_program('protoc', required : true)
proto_dir = join_paths(meson.source_root(), 'src/main/proto')
proto_gen = generator(protoc, \
  output    : ['@BASENAME@.pb.cc', '@BASENAME@.pb.h'],
  arguments : ['--proto_path=' + proto_dir, '--cpp_out=@BUILD_DIR@', '@INPUT@'])

protbuf_srcs = proto_gen.process(join_paths(proto_dir, 'measurements.proto'))

avro_dir = join_paths(meson.source_root(), 'src/main/avro')
avro_input = join_paths(avro_dir, 'measurements.avsc')


avrogencpp_exe = find_program('avrogencpp', required: true)

avro_srcs = custom_target(
    'avro_gen',
    output: ['measurements.avro.hh'],
    input: avro_input,
    command: [avrogencpp_exe, '-i', avro_input, '-o', '@OUTPUT@', '-n', 'esw_avro'],
)

avro_dep = meson.get_compiler('cpp').find_library('avrocpp')
srcs = ['dataset.cpp', 'main.cpp', 'measurementinfo.cpp', 'result.cpp'] + protbuf_srcs  + avro_srcs
executable('server', srcs ,
  dependencies: [thread_dep, boost_regex_dep, jsoncpp_dep,  protobuf_dep, avro_dep])