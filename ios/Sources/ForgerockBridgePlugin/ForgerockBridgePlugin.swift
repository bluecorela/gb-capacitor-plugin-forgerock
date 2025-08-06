import Foundation
import FRAuth
import Capacitor


/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(ForgerockBridgePlugin)
public class ForgerockBridgePlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "ForgerockBridgePlugin"
    public let jsName = "ForgerockBridge"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "initialize", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "authenticate", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "logout", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "userInfo", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getAccessToken", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "initializeOTPRegister", returnType: CAPPluginReturnPromise),
    ]
    public var pendingNode: Node? = nil
    public var didSubmitConfirmation = false

    /// Inicializa el SDK de ForgeRock con la configuración enviada desde el frontend.
    /// Parámetros requeridos: url, realm, journey, oauthClientId, oauthScope.
    @objc func initialize(_ call: CAPPluginCall) {
        SDKInitializer.initialize(call: call)
    }

    /// Inicia o continúa el flujo de autenticación. Maneja intentos, errores y callbacks como ConfirmationCallback.
    @objc func authenticate(_ call: CAPPluginCall) {
        AuthenticationHandler(call: call, plugin: self).authenticate()
    }

    /// Cierra la sesión del usuario actual. Limpia los tokens y estado del SDK.
    @objc func logout(_ call: CAPPluginCall) {
        UserHandler.logout(call: call)
    }

    /// Obtiene la información del usuario autenticado actual (claims del ID Token).
    @objc func userInfo(_ call: CAPPluginCall) {
        UserHandler.userInfo(call: call)
    }

    /// Retorna el Access Token del usuario actual para autenticación en APIs externas.
    @objc func getAccessToken(_ call: CAPPluginCall) {
        TokenHandler.getAccessToken(call: call)
    }
    
    // Inicializa el metodo de token y registro 
      @objc func initializeOTPRegister(_ call: CAPPluginCall) {
          let handler = OTPTokenHandler(call: call, plugin: self)
             handler.initializeOTPRegister()
    }

     // Eliminar token registrado 
      @objc func deleteOTPRegister(_ call: CAPPluginCall) {
          let handler = OTPTokenHandler(call: call, plugin: self)
             handler.deleteOTPRegistration()
    }

    
}
