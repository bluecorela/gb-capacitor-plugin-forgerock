import Foundation
import Capacitor
import FRAuth

class TokenHandler {
    static func getAccessToken(call: CAPPluginCall) {
        guard let user = FRUser.currentUser else {
            call.reject("error", "No current user", nil)
            return
        }

        user.getAccessToken { user, error in
            if let error = error {
                call.reject("error", error.localizedDescription, error)
            } else if let user = user, let accessToken = user.token {
                let encoder = JSONEncoder()
                do {
                    let data = try encoder.encode(accessToken)
                    let string = String(data: data, encoding: .utf8)
                    call.resolve(["token": string ?? ""])
                } catch {
                    call.reject("Error", "Serialization of tokens failed", error)
                }
            } else {
                call.reject("error", "getAccessToken returned no result", nil)
            }
        }
    }
}
