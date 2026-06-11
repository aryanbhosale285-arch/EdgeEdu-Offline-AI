// JNI bridge for LlamaCppEngine (Kotlin). Built only with -PenableLlama.
//
// Safety contract (PRD §13.1, JNI crash safety): every native entry point
// catches C++ exceptions and turns failures into Java RuntimeExceptions
// instead of faulting; token counts and context length are clamped before
// inference. Generation is constrained with a GBNF grammar so <calc> tool
// calls are syntactically enforced.

#include <jni.h>
#include <string>
#include <vector>

#include "llama.h"
#include "common/common.h"
#include "common/sampling.h"

namespace {

struct Session {
    llama_model *model = nullptr;
    llama_context *ctx = nullptr;
};

constexpr int kMaxContext = 4096;  // capped KV cache (memory discipline)

void throw_java(JNIEnv *env, const std::string &message) {
    jclass cls = env->FindClass("java/lang/RuntimeException");
    if (cls != nullptr) env->ThrowNew(cls, message.c_str());
}

std::string to_string(JNIEnv *env, jstring value) {
    const char *chars = env->GetStringUTFChars(value, nullptr);
    std::string out(chars ? chars : "");
    if (chars) env->ReleaseStringUTFChars(value, chars);
    return out;
}

}  // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_edgeedu_app_tutor_LlamaCppEngine_nativeLoad(JNIEnv *env, jobject, jstring model_path) {
    try {
        llama_backend_init();
        auto *session = new Session();

        llama_model_params model_params = llama_model_default_params();
        model_params.use_mmap = true;  // weights paged in lazily, not copied
        session->model = llama_model_load_from_file(to_string(env, model_path).c_str(), model_params);
        if (session->model == nullptr) {
            delete session;
            throw_java(env, "failed to load GGUF model (checksum-verify the file)");
            return 0;
        }

        llama_context_params ctx_params = llama_context_default_params();
        ctx_params.n_ctx = kMaxContext;
        session->ctx = llama_init_from_model(session->model, ctx_params);
        if (session->ctx == nullptr) {
            llama_model_free(session->model);
            delete session;
            throw_java(env, "failed to create llama context");
            return 0;
        }
        return reinterpret_cast<jlong>(session);
    } catch (const std::exception &e) {
        throw_java(env, std::string("nativeLoad: ") + e.what());
        return 0;
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_edgeedu_app_tutor_LlamaCppEngine_nativeGenerate(
    JNIEnv *env, jobject, jlong handle, jstring prompt, jstring grammar, jint max_tokens) {
    try {
        auto *session = reinterpret_cast<Session *>(handle);
        if (session == nullptr || session->ctx == nullptr) {
            throw_java(env, "generate called on a closed session");
            return nullptr;
        }
        const std::string prompt_text = to_string(env, prompt);
        const std::string grammar_text = to_string(env, grammar);
        const int n_predict = std::min(std::max((int) max_tokens, 1), 1024);

        const llama_vocab *vocab = llama_model_get_vocab(session->model);
        std::vector<llama_token> tokens(prompt_text.size() + 8);
        int n_tokens = llama_tokenize(vocab, prompt_text.c_str(), (int) prompt_text.size(),
                                      tokens.data(), (int) tokens.size(), true, true);
        if (n_tokens < 0 || n_tokens >= kMaxContext - n_predict) {
            throw_java(env, "prompt too long for the context window");
            return nullptr;
        }
        tokens.resize(n_tokens);

        common_params_sampling sampling;
        sampling.temp = 0.3f;
        sampling.grammar = grammar_text;  // GBNF: <calc> structure enforced here
        common_sampler *sampler = common_sampler_init(session->model, sampling);
        if (sampler == nullptr) {
            throw_java(env, "failed to init sampler (bad grammar?)");
            return nullptr;
        }

        llama_batch batch = llama_batch_get_one(tokens.data(), (int) tokens.size());
        std::string output;
        for (int i = 0; i < n_predict; ++i) {
            if (llama_decode(session->ctx, batch) != 0) break;
            llama_token token = common_sampler_sample(sampler, session->ctx, -1);
            if (llama_vocab_is_eog(vocab, token)) break;
            common_sampler_accept(sampler, token, true);
            output += common_token_to_piece(session->ctx, token);
            batch = llama_batch_get_one(&token, 1);
        }
        common_sampler_free(sampler);
        return env->NewStringUTF(output.c_str());
    } catch (const std::exception &e) {
        throw_java(env, std::string("nativeGenerate: ") + e.what());
        return nullptr;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_edgeedu_app_tutor_LlamaCppEngine_nativeFree(JNIEnv *, jobject, jlong handle) {
    auto *session = reinterpret_cast<Session *>(handle);
    if (session == nullptr) return;
    if (session->ctx != nullptr) llama_free(session->ctx);
    if (session->model != nullptr) llama_model_free(session->model);
    delete session;
}
