import React from 'react';

import {
  AppRegistry,
  StyleSheet,
  TouchableOpacity,
  Text,
  View
} from 'react-native';

class HelloWorld extends React.Component {
      state = {
        count: 0
      }

    onPress = () => {
        this.setState({
            count: this.state.count + 1
        })
    }

    render() {
        return (
            <View style={styles.container}>
                <TouchableOpacity
                    style={styles.button}
                    onPress={this.onPress}>
                    <Text>Click me</Text>
                </TouchableOpacity>
                <View>
                    <Text>
                        This View is constructed in ReactNative
                    </Text>
                    <Text>
                        You have pushed the button { this.state.count } times
                    </Text>
                </View>
            </View>
        );
    }
}
var styles = StyleSheet.create({
    container: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
    },
    hello: {
        fontSize: 20,
        textAlign: 'center',
        margin: 10
    },
    button: {
      alignItems: 'center',
      backgroundColor: '#DDDDDD',
      padding: 10,
      marginBottom: 10
    }
});

AppRegistry.registerComponent(
  'ReactNativeApp',
  () => HelloWorld
);