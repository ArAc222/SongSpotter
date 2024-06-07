package com.example.musicrecognitionapp

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.acrcloud.rec.ACRCloudClient
import com.acrcloud.rec.ACRCloudConfig
import com.acrcloud.rec.ACRCloudResult
import com.acrcloud.rec.IACRCloudListener
import org.json.JSONObject

class MusicRecognition(private val context: Context) {
    private var client: ACRCloudClient? = null
    private var config: ACRCloudConfig? = null
    private var handler: Handler? = null

    init {
        handler = Handler(Looper.getMainLooper())
    }

    fun initACRCloud(onResult: (String, Boolean) -> Unit) {
        config = ACRCloudConfig().apply {
            acrcloudListener = object : IACRCloudListener {
                override fun onResult(results: ACRCloudResult?) {
                    handler?.post {
                        results?.let {
                            val resultJson = JSONObject(it.result)
                            val status = resultJson.getJSONObject("status")
                            val code = status.getInt("code")
                            if (code == 0) {
                                val metadata = resultJson.getJSONObject("metadata")
                                val music = metadata.getJSONArray("music").getJSONObject(0)
                                val title = music.getString("title")
                                val artist = music.getJSONArray("artists").getJSONObject(0).getString("name")
                                val recognizedResult = "$artist - $title"
                                onResult(recognizedResult, true)
                            } else {
                                onResult("Song not found", false)
                            }
                        } ?: run {
                            onResult("Error parsing result", false)
                        }
                    }
                }

                override fun onVolumeChanged(volume: Double) {}
            }
            context = this@MusicRecognition.context
            host = "identify-eu-west-1.acrcloud.com"
            accessKey = "ee4028f1a8f0fcf5918f9c6d4058f7bf"
            accessSecret = "qBeleTq0y3epiJQI6rJaVXb0drLQ2pnKJwM4Kpxh"
            protocol = ACRCloudConfig.NetworkProtocol.HTTPS
            recMode = ACRCloudConfig.ACRCloudRecMode.REC_MODE_REMOTE
        }

        client = ACRCloudClient().apply {
            initWithConfig(config)
        }
    }

    fun startRecognition(onTimeout: () -> Unit) {
        client?.startRecognize()
        // Stop recognition after 15 seconds if not already stopped
        handler?.postDelayed({
            stopRecognition()
            onTimeout()
        }, 15000)
    }

    fun stopRecognition() {
        client?.stopRecordToRecognize()
    }

    fun release() {
        client?.release()
    }
}
