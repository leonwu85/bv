package dev.aaa1115910.bv.network

import kotlinx.serialization.json.JsonPrimitive

/** CaptchaLoader selects the challenge and validates its proof before resolving with gaia_vtoken. */
fun buildGaiaCaptchaHtml(voucher: String, sessionId: String? = null): String {
    val voucherJson = JsonPrimitive(voucher).toString().replace("<", "\\u003c")
    val sessionJson = sessionId?.let { JsonPrimitive(it).toString().replace("<", "\\u003c") } ?: "null"
    val transportPolicy = if (sessionId == null) "" else
        """<meta http-equiv="Content-Security-Policy" content="upgrade-insecure-requests">"""
    return """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  $transportPolicy
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <base href="https://api.bilibili.com/">
  <style>
    html, body { margin: 0; height: 100%; font: 16px sans-serif; }
    #status { margin: 1em; text-align: center; color: #777; }
    #retry { display: block; margin: auto; }
    #retry[hidden] { display: none; }
  </style>
  <script src="https://s1.hdslb.com/bfs/seed/jinkela/risk-captcha-sdk/CaptchaLoader.js"></script>
</head>
<body>
  <p id="status">正在加载人机验证…</p>
  <button id="retry" hidden>重新提交验证结果</button>
  <script>
    (function () {
      var voucher = $voucherJson;
      var sessionId = $sessionJson;
      var status = document.getElementById('status');
      var retry = document.getElementById('retry');
      var sessionUrl;
      var lanFetch;
      function notify(message) {
        status.textContent = message;
        if (window.Android && window.Android.onStatusUpdate) window.Android.onStatusUpdate(message);
      }
      if (sessionId) {
        // Only the outer LAN document talks HTTP to the TV. All remote SDK assets use HTTPS.
        var transport = window.parent;
        lanFetch = transport.fetch.bind(transport);
        sessionUrl = transport.location.origin + '/geetest/' + encodeURIComponent(sessionId);
        // HTTP LAN pages have getRandomValues, but no SubtleCrypto (including Safari).
        // The official SDK needs only raw AES-CBC import/encrypt after the user solves the
        // challenge. Delegate those operations to the TV's JCE provider for this session.
        // Loading HTTPS scripts or upgrade-insecure-requests does not make this page secure.
        if (window.crypto && !window.crypto.subtle) {
          var keys = new WeakMap();
          function bytes(value) {
            return ArrayBuffer.isView(value)
              ? new Uint8Array(value.buffer, value.byteOffset, value.byteLength)
              : new Uint8Array(value);
          }
          function base64(value) {
            var binary = '';
            bytes(value).forEach(function (byte) { binary += String.fromCharCode(byte); });
            return btoa(binary);
          }
          function isAesCbc(algorithm) {
            return String(typeof algorithm === 'string' ? algorithm : algorithm && algorithm.name)
              .toUpperCase() === 'AES-CBC';
          }
          Object.defineProperty(window.crypto, 'subtle', {value: {
            importKey: async function (format, data, algorithm, extractable, usages) {
              if (format !== 'raw' || !isAesCbc(algorithm) || extractable ||
                  !Array.isArray(usages) || usages.length !== 1 || usages[0] !== 'encrypt') {
                throw new Error('不支持的验证加密方式');
              }
              var raw = bytes(data);
              if ([16, 24, 32].indexOf(raw.byteLength) < 0) throw new Error('验证密钥长度无效');
              var key = Object.freeze({type: 'secret', extractable: false,
                algorithm: Object.freeze({name: 'AES-CBC', length: raw.byteLength * 8}),
                usages: Object.freeze(['encrypt'])});
              keys.set(key, base64(raw));
              return key;
            },
            encrypt: async function (algorithm, key, data) {
              if (!isAesCbc(algorithm) || !keys.has(key) || !algorithm.iv ||
                  bytes(algorithm.iv).byteLength !== 16 || bytes(data).byteLength > 65536) {
                throw new Error('验证加密参数无效');
              }
              var response = await lanFetch(sessionUrl + '/gaia/encrypt', {
                method: 'POST', headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({key: keys.get(key), iv: base64(algorithm.iv), data: base64(data)})
              });
              if (!response.ok) throw new Error('验证加密失败，请检查与电视的连接或重新扫码');
              var result = await response.json();
              if (typeof result.data !== 'string') throw new Error('验证加密结果无效');
              return Uint8Array.from(atob(result.data), function (char) { return char.charCodeAt(0); }).buffer;
            }
          }});
        }
        // The LAN page cannot make credentialed cross-origin SDK requests to Bilibili.
        var routes = {
          '/x/web-frontend/risk/config': 'config',
          '/x/gaia-vgate/v2/register': 'register',
          '/x/gaia-vgate/v2/validate': 'validate'
        };
        function relayUrl(url) {
          var target = new URL(url, document.baseURI);
          if (target.origin === 'https://api.bilibili.com' && routes[target.pathname]) {
            return sessionUrl + '/gaia/' + routes[target.pathname];
          }
        }
        window.XMLHttpRequest = class extends transport.XMLHttpRequest {
          open(method, url, ...args) {
            var target = new URL(url, document.baseURI);
            if (target.protocol === 'http:') target.protocol = 'https:';
            return super.open(method, relayUrl(target) || target.href, ...args);
          }
        };
        var fetchUrl = window.fetch.bind(window);
        window.fetch = function (input, options) {
          var url = relayUrl(input instanceof Request ? input.url : input);
          if (!url) return fetchUrl(input, options);
          var request = new Request(input, options);
          var init = {method: request.method, headers: request.headers, signal: request.signal};
          if (request.method === 'GET') return lanFetch(url, init);
          return request.text().then(function (body) {
            init.body = body;
            return lanFetch(url, init);
          });
        };
      }
      function deliver(token) {
        if (!sessionId) {
          window.Android.onGaiaVerified(token);
          return;
        }
        retry.hidden = true;
        notify('正在将验证结果发回电视…');
        lanFetch(sessionUrl + '/result', {
          method: 'POST', headers: {'Content-Type': 'application/json'},
          body: JSON.stringify({vtoken: token})
        }).then(function (response) {
          if (!response.ok) throw new Error('提交失败');
          notify('验证成功，可返回电视');
        }).catch(function () {
          notify('提交失败，请检查与电视的连接');
          retry.onclick = function () { deliver(token); };
          retry.hidden = false;
        });
      }
      // Both versions use a floating panel. Report the visible slider handle in WebView pixels.
      var observer;
      if (window.Android && window.Android.onVerificationType) {
        var lastGeometry;
        function reportPanel() {
          var panel = document.querySelector('.geetest_panel, [class*="geetest_box_"]');
          if (!panel || !panel.getBoundingClientRect().height) return;
          var handle = panel.querySelector('.geetest_slider_button, [class*="geetest_track_"] [class*="geetest_btn_"]');
          var rect = handle && handle.getBoundingClientRect();
          var isSlider = rect && rect.width && rect.height;
          var scale = window.devicePixelRatio || 1;
          var x = isSlider && (rect.left + rect.width / 2) * scale;
          var y = isSlider && (rect.top + rect.height / 2) * scale;
          var geometry = isSlider ? x + ',' + y : 'click';
          if (lastGeometry === geometry) return;
          lastGeometry = geometry;
          window.Android.onVerificationType(isSlider ? 'slider' : 'click');
          if (isSlider) window.Android.onSliderPosition(x, y);
        }
        observer = new MutationObserver(reportPanel);
        observer.observe(document.body, {subtree:true, childList:true, attributes:true});
      }
      Promise.resolve().then(function () {
        if (!window.CaptchaLoader) throw new Error('验证码脚本加载失败，请检查网络');
        return window.CaptchaLoader.load();
      }).then(function (useCaptcha) {
        return useCaptcha({riskParams: {v_voucher: voucher}, language: 'zh-Hans',
          onInit: function () { notify('请完成人机验证'); }
        });
      }).then(function (token) {
        if (typeof token !== 'string' || !token.trim()) throw new Error('未取得验证令牌');
        deliver(token);
      }).catch(function (error) {
        notify('验证未完成：' + (error && (error.tip || error.message) || '请关闭后重试'));
      }).finally(function () { if (observer) observer.disconnect(); });
    })();
  </script>
</body>
</html>
    """.trimIndent()
}

/** Keep a LAN transport in the parent while the SDK frame upgrades its external HTTP resources. */
internal fun gaiaCompanionFrame(sessionId: String): String = """
    <!DOCTYPE html><html lang="zh-CN"><head><meta charset="utf-8">
    <meta name="viewport" content="width=device-width,initial-scale=1"><title>BV 人机验证</title>
    <style>body { margin: 0; } iframe { width: 100%; height: 100vh; display: block; border: 0; }</style>
    </head><body><iframe title="人机验证" src="/geetest/$sessionId/captcha"></iframe></body></html>
""".trimIndent()
