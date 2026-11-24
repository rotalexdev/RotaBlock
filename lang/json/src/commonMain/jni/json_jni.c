#include <jni.h>
#include <stdint.h>

typedef struct TSLanguage TSLanguage;

extern const TSLanguage *tree_sitter_json(void);

JNIEXPORT jlong JNICALL
Java_dev_rotalex_rotablock_lang_json_JsonNative_jsonLanguage(JNIEnv *env, jobject self) {
    (void)env;
    (void)self;
    return (jlong)(intptr_t)tree_sitter_json();
}
