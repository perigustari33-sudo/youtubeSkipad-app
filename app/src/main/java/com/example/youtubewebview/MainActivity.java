package com.example.youtubewebview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    
    private WebView webView;
    private ProgressBar progressBar;
    private String currentUrl = "https://m.youtube.com";
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private int originalOrientation;
    private FrameLayout fullscreenContainer;
    private PowerManager.WakeLock wakeLock;
    
    // JavaScript untuk skip iklan
    private final String AD_SKIP_SCRIPT = 
        "(function() {" +
        "    const clear = (() => {" +
        "        const defined = v => v !== null && v !== undefined;" +
        "        const timeout = setInterval(() => {" +
        "            const ad = [...document.querySelectorAll('.ad-showing')][0];" +
        "            if (defined(ad)) {" +
        "                const video = document.querySelector('video');" +
        "                if (defined(video)) {" +
        "                    video.currentTime = video.duration;" +
        "                }" +
        "            }" +
        "        }, 500);" +
        "        return function() {" +
        "            clearTimeout(timeout);" +
        "        };" +
        "    })();" +
        "})();";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Setup wake lock untuk menjaga layar tetap menyala
        setupWakeLock();
        
        // Inisialisasi views
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        
        // Setup fullscreen container
        fullscreenContainer = new FrameLayout(this);
        
        // Setup WebView
        setupWebView();
        
        // Cek koneksi internet
        if (isNetworkAvailable()) {
            webView.loadUrl(currentUrl);
        } else {
            Toast.makeText(this, "Tidak ada koneksi internet", Toast.LENGTH_LONG).show();
        }
    }
    
    private void setupWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "YouTubeWebView:WakeLock"
        );
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        
        // Enable JavaScript
        webSettings.setJavaScriptEnabled(true);
        
        // Enable DOM Storage
        webSettings.setDomStorageEnabled(true);
        
        // Enable Database
        webSettings.setDatabaseEnabled(true);
        
        // Enable Cache
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        // Other settings
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);
        webSettings.setDefaultTextEncodingName("utf-8");
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        // User Agent untuk mobile YouTube
        String userAgent = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";
        webSettings.setUserAgentString(userAgent);
        
        // Enable cookies
        CookieManager.getInstance().setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }
        
        // WebViewClient
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                
                // Handle YouTube links
                if (url.contains("youtube.com") || url.contains("youtu.be")) {
                    view.loadUrl(url);
                    return true;
                }
                
                // Buka link eksternal di browser
                if (!url.startsWith("https://m.youtube.com") && 
                    !url.startsWith("https://www.youtube.com")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                }
                
                return false;
            }
            
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                currentUrl = url;
                progressBar.setVisibility(View.VISIBLE);
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                
                // Inject JavaScript untuk skip iklan
                view.evaluateJavascript(AD_SKIP_SCRIPT, null);
                
                // Inject CSS untuk menyembunyikan elemen iklan
                String hideAdsCSS = 
                    "javascript:(function() {" +
                    "    var style = document.createElement('style');" +
                    "    style.innerHTML = '" +
                    "        .ytp-ad-module, .ytp-ad-image-overlay, .ytp-ad-player-overlay, " +
                    "        .video-ads, .ytp-ad-overlay-container, .ytd-display-ad-renderer, " +
                    "        .ytd-action-companion-ad-renderer, .ytd-video-masthead-ad-advertiser-info-renderer, " +
                    "        .ytd-in-feed-ad-layout-renderer, #masthead-ad, .ytd-banner-promo-renderer, " +
                    "        ytd-compact-promoted-video-renderer, ytd-promoted-sparkles-web-renderer, " +
                    "        .ytd-display-ad-renderer, .ytd-statement-banner-renderer, " +
                    "        tp-yt-paper-dialog, ytd-engagement-panel-section-list-renderer[target-id=\"engagement-panel-ads\"] " +
                    "        { display: none !important; }" +
                    "    ';" +
                    "    document.head.appendChild(style);" +
                    "})();";
                
                view.evaluateJavascript(hideAdsCSS, null);
            }
            
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, 
                                       android.webkit.WebResourceError error) {
                super.onReceivedError(view, request, error);
                progressBar.setVisibility(View.GONE);
                
                if (!isNetworkAvailable()) {
                    Toast.makeText(MainActivity.this, 
                        "Koneksi internet terputus", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        // WebChromeClient dengan dukungan fullscreen
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                progressBar.setProgress(newProgress);
            }
            
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                if (title != null && !title.isEmpty()) {
                    setTitle(title);
                }
            }
            
            // Method untuk fullscreen
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                
                originalOrientation = getRequestedOrientation();
                customView = view;
                customViewCallback = callback;
                
                // Sembunyikan WebView
                webView.setVisibility(View.GONE);
                
                // Tampilkan fullscreen container
                fullscreenContainer = new FrameLayout(MainActivity.this);
                fullscreenContainer.setBackgroundColor(getResources().getColor(android.R.color.black));
                fullscreenContainer.addView(customView);
                setContentView(fullscreenContainer);
                
                // Set orientation ke landscape untuk fullscreen
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                
                // Sembunyikan status bar dan action bar
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().hide();
                }
            }
            
            @Override
            public void onHideCustomView() {
                if (customView == null) {
                    return;
                }
                
                // Kembalikan ke layout awal
                customView.setVisibility(View.GONE);
                fullscreenContainer.removeView(customView);
                customView = null;
                setContentView(R.layout.activity_main);
                
                // Setup ulang views
                webView = findViewById(R.id.webView);
                progressBar = findViewById(R.id.progressBar);
                setupWebView();
                
                // Load URL yang sedang dibuka
                webView.loadUrl(currentUrl);
                
                // Kembalikan orientation
                setRequestedOrientation(originalOrientation);
                
                // Tampilkan kembali status bar dan action bar
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().show();
                }
                
                customViewCallback.onCustomViewHidden();
                customViewCallback = null;
            }
            
            // Method untuk mengizinkan video fullscreen
            @Override
            public void onShowCustomView(View view, int requestedOrientation, CustomViewCallback callback) {
                onShowCustomView(view, callback);
            }
        });
        
        // Handle tombol back
        webView.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_BACK && customView != null) {
                    // Jika fullscreen, keluar dari fullscreen
                    webView.getWebChromeClient().onHideCustomView();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
                    webView.goBack();
                    return true;
                }
            }
            return false;
        });
    }
    
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
    
    @Override
    public void onBackPressed() {
        if (customView != null) {
            // Jika dalam mode fullscreen, keluar dari fullscreen
            webView.getWebChromeClient().onHideCustomView();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
        // Mengaktifkan wake lock saat activity resume
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire(10 * 60 * 1000L); // 10 menit timeout
        }
        // Alternatif: gunakan flag window
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
        // Melepas wake lock saat activity pause untuk hemat baterai
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        // Hapus flag keep screen on
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    
    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        super.onDestroy();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) {
            webView.saveState(outState);
        }
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (webView != null) {
            webView.restoreState(savedInstanceState);
        }
    }
}