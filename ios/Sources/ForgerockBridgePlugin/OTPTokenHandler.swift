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

    enum OTPError: Error {
        case noAccounts
        case noOtpRegistered
    }

    func startOtpJourney(_ call: CAPPluginCall) {
            let handler = NodeOTPCallBacks(call: call, plugin: plugin);
            guard let journey = call.getString("journey"), !journey.isEmpty else {
                call.reject("Missing journey name")
                return
            }

            do {
                FRSession.authenticate(authIndexValue: journey, completion: handler.handle)
            } catch {
                call.reject("[startOtpJourney] failed: \(error.localizedDescription)")
            }
    }

    func deleteOTPRegistration(_ call: CAPPluginCall) {
        
            let handler = NodeOTPDeleteCallBacks(call: call, plugin: plugin);
            guard let journey = call.getString("journey"), !journey.isEmpty else {
                call.reject("Missing journey name")
                return
            }
            do {
                FRSession.authenticate(authIndexValue: journey, completion: handler.handle)
            } catch {
                call.reject("[startOtpJourney] failed: \(error.localizedDescription)")
            }
    }
    

    func validateOTP(call: CAPPluginCall) {
        do {
            if let accounts = fraClient?.getAllAccounts() {
                var empty = true;
                for account in accounts {
                    for mech in account.mechanisms {
                        print("mech", mech)
                        empty = false;
                    }
                }
                var result = JSObject()
                
                result["empty"] = empty;
                call.resolve(result)
            }else {
                print("No hay cuentas registradas")
            }
        } catch {
            print("[validateOTP] Error al obtener cuentas OTP: \(error)")
            call.reject("Fallo en la validación OTP: \(error.localizedDescription)")
        }
    }
    
    func generateOTP(call: CAPPluginCall) {
    do {
            if let accounts = fraClient?.getAllAccounts() {
                let token = try generateCode(from: accounts);
                
                var result = JSObject()
                result["otp"] = token.otp
                result["expiresIn"] = token.time
                
                print("token", token, "result", result)
                call.resolve(result)
            }else {
                print("No hay cuentas registradas")
            }
        } catch OTPError.noAccounts {
            call.reject("No hay cuentas registradas")
        } catch OTPError.noOtpRegistered {
            call.reject("Sin OTP registrado")
        } catch {
            call.reject("Error generando OTP: \(error.localizedDescription)")
        }
    }


    func generateCode(from accounts: [Account]) throws -> (otp: String, time: Int)  {
        for account in accounts {
            for mech in account.mechanisms {
                if let totpMechanism = mech as? TOTPMechanism {
                    if let otp = try? totpMechanism.generateCode() {
                        print("Mecanismo encontrado:", totpMechanism)
                        print("OTP generado:", otp.code, otp.until)
                        let remainingTime = getRemainingTime(from: otp);
                        return (otp: otp.code, time: remainingTime)
                    }
                }
            }
        }
        throw OTPError.noOtpRegistered
    }
    
    func getRemainingTime(from token: OathTokenCode) -> Int {
        guard let until = token.until else { return 0}
        let now = Date().timeIntervalSince1970
        return Int(until - now)
    }

}



