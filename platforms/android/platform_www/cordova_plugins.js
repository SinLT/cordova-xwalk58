cordova.define('cordova/plugin_list', function(require, exports, module) {
  module.exports = [
    {
      "id": "cordova-plugin-facedetection-lite.FaceDetection-Lite",
      "file": "plugins/cordova-plugin-facedetection-lite/www/FaceDetection.js",
      "pluginId": "cordova-plugin-facedetection-lite",
      "clobbers": [
        "facedetection"
      ]
    }
  ];
  module.exports.metadata = {
    "cordova-plugin-whitelist": "1.3.4",
    "cordova-plugin-crosswalk-webview-v3": "3.0.0",
    "cordova-plugin-facedetection-lite": "0.3.5"
  };
});