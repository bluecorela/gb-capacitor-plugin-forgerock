import Capacitor
import FRAuthenticator
import FRAuth
import Foundation

@objc public class NodeForgotPasswordCallbacks: NSObject {
    private let call: CAPPluginCall
    let plugin: ForgerockBridgePlugin
    var errorMessage: String
    private let idPath: ForgotPasswordIdPath
    
    init(call: CAPPluginCall, plugin: ForgerockBridgePlugin, idPath: ForgotPasswordIdPath) {
        self.call = call
        self.plugin = plugin
        self.errorMessage = "Unknown error"
        self.idPath = idPath
    }
    
    func handle(token: Token?, node: Node?, error: Error?) {
        print("CALL NodeForgotPasswordCallbacks handle")
        if let error = error {
            print(error)
            ErrorHandler.reject(call, code: .unknown_error, message: "Unknown error")
            return
        }
        
        if let node = node {
            onCallbackReceived(node)
            return
        }
        
        if let token = token  {
            onSuccess()
            return
        }
        
        ErrorHandler.reject(call, code: .unknown_error, message: "Unknown error")
        
    }
    
    private func onSuccess() {
        plugin.pendingNode = nil
        print("[NodeForgotPasswordCallback:onSuccess] Password changed successfully")
        call.resolve([
            "status": "success",
            "message": "Password changed successfully"
        ])
    }
    
    private func onCallbackReceived(_ node: Node) {
        print("[NodeForgotPasswordCallback:onCallbackReceived] CALL onCallbackReceived")
        
        let activeNode = node
        //        let answer = call.getString("answer")
        //
        //        var hasName = false
        //        var hasTextOutput = false
        //        var hasQuestion = false;
        //
        //        for (_, callback) in node.callbacks.enumerated() {
        //            if let name = callback as? NameCallback {
        //                hasName = true
        //            } else if let passwordCallback = callback as? PasswordCallback {
        //                hasQuestion = true
        //            } else if let textCb = callback as? TextOutputCallback {
        //                hasTextOutput = true
        //                self.errorMessage = textCb.message
        //            }
        //        }
        print(idPath)
        switch idPath {
        case .initForgotPass:
            self.initForgotPasswordHandler(activeNode)
            return
        case .answerQuestion:
            self.answerQuestionHandler(activeNode)
            return
        case .changePass:
            print("Change password")
            return
        }
        
        call.resolve(["status": "error", "message":"Unhandled node state"])
        return
        
        //        if (hasTextOutput) {
        //            print("[NodeForgotPasswordCallback:onCallbackReceived] error TextOutput")
        //            call.resolve([
        //                "status": "error",
        //                "message": self.errorMessage])
        //            return
        //        }
        //
        //        if(hasName) {
        //            for callback in node.callbacks {
        //                if let name = callback as? NameCallback {
        //                    name.setValue(username)
        //                    break
        //                }
        //            }
        //            node.next(completion: self.handle)
        //            return
        //        }
        //
        //        if (hasQuestion && answer == nil) {
        //            print("[NodeForgotPasswordCallback:onCallbackReceived] verified username")
        //            plugin.pendingNode = activeNode
        //            call.resolve(["status": "success", "message": "verified username"])
        //            return
        //        }
        //
        //
        //        if (answer != nil) {
        //            plugin.pendingNode = activeNode
        //            print("[NodeForgotPasswordCallback:onCallbackReceived] question success")
        //            call.resolve([
        //                "status": "success",
        //                "message": "question success"
        //            ])
        //            return
        //        }
        
        
    }
    
    private func initForgotPasswordHandler(_ node: Node) {
        
        let username = call.getString("username")
        
        for (_, callback) in node.callbacks.enumerated() {
            if let textCb = callback as? TextOutputCallback {
                self.errorMessage = textCb.message
                call.resolve([
                    "status": "error",
                    "message": self.errorMessage
                ])
                return
            } else if let name = callback as? NameCallback {
                name.setValue(username)
                node.next(completion: self.handle)
                return
            } else if let passwordCallback = callback as? PasswordCallback {
                print("[NodeForgotPasswordCallback:initForgotPasswordHandler] verified username")
                plugin.pendingNode = node
                call.resolve(["status": "success", "message": "verified username"])
                return
            }
        }
    }
    
    private func answerQuestionHandler (_ node: Node) {
        
        var hasTextOutput = false;
        var hasConfirmationCallback = false;
        
        for (_, callback) in node.callbacks.enumerated() {
            if let textCb = callback as? TextOutputCallback {
                print("[NodeForgotPasswordCallback:answerQuestionHandler] TextOutputCallback")
                print(self.errorMessage)
                self.errorMessage = textCb.message
                hasTextOutput = true
            } else if let confirmationCallback = callback as? ConfirmationCallback {
                confirmationCallback.value = 0
                hasConfirmationCallback = true
            }
            
        }
        
        if(hasTextOutput && hasConfirmationCallback) {
            
            node.next { (session: FRSession?, nextNode: Node?, error: Error?) in
                
                if let error = error {
                    print("[NodeForgotPasswordCallback:answerQuestionHandler] Error al enviar credenciales")
                    self.call.resolve([
                        "status": "error",
                        "message": self.errorMessage ?? error.localizedDescription
                    ])
                } else if let session = session {
                    print("[NodeForgotPasswordCallback:answerQuestionHandler] Obtuvo la session)")
                    
                } else if let nextNode = nextNode {
                    print("Respuesta con siguiente node")
                   
                } else {
                    print("[ForgeRock] Estado inesperado")
                    
                }
                
            }
            
        }
        
    }
    
}
