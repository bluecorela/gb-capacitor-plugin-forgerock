import Foundation
import Capacitor
import FRAuth

class AuthenticationHandler {
    private let call: CAPPluginCall
    private let plugin: ForgerockBridgePlugin

    init(call: CAPPluginCall, plugin: ForgerockBridgePlugin) {
        self.call = call
        self.plugin = plugin
    }

    func authenticate() {
        let isRetry = call.getBool("isRetry") ?? false
        let handler = NodeAuthCallBacks(call: call, plugin: plugin)

        if isRetry {
            guard let pendingNode = plugin.pendingNode else {
                call.reject("No pending node to resume")
                return
            }
            handler.handle(node: pendingNode)
            return
        }

        guard let journey = call.getString("journey") else {
            call.reject("Missing required parameter: journey")
            return
        }

        FRSession.authenticate(authIndexValue: journey) { token, node, error in
            if let error = error {
                self.call.reject("Error starting authentication: \(error.localizedDescription)")
            } else if let node = node {
                handler.handle(node: node)
            } else if let token = token {
                handler.onSuccess(token: token)
            } else {
                self.call.reject("Unexpected authentication result")
            }
        }
    }
}
