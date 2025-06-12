package com.humblecoders.smartattendance.presentation.components

import android.annotation.SuppressLint
import android.webkit.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import timber.log.Timber

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FaceIoAuthWebView(
    modifier: Modifier = Modifier,
    onAuthenticated: (rollNumber: String) -> Unit,
    onError: (String) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var loadingMessage by remember { mutableStateOf("Initializing Face.io...") }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
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

                    // Set WebChromeClient to handle camera permissions
                    webChromeClient = object : WebChromeClient() {
                        override fun onPermissionRequest(request: PermissionRequest?) {
                            request?.let { permissionRequest ->
                                val requestedResources = permissionRequest.resources
                                val cameraPermission = PermissionRequest.RESOURCE_VIDEO_CAPTURE

                                if (requestedResources.contains(cameraPermission)) {
                                    permissionRequest.grant(arrayOf(cameraPermission))
                                    Timber.d("Camera permission granted to WebView for authentication")
                                } else {
                                    permissionRequest.grant(requestedResources)
                                }
                            }
                        }

                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            consoleMessage?.let {
                                Timber.d("Auth WebView Console: ${it.message()}")
                            }
                            return super.onConsoleMessage(consoleMessage)
                        }
                    }

                    // Add JavaScript interface for communication
                    addJavascriptInterface(
                        FaceIoAuthJsInterface(
                            onAuthenticated = onAuthenticated,
                            onError = onError,
                            onLoadingStateChange = { loading, message ->
                                isLoading = loading
                                loadingMessage = message
                            }
                        ),
                        "AndroidInterface"
                    )

                    // Load Face.io authentication HTML
                    loadDataWithBaseURL(
                        "https://localhost",
                        getEnhancedFaceIoAuthHtml(),
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            }
        )

        // Loading overlay
        if (isLoading) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.Center),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = loadingMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

class FaceIoAuthJsInterface(
    private val onAuthenticated: (rollNumber: String) -> Unit,
    private val onError: (String) -> Unit,
    private val onLoadingStateChange: (Boolean, String) -> Unit
) {
    @JavascriptInterface
    fun onAuthenticated(rollNumber: String) {
        Timber.d("User authenticated with roll number: $rollNumber")
        onAuthenticated(rollNumber)
    }

    @JavascriptInterface
    fun onError(error: String) {
        Timber.e("Authentication error: $error")
        onError(error)
    }

    @JavascriptInterface
    fun onLoadingStateChange(isLoading: Boolean, message: String) {
        onLoadingStateChange(isLoading, message)
    }

    @JavascriptInterface
    fun log(message: String) {
        Timber.d("Auth WebView JS: $message")
    }
}

private fun getEnhancedFaceIoAuthHtml(): String {
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
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                    min-height: 100vh;
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    color: white;
                }
                .container {
                    text-align: center;
                    background: rgba(255,255,255,0.1);
                    backdrop-filter: blur(10px);
                    padding: 30px;
                    border-radius: 20px;
                    box-shadow: 0 8px 32px rgba(0,0,0,0.3);
                    max-width: 400px;
                    width: 100%;
                    border: 1px solid rgba(255,255,255,0.2);
                }
                h2 {
                    margin-bottom: 20px;
                    font-size: 1.5em;
                    font-weight: 600;
                }
                .info {
                    margin-bottom: 20px;
                    font-size: 1.1em;
                    opacity: 0.9;
                }
                .status {
                    margin-top: 20px;
                    padding: 15px;
                    border-radius: 10px;
                    font-weight: 500;
                    transition: all 0.3s ease;
                }
                .success {
                    background: rgba(76, 175, 80, 0.3);
                    border: 1px solid rgba(76, 175, 80, 0.5);
                }
                .error {
                    background: rgba(244, 67, 54, 0.3);
                    border: 1px solid rgba(244, 67, 54, 0.5);
                }
                .loading {
                    background: rgba(33, 150, 243, 0.3);
                    border: 1px solid rgba(33, 150, 243, 0.5);
                }
                .button {
                    background: linear-gradient(45deg, #2196F3, #21CBF3);
                    color: white;
                    border: none;
                    padding: 12px 24px;
                    border-radius: 25px;
                    cursor: pointer;
                    margin-top: 15px;
                    font-size: 1em;
                    font-weight: 500;
                    transition: all 0.3s ease;
                    box-shadow: 0 4px 15px rgba(33, 150, 243, 0.3);
                }
                .button:hover {
                    transform: translateY(-2px);
                    box-shadow: 0 6px 20px rgba(33, 150, 243, 0.4);
                }
                .button:disabled {
                    opacity: 0.6;
                    cursor: not-allowed;
                    transform: none;
                }
                .icon {
                    font-size: 3em;
                    margin-bottom: 15px;
                }
                .pulse {
                    animation: pulse 2s infinite;
                }
                @keyframes pulse {
                    0% { opacity: 1; }
                    50% { opacity: 0.7; }
                    100% { opacity: 1; }
                }
            </style>
        </head>
        <body>
            <!-- FACEIO Widget will be injected here -->
            <div id="faceio-modal"></div>
            
            <div class="container">
                <div class="icon pulse">🔐</div>
                <h2>Face Authentication</h2>
                <p class="info">Position your face in the camera frame to mark attendance</p>
                <div id="status" class="status loading">Initializing Face.io...</div>
                <button id="startButton" class="button" style="display: none;">Start Face Scan</button>
            </div>
            
            <!-- Load Face.io JavaScript library -->
            <script src="https://cdn.faceio.net/fio.js"></script>
            
            <script type="text/javascript">
                // Helper function to log to Android
                function log(message) {
                    console.log(message);
                    if (window.AndroidInterface && window.AndroidInterface.log) {
                        window.AndroidInterface.log(message);
                    }
                }
                
                function updateLoadingState(isLoading, message) {
                    if (window.AndroidInterface && window.AndroidInterface.onLoadingStateChange) {
                        window.AndroidInterface.onLoadingStateChange(isLoading, message);
                    }
                }
                
                let faceio = null;
                const statusDiv = document.getElementById('status');
                const startButton = document.getElementById('startButton');
                
                // Initialize Face.io when page loads
                window.addEventListener('load', function() {
                    log('Page loaded, initializing Face.io for authentication...');
                    updateLoadingState(true, 'Initializing Face.io...');
                    
                    setTimeout(function() {
                        try {
                            // Initialize Face.io with your Public ID
                            faceio = new faceIO('fioa264a');
                            log('Face.io initialized successfully');
                            
                            statusDiv.innerHTML = 'Ready for face scan. Click button to start.';
                            statusDiv.className = 'status loading';
                            
                            // Show start button
                            startButton.style.display = 'block';
                            startButton.addEventListener('click', startAuthentication);
                            
                            updateLoadingState(false, '');
                            
                        } catch (error) {
                            log('Failed to initialize Face.io: ' + error.message);
                            statusDiv.innerHTML = 'Failed to initialize Face.io: ' + error.message;
                            statusDiv.className = 'status error';
                            
                            updateLoadingState(false, '');
                            
                            if (window.AndroidInterface) {
                                window.AndroidInterface.onError('Initialization failed: ' + error.message);
                            }
                        }
                    }, 2000);
                });
                
                function startAuthentication() {
                    log('Starting authentication...');
                    startButton.style.display = 'none';
                    statusDiv.innerHTML = 'Scanning face...';
                    statusDiv.className = 'status loading';
                    
                    updateLoadingState(true, 'Scanning face...');
                    
                    // Small delay before starting authentication
                    setTimeout(function() {
                        authenticateUser();
                    }, 500);
                }
                
                function authenticateUser() {
                    if (!faceio) {
                        log('Face.io not initialized');
                        statusDiv.innerHTML = 'Face.io not initialized';
                        statusDiv.className = 'status error';
                        updateLoadingState(false, '');
                        return;
                    }
                    
                    log('Calling faceio.authenticate()...');
                    
                    faceio.authenticate({
                        locale: "auto"
                    }).then(userData => {
                        log('Authentication successful!');
                        log('Payload: ' + JSON.stringify(userData.payload));
                        
                        // Extract roll number from payload
                        const rollNumber = userData.payload ? userData.payload.rollNumber : 'Unknown';
                        
                        statusDiv.innerHTML = 'Face recognized! Roll Number: ' + rollNumber;
                        statusDiv.className = 'status success';
                        
                        updateLoadingState(false, '');
                        
                        // Send roll number to Android
                        if (window.AndroidInterface) {
                            window.AndroidInterface.onAuthenticated(rollNumber);
                        }
                        
                    }).catch(errCode => {
                        log('Authentication failed with error code: ' + errCode);
                        let errorMessage = handleError(errCode);
                        statusDiv.innerHTML = 'Error: ' + errorMessage;
                        statusDiv.className = 'status error';
                        
                        // Show retry button
                        startButton.textContent = 'Try Again';
                        startButton.style.display = 'block';
                        
                        updateLoadingState(false, '');
                        
                        // Send error to Android
                        if (window.AndroidInterface) {
                            window.AndroidInterface.onError(errorMessage);
                        }
                    });
                }
                
                function handleError(errCode) {
                    // Map Face.io error codes to user-friendly messages
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
                    
                    return errorMessages[errCode] || "Unknown error occurred (Code: " + errCode + ")";
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}