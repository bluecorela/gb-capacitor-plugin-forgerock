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
    
    func isValidAuthMethod(call: CAPPluginCall) {
      guard let baseUrl = call.getString("url"), !baseUrl.isEmpty else {
        call.reject("Missing 'url'")
        return
      }
    
     let trxId = call.getString("trxId") ?? ""

      var payloadJson = "{}"
      if let payloadObj = call.getObject("payload"),
         let data = try? JSONSerialization.data(withJSONObject: payloadObj, options: []),
         let jsonStr = String(data: data, encoding: .utf8) {
        payloadJson = jsonStr
      }

      let adviceXml = "<Advices><AttributeValuePair><Attribute name='TransactionConditionAdvice'/><Value>\(trxId)</Value></AttributeValuePair></Advices>"

      guard var comps = URLComponents(string: baseUrl) else {
        call.reject("Invalid 'url'")
        return
      }

      var items = comps.queryItems ?? []
      items.append(URLQueryItem(name: "authIndexType", value: "composite_advice"))
      items.append(URLQueryItem(name: "authIndexValue", value: adviceXml))
      comps.queryItems = items

      guard let finalUrl = comps.url?.absoluteString else {
        call.reject("Failed to build URL")
        return
      }

      executeHttpQuery(urlString: finalUrl, payload: payloadJson, call: call)
    }


    func executeHttpQuery(urlString: String, payload: String, call: CAPPluginCall){
        let sessionToken = FRSession.currentSession?.sessionToken?.value
        let cookie = "iPlanetDirectoryPro=\(sessionToken ?? "")"
         
        guard let url = URL(string: urlString) else {
            ErrorHandler.reject(call, code: .httpRequestError)
            return
       }
        
        print("PAYLOAD executeHttpQuery: \(payload)")
        var req = URLRequest(url: url)
        if let bodyData = payload.data(using: .utf8) {
            req.httpBody = bodyData
        }
            req.httpMethod = "POST"
            req.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
            req.setValue("application/json", forHTTPHeaderField: "Accept")
            req.setValue("protocol=1.0,resource=2.0", forHTTPHeaderField: "Accept-API-Version")
            req.setValue("XMLHttpRequest", forHTTPHeaderField: "X-Requested-With")
            req.setValue(cookie, forHTTPHeaderField: "Cookie")
            

           print("REQUEST: \(req)")
        
        
        self.handleHttpRequest(call: call, request: req)
   }
    
    func handleHttpRequest(call: CAPPluginCall, request: URLRequest) {
        
        URLSession.shared.dataTask(with: request) { data, response, error in
            print("DATA: \(String(describing: data))")
            print("RESPONSE2: \(String(describing: response))")
            print("ERROR: \(String(describing: error))")
            
            
            if let error = error {
                print("[OTPTokenHandler] network error: \(error)")
                ErrorHandler.reject(call, code: .httpRequestError)
                return
            }
        
            guard let http = response as? HTTPURLResponse else {
                print("ERROR RESPONSE")
                ErrorHandler.reject(call, code: .httpRequestError)
                return
            }
        
            guard (200...299).contains(http.statusCode) else {
                print("HTTP error: \(http.statusCode) - \(HTTPURLResponse.localizedString(forStatusCode: http.statusCode))")
                ErrorHandler.reject(call, code: .httpRequestError)
                return
            }
            
            if let data = data {
                if let jsonString = String(data: data, encoding: .utf8) {
                    print("[Raw JSON Response]: \(jsonString)")
                }
                self.AuthMethodResponse(call: call, data)
            }
    
        }.resume()
        
    }
    
    func AuthMethodResponse (call: CAPPluginCall, _ data: Data) {
        if let jsonObject = (try? JSONSerialization.jsonObject(with: data, options: [])) as? [String: Any] {
            
            let successResponse = jsonObject["tokenId"] as? String ?? nil
            let errorResponse = jsonObject["code"] as? String ?? nil
            
            if(successResponse != nil || errorResponse != nil){
                
                let status = (successResponse != nil) ? "success" : "failed"
                print("[OTPTokenHandler]: Data send to front: ")
                print(jsonObject)
                self.buildSuccessErrorCallback(call: call, status: status)
                return
                
            }else {
                print("[OTPTokenHandler]: Json sent to front: ");
                call.resolve(jsonObject)
                return
            }
            
        }
        
    }
    
    func buildSuccessErrorCallback (call: CAPPluginCall, status: String) {
        
        let outMessage: [String: Any] = [
                "name": "status",
                "value": status
        ]
            
        let successCallback: [String: Any] = [
            "type": "SuccessCallback",
            "output": [outMessage]
        ]
        
        let result: [String: Any] = [
            "callbacks": [successCallback]
        ]
        
        call.resolve(result)
        
    }
    
    func checkServerAndDeviceOtpState(call: CAPPluginCall, uuid: String){
        
        let sessionToken = FRSession.currentSession?.sessionToken?.value
        let cookie = "iPlanetDirectoryPro=\(sessionToken ?? "")"
         
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
            if let jsonString = String(data: data, encoding: .utf8) {
                print("[Raw JSON Response]: \(jsonString)")
            }

                do {
                   
                    struct DevicesResponse: Decodable {
                        let resultCount: Int?
                    }
                    let decoded = try JSONDecoder().decode(DevicesResponse.self, from: data)
                    let resultCount = decoded.resultCount ?? 0
                    var hasServerToken = !(resultCount == 0)

                    let mechanism = try self.validateExistMechanism();
                    let hasDeviceToken = !mechanism.empty;
                    
                    //If exist mechanism and token in server, has another validation. If mechanism and Server token are same.
                    //If not match = in this case hasDeviceToken always will be true, because in case of create new Token, the mechanism must be removed first and this response say to front thats exist mechanis and need delete first. If match same are true
                    if(hasDeviceToken && hasServerToken){
                        hasServerToken = self.isOtpConsistentWithServer(uuid: uuid, mechanism: mechanism.mechanism )
                    }
                  
                    call.resolve([
                        "hasServerToken": hasServerToken,
                        "hasDeviceToken": hasDeviceToken,

                    ])
                } catch {
 
                    NSLog("[OTPTokenHandler] JSON parse error: \(error)")
                    ErrorHandler.reject(call, code: .httpRequestError)
                }
            }.resume()

    }
    
    func isOtpConsistentWithServer(uuid: String, mechanism: Mechanism?) -> Bool{
        if let mechanism = mechanism {
            if(uuid ==  mechanism.accountName){
                return true;
            }
        }
        return false;
    }
    
    func hasRegisteredMechanism(_ call: CAPPluginCall) {
        do {
            let empty = try validateExistMechanism().empty

            var result = JSObject()
            result["empty"] = empty;
            call.resolve(result)
        } catch let error as ErrorCode {
            ErrorHandler.reject(call, code: error)
        } catch {
            ErrorHandler.reject(call, code: .unknown_error)
        }
    }
     func validateExistMechanism() throws -> (empty: Bool, mechanism: Mechanism?) {

                if let accounts = fraClient?.getAllAccounts() {
                    var empty = true;
                    var mechanism: Mechanism? = nil
                    for account in accounts {
                        if let first = account.mechanisms.first {
                           mechanism = first
                           empty = false
                           break
                        }
                    }
                    return (empty: empty, mechanism: mechanism)
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



