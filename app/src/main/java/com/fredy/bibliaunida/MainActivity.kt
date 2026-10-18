package com.fredy.bibliaunida

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

/**
 * Caparazon de la app "Biblia y apocrifos".
 *
 * Igual que el de la Biblia, pero abre la direccion de la app unida, que lleva
 * los 66 libros canonicos y los 122 libros de fuera.
 *
 * El boton Atras del telefono (el de Samsung, el gesto o el de la barra) tiene
 * que retroceder una pantalla dentro de la app, no cerrarla. La pagina no usa
 * el historial del navegador: lleva su propia pila en JavaScript (pilaAtras y
 * volverAtras). Asi que el boton nativo hace tres cosas, por este orden:
 *   1. Si el WebView tiene historial propio, retrocede en el.
 *   2. Si la pagina tiene pantallas apiladas, llama a su volverAtras().
 *   3. Solo si no queda nada atras, cierra la app (que es lo que se espera
 *      del boton por defecto cuando ya estas en la portada).
 */
class MainActivity : AppCompatActivity() {

    private val URL = "https://bibliaunida.79.72.57.253.nip.io/"
    private var web: WebView? = null
    private var fallo = false

    /** Le pregunta a la pagina si tiene algo que deshacer y lo deshace. */
    private val JS_ATRAS = """
        (function(){
          try {
            if (typeof pilaAtras !== 'undefined' && pilaAtras.length) {
              volverAtras();
              return '1';
            }
          } catch (e) {}
          return '0';
        })()
    """.trimIndent()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val raiz = LinearLayout(this)
        raiz.orientation = LinearLayout.VERTICAL
        raiz.setBackgroundColor(Color.parseColor("#FFF8E7"))

        val progreso = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        progreso.max = 100
        raiz.addView(progreso)

        val aviso = TextView(this)
        aviso.text = "Cargando la Biblia y los libros de fuera…"
        aviso.textSize = 18f
        aviso.gravity = Gravity.CENTER
        aviso.setPadding(24, 48, 24, 48)
        aviso.visibility = View.GONE
        raiz.addView(aviso)

        val reintentar = Button(this)
        reintentar.text = "Reintentar"
        reintentar.visibility = View.GONE
        raiz.addView(reintentar)

        val web = WebView(this)
        web.setBackgroundColor(Color.parseColor("#FFF8E7"))
        raiz.addView(web)
        this.web = web

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
        }
        WebView.setWebContentsDebuggingEnabled(false)

        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                progreso.visibility = View.GONE
                // WebView tambien dice que 'termino' cuando la carga ha
                // fallado: sin este guardado, el aviso de error y el boton
                // Reintentar se ocultaban al instante y quedaba una pantalla
                // en blanco sin explicacion ni manera de volver a probar.
                if (fallo) return
                aviso.visibility = View.GONE
                reintentar.visibility = View.GONE
            }

            override fun onReceivedError(
                view: WebView, request: WebResourceRequest, error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    fallo = true
                    progreso.visibility = View.GONE
                    aviso.text =
                        "No se pudo conectar.\n\nComprueba que hay internet, " +
                            "y vuelve a intentarlo."
                    aviso.visibility = View.VISIBLE
                    reintentar.visibility = View.VISIBLE
                }
            }
        }

        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progreso.progress = newProgress
                progreso.visibility =
                    if (newProgress >= 100) View.GONE else View.VISIBLE
            }
        }

        reintentar.setOnClickListener {
            fallo = false
            aviso.visibility = View.GONE
            reintentar.visibility = View.GONE
            web.loadUrl(URL)
        }
        fallo = false
        web.loadUrl(URL)

        // El boton Atras del sistema, que en Samsung es el de la barra y el
        // gesto de la derecha. Con el despachador funciona igual en Android
        // viejo y en el 13 o superior, donde onBackPressed() ya no se llama.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                irAtras()
            }
        })

        setContentView(raiz)
    }

    private fun irAtras() {
        val w = web
        if (w == null) {
            finish()
            return
        }
        if (w.canGoBack()) {
            w.goBack()
            return
        }
        // Sin historial del navegador: manda la pila de la pagina.
        w.evaluateJavascript(JS_ATRAS) { resultado ->
            if (resultado == null || !resultado.contains("1")) {
                finish()   // no queda nada atras: se cierra, como el boton normal
            }
        }
    }
}
