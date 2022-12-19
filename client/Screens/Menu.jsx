import React, {useEffect,useState} from 'react';
import {View ,StyleSheet} from 'react-native';
import 'react-native-gesture-handler';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { Header } from 'react-native-elements';
import Icon from 'react-native-vector-icons/Ionicons';

import Test from './Test';
import Today from './Today';
import Info from './Info';


const Menu = ({route,navigation})=>{
    const userinfo=route.params
    console.log(userinfo)

    const Tab = createBottomTabNavigator();

    return(
        <>
         <Header
            backgroundColor='rgba(44,117,84,0.8)'
            leftComponent={{text:"오늘자격", style:{width:200,fontSize:35,fontWeight: 'bold'}}}
        />
            <Tab.Navigator initialRouteName="Test" screenOptions={({route }) => ({
                headerShown: false,
                tabBarInActiveTintColor:"grey",
                tabBarStyle:{
                    height: "10%",
                    backgroundColor:'rgba(44,117,84,0.8)',
                    showLabel:false
                },
                tabBarIcon: ({ color, size, focused }) => {
                    let iconName;
                    if (route.name === 'Test') {
                        iconName ='bulb-outline';
                        { focused ? color="white" :color="black"}
                        size=30
                    } else if (route.name === 'Today') {
                        iconName ='pencil';
                        { focused ? color="white" :color="black"}
                        size=30
                    }
                    else if (route.name === 'Info') {
                        iconName = 'ios-ribbon-outline';
                        { focused ? color="white" :color="black"}
                        size=30
                    }
                return <Icon name={iconName} size={size} color={color} />
                }}
            )}>

                <Tab.Screen 
                    name="Test" 
                    options={{
                        tabBarLabel:"문제 은행",
                        tabBarLabelStyle: {
                            color:"black",
                            fontSize: 12,
                            fontWeight: "bold",
                        }
                    }} 
                    children={({navigation})=><Test userinfo={userinfo}/>} 
                />
                <Tab.Screen 
                    name="Today" 
                    options={{
                        tabBarLabel:"오늘 자격",
                        tabBarLabelStyle: {
                            color:"black",
                            fontSize: 12,
                            fontWeight: "bold",
                        }
                    }} 
                    children={({navigation})=><Today userinfo={userinfo}/>} 
                />
                <Tab.Screen 
                    name="Info" 
                    options={{
                        tabBarLabel:"마이 페이지",
                        tabBarLabelStyle: {
                            color:"black",
                            fontSize: 12,
                            fontWeight: "bold",
                        }
                    }} 
                    children={({navigation})=><Info userinfo={userinfo}/>} 
                />
            </Tab.Navigator>
        </>
    );
}

export default Menu;

const styles = StyleSheet.create({

    topSection:{
        alignItems:'center',
        justifyContent:'center',
        height:'5%',
    },
    input:{
        height:52,
        width:'100%',
        color:'white',
        borderRadius:10,
        borderwidth: "1px",
        borderColor:'rgba(255,255,255,0.7)',
        paddingLeft:10
    },
    body:{
        flex:1,
        backgroundColor:'white',
        flexDirection:'column',
        justifyContent:'space-between'
    },
    bottomSection:{
        width:'100%',
        height:'95%',
        backgroundColor:'rgba(44,117,84,0.8)',
    },
    rowcontainer:{
        justifyContent: 'center', 
        alignItems: 'center',
        marginTop:"12%",
        flexDirection:"row",
    },
})
