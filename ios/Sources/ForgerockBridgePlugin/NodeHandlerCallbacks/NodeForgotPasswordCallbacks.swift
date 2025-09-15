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
            print("PASE POR SUCCES FINAL")
            onSuccess()
            return
        }
        
        ErrorHandler.reject(call, code: .unknown_error, message: "Unknown error")
        
    }
    
    private func onSuccess() {
        plugin.pendingNode = nil
        FRSession.currentSession?.logout()
        print("[NodeForgotPasswordCallback:onSuccess] Password changed successfully")
        call.resolve([
            "status": "success",
            "message": "Password changed successfully"
        ])
    }
    
    private func onCallbackReceived(_ node: Node) {
        print("[NodeForgotPasswordCallback:onCallbackReceived] CALL onCallbackReceived")
        
        let activeNode = node
        
        switch idPath {
        case .initForgotPass:
            self.initForgotPasswordHandler(activeNode)
            return
        case .answerQuestion:
            self.answerQuestionHandler(activeNode)
            return
        case .changePass:
            self.changePassHandler(activeNode)
            return
        }
        
        call.resolve(["status": "error", "message":"Unhandled node state"])
        return
        
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
                print("[NodeForgotPasswordCallback:answerQuestionHandler] confirmationCallback")
                confirmationCallback.value = 0
                hasConfirmationCallback = true
            }
            
        }
        
        if(hasTextOutput && hasConfirmationCallback) {
            
            node.next { (user: FRUser?, nextNode: Node?, error: Error?) in
                if let error = error {
                    //onException
                    for (_, callback) in node.callbacks.enumerated() {
                        if let textCb = callback as? TextOutputCallback {
                            self.call.resolve([
                                "status": "error",
                                "message": textCb.message,
                            ])
                            return
                        }
                    }
                    
                } else if let nextNode = nextNode {
                    //onCallback
                    self.plugin.pendingNode = nextNode
                    self.call.resolve([
                        "status": "error",
                        "message": self.errorMessage,
                    ])
                } else if let user = user {
                    //onSuccess
                    self.call.resolve([
                        "status": "success",
                        "message": "",])
                } else {
                    self.call.resolve([
                        "status": "error",
                        "message": "Unhandle case answer question",])
                }
            }
           return
        }
        
        plugin.pendingNode = node
        call.resolve([
            "status": "success",
            "message": "question success"
        ])
        
    }
    
    private func changePassHandler (_ node: Node) {
        
        var hasTextOutput = false;
        var hasConfirmationCallback = false;
        
        for (_, callback) in node.callbacks.enumerated() {
            if let textCb = callback as? TextOutputCallback {
                print("[NodeForgotPasswordCallback:changePassHandler] TextOutputCallback")
                print(self.errorMessage)
                self.errorMessage = textCb.message
                hasTextOutput = true
            } else if let confirmationCallback = callback as? ConfirmationCallback {
                print("[NodeForgotPasswordCallback:changePassHandler] confirmationCallback")
                confirmationCallback.value = 0
                hasConfirmationCallback = true
            }
            
        }
        
        if(hasTextOutput && hasConfirmationCallback) {
            
            node.next { (user: FRUser?, nextNode: Node?, error: Error?) in
                if let error = error {
                    //onException
                    self.call.resolve([
                        "status": "error",
                        "message": "onException changePassHandler" ])
                } else if let nextNode = nextNode {
                    //onCallback
                    self.plugin.pendingNode = nextNode
                    self.call.resolve([
                        "status": "error",
                        "message": self.errorMessage,
                    ])
                } else if let user = user {
                    //onSuccess
                    self.call.resolve([
                        "status": "success",
                        "message": "Password changed successfully"])
                } else {
                    self.call.resolve([
                        "status": "error",
                        "message": "Unhandle case changePassHandler",])
                }
            }
            
        }
        
    }
    
}
