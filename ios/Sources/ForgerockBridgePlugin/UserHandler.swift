import Foundation
import Capacitor
import FRAuth

class UserHandler {
    static func logout(call: CAPPluginCall) {
        FRUser.currentUser?.logout()
        call.resolve(["status": "loggedOut"])
    }

    static func userInfo(call: CAPPluginCall) {
        guard let user = FRUser.currentUser else {
            call.reject("error", "No current user", nil)
            return
        }

        user.getUserInfo { userInfo, error in
            if let error = error {
                call.reject("error", error.localizedDescription, error)
            } else if let userInfo = userInfo {
                call.resolve(userInfo.userInfo)
            } else {
                call.reject("error", "userInfo returned no result", nil)
            }
        }
    }
    
    static func getCurrentSession(call: CAPPluginCall) {
        let sessionToken = FRSession.currentSession?.sessionToken?.value
        call.resolve([
            "currentSession": sessionToken
        ])
    }
}
