import * as React from 'react';

import { StyleSheet, View, Text, Button } from 'react-native';
import RNCallAuth from 'react-native-instantpay-call-auth';

export default function App() {
  const [result, setResult] = React.useState();

    React.useEffect(() => {
       
    }, []);

    const startCallScreening = async () => {
        let out = await RNCallAuth.startCallScreening({
            initateFor : "",
            countryCode : "+91",
            verifyFor : "",
            requiredAction: {
                matchWithVerifyFor : true,
                silenceCall : false,
                disallowCall : true, //Call Stop for the number
                rejectCall : true, //rejectCall work based on disallowCall
                skipCallLog : false, //skipCallLog work based on disallowCall
                skipNotification : false,
            }
        });

        console.log(out)
      };
    
    const stopCallScreening = async () => {
        let out = await RNCallAuth.stopCallScreening();
        console.log(out)
    };

    const listenOnChangeBluetoothState = () => {
        RNCallAuth.addEventListener("CallScreeningResult",handleConnection)
    }

    const removeListener = () => {
        RNCallAuth.removeEventListener("CallScreeningResult",handleConnection)
        
    }

    handleConnection = (resp) => {
        //let {connectionState} = resp.type;  
        console.log('type ', resp);
    }

    const isAppAllowForScreenings = async () => {
        let out = await RNCallAuth.isAppAllowForScreening();

        console.log(out);
    }

    const callForPermission = async () => {

        let out = await RNCallAuth.requestForPermission();

        console.log(out);
    }
    

    return (
        <View style={styles.container}>
            <Text>Actions List </Text>
            <Button title="Request Permission" onPress={() => callForPermission()} />
            <Text></Text>
            <Button title="Check Allowness" onPress={() => isAppAllowForScreenings()} />
            <Text></Text>
            <Button title="Start Call Screening" onPress={startCallScreening} />
            <Text></Text>
            <Button title="Stop Call Screening" onPress={stopCallScreening} />
            <Text></Text>
            <Button title='Remove Listener' style={{marginBottom:20}} onPress={() => removeListener()} />
            <Text></Text>
            <Button title='Add Listener' onPress={() => listenOnChangeBluetoothState()} />
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
    },
    box: {
        width: 60,
        height: 60,
        marginVertical: 20,
    },
});
