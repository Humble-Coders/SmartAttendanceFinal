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
fun StudentFaceRegistrationWebView(
    modifier: Modifier = Modifier,
    rollNumber: String,
    onFaceRegistered: (String) -> Unit,
    onError: (String) -> Unit,
    onClose: () -> Unit
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
                                Timber.d("🎥 Camera permission granted to Student Registration WebView")
                            } else {
                                permissionRequest.grant(requestedResources)
                            }
                        }
                    }

                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        consoleMessage?.let {
                            Timber.d("📱 Student Registration WebView Console: ${it.message()}")
                        }
                        return super.onConsoleMessage(consoleMessage)
                    }
                }

                // Add JavaScript interface for communication
                addJavascriptInterface(
                    StudentFaceRegistrationJsInterface(
                        onFaceRegisteredCallback = onFaceRegistered,
                        onErrorCallback = onError,
                        onCloseCallback = onClose
                    ),
                    "AndroidInterface"
                )

                Timber.d("🚀 Loading Student Face.io registration WebView for roll number: $rollNumber")
                loadDataWithBaseURL(
                    "https://localhost",
                    getStudentFaceRegistrationHtml(rollNumber),
                    "text/html",
                    "UTF-8",
                    null
                )

                webView.value = this
            }
        }
    )
}

class StudentFaceRegistrationJsInterface(
    private val onFaceRegisteredCallback: (String) -> Unit,
    private val onErrorCallback: (String) -> Unit,
    private val onCloseCallback: () -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var hasRegistered = false

    @JavascriptInterface
    fun onFaceRegistered(faceId: String) {
        if (hasRegistered) {
            Timber.d("🔄 Registration already processed, ignoring duplicate call")
            return
        }

        hasRegistered = true
        Timber.d("✅ Student face registered successfully with ID: $faceId")

        mainHandler.post {
            try {
                Timber.d("🔥 CALLING STUDENT REGISTRATION SUCCESS CALLBACK FOR: $faceId")
                onFaceRegisteredCallback(faceId)
            } catch (e: Exception) {
                Timber.e(e, "❌ Error in onFaceRegistered callback")
            }
        }
    }

    @JavascriptInterface
    fun onError(error: String) {
        Timber.e("❌ Student face registration error: $error")
        mainHandler.post {
            try {
                onErrorCallback(error)
            } catch (e: Exception) {
                Timber.e(e, "❌ Error in onError callback")
            }
        }
    }

    @JavascriptInterface
    fun onClose() {
        Timber.d("🔙 Student registration WebView closed")
        mainHandler.post {
            try {
                onCloseCallback()
            } catch (e: Exception) {
                Timber.e(e, "❌ Error in onClose callback")
            }
        }
    }

    @JavascriptInterface
    fun log(message: String) {
        Timber.d("📱 Student Registration WebView JS: $message")
    }
}

private fun getStudentFaceRegistrationHtml(rollNumber: String): String {
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
                    background: linear-gradient(135deg, #007AFF 0%, #5856D6 100%);
                }
                .container {
                    text-align: center;
                    background: white;
                    padding: 30px;
                    border-radius: 15px;
                    box-shadow: 0 10px 30px rgba(0,0,0,0.2);
                    max-width: 400px;
                    width: 100%;
                }
                h2 {
                    color: #333;
                    margin-bottom: 10px;
                }
                .subtitle {
                    color: #666;
                    margin-bottom: 20px;
                    font-size: 18px;
                    font-weight: bold;
                }
                .info {
                    color: #666;
                    margin-bottom: 20px;
                }
                .status {
                    margin-top: 20px;
                    padding: 15px;
                    border-radius: 8px;
                    font-weight: bold;
                }
                .success {
                    background-color: #d4edda;
                    color: #155724;
                    border: 1px solid #c3e6cb;
                }
                .error {
                    background-color: #f8d7da;
                    color: #721c24;
                    border: 1px solid #f5c6cb;
                }
                .loading {
                    background-color: #cce5ff;
                    color: #004085;
                    border: 1px solid #b8daff;
                }
                .button {
                    background: linear-gradient(135deg, #007AFF 0%, #5856D6 100%);
                    color: white;
                    border: none;
                    padding: 12px 24px;
                    border-radius: 8px;
                    cursor: pointer;
                    margin-top: 15px;
                    font-size: 16px;
                    font-weight: bold;
                    transition: transform 0.2s;
                }
                .button:hover {
                    transform: translateY(-2px);
                }
                .button:disabled {
                    background-color: #6c757d;
                    cursor: not-allowed;
                    transform: none;
                }
                .roll-number {
                    background-color: #e3f2fd;
                    color: #1565c0;
                    padding: 10px 20px;
                    border-radius: 8px;
                    font-weight: bold;
                    font-size: 18px;
                    margin-bottom: 20px;
                    border: 2px solid #1976d2;
                }
                .close-button {
                    background-color: #6c757d;
                    color: white;
                    border: none;
                    padding: 8px 16px;
                    border-radius: 6px;
                    cursor: pointer;
                    margin-top: 10px;
                    font-size: 14px;
                }
            </style>
        </head>
        <body>
            <div id="faceio-modal"></div>
            
            <div class="container">
                <h2>🎓 Student Face Registration</h2>
                <p class="subtitle">Create Your Face Profile</p>
                
                <div class="roll-number">
                    Roll Number: $rollNumber
                </div>
                
                <p class="info">Position your face in the camera frame to register</p>
                <div id="status" class="status loading">Initializing Face.io...</div>
                <button id="startButton" class="button" style="display: none;">Start Face Registration</button>
                <button id="closeButton" class="close-button" onclick="closeRegistration()" style="display: none;">Cancel Registration</button>
            </div>
            
            <script src="https://cdn.faceio.net/fio.js"></script>
            
            <script type="text/javascript">
                let faceio = null;
                let isRegistering = false;
                let hasRegistered = false;
                const statusDiv = document.getElementById('status');
                const startButton = document.getElementById('startButton');
                const closeButton = document.getElementById('closeButton');
                
                function log(message) {
                    console.log("🔥 STUDENT FACE.IO: " + message);
                    if (window.AndroidInterface && window.AndroidInterface.log) {
                        try {
                            window.AndroidInterface.log(message);
                        } catch (e) {
                            console.error('Error calling AndroidInterface.log:', e);
                        }
                    }
                }
                
                function safeCallback(callbackName, data, delay = 100) {
                    if (hasRegistered && callbackName === 'onFaceRegistered') {
                        log('🔄 Registration already processed, preventing duplicate callback');
                        return;
                    }
                    
                    log('🚀 Preparing to call ' + callbackName + ' with data: ' + data);
                    
                    setTimeout(() => {
                        try {
                            if (window.AndroidInterface && window.AndroidInterface[callbackName]) {
                                log('📞 Calling AndroidInterface.' + callbackName + ' with data: ' + data);
                                window.AndroidInterface[callbackName](data);
                                
                                if (callbackName === 'onFaceRegistered') {
                                    hasRegistered = true;
                                    log('✅ Registration callback completed successfully');
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
                
                function closeRegistration() {
                    log('🔙 User closed registration');
                    safeCallback('onClose', 'user_closed');
                }
                
                window.addEventListener('load', function() {
                    log('📱 Student registration page loaded, initializing Face.io...');
                    
                    setTimeout(function() {
                        try {
                            log('🔧 Creating faceIO instance...');
                            faceio = new faceIO('fioa3e64'); // Use your Face.io app ID
                            log('✅ Student Face.io initialized successfully');
                            
                            statusDiv.innerHTML = 'Face.io ready. Click button to start registration.';
                            statusDiv.className = 'status loading';
                            
                            startButton.style.display = 'block';
                            closeButton.style.display = 'block';
                            startButton.addEventListener('click', startEnrollment);
                            
                        } catch (error) {
                            const errorMsg = 'Failed to initialize Face.io: ' + error.message;
                            log('❌ ' + errorMsg);
                            statusDiv.innerHTML = errorMsg;
                            statusDiv.className = 'status error';
                            
                            safeCallback('onError', 'Initialization failed: ' + error.message);
                        }
                    }, 2000);
                });
                
                function startEnrollment() {
                    if (isRegistering || hasRegistered) {
                        log('⏸️ Registration already in progress or completed');
                        return;
                    }
                    
                    log('🎯 Starting enrollment process for roll number: $rollNumber');
                    isRegistering = true;
                    startButton.style.display = 'none';
                    closeButton.style.display = 'none';
                    statusDiv.innerHTML = 'Starting face enrollment for $rollNumber...';
                    statusDiv.className = 'status loading';
                    
                    setTimeout(function() {
                        enrollNewUser();
                    }, 500);
                }
                
                function enrollNewUser() {
                    if (!faceio || hasRegistered) {
                        if (hasRegistered) {
                            log('✅ Registration already completed');
                            return;
                        }
                        log('❌ Face.io not initialized');
                        statusDiv.innerHTML = 'Face.io not initialized';
                        statusDiv.className = 'status error';
                        return;
                    }
                    
                    log('🔍 Calling faceio.enroll() for roll number: $rollNumber');
                    
                    faceio.enroll({
                        locale: "auto",
                        payload: {
                            rollNumber: "$rollNumber",
                            registeredBy: "student",
                            registrationDate: new Date().toISOString()
                        },
                        userConsent: false,
                        enrollIntroTimeout: 1,
                        noBoardingPass: true
                    }).then(userInfo => {
                        if (hasRegistered) {
                            log('🔄 Registration already processed, ignoring result');
                            return;
                        }
                        
                        log('🎉 Registration successful!');
                        log('📋 FacialId: ' + userInfo.facialId + ' for roll number: $rollNumber');
                        
                        statusDiv.innerHTML = 'Face registered successfully for $rollNumber!<br>Face ID: ' + userInfo.facialId;
                        statusDiv.className = 'status success';
                        
                        isRegistering = false;
                        startButton.disabled = true;
                        startButton.textContent = 'Registration Complete';
                        
                        log('📞 Calling success callback...');
                        safeCallback('onFaceRegistered', userInfo.facialId, 500);
                        
                    }).catch(errCode => {
                        if (hasRegistered) {
                            log('✅ Registration already completed, ignoring error');
                            return;
                        }
                        
                        log('❌ Registration failed with error code: ' + errCode);
                        let errorMessage = handleError(errCode);
                        statusDiv.innerHTML = 'Error: ' + errorMessage;
                        statusDiv.className = 'status error';
                        
                        isRegistering = false;
                        startButton.textContent = 'Try Again';
                        startButton.style.display = 'block';
                        closeButton.style.display = 'block';
                        
                        log('📞 Calling error callback...');
                        safeCallback('onError', errorMessage, 200);
                    });
                }
                
                function handleError(errCode) {
                    const errorMessages = {
                        1: "Camera permission denied",
                        2: "No face detected. Please position face in camera",
                        3: "Face not recognized during enrollment",
                        4: "Multiple faces detected. Please ensure only one face is visible",
                        5: "Face already enrolled. You are already registered!",
                        6: "Minors not allowed for registration",
                        7: "Presentation attack detected",
                        8: "Face mismatch during enrollment process",
                        9: "Wrong PIN code",
                        10: "Processing error. Please try again",
                        11: "Unauthorized. Please check application settings",
                        12: "Terms not accepted",
                        13: "UI not ready. Please refresh and try again",
                        14: "Session expired. Please try again",
                        15: "Operation timed out. Please try again",
                        16: "Too many requests. Please wait a moment",
                        17: "Empty origin error",
                        18: "Forbidden origin",
                        19: "Forbidden country",
                        20: "Session in progress",
                        21: "Network error. Please check connection"
                    };
                    
                    log('📋 Face.io error code: ' + errCode);
                    return errorMessages[errCode] || "Unknown error occurred (Code: " + errCode + ")";
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}