import { Platform } from 'react-native';
import type {
  SendMessage,
  Payload,
  SendMessageWithPath,
} from './NativeWearConnectivity';
import { WearConnectivity } from './index';
import { LIBRARY_NAME, IOS_NOT_SUPPORTED_WARNING } from './constants';

const UNHANDLED_CALLBACK =
  'The sendMessage function was called without a callback function. ';
const UNHANDLED_CALLBACK_REPLY =
  'The callback function was invoked with the payload: ';
const UNHANDLED_CALLBACK_ERROR =
  'The callback function was invoked with the error: ';

const defaultReplyCb = (reply: Payload) => {
  console.log(UNHANDLED_CALLBACK + UNHANDLED_CALLBACK_REPLY, reply);
};
const defaultErrCb = (err: string) => {
  console.warn(UNHANDLED_CALLBACK + UNHANDLED_CALLBACK_ERROR, err);
};

const sendMessage: SendMessage = (message, cb, errCb) => {
  const json: Payload = { ...message, event: 'message' };
  const callbackWithDefault = cb ?? defaultReplyCb;
  const errCbWithDefault = errCb ?? defaultErrCb;
  return WearConnectivity.sendMessage(
    json,
    callbackWithDefault,
    errCbWithDefault
  );
};

const sendMessageWithPath: SendMessageWithPath = (path, payload, cb, errCb) => {
  const callbackWithDefault = cb ?? defaultReplyCb;
  const errCbWithDefault = errCb ?? defaultErrCb;
  return WearConnectivity.sendMessageWithPath(
    path,
    payload,
    callbackWithDefault,
    errCbWithDefault
  );
};

const sendMessageMock: SendMessage = () =>
  console.warn(LIBRARY_NAME + 'message' + IOS_NOT_SUPPORTED_WARNING);

const sendMessageWithPathMock: SendMessageWithPath = () =>
  console.warn(LIBRARY_NAME + 'messageWithPath' + IOS_NOT_SUPPORTED_WARNING);

let sendMessageExport: SendMessage = sendMessageMock;
if (Platform.OS !== 'ios') {
  sendMessageExport = sendMessage;
}

let sendMessageWithPathExport: SendMessageWithPath = sendMessageWithPathMock;
if (Platform.OS !== 'ios') {
  sendMessageWithPathExport = sendMessageWithPath;
}

export {
  sendMessageExport as sendMessage,
  sendMessageWithPathExport as sendMessageWithPath,
};
