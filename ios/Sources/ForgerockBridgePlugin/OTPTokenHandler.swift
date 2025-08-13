import Foundation
import Capacitor
import FRAuth
import FRCore
import FRAuthenticator

class OTPTokenHandler {
    private let call: CAPPluginCall
    private let plugin: ForgerockBridgePlugin
    private var fraClient: FRAClient? { FRAClient.shared }
    
    init(call: CAPPluginCall, plugin: ForgerockBridgePlugin) {
        self.call = call
        self.plugin = plugin
    }

    func startJourney(_ call: CAPPluginCall,completion: @escaping NodeCompletion<Token> ) {
        guard let journey = call.getString("journey"), !journey.isEmpty else {
            ErrorHandler.reject(call, code: OTPErrorCode.missingJourney)
            return
        }
        
          FRSession.authenticate(authIndexValue: journey, completion: completion)
    }
    

    func validateOTP(call: CAPPluginCall) {
        do {
            if let accounts = fraClient?.getAllAccounts() {
                var empty = true;
                for account in accounts {
                    for mech in account.mechanisms {
                        empty = false;
                    }
                }
                print("[EMPTY]",empty)
                var result = JSObject()
                result["empty"] = empty;
                call.resolve(result)
            }else {
                throw OTPErrorCode.noAccountsRegistered
            }
        } catch let error as OTPErrorCode {
            ErrorHandler.reject(call, code: error)
        }
        catch {
            ErrorHandler.reject(call, code: .unknown_error)
        }
    }
    
    func generateOTP(call: CAPPluginCall) {
    do {
            guard let accounts = fraClient?.getAllAccounts(), !accounts.isEmpty else {
                throw OTPErrorCode.noAccountsRegistered
            }

             let token = try generateCode(from: accounts)

             var result = JSObject()
             result["otp"] = token.otp
             result["expiresIn"] = token.time
             call.resolve(result)
        
        } catch let error as OTPErrorCode {
            ErrorHandler.reject(call, code: error)
        } catch {
            ErrorHandler.reject(call, code: .unknown_error)
        }
    }


    func generateCode(from accounts: [Account]) throws -> (otp: String, time: Int)  {
        
        guard !accounts.isEmpty else {
           throw OTPErrorCode.noAccountsRegistered
        }
        
        for account in accounts {
            for mech in account.mechanisms {
                if let totpMechanism = mech as? TOTPMechanism {
                    if let otp = try? totpMechanism.generateCode() {
                        let remainingTime = getRemainingTime(from: otp);
                        return (otp: otp.code, time: remainingTime)
                    }
                }
            }
        }
        throw OTPErrorCode.noOtpRegistered
    }
    
    func getRemainingTime(from token: OathTokenCode) -> Int {
        guard let until = token.until else { return 0}
        let now = Date().timeIntervalSince1970
        return Int(until - now)
    }

}



