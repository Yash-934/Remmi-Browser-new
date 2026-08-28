// Remmi Engine WebExtension - Dedicated Ad/Tracker Blocker & Click Transparency Bridge
// CRITICAL SECURITY INVARIANT: Native Gecko layer is the SOLE authoritative proxy manager.
// The WebExtension does NOT modify browser.proxy or route settings.

let port = null;
const pendingMessages = [];

function logToNative(msg) {
  if (port) {
    try {
      port.postMessage({ type: "LOG", message: msg, action: "log", msg: msg });
    } catch (_e) {}
  }
}

function flushPendingMessages() {
  if (!port) return;
  while (pendingMessages.length > 0) {
    const msg = pendingMessages.shift();
    try {
      port.postMessage(msg);
    } catch (_e) {
      pendingMessages.unshift(msg);
      break;
    }
  }
}

function connectNative() {
  try {
    port = browser.runtime.connectNative("remmi_engine_extension");
    if (!port) return;

    try {
      port.postMessage({
        type: "PORT_STATUS",
        status: "CONNECTED",
        role: "AD_TRACKER_BLOCKER_ONLY"
      });
    } catch (_err) {}

    flushPendingMessages();

    port.onMessage.addListener((msg) => {
      if (!msg) return;
      // WebExtension operates strictly for ad blocking and click transparency.
      // Native Gecko layer handles all proxy and network hardening authoritatively.
    });

    port.onDisconnect.addListener(() => {
      port = null;
      setTimeout(connectNative, 3000);
    });
  } catch (_e) {
    setTimeout(connectNative, 5000);
  }
}

connectNative();

// 1. Content Script Message Listener: Forward CLICK_INSPECTED -> native port -> BlockExtension
browser.runtime.onMessage.addListener((message, _sender, sendResponse) => {
  if (!message) return;
  if (message.type === "CLICK_INSPECTED") {
    const payload = {
      type: "CLICK_INSPECTION_RESULT",
      candidates: message.candidates || [],
      hasOverlay: !!message.hasOverlay,
      intercepted: !!message.intercepted,
      pageUrl: message.pageUrl || "",
      timestamp: message.timestamp || Date.now()
    };

    if (port) {
      try {
        port.postMessage(payload);
      } catch (_e) {
        pendingMessages.push(payload);
      }
    } else {
      pendingMessages.push(payload);
    }
  }
  if (sendResponse) sendResponse({ received: true });
  return true;
});

// Built-in ad, tracker, analytics, cryptominer, and telemetric patterns
const blockPatterns = [
  // Primary Analytics & Telemetry
  /google-analytics\.com/i,
  /googletagmanager\.com/i,
  /analytics\.google\.com/i,
  /segment\.io/i,
  /segment\.com\/analytics/i,
  /mixpanel\.com/i,
  /amplitude\.com/i,
  /clarity\.ms/i,
  /hotjar\.com/i,
  /mouseflow\.com/i,
  /crazyegg\.com/i,
  /mc\.yandex\.ru/i,
  /statcounter\.com/i,
  /newrelic\.com/i,
  /optimizely\.com/i,
  /chartbeat\.com/i,
  /quantserve\.com/i,
  /quantcount\.com/i,
  /scorecardresearch\.com/i,

  // Ad Networks, DSPs, SSPs & Exchanges
  /doubleclick\.net/i,
  /googlesyndication\.com/i,
  /adservice\.google\./i,
  /pagead2\.googlesyndication\.com/i,
  /googleads\.g\.doubleclick\.net/i,
  /securepubads\.g\.doubleclick\.net/i,
  /adnxs\.com/i,
  /adsrvr\.org/i,
  /criteo\.com/i,
  /criteo\.net/i,
  /taboola\.com/i,
  /outbrain\.com/i,
  /moatads\.com/i,
  /rubiconproject\.com/i,
  /openx\.net/i,
  /casalemedia\.com/i,
  /pubmatic\.com/i,
  /yieldmanager\.com/i,
  /yieldmo\.com/i,
  /indexww\.com/i,
  /bidswitch\.net/i,
  /revcontent\.com/i,
  /mgid\.com/i,
  /zergnet\.com/i,
  /adroll\.com/i,
  /smartadserver\.com/i,
  /amazon-adsystem\.com/i,
  /zedo\.com/i,
  /advertising\.com/i,

  // Invasive Popups, Interstitials & Redirect Ads
  /popads\.net/i,
  /popcash\.net/i,
  /propellerads\.com/i,
  /adsterra\.com/i,
  /exoclick\.com/i,
  /trafficjunky\.com/i,
  /trafficfactory\.biz/i,
  /juicyads\.com/i,
  /clickadu\.com/i,
  /hilltopads\.net/i,

  // Mobile Attribution & Fingerprint Beacons
  /appsflyer\.com/i,
  /branch\.io/i,
  /adjust\.com/i,
  /kochava\.com/i,
  /singular\.net/i,
  /applovin\.com/i,
  /unityads\.unity3d\.com/i,
  /vungle\.com/i,
  /ironsrc\.com/i,
  /mintegral\.com/i,

  // Social & Platform Tracking Beacons / Pixels
  /facebook\.net\/tr/i,
  /connect\.facebook\.net/i,
  /ads-twitter\.com/i,
  /analytics\.twitter\.com/i,
  /static\.ads-twitter\.com/i,
  /bat\.bing\.com/i,
  /tr\.snapchat\.com/i,
  /analytics\.tiktok\.com/i,
  /pixel\.reddit\.com/i,
  /licdn\.com\/insight/i,
  /pinterest\.com\/ct\.html/i,

  // Cryptominers & Coercive Scripts
  /coinhive\.com/i,
  /coin-hive\.com/i,
  /cryptoloot\.pro/i,
  /crypto-loot\.com/i,
  /webminepool\.com/i,
  /authedmine\.com/i,
  /minr\.pw/i,

  // Generic ad server path patterns
  /\/(?:ad|ads|advert|banner|prebid|telemetry|trackers?|pixel)\.(?:js|gif|png|json)/i,
  /\/(?:adserver|adsystem|pagead|beacon)\./i
];

browser.webRequest.onBeforeRequest.addListener(
  function(details) {
    const url = details.url;
    if (!url) return { cancel: false };

    for (let i = 0; i < blockPatterns.length; i++) {
      if (blockPatterns[i].test(url)) {
        if (port) {
          try {
            port.postMessage({
              type: "BLOCKED",
              action: "blocked",
              url: url,
              category: details.type || "tracker"
            });
          } catch (_e) {}
        }
        return { cancel: true };
      }
    }
    return { cancel: false };
  },
  { urls: ["<all_urls>"] },
  ["blocking"]
);
