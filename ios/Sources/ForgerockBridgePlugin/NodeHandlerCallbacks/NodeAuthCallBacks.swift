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
        print("[ForgeRock] activeNode.callbacks.map: \(activeNode.callbacks.map { String(describing: type(of: $0)) })")

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
                print("[ForgeRock] TextOutputCallback: \(textOutput.message)")
                if textOutput.message.contains("FRE016") {
                      print("[ForgeRock] FRE016 detectado — reseteando estado del plugin")
                      plugin.pendingNode = nil
                      plugin.didSubmitConfirmation = false
                  }

            case let confirmation as ConfirmationCallback:
                hasConfirmation = true
                print("[ForgeRock] ConfirmationCallback inputName: \(confirmation.inputName ?? "unknown")")

                if isRetry {
                    confirmation.value = 0
                    print("[ForgeRock] ConfirmationCallback set with value 0")
                }

            default: break
            }
        }

        // 💬 Logging estado de control
        print("[ForgeRock] 🔁 isRetry: \(isRetry), hasNameAndPasswordCallbacks: \(hasNameAndPasswordCallbacks), didSubmitConfirmation: \(plugin.didSubmitConfirmation)")

        // 🚧 Primer intento con error (FRE015), guardar pendingNode y esperar retry
        if hasErrorMessage && hasConfirmation && !isRetry && !self.errorMessage.contains("FRE016") {
            if plugin.pendingNode == nil {
                print("[ForgeRock] Saving node with TextOutput + ConfirmationCallback as pendingNode")
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

        // 🚧 Segundo intento: responder ConfirmationCallback, pero no avanzar — esperamos que el usuario reenvíe credenciales
        if isRetry && hasConfirmation && !plugin.didSubmitConfirmation {
            print("[ForgeRock] En retry: procesamos ConfirmationCallback (value = 0), pero NO avanzamos")
            plugin.didSubmitConfirmation = true

            activeNode.next { (user: FRUser?, nextNode: Node?, error: Error?) in
                if let error = error {
                    print("[ForgeRock] Error durante next(): \(error)")
                    self.call.resolve([
                        "status": "authenticateFailed",
                        "errorMessage": self.errorMessage ?? error.localizedDescription,
                        "callbacks": activeNode.callbacks.map { String(describing: type(of: $0)) }
                    ])
                } else if let nextNode = nextNode {
                    print("[ForgeRock] Recibido siguiente node tras ConfirmationCallback → esperamos tercer intento")
                    self.plugin.pendingNode = nextNode

                    self.call.resolve([
                        "status": "awaitingRetry",
                        "errorMessage": self.errorMessage,
                        "callbacks": nextNode.callbacks.map { String(describing: type(of: $0)) }
                    ])
                } else if let user = user {
                    print("[ForgeRock] Autenticación completa tras ConfirmationCallback")
                    self.plugin.pendingNode = nil
                    self.onSuccess(token: user.token)
                } else {
                    print("[ForgeRock] Estado inesperado tras ConfirmationCallback")
                    self.call.reject("Unexpected authentication result")
                }
            }
            return
        }

        // 🚨 Tercer intento: usuario reintenta con credenciales
        if isRetry && hasNameAndPasswordCallbacks && plugin.didSubmitConfirmation {
            print("[ForgeRock] Tercer intento: enviando node con credenciales tras ConfirmationCallback")
            continueWithLogin(node: activeNode, username: username, password: password)
            return
        }

        // 💤 Retry recibido sin ConfirmationCallback (nuevo intento)
        if isRetry && hasNameAndPasswordCallbacks {
            print("[ForgeRock] Deteniendo flujo tras retry; espera nuevo intento del usuario")

            if plugin.pendingNode !== activeNode {
                print("[ForgeRock] Actualizando pendingNode con nuevo Name/Password callbacks")
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

        // ✅ Primer intento exitoso
        print("[ForgeRock] Continuando con flujo normal (primer intento u otro flujo no interceptado)")
        continueWithLogin(node: activeNode, username: username, password: password)
    }

    private func continueWithLogin(node: Node, username: String, password: String) {
        print("[ForgeRock] Enviando credenciales del usuario")

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
