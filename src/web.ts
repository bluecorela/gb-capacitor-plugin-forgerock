import type { ForgerockBridgePlugin } from './definitions';
import { WebPlugin } from '@capacitor/core';

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
}
