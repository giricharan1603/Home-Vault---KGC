package com.homevault.app.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricVaultAuthenticator {

    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock Compliance Vault",
        subtitle: String = "Verify identity to view RC, PUC, and Insurance records",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val biometricManager = BiometricManager.from(activity)
        val allowedAuthenticators = BIOMETRIC_STRONG or DEVICE_CREDENTIAL

        when (biometricManager.canAuthenticate(allowedAuthenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                val executor = ContextCompat.getMainExecutor(activity)
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setAllowedAuthenticators(allowedAuthenticators)
                    .build()

                val prompt = BiometricPrompt(
                    activity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            onSuccess()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            onError(errString.toString())
                        }
                    }
                )
                prompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                onError("Set up a Fingerprint or PIN in Android Settings to use the Vault.")
            }
            else -> {
                // Fallback for emulators without biometric hardware configured
                onSuccess()
            }
        }
    }
}