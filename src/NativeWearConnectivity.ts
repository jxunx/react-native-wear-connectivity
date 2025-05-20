import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

// Messages
export type Payload = {};
export type DataOptions = {};
export type ReplyCallback = (reply: Payload) => void;
export type ErrorCallback = (err: string) => void;

export type SendMessage = (
  message: Payload,
  cb: ReplyCallback,
  errCb: ErrorCallback
) => void;

export type SendMessageWithPath = (
  path: String,
  message: Payload,
  cb: ReplyCallback,
  errCb: ErrorCallback
) => void;

export type SendData = (
  path: String,
  message: Payload,
  options?: DataOptions
) => Promise<string>;

export type SendFile = (file: string, metadata: unknown) => Promise<any>;

export interface Spec extends TurboModule {
  sendMessage: SendMessage;
  sendFile: SendFile;
}

export default TurboModuleRegistry.getEnforcing<Spec>('WearConnectivity');
