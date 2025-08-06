import Foundation
import Capacitor
import FRAuth
import FRCore
import FRAuthenticator

class OTPTokenHandler {
    private let call: CAPPluginCall
    private let plugin: ForgerockBridgePlugin
    private let fraClient = FRAClient.shared

    init(call: CAPPluginCall, plugin: ForgerockBridgePlugin) {
        self.call = call
        self.plugin = plugin
    }

    func initializeOTPRegister() {
        guard let urlString = call.getString("url"),
              let url = URL(string: urlString),
              let realm = call.getString("realm"),
              let journey = call.getString("journey"),
              let oauthClientId = call.getString("oauthClientId"),
              let oauthScope = call.getString("oauthScope")
        else {
            call.reject("Missing required parameters")
            return
        }

        let bundleId = Bundle.main.bundleIdentifier ?? "com.example.app"

        FRRequestInterceptorRegistry.shared.registerInterceptors(
            interceptors: [
                QueryParamsAndHeaderRequestInterceptor(),
                DebugCookieInterceptor()
            ]
        )
        
        if let currentSession = FRSession.currentSession {
            print("Hay una sesión activa: \(currentSession.sessionToken)")
        }
    
        print("tuto bem")
            
        self.startOTPRegistrationFlow(journey: journey)
        print("startOTPRegistrationFlow")


    }
    
    func startOTPRegistrationFlow(journey: String) {
        let handler = NodeOTPCallBacks(call: call, plugin: plugin)
        print("Iniciando flujo OTP con journey: \(journey)")
        
        FRSession.authenticate(authIndexValue: journey) { token, node, error in
            if let error = error {
                print("[ForgeRock OTP] ERROR => \(error.localizedDescription)")
                self.call.reject("Error starting authentication: \(error.localizedDescription)")
            } else if let node = node {
                print("[ForgeRock OTP] NODE RECEIVED => \(node.callbacks.map { String(describing: type(of: $0)) })")

                print("Callbacks disponibles:")
                    for callback in node.callbacks {
                        print("Tipo: \(type(of: callback)) \(callback)")
                    }
                
                handler.handle(node: node)
            } else {
                print("[ForgeRock OTP] UNEXPECTED RESULT")
                self.call.reject("Unexpected authentication result")
            }
        }
    }

    func deleteOTPRegistration() {
        let handler = NodeOTPCallBacks(call: call, plugin: plugin)
     


        guard let journey = call.getString("journey") else {
            call.reject("Missing required parameter: journey")
            return
        }

        print("Iniciando flujo DELETE OTP con journey: \(journey)")
        
        FRSession.authenticate(authIndexValue: journey) { token, node, error in
            if let error = error {
                print("[ForgeRock OTP] ERROR => \(error.localizedDescription)")
                self.call.reject("Error starting authentication: \(error.localizedDescription)")
            } else if let node = node {
                print("[ForgeRock OTP] NODE RECEIVED => \(node.callbacks.map { String(describing: type(of: $0)) })")

                print("Callbacks disponibles:")
                    for callback in node.callbacks {
                        print("Tipo: \(type(of: callback)) \(callback)")
                    }
                
                handler.handle(node: node)
            } else {
                print("[ForgeRock OTP] UNEXPECTED RESULT")
                self.call.reject("Unexpected authentication result")
            }
        }
    }
    
    class DebugCookieInterceptor: RequestInterceptor {
        func intercept(request: Request, action: Action) -> Request {
            print("Request headers: \(request.headers)")
            return request
        }
    }


    class QueryParamsAndHeaderRequestInterceptor: RequestInterceptor {
       
        func intercept(request: Request, action: Action) -> Request {
            print("vino acaaa")
                    print("🎯 Interceptando request para acción: \(action.type)")

                    var headers = request.headers
                    var urlParams = request.urlParams

                    if let cookieValue = UserDefaults.standard.string(forKey: "SESSION_COOKIE") {
                        headers["Cookie"] = "iPlanetDirectoryPro=\(cookieValue)"
                        print("✅ Adjuntando cookie desde UserDefaults: \(cookieValue)")
                    }

                    return Request(
                        url: request.url,
                        method: request.method,
                        headers: headers,
                        bodyParams: request.bodyParams,
                        urlParams: urlParams,
                        requestType: request.requestType,
                        responseType: request.responseType,
                        timeoutInterval: request.timeoutInterval
                    )
                }
    }
}


