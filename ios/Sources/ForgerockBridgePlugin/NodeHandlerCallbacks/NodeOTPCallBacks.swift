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
            ErrorHandler.reject(call, code: .authenticateFailed)
            return
        }

        if let node = node {
            onCallbackReceived(node)
            return
        }
    }


    private func onCallbackReceived(_ node: Node) {
        FRLog.setLogLevel([.network,.info ]);
        var hasError = false;
        var messageError = "";
        var url: String? ;
        for (_, callback) in node.callbacks.enumerated() {
            
            if let textCb = callback as? TextOutputCallback {
                if textCb.messageType.rawValue == 2 {
                    hasError = true
                    messageError = textCb.message
                } else if textCb.messageType.rawValue == 3 {
                    url = self.extractOTPURL(message: textCb.message) ?? ""
                }
            }
        }
        
        if(url != ""){
            self.createMechanismFromUri(otpURL: URL(string: url ?? "null") ?? URL(string: "null")!, node: node)
        }else if(hasError && url == ""){
            ErrorHandler.reject(call, code: .callbackFailed)
        }else{
            ErrorHandler.reject(call, code: .unknown_error)
        }
       
    }

    private func createMechanismFromUri(otpURL: URL, node: Node){
        guard let fraClient = FRAClient.shared else {
            ErrorHandler.reject(call, code: .withOutInitializedShared)
            return
        }

        fraClient.createMechanismFromUri(
            uri: otpURL,
            onSuccess: { mechanism in
                self.finalStepToRegisterOTP(node: node)
            },
            onError: { error in
                ErrorHandler.reject(self.call, code: .registerOTPFailed)
            }
        )
    }
    
    private func finalStepToRegisterOTP(node: Node) {
        node.next(completion: self.handle)
        var result = JSObject()
        result["status"] = "success";
        self.call.resolve(result)
    }

    private func extractOTPURL(message: String) -> String? {
        if let rangeStart = message.range(of: "'otpauth://"),
           let rangeEnd = message.range(of: "'", range: rangeStart.upperBound..<message.endIndex) {
            
            let uri = String(message[rangeStart.lowerBound..<rangeEnd.upperBound])
                .replacingOccurrences(of: "'", with: "")

            return uri
        }
        return nil
    }
    


}
