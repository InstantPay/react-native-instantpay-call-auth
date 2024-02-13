import { NativeModules, Platform, NativeEventEmitter } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-instantpay-call-auth' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const InstantpayCallAuth = NativeModules.InstantpayCallAuth
  ? NativeModules.InstantpayCallAuth
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

/* export function multiply(a: number, b: number): Promise<number> {
  return InstantpayCallAuth.multiply(a, b);
}

export function userAuthenticate(): Promise<any> {
  return InstantpayCallAuth.userAuthenticate();
}

export function unbindUserAuthenticate(): Promise<any> {
  return InstantpayCallAuth.unbindUserAuthenticate();
}

export function startCallScreening(): Promise<any> {
  return InstantpayCallAuth.startCallScreening();
}

export function stopCallScreening(): Promise<any> {
  return InstantpayCallAuth.stopCallScreening();
} */

const InstantpayCallAuthEventEmitter = new NativeEventEmitter(InstantpayCallAuth);

const CONNECTIVITY_EVENT = ['CallScreeningResult','RequiredPermissionResult', 'CallerIdPermissionResult'];

const _subscriptions = new Map();

const RNCallAuth = {

    addEventListener: (eventName:string, handler:any) => {

        let listener;

        if(CONNECTIVITY_EVENT.includes(eventName)){

            listener = InstantpayCallAuthEventEmitter.addListener(
                eventName,
                (appStateData) => {
                    handler(appStateData);
                }
            );
        }
        else{

            console.warn('Trying to subscribe to unknown event: "' + eventName + '"');

            return {
                remove: () => {}
            };
        }

        _subscriptions.set(handler, listener);

        return {
            remove: () => RNCallAuth.removeEventListener(eventName, handler)
        };
    },
    removeEventListener: (_eventName:string, handler:any) => {
        
        const listener = _subscriptions.get(handler);
        
        if (!listener) {
            return;
        }
        
        listener.remove();

        _subscriptions.delete(handler);
    },
    isAppAllowForScreening: () => {
        return InstantpayCallAuth.isAppAllowForScreening();
    },
    requestForPermission:() =>{
        return InstantpayCallAuth.requestForPermission();
    },
    startCallScreening: (options={}) => {

        let params = null;

        if(Object.keys(options).length > 0){
            params = JSON.stringify(options);
        }

        return InstantpayCallAuth.startCallScreening(params);
    },
    stopCallScreening: () => {

        return InstantpayCallAuth.stopCallScreening();
    }
}

export default RNCallAuth;
