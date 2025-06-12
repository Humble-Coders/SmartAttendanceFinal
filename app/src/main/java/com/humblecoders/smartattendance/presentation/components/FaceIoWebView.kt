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
fun FaceIoWebView(
    modifier: Modifier = Modifier,
    rollNumber: String,
    onFaceRegistered: (String) -> Unit,
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
                    // Additional settings for camera access
                    allowFileAccessFromFileURLs = true
                    allowUniversalAccessFromFileURLs = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }

                webViewClient = WebViewClient()

                // Set WebChromeClient to handle camera permissions
                webChromeClient = object : WebChromeClient() {
                    override fun onPermissionRequest(request: PermissionRequest?) {
                        request?.let { permissionRequest ->
                            // Log the requested permissions
                            Timber.d("WebView permission request: ${permissionRequest.resources.joinToString()}")

                            // Check if camera permission is requested
                            val requestedResources = permissionRequest.resources
                            val cameraPermission = PermissionRequest.RESOURCE_VIDEO_CAPTURE

                            if (requestedResources.contains(cameraPermission)) {
                                // Grant camera permission to WebView
                                permissionRequest.grant(arrayOf(cameraPermission))
                                Timber.d("Camera permission granted to WebView")
                            } else {
                                // Grant all requested permissions (might include audio)
                                permissionRequest.grant(requestedResources)
                            }
                        }
                    }

                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        consoleMessage?.let {
                            Timber.d("WebView Console: ${it.message()}")
                        }
                        return super.onConsoleMessage(consoleMessage)
                    }
                }

                // Add JavaScript interface for communication
                addJavascriptInterface(
                    FaceIoJsInterface(
                        onFaceRegistered = onFaceRegistered,
                        onError = onError
                    ),
                    "AndroidInterface"
                )

                // Load Face.io HTML with proper origin
                loadDataWithBaseURL(
                    "https://localhost",
                    getFaceIoHtml(rollNumber),
                    "text/html",
                    "UTF-8",
                    null
                )

                webView.value = this
            }
        },
        update = { webView ->
            // Cleanup when composable is disposed
        }
    )
}

class FaceIoJsInterface(
    private val onFaceRegistered: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    @JavascriptInterface
    fun onFaceRegistered(faceId: String) {
        Timber.d("Face registered with ID: $faceId")
        onFaceRegistered(faceId)
    }

    @JavascriptInterface
    fun onError(error: String) {
        Timber.e("Face registration error: $error")
        onError(error)
    }

    @JavascriptInterface
    fun log(message: String) {
        Timber.d("WebView JS: $message")
    }
}

private fun getFaceIoHtml(rollNumber: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Face Registration</title>
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
                <h2>Face Registration</h2>
                <p class="info">Position your face in the camera frame</p>
                <p class="info">Roll Number: $rollNumber</p>
                <div id="status" class="status loading">Initializing Face.io...</div>
                <button id="startButton" class="button" style="display: none;">Start Face Registration</button>
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
                    log('Page loaded, initializing Face.io...');
                    
                    setTimeout(function() {
                        try {
                            // Initialize Face.io with your Public ID
                            faceio = new faceIO('fioa264a');
                            log('Face.io initialized successfully');
                            
                            statusDiv.innerHTML = 'Face.io ready. Click button to start.';
                            statusDiv.className = 'status loading';
                            
                            // Show start button
                            startButton.style.display = 'block';
                            startButton.addEventListener('click', startEnrollment);
                            
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
                
                function startEnrollment() {
                    log('Starting enrollment...');
                    startButton.style.display = 'none';
                    statusDiv.innerHTML = 'Starting face enrollment...';
                    statusDiv.className = 'status loading';
                    
                    // Small delay before starting enrollment
                    setTimeout(function() {
                        enrollNewUser();
                    }, 500);
                }
                
                function enrollNewUser() {
                    if (!faceio) {
                        log('Face.io not initialized');
                        statusDiv.innerHTML = 'Face.io not initialized';
                        statusDiv.className = 'status error';
                        return;
                    }
                    
                    log('Calling faceio.enroll()...');
                    
                    faceio.enroll({
                        locale: "auto",
                        payload: {
                            rollNumber: "$rollNumber"
                        },
                        userConsent: false // Skip consent screen as we already have permission
                    }).then(userInfo => {
                        log('Enrollment successful! FacialId: ' + userInfo.facialId);
                        statusDiv.innerHTML = 'Face registered successfully!';
                        statusDiv.className = 'status success';
                        
                        // Send success to Android
                        if (window.AndroidInterface) {
                            window.AndroidInterface.onFaceRegistered(userInfo.facialId);
                        }
                        
                    }).catch(errCode => {
                        log('Enrollment failed with error code: ' + errCode);
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
                        1: "Camera permission denied. Please ensure camera access is granted.",
                        2: "No face detected. Please position your face in the camera",
                        3: "Face not recognized",
                        4: "Multiple faces detected. Please ensure only one face is visible",
                        5: "Face spoofing detected",
                        6: "Face mismatch during enrollment",
                        7: "Network error. Please check your connection",
                        8: "Wrong PIN code",
                        9: "Processing error. Please try again",
                        10: "Unauthorized. Please check application settings",
                        11: "Terms not accepted",
                        12: "UI not ready. Please refresh and try again",
                        13: "Session expired. Please try again",
                        14: "Operation timed out. Please try again",
                        15: "Too many requests. Please wait a moment",
                        16: "Empty origin error",
                        17: "Origin not allowed",
                        18: "Country not allowed",
                        19: "Another session in progress",
                        20: "Face already enrolled"
                    };
                    
                    return errorMessages[errCode] || "Unknown error occurred (Code: " + errCode + ")";
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}