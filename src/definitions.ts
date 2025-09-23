import { registerPlugin } from '@capacitor/core';


export type PasswordCallback = {
  type: "PasswordCallback";
  output: [{ name: "prompt"; value: string }];
  input: [{ name: "IDToken1"; value: string }];
};

export type AuthMethodResponse = {
  authId?: string;
  callbacks: PasswordCallback[];
  header? : string
};

export type GetAuthMethodRequest = {
  url: string;
  trxId: string;
};

export type ValidAuthMethodRequest = {
  url: string;
  trxId: string;
  payload: AuthMethodResponse; 
};

export type ForgeRogeResponse = { status: string; message: string };
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
  initForgotPassword(options: { journey: string; username: string; language:string}): Promise<{ status: string, message:string }>;
  getQuestionForgotPassword(): Promise<{question: string}>;
  answerQuestionForgotPassword(options: {answer: string}) : Promise<{ status: string, message: string }>;
  changePasswordForgotPassword(options: {password?: string}) : Promise<{ status: string, message: string }>;
  getCurrentSession() : Promise<{ currentSesion: string}>;


  isValidAuthMethod(options: GetAuthMethodRequest): Promise<AuthMethodResponse>;
  isValidAuthMethod(options: ValidAuthMethodRequest): Promise<AuthMethodResponse>;
}

const ForgerockBridge = registerPlugin<ForgerockBridgePlugin>('ForgerockBridge');

export default ForgerockBridge;

