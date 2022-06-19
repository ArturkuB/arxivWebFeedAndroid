package com.example.pracalicencjacka.network

import javax.net.ssl.HttpsURLConnection

import java.security.KeyManagementException

import java.security.NoSuchAlgorithmException

import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

import javax.net.ssl.SSLContext

import javax.net.ssl.TrustManager


import javax.net.ssl.X509TrustManager


class HTTpsTrustManager : X509TrustManager {
    @Throws(CertificateException::class)
    override fun checkClientTrusted(
        x509Certificates: Array<X509Certificate>, s: String
    ) {
    }

    @Throws(CertificateException::class)
    override fun checkServerTrusted(
        x509Certificates: Array<X509Certificate>, s: String
    ) {
    }

    fun isClientTrusted(chain: Array<X509Certificate?>?): Boolean {
        return true
    }

    fun isServerTrusted(chain: Array<X509Certificate?>?): Boolean {
        return true
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return _AcceptedIssuers
    }

    companion object {
        private var trustManagers: Array<TrustManager>? = null
        private val _AcceptedIssuers: Array<X509Certificate> = arrayOf<X509Certificate>()
        fun allowAllSSL() {
            HttpsURLConnection.setDefaultHostnameVerifier { arg0, arg1 -> true }
            var context: SSLContext? = null
            if (trustManagers == null) {
                trustManagers = arrayOf(HTTpsTrustManager())
            }
            try {
                context = SSLContext.getInstance("TLS")
                context.init(null, trustManagers, SecureRandom())
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
            } catch (e: KeyManagementException) {
                e.printStackTrace()
            }
            if (context != null) {
                HttpsURLConnection.setDefaultSSLSocketFactory(
                    context
                        .getSocketFactory()
                )
            }
        }
    }
}