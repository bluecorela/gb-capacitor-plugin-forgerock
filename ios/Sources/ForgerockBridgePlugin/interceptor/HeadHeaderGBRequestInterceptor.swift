//
//  HeadHeaderGBRequestInterceptor.swift
//  Pods
//
//  Created by Indira Cornejo on 10/02/25.
//

import FRCore


class HeadHeaderGBRequestInterceptor: RequestInterceptor {
  
  private let languageCode: String
  
  init(languageCode: String) {
      self.languageCode = languageCode
  }
  
    func intercept(request: FRCore.Request, action: FRCore.Action) -> Request {
        
        if (action.type == "START_AUTHENTICATE" || action.type == "AUTHENTICATE") {
            print("action.payload: ",action.payload )
            print("action.type: ",action.type)
            var urlParams = request.urlParams
           urlParams["ForceAuth"] = "true"
            var headers = request.headers
            headers["Accept-Language"] = languageCode

            let newRequest = Request(
                            url: request.url,
                            method: request.method,
                            headers: headers,
                            bodyParams: request.bodyParams,
                            urlParams: urlParams,
                            requestType: request.requestType,
                            responseType: request.responseType,
                            timeoutInterval: request.timeoutInterval
                        )
            return newRequest
            
        }else{
            let modifiedRequest = request
            return modifiedRequest
        }
    }
}
