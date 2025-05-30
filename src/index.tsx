import { AppRegistry } from 'react-native';
import { NativeModules, Platform } from 'react-native';
import { DataOptionKey } from './constants';
import { watchEvents } from './subscriptions';
import { sendMessage, sendMessageWithPath } from './messages';
import type {
  ReplyCallback,
  ErrorCallback,
  SendFile,
  SendData,
} from './NativeWearConnectivity';
import { DeviceEventEmitter } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-wear-connectivity' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

// @ts-expect-error
const isTurboModuleEnabled = global.__turboModuleProxy != null;

const WearConnectivityModule = isTurboModuleEnabled
  ? require('./NativeWearConnectivity').default
  : NativeModules.WearConnectivity;

const WearConnectivity = WearConnectivityModule
  ? WearConnectivityModule
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

const getConnectedNodes = () => WearConnectivity.getConnectedNodes();

const startFileTransfer: SendFile = (file, _metadata) => {
  return WearConnectivity.sendFile(file, _metadata);
};

const sendData: SendData = (path, data, options = {}) => {
  return WearConnectivity.sendData(path, data, options);
};

export {
  getConnectedNodes,
  startFileTransfer,
  sendData,
  sendMessage,
  sendMessageWithPath,
  watchEvents,
  WearConnectivity,
  DataOptionKey,
};
export type { ReplyCallback, ErrorCallback };

type WearParameters = {
  event: string;
  text: string;
};

// Define the headless task
const WearConnectivityTask = async (taskData: WearParameters) => {
  // Emit an event or process the message as needed
  DeviceEventEmitter.emit('message', taskData);
};

/**
 * Monitors file transfer events.
 * @param callback Function to receive transfer events.
 * @returns Unsubscribe function.
 */
export function monitorFileTransfers(callback: (event: any) => void) {
  const subscription = DeviceEventEmitter.addListener(
    'FileTransferEvent',
    callback
  );

  return () => {
    subscription.remove();
  };
}

// Register the headless task with React Native
AppRegistry.registerHeadlessTask(
  'WearConnectivityTask',
  () => WearConnectivityTask
);
