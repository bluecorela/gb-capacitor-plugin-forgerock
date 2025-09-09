import { registerPlugin } from '@capacitor/core';

export interface ForgerockBridgePlugin {
  initialize(options: {
    url: string;
    realm: string;
    journey: string;
    oauthClientId: string;
    oauthScope: string;
  }): Promise<void>;
  authenticate(options: { journey: string; username?: string; password?: string; isRetry?: boolean }): Promise<{
    authId?: string;
    token?: string;
    userExists?: boolean;
    status?: string;
    errorMessage?: string;
    callbacks: string[];
  }>;
  logout(): Promise<{
    message: string;
  }>;
  userInfo(): Promise<string>;
  getAccessToken(): Promise<string>;
  initializeOTPRegister(options: {
    journey: string;
  }): Promise<{ status: string }>;
  deleteOTPRegister(options: {
    journey: string;
  }): Promise<{ status: string }>;
  validateOTP(options: { url: string;}): Promise<{
    empty: boolean;
  }>;
  hasRegisteredMechanism(): Promise<{
    empty: boolean;
  }>;
  generateOTP(): Promise<{
    otp: string;
  }>;
  initForgotPassword(options: { journey: string; username?: string;}): Promise<{ status: string, message:string }>;
  getQuestionForgotPassword(): Promise<{question: string}>;
  answerQuestionForgotPassword(options: {answer: string}) : Promise<{ status: string, message: string }>;
  changePasswordForgotPassword(options: {password: string}) : Promise<{ status: string, message: string }>;
}

const ForgerockBridge = registerPlugin<ForgerockBridgePlugin>('ForgerockBridge');

export default ForgerockBridge;

