package com.edgeedu.app.ui

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat

/**
 * Renders LaTeX via the bundled KaTeX distribution (assets/katex/) in a small
 * WebView. Fully offline: assets are served through WebViewAssetLoader from
 * inside the APK; JavaScript is enabled only for KaTeX itself and no remote
 * URL is ever requested.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun KatexView(latex: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
                .build()
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                setBackgroundColor(0x00000000)
                webViewClient = object : WebViewClientCompat() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest,
                    ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)
                }
            }
        },
        update = { webView ->
            val escaped = latex
                .replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("$", "\\$")
            val html = """
                <!DOCTYPE html><html><head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <link rel="stylesheet" href="https://appassets.androidplatform.net/assets/katex/katex.min.css">
                <script src="https://appassets.androidplatform.net/assets/katex/katex.min.js"></script>
                <style>body { margin: 4px; font-size: 17px; background: transparent; }</style>
                </head><body><div id="m"></div>
                <script>
                  katex.render(`$escaped`, document.getElementById('m'),
                               { throwOnError: false, displayMode: true });
                </script>
                </body></html>
            """.trimIndent()
            webView.loadDataWithBaseURL(
                "https://appassets.androidplatform.net/assets/",
                html, "text/html", "utf-8", null
            )
        }
    )
}
