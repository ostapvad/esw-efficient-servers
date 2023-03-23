boost_regex_dep = meson.get_compiler('cpp').find_library('boost_regex')
thread_dep = dependency('threads')
jsoncpp_dep = dependency('jsoncpp')
srcs = ['dataset.cpp', 'main.cpp', 'measurementinfo.cpp', 'result.cpp']
executable('server', srcs,
  dependencies: [thread_dep, boost_regex_dep, jsoncpp_dep])
