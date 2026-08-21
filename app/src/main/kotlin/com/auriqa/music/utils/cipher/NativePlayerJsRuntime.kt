package com.auriqo.music.utils.cipher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.EvaluatorException
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined

/**
 * Executes the configured player functions in Rhino, without an Android WebView renderer.
 *
 * The current YouTube bundle performs browser capability checks during initialization, so this
 * runtime supplies only the small browser surface those checks read. The cipher functions remain
 * closures over the player IIFE, which is what lets config expressions such as `Af(1,5321,INPUT)`
 * use the bundle's private helper state without trying to parse or reimplement it in Kotlin.
 */
internal class NativePlayerJsRuntime private constructor(
    private val scope: Scriptable,
    private val contextFactory: GuardedContextFactory,
) {
    fun deobfuscateSignature(obfuscatedSignature: String): String =
        contextFactory.callGuarded(EVAL_TIMEOUT_MS) { context ->
            val function = getFunction("_cipherSigFunc")
            val result = function.call(
                context,
                scope,
                scope,
                arrayOf(obfuscatedSignature),
            )
            resultString(result, "signature")
        }

    fun transformN(nValue: String): String =
        contextFactory.callGuarded(EVAL_TIMEOUT_MS) { context ->
            val function = getFunction("_nTransformFunc")
            val result = function.call(
                context,
                scope,
                scope,
                arrayOf(nValue),
            )
            resultString(result, "n-transform")
        }

    private fun getFunction(name: String): Function {
        val value = ScriptableObject.getProperty(scope, name)
        return value as? Function
            ?: throw IllegalStateException("Player JS function $name was not exported")
    }

    private fun resultString(result: Any?, operation: String): String {
        if (result == null || result === Undefined.instance) {
            throw IllegalStateException("Player JS $operation returned null")
        }
        return Context.toString(result).takeIf(String::isNotEmpty)
            ?: throw IllegalStateException("Player JS $operation returned an empty value")
    }

    companion object {
        private const val INIT_TIMEOUT_MS = 30_000L
        private const val EVAL_TIMEOUT_MS = 3_000L
        private const val PLAYER_MARKER = "})(_yt_player);"

        suspend fun create(
            playerJs: String,
            config: FunctionNameExtractor.HardcodedPlayerConfig,
        ): NativePlayerJsRuntime = withContext(Dispatchers.Default) {
            val contextFactory = GuardedContextFactory()
            contextFactory.callGuarded(INIT_TIMEOUT_MS) { context ->
                val scope = context.initStandardObjects()
                installBrowserSurface(context, scope)

                val modifiedPlayerJs = injectFunctions(playerJs, config)
                check(PLAYER_MARKER in playerJs) {
                    "Player JS does not contain the expected IIFE marker"
                }
                context.evaluateString(scope, modifiedPlayerJs, "player.js", 1, null)

                check(ScriptableObject.getProperty(scope, "_cipherSigFunc") is Function) {
                    "Player JS signature function was not exported"
                }
                NativePlayerJsRuntime(scope, contextFactory)
            }
        }

        private fun installBrowserSurface(context: Context, scope: Scriptable) {
            ScriptableObject.putProperty(scope, "window", scope)
            ScriptableObject.putProperty(scope, "globalThis", scope)
            ScriptableObject.putProperty(scope, "document", context.newObject(scope))
            ScriptableObject.putProperty(scope, "navigator", context.newObject(scope))

            val location = context.newObject(scope)
            ScriptableObject.putProperty(location, "hostname", "www.youtube.com")
            ScriptableObject.putProperty(scope, "location", location)

            context.evaluateString(
                scope,
                "var XMLHttpRequest = function() {}; XMLHttpRequest.prototype = {};",
                "browser-stubs.js",
                1,
                null,
            )
        }

        private fun injectFunctions(
            playerJs: String,
            config: FunctionNameExtractor.HardcodedPlayerConfig,
        ): String {
            check(PLAYER_MARKER in playerJs) { "Player JS IIFE marker not found" }

            val signatureExpression = config.sigJsExpression
                ?: error("Native player config does not contain a signature expression")
            val nExpression = config.nJsExpression
                ?: error("Native player config does not contain an n expression")

            val exports = buildString {
                append(";window._cipherSigFunc=function(sig){try{return ")
                append(signatureExpression.replace("INPUT", "sig"))
                append(";}catch(e){return null;}};")
                append("window._nTransformFunc=function(n){try{return ")
                append(nExpression.replace("INPUT", "n"))
                append(";}catch(e){return n;}};")
            }
            return playerJs.replace(PLAYER_MARKER, exports + PLAYER_MARKER)
        }
    }

    internal class GuardedContextFactory : ContextFactory() {
        @Volatile
        private var deadlineNanos = Long.MAX_VALUE

        fun <T> callGuarded(timeoutMs: Long, action: (Context) -> T): T {
            deadlineNanos = System.nanoTime() + timeoutMs * 1_000_000L
            return try {
                call { context ->
                    context.setLanguageVersion(Context.VERSION_ES6)
                    context.setOptimizationLevel(-1)
                    context.setInstructionObserverThreshold(INSTRUCTION_THRESHOLD)
                    action(context)
                }
            } finally {
                deadlineNanos = Long.MAX_VALUE
            }
        }

        override fun observeInstructionCount(context: Context, instructionCount: Int) {
            if (System.nanoTime() > deadlineNanos) {
                throw EvaluatorException("Player JavaScript execution exceeded its deadline")
            }
        }

        private companion object {
            const val INSTRUCTION_THRESHOLD = 10_000
        }
    }
}
