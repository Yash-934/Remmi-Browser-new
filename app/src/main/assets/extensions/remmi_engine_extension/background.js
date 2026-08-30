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
      if (msg.type === "EXTRACT_HTML") {
        const requestId = msg.requestId;
        const tabId = msg.tabId;
        if (tabId !== undefined && tabId !== null) {
          browser.tabs.executeScript(tabId, {
            code: "document.documentElement.outerHTML;"
          }).then((res) => {
            const html = (res && res[0]) ? res[0] : "";
            if (port) port.postMessage({ type: "EXTRACTED_HTML", html: html, url: "", requestId: requestId, tabId: tabId });
          }).catch(e => {
            if (port) port.postMessage({ type: "EXTRACTED_HTML", html: "", url: "", requestId: requestId, tabId: tabId });
          });
        }
      }
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
  
  // Validate sender context
  if (!_sender || !_sender.tab || !_sender.tab.url || !_sender.tab.url.startsWith("http")) {
      return;
  }

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

// 2. Delegate all network requests to the Rust Native Engine (P0-7)
browser.webRequest.onBeforeRequest.addListener(
  async function(details) {
    const url = details.url;
    if (!url) return { cancel: false };

    try {
      const response = await browser.runtime.sendNativeMessage("remmi_engine_extension", {
        type: "SHOULD_BLOCK",
        url: url,
        sourceUrl: details.originUrl || details.documentUrl || "",
        resourceType: details.type || "other"
      });
      if (response && response.cancel === true) {
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
    } catch (e) {
      // Fail-closed if Native Engine is unreachable for subresources
      if (details.type !== "main_frame" && details.type !== "sub_frame") {
         return { cancel: true };
      }
    }
    
    return { cancel: false };
  },
  { urls: ["<all_urls>"] },
  ["blocking"]
);
