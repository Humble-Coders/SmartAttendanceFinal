package com.humblecoders.smartattendance.presentation.components

import android.annotation.SuppressLint
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
                        onError = onError
                    ),
                    "AndroidInterface"
                )

                // Load Face.io authentication HTML
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
    private val onAuthenticated: (rollNumber: String) -> Unit,
    private val onError: (String) -> Unit
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
    fun log(message: String) {
        Timber.d("Auth WebView JS: $message")
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
            </style>
        </head>
        <body>
            <!-- FACEIO Widget will be injected here -->
            <div id="faceio-modal"></div>
            
            <div class="container">
                <h2>Attendance Verification</h2>
                <p class="info">Look at the camera to mark attendance</p>
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
                
                let faceio = null;
                const statusDiv = document.getElementById('status');
                const startButton = document.getElementById('startButton');
                
                // Initialize Face.io when page loads
                window.addEventListener('load', function() {
                    log('Page loaded, initializing Face.io for authentication...');
                    
                    setTimeout(function() {
                        try {
                            // Initialize Face.io with your Public ID
                            faceio = new faceIO('fioa264a');
                            log('Face.io initialized successfully');
                            
                            statusDiv.innerHTML = 'Face.io ready. Click button to scan face.';
                            statusDiv.className = 'status loading';
                            
                            // Show start button
                            startButton.style.display = 'block';
                            startButton.addEventListener('click', startAuthentication);
                            
                        } catch (error) {
                            log('Failed to initialize Face.io: ' + error.message);
                            statusDiv.innerHTML = 'Failed to initialize Face.io: ' + error.message;
                            statusDiv.className = 'status error';
                            
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