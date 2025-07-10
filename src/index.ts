import type { ForgerockBridgePlugin } from './definitions';
import { registerPlugin } from '@capacitor/core';

const ForgerockBridge = registerPlugin<ForgerockBridgePlugin>('ForgerockBridge', {
  web: () => import('./web').then((m) => new m.ForgerockBridgeWeb()),
});

export * from './definitions';
export default ForgerockBridge ;

