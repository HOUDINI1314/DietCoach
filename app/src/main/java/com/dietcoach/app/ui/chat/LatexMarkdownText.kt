package com.dietcoach.app.ui.chat

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Base64
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/** 含 $...$ / $$...$$ / \\(...\\) / \\[...\\] 时用 KaTeX 渲染，否则普通文本。 */
fun looksLikeLatex(text: String): Boolean {
    if (text.contains("$$") || text.contains("\\[") || text.contains("\\(")) return true
    // 成对的单个 $
    var count = 0
    var i = 0
    while (i < text.length) {
        if (text[i] == '$') {
            if (i + 1 < text.length && text[i + 1] == '$') {
                i += 2
                continue
            }
            count++
        }
        i++
    }
    return count >= 2
}

@Composable
fun ChatMessageBody(
    text: String,
    streaming: Boolean,
    textColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    if (streaming || !looksLikeLatex(text)) {
        Text(
            text = if (streaming && text.isNotEmpty()) "$text▍" else text,
            color = textColor,
            style = MaterialTheme.typography.bodyLarge,
            modifier = modifier
        )
    } else {
        LatexWebView(
            content = text,
            textColorArgb = textColor.toArgb(),
            modifier = modifier
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun LatexWebView(
    content: String,
    textColorArgb: Int,
    modifier: Modifier = Modifier
) {
    var heightPx by remember(content) { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val html = remember(content, textColorArgb) {
        buildKatexHtml(content, textColorArgb)
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (heightPx > 0) {
                    Modifier.heightIn(min = with(density) { heightPx.toDp() })
                        .wrapContentHeight()
                } else {
                    Modifier.heightIn(min = 48.dp)
                }
            ),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setBackgroundColor(Color.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        view?.evaluateJavascript(
                            "(function(){return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);})()"
                        ) { value ->
                            val h = value?.trim('"')?.toFloatOrNull() ?: return@evaluateJavascript
                            if (h > 0f) {
                                heightPx = (h * resources.displayMetrics.density).toInt()
                            }
                        }
                    }
                }
            }
        },
        update = { webView ->
            if (webView.tag != html) {
                webView.tag = html
                webView.loadDataWithBaseURL(
                    "https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/",
                    html,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        }
    )
}

private fun buildKatexHtml(content: String, textColorArgb: Int): String {
    val payload = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    val color = String.format("#%06X", 0xFFFFFF and textColorArgb)
    return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1"/>
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/katex.min.css"/>
<script src="https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/katex.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/contrib/auto-render.min.js"></script>
<style>
  html, body {
    margin: 0; padding: 0;
    background: transparent;
    color: $color;
    font-size: 15px;
    line-height: 1.55;
    font-family: sans-serif;
    word-wrap: break-word;
    overflow-wrap: anywhere;
  }
  #c { white-space: pre-wrap; }
  .katex-display { margin: 0.6em 0; overflow-x: auto; overflow-y: hidden; }
</style>
</head>
<body>
<div id="c"></div>
<script>
  (function() {
    try {
      var raw = atob('$payload');
      var bytes = Uint8Array.from(raw, function(c){ return c.charCodeAt(0); });
      var text = new TextDecoder('utf-8').decode(bytes);
      var el = document.getElementById('c');
      el.textContent = text;
      renderMathInElement(el, {
        delimiters: [
          {left: '$$', right: '$$', display: true},
          {left: '\\[', right: '\\]', display: true},
          {left: '$', right: '$', display: false},
          {left: '\\(', right: '\\)', display: false}
        ],
        throwOnError: false,
        ignoredTags: ['script', 'noscript', 'style', 'textarea', 'pre', 'code']
      });
    } catch (e) {
      document.getElementById('c').textContent = '公式渲染失败';
    }
  })();
</script>
</body>
</html>
""".trimIndent()
}
