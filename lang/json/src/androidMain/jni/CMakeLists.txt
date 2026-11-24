cmake_minimum_required(VERSION 3.10.0)

project(ktreesitter_json LANGUAGES C)

set(GRAMMAR_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../tree-sitter-json/src")

add_library(ktreesitter-json SHARED "${GRAMMAR_DIR}/parser.c")

set_target_properties(ktreesitter-json PROPERTIES C_STANDARD 11)

target_include_directories(
    ktreesitter-json
    PRIVATE
    "${GRAMMAR_DIR}"
)
