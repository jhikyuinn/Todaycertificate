import * as React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createStackNavigator } from '@react-navigation/stack';
import Login from './Screens/Login';
import Signup from './Screens/Signup';
import Menu from './Screens/Menu';
import TestDetails from './Screens/TestDetails';


export default function App() {
  const Stack = createStackNavigator();

  return (
    <NavigationContainer>
      <Stack.Navigator 
        screenOptions={{ 
            headerShown: false, 
            animationEnabled: Platform.select({
            ios: true,
            android: false,
            }) 
        }}>
        <Stack.Screen name="Login" component={Login} />
        <Stack.Screen name="Signup" component={Signup} />
        <Stack.Screen name="Menu" component={Menu} />
        <Stack.Screen name="TestDetails" component={TestDetails} />
        
      </Stack.Navigator>
    </NavigationContainer>
  );
}
