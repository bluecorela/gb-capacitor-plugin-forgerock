import Capacitor
import FRAuthenticator
import FRAuth
import Foundation

@objc public class NodeOTPDeleteCallBacks: NSObject {
    private let call: CAPPluginCall
    let plugin: ForgerockBridgePlugin


    init(call: CAPPluginCall, plugin: ForgerockBridgePlugin) {
        self.call = call
        self.plugin = plugin
    }

    func handle(token: Token?, node: Node?, error: Error?) {
        if let error = error {
            ErrorHandler.reject(call, code: OTPErrorCode.authenticateFailed)
            return
        }

        if let node = node {
            node.next(completion: self.handle)
            return
        }

        if token != nil {
            self.deleteLocalMechanism()
        }
    }
    
    func deleteLocalMechanism (){
        guard let client = FRAClient.shared else {
           ErrorHandler.reject(call, code: OTPErrorCode.withOutInitializedShared)
           return
        }
    
         let accounts = client.getAllAccounts()
            var deletedCount = 0
                
                for account in accounts {
                    for mechanism in account.mechanisms {
                        do {
                            try client.removeMechanism(mechanism: mechanism)
                            deletedCount += 1
                        } catch {
                            ErrorHandler.reject(call, code: OTPErrorCode.deleteOTPFailed)
                        }
                    }
                }
                
                if deletedCount > 0 {
                    call.resolve(["status": "success", "deletedCount": deletedCount])
                } else {
                    ErrorHandler.reject(call, code: OTPErrorCode.noOtpRegistered)
                }
    }
}
