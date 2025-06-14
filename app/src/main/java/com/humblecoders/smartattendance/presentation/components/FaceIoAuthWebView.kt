package com.humblecoders.smartattendance.presentation.components

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import timber.log.Timber

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FaceIoAuthWebView(
    modifier: Modifier = Modifier,
    onAuthenticated: (rollNumber: String) -> Unit,
    onError: (String) -> Unit
) {
    val webView = remember { mutableStateOf<WebView?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    allowFileAccess = true
                    allowContentAccess = true
                    allowFileAccessFromFileURLs = true
                    allowUniversalAccessFromFileURLs = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }

                webViewClient = WebViewClient()

                webChromeClient = object : WebChromeClient() {
                    override fun onPermissionRequest(request: PermissionRequest?) {
                        request?.let { permissionRequest ->
                            val requestedResources = permissionRequest.resources
                            val cameraPermission = PermissionRequest.RESOURCE_VIDEO_CAPTURE

                            if (requestedResources.contains(cameraPermission)) {
                                permissionRequest.grant(arrayOf(cameraPermission))
                                Timber.d("🎥 Camera permission granted to WebView for authentication")
                            } else {
                                permissionRequest.grant(requestedResources)
                            }
                        }
                    }

                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        consoleMessage?.let {
                            Timber.d("📱 Auth WebView Console: ${it.message()}")
                        }
                        return super.onConsoleMessage(consoleMessage)
                    }
                }

                // Add JavaScript interface for communication with thread safety
                addJavascriptInterface(
                    FaceIoAuthJsInterface(
                        // FIX: Rename parameters to avoid collision
                        onAuthenticatedCallback = onAuthenticated,
                        onErrorCallback = onError
                    ),
                    "AndroidInterface"
                )

                Timber.d("🚀 Loading Face.io authentication WebView")
                loadDataWithBaseURL(
                    "https://localhost",
                    getFaceIoAuthHtml(),
                    "text/html",
                    "UTF-8",
                    null
                )

                webView.value = this
            }
        }
    )
}

class FaceIoAuthJsInterface(
    // FIX: Renamed parameters to avoid method name collision
    private val onAuthenticatedCallback: (rollNumber: String) -> Unit,
    private val onErrorCallback: (String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var hasAuthenticated = false // Prevent multiple callbacks

    @JavascriptInterface
    fun onAuthenticated(rollNumber: String) {
        if (hasAuthenticated) {
            Timber.d("🔄 Authentication already processed, ignoring duplicate call")
            return
        }

        hasAuthenticated = true
        Timber.d("✅ User authenticated with roll number: $rollNumber")

        // Use Handler to ensure execution on main thread
        mainHandler.post {
            try {
                Timber.d("🔥 CALLING ATTENDANCE CALLBACK FOR: $rollNumber")
                onAuthenticatedCallback(rollNumber) // FIX: Now calls the callback, not itself
            } catch (e: Exception) {
                Timber.e(e, "❌ Error in onAuthenticated callback")
            }
        }
    }

    @JavascriptInterface
    fun onError(error: String) {
        Timber.e("❌ Authentication error: $error")
        // Use Handler to ensure execution on main thread
        mainHandler.post {
            try {
                onErrorCallback(error)
            } catch (e: Exception) {
                Timber.e(e, "❌ Error in onError callback")
            }
        }
    }

    @JavascriptInterface
    fun log(message: String) {
        Timber.d("📱 Auth WebView JS: $message")
    }
}

private fun getFaceIoAuthHtml(): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Face Authentication</title>
            <style>
                body {
                    margin: 0;
                    padding: 20px;
                    font-family: Arial, sans-serif;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                    min-height: 100vh;
                    background-color: #f5f5f5;
                }
                .container {
                    text-align: center;
                    background: white;
                    padding: 30px;
                    border-radius: 10px;
                    box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                    max-width: 400px;
                    width: 100%;
                }
                h2 {
                    color: #333;
                    margin-bottom: 20px;
                }
                .info {
                    color: #666;
                    margin-bottom: 20px;
                }
                .status {
                    margin-top: 20px;
                    padding: 10px;
                    border-radius: 5px;
                    font-weight: bold;
                }
                .success {
                    background-color: #d4edda;
                    color: #155724;
                }
                .error {
                    background-color: #f8d7da;
                    color: #721c24;
                }
                .loading {
                    background-color: #cce5ff;
                    color: #004085;
                }
                .button {
                    background-color: #007bff;
                    color: white;
                    border: none;
                    padding: 10px 20px;
                    border-radius: 5px;
                    cursor: pointer;
                    margin-top: 10px;
                }
                .button:disabled {
                    background-color: #6c757d;
                    cursor: not-allowed;
                }
            </style>
        </head>
        <body>
            <div id="faceio-modal"></div>
            
            <div class="container">
                <h2>Attendance Verification</h2>
                <p class="info">Look at the camera to mark attendance</p>
                <div id="status" class="status loading">Initializing Face.io...</div>
                <button id="startButton" class="button" style="display: none;">Start Face Scan</button>
            </div>
            
            <script src="https://cdn.faceio.net/fio.js"></script>
            
            <script type="text/javascript">
                let faceio = null;
                let isAuthenticating = false;
                let hasAuthenticated = false;
                const statusDiv = document.getElementById('status');
                const startButton = document.getElementById('startButton');
                
                function log(message) {
                    console.log("🔥 FACE.IO: " + message);
                    if (window.AndroidInterface && window.AndroidInterface.log) {
                        try {
                            window.AndroidInterface.log(message);
                        } catch (e) {
                            console.error('Error calling AndroidInterface.log:', e);
                        }
                    }
                }
                
                // Enhanced callback function with better error handling
                function safeCallback(callbackName, data, delay = 100) {
                    if (hasAuthenticated && callbackName === 'onAuthenticated') {
                        log('🔄 Authentication already processed, preventing duplicate callback');
                        return;
                    }
                    
                    log('🚀 Preparing to call ' + callbackName + ' with data: ' + data);
                    
                    setTimeout(() => {
                        try {
                            if (window.AndroidInterface && window.AndroidInterface[callbackName]) {
                                log('📞 Calling AndroidInterface.' + callbackName + ' with data: ' + data);
                                window.AndroidInterface[callbackName](data);
                                
                                if (callbackName === 'onAuthenticated') {
                                    hasAuthenticated = true;
                                    log('✅ Authentication callback completed successfully');
                                }
                            } else {
                                const errorMsg = 'AndroidInterface.' + callbackName + ' not available';
                                log('❌ ' + errorMsg);
                                console.error(errorMsg);
                            }
                        } catch (error) {
                            const errorMsg = 'Error calling ' + callbackName + ': ' + error.message;
                            log('❌ ' + errorMsg);
                            console.error('Callback error:', error);
                        }
                    }, delay);
                }
                
                window.addEventListener('load', function() {
                    log('📱 Page loaded, initializing Face.io for authentication...');
                    
                    setTimeout(function() {
                        try {
                            log('🔧 Creating faceIO instance...');
                            faceio = new faceIO('fioa264a');
                            log('✅ Face.io initialized successfully');
                            
                            statusDiv.innerHTML = 'Face.io ready. Click button to scan face.';
                            statusDiv.className = 'status loading';
                            
                            startButton.style.display = 'block';
                            startButton.addEventListener('click', startAuthentication);
                            
                        } catch (error) {
                            const errorMsg = 'Failed to initialize Face.io: ' + error.message;
                            log('❌ ' + errorMsg);
                            statusDiv.innerHTML = errorMsg;
                            statusDiv.className = 'status error';
                            
                            safeCallback('onError', 'Initialization failed: ' + error.message);
                        }
                    }, 2000);
                });
                
                function startAuthentication() {
                    if (isAuthenticating || hasAuthenticated) {
                        log('⏸️ Authentication already in progress or completed');
                        return;
                    }
                    
                    log('🎯 Starting authentication process...');
                    isAuthenticating = true;
                    startButton.style.display = 'none';
                    statusDiv.innerHTML = 'Scanning face...';
                    statusDiv.className = 'status loading';
                    
                    setTimeout(function() {
                        authenticateUser();
                    }, 500);
                }
                
                function authenticateUser() {
                    if (!faceio || hasAuthenticated) {
                        if (hasAuthenticated) {
                            log('✅ Authentication already completed');
                            return;
                        }
                        log('❌ Face.io not initialized');
                        statusDiv.innerHTML = 'Face.io not initialized';
                        statusDiv.className = 'status error';
                        return;
                    }
                    
                    log('🔍 Calling faceio.authenticate()...');
                    
                    faceio.authenticate({
                        locale: "auto"
                    }).then(userData => {
                        if (hasAuthenticated) {
                            log('🔄 Authentication already processed, ignoring result');
                            return;
                        }
                        
                        log('🎉 Authentication successful!');
                        log('📋 Payload: ' + JSON.stringify(userData.payload));
                        
                        const rollNumber = userData.payload ? userData.payload.rollNumber : 'Unknown';
                        log('🆔 Extracted Roll Number: ' + rollNumber);
                        
                        statusDiv.innerHTML = 'Face recognized! Roll Number: ' + rollNumber + '<br>Processing attendance...';
                        statusDiv.className = 'status success';
                        
                        // Disable further authentication attempts
                        isAuthenticating = false;
                        startButton.disabled = true;
                        startButton.textContent = 'Authentication Complete';
                        
                        // Use safe callback with delay
                        log('📞 Calling success callback...');
                        safeCallback('onAuthenticated', rollNumber, 200);
                        
                    }).catch(errCode => {
                        if (hasAuthenticated) {
                            log('✅ Authentication already completed, ignoring error');
                            return;
                        }
                        
                        log('❌ Authentication failed with error code: ' + errCode);
                        let errorMessage = handleError(errCode);
                        statusDiv.innerHTML = 'Error: ' + errorMessage;
                        statusDiv.className = 'status error';
                        
                        isAuthenticating = false;
                        startButton.textContent = 'Try Again';
                        startButton.style.display = 'block';
                        
                        // Use safe callback with delay
                        log('📞 Calling error callback...');
                        safeCallback('onError', errorMessage, 200);
                    });
                }
                
                function handleError(errCode) {
                    const errorMessages = {
                        1: "Camera permission denied",
                        2: "No face detected. Please position your face in the camera",
                        3: "Face not recognized. Please register first",
                        4: "Multiple faces detected. Please ensure only one face is visible",
                        5: "Face spoofing detected",
                        7: "Network error. Please check your connection",
                        8: "Wrong PIN code",
                        9: "Processing error. Please try again",
                        10: "Unauthorized. Please check application settings",
                        11: "Terms not accepted",
                        12: "UI not ready. Please refresh and try again",
                        13: "Session expired. Please try again",
                        14: "Operation timed out. Please try again",
                        15: "Too many requests. Please wait a moment"
                    };
                    
                    log('📋 Face.io error code: ' + errCode);
                    return errorMessages[errCode] || "Unknown error occurred (Code: " + errCode + ")";
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}