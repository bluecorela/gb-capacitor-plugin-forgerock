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
        self.errorMessage = ""
    }

    func handle(token: Token?, node: Node?, error: Error?) {
        print("CALL NodeForgotPasswordCallbacks handle")
        if let error = error {
            ErrorHandler.reject(call, code: .authenticateFailed)
            return
        }

        if let node = node {
            onCallbackReceived(node)
            return
        }
    }

    private func onCallbackReceived(_ node: Node) {
        let activeNode = plugin.pendingNode ?? node
        let username = call.getString("username")

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
            call.resolve([
                "errorMessage": self.errorMessage ?? "Unknown error",
            ])
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

        if (hasQuestion) {
            plugin.pendingNode = activeNode
            call.resolve(["status": "verified_username"])
            return;
        }

        call.resolve(["status": "Unhandled node state."])
        
    }


}
