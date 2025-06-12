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
                    allowFileAccessFromFileURLs = true
                    allowUniversalAccessFromFileURLs = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }

                webViewClient = WebViewClient()

                webChromeClient = object : WebChromeClient() {
                    override fun onPermissionRequest(request: PermissionRequest?) {
                        request?.let { permissionRequest ->
                            Timber.d("WebView permission request: ${permissionRequest.resources.joinToString()}")

                            val requestedResources = permissionRequest.resources
                            val cameraPermission = PermissionRequest.RESOURCE_VIDEO_CAPTURE

                            if (requestedResources.contains(cameraPermission)) {
                                permissionRequest.grant(arrayOf(cameraPermission))
                                Timber.d("Camera permission granted to WebView")
                            } else {
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

                // Add JavaScript interface for communication with thread safety
                addJavascriptInterface(
                    FaceIoJsInterface(
                        onFaceRegistered = onFaceRegistered,
                        onError = onError
                    ),
                    "AndroidInterface"
                )

                loadDataWithBaseURL(
                    "https://localhost",
                    getFaceIoHtml(rollNumber),
                    "text/html",
                    "UTF-8",
                    null
                )

                webView.value = this
            }
        }
    )
}

class FaceIoJsInterface(
    private val onFaceRegistered: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onFaceRegistered(faceId: String) {
        Timber.d("Face registered with ID: $faceId")
        // Use Handler to ensure execution on main thread
        mainHandler.post {
            try {
                onFaceRegistered(faceId)
            } catch (e: Exception) {
                Timber.e(e, "Error in onFaceRegistered callback")
            }
        }
    }

    @JavascriptInterface
    fun onError(error: String) {
        Timber.e("Face registration error: $error")
        // Use Handler to ensure execution on main thread
        mainHandler.post {
            try {
                onError(error)
            } catch (e: Exception) {
                Timber.e(e, "Error in onError callback")
            }
        }
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
            <div id="faceio-modal"></div>
            
            <div class="container">
                <h2>Face Registration</h2>
                <p class="info">Position your face in the camera frame</p>
                <p class="info">Roll Number: $rollNumber</p>
                <div id="status" class="status loading">Initializing Face.io...</div>
                <button id="startButton" class="button" style="display: none;">Start Face Registration</button>
            </div>
            
            <script src="https://cdn.faceio.net/fio.js"></script>
            
            <script type="text/javascript">
                function log(message) {
                    console.log(message);
                    if (window.AndroidInterface && window.AndroidInterface.log) {
                        try {
                            window.AndroidInterface.log(message);
                        } catch (e) {
                            console.error('Error calling AndroidInterface.log:', e);
                        }
                    }
                }
                
                // Safe callback function with delayed execution
                function safeCallback(callbackName, data, delay = 100) {
                    setTimeout(() => {
                        try {
                            if (window.AndroidInterface && window.AndroidInterface[callbackName]) {
                                log('Calling ' + callbackName + ' with data: ' + data);
                                window.AndroidInterface[callbackName](data);
                            } else {
                                log('AndroidInterface.' + callbackName + ' not available');
                            }
                        } catch (error) {
                            log('Error calling ' + callbackName + ': ' + error.message);
                            console.error('Callback error:', error);
                        }
                    }, delay);
                }
                
                let faceio = null;
                const statusDiv = document.getElementById('status');
                const startButton = document.getElementById('startButton');
                
                window.addEventListener('load', function() {
                    log('Page loaded, initializing Face.io...');
                    
                    setTimeout(function() {
                        try {
                            faceio = new faceIO('fioa264a');
                            log('Face.io initialized successfully');
                            
                            statusDiv.innerHTML = 'Face.io ready. Click button to start.';
                            statusDiv.className = 'status loading';
                            
                            startButton.style.display = 'block';
                            startButton.addEventListener('click', startEnrollment);
                            
                        } catch (error) {
                            log('Failed to initialize Face.io: ' + error.message);
                            statusDiv.innerHTML = 'Failed to initialize Face.io: ' + error.message;
                            statusDiv.className = 'status error';
                            
                            safeCallback('onError', 'Initialization failed: ' + error.message);
                        }
                    }, 2000);
                });
                
                function startEnrollment() {
                    log('Starting enrollment...');
                    startButton.style.display = 'none';
                    statusDiv.innerHTML = 'Starting face enrollment...';
                    statusDiv.className = 'status loading';
                    
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
                        userConsent: false,
                        enrollIntroTimeout: 15
                    }).then(userInfo => {
                        log('Enrollment successful! FacialId: ' + userInfo.facialId);
                        statusDiv.innerHTML = 'Face registered successfully!';
                        statusDiv.className = 'status success';
                        
                        // Use safe callback with delay
                        safeCallback('onFaceRegistered', userInfo.facialId, 200);
                        
                    }).catch(errCode => {
                        log('Enrollment failed with error code: ' + errCode);
                        let errorMessage = handleError(errCode);
                        statusDiv.innerHTML = 'Error: ' + errorMessage;
                        statusDiv.className = 'status error';
                        
                        startButton.textContent = 'Try Again';
                        startButton.style.display = 'block';
                        
                        // Use safe callback with delay
                        safeCallback('onError', errorMessage, 200);
                    });
                }
                
                function handleError(errCode) {
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
                        20: "Face already enrolled",
                        21: "Invalid PIN code. PIN must be 4-16 digits",
                        22: "Weak PIN code. Please use a stronger PIN",
                        23: "Invalid payload data"
                    };
                    
                    log('Face.io error code: ' + errCode);
                    return errorMessages[errCode] || "Unknown error occurred (Code: " + errCode + ")";
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}