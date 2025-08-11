import Capacitor
import FRAuthenticator
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

        print("vino handle", node)
        let activeNode = plugin.pendingNode ?? node

        print("activeNode", activeNode)
        var hasErrorMessage = false
        var hasConfirmation = false
        var hiddenValue = false


         for callback in activeNode.callbacks {
             print("call", callback)
            switch callback {

            case let textOutput as TextOutputCallback:
                hasErrorMessage = true
                var messageType: MessageType? = nil
                
                self.errorMessage = textOutput.message
                
                messageType = textOutput.messageType
                
                extractOTPURL(message: self.errorMessage, currentNode: node )

            case let confirmation as ConfirmationCallback:
                hasConfirmation = true
                print("ConfirmationCallback",hasConfirmation)
            case let hiddenValue as HiddenValueCallback:
                var uri = hiddenValue.getValue()
                print("hiddenValue", uri)
            default: break
            }
         }
    }

    private func extractOTPURL(message: String, currentNode: Node) {
        if message.contains("otpauth://") {
            
            // Extraer la URI entre comillas
            if let rangeStart = message.range(of: "'otpauth://"),
               let rangeEnd = message.range(of: "'", range: rangeStart.upperBound..<message.endIndex) {
                var uri = String(message[rangeStart.lowerBound..<rangeEnd.upperBound])
                    .replacingOccurrences(of: "'", with: "")
                               
                guard let url = URL(string: uri) else {
                    print("URI inválido: \(uri)")
                    return
                }
    
                print("✅ OTP URL extraída: \(url)")
                
                guard let fraClient = FRAClient.shared else {
                    print("FRAClient no está inicializado")
                    return
                }

                fraClient.createMechanismFromUri(
                    uri: url,
                    onSuccess: { mechanism in
                        print("OTP registrado:", mechanism.mechanismUUID)

                        // Finalizar flujo del árbol (importante para completar el journey)
                        currentNode.next { (_: FRUser?, nextNode: Node?, error: Error?) in
                            if let error = error {
                                print("Error al avanzar en el árbol:", error.localizedDescription)
                            } else if let nextNode = nextNode {
                                print("➡️ Nodo siguiente recibido:", nextNode)
                                self.handle(node: nextNode)
                            } else {
                                print("Registro OTP completado exitosamente")
                            }
                        }
                    },
                    onError: { error in
                        print("Error al registrar mecanismo:", error.localizedDescription)
                    }
                )
            } else {
                print("No se pudo extraer la URI del mensaje")
            }
        }
    }

}
