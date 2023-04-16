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

# avro_codegen = custom_target(
#     'avro_codegen',
#     output: ['measurements.hh', 'measurements.cc'],
#     input: avro_input,
#     command: ['avrogencpp', '-i', '@INPUT@', '-o', '@OUTDIR@', '-n', 'esw']
# )
avrogencpp_exe = find_program('avrogencpp', required: true)

# avro_gen = generator(
#     avrogencpp_exe,
#     output: ['@BASENAME@.avro.hh'],
#     arguments: ['avrogencpp', '-i', avro_input, '-o', '@BUILD_DIR@', '@INPUT@', '-n', 'esw'],
# )
avro_srcs = custom_target(
    'avro_gen',
    output: ['measurements.avro.hh'],
    input: avro_input,
    command: [avrogencpp_exe, '-i', avro_input, '-o', '@OUTPUT@', '-n', 'esw_avro'],
)


#avro_srcs = avro_gen.process(avro_input)
avro_dep = meson.get_compiler('cpp').find_library('avrocpp')
srcs = ['dataset.cpp', 'main.cpp', 'measurementinfo.cpp', 'result.cpp'] + protbuf_srcs  + avro_srcs
executable('server', srcs ,
  dependencies: [thread_dep, boost_regex_dep, jsoncpp_dep,  protobuf_dep, avro_dep])