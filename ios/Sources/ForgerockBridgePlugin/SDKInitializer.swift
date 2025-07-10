import Foundation
import Capacitor
import FRAuth

class SDKInitializer {
    static func initialize(call: CAPPluginCall) {
        guard let urlString = call.getString("url"),
              let url = URL(string: urlString),
              let realm = call.getString("realm"),
              let journey = call.getString("journey"),
              let oauthClientId = call.getString("oauthClientId"),
              let oauthScope = call.getString("oauthScope")
        else {
            call.reject("Missing required parameters: url, realm, journey, oauthClientId, or oauthScope")
            return
        }
        
        let bundleId = Bundle.main.bundleIdentifier ?? "com.globalbank.app"

        do {
            let options = FROptions(
                url: url.absoluteString,
                realm: realm,
                cookieName: "iPlanetDirectoryPro",
                authServiceName: journey,
                oauthClientId: oauthClientId,
                oauthRedirectUri: "\(bundleId)://oauth2redirect",
                oauthScope: oauthScope)

            try FRAuth.start(options: options)
            print("[ForgeRock] SDK initialized")
            call.resolve(["status": "success"])
        } catch {
            print("[ForgeRock] Initialization failed: \(error)")
            call.reject("ForgeRock SDK initialization failed", error.localizedDescription)
        }
    }
}
