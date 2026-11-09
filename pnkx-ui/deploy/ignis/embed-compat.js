/* PNKX Ignis iframe compatibility. Load before shim-loader.js and app.js.
 * Obsidian searches window.parent for Electron's require. In a web iframe,
 * the embedding application is not an Electron renderer. Keep that lookup
 * inside this renderer without granting access to the embedding origin.
 */
(function installEmbeddedRendererBoundary() {
  'use strict';
  if (window.parent === window) return;
  var descriptor = Object.getOwnPropertyDescriptor(window, 'parent');
  if (descriptor && !descriptor.configurable) {
    console.error('[ignis-embed] Cannot install renderer boundary');
    return;
  }
  Object.defineProperty(window, 'parent', {
    configurable: true,
    enumerable: descriptor ? descriptor.enumerable : true,
    get: function () { return window; }
  });
})();
