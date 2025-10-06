import Capacitor
import FRAuth
import Foundation

@objc public class NodeAuthCallBacks: NSObject {
    private let call: CAPPluginCall
    let plugin: ForgerockBridgePlugin
    var errorMessage: String

    init(call: CAPPluginCall, plugin: ForgerockBridgePlugin) {
        self.call = call
        self.plugin = plugin
        self.errorMessage = ""
    }

    func handle(node: Node) {
        guard let username = call.getString("username"),
              let password = call.getString("password") else {
            self.call.reject("Missing credentials")
            return
        }

        let isRetry = call.getBool("isRetry") ?? false
        let activeNode = plugin.pendingNode ?? node

        var hasErrorMessage = false
        var hasConfirmation = false
        var hasNameAndPasswordCallbacks = false

        for callback in activeNode.callbacks {
            switch callback {
            case is NameCallback, is PasswordCallback:
                if isRetry {
                    hasNameAndPasswordCallbacks = true
                }

            case let textOutput as TextOutputCallback:
                hasErrorMessage = true
                self.errorMessage = textOutput.message
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

        // 🚧 Primer intento con error (FRE015), guardar pendingNode y esperar retry
        if hasErrorMessage && hasConfirmation && !isRetry && !self.errorMessage.contains("FRE016") {
            if plugin.pendingNode == nil {
                plugin.pendingNode = activeNode
            }

            plugin.didSubmitConfirmation = false

            call.resolve([
                "status": "awaitingRetry",
                "errorMessage": self.errorMessage ?? "Unknown error",
                "callbacks": activeNode.callbacks.map { String(describing: type(of: $0)) }
            ])
            return
        }
        

        // Segundo intento: responder ConfirmationCallback, pero no avanzar — esperamos que el usuario reenvíe credenciales
        if isRetry && hasConfirmation && !plugin.didSubmitConfirmation {
            plugin.didSubmitConfirmation = true

            activeNode.next { (user: FRUser?, nextNode: Node?, error: Error?) in
                if let error = error {
                    self.call.resolve([
                        "status": "authenticateFailed",
                        "errorMessage": self.errorMessage ?? error.localizedDescription,
                        "callbacks": activeNode.callbacks.map { String(describing: type(of: $0)) }
                    ])
                } else if let nextNode = nextNode {
                    self.plugin.pendingNode = nextNode

                    self.call.resolve([
                        "status": "awaitingRetry",
                        "errorMessage": self.errorMessage,
                        "callbacks": nextNode.callbacks.map { String(describing: type(of: $0)) }
                    ])
                } else if let user = user {
                    self.plugin.pendingNode = nil
                    self.onSuccess(token: user.token)
                } else {
                    self.call.reject("Unexpected authentication result")
                }
            }
            return
        }

        // Tercer intento: usuario reintenta con credenciales
        if isRetry && hasNameAndPasswordCallbacks && plugin.didSubmitConfirmation {
            continueWithLogin(node: activeNode, username: username, password: password)
            return
        }

        // Retry recibido sin ConfirmationCallback (nuevo intento)
        if isRetry && hasNameAndPasswordCallbacks {

            if plugin.pendingNode !== activeNode {
                plugin.pendingNode = activeNode
            }

            plugin.didSubmitConfirmation = false

            call.resolve([
                "status": "awaitingRetry",
                "errorMessage": errorMessage ?? "Waiting for user to retry",
                "callbacks": activeNode.callbacks.map { String(describing: type(of: $0)) }
            ])
            return
        }

        // Primer intento exitoso
        continueWithLogin(node: activeNode, username: username, password: password)
    }

    private func continueWithLogin(node: Node, username: String, password: String) {
        
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