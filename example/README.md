# react-native-instantpay-call-auth

React Native module for Detecting Call with event listener. Supports only for Android.

## TOC

- [Installation](#installation)
- [Manual Installation](#manual-installation)
- [Usage](#usage)

## Installation

```sh
npm install react-native-instantpay-call-auth
```


## Manual installation
### Android
1. Open up `android/app/src/main/java/[...]/MainApplication.java`
  - Add `import com.instantpaycallauth.InstantpayCallAuthPackage;` to the imports at the top of the file
  - Add `new InstantpayCallAuthPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-instantpay-call-auth'
  	project(':react-native-instantpay-call-auth').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-instantpay-call-auth/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-instantpay-call-auth')
  	```

## Usage

```js
import RNCallAuth from 'react-native-instantpay-call-auth';

// ...

let data = await RNCallAuth.getStatus();
```


### Event Listener Methods

```js

//Add Listener
RNBluetooth.addEventListener("change", handleConnection);

//Remove Listener
RNBluetooth.removeEventListener("change", handleConnection);

handleConnection = (resp) => {
    console.log('response:', resp);
}

```


## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [Instantpay](https://www.instantpay.in)
