sed -i 's/CurrentTorRoute.clearRoute()/CurrentTorRoute.clearRoute()\n            NetworkHardening.resetAppliedState()/g' app/src/main/java/com/remmi/browser/security/PrivacyNetworkController.kt
