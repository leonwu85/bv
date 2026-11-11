import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { runInNewContext } from 'node:vm';
import test from 'node:test';
import { webcrypto } from 'node:crypto';

const repository = fileURLToPath(new URL('../../../../../', import.meta.url));
const pages = {
  tv: 'app/tv/src/main/kotlin/dev/aaa1115910/bv/tv/component/GeetestTvVerifyDialog.kt',
  phone: 'app/shared/src/main/kotlin/dev/aaa1115910/bv/network/GeetestCompanionService.kt',
};

function createPage(kind) {
  // Execute the production inline script with a fake SDK; no live captcha or network is needed.
  const source = readFileSync(resolve(repository, pages[kind]), 'utf8');
  const builder = source.slice(source.indexOf('fun buildGeetestHtml('));
  const html = builder.match(/return\s+"""([\s\S]*?)"""\.trimIndent\(\)/)?.[1];
  assert.ok(html, `Missing ${kind} verification HTML`);
  const script = [...html.matchAll(/<script>([\s\S]*?)<\/script>/g)].at(-1)?.[1];
  assert.ok(script);

  const events = {};
  const timers = new Map();
  const submissions = [];
  const requests = [];
  const elements = new Map();
  for (const match of html.matchAll(/\bid="([^"]+)"/g)) {
    elements.set(match[1], { style: {}, textContent: '', onclick: null });
  }
  let timerId = 0;
  let verificationCount = 0;
  let validation = {
    geetest_validate: 'validate',
    geetest_seccode: 'validate|jordan',
    geetest_challenge: 'result-challenge',
  };
  const captcha = {
    appendTo() {},
    verify() { verificationCount += 1; },
    getValidate() { return validation; },
    onReady(callback) { events.ready = callback; },
    onSuccess(callback) { events.success = callback; },
    onClose(callback) { events.close = callback; },
    onError(callback) { events.error = callback; },
  };
  const element = (id) => {
    assert.ok(elements.has(id), `Missing ${kind} HTML element: ${id}`);
    return elements.get(id);
  };
  runInNewContext(script, {
    window: {
      Android: {
        onStatusUpdate() {},
        onVerificationType() {},
        onSliderPosition() {},
        onGeetestResult(...payload) { submissions.push(payload); },
      },
    },
    document: { getElementById: (id) => elements.get(id) ?? null, querySelectorAll: () => [] },
    initGeetest(_config, callback) { callback(captcha); },
    setTimeout(callback, delay) {
      const id = ++timerId;
      timers.set(id, { callback, delay });
      return id;
    },
    clearTimeout(id) { timers.delete(id); },
    fetch(url, options) {
      submissions.push(JSON.parse(options.body));
      return new Promise((resolve, reject) => requests.push({ url, resolve, reject }));
    },
  });
  events.ready();
  return {
    events, submissions, requests, element,
    get verificationCount() { return verificationCount; },
    clearValidation() { validation = null; },
    emptyValidation() { validation = {}; },
    runReopenTimers() {
      for (const [id, timer] of [...timers]) {
        if (timer.delay !== 500) continue;
        timers.delete(id);
        timer.callback();
      }
    },
  };
}

for (const kind of Object.keys(pages)) {
  test(`${kind}: SDK close after success does not request another captcha`, () => {
    const page = createPage(kind);
    page.events.success();
    page.events.close();
    page.runReopenTimers();
    assert.equal(page.verificationCount, 1);
    assert.equal(page.submissions.length, 1);
  });

  test(`${kind}: success cancels a reopen already queued by SDK close`, () => {
    const page = createPage(kind);
    page.events.close();
    page.events.success();
    page.runReopenTimers();
    assert.equal(page.verificationCount, 1);
  });

  test(`${kind}: repeated success callbacks submit the result only once`, () => {
    const page = createPage(kind);
    page.events.success();
    page.events.close();
    page.events.success();
    assert.equal(page.submissions.length, 1);
  });

  test(`${kind}: closing an incomplete captcha still allows verification`, () => {
    const page = createPage(kind);
    page.events.close();
    page.runReopenTimers();
    assert.equal(page.verificationCount, 2);
    assert.equal(page.submissions.length, 0);
  });

  test(`${kind}: success without a valid result does not suppress verification`, () => {
    const page = createPage(kind);
    page.clearValidation();
    page.events.success();
    page.events.close();
    page.runReopenTimers();
    assert.equal(page.verificationCount, 2);
    assert.equal(page.submissions.length, 0);
  });

  test(`${kind}: an incomplete validation payload does not suppress verification`, () => {
    const page = createPage(kind);
    page.emptyValidation();
    page.events.success();
    page.events.close();
    page.runReopenTimers();
    assert.equal(page.verificationCount, 2);
    assert.equal(page.submissions.length, 0);
  });
}

test('phone: failed result delivery can retry without solving another captcha', async () => {
  const page = createPage('phone');
  page.events.success();
  page.events.close();
  page.requests[0].reject(new Error('connection lost'));
  await new Promise(setImmediate);
  assert.equal(page.element('retry').style.display, 'block');
  page.element('retry').onclick();
  assert.equal(page.submissions.length, 2);
  assert.deepEqual(page.submissions[0], page.submissions[1]);
  page.requests[1].resolve({ ok: true });
  await new Promise(setImmediate);
  page.events.close();
  page.events.success();
  page.runReopenTimers();
  assert.equal(page.verificationCount, 1);
  assert.equal(page.submissions.length, 2);
  assert.equal(page.element('ok').style.display, 'block');
});

async function runGaiaPage({ sessionId = null, token = 'server-vtoken', error, fetch, crypto, beforeToken } = {}) {
  const path = 'app/shared/src/main/kotlin/dev/aaa1115910/bv/network/GaiaCaptchaPage.kt';
  const source = readFileSync(resolve(repository, path), 'utf8');
  const script = source.match(/<script>([\s\S]*?)<\/script>/)[1]
    .replace('$voucherJson', '"voucher_123"').replace('$sessionJson', JSON.stringify(sessionId));
  const elements = { status: {}, retry: {} };
  const results = [];
  const requests = [];
  class Xhr {
    open(method, url) { requests.push([method, url]); }
  }
  let registered;
  const window = {
    fetch, crypto,
    parent: {fetch, XMLHttpRequest: Xhr, location: {origin: 'http://tv.local:1000'}},
    CaptchaLoader: { load: async () => async (options) => {
      registered = options.riskParams.v_voucher;
      if (error) throw error;
      if (beforeToken) await beforeToken(window);
      return token;
    } },
    Android: sessionId ? undefined : { onGaiaVerified(value) { results.push(value); } },
  };
  runInNewContext(script, {
    window, URL, Request, btoa, atob, XMLHttpRequest: Xhr, location: { origin: 'http://tv.local:1000' },
    document: { baseURI: 'https://api.bilibili.com/', getElementById: id => elements[id] }, fetch,
  });
  await new Promise(setImmediate);
  return { results, registered, elements, requests, crypto: window.crypto,
    xhr: new (window.XMLHttpRequest || Xhr)(), fetch: window.fetch };
}

test('Gaia sends the original voucher to the official SDK and delivers its validated token', async () => {
  const page = await runGaiaPage();
  assert.equal(page.registered, 'voucher_123');
  assert.deepEqual(page.results, ['server-vtoken']);
});

test('SDK rejection and empty Gaia tokens never resume playback', async () => {
  for (const options of [{ error: new Error('registration failed') }, { token: '' }]) {
    const page = await runGaiaPage(options);
    assert.deepEqual(page.results, []);
    assert.match(page.elements.status.textContent, /验证未完成/);
  }
});

test('Gaia companion relays only API calls and retries delivery of the same token', async () => {
  const posts = [];
  const page = await runGaiaPage({ sessionId: 'session', fetch: async (url, options) => {
    posts.push([url, JSON.parse(options.body)]);
    return { ok: posts.length > 1 };
  } });
  page.xhr.open('GET', '//api.bilibili.com/x/web-frontend/risk/config');
  page.xhr.open('POST', 'https://api.bilibili.com/x/gaia-vgate/v2/register');
  page.xhr.open('POST', 'https://api.bilibili.com/x/gaia-vgate/v2/validate');
  page.xhr.open('GET', 'https://gcaptcha4.geetest.com/load');
  const base = 'http://tv.local:1000/geetest/session';
  assert.deepEqual(page.requests, [
    ['GET', base + '/gaia/config'], ['POST', base + '/gaia/register'],
    ['POST', base + '/gaia/validate'], ['GET', 'https://gcaptcha4.geetest.com/load'],
  ]);
  assert.equal(page.elements.retry.hidden, false);
  page.elements.retry.onclick();
  await new Promise(setImmediate);
  assert.deepEqual(posts, Array(2).fill([base + '/result', { vtoken: 'server-vtoken' }]));
  assert.equal(page.elements.retry.hidden, true);
});

test('Gaia companion forwards SDK fetch requests with their form body', async () => {
  const posts = [];
  const page = await runGaiaPage({ sessionId: 'session', token: '', fetch: async (url, init) => {
    posts.push([url, init.method, init.body]);
  } });
  await page.fetch(new Request('https://api.bilibili.com/x/gaia-vgate/v2/register?locale=zh', {
    method: 'POST', body: new URLSearchParams({v_voucher: 'voucher_123'}),
  }));
  assert.deepEqual(posts, [['http://tv.local:1000/geetest/session/gaia/register', 'POST', 'v_voucher=voucher_123']]);
});

test('HTTP companion provides the SDK AES-CBC operation without native SubtleCrypto', async () => {
  const posts = [];
  const getRandomValues = webcrypto.getRandomValues.bind(webcrypto);
  const page = await runGaiaPage({ sessionId: 'session', token: '', crypto: { getRandomValues },
    fetch: async (url, init) => {
      const payload = JSON.parse(init.body);
      posts.push({ url, payload });
      const key = await webcrypto.subtle.importKey('raw', Buffer.from(payload.key, 'base64'),
        'AES-CBC', false, ['encrypt']);
      const encrypted = await webcrypto.subtle.encrypt({ name: 'AES-CBC', iv: Buffer.from(payload.iv, 'base64') },
        key, Buffer.from(payload.data, 'base64'));
      return { ok: true, json: async () => ({ data: Buffer.from(encrypted).toString('base64') }) };
    } });
  assert.equal(page.crypto.getRandomValues, getRandomValues);
  for (const size of [16, 24, 32]) {
    // Views with offsets must not leak bytes outside the view; importKey must copy the input.
    const raw = Uint8Array.from({ length: size + 2 }, (_, i) => i).subarray(1, size + 1);
    const saved = raw.slice();
    const key = await page.crypto.subtle.importKey('raw', raw, { name: 'AES-CBC' }, false, ['encrypt']);
    raw.fill(0);
    const iv = Uint8Array.from({ length: 18 }, (_, i) => i).subarray(1, 17);
    for (const length of [0, 16, 17]) {
      const data = Uint8Array.from({ length: length + 2 }, (_, i) => i).subarray(1, length + 1);
      const encrypted = await page.crypto.subtle.encrypt({ name: 'AES-CBC', iv }, key, data);
      const post = posts.at(-1);
      assert.equal(post.url, 'http://tv.local:1000/geetest/session/gaia/encrypt');
      assert.deepEqual(Buffer.from(post.payload.key, 'base64'), Buffer.from(saved));
      assert.deepEqual(Buffer.from(post.payload.iv, 'base64'), Buffer.from(iv));
      const nativeKey = await webcrypto.subtle.importKey('raw', saved, 'AES-CBC', false, ['decrypt']);
      const decrypted = await webcrypto.subtle.decrypt({ name: 'AES-CBC', iv }, nativeKey, encrypted);
      assert.deepEqual(Buffer.from(decrypted), Buffer.from(data));
    }
  }
});

test('Gaia leaves native SubtleCrypto and the TV WebView unchanged', async () => {
  const native = { subtle: webcrypto.subtle };
  const phone = await runGaiaPage({ sessionId: 'session', token: '', crypto: native, fetch: async () => {} });
  assert.equal(phone.crypto.subtle, webcrypto.subtle);
  const tv = await runGaiaPage({ crypto: {} });
  assert.equal(tv.crypto.subtle, undefined);
  assert.deepEqual(tv.results, ['server-vtoken']);
});

test('failed companion encryption rejects the SDK result and never submits a token', async () => {
  const posts = [];
  const page = await runGaiaPage({ sessionId: 'session', crypto: {}, fetch: async (url) => {
    posts.push(url);
    return { ok: false };
  }, beforeToken: async (window) => {
    const key = await window.crypto.subtle.importKey('raw', new Uint8Array(16), 'AES-CBC', false, ['encrypt']);
    await window.crypto.subtle.encrypt({ name: 'AES-CBC', iv: new Uint8Array(16) }, key, new Uint8Array(1));
  } });
  assert.deepEqual(posts, ['http://tv.local:1000/geetest/session/gaia/encrypt']);
  assert.match(page.elements.status.textContent, /验证未完成：验证加密失败/);
});

test('companion crypto rejects unsupported operations and invalid inputs before requesting the TV', async () => {
  let count = 0;
  const page = await runGaiaPage({ sessionId: 'session', token: '', crypto: {}, fetch: async () => { count++; } });
  const subtle = page.crypto.subtle;
  await assert.rejects(subtle.importKey('raw', new Uint8Array(16), 'AES-GCM', false, ['encrypt']));
  await assert.rejects(subtle.importKey('raw', new Uint8Array(15), 'AES-CBC', false, ['encrypt']));
  await assert.rejects(subtle.importKey('raw', new Uint8Array(16), 'AES-CBC', false, ['decrypt']));
  const key = await subtle.importKey('raw', new Uint8Array(16), 'AES-CBC', false, ['encrypt']);
  await assert.rejects(subtle.encrypt({ name: 'AES-CBC', iv: new Uint8Array(15) }, key, new Uint8Array(1)));
  await assert.rejects(subtle.encrypt({ name: 'AES-CBC', iv: new Uint8Array(16) }, {}, new Uint8Array(1)));
  await assert.rejects(subtle.encrypt({ name: 'AES-CBC', iv: new Uint8Array(16) }, key, new Uint8Array(65537)));
  assert.equal(count, 0);
});
