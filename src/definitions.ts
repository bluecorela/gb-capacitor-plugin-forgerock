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
}

const ForgerockBridge = registerPlugin<ForgerockBridgePlugin>('ForgerockBridge');

export default ForgerockBridge;

