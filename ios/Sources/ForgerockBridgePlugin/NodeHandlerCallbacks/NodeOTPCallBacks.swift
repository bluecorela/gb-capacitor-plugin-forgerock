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

    func handle(token: Token?, node: Node?, error: Error?) {
        
        if let error = error {
            print("error", error)
            call.reject("Auth error: \(error.localizedDescription)")
            return
        }

        if let node = node {
            print("node", node)
            onCallbackReceived(node)
            return
        }
    }


    private func onCallbackReceived(_ node: Node) {
        print("onCallbackReceived", node)
        
        var hasError = false;
        var messageError = "";
        var url: String? ;
        for (index, callback) in node.callbacks.enumerated() {
            
            switch callback {
                case let textCb as TextOutputCallback:
                    if(textCb.messageType.rawValue == 2){
                        hasError = true
                        messageError = textCb.message
                    }else if(textCb.messageType.rawValue == 3){
                        url = self.extractOTPURL(message: textCb.message) ?? ""
                    }
                case let hiddenCb as HiddenValueCallback:
                    print("HiddenValueCallback - valor:", hiddenCb.getValue() ?? "nil")
                case let confCb as ConfirmationCallback:
                    print("ConfirmationCallback - valor actual:", confCb.value)
                default:
                        print("Callback sin caso específico:", callback)
           }
            
            if(url != ""){
                let uri = URL(string: url ?? "null")
                self.createMechanismFromUri(otpURL: URL(string: url ?? "null") ?? URL(string: "null")!)
                node.next(completion: self.handle)
            }
        }
       
    }

    private func createMechanismFromUri(otpURL: URL){
        guard let fraClient = FRAClient.shared else {
            print("FRAClient no está inicializado")
            return
        }

        fraClient.createMechanismFromUri(
            uri: otpURL,
            onSuccess: { mechanism in
                print("OTP registrado:", mechanism.mechanismUUID)
            },
            onError: { error in
                print("Error al registrar mecanismo:", error.localizedDescription)
            }
        )
    }

    private func extractOTPURL(message: String) -> String? {
        if let rangeStart = message.range(of: "'otpauth://"),
           let rangeEnd = message.range(of: "'", range: rangeStart.upperBound..<message.endIndex) {
            
            let uri = String(message[rangeStart.lowerBound..<rangeEnd.upperBound])
                .replacingOccurrences(of: "'", with: "")

            print("OTP URL extraída: \(uri)")
            return uri
        }
        print("No se pudo extraer la URI del mensaje")
        return nil
    }


}
