import Capacitor
import FRAuthenticator
import FRAuth
import Foundation

@objc public class NodeForgotPasswordCallbacks: NSObject {
    private let call: CAPPluginCall
    let plugin: ForgerockBridgePlugin
    var errorMessage: String

    init(call: CAPPluginCall, plugin: ForgerockBridgePlugin) {
        self.call = call
        self.plugin = plugin
        self.errorMessage = "Unknown error"
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

    private func onCallbackReceived(_ node: Node) {
        print("[NodeForgotPasswordCallback:onCallbackReceived] CALL onCallbackReceived")
        let activeNode = node
        let username = call.getString("username")
        let answer = call.getString("answer")

        var hasName = false
        var hasTextOutput = false
        var hasQuestion = false;

        for (_, callback) in node.callbacks.enumerated() {
            if let name = callback as? NameCallback {
                hasName = true
            } else if let passwordCallback = callback as? PasswordCallback {
                hasQuestion = true
            } else if let textCb = callback as? TextOutputCallback {
                hasTextOutput = true
                self.errorMessage = textCb.message
            }

            
        }

        if (hasTextOutput) {
            print("[NodeForgotPasswordCallback:onCallbackReceived] error TextOutput")
            call.resolve([
                "status": "error",
                "message": self.errorMessage])
            return
        }

        if(hasName) {
            for callback in node.callbacks {
                if let name = callback as? NameCallback {
                    name.setValue(username)
                    break
                } 
            }
            node.next(completion: self.handle)
            return
        }

        if (hasQuestion && answer == nil) {
            print("[NodeForgotPasswordCallback:onCallbackReceived] verified username")
            plugin.pendingNode = activeNode
            call.resolve(["status": "success", "message": "verified username"])
            return
        }
    
        
        if (answer != nil) {
            plugin.pendingNode = activeNode
            print("[NodeForgotPasswordCallback:onCallbackReceived] question success")
            call.resolve([
                "status": "success",
                "message": "question success"
            ])
            return
        }

        call.resolve(["status": "Unhandled node state."])
        return
        
    }
    
    private func onSuccess() {
        plugin.pendingNode = nil
        print("[NodeForgotPasswordCallback:onCallbackReceived] Password changed successfully")
        call.resolve([
            "status": "success",
            "message": "Password changed successfully"
        ])
    }

}
