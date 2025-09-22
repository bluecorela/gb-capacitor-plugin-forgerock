import Foundation
import Capacitor
import FRAuth
import FRCore
import FRAuthenticator

class ForgotPasswordInterceptor: RequestInterceptor {
    
    private let languageCode: String
    
    init(languageCode: String) {
        self.languageCode = languageCode
    }
    
    func intercept(request: Request, action: Action) -> Request {
        
        var headers = request.headers
        headers["Accept-Language"] = languageCode

        let newRequest = Request(
            url: request.url,
            method: request.method,
            headers: headers,
            bodyParams: request.bodyParams,
            urlParams: request.urlParams,
            requestType: request.requestType,
            responseType: request.responseType,
            timeoutInterval: request.timeoutInterval
        )
        return newRequest
    }
}
