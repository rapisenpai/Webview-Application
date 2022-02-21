package com.rapisenpai.webview.stream.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.monstertechno.adblocker.AdBlockerWebView;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("ResultOfMethodCallIgnored")
@SuppressLint("ObsoleteSdkInt")
public class MainActivity extends AppCompatActivity {

    //Anime Play Website
    private WebView webView;
    public static final String USER_AGENT = "Mozilla/5.0 AppleWebKit/535.19 Chrome/56.0.0 Mobile Safari/535.19";

    //Swipe Refresh
    SwipeRefreshLayout swipeRefreshLayout;

    //Splash Screen
    private ProgressBar spinner;
    String ShowOrHideWebViewInitialUse = "show";

    //Progress Bar while loading URL
    ProgressBar progressBar;

    //Upload Files
    public Context context;
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int FILECHOOSER_RESULTCODE = 1;
    private ValueCallback<Uri> mUploadMessage;
    private Uri mCapturedImageURI = null;
    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;
    private int requestCode;
    private int resultCode;
    private Intent data;


    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Alert Dialog for DISCLAIMER
        String disclaimer = ("This App does not restore any files on this website. This is a WebView Application only." +
                " All contents are provided by non-affiliated third parties.");

        //Runtime External storage permission for saving download files
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED) {
                Log.d("permission", "permission denied to WRITE_EXTERNAL_STORAGE - requesting it");
                String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                requestPermissions(permissions, 1);
            }
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.ic_baseline_warning_24);
        builder.setTitle("DISCLAIMER");
        builder.setMessage(disclaimer);
        builder.setCancelable(false);
        builder.setPositiveButton("Accept", (dialog, which) -> {
        });
        builder.setNegativeButton("Decline", (dialog, which) -> {
            dialog.cancel();
            MainActivity.this.finish();
        });
        builder.show();

        //WebView Performance
        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.Anime_Play);
        webView.getSettings().setUserAgentString(USER_AGENT);
        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.getSettings().setSupportMultipleWindows(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setLoadsImagesAutomatically(true);
        WebSettings settings = webView.getSettings();
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(true);
        webView.setWebChromeClient(new ChromeClient());
        webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.getSettings().setSaveFormData(true);
        webView.clearCache(true);
        webView.clearHistory();
        registerForContextMenu(webView);
        webView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
        webView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setSavePassword(true);
        webView.getSettings().setEnableSmoothTransition(true);
        webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webView.getSettings().setPluginState(WebSettings.PluginState.ON);

         AdBlocker.init(this);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            private final Map<String, Boolean> loadedUrls = new HashMap<>();

            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                boolean ad;
                if (!loadedUrls.containsKey(url)) {
                    ad = AdBlocker.isAd(url);
                    loadedUrls.put(url, ad);
                } else {
                    ad = loadedUrls.get(url);
                }
                return ad ? AdBlocker.createEmptyResource() :
                        super.shouldInterceptRequest(view, url);
            }

            public void onPageFinished(WebView view, String url) {
                swipeRefreshLayout.setRefreshing(false);
            }

        });

        //Download files
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimeType);
            String cookies = CookieManager.getInstance().getCookie(url);
            request.addRequestHeader("cookie",cookies);
            request.addRequestHeader("User-Agent",userAgent);
            request.setDescription("Downloading file....");
            request.setTitle(URLUtil.guessFileName(url,contentDisposition,mimeType));
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,URLUtil.guessFileName(url, contentDisposition, mimeType));
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            dm.enqueue(request);
            Toast.makeText(getApplicationContext(),"Downloading File",Toast.LENGTH_SHORT).show();

        });

        //Splash Screen
        spinner = findViewById(R.id.progressBar1);

        //Progress Bar while loading URL
        progressBar = findViewById(R.id.progressBar2);
        class WebViewClient extends android.webkit.WebViewClient {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
            }
        }

        webView.setWebViewClient(new CustomWebViewClient());

        //Swipe Refresh
        swipeRefreshLayout = this.findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setColorSchemeColors(
                getResources().getColor(android.R.color.holo_blue_dark),
                getResources().getColor(android.R.color.holo_orange_dark),
                getResources().getColor(android.R.color.holo_green_dark),
                getResources().getColor(android.R.color.holo_red_dark));

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setLoadWithOverviewMode(true);
            webView.getSettings().setUseWideViewPort(true);
            webView.getSettings().setDomStorageEnabled(true);
            webView.getSettings().setLoadsImagesAutomatically(true);
        }

        swipeRefreshLayout.setOnRefreshListener(this::LoadWeb);
        LoadWeb();
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void LoadWeb() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAppCacheEnabled(true);
        webView.loadUrl("http://animixplay.to");
        //webView.loadUrl("javascript:window.location.reload(true)");
        swipeRefreshLayout.setRefreshing(true);

        new AdBlockerWebView.init(this).initializeWebView(webView);
        webView.setWebViewClient(new WebViewClient() {

            @SuppressWarnings("deprecation")
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {

                return AdBlockerWebView.blockAds(view, url) ? AdBlocker.createEmptyResource() :
                        super.shouldInterceptRequest(view, url);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }
        });

        AdBlocker.init(this);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            private final Map<String, Boolean> loadedUrls = new HashMap<>();

            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                boolean ad;
                if (!loadedUrls.containsKey(url)) {
                    ad = AdBlocker.isAd(url);
                    loadedUrls.put(url, ad);
                } else {
                    ad = loadedUrls.get(url);
                }
                return ad ? AdBlocker.createEmptyResource() :
                        super.shouldInterceptRequest(view, url);
            }

            public void onPageFinished(WebView view, String url) {
                swipeRefreshLayout.setRefreshing(false);
            }

        });
    }

    //Back Page with Alert Exit Button
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.app_name);
            builder.setIcon(R.drawable.icon);
            builder.setMessage("Do you really want to exit?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", (dialog, which) -> MainActivity.super.onBackPressed())
                    .setNegativeButton("No", (dialog, which) -> dialog.cancel());

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    }

    //Save current state
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack()) {
                webView.goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    //Playing Web Video in Landscape View
    private class ChromeClient extends WebChromeClient {
        private View mCustomView;
        private WebChromeClient.CustomViewCallback mCustomViewCallback;
        protected FrameLayout mFullscreenContainer;
        private int mOriginalOrientation;
        private int mOriginalSystemUiVisibility;

        public Bitmap getDefaultVideoPoster() {
            if (mCustomView == null) {
                return null;
            }
            return BitmapFactory.decodeResource(getApplicationContext().getResources(), 2130837573);
        }

        public void onHideCustomView() {
            ((FrameLayout) getWindow().getDecorView()).removeView(this.mCustomView);
            this.mCustomView = null;
            getWindow().getDecorView().setSystemUiVisibility(this.mOriginalSystemUiVisibility);
            setRequestedOrientation(this.mOriginalOrientation);
            this.mCustomViewCallback.onCustomViewHidden();
            this.mCustomViewCallback = null;
        }

        public void onShowCustomView(View paramView, WebChromeClient.CustomViewCallback paramCustomViewCallback) {
            if (this.mCustomView != null) {
                onHideCustomView();
                return;
            }
            this.mCustomView = paramView;
            this.mOriginalSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
            this.mOriginalOrientation = getRequestedOrientation();
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            this.mCustomViewCallback = paramCustomViewCallback;
            ((FrameLayout) getWindow().getDecorView()).addView(this.mCustomView, new FrameLayout.LayoutParams(-1, -1));
            getWindow().getDecorView().setSystemUiVisibility(3846 | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);


        }
    }

    private class CustomWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView webview, String url, Bitmap favicon) {
            if (ShowOrHideWebViewInitialUse.equals("show")) {
                webview.setVisibility(View.INVISIBLE);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            ShowOrHideWebViewInitialUse = "hide";
            spinner.setVisibility(View.GONE);
            view.setVisibility(View.VISIBLE);
            super.onPageFinished(view, url);
        }
    }
    // Upload Files
    @SuppressLint("QueryPermissionsNeeded")
    public boolean onShowFileChooser(
            WebView webView, ValueCallback<Uri[]> filePathCallback,
            WebChromeClient.FileChooserParams fileChooserParams) {
        if (mFilePathCallback != null) {
            mFilePathCallback.onReceiveValue(null);
        }
        mFilePathCallback = filePathCallback;

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            // create the file where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
                takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e(TAG, "Unable to create Image File", ex);
            }

            // continue only if the file was successfully created
            if (photoFile != null) {
                mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
            } else {
                takePictureIntent = null;
            }
        }

        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType("image/*");

        Intent[] intentArray;
        if (takePictureIntent != null) {
            intentArray = new Intent[]{takePictureIntent};
        } else {
            intentArray = new Intent[0];
        }

        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
        chooserIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.image_chooser));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
        startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);

        return true;
    }

    // Upload Files : Creating image files (Lollipop only)
    private File createImageFile() throws IOException {

        File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "DirectoryNameHere");

        if (!imageStorageDir.exists()) {
            imageStorageDir.mkdirs();
        }

        // Upload Files : create an image file name
        imageStorageDir = new File(imageStorageDir + File.separator + "IMG_" + System.currentTimeMillis() + ".jpg");
        return imageStorageDir;
    }

    // Upload Files : openFileChooser for Android 3.0+
    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
        mUploadMessage = uploadMsg;

        try {
            File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "DirectoryNameHere");

            if (!imageStorageDir.exists()) {
                imageStorageDir.mkdirs();
            }

            File file = new File(imageStorageDir + File.separator + "IMG_" + System.currentTimeMillis() + ".jpg");

            mCapturedImageURI = Uri.fromFile(file); // save to the private variable

            final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);
            //captureIntent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/*");

            Intent chooserIntent = Intent.createChooser(i, getString(R.string.image_chooser));
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[]{captureIntent});

            startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);
        } catch (Exception e) {
            Toast.makeText(getBaseContext(), "Camera Exception:" + e, Toast.LENGTH_LONG).show();
        }

    }

    // Upload Files : openFileChooser for Android < 3.0
    public void openFileChooser(ValueCallback<Uri> uploadMsg) {
        openFileChooser(uploadMsg, "");
    }

    // Upload Files : openFileChooser for other Android versions
            /* may not work on KitKat due to lack of implementation of openFileChooser() or onShowFileChooser()
               https://code.google.com/p/android/issues/detail?id=62220
               however newer versions of KitKat fixed it on some devices */
    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
        openFileChooser(uploadMsg, acceptType);
    }

    // Upload Files : return here when file selected from camera or from SD Card

    {
        boolean finished = false;
        this.requestCode = requestCode;
        this.resultCode = resultCode;
        this.data = data;

        // Upload Files : code for all versions except of Lollipop
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {

            if (requestCode == FILECHOOSER_RESULTCODE) {
                if (null == this.mUploadMessage) {
                    finished = true;
                } else {
                    Uri result = null;
                    try {
                        if (resultCode != RESULT_OK) {
                            result = null;
                        } else {
                            // retrieve from the private variable if the intent is null
                            result = data == null ? mCapturedImageURI : data.getData();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), "activity :" + e, Toast.LENGTH_LONG).show();
                    }
                    mUploadMessage.onReceiveValue(result);
                    mUploadMessage = null;
                }

            }

        } // Upload Files : end of code for all versions except of Lollipop
        if (!finished) {// Upload Files : start of code for Lollipop only
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                if (requestCode != FILECHOOSER_RESULTCODE || mFilePathCallback == null) {
                    super.onActivityResult(requestCode, resultCode, data);
                } else {
                    Uri[] results = null;// Upload Files : check that the response is a good one
                    if (resultCode == Activity.RESULT_OK) {
                        if (data == null || data.getData() == null) {
                            // if there is not data, then we may have taken a photo
                            if (mCameraPhotoPath != null) {
                                results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                            }
                        } else {
                            String dataString = data.getDataString();
                            if (dataString != null) {
                                results = new Uri[]{Uri.parse(dataString)};
                            }
                        }
                    }
                    mFilePathCallback.onReceiveValue(results);
                    mFilePathCallback = null;
                }
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == FILECHOOSER_RESULTCODE) {
            if (null == mUploadMessage)
                return;
            Uri result = intent == null || resultCode != RESULT_OK ? null
                    : intent.getData();
            mUploadMessage.onReceiveValue(result);
            mUploadMessage = null;

        }
    }
}