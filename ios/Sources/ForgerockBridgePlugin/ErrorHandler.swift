import Capacitor

enum ErrorCode: String, Error {
    case unknown_error              = "FRE000"
    case missingJourney             = "FRE024"
    case authenticateFailed         = "FRE025"
    case noAccountsRegistered       = "FRE026"
    case noOtpRegistered            = "FRE027"
    case withOutInitializedShared   = "FRE028"
    case deleteOTPFailed            = "FRE029"
    case callbackFailed             = "FRE030"
    case registerOTPFailed          = "FRE031"
    case gettingUserInfo            = "FRE033"
    case httpRequestError           = "FRE034"
    case missingParameter           = "FRE035"
}

struct ErrorHandler {
    
    static func reject(_ call: CAPPluginCall, code: ErrorCode) {
        call.reject("", code.rawValue )
    }
    
}

