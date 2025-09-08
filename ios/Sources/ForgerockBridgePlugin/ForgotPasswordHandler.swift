import Foundation
import Capacitor
import FRAuth
import FRCore
import FRAuthenticator

class ForgotPasswordHandler {
    private let call: CAPPluginCall
    private let plugin: ForgerockBridgePlugin

    init(call: CAPPluginCall, plugin: ForgerockBridgePlugin) {
        self.call = call
        self.plugin = plugin
    }

    func startForgotPasswordJourney (_ call: CAPPluginCall,completion: @escaping NodeCompletion<Token> ) {
        print("CALL startForgotPasswordJourney")
        FRLog.setLogLevel([.network,.info ])
        guard let journey = call.getString("journey"), !journey.isEmpty else {
            ErrorHandler.reject(call, code: ErrorCode.missingJourney)
            return
        }

        guard let username = call.getString("username"), !username.isEmpty else {
            ErrorHandler.reject(call, code: ErrorCode.missingParameter)
            return
        }
        
          FRSession.authenticate(authIndexValue: journey, completion: completion)
    }

    func getSecurityQuestion (_ call: CAPPluginCall) {
        print("CALL getSecurityQuestion")
        print(self.plugin.pendingNode)
        
        var question: String? = nil

        guard let pending = self.plugin.pendingNode else {
            print("[getSecurityQuestion] NO PENDING NODE SAVE")
            ErrorHandler.reject(call, code: .noPendingNode, message: "NO PENDING NODE SAVE")
            return
        }
        
        for (_, callback) in pending.callbacks.enumerated() {
            if let passwordCallback = callback as? PasswordCallback {
                question = passwordCallback.prompt
                break
            }
        }
        
        guard let q = question else {
            print("[getSecurityQuestion] NO QUESTION FOUND")
            ErrorHandler.reject(call, code: .noQuestionFound, message: "NO QUESTION FOUND")
            return
        }
        
        let out: [String: Any] = [
                    "question": q
                ]
                
        print("[getSecurityQuestion] Sending question")
        call.resolve(out)
    }
    
    func answerQuestionForgotPassword (_ call: CAPPluginCall,completion: @escaping NodeCompletion<Token> ) {
        
        guard let pending = self.plugin.pendingNode else {
            print("[answerQuestionForgotPassword] NO PENDING NODE SAVE")
            ErrorHandler.reject(call, code: .noPendingNode, message: "NO PENDING NODE SAVE")
            return
        }
        
        guard let answer = call.getString("answer"), !answer.isEmpty else {
            print("[answerQuestionForgotPassword] MISSING PARAMETER answer")
            ErrorHandler.reject(call, code: ErrorCode.missingParameter, message: "MISSING PARAMETER answer")
            return
        }
        
        for (_, callback) in pending.callbacks.enumerated() {
            if let passwordCallback = callback as? PasswordCallback {
                passwordCallback.setValue(answer)
                break
            }
        }
        
        pending.next(completion: completion)
        
    }
}
