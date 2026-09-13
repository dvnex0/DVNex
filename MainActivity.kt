package com.browser.dvnexnew

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.webkit.URLUtil
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.browser.dvnexnew.browser.BrowserClient
import com.browser.dvnexnew.browser.BrowserEngine
import com.browser.dvnexnew.browser.BrowserSettings
import com.browser.dvnexnew.browser.BrowserTab
import com.browser.dvnexnew.browser.BrowserWebView
import com.browser.dvnexnew.browser.DownloadsPage
import com.browser.dvnexnew.ui.browser.BrowserToolbar
import com.browser.dvnexnew.ui.browser.TabsPage
import com.browser.dvnexnew.ui.home.HomePage
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {

    private lateinit var webView: BrowserWebView
    private lateinit var browserEngine: BrowserEngine

    private lateinit var browserLayout: View
    private lateinit var homeContainer: FrameLayout

    private lateinit var toolbar: BrowserToolbar
    private lateinit var tabsPage: TabsPage
    private lateinit var downloadsPage: DownloadsPage

    private val tabs =
        mutableListOf<BrowserTab>()

    private var currentTabId =
        -1

    private var nextTabId =
        0

    private var isChangingTab =
        false

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_main
        )

        webView =
            findViewById(
                R.id.webView
            )

        browserLayout =
            findViewById(
                R.id.browserLayout
            )

        homeContainer =
            findViewById(
                R.id.homeContainer
            )

        browserEngine =
            BrowserEngine(
                webView
            )

        val toolbarContainer =
            findViewById<FrameLayout>(
                R.id.toolbarContainer
            )

        toolbar =
            BrowserToolbar(
                this,
                browserEngine,

                onTabsClick = {
                    showTabs()
                },

                onDesktopMode = {

                    BrowserSettings.setDesktopMode(
                        webView,
                        true
                    )

                    Toast.makeText(
                        this,
                        "Modo computador ativado",
                        Toast.LENGTH_SHORT
                    ).show()
                },

                onMobileMode = {

                    BrowserSettings.setDesktopMode(
                        webView,
                        false
                    )

                    Toast.makeText(
                        this,
                        "Modo celular ativado",
                        Toast.LENGTH_SHORT
                    ).show()
                },

                onTranslate = {
                    translateCurrentPage()
                },

                onDownload = {
                    showDownloads()
                }
            )

        toolbarContainer.removeAllViews()

        toolbarContainer.addView(
            toolbar
        )

        setupWebView()

        setupHome()

        setupTabs()

        setupDownloadsPage()

        setupDownloads()

        createFirstTab()

        showHome()
    }

    // =========================================================
    // WEBVIEW
    // =========================================================

    private fun setupWebView() {

        webView.webViewClient =
            BrowserClient { url ->

                if (
                    !url.isNullOrEmpty()
                ) {

                    toolbar.setAddress(
                        url
                    )

                    val current =
                        tabs.find {
                            it.id ==
                                    currentTabId
                        }

                    current?.let { tab ->

                        tab.url =
                            url

                        val title =
                            webView.title

                        if (
                            !title.isNullOrBlank()
                        ) {

                            tab.title =
                                title
                        }
                    }

                    showBrowser()

                    updateTabsPage()

                    // =================================================
                    // DETECTAR COR DA PÁGINA
                    // =================================================

                    webView.postDelayed({

                        webView.evaluateJavascript(
                            """
                        (function() {
                            try {
                                var body =
                                    window.getComputedStyle(
                                        document.body
                                    );

                                var html =
                                    window.getComputedStyle(
                                        document.documentElement
                                    );

                                var bodyBg =
                                    body.backgroundColor;

                                var htmlBg =
                                    html.backgroundColor;

                                function valid(c) {
                                    return c &&
                                           c !== "transparent" &&
                                           c !== "rgba(0, 0, 0, 0)";
                                }

                                if (valid(bodyBg)) {
                                    return bodyBg;
                                }

                                if (valid(htmlBg)) {
                                    return htmlBg;
                                }

                                return "rgb(255,255,255)";

                            } catch (e) {
                                return "rgb(255,255,255)";
                            }
                        })();
                        """.trimIndent()
                        ) { result ->

                            try {

                                val clean =
                                    result
                                        .replace(
                                            "\"",
                                            ""
                                        )
                                        .replace(
                                            "\\",
                                            ""
                                        )

                                val values =
                                    Regex(
                                        """\d+"""
                                    )
                                        .findAll(
                                            clean
                                        )
                                        .map {
                                            it.value.toInt()
                                        }
                                        .toList()

                                if (
                                    values.size >= 3
                                )

                                toolbar.setSiteColor(
                                    values[0],
                                    values[1],
                                    values[2]
                                )

                                downloadsPage.setThemeColor(
                                    values[0],
                                    values[1],
                                    values[2]
                                )

                            } catch (
                                _: Exception
                            ) {
                            }
                        }

                    }, 250)
                }
            }

        webView.setScrollCallback { compact ->

            toolbar.setCompactMode(
                compact
            )
        }

        if (
            webView.url.isNullOrEmpty()
        ) {

            webView.loadUrl(
                "https://www.google.com"
            )
        }
    }
    // =========================================================
    // HOME
    // =========================================================

    private fun setupHome() {

        val homePage =
            HomePage(
                this,

                onSearch = { text ->

                    openBrowserWith(
                        text
                    )
                },

                onOpenUrl = { url ->

                    openBrowserWith(
                        url
                    )
                }
            )

        homeContainer.addView(
            homePage
        )
    }

    // =========================================================
    // ABAS
    // =========================================================

    private fun setupTabs() {

        tabsPage =
            TabsPage(
                this,

                onTabSelected = { id ->

                    selectTab(
                        id
                    )
                },

                onNewTab = {

                    createNewTab()
                },

                onTabClosed = { id ->

                    closeTab(
                        id
                    )
                }
            )

        findViewById<FrameLayout>(
            R.id.main
        ).addView(
            tabsPage
        )

        tabsPage.visibility =
            View.GONE
    }

    // =========================================================
    // DOWNLOADS PAGE
    // =========================================================

    private fun setupDownloadsPage() {

        downloadsPage =
            DownloadsPage(
                this,

                onBack = {

                    showBrowser()
                }
            )

        findViewById<FrameLayout>(
            R.id.main
        ).addView(
            downloadsPage
        )

        downloadsPage.visibility =
            View.GONE
    }

    // =========================================================
    // PRIMEIRA ABA
    // =========================================================

    private fun createFirstTab() {

        if (
            tabs.isNotEmpty()
        ) {
            return
        }

        val tab =
            BrowserTab(
                id =
                    nextTabId++,

                title =
                    "Google",

                url =
                    "https://www.google.com"
            )

        tabs.add(
            tab
        )

        currentTabId =
            tab.id
    }

    // =========================================================
    // NOVA ABA
    // =========================================================

    private fun createNewTab() {

        saveCurrentTab()

        val tab =
            BrowserTab(
                id =
                    nextTabId++,

                title =
                    "Google",

                url =
                    "https://www.google.com"
            )

        tabs.add(
            tab
        )

        currentTabId =
            tab.id

        showBrowser()

        webView.loadUrl(
            "https://www.google.com"
        )

        updateTabsPage()
    }

    // =========================================================
    // ABRIR URL / PESQUISA
    // =========================================================

    private fun openBrowserWith(
        text: String
    ) {

        val cleanText =
            text.trim()

        if (
            cleanText.isEmpty()
        ) {
            return
        }

        val url =
            when {

                cleanText.startsWith(
                    "http://",
                    ignoreCase = true
                ) -> {

                    cleanText
                }

                cleanText.startsWith(
                    "https://",
                    ignoreCase = true
                ) -> {

                    cleanText
                }

                cleanText.contains(".") &&
                        !cleanText.contains(" ") -> {

                    "https://$cleanText"
                }

                else -> {

                    "https://www.google.com/search?q=" +
                            URLEncoder.encode(
                                cleanText,
                                "UTF-8"
                            )
                }
            }

        val current =
            tabs.find {
                it.id ==
                        currentTabId
            }

        if (
            current != null
        ) {

            current.url =
                url

            current.title =
                "Carregando..."
        }

        showBrowser()

        browserEngine.open(
            url
        )

        updateTabsPage()
    }

    // =========================================================
    // SALVAR ABA
    // =========================================================

    private fun saveCurrentTab() {

        if (
            currentTabId ==
            -1
        ) {
            return
        }

        val current =
            tabs.find {
                it.id ==
                        currentTabId
            } ?: return

        if (
            isChangingTab
        ) {
            return
        }

        val state =
            Bundle()

        webView.saveState(
            state
        )

        current.state =
            state

        webView.url?.let { url ->

            current.url =
                url
        }

        webView.title?.let { title ->

            if (
                title.isNotEmpty()
            ) {

                current.title =
                    title
            }
        }
    }

    // =========================================================
    // SELECIONAR ABA
    // =========================================================

    private fun selectTab(
        id: Int
    ) {

        if (
            id ==
            currentTabId
        ) {

            showBrowser()

            return
        }

        val target =
            tabs.find {
                it.id ==
                        id
            } ?: return

        isChangingTab =
            true

        saveCurrentTab()

        currentTabId =
            target.id

        showBrowser()

        if (
            target.state !=
            null
        ) {

            webView.restoreState(
                target.state!!
            )

        } else {

            webView.loadUrl(
                target.url
            )
        }

        toolbar.setAddress(
            target.url
        )

        updateTabsPage()

        isChangingTab =
            false
    }

    // =========================================================
    // FECHAR ABA
    // =========================================================

    private fun closeTab(
        id: Int
    ) {

        val index =
            tabs.indexOfFirst {
                it.id ==
                        id
            }

        if (
            index ==
            -1
        ) {
            return
        }

        val wasCurrent =
            id ==
                    currentTabId

        tabs.removeAt(
            index
        )

        if (
            tabs.isEmpty()
        ) {

            createFirstTab()

            showHome()

            updateTabsPage()

            return
        }

        if (
            wasCurrent
        ) {

            val newIndex =
                index.coerceAtMost(
                    tabs.lastIndex
                )

            val nextTab =
                tabs[
                    newIndex
                ]

            currentTabId =
                nextTab.id

            isChangingTab =
                true

            if (
                nextTab.state !=
                null
            ) {

                webView.restoreState(
                    nextTab.state!!
                )

            } else {

                webView.loadUrl(
                    nextTab.url
                )
            }

            toolbar.setAddress(
                nextTab.url
            )

            isChangingTab =
                false

            showBrowser()
        }

        updateTabsPage()
    }

    // =========================================================
    // ATUALIZAR PÁGINA DE ABAS
    // =========================================================

    private fun updateTabsPage() {

        if (
            ::tabsPage.isInitialized
        ) {

            tabsPage.updateTabs(
                tabs,
                currentTabId
            )
        }
    }

    // =========================================================
    // TELAS
    // =========================================================

    private fun showTabs() {

        saveCurrentTab()

        updateTabsPage()

        homeContainer.visibility =
            View.GONE

        browserLayout.visibility =
            View.GONE

        tabsPage.visibility =
            View.VISIBLE

        downloadsPage.visibility =
            View.GONE
    }

    private fun showDownloads() {

        homeContainer.visibility =
            View.GONE

        browserLayout.visibility =
            View.GONE

        tabsPage.visibility =
            View.GONE

        downloadsPage.visibility =
            View.VISIBLE

        downloadsPage.refreshDownloads()
    }

    private fun showBrowser() {

        homeContainer.visibility =
            View.GONE

        browserLayout.visibility =
            View.VISIBLE

        tabsPage.visibility =
            View.GONE

        downloadsPage.visibility =
            View.GONE

        updateTabsPage()
    }

    private fun showHome() {

        homeContainer.visibility =
            View.VISIBLE

        browserLayout.visibility =
            View.GONE

        tabsPage.visibility =
            View.GONE

        downloadsPage.visibility =
            View.GONE
    }

    // =========================================================
    // DV TRANSLATE
    // =========================================================

    private fun translateCurrentPage() {

        val currentUrl =
            webView.url

        if (
            currentUrl.isNullOrEmpty() ||
            currentUrl.startsWith(
                "about:",
                ignoreCase = true
            )
        ) {

            Toast.makeText(
                this,
                "Não há uma página para traduzir",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val encodedUrl =
            URLEncoder.encode(
                currentUrl,
                "UTF-8"
            )

        val translateUrl =
            "https://translate.google.com/translate" +
                    "?sl=auto&tl=pt&u=$encodedUrl"

        webView.loadUrl(
            translateUrl
        )
    }

    // =========================================================
    // DOWNLOADS
    // =========================================================

    private fun setupDownloads() {

        webView.setDownloadListener {
                url,
                userAgent,
                contentDisposition,
                mimeType,
                _ ->

            try {

                if (
                    url.isNullOrEmpty()
                ) {
                    return@setDownloadListener
                }

                val fileName =
                    URLUtil.guessFileName(
                        url,
                        contentDisposition,
                        mimeType
                    )

                val request =
                    DownloadManager.Request(
                        Uri.parse(
                            url
                        )
                    )

                if (
                    !mimeType.isNullOrEmpty()
                ) {

                    request.setMimeType(
                        mimeType
                    )
                }

                if (
                    !userAgent.isNullOrEmpty()
                ) {

                    request.addRequestHeader(
                        "User-Agent",
                        userAgent
                    )
                }

                request.setTitle(
                    fileName
                )

                request.setDescription(
                    "DVNex Browser"
                )

                request.setNotificationVisibility(
                    DownloadManager
                        .Request
                        .VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )

                request.setAllowedOverMetered(
                    true
                )

                request.setAllowedOverRoaming(
                    true
                )

                request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "DVNex/$fileName"
                )

                val downloadManager =
                    getSystemService(
                        Context.DOWNLOAD_SERVICE
                    ) as DownloadManager

                downloadManager.enqueue(
                    request
                )

                Toast.makeText(
                    this,
                    "Download iniciado",
                    Toast.LENGTH_SHORT
                ).show()

                if (
                    downloadsPage.visibility ==
                    View.VISIBLE
                ) {

                    downloadsPage.refreshDownloads()
                }

            } catch (
                e: Exception
            ) {

                Toast.makeText(
                    this,
                    "Erro ao iniciar o download",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // =========================================================
    // CICLO DE VIDA
    // =========================================================

    override fun onPause() {

        if (
            !isChangingTab &&
            currentTabId != -1 &&
            ::webView.isInitialized
        ) {

            saveCurrentTab()
        }

        super.onPause()
    }

    override fun onDestroy() {

        if (
            ::webView.isInitialized
        ) {

            webView.stopLoading()

            webView.onPause()

            webView.clearFocus()
        }

        super.onDestroy()
    }

    // =========================================================
    // BOTÃO VOLTAR
    // =========================================================

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {

        if (
            downloadsPage.visibility ==
            View.VISIBLE
        ) {

            showBrowser()

            return
        }

        if (
            tabsPage.visibility ==
            View.VISIBLE
        ) {

            showBrowser()

            return
        }

        if (
            browserLayout.visibility ==
            View.VISIBLE
        ) {

            if (
                browserEngine.goBack()
            ) {

                return
            }

            showHome()

            return
        }

        super.onBackPressed()
    }
}
