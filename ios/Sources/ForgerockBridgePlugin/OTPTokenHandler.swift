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

    func initializeOTPRegister(call: CAPPluginCall) {
        print("entro")
        guard  let journey = call.getString("journey")
        else {
            call.reject("Missing required parameters")
            return
        }

        print("journey", journey)

        if let currentSession = FRSession.currentSession {
            print("Hay una sesión activa: \(currentSession.sessionToken)")
        }
        startOTPRegistrationFlow(journey: journey)
       
        print("startOTPRegistrationFlow")
    }

    //  func getJourny(call: CAPPluginCall) {
    //     print("getJourny")
    //     guard  let journey = call.getString("journey")
    //     else {
    //         call.reject("Missing required parameters")
    //         return
    //     }
        
       

    //     print("journey", journey)

    //     if let currentSession = FRSession.currentSession {
    //         print("Hay una sesión activa: \(currentSession.sessionToken)")
    //     }

    //     return journey;

    // }

    func startOTPRegistrationFlow(journey: String){
        let handler = NodeOTPCallBacks(call: call, plugin: plugin)
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
    
    

    

    func deleteOTPRegistration(call: CAPPluginCall) {
        // let journy = getJourny(CAPPluginCall);
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
            } else {
                print("[ForgeRock OTP] UNEXPECTED RESULT")
                self.call.reject("Unexpected authentication result")
            }
        }
    }

    func validateOTP(call: CAPPluginCall) {
        do {
            print("entro al validateOTP")
            let accounts = self.fraClient?.getAllAccounts()
            var result = JSObject()

            result["empty"] = accounts?.isEmpty ?? true
            print("empty", result)
            call.resolve(result)
        } catch {
            print("[validateOTP] Error al obtener cuentas OTP: \(error)")
            call.reject("Fallo en la validación OTP: \(error.localizedDescription)")
        }
    }

}


