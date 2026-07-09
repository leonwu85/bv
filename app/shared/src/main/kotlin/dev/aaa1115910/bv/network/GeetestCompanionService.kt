package dev.aaa1115910.bv.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 手机代完成 Geetest：TV 局域网 HTTP 会话注册与回调。
 *
 * 手机访问 `http://{tv-ip}:{port}/geetest/{sessionId}`，在触屏上完成极验后
 * POST 结果，TV 端通过 [Session.onResult] 收到 challenge/validate/seccode。
 */
object GeetestCompanionService {
    private val logger = KotlinLogging.logger {}

    data class GeetestResultPayload(
        val challenge: String,
        val validate: String,
        val seccode: String,
    )

    data class Session(
        val id: String,
        val gt: String,
        val challenge: String,
        val mockMode: Boolean = false,
        @Volatile var completed: Boolean = false,
        @Volatile var result: GeetestResultPayload? = null,
        val onResult: (GeetestResultPayload) -> Unit,
        val createdAt: Long = System.currentTimeMillis(),
    )

    private val sessions = ConcurrentHashMap<String, Session>()

    fun createSession(
        gt: String,
        challenge: String,
        mockMode: Boolean = false,
        onResult: (GeetestResultPayload) -> Unit,
    ): Session {
        val id = UUID.randomUUID().toString().replace("-", "").take(16)
        val session = Session(
            id = id,
            gt = gt,
            challenge = challenge,
            mockMode = mockMode,
            onResult = onResult,
        )
        sessions[id] = session
        logger.info { "Geetest companion session created: $id mock=$mockMode" }
        return session
    }

    fun buildHtmlForSession(session: Session): String =
        if (session.mockMode) {
            buildMockGeetestHtml(session.id)
        } else {
            buildGeetestHtml(session.gt, session.challenge, session.id)
        }

    /**
     * Debug mock：无真实极验，手机点按钮即可回传假结果，用于测扫码链路。
     */
    fun buildMockGeetestHtml(sessionId: String): String {
        val safeId = sessionId.replace("'", "")
        return """
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8"/>
  <meta name="viewport" content="width=device-width,initial-scale=1.0,maximum-scale=1.0,user-scalable=no"/>
  <title>BV Mock 验证</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
      background: #0f1115; color: #f2f2f2;
      min-height: 100vh; padding: 24px 16px;
    }
    h1 { font-size: 20px; margin-bottom: 8px; }
    p { font-size: 14px; color: #aaa; margin-bottom: 20px; line-height: 1.5; }
    #status { margin-bottom: 16px; color: #8ec8ff; font-size: 14px; }
    button {
      width: 100%; max-width: 360px; padding: 16px;
      font-size: 18px; border: none; border-radius: 12px;
      background: #3d8bfd; color: #fff;
    }
    button:active { background: #2b6fd4; }
    .ok {
      margin-top: 16px; padding: 14px; border-radius: 10px;
      background: #16351f; color: #7dffa0; display: none;
    }
  </style>
</head>
<body>
  <h1>Mock 人机验证（调试）</h1>
  <p>此页不请求极验服务，仅用于测试「手机扫码 → 回传结果」链路。</p>
  <div id="status">点击下方按钮模拟验证成功</div>
  <button id="btn" type="button">模拟验证成功</button>
  <div id="ok" class="ok">验证成功，可返回电视</div>
  <script>
    (function() {
      var sessionId = '$safeId';
      document.getElementById('btn').onclick = function() {
        document.getElementById('status').textContent = '正在提交…';
        fetch('/geetest/' + sessionId + '/result', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            validate: 'mock_validate',
            seccode: 'mock_validate|jordan',
            challenge: 'mock_challenge'
          })
        }).then(function(r) {
          if (!r.ok) throw new Error('submit failed');
          document.getElementById('status').textContent = '验证成功';
          document.getElementById('ok').style.display = 'block';
          document.getElementById('btn').disabled = true;
        }).catch(function(e) {
          document.getElementById('status').textContent = '提交失败：' + (e && e.message || e);
        });
      };
    })();
  </script>
</body>
</html>
        """.trimIndent()
    }

    fun getSession(id: String): Session? = sessions[id]

    fun completeSession(
        id: String,
        challenge: String,
        validate: String,
        seccode: String,
    ): Boolean {
        val session = sessions[id] ?: return false
        if (session.completed) return true
        val payload = GeetestResultPayload(
            challenge = challenge.trim(),
            validate = validate.trim(),
            seccode = seccode.trim(),
        )
        if (payload.challenge.isBlank() || payload.validate.isBlank() || payload.seccode.isBlank()) {
            return false
        }
        session.completed = true
        session.result = payload
        runCatching { session.onResult(payload) }
            .onFailure { logger.warn(it) { "Geetest companion onResult failed" } }
        return true
    }

    fun removeSession(id: String) {
        sessions.remove(id)
        logger.info { "Geetest companion session removed: $id" }
    }

    fun buildVerifyUrl(host: String, port: Int, sessionId: String): String =
        "http://$host:$port/geetest/$sessionId"

    suspend fun resolveServerPort(): Int =
        HttpServer.server?.engine?.resolvedConnectors()?.firstOrNull()?.port ?: 0

    @Suppress("DEPRECATION")
    fun resolveLocalIpAddress(context: Context): String {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val isWifi: Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val caps = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.type == ConnectivityManager.TYPE_WIFI
        }

        var ip = ""
        if (isWifi) {
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            val ipNum = wifiInfo.ipAddress
            ip =
                "${ipNum and 0xFF}.${ipNum shr 8 and 0xFF}.${ipNum shr 16 and 0xFF}.${ipNum shr 24 and 0xFF}"
        } else {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val enumIpAddr = intf.inetAddresses
                while (ip == "" && enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        ip = inetAddress.hostAddress ?: ""
                        break
                    }
                }
            }
        }
        return ip
    }

    fun buildGeetestHtml(gt: String, challenge: String, sessionId: String): String {
        val safeGt = gt.replace("\\", "\\\\").replace("'", "\\'").replace("<", "&lt;")
        val safeChallenge = challenge.replace("\\", "\\\\").replace("'", "\\'").replace("<", "&lt;")
        val safeId = sessionId.replace("'", "")
        return """
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8"/>
  <meta name="viewport" content="width=device-width,initial-scale=1.0,maximum-scale=1.0,user-scalable=no"/>
  <title>BV 人机验证</title>
  <script src="https://static.geetest.com/static/tools/gt.js"></script>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
      background: #0f1115;
      color: #f2f2f2;
      min-height: 100vh;
      padding: 20px 16px 40px;
    }
    h1 { font-size: 18px; margin-bottom: 8px; }
    p { font-size: 14px; color: #aaa; margin-bottom: 16px; line-height: 1.5; }
    #status { margin-bottom: 12px; color: #8ec8ff; font-size: 14px; }
    #captcha { min-height: 320px; }
    .ok {
      margin-top: 16px; padding: 14px; border-radius: 10px;
      background: #16351f; color: #7dffa0; display: none;
    }
  </style>
</head>
<body>
  <h1>BV 风控人机验证</h1>
  <p>请在手机上完成验证，完成后电视端会自动继续播放。</p>
  <div id="status">正在加载验证码…</div>
  <div id="captcha"></div>
  <div id="ok" class="ok">验证成功，可返回电视</div>
  <script>
    (function() {
      var sessionId = '$safeId';
      function setStatus(msg) {
        var el = document.getElementById('status');
        if (el) el.textContent = msg;
      }
      function submitResult(validate, seccode, challenge) {
        setStatus('正在提交结果…');
        fetch('/geetest/' + sessionId + '/result', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            validate: validate,
            seccode: seccode,
            challenge: challenge
          })
        }).then(function(r) {
          if (!r.ok) throw new Error('submit failed');
          setStatus('验证成功');
          document.getElementById('ok').style.display = 'block';
        }).catch(function(e) {
          setStatus('提交失败，请重试：' + (e && e.message || e));
        });
      }
      if (typeof initGeetest !== 'function') {
        setStatus('验证码脚本加载失败，请检查网络');
        return;
      }
      initGeetest({
        gt: '$safeGt',
        challenge: '$safeChallenge',
        new_captcha: true,
        product: 'bind',
        offline: false,
        https: true
      }, function(captchaObj) {
        captchaObj.appendTo('#captcha');
        captchaObj.onReady(function() {
          setStatus('请完成下方验证');
          captchaObj.verify();
        });
        captchaObj.onSuccess(function() {
          var res = captchaObj.getValidate();
          if (!res) return;
          submitResult(res.geetest_validate, res.geetest_seccode, res.geetest_challenge);
        });
        captchaObj.onError(function(e) {
          setStatus('验证出错：' + (e && (e.msg || e.error_code) || '未知错误'));
        });
        captchaObj.onClose(function() {
          setStatus('验证已关闭，正在重新打开…');
          setTimeout(function() { captchaObj.verify(); }, 500);
        });
      });
    })();
  </script>
</body>
</html>
        """.trimIndent()
    }
}
