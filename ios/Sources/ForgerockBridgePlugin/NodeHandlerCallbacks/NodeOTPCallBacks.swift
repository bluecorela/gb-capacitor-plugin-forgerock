import Capacitor
import FRAuth
import Foundation

@objc public class NodeOTPCallBacks: NSObject {
    private let call: CAPPluginCall
    let plugin: ForgerockBridgePlugin
    var errorMessage: String

    init(call: CAPPluginCall, plugin: ForgerockBridgePlugin) {
        self.call = call
        self.plugin = plugin
        self.errorMessage = ""
    }

    func handle(node: Node) {

        let isRetry = call.getBool("isRetry") ?? false
        let activeNode = plugin.pendingNode ?? node

        var hasErrorMessage = false
        var hasConfirmation = false
        var hasNameAndPasswordCallbacks = false

        for callback in activeNode.callbacks {
            print("call", callback)
            switch callback {

            case let textOutput as TextOutputCallback:
                hasErrorMessage = true
                self.errorMessage = textOutput.message
                print("textOutput.message",textOutput.message)
                if textOutput.message.contains("FRE016") {
                      plugin.pendingNode = nil
                      plugin.didSubmitConfirmation = false
                  }

            case let confirmation as ConfirmationCallback:
                hasConfirmation = true
                if isRetry {
                    confirmation.value = 0
                }

            default: break
            }
        }

    }

    private func continueWithLogin(node: Node, username: String, password: String) {
        print("vino aca", self.errorMessage)
        for callback in node.callbacks {
            if let name = callback as? NameCallback {
                name.setValue(username)
            } else if let pass = callback as? PasswordCallback {
                pass.setValue(password)
            }
        }

        node.next { (user: FRUser?, nextNode: Node?, error: Error?) in
           
            if let error = error {
                print("[ForgeRock] Error al enviar credenciales: \(error)")
                self.call.resolve([
                    "status": "authenticateFailed",
                    "errorMessage": self.errorMessage ?? error.localizedDescription,
                    "callbacks": node.callbacks.map { String(describing: type(of: $0)) }
                ])
            } else if let user = user {
                print("[ForgeRock] Autenticación exitosa, token: \(user.token)")
                self.plugin.pendingNode = nil
                self.plugin.didSubmitConfirmation = false
                self.onSuccess(token: user.token)
            } else if let nextNode = nextNode {
                print("[ForgeRock] Respuesta con siguiente node")
                self.plugin.pendingNode = nil
                self.handle(node: nextNode)
            } else {
                print("[ForgeRock] Estado inesperado tras login: sin node ni token")
                self.call.reject("Unexpected authentication result")
            }
        }
    }

    public func onSuccess(token: Token?) {
        if let token = token {
            self.call.resolve([
                "status": "authenticated",
                "token": token.value,
            ])
        } else {
            print("[ForgeRock] Unexpected state — no token, node, or error.")
            self.call.reject("Unexpected authentication result")
        }
    }
}
