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
            ErrorHandler.reject(call, code: ErrorCode.missingJourney)
            return
        }
        
          FRSession.authenticate(authIndexValue: journey, completion: completion)
    }
    

    func validateExistenceOTP(call: CAPPluginCall) {
        guard let user = FRUser.currentUser else {
            ErrorHandler.reject(call, code: ErrorCode.gettingUserInfo)
            return
        }

        user.getUserInfo { userInfo, error in
            if let error = error {
                ErrorHandler.reject(call, code: ErrorCode.gettingUserInfo)
            } else if let userInfo = userInfo {
                
                print("userInfo",userInfo.userInfo)
                
                guard let payload = userInfo.userInfo as? [String: Any] else {
                       ErrorHandler.reject(call, code: ErrorCode.gettingUserInfo)
                       return
                   }

                if let uuid = payload["sub"] as? String {
                    print("sub:", uuid)
                    self.checkServerAndDeviceOtpState(call: call, uuid: uuid );
                } else {
                   ErrorHandler.reject(call, code: ErrorCode.gettingUserInfo)
                }
            } else {
                ErrorHandler.reject(call, code: ErrorCode.gettingUserInfo)
            }
        }
    }
    
    func checkServerAndDeviceOtpState(call: CAPPluginCall, uuid: String){
        
        let sessionToken = FRSession.currentSession?.sessionToken?.value
        let cookie = "iPlanetDirectoryPro=\(sessionToken ?? "")"
            print("headerValue",cookie);
        
        guard let base_url = call.getString("url"), !base_url.isEmpty else {
            ErrorHandler.reject(call, code: ErrorCode.missingParameter)
            return
        }
        let urlString = "\(base_url)/\(uuid)/devices/2fa/oath?_queryFilter=true"
        guard let url = URL(string: urlString) else {
                ErrorHandler.reject(call, code: .httpRequestError)
                return
       }
        
        var req = URLRequest(url: url)
            req.httpMethod = "GET"
            req.setValue("application/json", forHTTPHeaderField: "Accept")
            req.setValue(cookie, forHTTPHeaderField: "Cookie")
        

        URLSession.shared.dataTask(with: req) { data, response, error in
                if let error = error {
                    print("[OTPTokenHandler] network error: \(error)")
                    ErrorHandler.reject(call, code: .httpRequestError)
                    return
                }
            
            guard let http = response as? HTTPURLResponse else {
                ErrorHandler.reject(call, code: .httpRequestError)
                return
            }
            
            guard (200...299).contains(http.statusCode) else {
                print("HTTP error: \(http.statusCode) - \(HTTPURLResponse.localizedString(forStatusCode: http.statusCode))")
                ErrorHandler.reject(call, code: .httpRequestError)
                return
            }

            guard let data = data else {
                print("Without data (data == nil)")
                ErrorHandler.reject(call, code: .httpRequestError)
                return
            }

                do {
                   
                    struct DevicesResponse: Decodable {
                        let resultCount: Int?
                    }
                    let decoded = try JSONDecoder().decode(DevicesResponse.self, from: data)
                    let resultCount = decoded.resultCount ?? 0
                    let emptyTokenServer = (resultCount == 0)

                    let emptyMechanism = try self.validateExistMechanism();
                    print("hasDeviceToken", !emptyMechanism);
                    print("hasServerToken", !emptyTokenServer);
                    
                    call.resolve([
                        "hasServerToken": !emptyTokenServer,
                        "hasDeviceToken": !emptyMechanism
                    ])
                } catch {
 
                    NSLog("[OTPTokenHandler] JSON parse error: \(error)")
                    ErrorHandler.reject(call, code: .httpRequestError)
                }
            }.resume()
        

    }
    
    func hasRegisteredMechanism(_ call: CAPPluginCall) {
        do {
            let empty = try validateExistMechanism()

            var result = JSObject()
            result["empty"] = empty;
            call.resolve(result)
        } catch let error as ErrorCode {
            ErrorHandler.reject(call, code: error)
        } catch {
            ErrorHandler.reject(call, code: .unknown_error)
        }
    }


    func validateExistMechanism() throws -> Bool {

            if let accounts = fraClient?.getAllAccounts() {
                var empty = true;
                for account in accounts {
                    for mech in account.mechanisms {
                        empty = false;
                    }
                }

                return empty;
            }else {
                throw ErrorCode.noAccountsRegistered
            }
    }
    
    func generateOTP(call: CAPPluginCall) {
    do {
            guard let accounts = fraClient?.getAllAccounts(), !accounts.isEmpty else {
                throw ErrorCode.noAccountsRegistered
            }

             let token = try generateCode(from: accounts)

             var result = JSObject()
             result["otp"] = token.otp
             result["expiresIn"] = token.time
             call.resolve(result)
        
        } catch let error as ErrorCode {
            ErrorHandler.reject(call, code: error)
        } catch {
            ErrorHandler.reject(call, code: .unknown_error)
        }
    }


    func generateCode(from accounts: [Account]) throws -> (otp: String, time: Int)  {
        
        guard !accounts.isEmpty else {
           throw ErrorCode.noAccountsRegistered
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
        throw ErrorCode.noOtpRegistered
    }
    
    func getRemainingTime(from token: OathTokenCode) -> Int {
        guard let until = token.until else { return 0}
        let now = Date().timeIntervalSince1970
        return Int(until - now)
    }

}



