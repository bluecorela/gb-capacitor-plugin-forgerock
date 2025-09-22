import { WebPlugin } from '@capacitor/core';

import type { AuthMethodResponse, ForgerockBridgePlugin, GetAuthMethodRequest, ValidAuthMethodRequest } from './definitions';


export class ForgerockBridgeWeb extends WebPlugin implements ForgerockBridgePlugin {

  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }

    async initialize(options: {
    url: string;
    realm: string;
    journey: string;
    oauthClientId: string;
    oauthScope: string;
  }): Promise<void> {
    console.log('SDK inicializado en web con:', options);
  }

  async authenticate(): Promise<any> {
    return { success: true };
  }

  async logout(): Promise<any> {
    return { success: true };
  }

  async userInfo(): Promise<any> {
    return { success: true };
  }

  async getAccessToken(): Promise<any> {
    return { success: true };
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  async initializeOTPRegister(_options: {
    journey: string;
  }): Promise<{ status: string }> {
    console.warn('initializeOTPRegister is not supported on web');
    return { status: 'not_implemented' };
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  deleteOTPRegister(_options: { journey: string; }): Promise<{ status: string; }> {
    throw new Error('Method not implemented.');
  }

  validateOTP(_options: {
    url: string;
  }): Promise<{empty: true}> {
    return Promise.resolve({ empty: true });
  }

  hasRegisteredMechanism(): Promise<{empty: true}> {
    return Promise.resolve({ empty: true });
  }

  generateOTP(): Promise<{otp: ""}> {
    return Promise.resolve({ otp: "" });
  }

  initForgotPassword(_options: {
    journey: string; username: string; language:string
  }): Promise<{ status: string, message:string }> {
    return Promise.resolve({ status: "", message: "" });
  }

  getQuestionForgotPassword (): Promise<{question: string}> {
    return Promise.resolve({question: ""});
  }

  answerQuestionForgotPassword(_options: {
    answer: string;
  }): Promise<{ status: string, message: string }> {
    return Promise.resolve({ status: "", message: "" });
  }

  changePasswordForgotPassword(_options: {
    password: string;
  }): Promise<{ status: string, message: string }> {
    return Promise.resolve({ status: "", message: "" });
  }

  async isValidAuthMethod(options: GetAuthMethodRequest): Promise<AuthMethodResponse>;
  async isValidAuthMethod(options: ValidAuthMethodRequest): Promise<AuthMethodResponse>;
  async isValidAuthMethod(
    options: GetAuthMethodRequest | ValidAuthMethodRequest
  ): Promise<AuthMethodResponse > {
    try {
      const res = await fetch(options.url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Trx-Id': options.trxId,
        },
        body: 'payload' in options ? JSON.stringify(options.payload) : undefined,
      });

      if (!res.ok) {
   
        return Promise.resolve({callbacks: []});
      }

      const data = await res.json();
      return data;
    } catch (err: any) {
      return Promise.resolve({callbacks: []});
    }
  }
}
