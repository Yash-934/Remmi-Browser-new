sed -i 's/val state = torManager.bootstrapState.value/return CurrentTorRoute.isReady/g' app/src/main/java/com/remmi/browser/security/PrivacyNetworkController.kt
sed -i 's/return state is TorManager.TorState.READY &&//g' app/src/main/java/com/remmi/browser/security/PrivacyNetworkController.kt
sed -i 's/TorStatusChecker.isPortListening("127.0.0.1", state.port, 200) &&//g' app/src/main/java/com/remmi/browser/security/PrivacyNetworkController.kt
sed -i 's/CurrentTorRoute.isGhostActive//g' app/src/main/java/com/remmi/browser/security/PrivacyNetworkController.kt
