package com.example

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder

object V2RayParser {

    /**
     * vless:// သို့မဟုတ် trojan:// လင့်ခ်များကို 
     * V2Ray Core Engine စစ်စစ် ဖတ်ရှုနိုင်မည့် တရားဝင် Outbound JSON Standard သို့ ပြောင်းလဲပေးသည့် လုပ်ဆောင်ချက်
     */
    fun convertUrlToJson(urlStr: String): String {
        try {
            val trimmed = urlStr.trim()
            // Protocol စစ်ဆေးခြင်း
            if (!trimmed.startsWith("vless://") && !trimmed.startsWith("trojan://")) {
                return ""
            }

            // Android Uri Parser ဖြင့် လင့်ခ်တစ်ခုလုံးကို အစိတ်အပိုင်း ခွဲထုတ်ခြင်း
            val uri = Uri.parse(trimmed)
            val protocol = uri.scheme ?: "vless"
            
            // UserInfo နေရာမှ UUID (VLESS) သို့မဟုတ် Password (Trojan) ကို ယူခြင်း
            val userInfo = uri.userInfo ?: ""
            val host = uri.host ?: ""
            val port = if (uri.port != -1) uri.port else 443

            // Query Parameters များကို ခွဲထုတ်ခြင်း (WS, TLS, Path, SNI စသည်)
            val rawPath = uri.getQueryParameter("path") ?: "/"
            val path = try { URLDecoder.decode(rawPath, "UTF-8") } catch (e: Exception) { rawPath }
            val security = uri.getQueryParameter("security") ?: "none"
            val sni = uri.getQueryParameter("sni") ?: ""
            val hostHeader = uri.getQueryParameter("host") ?: ""
            val networkType = uri.getQueryParameter("type") ?: "tcp" // ws သို့မဟုတ် tcp

            // --- V2Ray Configuration JSON တည်ဆောက်ခြင်း ---
            val rootJson = JSONObject()

            // ၁။ Inbounds ပိုင်း (Android VPN Service မှ ပို့ပေးမည့် Data ကို လက်ခံရန် Local Port ဖွင့်ခြင်း)
            val inboundsArray = JSONArray()
            val inboundObject = JSONObject().apply {
                put("port", 10808) // Local SOCKS Port
                put("protocol", "socks")
                put("settings", JSONObject().apply {
                    put("auth", "noauth")
                    put("udp", true)
                })
            }
            inboundsArray.put(inboundObject)
            rootJson.put("inbounds", inboundsArray)

            // ၂။ Outbounds ပိုင်း (တကယ့်အပြင်က VPN Server ဆီ ဝှက်စာလုပ်ပြီး ချိတ်ဆက်မည့်အပိုင်း)
            val outboundsArray = JSONArray()
            val outboundObject = JSONObject()
            outboundObject.put("protocol", protocol)

            // ဆာဗာ သတ်မှတ်ချက်များ (Address, Port, Users)
            val vnextOrServersArray = JSONArray()
            val serverNode = JSONObject().apply {
                put("address", host)
                put("port", port)
                
                val usersArray = JSONArray()
                val userObject = JSONObject()
                
                if (protocol == "vless") {
                    userObject.put("id", userInfo)
                    userObject.put("encryption", "none")
                } else if (protocol == "trojan") {
                    userObject.put("password", userInfo)
                }
                
                usersArray.put(userObject)
                put("users", usersArray)
            }
            vnextOrServersArray.put(serverNode)

            // Protocol အလိုက် Key ချိန်ညှိခြင်း (VLESS သည့် vnext သုံးပြီး Trojan သည် servers သုံးသည်)
            val settingsObject = JSONObject()
            if (protocol == "vless") {
                settingsObject.put("vnext", vnextOrServersArray)
            } else if (protocol == "trojan") {
                settingsObject.put("servers", vnextOrServersArray)
            }
            outboundObject.put("settings", settingsObject)

            // Stream Settings (TLS နှင့် WebSocket လမ်းကြောင်း ခွဲဝေမှုအပိုင်း) - Cloudflare ကျော်ရန် အဓိကကျသည်
            val streamSettingsObject = JSONObject().apply {
                put("network", networkType)
                put("security", security)

                // TLS စနစ် ပါဝင်ပါက SNI ထည့်သွင်းခြင်း
                if (security == "tls") {
                    val tlsSettings = JSONObject().apply {
                        if (sni.isNotEmpty()) {
                            put("serverName", sni)
                        }
                        put("allowInsecure", false)
                    }
                    put("tlsSettings", tlsSettings)
                }

                // WebSocket (ws) စနစ်ဖြစ်ပါက Path နှင့် Host Header သတ်မှတ်ခြင်း
                if (networkType == "ws") {
                    val wsSettings = JSONObject().apply {
                        val headersObject = JSONObject()
                        if (hostHeader.isNotEmpty()) {
                            headersObject.put("Host", hostHeader)
                        }
                        put("headers", headersObject)
                        put("path", path)
                    }
                    put("wsSettings", wsSettings)
                }
            }
            
            outboundObject.put("streamSettings", streamSettingsObject)
            outboundsArray.put(outboundObject)
            rootJson.put("outbounds", outboundsArray)

            // JSON အား စနစ်တကျ Indent ချပြီး စာသား (String) အဖြစ် ပြန်ထုတ်ပေးခြင်း
            return rootJson.toString(2)

        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }
}
