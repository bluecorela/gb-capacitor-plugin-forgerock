import Capacitor
import FRAuthenticator
import FRAuth
import Foundation

@objc public class NodeOTPDeleteCallBacks: NSObject {
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
            node.next(completion: self.handle)
            return
        }

        if token != nil {
            print("token", token)
            self.deleteLocalMechanism()
        } else {
            call.reject("Unexpected auth state")
        }
    }
    
    func deleteLocalMechanism (){
            print("[deleteLocalMechanism]")
            guard let client = FRAClient.shared else {
                   call.reject("FRAClient no inicializado")
                   return
               }
    
               let accounts = client.getAllAccounts()
                var deletedCount = 0
                    
                    for account in accounts {
                        for mechanism in account.mechanisms {
                            do {
                                try client.removeMechanism(mechanism: mechanism)
                                print("Eliminado mecanismo \(mechanism ) de cuenta \(account.accountName ?? "-")")
                                deletedCount += 1
                            } catch {
                                print("Error eliminando mecanismo \(mechanism): \(error.localizedDescription)")
                            }
                        }
                    }
                    
                    if deletedCount > 0 {
                        call.resolve(["status": "success", "deletedCount": deletedCount])
                    } else {
                        call.resolve(["status": "no_mechanisms_found"])
                    }
        }
}
